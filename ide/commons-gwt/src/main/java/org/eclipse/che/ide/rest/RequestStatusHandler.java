/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat, Inc. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.rest;

/**
 * Callback interface, this interface needs to {@link AsyncRequest} can tell the application
 * execution state async REST Service
 *
 * @author Evgen Vidolob
 */
public interface RequestStatusHandler {

  /**
   * Calls when service started or in progress.
   *
   * @param id the Async REST Service id
   */
  void requestInProgress(String id);

  /**
   * Calls when service work done.
   *
   * @param id the Async REST Service id
   */
  void requestFinished(String id);

  /**
   * Calls when service return error
   *
   * @param id the Async REST Service id
   * @param exception the exception received from service
   */
  void requestError(String id, Throwable exception);
}
