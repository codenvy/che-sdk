/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.gdb;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.extension.ExtensionGinModule;
import org.eclipse.che.ide.ext.debugger.client.debug.DebuggerServiceClient;
import org.eclipse.che.ide.gdb.client.GdbDebuggerServiceClientImpl;

/** @author Anatolii Bazko */
@ExtensionGinModule
public class GdbGinModule extends AbstractGinModule {
    @Override
    protected void configure() {
        bind(DebuggerServiceClient.class).to(GdbDebuggerServiceClientImpl.class).in(Singleton.class);
    }
}
