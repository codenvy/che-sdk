/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat, Inc. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.security.oauth;

@SuppressWarnings("serial")
public final class OAuthAuthenticationException extends Exception {
  public OAuthAuthenticationException(String message) {
    super(message);
  }

  public OAuthAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
