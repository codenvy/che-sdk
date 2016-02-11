/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.docker.machine.ext.provider;

import org.eclipse.che.plugin.docker.machine.DockerInstanceMetadata;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Add env variable to docker dev-machine with url of Che API
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class ApiEndpointEnvVariableProvider implements Provider<String> {
    @Inject
    @Named("machine.docker.che_api.endpoint")
    private String apiEndpoint;

    @Override
    public String get() {
        return DockerInstanceMetadata.API_ENDPOINT_URL_VARIABLE + '=' + apiEndpoint;
    }
}
