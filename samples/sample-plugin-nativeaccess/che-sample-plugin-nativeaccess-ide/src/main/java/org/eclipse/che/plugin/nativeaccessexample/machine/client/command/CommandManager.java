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
package org.eclipse.che.plugin.nativeaccessexample.machine.client.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.function.Consumer;
import javax.validation.constraints.NotNull;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.command.exec.ExecAgentCommandManager;
import org.eclipse.che.ide.api.command.exec.ExecAgentConsumer;
import org.eclipse.che.ide.api.command.exec.dto.ProcessStartResponseDto;
import org.eclipse.che.ide.api.workspace.model.MachineImpl;
import org.eclipse.che.ide.api.workspace.model.WorkspaceImpl;
import org.eclipse.che.ide.dto.DtoFactory;

/**
 * Simple command manager which allows to run native commands within the workspace Docker container.
 *
 * @author Mathias Schaefer <mathias.schaefer@eclipsesource.com>
 */
@Singleton
public class CommandManager {

  private final DtoFactory dtoFactory;
  private final ExecAgentCommandManager commandManager;
  private final AppContext appContext;

  @Inject
  public CommandManager(
      DtoFactory dtoFactory, ExecAgentCommandManager commandManager, AppContext appContext) {
    this.dtoFactory = dtoFactory;
    this.commandManager = commandManager;
    this.appContext = appContext;
  }

  /** Execute the the given command command within the workspace Docker container. */
  public void execute(String commandLine) {
    final WorkspaceImpl workspace = appContext.getWorkspace();
    final Optional<MachineImpl> machine = workspace.getDevMachine();

    if (!machine.isPresent()) {
      return;
    }

    String machineID = machine.get().getName();
    final CommandDto command =
        dtoFactory
            .createDto(CommandDto.class)
            .withName("some-command")
            .withCommandLine(commandLine)
            .withType("arbitrary-type");
    executeCommand(command, machineID);
  }

  public void executeCommand(final CommandDto command, @NotNull final String machineID) {
    final ExecAgentConsumer<ProcessStartResponseDto> consumer =
        commandManager.startProcess(machineID, command);

    consumer.then(
        new Consumer<ProcessStartResponseDto>() {
          @Override
          public void accept(ProcessStartResponseDto arg) {
            //Do nothing in this example
          }
        });
  }
}
