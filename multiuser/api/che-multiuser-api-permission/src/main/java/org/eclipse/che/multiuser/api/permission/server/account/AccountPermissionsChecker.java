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
package org.eclipse.che.multiuser.api.permission.server.account;

import org.eclipse.che.api.core.ForbiddenException;

/**
 * Defines permissions checking for accounts with some type.
 *
 * @author Sergii Leshchenko
 */
public interface AccountPermissionsChecker {
  /**
   * Checks that current subject is authorized to perform given operation with specified account
   *
   * @param accountId account to check
   * @param operation operation that is going to be performed
   * @throws ForbiddenException when user doesn't have permissions to perform specified operation
   */
  void checkPermissions(String accountId, AccountOperation operation) throws ForbiddenException;

  /** Returns account type for which this class tracks check resources permissions. */
  String getAccountType();
}
