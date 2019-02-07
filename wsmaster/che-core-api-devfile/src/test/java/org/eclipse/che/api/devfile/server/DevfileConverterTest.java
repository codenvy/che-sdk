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
package org.eclipse.che.api.devfile.server;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import org.eclipse.che.api.devfile.model.Action;
import org.eclipse.che.api.devfile.model.Command;
import org.eclipse.che.api.devfile.model.Devfile;
import org.eclipse.che.api.devfile.model.Project;
import org.eclipse.che.api.devfile.model.Tool;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.commons.json.JsonHelper;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

public class DevfileConverterTest {

  private ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
  private KubernetesToolApplier kubernetesToolApplier = new KubernetesToolApplier();
  private DevfileConverter devfileConverter;

  @BeforeClass
  public void setUp() {
    devfileConverter = new DevfileConverter(kubernetesToolApplier);
  }

  @Test
  public void shouldBuildWorkspaceConfigFromYamlDevFile() throws Exception {
    // given
    String devfileYaml = getTestResource("devfile.yaml");
    String openshiftToolYaml = getTestResource("petclinic.yaml");
    Devfile devFile = objectMapper.readValue(devfileYaml, Devfile.class);

    // when
    WorkspaceConfigImpl wsConfigImpl =
        devfileConverter.devFileToWorkspaceConfig(devFile, local -> openshiftToolYaml);

    // then
    WorkspaceConfigImpl expectedWokrspaceConfig =
        JsonHelper.fromJson(
            getTestResource("workspace_config.json"), WorkspaceConfigImpl.class, null);
    assertEquals(wsConfigImpl, expectedWokrspaceConfig);
  }

  @Test
  public void shouldBuildYamlDevFileFromWorkspaceConfig() throws Exception {
    // given
    String workspaceConfigJson = getTestResource("workspace_config_no_environment.json");
    WorkspaceConfigImpl workspaceConfig =
        JsonHelper.fromJson(workspaceConfigJson, WorkspaceConfigImpl.class, null);

    // when
    Devfile devFile = devfileConverter.workspaceToDevFile(workspaceConfig);

    // then
    String yamlContent = getTestResource("devfile.yaml");
    Devfile expectedDevFile = objectMapper.readValue(yamlContent, Devfile.class);
    // Recursively compare
    assertEquals(devFile.getSpecVersion(), expectedDevFile.getSpecVersion());
    assertEquals(devFile.getName(), expectedDevFile.getName());
    assertEquals(devFile.getProjects().size(), expectedDevFile.getProjects().size());
    for (Project project : devFile.getProjects()) {
      Project expectedProject =
          expectedDevFile
              .getProjects()
              .stream()
              .filter(project1 -> project1.getName().equals(project.getName()))
              .findFirst()
              .get();
      assertEquals(project.getSource().getType(), expectedProject.getSource().getType());
      assertEquals(project.getSource().getLocation(), expectedProject.getSource().getLocation());
    }

    assertEquals(devFile.getCommands().size(), expectedDevFile.getCommands().size());
    for (Command command : devFile.getCommands()) {
      Command expectedCommand =
          expectedDevFile
              .getCommands()
              .stream()
              .filter(command1 -> command1.getName().equals(command.getName()))
              .findAny()
              .get();
      for (Action action : command.getActions()) {
        Action expectedAction =
            expectedCommand
                .getActions()
                .stream()
                .filter(action1 -> action1.getTool().equals(action.getTool()))
                .findAny()
                .get();
        assertEquals(action.getCommand(), expectedAction.getCommand());
        assertEquals(action.getType(), expectedAction.getType());
        assertEquals(action.getWorkdir(), expectedAction.getWorkdir());
      }
      if (command.getAttributes() != null && expectedCommand.getAttributes() != null) {
        assertTrue(
            command
                .getAttributes()
                .entrySet()
                .containsAll(expectedCommand.getAttributes().entrySet()));
      }
    }

    for (Tool tool : devFile.getTools()) {
      Tool expectedTool =
          expectedDevFile
              .getTools()
              .stream()
              .filter(tool1 -> tool1.getName().equals(tool.getName()))
              .findFirst()
              .get();
      assertEquals(tool.getId(), expectedTool.getId());
      assertEquals(tool.getType(), expectedTool.getType());
    }
  }

  @Test(
      expectedExceptions = WorkspaceExportException.class,
      expectedExceptionsMessageRegExp =
          "Workspace .* cannot be converted to devfile since it contains environments which have no equivalent in devfile model")
  public void shouldThrowExceptionWhenWorkspaceHasEnvironments() throws Exception {
    // given
    WorkspaceConfigImpl workspaceConfig = new WorkspaceConfigImpl();
    workspaceConfig.getEnvironments().put("env1", new EnvironmentImpl());

    // when
    devfileConverter.workspaceToDevFile(workspaceConfig);
  }

  private String getTestResource(String resource) throws IOException {
    return Files.readFile(getClass().getClassLoader().getResourceAsStream(resource));
  }
}
