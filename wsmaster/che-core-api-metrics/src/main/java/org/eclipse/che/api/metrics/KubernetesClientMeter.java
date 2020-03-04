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
package org.eclipse.che.api.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.workspace.infrastructure.kubernetes.KubernetesClientFactory;

@Singleton
public class KubernetesClientMeter implements MeterBinder {

  private final KubernetesClientFactory countedClientFactory;

  @Inject
  public KubernetesClientMeter(KubernetesClientFactory countedClientFactory) {
    this.countedClientFactory = countedClientFactory;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    Gauge.builder("k8s.client.invocation.total", countedClientFactory::getClientInvocationsCount)
        .register(registry);
  }
}
