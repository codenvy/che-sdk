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
package org.eclipse.che.workspace.infrastructure.kubernetes.wsplugins;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.wsplugins.ChePluginsApplier;
import org.eclipse.che.api.workspace.server.wsplugins.PluginFQNParser;
import org.eclipse.che.api.workspace.server.wsplugins.model.ChePlugin;
import org.eclipse.che.api.workspace.server.wsplugins.model.PluginFQN;
import org.eclipse.che.commons.annotation.Traced;
import org.eclipse.che.workspace.infrastructure.kubernetes.StartSynchronizer;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.namespace.pvc.EphemeralWorkspaceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provisions sidecars-powered development tooling in a workspace.
 *
 * @author Oleksandr Garagatyi
 */
@Beta
public class SidecarToolingProvisioner<E extends KubernetesEnvironment> {

  private static final Logger LOG = LoggerFactory.getLogger(SidecarToolingProvisioner.class);

  private final Map<String, ChePluginsApplier> workspaceNextAppliers;
  private final KubernetesArtifactsBrokerApplier<E> artifactsBrokerApplier;
  private final PluginFQNParser pluginFQNParser;
  private final PluginBrokerManager<E> pluginBrokerManager;
  private final EphemeralWorkspaceAdapter ephemeralWorkspaceAdapter;

  @Inject
  public SidecarToolingProvisioner(
      Map<String, ChePluginsApplier> workspaceNextAppliers,
      KubernetesArtifactsBrokerApplier<E> artifactsBrokerApplier,
      PluginFQNParser pluginFQNParser,
      PluginBrokerManager<E> pluginBrokerManager,
      EphemeralWorkspaceAdapter ephemeralWorkspaceAdapter) {
    this.workspaceNextAppliers = ImmutableMap.copyOf(workspaceNextAppliers);
    this.artifactsBrokerApplier = artifactsBrokerApplier;
    this.pluginFQNParser = pluginFQNParser;
    this.pluginBrokerManager = pluginBrokerManager;
    this.ephemeralWorkspaceAdapter = ephemeralWorkspaceAdapter;
  }

  @Traced
  @Beta
  public void provision(
      RuntimeIdentity identity, StartSynchronizer startSynchronizer, E environment)
      throws InfrastructureException {

    Collection<PluginFQN> pluginFQNs = pluginFQNParser.parsePlugins(environment.getAttributes());
    if (pluginFQNs.isEmpty()) {
      return;
    }
    LOG.debug("Started sidecar tooling provisioning workspace '{}'", identity.getWorkspaceId());
    String recipeType = environment.getType();
    ChePluginsApplier pluginsApplier = workspaceNextAppliers.get(recipeType);
    if (pluginsApplier == null) {
      throw new InfrastructureException(
          "Sidecar tooling configuration is not supported with environment type " + recipeType);
    }

    boolean isEphemeral = ephemeralWorkspaceAdapter.isEphemeral(environment.getAttributes());
    List<ChePlugin> chePlugins =
        pluginBrokerManager.getTooling(identity, startSynchronizer, pluginFQNs, isEphemeral);

    pluginsApplier.apply(identity, environment, chePlugins);
    artifactsBrokerApplier.apply(environment, identity, pluginFQNs);
    LOG.debug("Finished sidecar tooling provisioning workspace '{}'", identity.getWorkspaceId());
  }
}
