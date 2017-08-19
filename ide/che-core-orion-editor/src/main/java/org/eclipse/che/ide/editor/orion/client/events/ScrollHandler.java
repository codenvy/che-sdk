/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat, Inc. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.editor.orion.client.events;

import com.google.gwt.event.shared.EventHandler;

/**
 * Handler for {@link ScrollEvent}.
 *
 * @author "Mickaël Leduque"
 */
public interface ScrollHandler extends EventHandler {
  void onScroll(ScrollEvent event);
}
