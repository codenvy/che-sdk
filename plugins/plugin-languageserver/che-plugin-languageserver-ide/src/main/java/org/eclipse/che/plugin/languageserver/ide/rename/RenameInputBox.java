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

package org.eclipse.che.plugin.languageserver.ide.rename;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import elemental.events.KeyboardEvent;
import javax.inject.Inject;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.ide.runtime.OperationCanceledException;

/** */
class RenameInputBox extends PopupPanel {

  private final PromiseProvider promiseProvider;
  private TextBox valueTextBox;

  @Inject
  public RenameInputBox(PromiseProvider promiseProvider) {
    super(true, true);
    this.promiseProvider = promiseProvider;
    valueTextBox = new TextBox();
    valueTextBox.addStyleName("orionCodenvy");
    setWidget(valueTextBox);
  }

  public Promise<String> setPositionAndShow(int x, int y, String value) {
    setPopupPosition(x, y);
    valueTextBox.setValue(value);
    return promiseProvider.create(
        callback -> {
          show();

          HandlerRegistration registration =
              addCloseHandler(
                  event -> {
                    if (event.isAutoClosed()) {
                      callback.onFailure(new OperationCanceledException());
                    }
                  });

          KeyDownHandler handler =
              event -> {
                if (KeyboardEvent.KeyCode.ESC == event.getNativeEvent().getKeyCode()) {
                  event.stopPropagation();
                  event.preventDefault();
                  callback.onFailure(new OperationCanceledException());
                  registration.removeHandler();
                  hide(false);
                } else if (KeyboardEvent.KeyCode.ENTER == event.getNativeEvent().getKeyCode()) {
                  event.stopPropagation();
                  event.preventDefault();
                  registration.removeHandler();
                  hide(false);
                  callback.onSuccess(valueTextBox.getValue());
                }
              };
          valueTextBox.addDomHandler(handler, KeyDownEvent.getType());
        });
  }

  @Override
  public void show() {
    super.show();
    Scheduler.get()
        .scheduleDeferred(
            () -> {
              valueTextBox.selectAll();
              valueTextBox.setFocus(true);
            });
  }
}
