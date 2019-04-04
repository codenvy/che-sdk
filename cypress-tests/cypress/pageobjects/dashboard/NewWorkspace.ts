import { ElementStateChecker } from "../../utils/ElementStateChecker";

/*********************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

/// <reference types="Cypress" />

export class NewWorkspace {
    private static readonly CHE_7_STACK: string = "div[data-stack-id='che7-preview']";
    private static readonly SELECTED_CHE_7_STACK: string = ".stack-selector-item-selected[data-stack-id='che7-preview']"
    private static readonly CREATE_AND_OPEN_BUTTON: string = "che-button-save-flat[che-button-title='Create & Open']>button"
    private static readonly CREATE_AND_EDIT_BUTTON: string = "#dropdown-toggle button[name='dropdown-toggle']"
    private static readonly ADD_OR_IMPORT_PROJECT_BUTTON: string = ".add-import-project-toggle-button";
    private static readonly ADD_BUTTON: string = "button[aria-disabled='false'][name='addButton']";
    private static readonly NAME_FIELD: string = "#workspace-name-input";
    
    private readonly elementStateChecker: ElementStateChecker = new ElementStateChecker();

    
    selectCreateWorkspaceAndProceedEditing() {
        const createAndProceedDropDown: string = "che-button-dropdown.create-workspace-header-button ul.area-dropdown"

        // open drop down list
        cy.get(NewWorkspace.CREATE_AND_EDIT_BUTTON)
            .should('be.visible')
            .click()

        // wait until drop down container is not animated
        cy.get(createAndProceedDropDown)
            .should('not.have.class', 'animating')
            .should('be.visible')

        // click on "Create & Proceed Editing" item in the drop down list
        cy.contains('Create & Proceed Editing')
            .should('be.visible')
            .click()
    }

    typeWorkspaceName(workspaceName: string) {
        cy.get(NewWorkspace.NAME_FIELD)
            .clear()
            .should('have.value', "")
            .type(workspaceName)
            .should('have.value', workspaceName);
    }

    clickOnChe7Stack() {
        cy.get(NewWorkspace.CHE_7_STACK)
            .click();
    }

    waitChe7StackSelected() {
        cy.get(NewWorkspace.SELECTED_CHE_7_STACK)
            .should('be.visible');
    }

    clickOnCreateAndOpenButton() {
        let ideFrameLocator: string = "ide-iframe#ide-iframe-window";

        cy.get(NewWorkspace.CREATE_AND_OPEN_BUTTON)
            .first()
            .should('be.visible')
            .click();

        //check that the workspace has started to boot
        cy.get(ideFrameLocator)
            .should('have.attr', 'aria-hidden', 'false')
    }

    clickOnAddOrImportProjectButton() {
        cy.get(NewWorkspace.ADD_OR_IMPORT_PROJECT_BUTTON)
            .should('be.visible')
            .click();
    }

    enableSampleCheckbox(sampleName: string) {
        cy.get(`#sample-${sampleName}>md-checkbox>div`)
            .first()
            .should(element => {
                expect(this.elementStateChecker.isVisible(element)).to.be.true
            })
            .click({ force: true });

        this.waitSampleCheckboxEnabling(sampleName)
    }

    waitSampleCheckboxEnabling(sampleName: string) {
        cy.get(`#sample-${sampleName}>md-checkbox[aria-checked='true']`)
            .should(element => {
                expect(this.elementStateChecker.isVisible(element)).to.be.true
            });
    }

    waitProjectAdding(projectName: string) {
        cy.get(`#project-source-selector toggle-single-button#${projectName}`)
            .should(element => {
                expect(this.elementStateChecker.isVisible(element)).to.be.true
            })
    }

    waitProjectAbsence(projectName: string) {
        cy.get(`#project-source-selector toggle-single-button#${projectName}`)
            .should(element => {
                expect(this.elementStateChecker.isVisible(element)).to.be.false
            })
    }

    clickOnAddButton() {
        cy.get(NewWorkspace.ADD_BUTTON)
            .should('be.visible')
            .click();
    }

}
