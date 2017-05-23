/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.workspace.create;

import com.google.gwt.core.client.Callback;
import com.google.gwt.regexp.shared.RegExp;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.machine.shared.dto.recipe.OldRecipeDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.MachineConfigDto;
import org.eclipse.che.api.workspace.shared.dto.RecipeDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.context.BrowserAddress;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.workspace.WorkspaceServiceClient;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonMap;

/**
 * The class contains business logic which allow to create user workspace if it doesn't exist.
 *
 * @author Dmitry Shnurenko
 */
@Singleton
public class CreateWorkspacePresenter implements CreateWorkspaceView.ActionDelegate {

    protected static final String MEMORY_LIMIT_BYTES = Long.toString(2000L * 1024L * 1024L);
    static final           String RECIPE_TYPE        = "docker";
    static final           int    SKIP_COUNT         = 0;
    static final           int    MAX_COUNT          = 100;
    static final           int    MAX_NAME_LENGTH    = 20;
    static final           int    MIN_NAME_LENGTH    = 3;
    private static final   RegExp FILE_NAME          = RegExp.compile("^[A-Za-z0-9_\\s-\\.]+$");
    private static final   String URL_PATTERN        = "^((ftp|http|https)://[\\w@.\\-\\_]+(:\\d{1,5})?(/[\\w#!:.?+=&%@!\\_\\-/]+)*){1}$";
    private static final   RegExp URL                = RegExp.compile(URL_PATTERN);
    private final CreateWorkspaceView      view;
    private final DtoFactory               dtoFactory;
    private final WorkspaceServiceClient   workspaceClient;
    private final CoreLocalizationConstant locale;
    //    private final Provider<DefaultWorkspaceComponent> wsComponentProvider;
//    private final RecipeServiceClient                 recipeService;
    private final BrowserAddress           browserAddress;

    private Callback<Workspace, Exception> callback;
    private List<OldRecipeDescriptor>      recipes;
    private List<String>                   workspacesNames;

    @Inject
    public CreateWorkspacePresenter(CreateWorkspaceView view,
                                    DtoFactory dtoFactory,
                                    WorkspaceServiceClient workspaceClient,
                                    CoreLocalizationConstant locale,
//                                    Provider<DefaultWorkspaceComponent> wsComponentProvider,
//                                    RecipeServiceClient recipeService,
                                    BrowserAddress browserAddress) {
        this.view = view;
        this.view.setDelegate(this);

        this.dtoFactory = dtoFactory;
        this.workspaceClient = workspaceClient;
        this.locale = locale;
//        this.wsComponentProvider = wsComponentProvider;
//        this.recipeService = recipeService;
        this.browserAddress = browserAddress;

        this.workspacesNames = new ArrayList<>();
    }

    /**
     * Shows special dialog window which allows set up workspace which will be created.
     *
     * @param workspaces
     *         list of existing workspaces
     */
    public void show(/*List<WorkspaceDto> workspaces, */final Callback<Workspace, Exception> callback) {
        this.callback = callback;

        workspacesNames.clear();

//        for (WorkspaceDto workspace : workspaces) {
//            workspacesNames.add(workspace.getConfig().getName());
//        }

//        Promise<List<OldRecipeDescriptor>> recipes = recipeService.getAllRecipes();
//
//        recipes.then(new Operation<List<OldRecipeDescriptor>>() {
//            @Override
//            public void apply(List<OldRecipeDescriptor> recipeDescriptors) throws OperationException {
//                CreateWorkspacePresenter.this.recipes = recipeDescriptors;
//            }
//        });

        String workspaceName = browserAddress.getWorkspaceName();

        view.setWorkspaceName(workspaceName);

        validateCreateWorkspaceForm();

        view.show();
    }

    private void validateCreateWorkspaceForm() {
        String workspaceName = view.getWorkspaceName();

        int nameLength = workspaceName.length();

        String errorDescription = "";

        boolean nameLengthIsInCorrect = nameLength < MIN_NAME_LENGTH || nameLength > MAX_NAME_LENGTH;

        if (nameLengthIsInCorrect) {
            errorDescription = locale.createWsNameLengthIsNotCorrect();
        }

        boolean nameIsInCorrect = !FILE_NAME.test(workspaceName);

        if (nameIsInCorrect) {
            errorDescription = locale.createWsNameIsNotCorrect();
        }

        boolean nameAlreadyExist = workspacesNames.contains(workspaceName);

        if (nameAlreadyExist) {
            errorDescription = locale.createWsNameAlreadyExist();
        }

        view.showValidationNameError(errorDescription);
    }

    /** {@inheritDoc} */
    @Override
    public void onNameChanged() {
        validateCreateWorkspaceForm();
    }

    /** {@inheritDoc} */
    @Override
    public void onCreateButtonClicked() {
        view.hide();

        createWorkspace();
    }

    private void createWorkspace() {
        WorkspaceConfigDto workspaceConfig = getWorkspaceConfig();

        workspaceClient.create(workspaceConfig, null).then(new Operation<WorkspaceDto>() {
            @Override
            public void apply(WorkspaceDto workspace) throws OperationException {
                callback.onSuccess(workspace);
//                DefaultWorkspaceComponent component = wsComponentProvider.get();
//                component.startWorkspace(workspace, callback);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    private WorkspaceConfigDto getWorkspaceConfig() {
        String wsName = view.getWorkspaceName();

        RecipeDto recipe = dtoFactory.createDto(RecipeDto.class)
                                     .withType("dockerimage")
                                     .withLocation("eclipse/ubuntu_jdk8");


        List<String> agents = new ArrayList<>();
        agents.add("org.eclipse.che.exec");
        agents.add("org.eclipse.che.terminal");
        agents.add("org.eclipse.che.ws-agent");
        agents.add("org.eclipse.che.ssh");

        MachineConfigDto machine = dtoFactory.createDto(MachineConfigDto.class)
                                             .withAgents(agents)
                                             .withAttributes(singletonMap("memoryLimitBytes", MEMORY_LIMIT_BYTES));

        EnvironmentDto environment = dtoFactory.createDto(EnvironmentDto.class)
                                               .withRecipe(recipe)
                                               .withMachines(singletonMap("default", machine));

        return dtoFactory.createDto(WorkspaceConfigDto.class)
                         .withName(wsName)
                         .withDefaultEnv(wsName)
                         .withEnvironments(singletonMap(wsName, environment));
    }
}
