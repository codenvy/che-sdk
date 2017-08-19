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
package org.eclipse.che.ide.workspace.events;

import static java.util.Collections.singletonMap;
import static org.eclipse.che.api.workspace.shared.Constants.INSTALLER_LOG_METHOD;
import static org.eclipse.che.api.workspace.shared.Constants.MACHINE_LOG_METHOD;
import static org.eclipse.che.api.workspace.shared.Constants.MACHINE_STATUS_CHANGED_METHOD;
import static org.eclipse.che.api.workspace.shared.Constants.SERVER_STATUS_CHANGED_METHOD;
import static org.eclipse.che.api.workspace.shared.Constants.WORKSPACE_STATUS_CHANGED_METHOD;
import static org.eclipse.che.ide.api.jsonrpc.Constants.WS_MASTER_JSON_RPC_ENDPOINT_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import java.util.Map;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.jsonrpc.SubscriptionManagerClient;
import org.eclipse.che.ide.api.workspace.event.WorkspaceStoppedEvent;

/**
 * Unsubscribes from receiving all JSON-RPC notifications from workspace master when workspace is
 * stopped.
 */
@Singleton
class WorkspaceEventsUnsubscriber {

  @Inject
  WorkspaceEventsUnsubscriber(
      EventBus eventBus,
      AppContext appContext,
      SubscriptionManagerClient subscriptionManagerClient) {
    eventBus.addHandler(
        WorkspaceStoppedEvent.TYPE,
        e -> {
          Map<String, String> scope = singletonMap("workspaceId", appContext.getWorkspaceId());

          subscriptionManagerClient.unSubscribe(
              WS_MASTER_JSON_RPC_ENDPOINT_ID, WORKSPACE_STATUS_CHANGED_METHOD, scope);
          subscriptionManagerClient.unSubscribe(
              WS_MASTER_JSON_RPC_ENDPOINT_ID, MACHINE_STATUS_CHANGED_METHOD, scope);
          subscriptionManagerClient.unSubscribe(
              WS_MASTER_JSON_RPC_ENDPOINT_ID, SERVER_STATUS_CHANGED_METHOD, scope);
          subscriptionManagerClient.unSubscribe(
              WS_MASTER_JSON_RPC_ENDPOINT_ID, MACHINE_LOG_METHOD, scope);
          subscriptionManagerClient.unSubscribe(
              WS_MASTER_JSON_RPC_ENDPOINT_ID, INSTALLER_LOG_METHOD, scope);
        });
  }
}
