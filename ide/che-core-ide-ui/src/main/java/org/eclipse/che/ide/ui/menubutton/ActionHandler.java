/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat, Inc. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.ui.menubutton;

/** Handles actions on {@link MenuItem}s. */
public interface ActionHandler {

  /**
   * Called when action on the {@code item} has been requested.
   *
   * @param item the item on which action has been requested
   */
  void onAction(MenuItem item);
}
