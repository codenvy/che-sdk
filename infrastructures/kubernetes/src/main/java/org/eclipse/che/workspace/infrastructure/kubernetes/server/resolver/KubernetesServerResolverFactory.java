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
package org.eclipse.che.workspace.infrastructure.kubernetes.server.resolver;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.che.workspace.infrastructure.kubernetes.server.external.IngressPathTransformInverter;

@Singleton
public class KubernetesServerResolverFactory {

  private final IngressPathTransformInverter pathTransformInverter;
  private final String cheHost;
  private final String exposureStrategy;

  @Inject
  public KubernetesServerResolverFactory(
      IngressPathTransformInverter pathTransformInverter,
      @Named("che.host") String cheHost,
      @Named("che.infra.kubernetes.server_strategy") String exposureStrategy) {
    this.pathTransformInverter = pathTransformInverter;
    this.cheHost = cheHost;
    this.exposureStrategy = exposureStrategy;
  }

  public ServerResolver create(
      List<Service> services, List<Ingress> ingresses, List<ConfigMap> configMaps) {
    // TODO: when gateway-based configuration is available, return Server resolver by configured
    // exposureStrategy
    return new IngressServerResolver(pathTransformInverter, services, ingresses);
  }
}
