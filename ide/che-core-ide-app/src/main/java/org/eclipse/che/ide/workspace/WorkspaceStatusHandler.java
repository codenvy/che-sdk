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
package org.eclipse.che.ide.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.actions.WorkspaceSnapshotNotifier;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.machine.WsAgentStateController;
import org.eclipse.che.ide.api.machine.WsAgentURLModifier;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.workspace.event.WorkspaceStartedEvent;
import org.eclipse.che.ide.api.workspace.event.WorkspaceStartingEvent;
import org.eclipse.che.ide.api.workspace.event.WorkspaceStoppedEvent;
import org.eclipse.che.ide.context.AppContextImpl;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.ui.loaders.LoaderPresenter;
import org.eclipse.che.ide.workspace.start.StartWorkspaceNotification;

import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STARTING;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STOPPED;
import static org.eclipse.che.ide.api.notification.StatusNotification.DisplayMode.FLOAT_MODE;
import static org.eclipse.che.ide.api.notification.StatusNotification.Status.FAIL;
import static org.eclipse.che.ide.ui.loaders.LoaderPresenter.Phase.CREATING_WORKSPACE_SNAPSHOT;
import static org.eclipse.che.ide.ui.loaders.LoaderPresenter.Phase.STARTING_WORKSPACE_RUNTIME;
import static org.eclipse.che.ide.ui.loaders.LoaderPresenter.Phase.STOPPING_WORKSPACE;

/**
 * Handles changes of the workspace status and fires
 * the corresponded events to notify all interested subscribers.
 */
@Singleton
public class WorkspaceStatusHandler {

    private final WorkspaceServiceClient     workspaceServiceClient;
    private final AppContext                 appContext;
    private final StartWorkspaceNotification startWorkspaceNotificationProvider;
    private final NotificationManager        notificationManagerProvider;
    private final WorkspaceSnapshotNotifier  snapshotNotifierProvider;
    private final LoaderPresenter            wsStatusNotification;
    private final WsAgentStateController     wsAgentStateController;
    private final WsAgentURLModifier         wsAgentURLModifier;
    private final EventBus                   eventBus;
    private final CoreLocalizationConstant   messages;

    @Inject
    WorkspaceStatusHandler(WorkspaceServiceClient workspaceServiceClient,
                           AppContext appContext,
                           StartWorkspaceNotification startWorkspaceNotification,
                           NotificationManager notificationManager,
                           WorkspaceSnapshotNotifier snapshotNotifier,
                           LoaderPresenter wsStatusNotification,
                           WsAgentStateController wsAgentStateController,
                           WsAgentURLModifier wsAgentURLModifier,
                           EventBus eventBus,
                           CoreLocalizationConstant messages) {
        this.workspaceServiceClient = workspaceServiceClient;
        this.appContext = appContext;
        this.startWorkspaceNotificationProvider = startWorkspaceNotification;
        this.notificationManagerProvider = notificationManager;
        this.snapshotNotifierProvider = snapshotNotifier;
        this.wsStatusNotification = wsStatusNotification;
        this.wsAgentStateController = wsAgentStateController;
        this.wsAgentURLModifier = wsAgentURLModifier;
        this.eventBus = eventBus;
        this.messages = messages;
    }

    public void handleWorkspaceStatusChanged() {
        handleWorkspaceStatusChanged(null);
    }

    public void handleWorkspaceStatusChanged(@Nullable WorkspaceStatusEvent serverEvent) {
        workspaceServiceClient.getWorkspace(appContext.getWorkspaceId()).then(workspace -> {
            ((AppContextImpl)appContext).setWorkspace(workspace);

            // FIXME: spi
            // should be set on server `ws-agent` has been started
            ((AppContextImpl)appContext).setProjectsRoot(Path.valueOf("/projects"));

            if (workspace.getStatus() == RUNNING) {
                wsStatusNotification.setSuccess(STARTING_WORKSPACE_RUNTIME);
                wsAgentStateController.initialize(appContext.getDevMachine());
                wsAgentURLModifier.initialize(appContext.getDevMachine());

                eventBus.fireEvent(new WorkspaceStartedEvent(workspace));
            } else if (workspace.getStatus() == STARTING) {
                eventBus.fireEvent(new WorkspaceStartingEvent(workspace));
            } else if (workspace.getStatus() == STOPPED) {
                eventBus.fireEvent(new WorkspaceStoppedEvent(workspace));
            }

            if (serverEvent != null) {
                notify(serverEvent);
            }
        });
    }

    // TODO: move to the separate component that should listen appropriate events
    private void notify(WorkspaceStatusEvent event) {
        switch (event.getEventType()) {
            case STARTING:
                wsStatusNotification.setSuccess(STARTING_WORKSPACE_RUNTIME);
                break;
            case RUNNING:
                startWorkspaceNotificationProvider.hide();
                wsStatusNotification.setSuccess(STARTING_WORKSPACE_RUNTIME);
                break;
            case STOPPING:
                wsStatusNotification.show(STOPPING_WORKSPACE);
                break;
            case STOPPED:
                wsStatusNotification.setSuccess(STOPPING_WORKSPACE);
                startWorkspaceNotificationProvider.show();
                break;
            case ERROR:
                notificationManagerProvider.notify(messages.workspaceStartFailed(), FAIL, FLOAT_MODE);
                startWorkspaceNotificationProvider.show();
                break;
            case SNAPSHOT_CREATING:
                wsStatusNotification.show(CREATING_WORKSPACE_SNAPSHOT);
                snapshotNotifierProvider.creationStarted();
                break;
            case SNAPSHOT_CREATED:
                wsStatusNotification.setSuccess(CREATING_WORKSPACE_SNAPSHOT);
                snapshotNotifierProvider.successfullyCreated();
                break;
            case SNAPSHOT_CREATION_ERROR:
                wsStatusNotification.setError(CREATING_WORKSPACE_SNAPSHOT);
                snapshotNotifierProvider.creationError("Snapshot creation error: " + event.getError());
                break;
            default:
                break;
        }
    }
}
