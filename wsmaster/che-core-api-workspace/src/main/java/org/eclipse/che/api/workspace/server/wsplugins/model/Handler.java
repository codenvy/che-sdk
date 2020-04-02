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
package org.eclipse.che.api.workspace.server.wsplugins.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** Handler defines a specific action that should be taken on postStart or preStop. */
public class Handler {

  @JsonProperty("exec")
  private Exec exec;

  public Handler exec(Exec exec) {
    this.exec = exec;
    return this;
  }

  public Exec getExec() {
    return exec;
  }

  public void setExec(Exec exec) {
    this.exec = exec;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Handler that = (Handler) o;
    return Objects.equals(getExec(), that.getExec());
  }

  @Override
  public int hashCode() {
    return Objects.hash(exec);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Handler {\n");
    sb.append("    exec: ").append(toIndentedString(exec)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
