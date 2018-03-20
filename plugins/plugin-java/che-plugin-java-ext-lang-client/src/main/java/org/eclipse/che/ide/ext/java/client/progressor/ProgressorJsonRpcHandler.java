/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.ext.java.client.progressor;

import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;
import static org.eclipse.che.ide.api.jsonrpc.Constants.WS_AGENT_JSON_RPC_ENDPOINT_ID;
import static org.eclipse.che.ide.ext.java.shared.Constants.PROGRESS_OUTPUT_SUBSCRIBE;
import static org.eclipse.che.ide.ext.java.shared.Constants.PROGRESS_REPORT_METHOD;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.eclipse.che.api.core.jsonrpc.commons.RequestHandlerConfigurator;
import org.eclipse.che.api.core.jsonrpc.commons.RequestTransmitter;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.workspace.event.WorkspaceRunningEvent;
import org.eclipse.che.ide.ext.java.shared.dto.progressor.ProgressReportDto;

/**
 * The mechanism for handling all messages from the jdt.ls server and applying registered consumers.
 */
@Singleton
public class ProgressorJsonRpcHandler {
  private final RequestHandlerConfigurator configurator;
  private final RequestTransmitter requestTransmitter;

  private Set<Consumer<ProgressReportDto>> progressReportConsumers = new HashSet<>();

  @Inject
  public ProgressorJsonRpcHandler(
      RequestHandlerConfigurator configurator,
      AppContext appContext,
      EventBus eventBus,
      RequestTransmitter requestTransmitter) {
    this.configurator = configurator;
    this.requestTransmitter = requestTransmitter;

    handleProgressesReports();
    eventBus.addHandler(WorkspaceRunningEvent.TYPE, e -> subscribe());
    if (appContext.getWorkspace().getStatus() == RUNNING) {
      subscribe();
    }
  }

  private void subscribe() {
    requestTransmitter
        .newRequest()
        .endpointId(WS_AGENT_JSON_RPC_ENDPOINT_ID)
        .methodName(PROGRESS_OUTPUT_SUBSCRIBE)
        .noParams()
        .sendAndSkipResult();
  }

  /**
   * Adds consumer for the event with {@link ProgressReportDto}.
   *
   * @param consumer new consumer
   */
  void addProgressReportHandler(Consumer<ProgressReportDto> consumer) {
    progressReportConsumers.add(consumer);
  }

  private void handleProgressesReports() {
    configurator
        .newConfiguration()
        .methodName(PROGRESS_REPORT_METHOD)
        .paramsAsDto(ProgressReportDto.class)
        .noResult()
        .withConsumer(
            progressNotification ->
                progressReportConsumers.forEach(it -> it.accept(progressNotification)));
  }
}
