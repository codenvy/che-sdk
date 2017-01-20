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
package org.eclipse.che.api.agent;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import org.eclipse.che.api.agent.server.launcher.AgentLauncher;
import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.inject.DynaModule;

/**
 * @author Anatolii Bazko
 */
@DynaModule
public class SshAgentModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<Agent> agents = Multibinder.newSetBinder(binder(), Agent.class);
        agents.addBinding().to(SshAgent.class);

        Multibinder<AgentLauncher> launchers = Multibinder.newSetBinder(binder(), AgentLauncher.class);
        launchers.addBinding().to(org.eclipse.che.api.agent.SshAgentLauncher.class);
    }
}
