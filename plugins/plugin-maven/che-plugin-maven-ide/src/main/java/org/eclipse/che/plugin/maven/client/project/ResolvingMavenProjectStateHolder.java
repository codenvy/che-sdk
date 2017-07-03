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
package org.eclipse.che.plugin.maven.client.project;

import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.machine.events.WsAgentStateEvent;
import org.eclipse.che.ide.api.machine.events.WsAgentStateHandler;
import org.eclipse.che.ide.project.ResolvingProjectStateHolder;
import org.eclipse.che.plugin.maven.client.MavenJsonRpcHandler;
import org.eclipse.che.plugin.maven.shared.dto.StartStopNotification;

import java.util.HashSet;

import static org.eclipse.che.ide.project.ResolvingProjectStateHolder.ResolvingProjectState.IN_PROGRESS;
import static org.eclipse.che.ide.project.ResolvingProjectStateHolder.ResolvingProjectState.NOT_RESOLVED;
import static org.eclipse.che.ide.project.ResolvingProjectStateHolder.ResolvingProjectState.RESOLVED;
import static org.eclipse.che.plugin.maven.shared.MavenAttributes.MAVEN_ID;

/**
 * Keeper for the state of Resolving Project process.
 * 'Resolving Project process' for a Maven project means reimporting maven model.
 * <ul> Makes it possible to:
 * <li> keep the state of Resolving Project process</li>
 * <li> get the state of Resolving Project process when you need</li>
 * <li> notify the corresponding listener when the state of Resolving Project process has been changed</li>
 * </ul>
 *
 * @author Roman Nikitenko
 */
@Singleton
public class ResolvingMavenProjectStateHolder implements ResolvingProjectStateHolder, WsAgentStateHandler {
    private       ResolvingProjectState                  state;
    private       HashSet<ResolvingProjectStateListener> listeners;

    @Inject
    public ResolvingMavenProjectStateHolder(MavenJsonRpcHandler mavenJsonRpcHandler, EventBus eventBus) {
        this.state = NOT_RESOLVED;
        this.listeners = new HashSet<>();

        eventBus.addHandler(WsAgentStateEvent.TYPE, this);
        mavenJsonRpcHandler.addStartStopHandler(this::handleStartStop);
    }

    @Override
    public ResolvingProjectState getState() {
        return state;
    }

    @Override
    public String getProjectType() {
        return MAVEN_ID;
    }

    @Override
    public void addResolvingProjectStateListener(ResolvingProjectStateListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeResolvingProjectStateListener(ResolvingProjectStateListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void onWsAgentStarted(WsAgentStateEvent event) {
    }

    @Override
    public void onWsAgentStopped(WsAgentStateEvent event) {
    }

    private void handleStartStop(StartStopNotification startStopNotification) {
        if (startStopNotification.isStart()) {
            state = IN_PROGRESS;
        } else {
            state = RESOLVED;
        }
        notifyListenersTimer.cancel();
        notifyListenersTimer.schedule(200);
    }

    /** We need to have some delay to avoid a flashing when a resolving project state has been changed */
    private Timer notifyListenersTimer = new Timer() {
        @Override
        public void run() {
            for (ResolvingProjectStateListener listener : listeners) {
                listener.onResolvingProjectStateChanged(state);
            }
        }
    };

}
