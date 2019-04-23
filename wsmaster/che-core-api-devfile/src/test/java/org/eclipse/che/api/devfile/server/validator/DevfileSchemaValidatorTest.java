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
package org.eclipse.che.api.devfile.server.validator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.IOException;
import org.eclipse.che.api.devfile.server.exception.DevfileFormatException;
import org.eclipse.che.api.devfile.server.schema.DevfileSchemaProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

public class DevfileSchemaValidatorTest {

  private DevfileSchemaValidator schemaValidator;

  @BeforeClass
  public void setUp() {
    schemaValidator = new DevfileSchemaValidator(new DevfileSchemaProvider());
  }

  @Test(dataProvider = "validDevfiles")
  public void shouldNotThrowExceptionOnValidationOfValidDevfile(String resourceFilePath)
      throws Exception {
    schemaValidator.validateBySchema(getResource(resourceFilePath));
  }

  @DataProvider
  public Object[][] validDevfiles() {
    return new Object[][] {
      {"editor_plugin_component/devfile_editor_plugins.yaml"},
      {"kubernetes_openshift_component/devfile_kubernetes_component_reference.yaml"},
      {"kubernetes_openshift_component/devfile_kubernetes_component_absolute_reference.yaml"},
      {"component/devfile_without_any_component.yaml"},
      {
        "kubernetes_openshift_component/devfile_kubernetes_component_reference_and_content_as_block.yaml"
      },
      {"kubernetes_openshift_component/devfile_openshift_component.yaml"},
      {"kubernetes_openshift_component/devfile_openshift_component_reference_and_content.yaml"},
      {
        "kubernetes_openshift_component/devfile_openshift_component_reference_and_content_as_block.yaml"
      },
      {"kubernetes_openshift_component/devfile_openshift_component_content_without_reference.yaml"},
      {
        "kubernetes_openshift_component/devfile_kubernetes_component_content_without_reference.yaml"
      },
      {"dockerimage_component/devfile_dockerimage_component.yaml"},
      {"dockerimage_component/devfile_dockerimage_component_without_entry_point.yaml"},
      {"editor_plugin_component/devfile_editor_component_with_custom_registry.yaml"},
      {"editor_plugin_component/devfile_editor_plugins_components_with_memory_limit.yaml"}
    };
  }

  @Test(dataProvider = "invalidDevfiles")
  public void shouldThrowExceptionOnValidationOfNonValidDevfile(
      String resourceFilePath, String expectedMessageRegexp) throws Exception {
    try {
      schemaValidator.validateBySchema(getResource(resourceFilePath));
    } catch (DevfileFormatException e) {
      assertEquals(
          e.getMessage(),
          expectedMessageRegexp,
          "DevfileFormatException thrown with message that doesn't match expected pattern:");
      return;
    }
    fail("DevfileFormatException expected to be thrown but is was not");
  }

  @DataProvider
  public Object[][] invalidDevfiles() {
    return new Object[][] {
      // Devfile model testing
      {
        "devfile/devfile_missing_name.yaml",
        "Devfile schema validation failed. Error: /devfile object has missing required properties ([\"name\"])"
      },
      {
        "devfile/devfile_missing_spec_version.yaml",
        "Devfile schema validation failed. Error: /devfile object has missing required properties ([\"specVersion\"])"
      },
      {
        "devfile/devfile_with_undeclared_field.yaml",
        "Devfile schema validation failed. Error: /devfile object instance has properties which are not allowed by the schema: [\"unknown\"]"
      },
      // component model testing
      {
        "component/devfile_missing_component_type.yaml",
        "Devfile schema validation failed. Error: /devfile/components/0 object has missing required properties ([\"type\"])"
      },
      {
        "component/devfile_component_with_undeclared_field.yaml",
        "Devfile schema validation failed. Errors: [/devfile/components/0 object instance has properties which are not allowed by the schema: "
            + "[\"unknown\"],instance failed to match exactly one schema (matched 0 out of 3),"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"unknown\"],"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"id\",\"unknown\"],"
            + "instance failed to match at least one required schema among 2,"
            + "/devfile/components/0 object has missing required properties ([\"reference\"]),"
            + "/devfile/components/0 object has missing required properties ([\"referenceContent\"]),"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"id\",\"unknown\"],"
            + "/devfile/components/0 object has missing required properties ([\"image\",\"memoryLimit\"])]"
      },
      // Command model testing
      {
        "command/devfile_missing_command_name.yaml",
        "Devfile schema validation failed. Error: /devfile/commands/0 object has missing required properties ([\"name\"])"
      },
      {
        "command/devfile_missing_command_actions.yaml",
        "Devfile schema validation failed. Error: /devfile/commands/0 object has missing required properties ([\"actions\"])"
      },
      {
        "command/devfile_multiple_commands_actions.yaml",
        "Devfile schema validation failed. Error: /devfile/commands/0/actions array is too long: must have at most 1 elements but instance has 2 elements"
      },
      // cheEditor/chePlugin component model testing
      {
        "editor_plugin_component/devfile_editor_component_with_missing_id.yaml",
        "Devfile schema validation failed. Errors: [instance failed to match exactly one schema (matched 0 out of 3),"
            + "/devfile/components/0 object has missing required properties ([\"id\"]),"
            + "instance failed to match at least one required schema among 2,"
            + "/devfile/components/0 object has missing required properties ([\"reference\"]),"
            + "/devfile/components/0 object has missing required properties ([\"referenceContent\"]),"
            + "/devfile/components/0 object has missing required properties ([\"image\",\"memoryLimit\"])]"
      },
      {
        "editor_plugin_component/devfile_editor_component_with_indistinctive_field_reference.yaml",
        "Devfile schema validation failed. Errors: [instance failed to match exactly one schema (matched 0 out of 3),"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"reference\"],"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"id\"],"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"id\",\"reference\"],"
            + "/devfile/components/0 object has missing required properties ([\"image\",\"memoryLimit\"])]"
      },
      {
        "editor_plugin_component/devfile_editor_component_without_version.yaml",
        "Devfile schema validation failed. Error: "
            + "/devfile/components/0/id ECMA 262 regex \"^((https?://)[a-zA-Z0-9_\\-\\./]+)?[a-zA-Z0-9_\\-\\.]{1,}:[a-zA-Z0-9_\\-\\.]{1,}$\" does not match input string \"org.eclipse.theia\""
      },
      {
        "editor_plugin_component/devfile_editor_plugins_components_with_invalid_memory_limit.yaml",
        "Devfile schema validation failed. Error: /devfile/components/0/memoryLimit instance type (integer) does not match any allowed primitive type (allowed: [\"string\"])"
      },
      {
        "editor_plugin_component/devfile_editor_component_with_multiple_colons_in_id.yaml",
        "Devfile schema validation failed. Error: "
            + "/devfile/components/0/id ECMA 262 regex \"^((https?://)[a-zA-Z0-9_\\-\\./]+)?[a-zA-Z0-9_\\-\\.]{1,}:[a-zA-Z0-9_\\-\\.]{1,}$\" does not match input string \"org.eclipse.theia:dev:v1\""
      },
      // kubernetes/openshift component model testing
      {
        "kubernetes_openshift_component/devfile_openshift_component_with_missing_reference_and_referenceContent.yaml",
        "Devfile schema validation failed. Errors: [instance failed to match exactly one schema (matched 0 out of 3),"
            + "/devfile/components/0 object has missing required properties ([\"id\"]),"
            + "instance failed to match at least one required schema among 2,"
            + "/devfile/components/0 object has missing required properties ([\"reference\"]),"
            + "/devfile/components/0 object has missing required properties ([\"referenceContent\"]),"
            + "/devfile/components/0 object has missing required properties ([\"image\",\"memoryLimit\"])]"
      },
      {
        "kubernetes_openshift_component/devfile_openshift_component_with_indistinctive_field_id.yaml",
        "Devfile schema validation failed. Errors: [instance failed to match exactly one schema (matched 0 out of 3)"
            + ",/devfile/components/0 object instance has properties which are not allowed by the schema: [\"reference\",\"selector\"],"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"id\"],"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"id\",\"reference\",\"selector\"],"
            + "/devfile/components/0 object has missing required properties ([\"image\",\"memoryLimit\"])]"
      },
      // Dockerimage component model testing
      {
        "dockerimage_component/devfile_dockerimage_component_with_missing_image.yaml",
        "Devfile schema validation failed. Errors: [instance failed to match exactly one schema (matched 0 out of 3),"
            + "/devfile/components/0 object has missing required properties ([\"id\"]),"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"memoryLimit\"],"
            + "instance failed to match at least one required schema among 2,"
            + "/devfile/components/0 object has missing required properties ([\"reference\"]),"
            + "/devfile/components/0 object has missing required properties ([\"referenceContent\"]),"
            + "/devfile/components/0 object has missing required properties ([\"image\"])]"
      },
      {
        "dockerimage_component/devfile_dockerimage_component_with_missing_memory_limit.yaml",
        "Devfile schema validation failed. Errors: [instance failed to match exactly one schema (matched 0 out of 3),"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"image\"],"
            + "/devfile/components/0 object has missing required properties ([\"id\"]),"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"image\"],"
            + "instance failed to match at least one required schema among 2,"
            + "/devfile/components/0 object has missing required properties ([\"reference\"]),"
            + "/devfile/components/0 object has missing required properties ([\"referenceContent\"]),"
            + "/devfile/components/0 object has missing required properties ([\"memoryLimit\"])]"
      },
      {
        "dockerimage_component/devfile_dockerimage_component_with_indistinctive_field_selector.yaml",
        "Devfile schema validation failed. Errors: [instance failed to match exactly one schema (matched 0 out of 3),"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"endpoints\",\"env\",\"image\",\"selector\",\"volumes\"],"
            + "/devfile/components/0 object has missing required properties ([\"id\"]),"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"endpoints\",\"env\",\"image\",\"memoryLimit\",\"volumes\"],"
            + "instance failed to match at least one required schema among 2,"
            + "/devfile/components/0 object has missing required properties ([\"reference\"]),"
            + "/devfile/components/0 object has missing required properties ([\"referenceContent\"]),"
            + "/devfile/components/0 object instance has properties which are not allowed by the schema: [\"selector\"]]"
      },
    };
  }

  private String getResource(String name) throws IOException {
    return Files.readFile(getClass().getClassLoader().getResourceAsStream("schema_test/" + name));
  }
}
