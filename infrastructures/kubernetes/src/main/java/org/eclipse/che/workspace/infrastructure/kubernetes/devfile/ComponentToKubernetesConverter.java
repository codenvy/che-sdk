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
package org.eclipse.che.workspace.infrastructure.kubernetes.devfile;

import static java.util.Collections.singletonList;
import static org.eclipse.che.api.workspace.server.devfile.Constants.DISCOVERABLE_ENDPOINT_ATTRIBUTE;
import static org.eclipse.che.workspace.infrastructure.kubernetes.devfile.DockerimageComponentToWorkspaceApplier.CHE_COMPONENT_NAME_LABEL;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import org.eclipse.che.api.core.model.workspace.devfile.Component;
import org.eclipse.che.api.core.model.workspace.devfile.Endpoint;

/**
 * Converts devfile entities into kubernetes entities.
 */
@Singleton
class ComponentToKubernetesConverter {

  /**
   * Converts given {@link Component}'s {@link Endpoint}s into k8s {@link Service}`.
   *
   * @param component to convert
   * @return created services
   */
  List<Service> publicEndpointsToServices(Component component, String serviceLabel) {
    return component
        .getEndpoints()
        .stream()
        .filter(e -> "true".equals(e.getAttributes().get(DISCOVERABLE_ENDPOINT_ATTRIBUTE)))
        .map(e -> createService(e, serviceLabel))
        .collect(Collectors.toList());
  }

  private Service createService(Endpoint endpoint, String label) {
    ServicePort servicePort =
        new ServicePortBuilder()
            .withPort(endpoint.getPort())
            .withProtocol("TCP")
            .withNewTargetPort(endpoint.getPort())
            .build();
    return new ServiceBuilder()
        .withNewMetadata()
        .withName(endpoint.getName())
        .endMetadata()
        .withNewSpec()
        .withSelector(ImmutableMap.of(CHE_COMPONENT_NAME_LABEL, label))
        .withPorts(singletonList(servicePort))
        .endSpec()
        .build();
  }
}
