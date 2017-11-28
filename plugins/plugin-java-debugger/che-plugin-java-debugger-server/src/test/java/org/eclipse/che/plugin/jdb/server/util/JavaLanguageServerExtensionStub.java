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
package org.eclipse.che.plugin.jdb.server.util;

import static java.util.Collections.singletonList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import org.eclipse.che.api.core.jsonrpc.commons.RequestHandlerConfigurator;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistry;
import org.eclipse.che.jdt.ls.extension.api.dto.LocationParameters;
import org.eclipse.che.plugin.java.languageserver.JavaLanguageServerExtensionService;
import org.mockito.Mockito;

import java.util.List;

/** @author Anatolii Bazko */
public class JavaLanguageServerExtensionStub extends JavaLanguageServerExtensionService {
  private static final String SRC = "/test/src/";

  public JavaLanguageServerExtensionStub() {
    super(
        mock(
            LanguageServerRegistry.class, Mockito.withSettings().defaultAnswer(RETURNS_DEEP_STUBS)),
        mock(
            RequestHandlerConfigurator.class,
            Mockito.withSettings().defaultAnswer(RETURNS_DEEP_STUBS)));
  }

  @Override
  public String debuggerLocationToFqn(LocationParameters params) {
    String target = params.getTarget();
    int srcPos = target.indexOf(SRC);
    int beginIndex = srcPos + SRC.length();
    int endIndex = target.endsWith(".java") ? target.length() - 5 : target.length();

    return srcPos == -1 ? target : target.substring(beginIndex, endIndex).replace("/", ".");
  }

  @Override
  public List<LocationParameters> fqnToDebuggerLocation(String fqn, Integer lineNumber) {
    int innerClassStartPos = fqn.indexOf("$");
    if (innerClassStartPos != -1) {
      fqn = fqn.substring(0, innerClassStartPos);
    }

    if (fqn.startsWith("org.eclipse")) {
      return singletonList(
          new LocationParameters(SRC + fqn.replace(".", "/") + ".java", lineNumber, "/test"));
    } else {
      return singletonList(new LocationParameters(fqn, lineNumber, null));
    }
  }
}
