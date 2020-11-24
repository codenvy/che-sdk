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
package org.eclipse.che.workspace.infrastructure.kubernetes;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.commons.annotation.Nullable;

public class DirectKubernetesAPIAccessHelper {
  private DirectKubernetesAPIAccessHelper() {}

  /**
   * This method just performs an HTTP request of given {@code httpMethod} on an URL composed of the
   * {@code masterUrl} and {@code relativeUri} using the provided {@code httpClient}, optionally
   * sending the provided {@code body}.
   *
   * @param masterUrl the base of the final URL
   * @param httpClient the HTTP client to perform the request with
   * @param httpMethod the HTTP method of the request
   * @param relativeUri the relative URI that should be appended ot the {@code masterUrl}
   * @param body the body to send with the request, if any
   * @return the HTTP response received
   * @throws InfrastructureException on failure to validate or perform the request
   */
  public static Response call(
      String masterUrl,
      OkHttpClient httpClient,
      String httpMethod,
      URI relativeUri,
      @Nullable HttpHeaders headers,
      @Nullable InputStream body)
      throws InfrastructureException {
    if (relativeUri.isAbsolute() || relativeUri.isOpaque()) {
      throw new InfrastructureException(
          "The direct infrastructure URL must be relative and not opaque.");
    }

    try {
      URI fullUrl = new URI(masterUrl).resolve(relativeUri);
      javax.ws.rs.core.MediaType mediaTypeHeader = headers == null ? null : headers.getMediaType();
      String mediaType =
          mediaTypeHeader == null ? "application/json;charset=utf-8" : mediaTypeHeader.toString();

      RequestBody requestBody =
          body == null ? null : new InputStreamBasedRequestBody(body, mediaType);

      Call httpCall =
          httpClient.newCall(
              new Request.Builder()
                  .url(fullUrl.toURL())
                  .method(httpMethod, requestBody)
                  .headers(toOkHttpHeaders(headers))
                  .build());

      okhttp3.Response response = httpCall.execute();
      Response.ResponseBuilder bld = Response.status(response.code());
      for (int i = 0; i < response.headers().size(); ++i) {
        String name = response.headers().name(i);
        String value = response.headers().value(i);
        bld.header(name, value);
      }

      ResponseBody responseBody = response.body();
      if (responseBody != null) {
        bld.entity(responseBody.byteStream());
        MediaType contentType = responseBody.contentType();
        if (contentType != null) {
          bld.type(contentType.toString());
        }
      }

      return bld.build();
    } catch (URISyntaxException | MalformedURLException e) {
      throw new InfrastructureException("Could not compose the direct URI.", e);
    } catch (IOException e) {
      throw new InfrastructureException("Error sending the direct infrastructure request.", e);
    }
  }

  private static Headers toOkHttpHeaders(HttpHeaders headers) {
    Headers.Builder bld = new Headers.Builder();

    if (headers != null) {
      for (Map.Entry<String, List<String>> e : headers.getRequestHeaders().entrySet()) {
        String name = e.getKey();
        List<String> values = e.getValue();
        for (String value : values) {
          bld.add(name, value);
        }
      }
    }

    return bld.build();
  }

  private static final class InputStreamBasedRequestBody extends RequestBody {
    private final InputStream inputStream;
    private final MediaType mediaType;

    private InputStreamBasedRequestBody(InputStream is, String contentType) {
      this.inputStream = is;
      this.mediaType = contentType == null ? null : MediaType.parse(contentType);
    }

    @Override
    public MediaType contentType() {
      return mediaType;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
      byte[] buffer = new byte[1024];
      int cnt;
      while ((cnt = inputStream.read(buffer)) != -1) {
        sink.write(buffer, 0, cnt);
      }
    }
  }
}
