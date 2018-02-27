/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.kubernetes.provision;

import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressTLS;
import io.fabric8.kubernetes.api.model.extensions.IngressTLSBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.model.impl.ServerConfigImpl;
import org.eclipse.che.workspace.infrastructure.kubernetes.Annotations;
import org.eclipse.che.workspace.infrastructure.kubernetes.KubernetesInfrastructureException;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;

/**
 * Enables Transport Layer Security (TLS) for external server ingresses
 *
 * @author Guy Daich
 */
public class ExternalServerIngressTlsProvisioner
    implements ConfigurationProvisioner<KubernetesEnvironment> {

  protected final boolean isTlsEnabled;
  protected final String tlsSecret;
  protected final String cheHost;

  @Inject
  public ExternalServerIngressTlsProvisioner(
      @Named("che.infra.kubernetes.tls_enabled") boolean isTlsEnabled,
      @Named("che.infra.kubernetes.tls_secret") String tlsSecret,
      @Named("che.host") String cheHost) {
    this.isTlsEnabled = isTlsEnabled;
    this.cheHost = cheHost;
    this.tlsSecret = tlsSecret;
  }

  @Override
  public void provision(KubernetesEnvironment k8sEnv, RuntimeIdentity identity)
      throws KubernetesInfrastructureException {
    if (!isTlsEnabled) {
      return;
    }

    Collection<Ingress> ingresses = k8sEnv.getIngresses().values();
    for (Ingress ingress : ingresses) {
      useSecureProtocolForServers(ingress);
      enableTLS(ingress);
    }
  }

  private void enableTLS(Ingress ingress) {
    IngressTLS ingressTLS =
        new IngressTLSBuilder().withHosts(cheHost).withSecretName(tlsSecret).build();
    List<IngressTLS> ingressTLSList = new ArrayList<>(Arrays.asList(ingressTLS));
    ingress.getSpec().setTls(ingressTLSList);
  }

  private void useSecureProtocolForServers(final Ingress ingress) {
    Map<String, ServerConfigImpl> servers =
        Annotations.newDeserializer(ingress.getMetadata().getAnnotations()).servers();

    servers.values().forEach(s -> s.setProtocol(getSecureProtocol(s.getProtocol())));

    Map<String, String> annotations = Annotations.newSerializer().servers(servers).annotations();

    ingress.getMetadata().getAnnotations().putAll(annotations);
  }

  private String getSecureProtocol(final String protocol) {
    if ("ws".equals(protocol)) {
      return "wss";
    } else if ("http".equals(protocol)) {
      return "https";
    } else return protocol;
  }
}
