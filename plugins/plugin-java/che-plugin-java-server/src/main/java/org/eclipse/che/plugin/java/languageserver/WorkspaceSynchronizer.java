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
package org.eclipse.che.plugin.java.languageserver;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.che.commons.lang.concurrent.LoggingUncaughtExceptionHandler;
import org.eclipse.che.jdt.ls.extension.api.dto.JobResult;
import org.eclipse.che.jdt.ls.extension.api.dto.UpdateWorkspaceParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors projects activity and updates jdt.ls workspace.
 *
 * @author Anatolii Bazko
 */
@Singleton
public class WorkspaceSynchronizer {
  private static final Logger LOG = LoggerFactory.getLogger(WorkspaceSynchronizer.class);

  private final JavaLanguageServerExtensionService service;
  private final ExecutorService executorService;
  private final ProjectsSynchronizer projectsSynchronizer;

  @Inject
  public WorkspaceSynchronizer(
      JavaLanguageServerExtensionService service, ProjectsSynchronizer projectsSynchronizer) {
    this.service = service;
    this.projectsSynchronizer = projectsSynchronizer;
    this.executorService =
        Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                .setNameFormat("WorkspaceUpdater-%d")
                .setUncaughtExceptionHandler(LoggingUncaughtExceptionHandler.getInstance())
                .setDaemon(true)
                .build());
  }

  public void syncronizerWorkspaceAsync(UpdateWorkspaceParameters updateWorkspaceParameters) {
    executorService.submit(
        () -> {
          JobResult jobResult = service.updateWorkspace(updateWorkspaceParameters);

          switch (jobResult.getSeverity()) {
            case ERROR:
              LOG.error(
                  "Workspace updated. Result code: '{}', message: '{}'. Added projects: '{}', removed projects: '{}'",
                  jobResult.getResultCode(),
                  jobResult.getMessage(),
                  updateWorkspaceParameters.getAddedProjectsUri().toString(),
                  updateWorkspaceParameters.getRemovedProjectsUri().toString());
              break;
            case WARNING:
            case CANCEL:
              LOG.warn(
                  "Workspace updated. Result code: '{}', message: '{}'. Added projects: '{}', removed projects: '{}'",
                  jobResult.getResultCode(),
                  jobResult.getMessage(),
                  updateWorkspaceParameters.getAddedProjectsUri().toString(),
                  updateWorkspaceParameters.getRemovedProjectsUri().toString());
              break;
            default:
              LOG.info(
                  "Workspace updated. Result code: '{}', message: '{}'. Added projects: '{}', removed projects: '{}'",
                  jobResult.getResultCode(),
                  jobResult.getMessage(),
                  updateWorkspaceParameters.getAddedProjectsUri().toString(),
                  updateWorkspaceParameters.getRemovedProjectsUri().toString());
              break;
          }

          updateWorkspaceParameters
              .getAddedProjectsUri()
              .forEach(projectsSynchronizer::synchronize);
        });
  }
}
