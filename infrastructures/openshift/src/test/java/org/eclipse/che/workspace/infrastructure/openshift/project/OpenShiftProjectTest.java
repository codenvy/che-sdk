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
package org.eclipse.che.workspace.infrastructure.openshift.project;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.DoneableProjectRequest;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.api.model.ProjectRequestFluent.MetadataNested;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.dsl.ProjectRequestOperation;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.workspace.infrastructure.openshift.OpenShiftClientFactory;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Tests {@link OpenShiftProject}
 *
 * @author Sergii Leshchenko
 */
@Listeners(MockitoTestNGListener.class)
public class OpenShiftProjectTest {

  public static final String PROJECT_NAME = "testProject";
  public static final String WORKSPACE_ID = "workspace123";

  @Mock private OpenShiftPods pods;
  @Mock private OpenShiftServices services;
  @Mock private OpenShiftRoutes routes;
  @Mock private OpenShiftPersistentVolumeClaims pvcs;
  @Mock private OpenShiftClientFactory clientFactory;
  @Mock private OpenShiftClient openShiftClient;

  private OpenShiftProject openShiftProject;

  @BeforeMethod
  public void setUp() throws Exception {
    when(clientFactory.create()).thenReturn(openShiftClient);

    openShiftProject = new OpenShiftProject(WORKSPACE_ID, pods, services, routes, pvcs);
  }

  @Test
  public void testOpenShiftProjectCreationWhenProjectExists() throws Exception {
    // given
    prepareProject(PROJECT_NAME);

    // when
    new OpenShiftProject(clientFactory, PROJECT_NAME, WORKSPACE_ID);
  }

  @Test
  public void testOpenShiftProjectCreationWhenProjectDoesNotExist() throws Exception {
    // given
    MetadataNested projectMetadata = prepareProjectRequest();

    Resource resource = prepareProjectResource(PROJECT_NAME);
    doThrow(new KubernetesClientException("error", 403, null)).when(resource).get();

    // when
    OpenShiftProject openShiftProject =
        new OpenShiftProject(clientFactory, PROJECT_NAME, WORKSPACE_ID);

    // then
    verify(projectMetadata).withName(PROJECT_NAME);
  }

  @Test
  public void testOpenShiftProjectCleaningUp() throws Exception {
    // when
    openShiftProject.cleanUp();

    verify(pods).delete();
    verify(services).delete();
    verify(routes).delete();
  }

  @Test
  public void testOpenShiftProjectCleaningUpIfExceptionsOccurs() throws Exception {
    doThrow(new InfrastructureException("err1.")).when(pods).delete();
    doThrow(new InfrastructureException("err2.")).when(services).delete();

    InfrastructureException error = null;
    // when
    try {
      openShiftProject.cleanUp();

    } catch (InfrastructureException e) {
      error = e;
    }

    // then
    assertNotNull(error);
    String message = error.getMessage();
    assertEquals(message, "Error(s) occurs while cleaning project up. err1. err2.");
    verify(routes).delete();
  }

  private MetadataNested prepareProjectRequest() {
    ProjectRequestOperation projectRequestOperation = mock(ProjectRequestOperation.class);
    DoneableProjectRequest projectRequest = mock(DoneableProjectRequest.class);
    MetadataNested metadataNested = mock(MetadataNested.class);

    doReturn(projectRequestOperation).when(openShiftClient).projectrequests();
    doReturn(projectRequest).when(projectRequestOperation).createNew();
    doReturn(metadataNested).when(projectRequest).withNewMetadata();
    doReturn(metadataNested).when(metadataNested).withName(anyString());
    doReturn(projectRequest).when(metadataNested).endMetadata();
    return metadataNested;
  }

  private Resource prepareProjectResource(String projectName) {
    Resource projectResource = mock(Resource.class);

    NonNamespaceOperation projectOperation = mock(NonNamespaceOperation.class);
    doReturn(projectResource).when(projectOperation).withName(projectName);
    doReturn(projectOperation).when(openShiftClient).projects();
    openShiftClient.projects().withName(projectName).get();
    return projectResource;
  }

  private void prepareProject(String projectName) {
    Project project = mock(Project.class);
    Resource projectResource = prepareProjectResource(projectName);
    doReturn(project).when(projectResource).get();
  }
}
