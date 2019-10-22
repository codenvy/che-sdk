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

package org.eclipse.che.workspace.infrastructure.kubernetes.provision;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressBackend;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.che.api.workspace.server.model.impl.CommandImpl;
import org.eclipse.che.api.workspace.server.model.impl.devfile.PreviewUrlImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.InternalInfrastructureException;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.namespace.KubernetesIngresses;
import org.eclipse.che.workspace.infrastructure.kubernetes.namespace.KubernetesNamespace;
import org.eclipse.che.workspace.infrastructure.kubernetes.namespace.KubernetesServices;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(MockitoTestNGListener.class)
public class PreviewUrlCommandProvisionerTest {

  private PreviewUrlCommandProvisioner<KubernetesEnvironment> previewUrlCommandProvisioner;
  @Mock private KubernetesEnvironment mockEnvironment;
  @Mock private KubernetesNamespace mockNamespace;
  @Mock private KubernetesServices mockServices;
  @Mock private KubernetesIngresses mockIngresses;

  @BeforeMethod
  public void setUp() {
    previewUrlCommandProvisioner = new PreviewUrlCommandProvisioner<>();
  }

  @Test
  public void shouldDoNothingWhenGetCommandsIsNull() throws InfrastructureException {
    Mockito.when(mockEnvironment.getCommands()).thenReturn(null);

    previewUrlCommandProvisioner.provision(mockEnvironment, mockNamespace);
  }

  @Test
  public void shouldDoNothingWhenNoCommandsDefined() throws InfrastructureException {
    Mockito.when(mockEnvironment.getCommands()).thenReturn(Collections.emptyList());
    Mockito.when(mockNamespace.ingresses()).thenReturn(mockIngresses);

    previewUrlCommandProvisioner.provision(mockEnvironment, mockNamespace);
  }

  @Test
  public void shouldDoNothingWhenCommandsWithoutPreviewUrlDefined() throws InfrastructureException {
    List<CommandImpl> commands =
        Arrays.asList(new CommandImpl("a", "a", "a"), new CommandImpl("b", "b", "b"));
    KubernetesEnvironment env =
        KubernetesEnvironment.builder().setCommands(new ArrayList<>(commands)).build();

    Mockito.when(mockNamespace.ingresses()).thenReturn(mockIngresses);

    previewUrlCommandProvisioner.provision(env, mockNamespace);

    assertTrue(commands.containsAll(env.getCommands()));
    assertTrue(env.getCommands().containsAll(commands));
  }

  @Test
  public void shouldDoNothingWhenCantFindServiceForPreviewurl() throws InfrastructureException {
    List<CommandImpl> commands =
        Collections.singletonList(
            new CommandImpl("a", "a", "a", new PreviewUrlImpl(8080, null), Collections.emptyMap()));
    KubernetesEnvironment env =
        KubernetesEnvironment.builder().setCommands(new ArrayList<>(commands)).build();

    Mockito.when(mockNamespace.ingresses()).thenReturn(mockIngresses);
    Mockito.when(mockNamespace.services()).thenReturn(mockServices);
    Mockito.when(mockServices.get()).thenReturn(Collections.emptyList());

    previewUrlCommandProvisioner.provision(env, mockNamespace);

    assertTrue(commands.containsAll(env.getCommands()));
    assertTrue(env.getCommands().containsAll(commands));
  }

  @Test
  public void shouldDoNothingWhenCantFindIngressForPreviewUrl() throws InfrastructureException {
    int port = 8080;
    List<CommandImpl> commands =
        Collections.singletonList(
            new CommandImpl("a", "a", "a", new PreviewUrlImpl(port, null), Collections.emptyMap()));
    KubernetesEnvironment env =
        KubernetesEnvironment.builder().setCommands(new ArrayList<>(commands)).build();

    Mockito.when(mockNamespace.services()).thenReturn(mockServices);
    Service service = new Service();
    ServiceSpec spec = new ServiceSpec();
    spec.setPorts(
        Collections.singletonList(new ServicePort("a", null, port, "TCP", new IntOrString(port))));
    service.setSpec(spec);
    Mockito.when(mockServices.get()).thenReturn(Collections.singletonList(service));

    Mockito.when(mockNamespace.ingresses()).thenReturn(mockIngresses);
    Mockito.when(mockIngresses.get()).thenReturn(Collections.emptyList());

    previewUrlCommandProvisioner.provision(env, mockNamespace);

    assertTrue(commands.containsAll(env.getCommands()));
    assertTrue(env.getCommands().containsAll(commands));
  }

  @Test
  public void shouldUpdateCommandWhenServiceAndIngressFound() throws InfrastructureException {
    final int PORT = 8080;
    final String SERVICE_PORT_NAME = "service-" + PORT;
    List<CommandImpl> commands =
        Collections.singletonList(
            new CommandImpl("a", "a", "a", new PreviewUrlImpl(PORT, null), Collections.emptyMap()));
    KubernetesEnvironment env =
        KubernetesEnvironment.builder().setCommands(new ArrayList<>(commands)).build();

    Mockito.when(mockNamespace.services()).thenReturn(mockServices);
    Service service = new Service();
    ObjectMeta metadata = new ObjectMeta();
    metadata.setName("servicename");
    service.setMetadata(metadata);
    ServiceSpec spec = new ServiceSpec();
    spec.setPorts(
        Collections.singletonList(
            new ServicePort(SERVICE_PORT_NAME, null, PORT, "TCP", new IntOrString(PORT))));
    service.setSpec(spec);
    Mockito.when(mockServices.get()).thenReturn(Collections.singletonList(service));

    Ingress ingress = new Ingress();
    IngressSpec ingressSpec = new IngressSpec();
    IngressRule rule =
        new IngressRule(
            "testhost",
            new HTTPIngressRuleValue(
                Collections.singletonList(
                    new HTTPIngressPath(
                        new IngressBackend("servicename", new IntOrString(SERVICE_PORT_NAME)),
                        null))));
    ingressSpec.setRules(Collections.singletonList(rule));
    ingress.setSpec(ingressSpec);
    Mockito.when(mockNamespace.ingresses()).thenReturn(mockIngresses);
    Mockito.when(mockIngresses.get()).thenReturn(Collections.singletonList(ingress));

    previewUrlCommandProvisioner.provision(env, mockNamespace);

    assertTrue(env.getCommands().get(0).getAttributes().containsKey("previewUrl"));
    assertEquals(env.getCommands().get(0).getAttributes().get("previewUrl"), "testhost");
  }

  @Test(expectedExceptions = InternalInfrastructureException.class)
  public void throwsInfrastructureExceptionWhenCantCastToIngress() throws InfrastructureException {
    previewUrlCommandProvisioner.findHostForServicePort(Arrays.asList("1", "2", "3"), null, 1);
  }
}
