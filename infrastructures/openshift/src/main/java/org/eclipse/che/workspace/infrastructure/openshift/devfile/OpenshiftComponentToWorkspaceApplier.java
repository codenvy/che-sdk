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
package org.eclipse.che.workspace.infrastructure.openshift.devfile;

import static org.eclipse.che.workspace.infrastructure.kubernetes.devfile.KubernetesDevfileBindings.KUBERNETES_BASED_COMPONENTS_KEY_NAME;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.che.workspace.infrastructure.kubernetes.devfile.KubernetesComponentToWorkspaceApplier;
import org.eclipse.che.workspace.infrastructure.kubernetes.devfile.KubernetesEnvironmentProvisioner;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesRecipeParser;
import org.eclipse.che.workspace.infrastructure.openshift.environment.OpenShiftEnvironment;

public class OpenshiftComponentToWorkspaceApplier extends KubernetesComponentToWorkspaceApplier {
  @Inject
  public OpenshiftComponentToWorkspaceApplier(
      KubernetesRecipeParser objectsParser,
      KubernetesEnvironmentProvisioner k8sEnvProvisioner,
      @Named("che.workspace.projects.storage") String projectFolderPath,
      @Named("che.workspace.projects.storage.default.size") String defaultProjectPVCSize,
      @Named("che.infra.kubernetes.pvc.access_mode") String defaultPVCAccessMode,
      @Named(KUBERNETES_BASED_COMPONENTS_KEY_NAME) Set<String> kubernetesBasedComponentTypes) {
    super(
        objectsParser,
        k8sEnvProvisioner,
        OpenShiftEnvironment.TYPE,
        projectFolderPath,
        defaultProjectPVCSize,
        defaultPVCAccessMode,
        kubernetesBasedComponentTypes);
  }
}
