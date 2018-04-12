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
package org.eclipse.che.selenium.pageobject;

import static java.lang.String.format;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.ELEMENT_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.EXPECTED_MESS_IN_CONSOLE_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.LOAD_PAGE_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.REDRAW_UI_ELEMENTS_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.UPDATING_PROJECT_TIMEOUT_SEC;
import static org.eclipse.che.selenium.core.constant.TestTimeoutsConstants.WIDGET_TIMEOUT_SEC;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.utils.WaitUtils;
import org.eclipse.che.selenium.core.webdriver.SeleniumWebDriverHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

/** @author Musienko Maxim */
@Singleton
public class ToastLoader {
  private final SeleniumWebDriver seleniumWebDriver;
  private final SeleniumWebDriverHelper seleniumWebDriverHelper;
  private final Loader loader;

  @Inject
  public ToastLoader(
      SeleniumWebDriver seleniumWebDriver,
      SeleniumWebDriverHelper seleniumWebDriverHelper,
      Loader loader) {
    this.seleniumWebDriver = seleniumWebDriver;
    this.seleniumWebDriverHelper = seleniumWebDriverHelper;
    this.loader = loader;
    PageFactory.initElements(seleniumWebDriver, this);
  }

  private interface Locators {
    String MAIN_FORM_ID = "gwt-debug-popupLoader";
    String TOAST_LOADER_BUTTON_XPATH = "//div[@id='gwt-debug-popupLoader']//button[text()='%s']";
    String MAINFORM_WITH_TEXT_CONTAINER_XPATH =
        "//div[@id='gwt-debug-popupLoader']/div[contains(text(),'%s')]";
  }

  @FindBy(id = Locators.MAIN_FORM_ID)
  WebElement mainForm;

  /** wait appearance toast loader widget */
  public void waitToastLoaderIsOpen() {
    seleniumWebDriverHelper.waitVisibility(mainForm, UPDATING_PROJECT_TIMEOUT_SEC);
  }

  /**
   * wait expected text in the widget
   *
   * @param expText
   */
  public void waitExpectedTextInToastLoader(String expText) {
    waitExpectedTextInToastLoader(expText, EXPECTED_MESS_IN_CONSOLE_SEC);
  }

  /**
   * wait expected text in the widget
   *
   * @param expText expected text
   * @param timeout timeout sets in seconds
   */
  public void waitExpectedTextInToastLoader(String expText, int timeout) {
    seleniumWebDriverHelper.waitVisibility(
        By.xpath(format(Locators.MAINFORM_WITH_TEXT_CONTAINER_XPATH, expText)), timeout);
  }

  /** wait for closing of widget */
  public void waitToastLoaderIsClosed() {
    waitToastLoaderIsClosed(ELEMENT_TIMEOUT_SEC);
  }

  /**
   * wait for closing of widget
   *
   * @param userTimeout timeout defined by user
   */
  public void waitToastLoaderIsClosed(int userTimeout) {
    seleniumWebDriverHelper.waitInvisibility(mainForm, userTimeout);
  }

  /** wait appearance the 'Toast widget' with some text, and wait disappearance this one */
  public void waitAppeareanceAndClosing() {
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(
            new ExpectedCondition<Boolean>() {
              @Override
              public Boolean apply(WebDriver driver) {
                // if loader appears
                try {
                  return !getTextFromToastLoader().isEmpty();
                }
                // if we have state exception during get element
                catch (StaleElementReferenceException ex) {
                  // average timeout for disappearance loader
                  WaitUtils.sleepQuietly(7);
                  return true;
                }
                // if loader does not appear
                catch (TimeoutException ex) {
                  return true;
                }
              }
            });
    new WebDriverWait(seleniumWebDriver, LOAD_PAGE_TIMEOUT_SEC)
        .until(
            new ExpectedCondition<Boolean>() {
              @Override
              public Boolean apply(WebDriver driver) {
                try {
                  return getTextFromToastLoader().isEmpty();
                } catch (org.openqa.selenium.TimeoutException ex) {
                  return true;
                }
              }
            });
    waitToastLoaderIsClosed();
  }

  /**
   * wait appearance the 'Toast widget' with some text, and wait disappearance this one
   *
   * @param userTimeout timeout defined by user
   */
  public void waitAppeareanceAndClosing(int userTimeout) {
    new WebDriverWait(seleniumWebDriver, REDRAW_UI_ELEMENTS_TIMEOUT_SEC)
        .until(
            new ExpectedCondition<Boolean>() {
              @Override
              public Boolean apply(WebDriver driver) {
                // if loader appears
                try {
                  return !getTextFromToastLoader().isEmpty();
                }
                // if we have state exception during get element
                catch (StaleElementReferenceException ex) {
                  // average timeout for disappearance loader
                  WaitUtils.sleepQuietly(userTimeout);
                  return true;
                }
                // if loader does not appear
                catch (TimeoutException ex) {
                  return true;
                }
              }
            });
    new WebDriverWait(seleniumWebDriver, userTimeout)
        .until(
            new ExpectedCondition<Boolean>() {
              @Override
              public Boolean apply(WebDriver driver) {
                try {
                  return getTextFromToastLoader().isEmpty();
                } catch (org.openqa.selenium.TimeoutException ex) {
                  return true;
                }
              }
            });
    waitToastLoaderIsClosed(userTimeout);
  }

  public String getTextFromToastLoader() {
    return seleniumWebDriverHelper.waitVisibilityAndGetText(mainForm);
  }

  /** wait appearance of button in the widget */
  public void waitToastLoaderButton(String buttonName) {
    seleniumWebDriverHelper.waitVisibility(
        By.xpath(format(Locators.TOAST_LOADER_BUTTON_XPATH, buttonName)), WIDGET_TIMEOUT_SEC);
  }

  /** wait appearance of button in the widget and click on this one */
  public void clickOnToastLoaderButton(String buttonName) {
    loader.waitOnClosed();
    seleniumWebDriverHelper.waitAndClick(
        By.xpath(format(Locators.TOAST_LOADER_BUTTON_XPATH, buttonName)));
  }
}
