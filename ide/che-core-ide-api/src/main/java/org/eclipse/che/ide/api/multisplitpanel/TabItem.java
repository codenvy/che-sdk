/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 ******************************************************************************/
package org.eclipse.che.ide.api.multisplitpanel;

import com.google.gwt.event.dom.client.ClickHandler;

import org.eclipse.che.ide.api.mvp.View;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * //
 *
 * @author Artem Zatsarynnyi
 */
public interface TabItem extends View<TabItem.ActionDelegate>, ClickHandler {

    SVGResource getIcon();

    String getTitleText();

    void select();

    void unSelect();

    interface ActionDelegate {

        void onTabClicked(TabItem tab);

        /** Called just before the {@code tab} closes. */
        void onTabClosing(TabItem tab);
    }
}
