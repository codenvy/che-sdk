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
package org.eclipse.che.selenium.dashboard.organization;

import static org.eclipse.che.commons.lang.NameGenerator.generate;
import static org.eclipse.che.selenium.pageobject.dashboard.NavigationBar.MenuItem.ORGANIZATIONS;
import static org.eclipse.che.selenium.pageobject.dashboard.organization.OrganizationListPage.OrganizationListHeader.NAME;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eclipse.che.multiuser.organization.shared.dto.OrganizationDto;
import org.eclipse.che.selenium.core.client.TestOrganizationServiceClient;
import org.eclipse.che.selenium.core.user.AdminTestUser;
import org.eclipse.che.selenium.core.user.TestUser;
import org.eclipse.che.selenium.pageobject.dashboard.Dashboard;
import org.eclipse.che.selenium.pageobject.dashboard.EditMode;
import org.eclipse.che.selenium.pageobject.dashboard.NavigationBar;
import org.eclipse.che.selenium.pageobject.dashboard.organization.OrganizationListPage;
import org.eclipse.che.selenium.pageobject.dashboard.organization.OrganizationPage;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test validates organization rename.
 *
 * @author Ann Shumilova
 */
public class RenameOrganizationTest {
  private static final String PARENT_ORG_NAME = generate("organization", 5);
  private static final String CHILD_ORG_NAME = generate("organization", 5);
  private static final String NEW_PARENT_ORG_NAME = generate("organization", 5);
  private static final String NEW_CHILD_ORG_NAME = generate("organization", 5);

  private OrganizationDto parentOrganization;
  private OrganizationDto childOrganization;

  @Inject
  @Named("admin")
  private TestOrganizationServiceClient testOrganizationServiceClient;

  @Inject private OrganizationListPage organizationListPage;
  @Inject private OrganizationPage organizationPage;
  @Inject private NavigationBar navigationBar;
  @Inject private EditMode editMode;
  @Inject private Dashboard dashboard;
  @Inject private TestUser testUser;
  @Inject private AdminTestUser adminTestUser;

  @BeforeClass
  public void setUp() throws Exception {
    parentOrganization = testOrganizationServiceClient.create(PARENT_ORG_NAME);
    childOrganization =
        testOrganizationServiceClient.create(CHILD_ORG_NAME, parentOrganization.getId());

    testOrganizationServiceClient.addAdmin(parentOrganization.getId(), testUser.getId());
    testOrganizationServiceClient.addAdmin(childOrganization.getId(), testUser.getId());

    dashboard.open(testUser.getName(), testUser.getPassword());
  }

  @AfterClass
  public void tearDown() throws Exception {
    for (OrganizationDto organization : testOrganizationServiceClient.getAll())
      testOrganizationServiceClient.deleteById(organization.getId());
  }

  @Test(priority = 1)
  public void testParentOrganizationRename() {
    navigationBar.waitNavigationBar();
    navigationBar.clickOnMenu(ORGANIZATIONS);
    organizationListPage.waitForOrganizationsToolbar();
    organizationListPage.waitForOrganizationsList();

    // Open the parent organization and try to set is not valid name
    organizationListPage.clickOnOrganization(parentOrganization.getName());
    organizationPage.waitOrganizationTitle(parentOrganization.getName());
    organizationPage.setOrganizationName(" ");
    editMode.waitDisplayed();
    assertFalse(editMode.isSaveEnabled());
    editMode.clickCancel();
    editMode.waitHidden();
    assertEquals(parentOrganization.getName(), organizationPage.getOrganizationName());

    // Test renaming of the parent organization
    organizationPage.setOrganizationName(NEW_PARENT_ORG_NAME);
    editMode.waitDisplayed();
    assertTrue(editMode.isSaveEnabled());
    editMode.clickSave();
    editMode.waitHidden();
    organizationPage.waitOrganizationTitle(NEW_PARENT_ORG_NAME);
    assertEquals(NEW_PARENT_ORG_NAME, organizationPage.getOrganizationName());
  }

  @Test(priority = 2)
  public void testSubOrganizationRename() {
    String organizationPath = NEW_PARENT_ORG_NAME + "/" + CHILD_ORG_NAME;
    String path = NEW_PARENT_ORG_NAME + "/" + NEW_CHILD_ORG_NAME;

    navigationBar.waitNavigationBar();
    navigationBar.clickOnMenu(ORGANIZATIONS);
    organizationListPage.waitForOrganizationsToolbar();
    organizationListPage.waitForOrganizationsList();

    // Test renaming of the sub-organization
    organizationListPage.clickOnOrganization(organizationPath);
    organizationPage.waitOrganizationTitle(organizationPath);
    organizationPage.setOrganizationName(NEW_CHILD_ORG_NAME);
    editMode.waitDisplayed();
    assertTrue(editMode.isSaveEnabled());
    editMode.clickSave();
    editMode.waitHidden();
    organizationPage.waitOrganizationTitle(path);
    assertEquals(organizationPage.getOrganizationName(), NEW_CHILD_ORG_NAME);

    // Back to the parent organization and test that the sub-organization renamed
    organizationPage.clickBackButton();
    organizationPage.waitOrganizationTitle(NEW_PARENT_ORG_NAME);
    organizationPage.clickSubOrganizationsTab();
    organizationListPage.waitForOrganizationsList();
    assertTrue(organizationListPage.getValues(NAME).contains(path));

    // Back to the Organizations list and test that the organizations renamed
    organizationPage.clickBackButton();
    organizationListPage.waitForOrganizationsList();
    assertTrue(organizationListPage.getValues(NAME).contains(path));
    assertTrue(organizationListPage.getValues(NAME).contains(NEW_PARENT_ORG_NAME));
  }
}
