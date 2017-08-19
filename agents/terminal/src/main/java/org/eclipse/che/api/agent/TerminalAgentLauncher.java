/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat, Inc. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.api.agent;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.che.api.agent.server.exception.AgentStartException;
import org.eclipse.che.api.agent.server.launcher.AbstractAgentLauncher;
import org.eclipse.che.api.agent.server.launcher.ProcessIsLaunchedChecker;
import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.api.agent.shared.model.impl.AgentImpl;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.server.spi.Instance;

/**
 * Starts terminal agent.
 *
 * @author Garagatyi Alexander
 */
@Singleton
public class TerminalAgentLauncher extends AbstractAgentLauncher {
  private final String runCommand;

  @Inject
  public TerminalAgentLauncher(
      @Named("che.agent.dev.max_start_time_ms") long agentMaxStartTimeMs,
      @Named("che.agent.dev.ping_delay_ms") long agentPingDelayMs,
      @Named("machine.terminal_agent.run_command") String runCommand) {
    super(
        agentMaxStartTimeMs,
        agentPingDelayMs,
        new ProcessIsLaunchedChecker("che-websocket-terminal"));
    this.runCommand = runCommand;
  }

  @Override
  public void launch(Instance machine, Agent agent) throws ServerException, AgentStartException {
    final AgentImpl agentCopy = new AgentImpl(agent);
    agentCopy.setScript(agent.getScript() + "\n" + runCommand);
    super.launch(machine, agentCopy);
  }

  @Override
  public String getMachineType() {
    return "docker";
  }

  @Override
  public String getAgentId() {
    return "org.eclipse.che.terminal";
  }
}
