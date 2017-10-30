/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.docker.environment.dockerimage;

import static java.lang.String.format;
import static org.eclipse.che.workspace.infrastructure.docker.ArgumentsValidator.checkArgument;

import com.google.common.base.Joiner;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.workspace.server.spi.InternalEnvironment;
import org.eclipse.che.api.workspace.server.spi.InternalRecipe;
import org.eclipse.che.workspace.infrastructure.docker.environment.DockerConfigSourceSpecificEnvironmentParser;
import org.eclipse.che.workspace.infrastructure.docker.model.DockerContainerConfig;
import org.eclipse.che.workspace.infrastructure.docker.model.DockerEnvironment;

/**
 * Docker image specific environment parser.
 *
 * @author Alexander Garagatyi
 * @author Alexander Andrienko
 */
public class DockerImageEnvironmentParser implements DockerConfigSourceSpecificEnvironmentParser {
  public static final String TYPE = "dockerimage";

  @Override
  public DockerEnvironment parse(InternalEnvironment environment) throws ValidationException {
    InternalRecipe recipe = environment.getRecipe();

    if (!TYPE.equals(recipe.getType())) {
      throw new ValidationException(
          format(
              "Docker image environment parser doesn't support recipe type '%s'",
              recipe.getType()));
    }

    DockerEnvironment dockerEnv = new DockerEnvironment();
    DockerContainerConfig container = new DockerContainerConfig();
    dockerEnv.getContainers().put(getMachineName(environment), container);
    container.setImage(recipe.getContent());

    return dockerEnv;
  }

  private String getMachineName(InternalEnvironment environment) throws ValidationException {
    checkArgument(
        environment.getMachines().size() == 1,
        "Environment of type '%s' doesn't support multiple machines, but contains machines: %s",
        environment.getRecipe().getType(),
        Joiner.on(", ").join(environment.getMachines().keySet()));

    return environment.getMachines().keySet().iterator().next();
  }
}
