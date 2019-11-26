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
package org.eclipse.che.api.workspace.server.devfile;

import static org.testng.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.che.api.workspace.server.devfile.exception.DevfileFormatException;
import org.testng.annotations.Test;

public class OverridePropertiesApplierTest {

  private final OverridePropertiesApplier applier = new OverridePropertiesApplier();
  private final ObjectMapper jsonMapper = new ObjectMapper();

  @Test
  public void shouldOverrideExistingPropertiesInDevfile() throws Exception {
    String json =
        "{"
            + "\"apiVersion\": \"1.0.0\","
            + "\"metadata\": {"
            + "   \"generateName\": \"python\""
            + "  }"
            + "}";
    Map<String, String> overrides = new HashMap<>();
    overrides.put("apiVersion", "2.0.0");
    overrides.put("metadata.generateName", "go");
    // when
    JsonNode result = applier.applyPropertiesOverride(jsonMapper.readTree(json), overrides);
    // then
    assertEquals(result.get("apiVersion").textValue(), "2.0.0");
    assertEquals(result.get("metadata").get("generateName").textValue(), "go");
  }

  @Test
  public void shouldCreateUnExistingOverridePropertiesInDevfile() throws Exception {
    String json = "{" + "\"apiVersion\": \"1.0.0\"" + "}";
    Map<String, String> overrides = new HashMap<>();
    overrides.put("metadata.generateName", "go");
    // when
    JsonNode result = applier.applyPropertiesOverride(jsonMapper.readTree(json), overrides);
    assertEquals(result.get("metadata").get("generateName").textValue(), "go");
  }

  @Test
  public void shouldRewriteValueInProjectsArrayElementByName() throws Exception {
    String json =
        "{"
            + "\"apiVersion\": \"1.0.0\","
            + "\"projects\": ["
            + "   {"
            + "      \"name\": \"test1\","
            + "      \"clonePath\": \"/foo/bar1\""
            + "   },"
            + "   {"
            + "      \"name\": \"test2\","
            + "      \"clonePath\": \"/foo/bar2\""
            + "   }"
            + "  ]"
            + "}";
    Map<String, String> overrides = new HashMap<>();
    overrides.put("projects.test1.clonePath", "baz1");
    overrides.put("projects.test2.clonePath", "baz2");

    // when
    JsonNode result = applier.applyPropertiesOverride(jsonMapper.readTree(json), overrides);
    // then
    assertEquals(result.get("projects").get(0).get("clonePath").textValue(), "baz1");
    assertEquals(result.get("projects").get(1).get("clonePath").textValue(), "baz2");
  }

  @Test(
      expectedExceptions = DevfileFormatException.class,
      expectedExceptionsMessageRegExp = "Object with name 'test3' not found in array of projects.")
  public void shouldThrowExceptionIfOverrideArrayObjectNotFoundByName() throws Exception {
    String json =
        "{"
            + "\"apiVersion\": \"1.0.0\","
            + "\"projects\": ["
            + "   {"
            + "      \"name\": \"test1\","
            + "      \"clonePath\": \"/foo/bar1\""
            + "   },"
            + "   {"
            + "      \"name\": \"test2\","
            + "      \"clonePath\": \"/foo/bar2\""
            + "   }"
            + "  ]"
            + "}";
    Map<String, String> overrides = new HashMap<>();
    overrides.put("projects.test3.clonePath", "baz1");
    // when
    applier.applyPropertiesOverride(jsonMapper.readTree(json), overrides);
  }

  @Test(
      expectedExceptions = DevfileFormatException.class,
      expectedExceptionsMessageRegExp =
          "Override property reference 'projects' points to an array type object. Please add an item qualifier by name.")
  public void shouldThrowExceptionIfOverrideReferenceEndsWithArray() throws Exception {
    String json =
        "{"
            + "\"apiVersion\": \"1.0.0\","
            + "\"projects\": ["
            + "   {"
            + "      \"name\": \"test1\","
            + "      \"clonePath\": \"/foo/bar1\""
            + "   },"
            + "   {"
            + "      \"name\": \"test2\","
            + "      \"clonePath\": \"/foo/bar2\""
            + "   }"
            + "  ]"
            + "}";
    Map<String, String> overrides = new HashMap<>();
    overrides.put("projects", "baz1");
    // when
    applier.applyPropertiesOverride(jsonMapper.readTree(json), overrides);
  }

  @Test(
      expectedExceptions = DevfileFormatException.class,
      expectedExceptionsMessageRegExp =
          "Override property reference 'projects' points to an array type object. Please add an item qualifier by name.")
  public void shouldThrowExceptionIfOverrideReferenceIsJustWithArray() throws Exception {
    String json =
        "{"
            + "\"apiVersion\": \"1.0.0\","
            + "\"projects\": ["
            + "   {"
            + "      \"name\": \"test1\","
            + "      \"clonePath\": \"/foo/bar1\""
            + "   }"
            + "  ]"
            + "}";
    Map<String, String> overrides = new HashMap<>();
    overrides.put("projects", "baz1");
    // when
    applier.applyPropertiesOverride(jsonMapper.readTree(json), overrides);
  }

  @Test(
      expectedExceptions = DevfileFormatException.class,
      expectedExceptionsMessageRegExp =
          "Override path 'commands.run.foo.bar' starts with an unsupported field pointer. Supported fields are \\{\"apiVersion\",\"metadata\",\"projects\"\\}.")
  public void shouldThrowExceptionIfOverrideReferenceUsesUnsupportedField() throws Exception {
    String json =
        "{"
            + "\"apiVersion\": \"1.0.0\","
            + "\"projects\": ["
            + "   {"
            + "      \"name\": \"test1\","
            + "      \"clonePath\": \"/foo/bar1\""
            + "   }"
            + "  ]"
            + "}";
    Map<String, String> overrides = new HashMap<>();
    overrides.put("commands.run.foo.bar", "baz1");
    // when
    applier.applyPropertiesOverride(jsonMapper.readTree(json), overrides);
  }
}
