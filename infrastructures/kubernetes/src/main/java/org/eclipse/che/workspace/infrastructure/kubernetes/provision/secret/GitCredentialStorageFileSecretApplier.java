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

import static java.lang.String.format;
import static org.eclipse.che.workspace.infrastructure.kubernetes.provision.secret.SecretAsContainerResourceProvisioner.ANNOTATION_PREFIX;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.namespace.K8sVersion;
import org.eclipse.che.workspace.infrastructure.kubernetes.provision.GitConfigProvisioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitCredentialStorageFileSecretApplier extends FileSecretApplier {

  private static final Logger LOG =
      LoggerFactory.getLogger(GitCredentialStorageFileSecretApplier.class);

  public static final String ANNOTATION_GIT_CREDENTIALS =
      ANNOTATION_PREFIX + "/" + "git-credential";

  @Inject
  public GitCredentialStorageFileSecretApplier(K8sVersion k8sVersion) {
    super(k8sVersion);
  }

  @Override
  public void applySecret(KubernetesEnvironment env, RuntimeIdentity runtimeIdentity, Secret secret)
      throws InfrastructureException {
    super.applySecret(env, runtimeIdentity, secret);
    final String secretMountPath = secret.getMetadata().getAnnotations().get(ANNOTATION_MOUNT_PATH);
    Set<String> keys = secret.getData().keySet();
    if (keys.size() != 1) {
      throw new InfrastructureException(
          format(
              "Invalid git credential secret data. It should contain only 1 data item but it have %d",
              keys.size()));
    }
    Path gitSecretFilePath = Paths.get(secretMountPath, keys.iterator().next());
    ConfigMap gitConfigMap =
        env.getConfigMaps()
            .get(
                runtimeIdentity.getWorkspaceId() + GitConfigProvisioner.GIT_CONFIG_MAP_NAME_SUFFIX);
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
        LOG.info(gitConfigBuilder.toString());
        gitConfigMap.setData(newGitConfigMapData);
      }
    }
  }
}
