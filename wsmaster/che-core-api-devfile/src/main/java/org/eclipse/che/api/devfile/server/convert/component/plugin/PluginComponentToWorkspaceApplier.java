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
package org.eclipse.che.api.devfile.server.convert.component.plugin;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.eclipse.che.api.core.model.workspace.config.Command.PLUGIN_ATTRIBUTE;
import static org.eclipse.che.api.devfile.server.Constants.COMPONENT_NAME_COMMAND_ATTRIBUTE;
import static org.eclipse.che.api.devfile.server.Constants.PLUGINS_COMPONENTS_ALIASES_WORKSPACE_ATTRIBUTE;
import static org.eclipse.che.api.devfile.server.Constants.PLUGIN_COMPONENT_TYPE;
import static org.eclipse.che.api.workspace.shared.Constants.WORKSPACE_TOOLING_PLUGINS_ATTRIBUTE;

import org.eclipse.che.api.core.model.workspace.devfile.Component;
import org.eclipse.che.api.devfile.server.FileContentProvider;
import org.eclipse.che.api.devfile.server.convert.component.ComponentToWorkspaceApplier;
import org.eclipse.che.api.workspace.server.model.impl.CommandImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.commons.annotation.Nullable;

/**
 * Applies changes on workspace config according to the specified plugin component.
 *
 * @author Sergii Leshchenko
 */
public class PluginComponentToWorkspaceApplier implements ComponentToWorkspaceApplier {

  /**
   * Applies changes on workspace config according to the specified plugin component.
   *
   * @param workspaceConfig workspace config on which changes should be applied
   * @param pluginComponent plugin component that should be applied
   * @param contentProvider optional content provider that may be used for external component
   *     resource fetching
   * @throws IllegalArgumentException if specified workspace config or plugin component is null
   * @throws IllegalArgumentException if specified component has type different from chePlugin
   */
  @Override
  public void apply(
      WorkspaceConfigImpl workspaceConfig,
      Component pluginComponent,
      @Nullable FileContentProvider contentProvider) {
    checkArgument(workspaceConfig != null, "Workspace config must not be null");
    checkArgument(pluginComponent != null, "Component must not be null");
    checkArgument(
        PLUGIN_COMPONENT_TYPE.equals(pluginComponent.getType()),
        format("Plugin must have `%s` type", PLUGIN_COMPONENT_TYPE));

    String workspacePluginsAttribute =
        workspaceConfig.getAttributes().get(WORKSPACE_TOOLING_PLUGINS_ATTRIBUTE);
    workspaceConfig
        .getAttributes()
        .put(
            WORKSPACE_TOOLING_PLUGINS_ATTRIBUTE,
            append(workspacePluginsAttribute, pluginComponent.getId()));

    String pluginsAliases =
        workspaceConfig.getAttributes().get(PLUGINS_COMPONENTS_ALIASES_WORKSPACE_ATTRIBUTE);
    workspaceConfig
        .getAttributes()
        .put(
            PLUGINS_COMPONENTS_ALIASES_WORKSPACE_ATTRIBUTE,
            append(pluginsAliases, pluginComponent.getId() + "=" + pluginComponent.getName()));

    String pluginIdVersion = resolveIdAndVersion(pluginComponent.getId());
    for (CommandImpl command : workspaceConfig.getCommands()) {
      String commandComponent = command.getAttributes().get(COMPONENT_NAME_COMMAND_ATTRIBUTE);

      if (commandComponent == null) {
        // command does not have component information
        continue;
      }

      if (!commandComponent.equals(pluginComponent.getName())) {
        continue;
      }

      command.getAttributes().put(PLUGIN_ATTRIBUTE, pluginIdVersion);
    }
  }

  private String resolveIdAndVersion(String ref) {
    int lastSlashPosition = ref.lastIndexOf("/");
    if (lastSlashPosition < 0) {
      return ref;
    } else {
      return ref.substring(lastSlashPosition + 1);
    }
  }

  private String append(String source, String toAppend) {
    if (isNullOrEmpty(source)) {
      return toAppend;
    } else {
      return source + "," + toAppend;
    }
  }
}
