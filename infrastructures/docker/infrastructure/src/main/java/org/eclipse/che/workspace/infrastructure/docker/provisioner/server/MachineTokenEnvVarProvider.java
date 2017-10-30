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
package org.eclipse.che.workspace.infrastructure.docker.provisioner.server;

import static org.eclipse.che.workspace.infrastructure.docker.DockerMachine.CHE_MACHINE_TOKEN;

import javax.inject.Inject;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.token.MachineTokenProvider;
import org.eclipse.che.commons.lang.Pair;

/**
 * Provides environment variable with a token that should be used by servers in a container to
 * access Che master API.
 *
 * @author Alexander Garagatyi
 * @author Sergii Leshchenko
 */
public class MachineTokenEnvVarProvider implements ServerEnvironmentVariableProvider {
  private final MachineTokenProvider machineTokenProvider;

  @Inject
  public MachineTokenEnvVarProvider(MachineTokenProvider machineTokenProvider) {
    this.machineTokenProvider = machineTokenProvider;
  }

  @Override
  public Pair<String, String> get(RuntimeIdentity runtimeIdentity) {
    try {
      return Pair.of(
          CHE_MACHINE_TOKEN, machineTokenProvider.getToken(runtimeIdentity.getWorkspaceId()));
    } catch (InfrastructureException e) {
      return null;
    }
  }
}
