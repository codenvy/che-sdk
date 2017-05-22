/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.api.workspace;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.WsAgentHealthStateDto;

import java.util.List;
import java.util.Map;

/**
 * GWT Client for Workspace Service.
 *
 * @author Yevhenii Voevodin
 * @author Igor Vinokur
 */
public interface WorkspaceServiceClient {

    /**
     * Creates new workspace.
     *
     * @param newWorkspace
     *         the configuration to create the new workspace
     * @param account
     *         the account id related to this operation
     * @return a promise that resolves to the {@link WorkspaceDto}, or rejects with an error
     * @see WorkspaceService#create(WorkspaceConfigDto, List, Boolean, String)
     */
    Promise<WorkspaceDto> create(WorkspaceConfigDto newWorkspace, String account);

    /**
     * Gets users workspace by key.
     *
     * @param key
     *         composite key can be just workspace ID or in the namespace/workspace_name form
     * @return a promise that resolves to the {@link WorkspaceDto}, or rejects with an error
     * @see WorkspaceService#getByKey(String)
     */
    Promise<WorkspaceDto> getWorkspace(String key);

    /**
     * Gets workspace by namespace and name
     *
     * @param namespace
     *         namespace
     * @param workspaceName
     *         workspace name
     * @return a promise that resolves to the {@link WorkspaceDto}, or rejects with an error
     * @see WorkspaceService#getByKey(String)
     */
    Promise<WorkspaceDto> getWorkspace(String namespace, String workspaceName);

    /**
     * Gets all workspaces of current user.
     *
     * @param skip
     *         the number of the items to skip
     * @param limit
     *         the limit of the items in the response, default is 30
     * @return a promise that will provide a list of {@link WorkspaceDto}, or rejects with an error
     * @see #getWorkspaces(int, int)
     */
    Promise<List<WorkspaceDto>> getWorkspaces(int skip, int limit);

    /**
     * Starts workspace based on workspace id and environment.
     *
     * @param id
     *         workspace ID
     * @param envName
     *         the name of the workspace environment that should be used for start
     * @param restore
     *         if <code>true</code> workspace will be restored from snapshot if snapshot exists,
     *         if <code>false</code> workspace will not be restored from snapshot
     *         even if auto-restore is enabled and snapshot exists
     * @return a promise that resolves to the {@link WorkspaceDto}, or rejects with an error
     */
    Promise<WorkspaceDto> startById(String id, String envName, Boolean restore);

    /**
     * Stops running workspace.
     *
     * @param wsId
     *         workspace ID
     * @return a promise that will resolve when the workspace has been stopped, or rejects with an error
     * @see WorkspaceService#stop(String, Boolean)
     */
    Promise<Void> stop(String wsId);

    /**
     * Stops currently run runtime with ability to create snapshot.
     *
     * @param wsId
     *         workspace ID
     * @param createSnapshot
     *         create snapshot during the stop operation
     * @return a promise that will resolve when the workspace has been stopped, or rejects with an error
     */
    Promise<Void> stop(String wsId, boolean createSnapshot);

    /**
     * Get all commands from the specified workspace.
     *
     * @param wsId
     *         workspace ID
     * @return a promise that will provide a list of {@link CommandDto}s, or rejects with an error
     */
    Promise<List<CommandDto>> getCommands(String wsId);

    /**
     * Adds command to workspace
     *
     * @param wsId
     *         workspace ID
     * @param newCommand
     *         the new workspace command
     * @return a promise that resolves to the {@link WorkspaceDto}, or rejects with an error
     * @see WorkspaceService#addCommand(String, CommandDto)
     */
    Promise<WorkspaceDto> addCommand(String wsId, CommandDto newCommand);

    /**
     * Updates command.
     *
     * @return a promise that resolves to the {@link WorkspaceDto}, or rejects with an error
     * @see WorkspaceService#updateCommand(String, String, CommandDto)
     */
    Promise<WorkspaceDto> updateCommand(String wsId, String commandName, CommandDto commandUpdate);

    /**
     * Removes command from workspace.
     *
     * @param wsId
     *         workspace ID
     * @param commandName
     *         the name of the command to remove
     * @return a promise that resolves to the {@link WorkspaceDto}, or rejects with an error
     * @see WorkspaceService#deleteCommand(String, String)
     */
    Promise<WorkspaceDto> deleteCommand(String wsId, String commandName);

    /**
     * Gets state of the workspace agent.
     *
     * @param workspaceId
     *         workspace ID
     * @return a promise that will resolve when the snapshot has been created, or rejects with an error
     * @see WorkspaceService#checkAgentHealth(String)
     */
    Promise<WsAgentHealthStateDto> getWsAgentState(String workspaceId, String devMachineName);

    /**
     * Get workspace related server configuration values defined in che.properties
     *
     * @see WorkspaceService#getSettings()
     */
    Promise<Map<String, String>> getSettings();

}
