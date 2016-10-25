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
package org.eclipse.che.providers;

import com.google.inject.Provider;

import org.eclipse.che.commons.annotation.Nullable;

/**
 * Provider that can create instance of some object by class name.
 *
 *
 * @author Evgen Vidolob
 */
public interface DynaProvider {

    /**
     * Get provider for class name.
     * @param className the class name: {@link Class#getName()}
     * @param <T> the type
     * @return the provider for class
     */
    @Nullable
    <T> Provider<T> getProvider(String className);
}
