/*******************************************************************************
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.api.command;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.command.CommandImpl.ApplicableContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Facade for command management. */
public interface CommandManager {

    /** Returns all commands. */
    List<CommandImpl> getCommands();

    /** Fetches all commands related to the workspace. */
    void fetchCommands();

    /** Returns optional command by the specified name or {@link Optional#empty()} if none. */
    Optional<CommandImpl> getCommand(String name);

    /** Returns commands which are applicable to the current IDE context. */
    List<CommandImpl> getApplicableCommands();

    /** Checks whether the given {@code command} is applicable to the current IDE context or not. */
    boolean isCommandApplicable(CommandImpl command);

    /**
     * Creates new command based on the given data.
     * Command's name and command line will be generated automatically.
     * Command will be bound to the workspace.
     *
     * @param goalId
     *         ID of the goal to which created command should belong
     * @param typeId
     *         ID of the type to which created command should belong
     * @return created command
     */
    Promise<CommandImpl> createCommand(String goalId, String typeId);

    /**
     * Creates new command based on the given data.
     * Command's name and command line will be generated automatically.
     *
     * @param goalId
     *         ID of the goal to which created command should belong
     * @param typeId
     *         ID of the type to which created command should belong
     * @param context
     *         command's {@link ApplicableContext}
     * @return created command
     */
    Promise<CommandImpl> createCommand(String goalId, String typeId, ApplicableContext context);

    /**
     * Creates new command based on the given data. Command will be bound to the workspace.
     *
     * @param goalId
     *         ID of the goal to which created command should belong
     * @param typeId
     *         ID of the type to which created command should belong
     * @param name
     *         command's name.
     *         <strong>Note</strong> that actual name may differ from the given one in order to prevent duplication.
     *         If {@code null}, name will be generated automatically.
     * @param commandLine
     *         actual command line. If {@code null}, command line will be generated by the corresponding command type.
     * @param attributes
     *         command's attributes
     * @return created command
     */
    Promise<CommandImpl> createCommand(String goalId,
                                       String typeId,
                                       @Nullable String name,
                                       @Nullable String commandLine,
                                       Map<String, String> attributes);

    /**
     * Creates new command based on the given data.
     *
     * @param goalId
     *         ID of the goal to which created command should belong
     * @param typeId
     *         ID of the type to which created command should belong
     * @param name
     *         command's name.
     *         <strong>Note</strong> that actual name may differ from the given one in order to prevent duplication.
     *         If {@code null}, name will be generated automatically.
     * @param commandLine
     *         actual command line. If {@code null}, command line will be generated by the corresponding command type.
     * @param attributes
     *         command's attributes
     * @param context
     *         command's {@link ApplicableContext}
     * @return created command
     */
    Promise<CommandImpl> createCommand(String goalId,
                                       String typeId,
                                       @Nullable String name,
                                       @Nullable String commandLine,
                                       Map<String, String> attributes,
                                       ApplicableContext context);

    /**
     * Creates copy of the given {@code command}.
     * <p><b>Note</b> that name of the created command may differ from
     * the given {@code command}'s name in order to prevent name duplication.
     */
    Promise<CommandImpl> createCommand(CommandImpl command);

    /**
     * Updates the command with the specified {@code name} by replacing it with the given {@code command}.
     * <p><b>Note</b> that name of the updated command may differ from the name provided by the given {@code command}
     * in order to prevent name duplication.
     */
    Promise<CommandImpl> updateCommand(String name, CommandImpl command);

    /** Removes command with the specified {@code commandName}. */
    Promise<Void> removeCommand(String commandName);
}
