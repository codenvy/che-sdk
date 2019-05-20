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

import static java.lang.String.format;
import static org.eclipse.che.api.devfile.server.Components.getIdentifiableComponentName;
import static org.eclipse.che.api.devfile.server.Constants.DOCKERIMAGE_COMPONENT_TYPE;
import static org.eclipse.che.api.devfile.server.Constants.EDITOR_COMPONENT_TYPE;
import static org.eclipse.che.api.devfile.server.Constants.KUBERNETES_COMPONENT_TYPE;
import static org.eclipse.che.api.devfile.server.Constants.OPENSHIFT_COMPONENT_TYPE;
import static org.eclipse.che.api.devfile.server.Constants.PLUGIN_COMPONENT_TYPE;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.utils.Serialization;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.devfile.Action;
import org.eclipse.che.api.core.model.workspace.devfile.Command;
import org.eclipse.che.api.core.model.workspace.devfile.Component;
import org.eclipse.che.api.core.model.workspace.devfile.Devfile;
import org.eclipse.che.api.core.model.workspace.devfile.Entrypoint;
import org.eclipse.che.api.core.model.workspace.devfile.Project;
import org.eclipse.che.api.devfile.server.FileContentProvider;
import org.eclipse.che.api.devfile.server.convert.component.kubernetes.ContainerSearch;
import org.eclipse.che.api.devfile.server.convert.component.kubernetes.SelectorFilter;
import org.eclipse.che.api.devfile.server.exception.DevfileException;
import org.eclipse.che.api.devfile.server.exception.DevfileFormatException;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesRecipeParser;

/** Validates devfile logical integrity. */
@Singleton
public class DevfileIntegrityValidator {

  /**
   * Checks than name may contain only letters, digits, symbols _.- and does not starts with
   * non-word character.
   */
  private static final Pattern PROJECT_NAME_PATTERN = Pattern.compile("^[\\w\\d]+[\\w\\d_.-]*$");

  private final KubernetesRecipeParser kubernetesRecipeParser;

  @Inject
  public DevfileIntegrityValidator(KubernetesRecipeParser kubernetesRecipeParser) {
    this.kubernetesRecipeParser = kubernetesRecipeParser;
  }

  /**
   * Performs the following checks:
   *
   * <pre>
   * <ul>
   *   <li>All listed items (projects, components, commands) have unique names</li>
   *   <li>There is only one component of type cheEditor</li>
   *   <li>All components exists which are referenced/required by command actions</li>
   *   <li>Project names conforms naming rules</li>
   * </ul>
   * </pre>
   *
   * <p>Note that this doesn't validate the selectors in the devfile. If you have access to the
   * {@link FileContentProvider} instance, you may want to also invoke {@link
   * #validateContentReferences(Devfile, FileContentProvider)} to perform further validation that is
   * otherwise impossible.
   *
   * @param devfile input devfile
   * @throws DevfileFormatException if some of the checks is failed
   * @see #validateContentReferences(Devfile, FileContentProvider)
   */
  public void validateDevfile(Devfile devfile) throws DevfileFormatException {
    validateProjects(devfile);
    Set<String> knownAliases = validateComponents(devfile);
    validateCommands(devfile, knownAliases);
  }

  /**
   * Validates that various selectors in the devfile reference something in the referenced content.
   *
   * @param devfile the validated devfile
   * @param provider the file content provider to fetch the referenced content with.
   * @throws DevfileFormatException when some selectors don't match any objects in the referenced
   *     content or any other exception thrown during the validation
   */
  public void validateContentReferences(Devfile devfile, FileContentProvider provider)
      throws DevfileFormatException {
    for (Component component : devfile.getComponents()) {
      try {
        List<HasMetadata> selectedObjects = validateSelector(component, provider);
        validateEntrypointSelector(component, selectedObjects);
      } catch (Exception e) {
        throw new DevfileFormatException(
            format(
                "Failed to validate content reference of component '%s' of type '%s': %s",
                getIdentifiableComponentName(component), component.getType(), e.getMessage()),
            e);
      }
    }
  }

  private Set<String> validateComponents(Devfile devfile) throws DevfileFormatException {
    Set<String> definedAliases = new HashSet<>();
    Component editorComponent = null;

    Map<String, Set<String>> idsPerComponentType = new HashMap<>();

    for (Component component : devfile.getComponents()) {
      if (component.getAlias() != null && !definedAliases.add(component.getAlias())) {
        throw new DevfileFormatException(
            format("Duplicate component alias found:'%s'", component.getAlias()));
      }

      if (!idsPerComponentType
          .computeIfAbsent(component.getType(), __ -> new HashSet<>())
          .add(getIdentifiableComponentName(component))) {

        throw new DevfileFormatException(
            format(
                "There are multiple components '%s' of type '%s' that cannot be uniquely"
                    + " identified. Please add aliases that would distinguish the components.",
                getIdentifiableComponentName(component), component.getType()));
      }

      switch (component.getType()) {
        case EDITOR_COMPONENT_TYPE:
          if (editorComponent != null) {
            throw new DevfileFormatException(
                format(
                    "Multiple editor components found: '%s', '%s'",
                    getIdentifiableComponentName(editorComponent),
                    getIdentifiableComponentName(component)));
          }
          editorComponent = component;
          break;

        case PLUGIN_COMPONENT_TYPE:
        case KUBERNETES_COMPONENT_TYPE:
        case OPENSHIFT_COMPONENT_TYPE:
        case DOCKERIMAGE_COMPONENT_TYPE:
          // do nothing
          break;

        default:
          throw new DevfileFormatException(
              format(
                  "One of the components has unsupported component type: '%s'",
                  component.getType()));
      }
    }
    return definedAliases;
  }

  private void validateCommands(Devfile devfile, Set<String> knownAliases)
      throws DevfileFormatException {
    Set<String> existingNames = new HashSet<>();
    for (Command command : devfile.getCommands()) {
      if (!existingNames.add(command.getName())) {
        throw new DevfileFormatException(
            format("Duplicate command name found:'%s'", command.getName()));
      }

      if (command.getActions().isEmpty()) {
        throw new DevfileFormatException(
            format("Command '%s' does not have actions.", command.getName()));
      }

      // It is temporary restriction while exec plugin is not able to handle multiple commands
      // in different containers as one Task. Later supporting of multiple actions in one command
      // may be implemented.
      if (command.getActions().size() > 1) {
        throw new DevfileFormatException(
            format("Multiple actions in command '%s' are not supported yet.", command.getName()));
      }

      Action action = command.getActions().get(0);

      if (action.getComponent() == null
          && (action.getReference() != null || action.getReferenceContent() != null)) {
        // ok, this action contains a reference to the file containing the definition. Such
        // actions don't have to have component alias defined.
        continue;
      }

      if (!knownAliases.contains(action.getComponent())) {
        throw new DevfileFormatException(
            format(
                "Command '%s' has action that refers to a component with unknown alias '%s'",
                command.getName(), action.getComponent()));
      }
    }
  }

  private void validateProjects(Devfile devfile) throws DevfileFormatException {
    Set<String> existingNames = new HashSet<>();
    for (Project project : devfile.getProjects()) {
      if (!existingNames.add(project.getName())) {
        throw new DevfileFormatException(
            format("Duplicate project name found:'%s'", project.getName()));
      }
      if (!PROJECT_NAME_PATTERN.matcher(project.getName()).matches()) {
        throw new DevfileFormatException(
            format(
                "Invalid project name found:'%s'. Name must contain only Latin letters,"
                    + "digits or these following special characters ._-",
                project.getName()));
      }
    }
  }

  /**
   * Validates that the selector, if any, selects some objects from the component's referenced
   * content. Only does anything for kubernetes and openshift components.
   *
   * @param component the component to check
   * @param contentProvider the content provider to use when fetching content
   * @return the list of referenced objects matching the selector or empty list
   * @throws ValidationException on failure to validate the referenced content
   * @throws InfrastructureException on failure to parse the referenced content
   * @throws IOException on failure to retrieve the referenced content
   * @throws DevfileException if the selector filters out all referenced objects
   */
  private List<HasMetadata> validateSelector(
      Component component, FileContentProvider contentProvider)
      throws ValidationException, InfrastructureException, IOException, DevfileException {

    if (!component.getType().equals(KUBERNETES_COMPONENT_TYPE)
        && !component.getType().equals(OPENSHIFT_COMPONENT_TYPE)) {
      return Collections.emptyList();
    }

    List<HasMetadata> content = getReferencedKubernetesList(component, contentProvider);

    Map<String, String> selector = component.getSelector();
    if (selector == null || selector.isEmpty()) {
      return content;
    }

    content = SelectorFilter.filter(content, selector);

    if (content.isEmpty()) {
      throw new DevfileException(
          format(
              "The selector of the component '%s' of type '%s' filters out all objects from"
                  + " the list.",
              getIdentifiableComponentName(component), component.getType()));
    }

    return content;
  }

  private void validateEntrypointSelector(Component component, List<HasMetadata> filteredObjects)
      throws DevfileException {

    if (component.getEntrypoints() == null || component.getEntrypoints().isEmpty()) {
      return;
    }

    for (Entrypoint ep : component.getEntrypoints()) {
      ContainerSearch search =
          new ContainerSearch(ep.getParentName(), ep.getParentSelector(), ep.getContainerName());

      List<Container> cs = search.search(filteredObjects);

      if (cs.isEmpty()) {
        throw new DevfileFormatException(
            format(
                "Component '%s' of type '%s' contains an entry point that doesn't match any"
                    + " container:\n%s",
                getIdentifiableComponentName(component), component.getType(), toYAML(ep)));
      }
    }
  }

  private List<HasMetadata> getReferencedKubernetesList(
      Component component, FileContentProvider contentProvider)
      throws ValidationException, InfrastructureException, IOException, DevfileException {
    List<HasMetadata> content;
    if (component.getReferenceContent() != null) {
      content = kubernetesRecipeParser.parse(component.getReferenceContent());
    } else if (component.getReference() != null) {
      String data = contentProvider.fetchContent(component.getReference());
      content = kubernetesRecipeParser.parse(data);
    } else {
      content = Collections.emptyList();
    }

    return content;
  }

  private String toYAML(Entrypoint ep) throws DevfileException {
    try {
      return Serialization.asYaml(ep);
    } catch (Exception e) {
      throw new DevfileException(e.getMessage(), e);
    }
  }
}
