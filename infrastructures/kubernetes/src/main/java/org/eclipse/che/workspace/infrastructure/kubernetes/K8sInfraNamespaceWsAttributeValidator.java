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
package org.eclipse.che.workspace.infrastructure.kubernetes;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.eclipse.che.api.workspace.shared.Constants.WORKSPACE_INFRASTRUCTURE_NAMESPACE_ATTRIBUTE;

import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Provider;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.workspace.server.WorkspaceAttributeValidator;
import org.eclipse.che.workspace.infrastructure.kubernetes.namespace.KubernetesNamespaceFactory;

/** @author Sergii Leshchenko */
public class K8sInfraNamespaceWsAttributeValidator implements WorkspaceAttributeValidator {

  private final int METADATA_NAME_MAX_LENGTH = 63;
  private final String METADATA_NAME_REGEX = "[a-z0-9]([-a-z0-9]*[a-z0-9])?";
  private final Pattern METADATA_NAME_PATTERN = Pattern.compile(METADATA_NAME_REGEX);

  private final Provider<KubernetesNamespaceFactory> namespaceFactoryProvider;

  @Inject
  public K8sInfraNamespaceWsAttributeValidator(
      Provider<KubernetesNamespaceFactory> namespaceFactoryProvider) {
    this.namespaceFactoryProvider = namespaceFactoryProvider;
  }

  @Override
  public void validate(Map<String, String> attributes) throws ValidationException {
    String namespace = attributes.get(WORKSPACE_INFRASTRUCTURE_NAMESPACE_ATTRIBUTE);
    if (!isNullOrEmpty(namespace)) {
      if (namespace.length() > METADATA_NAME_MAX_LENGTH) {
        throw new ValidationException(
            "The specified namespace "
                + namespace
                + " is invalid: must be no more than 63 characters");
      }

      if (!METADATA_NAME_PATTERN.matcher(namespace).matches()) {
        throw new ValidationException(
            "The specified namespace "
                + namespace
                + " is invalid: a DNS-1123 label must consist of lower case alphanumeric"
                + " characters or '-', and must start and end with an"
                + " alphanumeric character (e.g. 'my-name', or '123-abc', regex used for"
                + " validation is '"
                + METADATA_NAME_REGEX
                + "')");
      }

      namespaceFactoryProvider.get().checkIfNamespaceIsAllowed(namespace);
    }
  }

  @Override
  public void validateUpdate(Map<String, String> existing, Map<String, String> update)
      throws ValidationException {
    String existingNamespace = existing.get(WORKSPACE_INFRASTRUCTURE_NAMESPACE_ATTRIBUTE);
    String updateNamespace = update.get(WORKSPACE_INFRASTRUCTURE_NAMESPACE_ATTRIBUTE);

    if (isNullOrEmpty(updateNamespace)) {
      if (isNullOrEmpty(existingNamespace)) {
        // this workspace was created before we start storing namespace info
        // namespace info will be stored during the next workspace start
        return;
      }

      throw new ValidationException(
          format(
              "The namespace information must not be updated or "
                  + "deleted. You must provide \"%s\" attribute with \"%s\" as a value",
              WORKSPACE_INFRASTRUCTURE_NAMESPACE_ATTRIBUTE, existingNamespace));
    }

    if (!updateNamespace.equals(existingNamespace)) {
      throw new ValidationException(
          format(
              "The namespace from the provided object \"%s\" does "
                  + "not match the actual namespace \"%s\"",
              updateNamespace, existingNamespace));
    }
  }
}
