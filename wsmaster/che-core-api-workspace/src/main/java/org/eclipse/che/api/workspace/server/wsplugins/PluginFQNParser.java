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
package org.eclipse.che.api.workspace.server.wsplugins;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

import com.google.common.annotations.Beta;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.wsplugins.model.PluginFQN;
import org.eclipse.che.api.workspace.shared.Constants;

/**
 * Parses workspace attributes into a list of {@link PluginFQN}.
 *
 * <p>This API is in <b>Beta</b> and is subject to changes or removal.
 *
 * @author Oleksander Garagatyi
 * @author Angel Misevski
 */
@Beta
public class PluginFQNParser {

  private static final String INCORRECT_PLUGIN_FORMAT_TEMPLATE =
      "Plugin '%s' has incorrect format. Should be: 'registryURL/publisher/name/version' or 'registryURL/name/version' or 'name:version' or 'registryURL/name:version'";
  private static final Pattern PLUGIN_PATTERN_V1 =
      Pattern.compile("(?<registry>(https?://)[-./\\w]+/)?(?<name>\\w+):(?<version>[-\\w.]+)");
  private static final Pattern PLUGIN_PATTERN_V2 =
      Pattern.compile(
          "(?<registry>(https?://)[-./\\w]+/)?(?<id>[-a-z0-9]+/[-a-z0-9]+/[-.a-z0-9]+)");

  /**
   * Parses a workspace attributes map into a collection of {@link PluginFQN}.
   *
   * @param attributes workspace attributes containing plugin and/or editor fields
   * @return a Collection of PluginFQN containing the editor and all plugins for this attributes
   * @throws InfrastructureException if attributes defines more than one editor
   */
  public Collection<PluginFQN> parsePlugins(Map<String, String> attributes)
      throws InfrastructureException {
    if (attributes == null) {
      return emptyList();
    }

    String pluginsAttribute =
        attributes.getOrDefault(Constants.WORKSPACE_TOOLING_PLUGINS_ATTRIBUTE, null);
    String editorAttribute =
        attributes.getOrDefault(Constants.WORKSPACE_TOOLING_EDITOR_ATTRIBUTE, null);

    List<PluginFQN> metaFQNs = new ArrayList<>();
    if (!isNullOrEmpty(pluginsAttribute)) {
      metaFQNs.addAll(parsePluginFQNs(pluginsAttribute));
    }
    if (!isNullOrEmpty(editorAttribute)) {
      Collection<PluginFQN> editorsFQNs = parsePluginFQNs(editorAttribute);
      if (editorsFQNs.size() > 1) {
        throw new InfrastructureException(
            "Multiple editors found in workspace config attributes. "
                + "Only one editor is supported per workspace.");
      }
      metaFQNs.addAll(editorsFQNs);
    }
    return metaFQNs;
  }

  private Collection<PluginFQN> parsePluginFQNs(String attribute) throws InfrastructureException {

    String[] plugins = splitAttribute(attribute);
    if (plugins.length == 0) {
      return Collections.emptyList();
    }

    List<PluginFQN> collectedFQNs = new ArrayList<>();
    for (String plugin : plugins) {
      PluginFQN pFQN = parsePluginFQN(plugin);

      if (collectedFQNs.stream().anyMatch(p -> p.getId().equals(pFQN.getId()))) {
        throw new InfrastructureException(
            format(
                "Invalid Che tooling plugins configuration: plugin %s is duplicated",
                pFQN.getId())); // even if different registries
      }
      collectedFQNs.add(pFQN);
    }
    return collectedFQNs;
  }

  private PluginFQN parsePluginFQN(String plugin) throws InfrastructureException {
    String registry;
    String id;
    URI registryURI = null;
    Matcher matcher = PLUGIN_PATTERN_V1.matcher(plugin);
    if (matcher.matches()) {
      registry = matcher.group("registry");
      id = matcher.group("name") + "/" + matcher.group("version");
    } else {
      matcher = PLUGIN_PATTERN_V2.matcher(plugin);
      if (matcher.matches()) {
        registry = matcher.group("registry");
        id = matcher.group("id");
      } else {
        throw new InfrastructureException(format(INCORRECT_PLUGIN_FORMAT_TEMPLATE, plugin));
      }
    }
    if (!isNullOrEmpty(registry)) {
      try {
        registryURI = new URI(registry);
      } catch (URISyntaxException e) {
        throw new InfrastructureException(
            format(
                "Plugin registry URL '%s' is incorrect. Problematic plugin entry: '%s'",
                registry, plugin));
      }
    }

    return new PluginFQN(registryURI, id);
  }

  private String[] splitAttribute(String attribute) {
    String[] plugins = attribute.split(",");
    return Arrays.stream(plugins).map(String::trim).toArray(String[]::new);
  }
}
