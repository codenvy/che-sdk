/*******************************************************************************
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.editor.macro;

import com.google.common.annotations.Beta;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;

/**
 * Provider which is responsible for retrieving the relative to the {@code /projects} folder file path from the opened editor.
 *
 * Macro provided: <code>${editor.current.file.relpath}</code>
 *
 * @see AbstractEditorMacro
 * @see EditorAgent
 * @since 4.7.0
 */
@Beta
@Singleton
public class EditorCurrentFileRelativePathMacro extends AbstractEditorMacro {

    public static final String KEY = "${editor.current.file.relpath}";

    private PromiseProvider promises;
    private final CoreLocalizationConstant localizationConstants;

    @Inject
    public EditorCurrentFileRelativePathMacro(EditorAgent editorAgent,
                                              PromiseProvider promises,
                                              CoreLocalizationConstant localizationConstants) {
        super(editorAgent);
        this.promises = promises;
        this.localizationConstants = localizationConstants;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return KEY;
    }

    @Override
    public String getDescription() {
        return localizationConstants.macroEditorCurrentFileRelpathDescription();
    }

    /** {@inheritDoc} */
    @Override
    public Promise<String> expand() {
        final EditorPartPresenter editor = getActiveEditor();

        if (editor == null) {
            return promises.resolve("");
        }

        return promises.resolve(editor.getEditorInput().getFile().getLocation().toString());
    }
}
