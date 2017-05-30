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
package org.eclipse.che.ide.api.machine.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import org.eclipse.che.ide.api.workspace.model.MachineImpl;

public class ProcessStartedEvent extends GwtEvent<ProcessStartedEvent.Handler> {

    public static final Type<ProcessStartedEvent.Handler> TYPE = new Type<>();

    private final int         processID;
    private final MachineImpl machine;

    public ProcessStartedEvent(int processID, MachineImpl machine) {
        this.processID = processID;
        this.machine = machine;
    }

    public int getProcessID() {
        return processID;
    }

    public MachineImpl getMachine() {
        return machine;
    }

    @Override
    public Type<ProcessStartedEvent.Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onProcessStarted(this);
    }

    public interface Handler extends EventHandler {

        void onProcessStarted(ProcessStartedEvent event);
    }
}
