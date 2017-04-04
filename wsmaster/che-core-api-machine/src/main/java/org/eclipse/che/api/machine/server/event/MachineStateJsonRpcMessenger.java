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
package org.eclipse.che.api.machine.server.event;

import org.eclipse.che.api.core.jsonrpc.RequestHandlerConfigurator;
import org.eclipse.che.api.core.jsonrpc.RequestTransmitter;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.machine.shared.dto.event.MachineProcessEvent;
import org.eclipse.che.api.machine.shared.dto.event.MachineStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.collect.Sets.newConcurrentHashSet;

@Singleton
public class MachineStateJsonRpcMessenger implements EventSubscriber<MachineStatusEvent> {
    private final RequestTransmitter transmitter;
    private final EventService       eventService;

    private final Map<String, Set<String>> endpointIds = new ConcurrentHashMap<>();

    @Inject
    public MachineStateJsonRpcMessenger(RequestTransmitter transmitter, EventService eventService) {
        this.transmitter = transmitter;
        this.eventService = eventService;
    }

    @Override
    public void onEvent(MachineStatusEvent event) {
        String id = event.getWorkspaceId();
        endpointIds.entrySet()
                   .stream()
                   .filter(it -> it.getValue().contains(id))
                   .map(Map.Entry::getKey)
                   .forEach(it -> transmitter.transmitOneToNone(it, "event:environment-status:changed", event));
    }

    @Inject
    private void configureSubscribeHandler(RequestHandlerConfigurator configurator) {

        configurator.newConfiguration()
                    .methodName("event:environment-status:subscribe")
                    .paramsAsString()
                    .noResult()
                    .withConsumer((endpointId, workspaceId) -> {
                        endpointIds.putIfAbsent(endpointId, newConcurrentHashSet());
                        endpointIds.get(endpointId).add(workspaceId);
                    });
    }

    @Inject
    private void configureUnSubscribeHandler(RequestHandlerConfigurator configurator) {
        configurator.newConfiguration()
                    .methodName("event:environment-status:un-subscribe")
                    .paramsAsString()
                    .noResult()
                    .withConsumer((endpointId, workspaceId) -> {
                        Set<String> workspaceIds = endpointIds.get(endpointId);
                        if (workspaceIds != null) {
                            workspaceIds.remove(workspaceId);

                            if (workspaceIds.isEmpty()) {
                                endpointIds.remove(endpointId);
                            }
                        }
                    });
    }

    @PostConstruct
    private void subscribe() {
        eventService.subscribe(this);
    }

    @PreDestroy
    private void unsubscribe() {
        eventService.unsubscribe(this);
    }
}
