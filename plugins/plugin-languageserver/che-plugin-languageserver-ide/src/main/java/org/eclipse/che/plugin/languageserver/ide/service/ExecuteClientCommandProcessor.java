/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.plugin.languageserver.ide.service;

import static org.eclipse.che.jdt.ls.extension.api.Commands.CLIENT_UPDATE_ON_PROJECT_CLASSPATH_CHANGED;
import static org.eclipse.che.jdt.ls.extension.api.Commands.CLIENT_UPDATE_PROJECT;
import static org.eclipse.che.jdt.ls.extension.api.Commands.CLIENT_UPDATE_PROJECTS_CLASSPATH;
import static org.eclipse.che.jdt.ls.extension.api.Commands.CLIENT_UPDATE_PROJECT_CONFIG;

import com.google.gwt.json.client.JSONString;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import java.util.List;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.project.ProjectServiceClient;
import org.eclipse.che.ide.project.node.ProjectClasspathChangedEvent;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.lsp4j.ExecuteCommandParams;

/**
 * A processor for incoming <code>workspace/ClasspathChanged</code> notifications sent by a language
 * server.
 *
 * @author V. Rubezhny
 */
@Singleton
public class ExecuteClientCommandProcessor {
  private EventBus eventBus;
  private AppContext appContext;
  private final ProjectServiceClient projectService;
  private final PromiseProvider promises;

  @Inject
  public ExecuteClientCommandProcessor(
      EventBus eventBus,
      AppContext appContext,
      ProjectServiceClient projectService,
      PromiseProvider promises) {
    this.eventBus = eventBus;
    this.appContext = appContext;
    this.projectService = projectService;
    this.promises = promises;
  }

  public void execute(ExecuteCommandParams params) {
    switch (params.getCommand()) {
      case CLIENT_UPDATE_PROJECTS_CLASSPATH:
        for (Object project : params.getArguments()) {
          eventBus.fireEvent(new ProjectClasspathChangedEvent(stringValue(project)));
        }
        break;
      case CLIENT_UPDATE_PROJECT:
          updateProject(params.getArguments());
        break;
        case CLIENT_UPDATE_PROJECT_CONFIG:
            updateProjectConfig(stringValue(params.getArguments()));
            break;
        case CLIENT_UPDATE_ON_PROJECT_CLASSPATH_CHANGED:
            for (Object project : params.getArguments()) {
                updateProject(stringValue(project))
                        .then(
                                container -> {
                                    eventBus.fireEvent(
                                            new ProjectClasspathChangedEvent(
                                                    stringValue(container.getLocation().toString())));
                                });
            }
            break;
      default:
        break;
    }
  }

  private void updateProject(List<Object> projects) {
    for (Object project : projects) {
      appContext
          .getWorkspaceRoot()
          .getContainer(stringValue(project))
          .then(
              container -> {
                if (container.isPresent()) {
                  container.get().synchronize();
                }
              });
    }
  private Promise<Container> updateProject(String project) {
    return appContext
        .getWorkspaceRoot()
        .getContainer(project)
        .thenPromise(
            optContainer -> {
              if (optContainer.isPresent()) {
                Container container = optContainer.get();
                container.synchronize();
                return promises.resolve(container);
              }
              return promises.resolve(null);
            });
  }

  private void updateProjectConfig(String project) {
    appContext
        .getWorkspaceRoot()
        .getContainer(project)
        .then(
            container -> {
              projectService
                  .getProject(Path.valueOf(project))
                  .then(
                      projectConfigDto -> {
                        projectService
                            .updateProject(projectConfigDto)
                            .then(
                                arg -> {
                                  if (container.isPresent()) {
                                    container.get().synchronize();
                                  }
                                });
                      });
            });
  }

  private String stringValue(Object value) {
    return value instanceof JSONString
        ? ((JSONString) value).stringValue()
        : (value instanceof List ? stringValue(((List) value).get(0)) : String.valueOf(value));
  }
}
