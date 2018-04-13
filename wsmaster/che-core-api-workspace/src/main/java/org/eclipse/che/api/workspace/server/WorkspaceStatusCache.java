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
package org.eclipse.che.api.workspace.server;

import java.util.Map;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;

/** @author Anton Korneta */
public interface WorkspaceStatusCache {

  /**
   * Returns {@link WorkspaceStatus} mapped to given workspace id. When no mapping for given
   * workspace id found then {@code null} will be returned.
   *
   * @param workspaceId workspace identifier
   * @return workspace status mapped to given workspace id
   */
  WorkspaceStatus get(String workspaceId);

  /**
   * Replaces workspace status with given value and returns previous value mapped to given workspace
   * id. When no workspace status mapped to given workspace id then {@code null} will be returned.
   *
   * @param workspaceId workspace identifier
   * @param newStatus new workspace status
   * @return old workspace status or {@code null} if no previously mapped value
   */
  WorkspaceStatus replace(String workspaceId, WorkspaceStatus newStatus);

  /**
   * Replaces workspace status with given new value only if currently mapped to the specified value.
   *
   * @param workspaceId workspace identifier
   * @param prevStatus expected workspace status mapped to given id
   * @param newStatus new workspace status that is going to be mapped to given id
   * @return {@code true} when workspace status is replaced
   */
  boolean replace(String workspaceId, WorkspaceStatus prevStatus, WorkspaceStatus newStatus);

  /**
   * Removes workspace status mapped to given workspace id.
   *
   * @param workspaceId workspace identifier
   * @return workspace status mapped to given workspace id or {@code null} when no mapped value for
   *     given workspace id
   */
  WorkspaceStatus remove(String workspaceId);

  /**
   * Puts workspace status when specified key is not already mapped.
   *
   * @param workspaceId workspace identifier
   * @param status workspace status
   * @return previous workspace status or {@code null} mapped with given workspace id
   */
  WorkspaceStatus putIfAbsent(String workspaceId, WorkspaceStatus status);

  /** Returns copy of this cache in map representation */
  Map<String, WorkspaceStatus> toMap();
}
