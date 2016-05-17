/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.maven.server.projecttype.handler;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.project.ProjectConfig;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.project.server.FolderEntry;
import org.eclipse.che.api.project.server.ProjectRegistry;
import org.eclipse.che.api.project.server.WorkspaceProjectsSyncer;
import org.eclipse.che.api.project.server.handlers.ProjectHandler;
import org.eclipse.che.api.project.server.handlers.ProjectHandlerRegistry;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.vfs.VirtualFile;
import org.eclipse.che.api.vfs.VirtualFileSystemProvider;
import org.eclipse.che.api.vfs.impl.file.LocalVirtualFileSystemProvider;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.plugin.java.server.projecttype.JavaProjectType;
import org.eclipse.che.plugin.java.server.projecttype.JavaValueProviderFactory;
import org.eclipse.che.plugin.maven.server.projecttype.MavenProjectResolver;
import org.eclipse.che.plugin.maven.server.projecttype.MavenProjectType;
import org.eclipse.che.plugin.maven.server.projecttype.MavenValueProviderFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vitaly Parfonov
 */
public class MavenProjectResolverTest {

    private String pomJar =
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>org.eclipse.che.sdk</groupId>\n" +
            "    <artifactId>codenvy-sdk-parent</artifactId>\n" +
            "    <version>3.1.0-SNAPSHOT</version>\n" +
            "</project>";

    private String pom =
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>org.eclipse.che.sdk</groupId>\n" +
            "    <artifactId>codenvy-sdk-parent</artifactId>\n" +
            "    <version>3.1.0-SNAPSHOT</version>\n" +
            "    <packaging>pom</packaging>\n" +
            "    <modules>" +
            "      <module>module1</module>" +
            "      <module>module2</module>" +
            "   </modules>" +
            "</project>";

    private String pomWithNestingModule =
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>org.eclipse.che.sdk</groupId>\n" +
            "    <artifactId>codenvy-sdk-parent</artifactId>\n" +
            "    <version>3.1.0-SNAPSHOT</version>\n" +
            "    <packaging>pom</packaging>\n" +
            "    <modules>" +
            "      <module>../module2</module>" +
            "      <module>../module3</module>" +
            "   </modules>" +
            "</project>";


    private File            rootDirectory;
    private VirtualFile     root;
    private ProjectRegistry projectRegistry;


    //    @Before
    public void setUp() throws Exception {
        rootDirectory = Files.createTempDirectory(null).toFile();
        List<ProjectConfigDto> projects = new ArrayList<>();

        TestWorkspaceHolder workspaceHolder = new TestWorkspaceHolder(projects);
        ProjectTypeRegistry projectTypeRegistry = new ProjectTypeRegistry(new HashSet<>());
        projectTypeRegistry.registerProjectType(new JavaProjectType(new JavaValueProviderFactory()));
        projectTypeRegistry.registerProjectType(new MavenProjectType(new MavenValueProviderFactory()));

        VirtualFileSystemProvider vfsProvider = new LocalVirtualFileSystemProvider(rootDirectory, null);
        root = vfsProvider.getVirtualFileSystem().getRoot();


        projectRegistry = new ProjectRegistry(workspaceHolder, vfsProvider, projectTypeRegistry, new ProjectHandlerRegistry(
                Collections.<ProjectHandler>emptySet()), new EventService());

    }

    //    @Test
    public void withPomXmlMultiModule() throws Exception {
        final String projectName = NameGenerator.generate("maven-project-", 3);
        final Path parentProjectPath = Files.createDirectory(rootDirectory.toPath().resolve(projectName));
        final Path basePomFile = Files.createFile(parentProjectPath.resolve("pom.xml"));
        Files.write(basePomFile, pom.getBytes());

        createModule(parentProjectPath, "module1");
        createModule(parentProjectPath, "module2");

        final VirtualFile virtualFile = root.getChild(org.eclipse.che.api.vfs.Path.of(projectName));
        FolderEntry projectFolder = new FolderEntry(virtualFile);

        MavenProjectResolver.resolve(projectFolder, projectRegistry);

        assertNotNull(projectRegistry.getProjects());
        assertEquals(3, projectRegistry.getProjects().size());
        assertNotNull(projectRegistry.getProject(projectName));
        assertNotNull(projectRegistry.getProject(projectName + "/module1"));
        assertNotNull(projectRegistry.getProject(projectName + "/module2"));
    }

    //    @Test
    public void withPomXmlMultiModuleWithNesting() throws Exception {
        final String parentProject = NameGenerator.generate("project", 5);

        final String mavenProjectName = NameGenerator.generate("maven-project-", 5);
        final Path parentProjectPath = Files.createDirectory(rootDirectory.toPath().resolve(parentProject));
        final Path mavenProjectPath = Files.createDirectory(parentProjectPath.resolve(mavenProjectName));
        final Path basePomFile = Files.createFile(mavenProjectPath.resolve("pom.xml"));
        Files.write(basePomFile, pomWithNestingModule.getBytes());

        createModule(parentProjectPath, "module3");
        createModule(parentProjectPath, "module2");

        final String path = parentProject + "/" + mavenProjectName;
        final VirtualFile virtualFile = root.getChild(org.eclipse.che.api.vfs.Path.of(path));
        FolderEntry projectFolder = new FolderEntry(virtualFile);

        MavenProjectResolver.resolve(projectFolder, projectRegistry);

        assertNotNull(projectRegistry.getProjects());
        assertEquals(3, projectRegistry.getProjects().size());
        assertNotNull(projectRegistry.getProject(path));
        assertNotNull(projectRegistry.getProject(parentProject + "/module3"));
        assertNotNull(projectRegistry.getProject(parentProject + "/module2"));
    }


    private void createModule(Path parent, String moduleName) throws Exception {
        final Path modulePath = Files.createDirectory(parent.resolve(moduleName));
        final Path pom = Files.createFile(modulePath.resolve("pom.xml"));
        Files.write(pom, pomJar.getBytes());
    }

    /**
     * Dummy implementation do nothing
     */
    protected static class TestWorkspaceHolder extends WorkspaceProjectsSyncer {

        private List<ProjectConfigDto> projects;

        public TestWorkspaceHolder(List<ProjectConfigDto> projects) {
            this.projects = projects;
        }

        @Override
        public List<? extends ProjectConfig> getProjects() {
            return projects;
        }

        @Override
        public String getWorkspaceId() {
            return "id";
        }

        @Override
        protected void addProject(ProjectConfig project) throws ServerException {

        }

        @Override
        protected void updateProject(ProjectConfig project) throws ServerException {

        }

        @Override
        protected void removeProject(ProjectConfig project) throws ServerException {

        }


    }
}



