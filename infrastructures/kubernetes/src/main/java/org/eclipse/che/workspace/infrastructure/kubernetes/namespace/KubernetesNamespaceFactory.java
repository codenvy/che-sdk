/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.kubernetes.namespace;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.eclipse.che.api.workspace.shared.Constants.WORKSPACE_INFRASTRUCTURE_NAMESPACE_ATTRIBUTE;
import static org.eclipse.che.workspace.infrastructure.kubernetes.api.shared.KubernetesNamespaceMeta.DEFAULT_ATTRIBUTE;
import static org.eclipse.che.workspace.infrastructure.kubernetes.api.shared.KubernetesNamespaceMeta.PHASE_ATTRIBUTE;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Named;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.user.User;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.NamespaceResolutionContext;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.inject.ConfigurationException;
import org.eclipse.che.workspace.infrastructure.kubernetes.KubernetesClientFactory;
import org.eclipse.che.workspace.infrastructure.kubernetes.api.server.impls.KubernetesNamespaceMetaImpl;
import org.eclipse.che.workspace.infrastructure.kubernetes.api.shared.KubernetesNamespaceMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps to create {@link KubernetesNamespace} instances.
 *
 * @author Anton Korneta
 */
@Singleton
public class KubernetesNamespaceFactory {

  private static final Logger LOG = LoggerFactory.getLogger(KubernetesNamespaceFactory.class);

  private static final Map<String, Function<NamespaceResolutionContext, String>>
      NAMESPACE_NAME_PLACEHOLDERS = new HashMap<>();

  private static final String USERNAME_PLACEHOLDER = "<username>";
  private static final String USERID_PLACEHOLDER = "<userid>";
  private static final String WORKSPACEID_PLACEHOLDER = "<workspaceid>";

  static {
    NAMESPACE_NAME_PLACEHOLDERS.put(USERNAME_PLACEHOLDER, NamespaceResolutionContext::getUserName);
    NAMESPACE_NAME_PLACEHOLDERS.put(USERID_PLACEHOLDER, NamespaceResolutionContext::getUserId);
    NAMESPACE_NAME_PLACEHOLDERS.put(
        WORKSPACEID_PLACEHOLDER, NamespaceResolutionContext::getWorkspaceId);
  }

  private final String defaultNamespaceName;
  private final boolean allowUserDefinedNamespaces;

  private final String legacyNamespaceName;
  private final String serviceAccountName;
  private final String clusterRoleName;
  private final KubernetesClientFactory clientFactory;
  private final UserManager userManager;

  @Inject
  public KubernetesNamespaceFactory(
      @Nullable @Named("che.infra.kubernetes.namespace") String legacyNamespaceName,
      @Nullable @Named("che.infra.kubernetes.service_account_name") String serviceAccountName,
      @Nullable @Named("che.infra.kubernetes.cluster_role_name") String clusterRoleName,
      @Nullable @Named("che.infra.kubernetes.namespace.default") String defaultNamespaceName,
      @Named("che.infra.kubernetes.namespace.allow_user_defined")
          boolean allowUserDefinedNamespaces,
      KubernetesClientFactory clientFactory,
      UserManager userManager)
      throws ConfigurationException {
    this.userManager = userManager;
    this.legacyNamespaceName = legacyNamespaceName;
    this.serviceAccountName = serviceAccountName;
    this.clusterRoleName = clusterRoleName;
    this.clientFactory = clientFactory;
    this.defaultNamespaceName = defaultNamespaceName;
    this.allowUserDefinedNamespaces = allowUserDefinedNamespaces;

    // This will disappear once we support user selection of workspaces...
    if (allowUserDefinedNamespaces) {
      LOG.warn(
          "'che.infra.kubernetes.namespace.allow_user_defined' is not supported yet. It currently has no"
              + " effect.");
    }

    // right now allowUserDefinedNamespaces can't be true, but eventually we will implement it.
    if (isNullOrEmpty(defaultNamespaceName) && !allowUserDefinedNamespaces) {
      throw new ConfigurationException(
          "che.infra.kubernetes.namespace.default or "
              + "che.infra.kubernetes.namespace.allow_user_defined must be configured");
    }
  }

  private boolean hasPlaceholders(String namespaceName) {
    return namespaceName != null
        && NAMESPACE_NAME_PLACEHOLDERS.keySet().stream().anyMatch(namespaceName::contains);
  }

  /**
   * True if namespace is potentially created for the workspace, false otherwise.
   *
   * <p>The logic is a little bit non-trivial and best expressed by just fully evaluating the truth
   * table as below ({@code ...namespace} stands for the legacy namespace property, {@code
   * ...namespace.default} stands for the namespace default property):
   *
   * <pre>{@code
   * ...namespace    | ...namespace exists | ...namespace.default | creating?
   * no-placeholders |       no            |       null           | error
   * no-placeholders |       no            |   no-placeholders    | no
   * no-placeholders |       no            |    placeholders      | yes
   * no-placeholders |      yes            |       null           | no
   * no-placeholders |      yes            |   no-placeholders    | no
   * no-placeholders |      yes            |    placeholders      | no
   *  placeholders   |       no            |        null          | error
   *  placeholders   |       no            |   no-placeholders    | no
   *  placeholders   |       no            |    placeholders      | yes
   *  placeholders   |      yes            |        null          | yes
   *  placeholders   |      yes            |   no-placeholders    | yes
   *  placeholders   |      yes            |    placeholders      | yes
   * }</pre>
   */
  protected boolean isCreatingNamespace(RuntimeIdentity identity) throws InfrastructureException {
    User owner;
    try {
      owner = userManager.getById(identity.getOwnerId());
    } catch (NotFoundException | ServerException e) {
      throw new InfrastructureException(
          "Failed to resolve workspace owner. Cause: " + e.getMessage(), e);
    }

    boolean legacyExists =
        checkNamespaceExists(
            resolveLegacyNamespaceName(
                new NamespaceResolutionContext(
                    identity.getWorkspaceId(), identity.getOwnerId(), owner.getName())));

    // legacy namespace exists and should be used
    if (legacyExists) {
      // if it contains any placeholder("" is <workspaceid>) - it indicates that Che created
      // namespace by itself
      return isNullOrEmpty(legacyNamespaceName) || hasPlaceholders(legacyNamespaceName);
    }

    if (isNullOrEmpty(defaultNamespaceName)) {
      throw new InfrastructureException(
          "Cannot determine whether a new namespace and service account should be"
              + " created for workspace %s. There is no pre-existing workspace namespace to be"
              + " found using the legacy `che.infra.kubernetes.namespace` property yet the"
              + " `che.infra.kubernetes.namespace.default` property is undefined.");
    }

    return hasPlaceholders(defaultNamespaceName);
  }

  /**
   * Returns true if the namespace is fully managed by Che (e.g. created, used and deleted), false
   * otherwise.
   */
  public boolean isManagingNamespace(Workspace workspace) throws InfrastructureException {
    // the namespace is managed if it is marked as managed or if we're using the legacy method
    // on old workspaces lacking the explicit namespace attribute
    return get(workspace).isMarkedManaged()
        || (workspace.getAttributes().get(WORKSPACE_INFRASTRUCTURE_NAMESPACE_ATTRIBUTE) == null
            && isManagingImpliedNamespace(workspace.getId()));
  }

  @VisibleForTesting
  boolean isManagingImpliedNamespace(String workspaceId) throws InfrastructureException {
    // the new logic is quite simple.
    boolean newLogic =
        defaultNamespaceName != null && defaultNamespaceName.contains(WORKSPACEID_PLACEHOLDER);

    // but we must follow the same logic as #evalNamespaceName - we need to make sure that the old
    // logic can't be used first...
    // empty legacy namespace name ~ <workspaceid>
    if (isNullOrEmpty(legacyNamespaceName)
        || legacyNamespaceName.contains(WORKSPACEID_PLACEHOLDER)) {

      // there's a chance of using the old logic - if the namespace exists, we're managing it.
      // if it doesn't, we're using the new logic.

      Subject subject = EnvironmentContext.getCurrent().getSubject();
      NamespaceResolutionContext resolutionCtx =
          new NamespaceResolutionContext(workspaceId, subject.getUserId(), subject.getUserName());
      return checkNamespaceExists(resolveLegacyNamespaceName(resolutionCtx)) || newLogic;
    } else {
      // there's no way the namespace of the workspace is managed using the old logic. Let's just
      // use the result of the new logic.
      return newLogic;
    }
  }

  /**
   * Creates a Kubernetes namespace for the specified workspace.
   *
   * <p>Namespace won't be prepared. This method should be used only in case workspace recovering.
   *
   * @param workspaceId identifier of the workspace
   * @return created namespace
   */
  public KubernetesNamespace access(String workspaceId, String namespace) {
    return doCreateNamespaceAccess(workspaceId, namespace);
  }

  @VisibleForTesting
  KubernetesNamespace doCreateNamespaceAccess(String workspaceId, String name) {
    return new KubernetesNamespace(clientFactory, name, workspaceId);
  }

  /**
   * Checks if the current user is able to use the specified namespace for their new workspaces.
   *
   * @param namespaceName namespace name to check
   * @throws ValidationException if the specified namespace is not permitted for the current user
   */
  public void checkIfNamespaceIsAllowed(String namespaceName) throws ValidationException {
    if (allowUserDefinedNamespaces) {
      // any namespace name is allowed but workspace start may fail
      return;
    }

    String defaultNamespace =
        evalPlaceholders(defaultNamespaceName, EnvironmentContext.getCurrent().getSubject(), null);
    if (!namespaceName.equals(defaultNamespace)) {
      throw new ValidationException(
          "User defined namespaces are not allowed. You're able to specify only admin configured which is '"
              + defaultNamespaceName
              + '\'');
    }
  }

  /** Returns list of k8s namespaces names where a user is able to run workspaces. */
  public List<KubernetesNamespaceMeta> list() throws InfrastructureException {
    if (!allowUserDefinedNamespaces) {
      return singletonList(getDefaultNamespace());
    }

    // if user defined namespaces are allowed - fetch all available
    List<KubernetesNamespaceMeta> namespaces = fetchNamespaces();

    // propagate default namespace if it's configured
    if (!isNullOrEmpty(defaultNamespaceName)) {
      provisionDefaultNamespace(namespaces);
    }
    return namespaces;
  }

  /**
   * Returns default namespace, it's based on existing namespace if there is such or just object
   * holder if there is no such namespace on cluster.
   */
  private KubernetesNamespaceMeta getDefaultNamespace() throws InfrastructureException {
    // the default namespace must be configured if user defined are not allowed
    // so return only it
    NamespaceResolutionContext resolutionCtx =
        new NamespaceResolutionContext(
            // workspace id is not know at this stage.
            // It's good enough to have <workspaceid> placeholder after evaluating
            null,
            EnvironmentContext.getCurrent().getSubject().getUserId(),
            EnvironmentContext.getCurrent().getSubject().getUserName());
    String evaluatedName = evaluateNamespaceName(resolutionCtx);

    Optional<KubernetesNamespaceMeta> defaultNamespaceOpt = fetchNamespace(evaluatedName);

    KubernetesNamespaceMeta defaultNamespace =
        defaultNamespaceOpt
            // if the predefined namespace does not exist - return dummy info and it will be created
            // during the first workspace start
            .orElseGet(() -> new KubernetesNamespaceMetaImpl(evaluatedName));

    defaultNamespace.getAttributes().put(DEFAULT_ATTRIBUTE, "true");
    return defaultNamespace;
  }

  /**
   * Provision default namespace into the specified list. If default namespace is already there -
   * just provision the corresponding attributes to it.
   *
   * @param namespaces list where default namespace should be provisioned
   */
  private void provisionDefaultNamespace(List<KubernetesNamespaceMeta> namespaces) {
    String evaluatedName =
        evalPlaceholders(defaultNamespaceName, EnvironmentContext.getCurrent().getSubject(), null);

    Optional<KubernetesNamespaceMeta> defaultNamespaceOpt =
        namespaces.stream().filter(n -> evaluatedName.equals(n.getName())).findAny();
    KubernetesNamespaceMeta defaultNamespace;
    if (defaultNamespaceOpt.isPresent()) {
      defaultNamespace = defaultNamespaceOpt.get();
    } else {
      defaultNamespace = new KubernetesNamespaceMetaImpl(evaluatedName);
      namespaces.add(defaultNamespace);
    }

    defaultNamespace.getAttributes().put(DEFAULT_ATTRIBUTE, "true");
  }

  /**
   * Fetches the specified namespace from a cluster.
   *
   * @param name name of namespace that should be fetched.
   * @return optional with kubernetes namespace meta
   * @throws InfrastructureException when any error occurs during namespace fetching
   */
  protected Optional<KubernetesNamespaceMeta> fetchNamespace(String name)
      throws InfrastructureException {
    try {
      Namespace namespace = clientFactory.create().namespaces().withName(name).get();
      if (namespace == null) {
        return Optional.empty();
      } else {
        return Optional.of(asNamespaceMeta(namespace));
      }
    } catch (KubernetesClientException e) {
      throw new InfrastructureException(
          "Error occurred when tried to fetch default namespace. Cause: " + e.getMessage(), e);
    }
  }

  /**
   * Fetched namespace from a k8s cluster.
   *
   * @return list with available k8s namespace metas.
   * @throws InfrastructureException when any error occurs during namespaces fetching
   */
  protected List<KubernetesNamespaceMeta> fetchNamespaces() throws InfrastructureException {
    try {
      return clientFactory
          .create()
          .namespaces()
          .list()
          .getItems()
          .stream()
          .map(this::asNamespaceMeta)
          .collect(Collectors.toList());
    } catch (KubernetesClientException e) {
      throw new InfrastructureException(
          "Error occurred when tried to list all available namespaces. Cause: " + e.getMessage(),
          e);
    }
  }

  private KubernetesNamespaceMeta asNamespaceMeta(Namespace namespace) {
    Map<String, String> attributes = new HashMap<>(2);
    if (namespace.getStatus() != null && namespace.getStatus().getPhase() != null) {
      attributes.put(PHASE_ATTRIBUTE, namespace.getStatus().getPhase());
    }
    return new KubernetesNamespaceMetaImpl(namespace.getMetadata().getName(), attributes);
  }

  public KubernetesNamespace getOrCreate(RuntimeIdentity identity) throws InfrastructureException {
    KubernetesNamespace namespace = get(identity);

    if (isCreatingNamespace(identity) && !isNullOrEmpty(serviceAccountName)) {
      boolean markManaged =
          // TODO Rework it according to fact that infra namespace can not be null anymore
          identity.getInfrastructureNamespace() != null
              || isManagingImpliedNamespace(identity.getWorkspaceId());

      namespace.prepare(markManaged);

      // prepare service account for workspace only if account name is configured
      // and project is not predefined
      // since predefined project should be prepared during Che deployment
      KubernetesWorkspaceServiceAccount workspaceServiceAccount =
          doCreateServiceAccount(namespace.getWorkspaceId(), namespace.getName());
      workspaceServiceAccount.prepare();
    }

    return namespace;
  }

  public KubernetesNamespace get(RuntimeIdentity identity) throws InfrastructureException {
    String workspaceId = identity.getWorkspaceId();
    String namespaceName = identity.getInfrastructureNamespace();
    return doCreateNamespaceAccess(workspaceId, namespaceName);
  }

  /**
   * Gets a namespace the workspace is deployed to.
   *
   * @param workspace the workspace
   * @return the namespace
   */
  public KubernetesNamespace get(Workspace workspace) throws InfrastructureException {
    return doCreateNamespaceAccess(workspace.getId(), getNamespaceName(workspace));
  }

  /** Returns a namespace name where workspace is assigned to. */
  protected String getNamespaceName(Workspace workspace) throws InfrastructureException {
    String namespace = workspace.getAttributes().get(WORKSPACE_INFRASTRUCTURE_NAMESPACE_ATTRIBUTE);
    if (namespace == null) {
      // it seems to be legacy workspace since the namespace is not stored in workspace attributes
      // it's needed to evaluate that with current user and workspace id
      NamespaceResolutionContext resolutionCtx =
          new NamespaceResolutionContext(
              workspace.getId(),
              EnvironmentContext.getCurrent().getSubject().getUserId(),
              EnvironmentContext.getCurrent().getSubject().getUserName());
      namespace = evaluateNamespaceName(resolutionCtx);
    }
    return namespace;
  }

  public String evaluateNamespaceName(NamespaceResolutionContext resolutionCtx)
      throws InfrastructureException {
    String namespace = resolveLegacyNamespaceName(resolutionCtx);

    if (checkNamespaceExists(namespace)) {
      LOG.debug(
          "The namespace specified using the legacy config exists: {}. Using it for workspace {}.",
          namespace,
          resolutionCtx.getWorkspaceId());
    } else {
      // ok, the namespace pointed to by the legacy config doesn't exist.. that means there can be
      // no damage done by storing the workspace in the namespace designated by the new way of
      // doing things...

      if (isNullOrEmpty(defaultNamespaceName)) {
        throw new InfrastructureException(
            format(
                "'che.infra.kubernetes.namespace.default' is not"
                    + " defined and no explicit namespace configured for workspace %s",
                resolutionCtx.getWorkspaceId()));
      }

      namespace = evalPlaceholders(defaultNamespaceName, resolutionCtx);

      LOG.debug(
          "Evaluated the namespace for workspace {} using the namespace default to {}",
          resolutionCtx.getWorkspaceId(),
          namespace);
    }

    return namespace;
  }

  public void deleteIfManaged(Workspace workspace) throws InfrastructureException {
    KubernetesNamespace namespace =
        doCreateNamespaceAccess(workspace.getId(), getNamespaceName(workspace));
    namespace.deleteIfManaged();
  }

  private String resolveLegacyNamespaceName(NamespaceResolutionContext resolutionCtx) {
    String effectiveOldLogicNamespace =
        isNullOrEmpty(legacyNamespaceName) ? WORKSPACEID_PLACEHOLDER : legacyNamespaceName;

    return evalPlaceholders(effectiveOldLogicNamespace, resolutionCtx);
  }

  protected boolean checkNamespaceExists(String namespaceName) throws InfrastructureException {
    try {
      return clientFactory.create().namespaces().withName(namespaceName).get() != null;
    } catch (KubernetesClientException e) {
      if (e.getCode() == 403) {
        // 403 means that the project does not exist
        // or a user really is not permitted to access it which is Che Server misconfiguration
        return false;
      } else {
        throw new InfrastructureException(
            "Error occurred when tried to fetch default project. Cause: " + e.getMessage(), e);
      }
    }
  }

  protected String evalPlaceholders(String namespace, Subject currentUser, String workspaceId) {
    return evalPlaceholders(
        namespace,
        new NamespaceResolutionContext(
            workspaceId, currentUser.getUserId(), currentUser.getUserName()));
  }

  protected String evalPlaceholders(String namespace, NamespaceResolutionContext ctx) {
    checkArgument(!isNullOrEmpty(namespace));
    String evaluated = namespace;
    for (Entry<String, Function<NamespaceResolutionContext, String>> placeHolder :
        NAMESPACE_NAME_PLACEHOLDERS.entrySet()) {

      String key = placeHolder.getKey();
      String value = placeHolder.getValue().apply(ctx);

      if (value != null) {
        evaluated = evaluated.replaceAll(key, value);
      }
    }
    return evaluated;
  }

  @VisibleForTesting
  KubernetesWorkspaceServiceAccount doCreateServiceAccount(
      String workspaceId, String namespaceName) {
    return new KubernetesWorkspaceServiceAccount(
        workspaceId, namespaceName, serviceAccountName, clusterRoleName, clientFactory);
  }

  protected String getServiceAccountName() {
    return serviceAccountName;
  }

  protected String getClusterRoleName() {
    return clusterRoleName;
  }
}
