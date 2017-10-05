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
package org.eclipse.che.ide.jsonrpc;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;
import static org.eclipse.che.api.workspace.shared.Constants.SERVER_WS_AGENT_WEBSOCKET_REFERENCE;
import static org.eclipse.che.ide.api.jsonrpc.Constants.WS_AGENT_JSON_RPC_ENDPOINT_ID;

import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import java.util.Set;
import javax.inject.Singleton;
import org.eclipse.che.api.core.jsonrpc.commons.RequestTransmitter;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.workspace.event.WsAgentServerRunningEvent;
import org.eclipse.che.ide.api.workspace.event.WsAgentServerStoppedEvent;
import org.eclipse.che.ide.api.workspace.model.RuntimeImpl;
import org.eclipse.che.ide.api.workspace.model.WorkspaceImpl;
import org.eclipse.che.ide.bootstrap.BasicIDEInitializedEvent;
import org.eclipse.che.ide.util.loging.Log;

/** Initializes JSON-RPC connection to the ws-agent server. */
@Singleton
public class WsAgentJsonRpcInitializer {

  private final AppContext appContext;
  private final JsonRpcInitializer initializer;
  private final RequestTransmitter requestTransmitter;

  @Inject
  public WsAgentJsonRpcInitializer(
      JsonRpcInitializer initializer,
      AppContext appContext,
      EventBus eventBus,
      RequestTransmitter requestTransmitter) {
    this.appContext = appContext;
    this.initializer = initializer;
    this.requestTransmitter = requestTransmitter;

    eventBus.addHandler(WsAgentServerRunningEvent.TYPE, event -> initializeJsonRpcService());
    eventBus.addHandler(
        WsAgentServerStoppedEvent.TYPE,
        event -> initializer.terminate(WS_AGENT_JSON_RPC_ENDPOINT_ID));

    // in case ws-agent is already running
    eventBus.addHandler(
        BasicIDEInitializedEvent.TYPE,
        event -> {
          if (appContext.getWorkspace().getStatus() == RUNNING) {
            initializeJsonRpcService();
          }
        });
  }

  private void initializeJsonRpcService() {
    Log.debug(WsAgentJsonRpcInitializer.class, "Web socket agent started event caught.");

    try {
      internalInitialize();
    } catch (Exception e) {
      Log.debug(WsAgentJsonRpcInitializer.class, "Failed, will try one more time.");

      new Timer() {
        @Override
        public void run() {
          internalInitialize();
        }
      }.schedule(1_000);
    }
  }

  private void internalInitialize() {
    final WorkspaceImpl workspace = appContext.getWorkspace();
    final RuntimeImpl runtime = workspace.getRuntime();

    if (runtime == null) {
      return; // workspace is stopped
    }

    runtime
        .getDevMachine()
        .ifPresent(
            devMachine -> {
              devMachine
                  .getServerByName(SERVER_WS_AGENT_WEBSOCKET_REFERENCE)
                  .ifPresent(
                      server -> {
                        String separator = server.getUrl().contains("?") ? "&" : "?";
                        String queryParams =
                            appContext
                                .getApplicationId()
                                .map(id -> separator + "clientId=" + id)
                                .orElse("");
                        Set<Runnable> initActions =
                            appContext.getApplicationId().isPresent()
                                ? emptySet()
                                : singleton(this::processWsId);

                        initializer.initialize(
                            WS_AGENT_JSON_RPC_ENDPOINT_ID,
                            singletonMap("url", server.getUrl() + queryParams),
                            initActions);
                      });
            });
  }

  private void processWsId() {
    requestTransmitter
        .newRequest()
        .endpointId(WS_AGENT_JSON_RPC_ENDPOINT_ID)
        .methodName("websocketIdService/getId")
        .noParams()
        .sendAndReceiveResultAsString()
        .onSuccess(appContext::setApplicationWebsocketId);
  }
}
