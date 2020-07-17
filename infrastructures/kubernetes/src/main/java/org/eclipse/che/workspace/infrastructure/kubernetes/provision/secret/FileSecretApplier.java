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
package org.eclipse.che.workspace.infrastructure.kubernetes.provision.secret;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.eclipse.che.workspace.infrastructure.kubernetes.provision.secret.SecretAsContainerResourceProvisioner.ANNOTATION_PREFIX;

import com.google.common.annotations.Beta;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.model.impl.devfile.ComponentImpl;
import org.eclipse.che.api.workspace.server.model.impl.devfile.VolumeImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment.PodData;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment.PodRole;
import org.eclipse.che.workspace.infrastructure.kubernetes.namespace.K8sVersion;
import org.eclipse.che.workspace.infrastructure.kubernetes.provision.GitConfigProvisioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mounts Kubernetes secret with specific annotations as an file in workspace containers. Via
 * devfile, allows per-component control of secret applying and path overrides using specific
 * property.
 */
@Beta
@Singleton
public class FileSecretApplier extends KubernetesSecretApplier<KubernetesEnvironment> {

  private static final Logger LOG = LoggerFactory.getLogger(FileSecretApplier.class);

  static final String ANNOTATION_MOUNT_PATH = ANNOTATION_PREFIX + "/" + "mount-path";
  static final String ANNOTATION_GIT_CREDENTIALS = ANNOTATION_PREFIX + "/" + "git-credential";

  private final K8sVersion k8sVersion;

  @Inject
  public FileSecretApplier(K8sVersion k8sVersion) {
    this.k8sVersion = k8sVersion;
  }

  /**
   * Applies secret as file into workspace containers, respecting automount attribute and optional
   * devfile automount property and/or mount path override.
   *
   * @param env kubernetes environment with workspace containers configuration
   * @param runtimeIdentity identity of current runtime
   * @param secret source secret to apply
   * @throws InfrastructureException on misconfigured secrets or other apply error
   */
  @Override
  public void applySecret(KubernetesEnvironment env, RuntimeIdentity runtimeIdentity, Secret secret)
      throws InfrastructureException {
    final String secretMountPath = secret.getMetadata().getAnnotations().get(ANNOTATION_MOUNT_PATH);
    boolean secretAutomount =
        Boolean.parseBoolean(secret.getMetadata().getAnnotations().get(ANNOTATION_AUTOMOUNT));
    if (secretMountPath == null) {
      throw new InfrastructureException(
          format(
              "Unable to mount secret '%s': It is configured to be mounted as a file but the mount path was not specified. Please define the '%s' annotation on the secret to specify it.",
              secret.getMetadata().getName(), ANNOTATION_MOUNT_PATH));
    }
    Volume volumeFromSecret =
        new VolumeBuilder()
            .withName(secret.getMetadata().getName())
            .withSecret(
                new SecretVolumeSourceBuilder()
                    .withNewSecretName(secret.getMetadata().getName())
                    .build())
            .build();
    Path gitSecretFilePath = null;
    for (PodData podData : env.getPodsData().values()) {
      if (!podData.getRole().equals(PodRole.DEPLOYMENT)) {
        continue;
      }
      if (podData
          .getSpec()
          .getVolumes()
          .stream()
          .anyMatch(v -> v.getName().equals(volumeFromSecret.getName()))) {
        volumeFromSecret.setName(volumeFromSecret.getName() + "_" + NameGenerator.generate("", 6));
      }

      podData.getSpec().getVolumes().add(volumeFromSecret);

      for (Container container : podData.getSpec().getContainers()) {
        Optional<ComponentImpl> component = getComponent(env, container.getName());
        // skip components that explicitly disable automount
        if (component.isPresent() && isComponentAutomountFalse(component.get())) {
          continue;
        }
        // if automount disabled globally and not overridden in component
        if (!secretAutomount
            && (!component.isPresent() || !isComponentAutomountTrue(component.get()))) {
          continue;
        }
        // find path override if any
        Optional<String> overridePathOptional = Optional.empty();
        if (component.isPresent()) {
          overridePathOptional =
              getOverridenComponentPath(component.get(), secret.getMetadata().getName());
        }
        final String componentMountPath = overridePathOptional.orElse(secretMountPath);
        // it's not possible to mount multiple volumes on same path on older k8s, so we
        // remove the existing mount here to replace it with new one.
        if (k8sVersion.olderThan(1, 13)) {
          LOG.debug(
              "Unable to mount multiple VolumeMounts on same path on this k8s version. Removing conflicting volumes in favor of secret mounts.");
          container
              .getVolumeMounts()
              .removeIf(vm -> Paths.get(vm.getMountPath()).equals(Paths.get(componentMountPath)));
        }

        container
            .getVolumeMounts()
            .addAll(
                secret
                    .getData()
                    .keySet()
                    .stream()
                    .map(
                        secretFile ->
                            buildVolumeMount(volumeFromSecret, componentMountPath, secretFile))
                    .collect(Collectors.toList()));

        if (Boolean.parseBoolean(
            secret.getMetadata().getAnnotations().get(ANNOTATION_GIT_CREDENTIALS))) {
          Set<String> keys = secret.getData().keySet();
          if (keys.size() != 1) {
            throw new InfrastructureException(
                format(
                    "Invalid git credential secret data. It should contain only 1 data item but it have %d",
                    keys.size()));
          }
          gitSecretFilePath = Paths.get(secretMountPath, keys.iterator().next());
        }
      }
    }

    if (gitSecretFilePath != null) {
      ConfigMap gitConfigMap =
          env.getConfigMaps()
              .get(
                  runtimeIdentity.getWorkspaceId()
                      + GitConfigProvisioner.GIT_CONFIG_MAP_NAME_SUFFIX);
      if (gitConfigMap != null) {
        Map<String, String> gitConfigMapData = gitConfigMap.getData();
        String gitConfig = gitConfigMapData.get(GitConfigProvisioner.GIT_CONFIG);
        if (gitConfig != null) {
          StringBuilder gitConfigBuilder = new StringBuilder(gitConfig);
          gitConfigBuilder
              .append('\n')
              .append("[credential]")
              .append('\n')
              .append('\t')
              .append("helper = store --file ")
              .append(gitSecretFilePath.toString())
              .append('\n');
          HashMap<String, String> newGitConfigMapData = new HashMap<>(gitConfigMapData);
          newGitConfigMapData.put(GitConfigProvisioner.GIT_CONFIG, gitConfigBuilder.toString());
          gitConfigMap.setData(newGitConfigMapData);
        }
      }
    }
  }

  private VolumeMount buildVolumeMount(
      Volume volumeFromSecret, String componentMountPath, String secretFile) {
    VolumeMountBuilder volumeMountBuilder =
        new VolumeMountBuilder().withName(volumeFromSecret.getName()).withReadOnly(true);

    // subPaths are supported from k8s v1.13
    if (k8sVersion.newerOrEqualThan(1, 13)) {
      volumeMountBuilder
          .withMountPath(componentMountPath + "/" + secretFile)
          .withSubPath(secretFile);
    } else {
      volumeMountBuilder.withMountPath(componentMountPath);
      LOG.debug("This version of k8s does not support sutPaths for VolumeMounts.");
    }

    return volumeMountBuilder.build();
  }

  private Optional<String> getOverridenComponentPath(ComponentImpl component, String secretName) {
    Optional<VolumeImpl> matchedVolume =
        component.getVolumes().stream().filter(v -> v.getName().equals(secretName)).findFirst();
    if (matchedVolume.isPresent() && !isNullOrEmpty(matchedVolume.get().getContainerPath())) {
      return Optional.of(matchedVolume.get().getContainerPath());
    }
    return Optional.empty();
  }
}
