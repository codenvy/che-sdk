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
package org.eclipse.che.api.devfile.server.convert.component.editor;

import static org.eclipse.che.api.core.model.workspace.config.Command.PLUGIN_ATTRIBUTE;
import static org.eclipse.che.api.devfile.server.Constants.COMPONENT_NAME_COMMAND_ATTRIBUTE;
import static org.eclipse.che.api.devfile.server.Constants.EDITOR_COMPONENT_ALIAS_WORKSPACE_ATTRIBUTE;
import static org.eclipse.che.api.devfile.server.Constants.EDITOR_COMPONENT_TYPE;
import static org.eclipse.che.api.workspace.shared.Constants.WORKSPACE_TOOLING_EDITOR_ATTRIBUTE;
import static org.testng.Assert.assertEquals;

import org.eclipse.che.api.workspace.server.model.impl.CommandImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.devfile.ComponentImpl;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** @author Sergii Leshchenko */
public class EditorComponentToWorkspaceApplierTest {

  private EditorComponentToWorkspaceApplier editorComponentApplier;

  @BeforeMethod
  public void setUp() {
    editorComponentApplier = new EditorComponentToWorkspaceApplier();
  }

  @Test
  public void shouldProvisionWorkspaceEditorAttributeDuringCheEditorComponentApplying()
      throws Exception {
    // given
    WorkspaceConfigImpl workspaceConfig = new WorkspaceConfigImpl();
    ComponentImpl editorComponent = new ComponentImpl();
    editorComponent.setType(EDITOR_COMPONENT_TYPE);
    editorComponent.setName("editor");
    editorComponent.setId("org.eclipse.che.super-editor:0.0.1");

    // when
    editorComponentApplier.apply(workspaceConfig, editorComponent, null);

    // then
    assertEquals(
        workspaceConfig.getAttributes().get(WORKSPACE_TOOLING_EDITOR_ATTRIBUTE),
        "org.eclipse.che.super-editor:0.0.1");
    assertEquals(
        workspaceConfig.getAttributes().get(EDITOR_COMPONENT_ALIAS_WORKSPACE_ATTRIBUTE), "editor");
  }

  @Test
  public void shouldProvisionPluginCommandAttributesDuringCheEditorComponentApplying()
      throws Exception {
    // given
    ComponentImpl superPluginComponent = new ComponentImpl();
    superPluginComponent.setName("editor");
    superPluginComponent.setId("org.eclipse.che.super-editor:0.0.1");
    superPluginComponent.setType(EDITOR_COMPONENT_TYPE);

    WorkspaceConfigImpl workspaceConfig = new WorkspaceConfigImpl();
    CommandImpl command = new CommandImpl();
    command.getAttributes().put(COMPONENT_NAME_COMMAND_ATTRIBUTE, "editor");
    workspaceConfig.getCommands().add(command);

    // when
    editorComponentApplier.apply(workspaceConfig, superPluginComponent, null);

    // then
    assertEquals(
        workspaceConfig.getCommands().get(0).getAttributes().get(PLUGIN_ATTRIBUTE),
        "org.eclipse.che.super-editor:0.0.1");
  }

  @Test
  public void shouldProvisionPluginCommandAttributeWhenIdIsURLToCustomPluginRegistry()
      throws Exception {
    // given
    ComponentImpl superPluginComponent = new ComponentImpl();
    superPluginComponent.setName("editor");
    superPluginComponent.setId(
        "https://custom-plugin.registry/plugins/org.eclipse.che.super-editor:0.0.1");
    superPluginComponent.setType(EDITOR_COMPONENT_TYPE);

    WorkspaceConfigImpl workspaceConfig = new WorkspaceConfigImpl();
    CommandImpl command = new CommandImpl();
    command.getAttributes().put(COMPONENT_NAME_COMMAND_ATTRIBUTE, "editor");
    workspaceConfig.getCommands().add(command);

    // when
    editorComponentApplier.apply(workspaceConfig, superPluginComponent, null);

    // then
    assertEquals(
        workspaceConfig.getCommands().get(0).getAttributes().get(PLUGIN_ATTRIBUTE),
        "org.eclipse.che.super-editor:0.0.1");
  }
}
