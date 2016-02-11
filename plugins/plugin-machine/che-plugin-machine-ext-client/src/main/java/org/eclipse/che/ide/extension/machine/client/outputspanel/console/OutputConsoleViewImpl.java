/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.extension.machine.client.outputspanel.console;

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * View for {@link OutputConsole}.
 *
 * @author Artem Zatsarynnyi
 */
public class OutputConsoleViewImpl extends Composite implements OutputConsoleView {

    interface OutputConsoleViewUiBinder extends UiBinder<Widget, OutputConsoleViewImpl> {
    }

    private static final OutputConsoleViewUiBinder UI_BINDER = GWT.create(OutputConsoleViewUiBinder.class);

    private ActionDelegate delegate;

    @UiField
    DockLayoutPanel consolePanel;

    @UiField
    FlowPanel   commandPanel;

    @UiField
    FlowPanel   previewPanel;

    @UiField
    Label       commandTitle;

    @UiField
    Label       commandLabel;

    @UiField
    ScrollPanel scrollPanel;
    
    @UiField
    FlowPanel   consoleLines;

    @UiField
    Anchor      previewUrlLabel;
    
    /** scroll events to the bottom if view is visible */
    private boolean scrollBottomRequired = false;

    /** If true - next printed line should replace the previous one. */
    private boolean carriageReturn;

    @Inject
    public OutputConsoleViewImpl() {
        initWidget(UI_BINDER.createAndBindUi(this));
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void hideCommand() {
        consolePanel.setWidgetHidden(commandPanel, true);
    }

    @Override
    public void hidePreview() {
        consolePanel.setWidgetHidden(previewPanel, true);
    }

    @Override
    public void printCommandLine(String commandLine) {
        commandLabel.setText(commandLine);
    }

    @Override
    public void printPreviewUrl(String previewUrl) {
        if (!Strings.isNullOrEmpty(previewUrl)) {
            previewUrlLabel.setText(previewUrl);
            previewUrlLabel.setTitle(previewUrl);
            previewUrlLabel.setHref(previewUrl);
        } else {
            hidePreview();
        }
    }

    @Override
    public void print(String message, boolean cr) {
        if (carriageReturn) {
            Node lastChild = consoleLines.getElement().getLastChild();
            if (lastChild != null) {
                lastChild.removeFromParent();
            }
        }

        carriageReturn = cr;

        PreElement pre = DOM.createElement("pre").cast();
        pre.setInnerText(message.isEmpty() ? " " : message);
        consoleLines.getElement().appendChild(pre);
    }

    @Override
    public void scrollBottom() {
        /** scroll bottom immediately if view is visible */
        if (scrollPanel.getElement().getOffsetParent() != null) {
            scrollPanel.scrollToBottom();
            return;
        }

        /** otherwise, check the visibility periodically and scroll the view when it's visible */
        if (!scrollBottomRequired) {
            scrollBottomRequired = true;

            Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
                @Override
                public boolean execute() {
                    if (scrollPanel.getElement().getOffsetParent() != null) {
                        scrollPanel.scrollToBottom();
                        scrollBottomRequired = false;
                        return false;
                    }
                    return true;
                }
            }, 1000);
        }
    }


}
