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
package org.eclipse.che.workspace.infrastructure.kubernetes.server.secure.jwtproxy;

import static org.eclipse.che.api.core.model.workspace.config.ServerConfig.SECURE_SERVER_COOKIES_AUTH_ENABLED_ATTRIBUTE;
import static org.eclipse.che.workspace.infrastructure.kubernetes.server.KubernetesServerExposer.SERVER_PREFIX;
import static org.eclipse.che.workspace.infrastructure.kubernetes.server.KubernetesServerExposer.SERVER_UNIQUE_PART_SIZE;
import static org.eclipse.che.workspace.infrastructure.kubernetes.server.external.MultiHostExternalServiceExposureStrategy.MULTI_HOST_STRATEGY;
import static org.eclipse.che.workspace.infrastructure.kubernetes.server.secure.jwtproxy.JwtProxyProvisioner.JWT_PROXY_CONFIG_FILE;
import static org.eclipse.che.workspace.infrastructure.kubernetes.server.secure.jwtproxy.JwtProxyProvisioner.JWT_PROXY_PUBLIC_KEY_FILE;
import static org.eclipse.che.workspace.infrastructure.kubernetes.server.secure.jwtproxy.JwtProxyProvisioner.PUBLIC_KEY_FOOTER;
import static org.eclipse.che.workspace.infrastructure.kubernetes.server.secure.jwtproxy.JwtProxyProvisioner.PUBLIC_KEY_HEADER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import java.net.URI;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.regex.Pattern;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.model.impl.RuntimeIdentityImpl;
import org.eclipse.che.api.workspace.server.model.impl.ServerConfigImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.environment.InternalMachineConfig;
import org.eclipse.che.multiuser.machine.authentication.server.signature.SignatureKeyManager;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.server.external.ExternalServiceExposureStrategy;
import org.eclipse.che.workspace.infrastructure.kubernetes.server.secure.jwtproxy.factory.JwtProxyConfigBuilderFactory;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Tests {@link JwtProxyProvisioner}.
 *
 * @author Sergii Leshchenko
 */
@Listeners(MockitoTestNGListener.class)
public class JwtProxyProvisionerTest {

  private static final String WORKSPACE_ID = "workspace123";
  private static final Pattern JWTPROXY_SERVICE_NAME_PATTERN =
      Pattern.compile(SERVER_PREFIX + "\\w{" + SERVER_UNIQUE_PART_SIZE + "}-jwtproxy");
  private final RuntimeIdentity runtimeId =
      new RuntimeIdentityImpl(WORKSPACE_ID, "env123", "owner123", "infraNamespace");

  @Mock private SignatureKeyManager signatureKeyManager;
  @Mock private PublicKey publicKey;
  @Mock private JwtProxyConfigBuilderFactory configBuilderFactory;
  @Mock private ExternalServiceExposureStrategy externalServiceExposureStrategy;
  private CookiePathStrategy cookiePathStrategy = new CookiePathStrategy(MULTI_HOST_STRATEGY);

  private JwtProxyProvisioner jwtProxyProvisioner;
  private KubernetesEnvironment k8sEnv;

  @BeforeMethod
  public void setUp() throws Exception {
    when(signatureKeyManager.getOrCreateKeyPair(anyString()))
        .thenReturn(new KeyPair(publicKey, null));
    when(publicKey.getEncoded()).thenReturn("publickey".getBytes());

    when(configBuilderFactory.create(any()))
        .thenReturn(
            new JwtProxyConfigBuilder(
                URI.create("http://che.api"), "iss", "1h", "", runtimeId.getWorkspaceId()));
    jwtProxyProvisioner =
        new JwtProxyProvisioner(
            signatureKeyManager,
            configBuilderFactory,
            externalServiceExposureStrategy,
            cookiePathStrategy,
            "eclipse/che-jwtproxy",
            "128mb",
            runtimeId);
    k8sEnv = KubernetesEnvironment.builder().build();
  }

  @Test
  public void shouldReturnGeneratedJwtProxyServiceName() {
    // when
    String jwtProxyServiceName = jwtProxyProvisioner.getServiceName();

    // then
    assertTrue(JWTPROXY_SERVICE_NAME_PATTERN.matcher(jwtProxyServiceName).matches());
  }

  @Test
  public void shouldReturnGeneratedJwtProxyConfigMapName() {
    // when
    String jwtProxyConfigMap = jwtProxyProvisioner.getConfigMapName();

    // then
    assertEquals(jwtProxyConfigMap, "jwtproxy-config-" + WORKSPACE_ID);
  }

  @Test
  public void shouldProvisionJwtProxyRelatedObjectsIntoKubernetesEnvironment() throws Exception {
    // given
    ServerConfigImpl secureServer =
        new ServerConfigImpl("4401/tcp", "ws", "/", Collections.emptyMap());

    ServicePort port = new ServicePort();
    port.setTargetPort(new IntOrString(4401));

    // when
    jwtProxyProvisioner.expose(
        k8sEnv, "terminal", port, "TCP", ImmutableMap.of("server", secureServer));

    // then
    InternalMachineConfig jwtProxyMachine =
        k8sEnv.getMachines().get(JwtProxyProvisioner.JWT_PROXY_MACHINE_NAME);
    assertNotNull(jwtProxyMachine);

    ConfigMap configMap = k8sEnv.getConfigMaps().get(jwtProxyProvisioner.getConfigMapName());
    assertNotNull(configMap);
    assertEquals(
        configMap.getData().get(JWT_PROXY_PUBLIC_KEY_FILE),
        PUBLIC_KEY_HEADER
            + Base64.getEncoder().encodeToString("publickey".getBytes())
            + PUBLIC_KEY_FOOTER);
    assertNotNull(configMap.getData().get(JWT_PROXY_CONFIG_FILE));

    Pod jwtProxyPod = k8sEnv.getPodsCopy().get("che-jwtproxy");
    assertNotNull(jwtProxyPod);

    Service jwtProxyService = k8sEnv.getServices().get(jwtProxyProvisioner.getServiceName());
    assertNotNull(jwtProxyService);
  }

  @Test(
      expectedExceptions = InfrastructureException.class,
      expectedExceptionsMessageRegExp =
          "Secure servers which expose the same port should have "
              + "the same `cookiesAuthEnabled` value\\.")
  public void shouldThrowAnExceptionIsServersHaveDifferentValueForCookiesAuthEnabled()
      throws Exception {
    // given
    ServerConfigImpl server1 =
        new ServerConfigImpl(
            "4401/tcp",
            "ws",
            "/",
            ImmutableMap.of(SECURE_SERVER_COOKIES_AUTH_ENABLED_ATTRIBUTE, "true"));
    ServerConfigImpl server2 =
        new ServerConfigImpl(
            "4401/tcp",
            "http",
            "/",
            ImmutableMap.of(SECURE_SERVER_COOKIES_AUTH_ENABLED_ATTRIBUTE, "false"));
    ServerConfigImpl server3 = new ServerConfigImpl("4401/tcp", "ws", "/", Collections.emptyMap());

    ServicePort port = new ServicePort();
    port.setTargetPort(new IntOrString(4401));

    // when
    jwtProxyProvisioner.expose(
        k8sEnv,
        "terminal",
        port,
        "TCP",
        ImmutableMap.of("server1", server1, "server2", server2, "server3", server3));
  }

  @Test
  public void shouldUseCookiesAuthEnabledFromServersConfigs() throws Exception {
    // given
    JwtProxyConfigBuilder configBuilder = mock(JwtProxyConfigBuilder.class);
    when(configBuilderFactory.create(any())).thenReturn(configBuilder);

    jwtProxyProvisioner =
        new JwtProxyProvisioner(
            signatureKeyManager,
            configBuilderFactory,
            externalServiceExposureStrategy,
            cookiePathStrategy,
            "eclipse/che-jwtproxy",
            "128mb",
            runtimeId);

    ServerConfigImpl server1 =
        new ServerConfigImpl(
            "4401/tcp",
            "http",
            "/",
            ImmutableMap.of(SECURE_SERVER_COOKIES_AUTH_ENABLED_ATTRIBUTE, "true"));
    ServerConfigImpl server2 =
        new ServerConfigImpl(
            "4401/tcp",
            "ws",
            "/",
            ImmutableMap.of(SECURE_SERVER_COOKIES_AUTH_ENABLED_ATTRIBUTE, "true"));

    ServicePort port = new ServicePort();
    port.setTargetPort(new IntOrString(4401));

    // when
    jwtProxyProvisioner.expose(
        k8sEnv, "terminal", port, "TCP", ImmutableMap.of("server1", server1, "server2", server2));

    // then
    verify(configBuilder).addVerifierProxy(any(), any(), any(), eq(true), any(), any());
  }

  @Test
  public void shouldFalseValueAsDefaultForCookiesAuthEnabledAttribute() throws Exception {
    // given
    JwtProxyConfigBuilder configBuilder = mock(JwtProxyConfigBuilder.class);
    when(configBuilderFactory.create(any())).thenReturn(configBuilder);

    jwtProxyProvisioner =
        new JwtProxyProvisioner(
            signatureKeyManager,
            configBuilderFactory,
            externalServiceExposureStrategy,
            cookiePathStrategy,
            "eclipse/che-jwtproxy",
            "128mb",
            runtimeId);

    ServerConfigImpl server1 =
        new ServerConfigImpl("4401/tcp", "http", "/", Collections.emptyMap());

    ServicePort port = new ServicePort();
    port.setTargetPort(new IntOrString(4401));

    // when
    jwtProxyProvisioner.expose(
        k8sEnv, "terminal", port, "TCP", ImmutableMap.of("server1", server1));

    // then
    verify(configBuilder).addVerifierProxy(any(), any(), any(), eq(false), any(), any());
  }
}
