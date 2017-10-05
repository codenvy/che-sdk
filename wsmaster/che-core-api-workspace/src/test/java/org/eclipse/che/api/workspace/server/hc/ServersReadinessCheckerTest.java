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
package org.eclipse.che.api.workspace.server.hc;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import com.google.common.collect.ImmutableMap;
import java.net.URL;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.model.impl.ServerImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/** @author Alexander Garagatyi */
@Listeners(MockitoTestNGListener.class)
public class ServersReadinessCheckerTest {
  private static final String MACHINE_NAME = "mach1";

  @Mock private Consumer<String> readinessHandler;
  @Mock private ServerCheckerFactory factory;
  @Mock private HttpConnectionServerChecker connectionChecker;
  @Mock private RuntimeIdentity runtimeIdentity;

  private CompletableFuture<String> compFuture;
  private ServersReadinessChecker checker;

  @BeforeMethod
  public void setUp() throws Exception {
    compFuture = new CompletableFuture<>();

    when(factory.httpChecker(any(URL.class), any(), anyString(), anyString(), any(Timer.class)))
        .thenReturn(connectionChecker);
    when(connectionChecker.getReportCompFuture()).thenReturn(compFuture);

    checker =
        new ServersReadinessChecker(runtimeIdentity, MACHINE_NAME, getDefaultServers(), factory);
  }

  @AfterMethod(timeOut = 1000)
  public void tearDown() throws Exception {
    try {
      checker.await();
    } catch (Exception ignored) {
    }
  }

  @Test(timeOut = 1000)
  public void shouldNotifyReadinessHandlerAboutEachServerReadiness() throws Exception {
    checker.startAsync(readinessHandler);

    verify(readinessHandler, timeout(500).never()).accept(anyString());

    connectionChecker.getReportCompFuture().complete("test_ref");

    verify(readinessHandler, times(3)).accept("test_ref");
  }

  @Test(timeOut = 1000)
  public void shouldThrowExceptionIfAServerIsUnavailable() throws Exception {
    checker.startAsync(readinessHandler);

    connectionChecker
        .getReportCompFuture()
        .completeExceptionally(new InfrastructureException("my exception"));

    try {
      checker.await();
    } catch (InfrastructureException e) {
      assertEquals(e.getMessage(), "my exception");
    }
  }

  @Test(timeOut = 1000)
  public void shouldNotCheckNotHardcodedServers() throws Exception {
    Map<String, ServerImpl> servers =
        ImmutableMap.of(
            "wsagent/http", new ServerImpl("http://localhost"),
            "not-hardcoded", new ServerImpl("http://localhost"));
    checker = new ServersReadinessChecker(runtimeIdentity, MACHINE_NAME, servers, factory);

    checker.startAsync(readinessHandler);
    connectionChecker.getReportCompFuture().complete("test_ref");
    checker.await();

    verify(readinessHandler).accept("test_ref");
  }

  @Test(timeOut = 1000)
  public void awaitShouldReturnOnFirstUnavailability() throws Exception {
    CompletableFuture<String> future1 = spy(new CompletableFuture<>());
    CompletableFuture<String> future2 = spy(new CompletableFuture<>());
    CompletableFuture<String> future3 = spy(new CompletableFuture<>());
    when(connectionChecker.getReportCompFuture())
        .thenReturn(future1)
        .thenReturn(future2)
        .thenReturn(future3);

    checker.startAsync(readinessHandler);

    future2.completeExceptionally(new InfrastructureException("error"));

    try {
      checker.await();
      fail();
    } catch (InfrastructureException ignored) {
      verify(future1, never()).complete(anyString());
      verify(future2, never()).complete(anyString());
      verify(future3, never()).complete(anyString());
      verify(future1, never()).completeExceptionally(any(Throwable.class));
      verify(future2).completeExceptionally(any(Throwable.class));
      verify(future3, never()).completeExceptionally(any(Throwable.class));
    }
  }

  @Test(
    expectedExceptions = InfrastructureException.class,
    expectedExceptionsMessageRegExp = "oops!"
  )
  public void throwsExceptionIfAnyServerIsNotAvailable() throws InfrastructureException {
    doThrow(new InfrastructureException("oops!")).when(connectionChecker).checkOnce(anyObject());

    checker.checkOnce(ref -> {});
  }

  Map<String, ServerImpl> getDefaultServers() {
    return ImmutableMap.of(
        "wsagent/http", new ServerImpl("http://localhost"),
        "exec-agent/http", new ServerImpl("http://localhost"),
        "terminal", new ServerImpl("http://localhost"));
  }
}
