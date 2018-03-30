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
package org.eclipse.che.workspace.infrastructure.kubernetes.cache;

import java.util.Set;
import java.util.function.Predicate;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;

/**
 * TODO Add docs TODO Add TCK
 *
 * @author Sergii Leshchenko
 */
public interface KubernetesRuntimeStateCache {

  Set<RuntimeIdentity> getIdentities() throws InfrastructureException;

  boolean initStatus(RuntimeIdentity identity, String namespace, WorkspaceStatus newStatus)
      throws InfrastructureException;

  void updateStatus(RuntimeIdentity runtimeIdentity, WorkspaceStatus status)
      throws InfrastructureException;

  void delete(RuntimeIdentity runtimeIdentity) throws InfrastructureException;

  WorkspaceStatus getStatus(RuntimeIdentity identity) throws InfrastructureException;

  boolean replaceStatus(
      RuntimeIdentity identity, Predicate<WorkspaceStatus> predicate, WorkspaceStatus newStatus)
      throws InfrastructureException;
}
