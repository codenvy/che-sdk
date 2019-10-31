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
package org.eclipse.che.multiuser.api.authentication.commons;

import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;

/**
 * Thread safe {@link HttpSession} storage based on {@link ConcurrentHashMap}.
 * Sessions are stored per user Id.
 */
@Singleton
public class SessionStore {

  private final ConcurrentHashMap<String, HttpSession> userIdToSession;

  public SessionStore() {
    this.userIdToSession = new ConcurrentHashMap<>();
  }

  public HttpSession getSession(String userId) {
    return userIdToSession.get(userId);
  }

  public HttpSession saveSession(String userId, HttpSession session) {
    return userIdToSession.put(userId, session);
  }

  public void remove(String userId) {
    userIdToSession.remove(userId);
  }
}
