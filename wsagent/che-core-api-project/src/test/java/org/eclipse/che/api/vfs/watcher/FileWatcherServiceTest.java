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
package org.eclipse.che.api.vfs.watcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.util.Set;

import static java.nio.file.Files.createDirectory;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Collections.emptySet;
import static org.apache.commons.io.FileUtils.write;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link FileWatcherService}
 */
@RunWith(MockitoJUnitRunner.class)
public class FileWatcherServiceTest {
    private static final int TIMEOUT_VALUE = 3_000;

    @Rule
    public TemporaryFolder rootFolder = new TemporaryFolder();

    FileWatcherEventHandler handler;

    FileWatcherService service;

    @Before
    public void setUp() throws Exception {
        Set<PathMatcher> excludes = emptySet();
        WatchService service = FileSystems.getDefault().newWatchService();
        handler = mock(FileWatcherEventHandler.class);
        this.service = new FileWatcherService(excludes, handler, service);
        this.service.start();
    }

    @After
    public void tearDown() throws Exception {
        service.stop();
        for (int i = 0; i < 10; i++) {
            if (service.isStopped()) {
                return;
            }
            Thread.sleep(1_000);
        }
        fail();
    }

    @Test
    public void shouldWatchRegisteredFolderForFileCreation() throws Exception {
        service.register(rootFolder.getRoot().toPath());

        Path path = rootFolder.newFile().toPath();

        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_CREATE);
    }

    @Test
    public void shouldWatchRegisteredFolderForDirectoryCreation() throws Exception {
        service.register(rootFolder.getRoot().toPath());

        Path path = rootFolder.newFolder().toPath();

        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_CREATE);
    }

    @Test
    public void shouldWatchRegisteredFolderForFileRemoval() throws Exception {
        service.register(rootFolder.getRoot().toPath());

        File file = rootFolder.newFile();
        Path path = file.toPath();
        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_CREATE);

        boolean deleted = file.delete();
        assertTrue(deleted);
        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_DELETE);
    }

    @Test
    public void shouldWatchRegisteredFolderForFolderRemoval() throws Exception {
        service.register(rootFolder.getRoot().toPath());

        File file = rootFolder.newFolder();
        Path path = file.toPath();
        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_CREATE);

        boolean deleted = file.delete();
        assertTrue(deleted);
        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_DELETE);
    }

    @Test
    public void shouldWatchForRegisteredFolderForFileModification() throws Exception {
        service.register(rootFolder.getRoot().toPath());

        File file = rootFolder.newFile();
        Path path = file.toPath();
        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_CREATE);

        write(file, "");
        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_MODIFY);
    }

    @Test
    public void shouldWatchForRegisteredFolderForFolderModification() throws Exception {
        service.register(rootFolder.getRoot().toPath());

        File file = rootFolder.newFolder();
        Path path = file.toPath();
        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_CREATE);

        createDirectory(path.resolve(file.getName()));
        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_MODIFY);
    }

    @Test
    public void shouldNotWatchUnRegisteredFolderForFileCreation() throws Exception {
        Path path = rootFolder.newFile().toPath();

        verify(handler, timeout(TIMEOUT_VALUE).never()).handle(path, ENTRY_CREATE);
    }

    @Test
    public void shouldNotWatchUnRegisteredFolderForDirectoryCreation() throws Exception {
        Path path = rootFolder.newFolder().toPath();

        verify(handler, timeout(TIMEOUT_VALUE).never()).handle(path, ENTRY_CREATE);
    }

    @Test
    public void shouldNotWatchUnRegisteredFolderForFileRemoval() throws Exception {
        service.register(rootFolder.getRoot().toPath());

        File file = rootFolder.newFile();
        Path path = file.toPath();
        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_CREATE);

        service.unRegister(rootFolder.getRoot().toPath());

        boolean deleted = file.delete();
        assertTrue(deleted);
        verify(handler, timeout(TIMEOUT_VALUE).never()).handle(path, ENTRY_DELETE);
    }

    @Test
    public void shouldNotWatchUnRegisteredFolderForFolderRemoval() throws Exception {
        service.register(rootFolder.getRoot().toPath());

        File file = rootFolder.newFolder();
        Path path = file.toPath();
        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_CREATE);

        service.unRegister(rootFolder.getRoot().toPath());

        boolean deleted = file.delete();
        assertTrue(deleted);
        verify(handler, timeout(TIMEOUT_VALUE).never()).handle(path, ENTRY_DELETE);
    }

    @Test
    public void shouldNotWatchUnRegisteredFolderForFileModification() throws Exception {
        service.register(rootFolder.getRoot().toPath());

        File file = rootFolder.newFile();
        Path path = file.toPath();
        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_CREATE);

        service.unRegister(rootFolder.getRoot().toPath());

        write(file, "");
        verify(handler, timeout(TIMEOUT_VALUE).never()).handle(path, ENTRY_MODIFY);
    }

    @Test
    public void shouldNotWatchUnRegisteredFolderForFolderModification() throws Exception {
        service.register(rootFolder.getRoot().toPath());

        File file = rootFolder.newFolder();
        Path path = file.toPath();
        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_CREATE);

        service.unRegister(rootFolder.getRoot().toPath());

        createDirectory(path.resolve(file.getName()));
        verify(handler, timeout(TIMEOUT_VALUE).never()).handle(path, ENTRY_MODIFY);
    }

    @Test
    public void shouldWatchTwiceRegisteredFolderForFileCreationAfterSingleUnregister() throws Exception {
        Path root = rootFolder.getRoot().toPath();

        service.register(root);
        service.register(root);
        service.unRegister(root);


        Path path = rootFolder.newFile().toPath();

        verify(handler, timeout(TIMEOUT_VALUE)).handle(path, ENTRY_CREATE);
    }

    @Test
    public void shouldNotWatchTwiceRegisteredFolderForFileCreationAfterDoubleUnRegister() throws Exception {
        Path root = rootFolder.getRoot().toPath();

        service.register(root);
        service.register(root);
        service.unRegister(root);
        service.unRegister(root);

        Path path = rootFolder.newFile().toPath();

        verify(handler, timeout(TIMEOUT_VALUE).never()).handle(path, ENTRY_CREATE);
    }
}
