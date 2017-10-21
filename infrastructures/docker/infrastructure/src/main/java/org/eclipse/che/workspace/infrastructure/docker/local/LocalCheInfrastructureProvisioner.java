/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.docker.local;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalEnvironment;
import org.eclipse.che.workspace.infrastructure.docker.InfrastructureProvisioner;
import org.eclipse.che.workspace.infrastructure.docker.local.dod.DockerApiHostEnvVariableProvisioner;
import org.eclipse.che.workspace.infrastructure.docker.local.installer.LocalInstallersConfigProvisioner;
import org.eclipse.che.workspace.infrastructure.docker.local.installer.WsAgentServerConfigProvisioner;
import org.eclipse.che.workspace.infrastructure.docker.local.projects.ProjectsVolumeProvisioner;
import org.eclipse.che.workspace.infrastructure.docker.model.DockerEnvironment;
import org.eclipse.che.workspace.infrastructure.docker.provisioner.ContainerSystemSettingsProvisionersApplier;
import org.eclipse.che.workspace.infrastructure.docker.provisioner.labels.LabelsProvisioner;
import org.eclipse.che.workspace.infrastructure.docker.provisioner.server.ToolingServersEnvVarsProvisioner;
import org.eclipse.che.workspace.infrastructure.docker.provisioner.snapshot.ExcludeFoldersFromSnapshotProvisioner;

/**
 * Infrastructure provisioner that apply needed configuration to docker containers to run it
 * locally.
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class LocalCheInfrastructureProvisioner implements InfrastructureProvisioner {
  private final ContainerSystemSettingsProvisionersApplier settingsProvisioners;
  private final ExcludeFoldersFromSnapshotProvisioner snapshotProvisioner;
  private final ProjectsVolumeProvisioner projectsVolumeProvisioner;
  private final LocalInstallersConfigProvisioner installerConfigProvisioner;
  private final LabelsProvisioner labelsProvisioner;
  private final DockerApiHostEnvVariableProvisioner dockerApiEnvProvisioner;
  private final ToolingServersEnvVarsProvisioner toolingServersEnvVarsProvisioner;
  private final WsAgentServerConfigProvisioner wsAgentServerConfigProvisioner;

  @Inject
  public LocalCheInfrastructureProvisioner(
      ContainerSystemSettingsProvisionersApplier settingsProvisioners,
      ExcludeFoldersFromSnapshotProvisioner snapshotProvisioner,
      ProjectsVolumeProvisioner projectsVolumeProvisioner,
      LocalInstallersConfigProvisioner installerConfigProvisioner,
      LabelsProvisioner labelsProvisioner,
      DockerApiHostEnvVariableProvisioner dockerApiEnvProvisioner,
      ToolingServersEnvVarsProvisioner toolingServersEnvVarsProvisioner,
      WsAgentServerConfigProvisioner wsAgentServerConfigProvisioner) {

    this.settingsProvisioners = settingsProvisioners;
    this.snapshotProvisioner = snapshotProvisioner;
    this.projectsVolumeProvisioner = projectsVolumeProvisioner;
    this.installerConfigProvisioner = installerConfigProvisioner;
    this.labelsProvisioner = labelsProvisioner;
    this.dockerApiEnvProvisioner = dockerApiEnvProvisioner;
    this.toolingServersEnvVarsProvisioner = toolingServersEnvVarsProvisioner;
    this.wsAgentServerConfigProvisioner = wsAgentServerConfigProvisioner;
  }

  @Override
  public void provision(
      InternalEnvironment envConfig, DockerEnvironment internalEnv, RuntimeIdentity identity)
      throws InfrastructureException {

    snapshotProvisioner.provision(envConfig, internalEnv, identity);
    installerConfigProvisioner.provision(envConfig, internalEnv, identity);
    projectsVolumeProvisioner.provision(envConfig, internalEnv, identity);
    settingsProvisioners.provision(envConfig, internalEnv, identity);
    labelsProvisioner.provision(envConfig, internalEnv, identity);
    dockerApiEnvProvisioner.provision(envConfig, internalEnv, identity);
    toolingServersEnvVarsProvisioner.provision(envConfig, internalEnv, identity);
    wsAgentServerConfigProvisioner.provision(envConfig, internalEnv, identity);
  }
}
