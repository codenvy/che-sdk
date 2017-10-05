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
package org.eclipse.che.plugin.jdb.server.util;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Anatolii Bazko
 */
public class ProjectApiUtils {

  private static final AtomicBoolean initialized = new AtomicBoolean();

  /**
   * Ensures that project api has been initialized only once.
   */
  public static void ensure() throws Exception {
    if (!initialized.get()) {
      synchronized (initialized) {
        if (!initialized.get()) {
          init();
          initialized.set(true);
        }
      }
    }
  }

  /**
   * Initialize project API for tests.
   */
  private static void init() throws Exception {
    //    TestWorkspaceHolder workspaceHolder = new TestWorkspaceHolder(new ArrayList<>());
    //    File root = new File("target/test-classes/workspace");
    //    assertTrue(root.exists());
    //
    //    File indexDir = new File("target/fs_index");
    //    assertTrue(indexDir.mkdirs());
    //
    //    Set<PathMatcher> filters = new HashSet<>();
    //    filters.add(path -> true);
    //    FSLuceneSearcherProvider sProvider = new FSLuceneSearcherProvider(indexDir, filters);
    //
    //    EventService eventService = new EventService();
    //    LocalVirtualFileSystemProvider vfsProvider =
    //        new LocalVirtualFileSystemProvider(root, sProvider);
    //    ProjectTypeRegistry projectTypeRegistry = new ProjectTypeRegistry(new HashSet<>());
    //    projectTypeRegistry.registerProjectType(new JavaProjectType(new JavaValueProviderFactory()));
    //    ProjectHandlerRegistry projectHandlerRegistry = new ProjectHandlerRegistry(new HashSet<>());
    //    ProjectRegistry projectRegistry =
    //        new ProjectRegistry(
    //            workspaceHolder,
    //            vfsProvider,
    //            projectTypeRegistry,
    //            projectHandlerRegistry,
    //            eventService);
    //    projectRegistry.initProjects();
    //
    //    ProjectImporterRegistry importerRegistry = new ProjectImporterRegistry(new HashSet<>());
    //    FileWatcherNotificationHandler fileWatcherNotificationHandler =
    //        new DefaultFileWatcherNotificationHandler(vfsProvider);
    //    FileTreeWatcher fileTreeWatcher =
    //        new FileTreeWatcher(root, new HashSet<>(), fileWatcherNotificationHandler);
    //    ProjectManager_ projectManager =
    //        new ProjectManager_(
    //            vfsProvider,
    //            projectTypeRegistry,
    //            projectRegistry,
    //            projectHandlerRegistry,
    //            importerRegistry,
    //            fileWatcherNotificationHandler,
    //            fileTreeWatcher,
    //            workspaceHolder,
    //            mock(FileWatcherManager.class));
    //
    //    ResourcesPlugin resourcesPlugin =
    //        new ResourcesPlugin(
    //            "target/index", root.getAbsolutePath(), () -> projectRegistry, () -> projectManager);
    //    resourcesPlugin.start();
    //
    //    JavaPlugin javaPlugin =
    //        new JavaPlugin(root.getAbsolutePath() + "/.settings", resourcesPlugin, projectRegistry);
    //    javaPlugin.start();
    //
    //    projectRegistry.setProjectType("test", "java", false);
    //
    //    JavaModelManager.getDeltaState().initializeRoots(true);
  }

//  private static class TestWorkspaceHolder extends WorkspaceProjectsSyncer {
//    private List<ProjectConfigDto> projects;
//
//    TestWorkspaceHolder(List<ProjectConfigDto> projects) {
//      super(null);
//      this.projects = projects;
//    }
//
//    @Override
//    public List<? extends ProjectConfig> getProjects() {
//      return projects;
//    }
//
//    @Override
//    public String getWorkspaceId() {
//      return "id";
//    }
//
//    @Override
//    protected void addProject(ProjectConfig project) throws ServerException {}
//
//    @Override
//    protected void updateProject(ProjectConfig project) throws ServerException {}
//
//    @Override
//    protected void removeProject(ProjectConfig project) throws ServerException {}
//  }
}
