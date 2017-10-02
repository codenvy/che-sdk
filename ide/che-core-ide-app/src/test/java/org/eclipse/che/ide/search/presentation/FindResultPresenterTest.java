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
package org.eclipse.che.ide.search.presentation;

import static java.util.Collections.emptyList;
import static org.eclipse.che.ide.search.FullTextSearchPresenter.SEARCH_RESULT_ITEMS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.project.ProjectServiceClient;
import org.eclipse.che.ide.api.project.QueryExpression;
import org.eclipse.che.ide.api.resources.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

/**
 * Tests for {@link FindResultPresenter}.
 *
 * @author Valeriy Svydenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class FindResultPresenterTest {
  @Mock private CoreLocalizationConstant localizationConstant;
  @Mock private FindResultView view;
  @Mock private WorkspaceAgent workspaceAgent;
  @Mock private Resources resources;
  @Mock private ProjectServiceClient projectServiceClient;
  @Mock private EventBus eventBus;

  @Mock private QueryExpression queryExpression;
  @Mock private Promise<List<SearchResult>> searchResultPromise;
  @Mock private SearchResult searchResult;
  @Captor private ArgumentCaptor<Operation<List<SearchResult>>> argumentCaptor;

  @InjectMocks FindResultPresenter findResultPresenter;

  private ArrayList<SearchResult> results = new ArrayList<>(SEARCH_RESULT_ITEMS);

  @Before
  public void setUp() throws Exception {
    for (int i = 0; i < SEARCH_RESULT_ITEMS; i++) {
      results.add(searchResult);
    }

    when(projectServiceClient.search(queryExpression)).thenReturn(searchResultPromise);
    when(searchResultPromise.then(Matchers.<Operation<List<SearchResult>>>any()))
        .thenReturn(searchResultPromise);
  }

  @Test
  public void titleShouldBeReturned() {
    findResultPresenter.getTitle();

    verify(localizationConstant).actionFullTextSearch();
  }

  @Test
  public void viewShouldBeReturned() {
    assertEquals(findResultPresenter.getView(), view);
  }

  @Test
  public void imageShouldBeReturned() {
    findResultPresenter.getTitleImage();

    verify(resources).find();
  }

  @Test
  public void methodGoShouldBePerformed() {
    AcceptsOneWidget container = mock(AcceptsOneWidget.class);
    findResultPresenter.go(container);

    verify(container).setWidget(view);
  }

  @Test
  public void responseShouldBeHandled() throws Exception {
    QueryExpression queryExpression = mock(QueryExpression.class);
    findResultPresenter.handleResponse(emptyList(), queryExpression, "request");

    verify(workspaceAgent).openPart(findResultPresenter, PartStackType.INFORMATION);
    verify(workspaceAgent).setActivePart(findResultPresenter);
    verify(view).showResults(emptyList(), "request");
    verify(view).setPreviousBtnActive(false);
    verify(view).setNextBtnActive(false);
  }

  @Test
  public void nextPageShouldNotBeShownIfNoResults() throws Exception {
    findResultPresenter.handleResponse(emptyList(), queryExpression, "request");
    reset(view);
    findResultPresenter.onNextButtonClicked();

    verify(queryExpression).setSkipCount(SEARCH_RESULT_ITEMS);

    verify(searchResultPromise).then(argumentCaptor.capture());
    argumentCaptor.getValue().apply(emptyList());

    verify(view).setPreviousBtnActive(true);
    verify(view).setNextBtnActive(false);
    verify(view, never()).showResults(anyObject(), anyString());
  }

  @Test
  public void nextButtonShouldBeActiveIfResultHasMaxValueElements() throws Exception {
    findResultPresenter.handleResponse(results, queryExpression, "request");

    findResultPresenter.handleResponse(results, queryExpression, "request");
    reset(view);
    findResultPresenter.onNextButtonClicked();

    verify(queryExpression).setSkipCount(SEARCH_RESULT_ITEMS);

    verify(searchResultPromise).then(argumentCaptor.capture());

    argumentCaptor.getValue().apply(results);

    verify(view).setPreviousBtnActive(true);
    verify(view).setNextBtnActive(true);
    verify(view).showResults(results, "request");
  }

  @Test
  public void nextButtonShouldBeDisableIfResultHasLessThanMaxValue() throws Exception {
    results.remove(0);
    findResultPresenter.handleResponse(results, queryExpression, "request");
    reset(view);
    findResultPresenter.onNextButtonClicked();

    verify(queryExpression).setSkipCount(SEARCH_RESULT_ITEMS);

    verify(searchResultPromise).then(argumentCaptor.capture());

    argumentCaptor.getValue().apply(results);

    verify(view).setPreviousBtnActive(true);
    verify(view).setNextBtnActive(false);
    verify(view).showResults(results, "request");
  }

  @Test
  public void previousButtonShouldBeActiveIfResultHasLessThanMaxValue() throws Exception {
    results.remove(0);
    findResultPresenter.handleResponse(results, queryExpression, "request");
    reset(view);
    findResultPresenter.onPreviousButtonClicked();

    verify(queryExpression).setSkipCount(-SEARCH_RESULT_ITEMS);

    verify(searchResultPromise).then(argumentCaptor.capture());

    argumentCaptor.getValue().apply(results);

    verify(view).setNextBtnActive(true);
    verify(view).setPreviousBtnActive(false);
    verify(view).showResults(results, "request");
  }

  @Test
  public void previousButtonShouldBeActiveIfResultHasMaxValueElements() throws Exception {
    findResultPresenter.handleResponse(results, queryExpression, "request");
    reset(view);
    findResultPresenter.onPreviousButtonClicked();

    verify(queryExpression).setSkipCount(-SEARCH_RESULT_ITEMS);

    verify(searchResultPromise).then(argumentCaptor.capture());

    argumentCaptor.getValue().apply(results);

    verify(view).setNextBtnActive(true);
    verify(view).setPreviousBtnActive(true);
    verify(view).showResults(results, "request");
  }
}
