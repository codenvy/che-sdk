/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.workspace.server;

/**
 * Provides machine token that should be used for access to workspace master from machine.
 *
 * @author Sergii Leshchenko
 */
public interface MachineTokenProvider {

  /**
   * Returns machine token for specified workspace.
   *
   * @param workspaceId identifier of workspace to fetch token
   */
  String getToken(String workspaceId);

  /** Returns empty string as machine token. */
  class EmptyMachineTokenProvider implements MachineTokenProvider {
    @Override
    public String getToken(String workspaceId) {
      return "";
    }
  }
}
