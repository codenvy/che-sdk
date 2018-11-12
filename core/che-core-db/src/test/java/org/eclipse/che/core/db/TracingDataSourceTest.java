/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.core.db;

import static org.testng.Assert.*;

import com.google.common.collect.ImmutableMap;
import io.opentracing.contrib.jdbc.TracingConnection;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(value = {MockitoTestNGListener.class})
public class TracingDataSourceTest {

  @Mock DataSource dataSource;

  @BeforeMethod
  @AfterMethod
  public void cleanup() throws Exception {
    HashMap<String, String> newEnv = new HashMap<>(System.getenv());
    newEnv.remove("CHE_TRACING_ENABLED");
    setEnv(newEnv);
  }

  @Test
  public void testGetConnection() throws SQLException {
    TracingDataSource ds = new TracingDataSource(dataSource);

    Connection actual = ds.getConnection();
    assertEquals(actual.getClass(), TracingConnection.class);
    Mockito.verify(dataSource).getConnection();
  }

  @Test
  public void testGetConnection1() throws SQLException {
    TracingDataSource ds = new TracingDataSource(dataSource);

    Connection actual = ds.getConnection("user", "password");
    assertEquals(actual.getClass(), TracingConnection.class);
    Mockito.verify(dataSource).getConnection(Mockito.eq("user"), Mockito.eq("password"));
  }

  @Test
  public void testWrapWithTracingIfEnabled() throws Exception {
    setEnv(ImmutableMap.of("CHE_TRACING_ENABLED", "true"));

    DataSource actual = TracingDataSource.wrapWithTracingIfEnabled(dataSource);

    assertEquals(actual.getClass(), TracingDataSource.class);
  }

  @Test
  public void testShouldNotWrapWithTracingIfEnabled() throws Exception {
    setEnv(ImmutableMap.of("CHE_TRACING_ENABLED", "false"));

    DataSource actual = TracingDataSource.wrapWithTracingIfEnabled(dataSource);

    assertEquals(actual, dataSource);
  }

  @Test
  public void testShouldNot2WrapWithTracingIfEnabled() throws Exception {
    DataSource actual = TracingDataSource.wrapWithTracingIfEnabled(dataSource);

    assertEquals(actual, dataSource);
  }

  protected static void setEnv(Map<String, String> newenv) throws Exception {
    try {
      Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
      Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
      theEnvironmentField.setAccessible(true);
      Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
      env.putAll(newenv);
      Field theCaseInsensitiveEnvironmentField =
          processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
      theCaseInsensitiveEnvironmentField.setAccessible(true);
      Map<String, String> cienv =
          (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
      cienv.putAll(newenv);
    } catch (NoSuchFieldException e) {
      Class[] classes = Collections.class.getDeclaredClasses();
      Map<String, String> env = System.getenv();
      for (Class cl : classes) {
        if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
          Field field = cl.getDeclaredField("m");
          field.setAccessible(true);
          Object obj = field.get(env);
          Map<String, String> map = (Map<String, String>) obj;
          map.clear();
          map.putAll(newenv);
        }
      }
    }
  }
}
