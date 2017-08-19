/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat, Inc. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.api.editor;

import com.google.gwt.event.shared.EventHandler;

/**
 * Handles {@link EditorOpenedEvent}.
 *
 * @author Anatoliy Bazko
 */
public interface EditorOpenedEventHandler extends EventHandler {

  /**
   * On editor opened.
   *
   * @param event the event
   */
  void onEditorOpened(EditorOpenedEvent event);
}
