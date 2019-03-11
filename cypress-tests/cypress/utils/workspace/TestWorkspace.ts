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

export class TestWorkspace {
    private static readonly API_ENDPOINT: string = Cypress.config().baseUrl + "/api/";
    private static readonly WORKSPACE_API_URL: string = TestWorkspace.API_ENDPOINT + "workspace";

    private workspaceName: string = "";
    private workspaceId: string = "";
    private workspaceIdeUrl: string = "";

    constructor(workspaceName: string) {
        this.workspaceName = workspaceName;
        this.createWorkspace(workspaceName);
    }

    private createWorkspace(workspaceName: string) {
        cy.fixture('workspace/che-7-preview.json').then(workspaceJson => {
            workspaceJson.name = workspaceName;
            cy.request('POST', TestWorkspace.WORKSPACE_API_URL, workspaceJson);
        }).then(response => {
            let responceData = response.body;
            this.workspaceId = responceData.id;
            this.workspaceIdeUrl = responceData.links.ide;
        }).then(() => {
            this.startWorkspace();
        })

    };

    getName(): string {
        return this.workspaceName;
    }

    getId(): string {
        return this.workspaceId;
    }

    getIdeUrl(): string {
        return this.workspaceIdeUrl;
    }

    startWorkspace() {
        let workspaceApiUrl: string = `${TestWorkspace.API_ENDPOINT}workspace/${this.getId()}/runtime`;
        cy.request('POST', workspaceApiUrl);
    }

    openWorkspaceIde() {
        cy.visit(this.workspaceIdeUrl);
    }

    deleteWorkspace() {
        let workspaceApiUrl: string = `${TestWorkspace.API_ENDPOINT}workspace/${this.getId()}`;
        cy.request('DELETE', workspaceApiUrl);
    }

}
