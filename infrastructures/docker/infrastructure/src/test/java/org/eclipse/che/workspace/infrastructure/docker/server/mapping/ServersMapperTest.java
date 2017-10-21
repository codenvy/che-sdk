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
package org.eclipse.che.workspace.infrastructure.docker.server.mapping;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.testng.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.eclipse.che.api.core.model.workspace.config.ServerConfig;
import org.eclipse.che.api.workspace.server.model.impl.ServerConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.ServerImpl;
import org.eclipse.che.infrastructure.docker.client.json.PortBinding;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Tests {@link ServersMapper}. */
public class ServersMapperTest {

  private final String hostname = "localhost";
  private final ServersMapper mapper = new ServersMapper(hostname);

  @Test(dataProvider = "servers")
  public void mapsServers(
      Map<String, String> dockerBindings,
      Map<String, ServerConfig> configs,
      Map<String, ServerImpl> expected) {
    Map<String, List<PortBinding>> bindings = createBindings(dockerBindings);

    Map<String, ServerImpl> result = mapper.map(bindings, configs);

    assertEquals(result, expected);
  }

  @DataProvider(name = "servers")
  private Object[][] serversProvider() {
    return new Object[][] {
      {
        ImmutableMap.of(
            "8080/tcp", "0.0.0.0:32080",
            "8081/tcp", "0.0.0.0:32081"),
        ImmutableMap.of(
            "server1", new ServerConfigImpl("8080", "http", "no-slash-path"),
            "server2", new ServerConfigImpl("8081", "http", "/slash-path")),
        ImmutableMap.of(
            "server1", new ServerImpl("http://" + hostname + ":32080/no-slash-path"),
            "server2", new ServerImpl("http://" + hostname + ":32081/slash-path"))
      },
      {
        ImmutableMap.of("8080/tcp", "0.0.0.0:32080"),
        ImmutableMap.of(
            "server1", new ServerConfigImpl("8080", "http", "http-endpoint"),
            "server2", new ServerConfigImpl("8080", "ws", "ws-endpoint")),
        ImmutableMap.of(
            "server1", new ServerImpl("http://" + hostname + ":32080/http-endpoint"),
            "server2", new ServerImpl("ws://" + hostname + ":32080/ws-endpoint"))
      },
      {
        ImmutableMap.of("8080/tcp", "0.0.0.0:32080"),
        ImmutableMap.of(
            "server1", new ServerConfigImpl("8080", "http", "http-endpoint"),
            "server2", new ServerConfigImpl("8080/tcp", "ws", "ws-endpoint")),
        ImmutableMap.of(
            "server1", new ServerImpl("http://" + hostname + ":32080/http-endpoint"),
            "server2", new ServerImpl("ws://" + hostname + ":32080/ws-endpoint"))
      },
      {
        ImmutableMap.of("8080/tcp", "0.0.0.0:32080"),
        ImmutableMap.of(),
        ImmutableMap.of("8080/tcp", new ServerImpl("tcp://" + hostname + ":32080"))
      },
      {
        ImmutableMap.of(
            "8080/tcp", "0.0.0.0:32080",
            "8081/udp", "0.0.0.0:32081",
            "8082", "0.0.0.0:32082"),
        ImmutableMap.of(),
        ImmutableMap.of(
            "8080/tcp", new ServerImpl("tcp://" + hostname + ":32080"),
            "8081/udp", new ServerImpl("udp://" + hostname + ":32081"),
            "8082/tcp", new ServerImpl("tcp://" + hostname + ":32082"))
      },
      {
        ImmutableMap.of(
            "8000/tcp", "0.0.0.0:32000",
            "8080/tcp", "0.0.0.0:32080",
            "2288/udp", "0.0.0.0:32288",
            "4401/tcp", "0.0.0.0:32401"),
        ImmutableMap.of(
            "ws-master", new ServerConfigImpl("8080", "http", "/api"),
            "exec-agent-api", new ServerConfigImpl("4401", "http", "/process"),
            "exec-agent-ws", new ServerConfigImpl("4401", "ws", "/connect")),
        ImmutableMap.of(
            "ws-master", new ServerImpl("http://" + hostname + ":32080/api"),
            "exec-agent-api", new ServerImpl("http://" + hostname + ":32401/process"),
            "exec-agent-ws", new ServerImpl("ws://" + hostname + ":32401/connect"),
            "8000/tcp", new ServerImpl("tcp://" + hostname + ":32000"),
            "2288/udp", new ServerImpl("udp://" + hostname + ":32288"))
      }
    };
  }

  private static Map<String, List<PortBinding>> createBindings(Map<String, String> bindings) {
    return bindings
        .entrySet()
        .stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> {
                  String[] split = entry.getValue().split(":");
                  PortBinding pb = new PortBinding(split[0], split[1]);
                  return singletonList(pb);
                }));
  }
}
