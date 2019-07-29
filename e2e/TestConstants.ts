/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

export const TestConstants = {
    /**
     * Base URL of the application which should be checked
     */
    TS_SELENIUM_BASE_URL: process.env.TS_SELENIUM_BASE_URL || 'http://sample-url',

    /**
     * Run browser in "Headless" (hiden) mode, "false" by default.
     */
    TS_SELENIUM_HEADLESS: process.env.TS_SELENIUM_HEADLESS === 'true',

    /**
     * Browser width resolution, "1920" by default.
     */
    TS_SELENIUM_RESOLUTION_WIDTH: Number(process.env.TS_SELENIUM_RESOLUTION_WIDTH) || 1920,

    /**
     * Browser height resolution, "1080" by default.
     */
    TS_SELENIUM_RESOLUTION_HEIGHT: Number(process.env.TS_SELENIUM_RESOLUTION_HEIGHT) || 1080,

    /**
     * Timeout in milliseconds waiting for workspace start, "240 000" by default.
     */
    TS_SELENIUM_START_WORKSPACE_TIMEOUT: Number(process.env.TS_SELENIUM_START_WORKSPACE_TIMEOUT) || 240000,

    /**
     * Timeout in milliseconds waiting for page load, "120 000" by default.
     */
    TS_SELENIUM_LOAD_PAGE_TIMEOUT: Number(process.env.TS_SELENIUM_LOAD_PAGE_TIMEOUT) || 120000,

    /**
     * Timeout in milliseconds waiting for language server initialization, "180 000" by default.
     */
    TS_SELENIUM_LANGUAGE_SERVER_START_TIMEOUT: Number(process.env.TS_SELENIUM_LANGUAGE_SERVER_START_TIMEOUT) || 180000,

    /**
     * Default timeout for most of the waitings, "20 000" by default.
     */
    TS_SELENIUM_DEFAULT_TIMEOUT: Number(process.env.TS_SELENIUM_DEFAULT_TIMEOUT) || 20000,

    /**
     * Default ammount of tries, "5" by default.
     */
    TS_SELENIUM_DEFAULT_ATTEMPTS: Number(process.env.TS_SELENIUM_DEFAULT_ATTEMPTS) || 5,

    /**
     * Default delay in milliseconds between tries, "1000" by default.
     */
    TS_SELENIUM_DEFAULT_POLLING: Number(process.env.TS_SELENIUM_DEFAULT_POLLING) || 1000,

    /**
     * Amount of tries for checking workspace status.
     */
    TS_SELENIUM_WORKSPACE_STATUS_ATTEMPTS: Number(process.env.TS_SELENIUM_WORKSPACE_STATUS_ATTEMPTS) || 90,

    /**
     * Delay in milliseconds between checking workspace status tries.
     */
    TS_SELENIUM_WORKSPACE_STATUS_POLLING: Number(process.env.TS_SELENIUM_WORKSPACE_STATUS_POLLING) || 10000,

    /**
     * Amount of tries for checking plugin precence.
     */
    TS_SELENIUM_PLUGIN_PRECENCE_ATTEMPTS: Number(process.env.TS_SELENIUM_PLUGIN_PRECENCE_ATTEMPTS) || 20,

    /**
     * Delay in milliseconds between checking plugin precence.
     */
    TS_SELENIUM_PLUGIN_PRECENCE_POLLING: Number(process.env.TS_SELENIUM_PLUGIN_PRECENCE_POLLING) || 2000,

    /**
     * Name of workspace created for 'Happy Path' scenario validation.
     */
    TS_SELENIUM_HAPPY_PATH_WORKSPACE_NAME: process.env.TS_SELENIUM_HAPPY_PATH_WORKSPACE_NAME || 'petclinic-dev-environment',

    /**
     * Name of namespace created for 'Install Che' on OCP 4.x by OperatorHub UI.
     */
    TS_INSTALL_CHE_PROJECT_NAME: process.env.TS_INSTALL_CHE_PROJECT_NAME || 'test-che-operator',

    /**
     * Username used to log in MultiUser Che.
     */
    TS_SELENIUM_USERNAME: process.env.TS_SELENIUM_CHE_USERNAME || '',

    /**
     * Password used to log in MultiUser Che.
     */
    TS_SELENIUM_PASSWORD: process.env.TS_SELENIUM_CHE_PASSWORD || '',

    /**
     * Url of console OpenShift 4.x.
     */
    TS_SELENIUM_OPENSHIFT4_URL: process.env.TS_SELENIUM_OPENSHIFT4_URL || '',

    /**
     * Username used to log in OCP 4.x.
     */
    TS_SELENIUM_OPENSHIFT4_USERNAME: process.env.TS_SELENIUM_OPENSHIFT4_USERNAME || 'kubeadmin',

    /**
     * Password used to log in OCP 4.x.
     */
    TS_SELENIUM_OPENSHIFT4_PASSWORD: process.env.TS_SELENIUM_OPENSHIFT4_PASSWORD || '',

    /**
     * Log into CHE in MultiUser mode, "false" by default.
     */
    TS_SELENIUM_MULTIUSER: process.env.TS_SELENIUM_MULTIUSER === 'true',

    /**
     * Log into OCP by common user (not temporary admin user), "false" by default.
     */
    TS_OCP_LOGIN_PAGE_OAUTH: process.env.TS_OCP_LOGIN_PAGE_OAUTH === 'true',

    /**
     * Update Channel name on 'Create Operator Subscription' page on OCP 4.x, "nightly" or "stable".
     */
    TS_OCP_UPDATE_CHANNEL_OPERATOR: process.env.TS_OCP_UPDATE_CHANNEL_OPERATOR || 'nightly'
};
