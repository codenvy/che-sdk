/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.workspace.infrastructure.docker;

import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.config.Environment;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.installer.server.InstallerRegistry;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalInfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalRuntime;
import org.eclipse.che.api.workspace.server.spi.RuntimeContext;
import org.eclipse.che.api.workspace.shared.Utils;
import org.eclipse.che.workspace.infrastructure.docker.model.DockerEnvironment;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;
import java.net.URI;
import java.util.List;

import static org.eclipse.che.workspace.infrastructure.docker.output.OutputEndpoint.OUTPUT_WEBSOCKET_ENDPOINT_BASE;

/**
 * @author Alexander Garagatyi
 */
// TODO Check what if start fails and interruption called or stop called
// TODO interrupted exception, closedbyinteruptionexception

// TODO stop of starting WS - if not supported specific exception
// TODO stop add warning on errors?
// TODO stop in which cases to throw an exception?

// TODO exception on start
// TODO remove starting machine if present
// TODO Check if interruption came from stop or because of another reason
// TODO if because of another reason stop environment
public class DockerRuntimeContext extends RuntimeContext {
    private final DockerEnvironment    dockerEnvironment;
    private final DockerRuntimeFactory dockerRuntimeFactory;
    private final List<String>         orderedServices;
    private final String               devMachineName;
    private final String               apiEndpoint;

    @Inject
    public DockerRuntimeContext(@Assisted DockerRuntimeInfrastructure infrastructure,
                                @Assisted RuntimeIdentity identity,
                                @Assisted Environment environment,
                                @Assisted DockerEnvironment dockerEnvironment,
                                @Assisted List<String> orderedServices,
                                InstallerRegistry installerRegistry,
                                DockerRuntimeFactory dockerRuntimeFactory,
                                String apiEndpoint)
            throws ValidationException, InfrastructureException {
        super(environment, identity, infrastructure, installerRegistry);
        this.devMachineName = Utils.getDevMachineName(environment);
        this.orderedServices = orderedServices;
        this.dockerEnvironment = dockerEnvironment;
        this.dockerRuntimeFactory = dockerRuntimeFactory;
        this.apiEndpoint = apiEndpoint;
    }

    @Override
    public URI getOutputChannel() throws InfrastructureException, UnsupportedOperationException {
        try {
            final URI apiURI = URI.create(apiEndpoint);
            return UriBuilder.fromUri(apiURI)
                             .scheme("https".equals(apiURI.getScheme()) ? "wss" : "ws")
                             .replacePath(apiURI.getPath().replace("/api", ""))
                             .path(OUTPUT_WEBSOCKET_ENDPOINT_BASE)
                             .build();
        } catch (UriBuilderException | IllegalArgumentException ex) {
            throw new InternalInfrastructureException("Failed to get the output channel because: " +
                                                      ex.getLocalizedMessage());
        }
    }

    @Override
    public InternalRuntime getRuntime() {
        return dockerRuntimeFactory.createRuntime(this,
                                                  devMachineName,
                                                  orderedServices,
                                                  dockerEnvironment,
                                                  identity);
    }
}
