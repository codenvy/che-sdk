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
package org.eclipse.che.ide.api.project;

import com.google.inject.Inject;

import org.eclipse.che.api.project.shared.dto.CopyOptions;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.MoveOptions;
import org.eclipse.che.api.project.shared.dto.SearchResultDto;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.api.project.shared.dto.TreeElement;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.shared.dto.NewProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.machine.WsAgentStateController;
import org.eclipse.che.ide.api.resources.SearchResult;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.StringUnmarshaller;
import org.eclipse.che.ide.rest.UrlBuilder;
import org.eclipse.che.ide.ui.loaders.request.LoaderFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.gwt.http.client.RequestBuilder.DELETE;
import static com.google.gwt.http.client.RequestBuilder.PUT;
import static com.google.gwt.safehtml.shared.UriUtils.encodeAllowEscapes;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * Implementation of {@link ProjectServiceClient}.
 *
 * <p>TODO need to remove interface as this component is internal one and couldn't have more than
 * one instance
 *
 * @author Vitaly Parfonov
 * @author Artem Zatsarynnyi
 * @author Valeriy Svydenko
 * @see ProjectServiceClient
 */
public class ProjectServiceClientImpl implements ProjectServiceClient {

  private static final String PROJECT = "/project";
  private static final String BATCH_PROJECTS = "/batch";

  private static final String ITEM = "/item";
  private static final String TREE = "/tree";
  private static final String MOVE = "/move";
  private static final String COPY = "/copy";
  private static final String FOLDER = "/folder";
  private static final String FILE = "/file";
  private static final String SEARCH = "/search";
  private static final String IMPORT = "/import";
  private static final String RESOLVE = "/resolve";
  private static final String ESTIMATE = "/estimate";

  private final WsAgentStateController wsAgentStateController;
  private final LoaderFactory loaderFactory;
  private final AsyncRequestFactory reqFactory;
  private final DtoFactory dtoFactory;
  private final DtoUnmarshallerFactory unmarshaller;
  private final AppContext appContext;

  @Inject
  protected ProjectServiceClientImpl(
      WsAgentStateController wsAgentStateController,
      LoaderFactory loaderFactory,
      AsyncRequestFactory reqFactory,
      DtoFactory dtoFactory,
      DtoUnmarshallerFactory unmarshaller,
      AppContext appContext) {
    this.wsAgentStateController = wsAgentStateController;
    this.loaderFactory = loaderFactory;
    this.reqFactory = reqFactory;
    this.dtoFactory = dtoFactory;
    this.unmarshaller = unmarshaller;
    this.appContext = appContext;
  }

  /** {@inheritDoc} */
  @Override
  public Promise<List<ProjectConfigDto>> getProjects() {
    final String url = getBaseUrl();

    return reqFactory
        .createGetRequest(url)
        .header(ACCEPT, MimeType.APPLICATION_JSON)
        .send(unmarshaller.newListUnmarshaller(ProjectConfigDto.class));
  }

  /** {@inheritDoc} */
  @Override
  public Promise<SourceEstimation> estimate(Path path, String pType) {
    final String url =
        encodeAllowEscapes(getBaseUrl() + ESTIMATE + path(path.toString()) + "?type=" + pType);

    return reqFactory
        .createGetRequest(url)
        .header(ACCEPT, MimeType.APPLICATION_JSON)
        .loader(loaderFactory.newLoader("Estimating project..."))
        .send(unmarshaller.newUnmarshaller(SourceEstimation.class));
  }

  /** {@inheritDoc} */
  @Override
  public Promise<List<SourceEstimation>> resolveSources(Path path) {
    final String url = encodeAllowEscapes(getBaseUrl() + RESOLVE + path(path.toString()));

    return reqFactory
        .createGetRequest(url)
        .header(ACCEPT, MimeType.APPLICATION_JSON)
        .loader(loaderFactory.newLoader("Resolving sources..."))
        .send(unmarshaller.newListUnmarshaller(SourceEstimation.class));
  }

  /** {@inheritDoc} */
  @Override
  public Promise<Void> importProject(Path path, SourceStorageDto source) {
    String url = encodeAllowEscapes(getBaseUrl() + IMPORT + path(path.toString()));

    return reqFactory.createPostRequest(url, source).header(CONTENT_TYPE, APPLICATION_JSON).send();
  }

  /** {@inheritDoc} */
  @Override
  public Promise<List<SearchResult>> search(QueryExpression expression) {
    final String url =
        encodeAllowEscapes(
            getBaseUrl()
                + SEARCH
                + (isNullOrEmpty(expression.getPath()) ? Path.ROOT : path(expression.getPath())));

    StringBuilder queryParameters = new StringBuilder();
    if (expression.getName() != null && !expression.getName().isEmpty()) {
      queryParameters.append("&name=").append(expression.getName());
    }
    if (expression.getText() != null && !expression.getText().isEmpty()) {
      queryParameters.append("&text=").append(expression.getText());
    }
    if (expression.getMaxItems() == 0) {
      expression.setMaxItems(100); //for avoiding block client by huge response until search not support pagination will limit result here
    }
    queryParameters.append("&maxItems=").append(expression.getMaxItems());
    if (expression.getSkipCount() != 0) {
      queryParameters.append("&skipCount=").append(expression.getSkipCount());
    }

    return reqFactory
        .createGetRequest(url + queryParameters.toString().replaceFirst("&", "?"))
        .header(ACCEPT, MimeType.APPLICATION_JSON)
        .loader(loaderFactory.newLoader("Searching..."))
        .send(unmarshaller.newListUnmarshaller(SearchResultDto.class))
        .then(
            (Function<List<SearchResultDto>, List<SearchResult>>)
                searchResultDtos -> {
                  if (searchResultDtos.isEmpty()) {
                    return Collections.emptyList();
                  }
                  return searchResultDtos
                      .stream()
                      .map(SearchResult::new)
                      .collect(Collectors.toList());
                });
  }

  /** {@inheritDoc} */
  @Override
  public Promise<ProjectConfigDto> createProject(
      ProjectConfigDto configuration, Map<String, String> options) {
    UrlBuilder urlBuilder = new UrlBuilder(getBaseUrl());
    for (String key : options.keySet()) {
      urlBuilder.setParameter(key, options.get(key));
    }
    return reqFactory
        .createPostRequest(urlBuilder.buildString(), configuration)
        .header(ACCEPT, MimeType.APPLICATION_JSON)
        .loader(loaderFactory.newLoader("Creating project..."))
        .send(unmarshaller.newUnmarshaller(ProjectConfigDto.class));
  }

  @Override
  public Promise<List<ProjectConfigDto>> createBatchProjects(
      List<NewProjectConfigDto> configurations) {
    final String url = encodeAllowEscapes(getBaseUrl() + BATCH_PROJECTS);
    final String loaderMessage =
        configurations.size() > 1 ? "Creating the batch of projects..." : "Creating project...";
    return reqFactory
        .createPostRequest(url, configurations)
        .header(ACCEPT, MimeType.APPLICATION_JSON)
        .loader(loaderFactory.newLoader(loaderMessage))
        .send(unmarshaller.newListUnmarshaller(ProjectConfigDto.class));
  }

  /** {@inheritDoc} */
  @Override
  public Promise<ItemReference> createFile(Path path, String content) {
    final String url =
        encodeAllowEscapes(
            getBaseUrl() + FILE + path(path.parent().toString()) + "?name=" + path.lastSegment());

    return reqFactory
        .createPostRequest(url, null)
        .data(content)
        .loader(loaderFactory.newLoader("Creating file..."))
        .send(unmarshaller.newUnmarshaller(ItemReference.class));
  }

  /** {@inheritDoc} */
  @Override
  public Promise<String> getFileContent(Path path) {
    final String url = encodeAllowEscapes(getBaseUrl() + FILE + path(path.toString()));

    return reqFactory.createGetRequest(url).send(new StringUnmarshaller());
  }

  /** {@inheritDoc} */
  @Override
  public Promise<Void> setFileContent(Path path, String content) {
    final String url = encodeAllowEscapes(getBaseUrl() + FILE + path(path.toString()));

    return reqFactory.createRequest(PUT, url, null, false).data(content).send();
  }

  /** {@inheritDoc} */
  @Override
  public Promise<ItemReference> createFolder(Path path) {
    final String url = encodeAllowEscapes(getBaseUrl() + FOLDER + path(path.toString()));

    return reqFactory
        .createPostRequest(url, null)
        .loader(loaderFactory.newLoader("Creating folder..."))
        .send(unmarshaller.newUnmarshaller(ItemReference.class));
  }

  /** {@inheritDoc} */
  @Override
  public Promise<Void> deleteItem(Path path) {
    final String url = encodeAllowEscapes(getBaseUrl() + path(path.toString()));

    return reqFactory
        .createRequest(DELETE, url, null, false)
        .loader(loaderFactory.newLoader("Deleting project..."))
        .send();
  }

  /** {@inheritDoc} */
  @Override
  public Promise<Void> copy(Path source, Path target, String newName, boolean overwrite) {
    final String url =
        encodeAllowEscapes(
            getBaseUrl() + COPY + path(source.toString()) + "?to=" + target.toString());

    final CopyOptions copyOptions = dtoFactory.createDto(CopyOptions.class);
    copyOptions.setName(newName);
    copyOptions.setOverWrite(overwrite);

    return reqFactory
        .createPostRequest(url, copyOptions)
        .loader(loaderFactory.newLoader("Copying..."))
        .send();
  }

  /** {@inheritDoc} */
  @Override
  public Promise<Void> move(Path source, Path target, String newName, boolean overwrite) {
    final String url =
        encodeAllowEscapes(
            getBaseUrl() + MOVE + path(source.toString()) + "?to=" + target.toString());

    final MoveOptions moveOptions = dtoFactory.createDto(MoveOptions.class);
    moveOptions.setName(newName);
    moveOptions.setOverWrite(overwrite);

    return reqFactory
        .createPostRequest(url, moveOptions)
        .loader(loaderFactory.newLoader("Moving..."))
        .send();
  }

  /** {@inheritDoc} */
  @Override
  public Promise<TreeElement> getTree(Path path, int depth, boolean includeFiles) {
    final String url =
        encodeAllowEscapes(
            getBaseUrl()
                + TREE
                + path(path.toString())
                + "?depth="
                + depth
                + "&includeFiles="
                + includeFiles);

    // temporary workaround for CHE-3467, remove loader for disable UI blocking
    // later this loader should be added with the new mechanism of client-server synchronization

    return reqFactory
        .createGetRequest(url)
        .header(ACCEPT, MimeType.APPLICATION_JSON)
        .send(unmarshaller.newUnmarshaller(TreeElement.class));
  }

  /** {@inheritDoc} */
  @Override
  public Promise<ItemReference> getItem(Path path) {
    final String url = encodeAllowEscapes(getBaseUrl() + ITEM + path(path.toString()));

    return reqFactory
        .createGetRequest(url)
        .header(ACCEPT, MimeType.APPLICATION_JSON)
        .loader(loaderFactory.newLoader("Getting item..."))
        .send(unmarshaller.newUnmarshaller(ItemReference.class));
  }

  /** {@inheritDoc} */
  @Override
  public Promise<ProjectConfigDto> getProject(Path path) {
    final String url = encodeAllowEscapes(getBaseUrl() + path(path.toString()));

    return reqFactory
        .createGetRequest(url)
        .header(ACCEPT, MimeType.APPLICATION_JSON)
        .loader(loaderFactory.newLoader("Getting project..."))
        .send(unmarshaller.newUnmarshaller(ProjectConfigDto.class));
  }

  /** {@inheritDoc} */
  @Override
  public Promise<ProjectConfigDto> updateProject(ProjectConfigDto configuration) {
    final String url = encodeAllowEscapes(getBaseUrl() + path(configuration.getPath()));

    return reqFactory
        .createRequest(PUT, url, configuration, false)
        .header(CONTENT_TYPE, MimeType.APPLICATION_JSON)
        .header(ACCEPT, MimeType.APPLICATION_JSON)
        .loader(loaderFactory.newLoader("Updating project..."))
        .send(unmarshaller.newUnmarshaller(ProjectConfigDto.class));
  }

  /**
   * Returns the base url for the project service. It consists of workspace agent base url plus
   * project prefix.
   *
   * @return base url for project service
   * @since 4.4.0
   */
  private String getBaseUrl() {
    return appContext.getDevMachine().getWsAgentBaseUrl() + PROJECT;
  }

  /**
   * Normalizes the path by adding a leading '/' if it doesn't exist. Also escapes some special
   * characters.
   *
   * <p>See following javascript functions for details: escape() will not encode: @ * / +
   * encodeURI() will not encode: ~ ! @ # $ & * ( ) = : / , ; ? + ' encodeURIComponent() will not
   * encode: ~ ! * ( ) '
   *
   * @param path path to normalize
   * @return normalized path
   */
  private String path(String path) {
    while (path.indexOf('+') >= 0) {
      path = path.replace("+", "%2B");
    }

    return path.startsWith("/") ? path : '/' + path;
  }
}
