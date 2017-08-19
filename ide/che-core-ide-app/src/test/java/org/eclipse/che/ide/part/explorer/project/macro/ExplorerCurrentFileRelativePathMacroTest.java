/**
 * ***************************************************************************** Copyright (c)
 * 2012-2017 Red Hat, Inc. All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Red Hat, Inc. - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.che.ide.part.explorer.project.macro;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.google.common.base.Joiner;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for the {@link ExplorerCurrentFileRelativePathMacro}
 *
 * @author Vlad Zhukovskyi
 */
@RunWith(GwtMockitoTestRunner.class)
public class ExplorerCurrentFileRelativePathMacroTest extends AbstractExplorerMacroTest {

  private ExplorerCurrentFileRelativePathMacro provider;

  @Before
  public void init() throws Exception {
    super.init();
    provider =
        new ExplorerCurrentFileRelativePathMacro(
            projectExplorer, promiseProvider, localizationConstants);
  }

  @Test
  public void testGetKey() throws Exception {
    assertSame(provider.getName(), ExplorerCurrentFileRelativePathMacro.KEY);
  }

  @Test
  public void getValue() throws Exception {
    initWithOneFile();

    provider.expand();

    verify(promiseProvider).resolve(eq(FILE_1_PATH));
  }

  @Test
  public void getMultipleValues() throws Exception {
    initWithTwoFiles();

    provider.expand();

    verify(promiseProvider).resolve(eq(Joiner.on(", ").join(FILE_1_PATH, FILE_2_PATH)));
  }

  @Test
  public void getEmptyValues() throws Exception {
    initWithNoFiles();

    provider.expand();

    verify(promiseProvider).resolve(eq(""));
  }
}
