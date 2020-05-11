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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Secret;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.ssh.server.SshManager;
import org.eclipse.che.api.ssh.server.model.impl.SshPairImpl;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Tests {@link SshKeysProvisioner}.
 *
 * @author Vitalii Parfonov
 * @author Vlad Zhukovskyi
 */
@Listeners(MockitoTestNGListener.class)
public class SshKeySecretProvisionerTest {

  private KubernetesEnvironment k8sEnv;
  @Mock private RuntimeIdentity runtimeIdentity;

  @Mock private SshManager sshManager;

  @Mock private Pod pod;

  @Mock private PodSpec podSpec;

  @Mock private Container container;

  private final String someUser = "someuser";

  private SshKeysProvisioner sshKeysProvisioner;

  @BeforeMethod
  public void setup() {
    when(runtimeIdentity.getOwnerId()).thenReturn(someUser);
    when(runtimeIdentity.getWorkspaceId()).thenReturn("wksp");
    k8sEnv = KubernetesEnvironment.builder().build();
    ObjectMeta podMeta = new ObjectMetaBuilder().withName("wksp").build();
    when(pod.getMetadata()).thenReturn(podMeta);
    when(pod.getSpec()).thenReturn(podSpec);
    when(podSpec.getVolumes()).thenReturn(new ArrayList<>());
    when(podSpec.getContainers()).thenReturn(Collections.singletonList(container));
    when(container.getVolumeMounts()).thenReturn(new ArrayList<>());
    k8sEnv.addPod(pod);
    sshKeysProvisioner = new SshKeysProvisioner(sshManager);
  }

  @Test
  public void generateSshKeyIfNoSshKeys() throws Exception {
    when(sshManager.getPairs(someUser, "vcs")).thenReturn(Collections.emptyList());
    when(sshManager.generatePair(eq(someUser), eq("vcs"), anyString()))
        .thenReturn(
            new SshPairImpl(
                someUser, "vcs", "default-" + UUID.randomUUID().toString(), "public", "private"));

    sshKeysProvisioner.provision(k8sEnv, runtimeIdentity);

    assertEquals(k8sEnv.getSecrets().size(), 1);
  }

  @Test
  public void addSshKeysConfigInPod() throws Exception {
    String keyName1 = UUID.randomUUID().toString();
    String keyName2 = "default-" + UUID.randomUUID().toString();
    String keyName3 = "github.com";
    String keyName4 = UUID.randomUUID().toString();
    lenient()
        .when(sshManager.getPairs(someUser, "vcs"))
        .thenReturn(
            ImmutableList.of(
                new SshPairImpl(someUser, "vcs", keyName1, "public", "private"),
                new SshPairImpl(someUser, "vcs", keyName2, "public", "private"),
                new SshPairImpl(someUser, "vcs", keyName3, "public", "private")));

    lenient()
        .when(sshManager.getPairs(someUser, "internal"))
        .thenReturn(
            ImmutableList.of(new SshPairImpl(someUser, "internal", keyName4, "public", "private")));

    sshKeysProvisioner.provision(k8sEnv, runtimeIdentity);

    verify(podSpec, times(2)).getVolumes();
    verify(podSpec, times(2)).getContainers();

    Secret secret = k8sEnv.getSecrets().get("wksp-sshprivatekeys");
    assertNotNull(secret);
    assertEquals(secret.getType(), "opaque");

    String key1 = secret.getData().get(keyName1);
    assertNotNull(key1);
    assertEquals("private", new String(Base64.getDecoder().decode(key1)));

    String key2 = secret.getData().get(keyName2);
    assertNotNull(key2);
    assertEquals("private", new String(Base64.getDecoder().decode(key2)));

    String key3 = secret.getData().get(keyName3);
    assertNotNull(key3);
    assertEquals("private", new String(Base64.getDecoder().decode(key3)));

    String key4 = secret.getData().get(keyName3);
    assertNotNull(key3);
    assertEquals("private", new String(Base64.getDecoder().decode(key4)));

    Map<String, ConfigMap> configMaps = k8sEnv.getConfigMaps();
    assertNotNull(configMaps);
    assertTrue(configMaps.containsKey("wksp-sshconfigmap"));

    ConfigMap sshConfigMap = configMaps.get("wksp-sshconfigmap");
    assertNotNull(sshConfigMap);

    Map<String, String> mapData = sshConfigMap.getData();
    assertNotNull(mapData);
    assertTrue(mapData.containsKey("ssh_config"));

    String sshConfig = mapData.get("ssh_config");
    assertTrue(sshConfig.contains("host " + keyName1));
    assertTrue(sshConfig.contains("IdentityFile " + "/etc/ssh/private/" + keyName1));

    assertTrue(sshConfig.contains("host *"));
    assertTrue(sshConfig.contains("IdentityFile " + "/etc/ssh/private/" + keyName2));

    assertTrue(sshConfig.contains("host github.com"));
    assertTrue(sshConfig.contains("IdentityFile /etc/ssh/private/github.com"));
  }

  @Test
  public void shouldNotProvisionVolumeButShouldMountInInjectablePods() throws Exception {
    // given
    String keyName1 = UUID.randomUUID().toString();
    String keyName2 = "default-" + UUID.randomUUID().toString();
    String keyName3 = "github.com";
    when(sshManager.getPairs(someUser, "vcs"))
        .thenReturn(
            ImmutableList.of(
                new SshPairImpl(someUser, "vcs", keyName1, "public", "private"),
                new SshPairImpl(someUser, "vcs", keyName2, "public", "private"),
                new SshPairImpl(someUser, "vcs", keyName3, "public", "private")));

    Pod pod =
        new PodBuilder()
            .withNewMetadata()
            .withName("wkspc")
            .and()
            .withNewSpec()
            .withContainers(new ContainerBuilder().withImage("image").build())
            .and()
            .build();

    // we want to replace everything in the env that the setup() put there, so let's just re-init.
    k8sEnv = KubernetesEnvironment.builder().build();
    k8sEnv.addPod(pod);

    Pod injectedPod =
        new PodBuilder()
            .withNewMetadata()
            .withName("injected")
            .and()
            .withNewSpec()
            .withContainers(new ContainerBuilder().withImage("image").build())
            .and()
            .build();

    k8sEnv.addInjectablePod("r", "i", injectedPod);

    // when
    sshKeysProvisioner.provision(k8sEnv, runtimeIdentity);

    // then
    assertEquals(pod.getSpec().getVolumes().size(), 2);
    assertEquals(injectedPod.getSpec().getVolumes().size(), 0);

    Container podContainer = pod.getSpec().getContainers().get(0);
    Container injectedPodContainer = injectedPod.getSpec().getContainers().get(0);

    assertEquals(podContainer.getVolumeMounts().size(), 2);
    assertEquals(injectedPodContainer.getVolumeMounts().size(), 2);
  }

  @Test
  public void addInternalSshKeysConfigInPod() throws Exception {
    String keyName = UUID.randomUUID().toString();
    lenient()
        .when(sshManager.getPairs(someUser, "internal"))
        .thenReturn(
            ImmutableList.of(new SshPairImpl(someUser, "internal", keyName, "public", "private")));

    // should exist at least one 'vcs' key by design
    when(sshManager.generatePair(eq(someUser), eq("vcs"), anyString()))
        .thenReturn(
            new SshPairImpl(
                someUser, "vcs", "default-" + UUID.randomUUID().toString(), "public", "private"));

    sshKeysProvisioner.provision(k8sEnv, runtimeIdentity);

    verify(podSpec, times(2)).getVolumes();
    verify(podSpec, times(2)).getContainers();

    Secret secret = k8sEnv.getSecrets().get("wksp-sshprivatekeys");
    assertNotNull(secret);
    assertEquals(secret.getType(), "opaque");
    String key = secret.getData().get(keyName);
    assertNotNull(key);
    assertEquals("private", new String(Base64.getDecoder().decode(key)));
  }
}
