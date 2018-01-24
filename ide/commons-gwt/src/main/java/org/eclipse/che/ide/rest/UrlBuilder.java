/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.ide.rest;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import java.util.function.Function;

/**
 * Extended {@link com.google.gwt.http.client.UrlBuilder} with constructor that consumes string url.
 *
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 * @version $Id: Aug 29, 2011 evgen $
 */
public class UrlBuilder extends com.google.gwt.http.client.UrlBuilder {

  private JSONObject o;

  /** */
  public UrlBuilder() {}

  /**
   * Parse url and set initial parameters(protocol, host, port, path)<br>
   *
   * @param url
   */
  public UrlBuilder(String url) {
    JavaScriptObject jso = parseUrl(url);
    o = new JSONObject(jso);
    setHost(o.get("host").isString().stringValue());
    setProtocol(o.get("protocol").isString().stringValue());
    if (o.containsKey("port")) {
      final String port = o.get("port").isString().stringValue();
      if (!port.isEmpty()) {
        setPort(Integer.valueOf(port));
      }
    }
    setPath(o.get("path").isString().stringValue());
    // fill query parameters
    JSONObject query = o.get("queryKey").isObject();
    for (String key : query.keySet()) {
      setParameter(key, query.get(key).isString().stringValue());
    }
  }

  public String getHost() {
    return String.valueOf(convert(o, "host", JSONValue::isNumber, JSONNumber::doubleValue));
  }

  public String getProtocol() {
    return convert(o, "protocol", JSONValue::isString, JSONString::stringValue);
  }

  public String getPort() {
    return convert(o, "port", JSONValue::isString, JSONString::stringValue);
  }

  public String getPath() {
    return convert(o, "path", JSONValue::isString, JSONString::stringValue);
  }

  public boolean containsPort() {
    return getPort() != null && !getPort().isEmpty();
  }

  public String getUrl() {
    return buildString();
  }

  private <T, V> V convert(JSONObject o, String key, Function<JSONValue, T> f, Function<T, V> g) {
    if (!o.containsKey(key)) {
      return null;
    }
    JSONValue v = o.get(key);
    T value = f.apply(v);
    if (value == null) {
      return null;
    }
    return g.apply(value);
  }

  private native JavaScriptObject parseUrl(String url) /*-{

        options = {
            strictMode: false,
            key: [ "source", "protocol", "authority", "userInfo", "user",
                "password", "host", "port", "relative", "path",
                "directory", "file", "query", "anchor" ],
            q: {
                name: "queryKey",
                parser: /(?:^|&)([^&=]*)=?([^&]*)/g
            },
            parser: {
                strict: /^(?:([^:\/?#]+):)?(?:\/\/((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:\/?#]*)(?::(\d*))?))?((((?:[^?#\/]*\/)*)([^?#]*))(?:\?([^#]*))?(?:#(.*))?)/,
                loose: /^(?:(?![^:@]+:[^:@\/]*@)([^:\/?#.]+):)?(?:\/\/)?((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:\/?#]*)(?::(\d*))?)(((\/(?:[^?#](?![^?#\/]*\.[^?#\/.]+(?:[?#]|$)))*\/?)?([^?#\/]*))(?:\?([^#]*))?(?:#(.*))?)/
            }
        }
        var o = options, m = o.parser[o.strictMode ? "strict" : "loose"]
            .exec(url), uri = {}, i = 14;

        while (i--)
            uri[o.key[i]] = m[i] || "";

        uri[o.q.name] = {};
        uri[o.key[12]].replace(o.q.parser, function ($0, $1, $2) {
            if ($1)
                uri[o.q.name][$1] = $2;
        });
        return uri;
    }-*/;
}
