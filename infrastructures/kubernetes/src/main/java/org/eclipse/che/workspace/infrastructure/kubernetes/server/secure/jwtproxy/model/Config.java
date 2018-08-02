/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which is available at http://www.eclipse.org/legal/epl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.kubernetes.server.secure.jwtproxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {
  @JsonProperty("jwtproxy")
  private JWTProxy jwtProxy;

  public JWTProxy getJwtProxy() {
    return jwtProxy;
  }

  public void setJwtProxy(JWTProxy jwtProxy) {
    this.jwtProxy = jwtProxy;
  }

  public Config withJWTProxy(JWTProxy jwtProxy) {
    this.jwtProxy = jwtProxy;
    return this;
  }
}
