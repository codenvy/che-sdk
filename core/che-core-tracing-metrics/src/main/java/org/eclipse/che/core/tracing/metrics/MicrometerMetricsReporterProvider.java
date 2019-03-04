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
package org.eclipse.che.core.tracing.metrics;

import io.opentracing.contrib.metrics.micrometer.MicrometerMetricsReporter;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Provider of {@link MicrometerMetricsReporter}, which is responsible for reporting metrics of
 * traced spans to prometheus server. Here is also specified configuration as for metrics name and
 * tags.
 */
@Singleton
public class MicrometerMetricsReporterProvider implements Provider<MicrometerMetricsReporter> {

  private final MicrometerMetricsReporter micrometerMetricsReporter;

  public MicrometerMetricsReporterProvider() {
    micrometerMetricsReporter =
        MicrometerMetricsReporter.newMetricsReporter()
            .withName("che_server_api_tracing_span")
            .withTagLabel("span.kind", null)
            .withTagLabel("http.status_code", null)
            .build();
  }

  @Override
  public MicrometerMetricsReporter get() {
    return micrometerMetricsReporter;
  }
}
