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
package org.eclipse.che.multiuser.api.distributed.cache;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.locks.Lock;
import javax.inject.Named;
import org.eclipse.che.api.workspace.server.WorkspaceLockService;
import org.eclipse.che.commons.lang.concurrent.Unlocker;
import org.jgroups.JChannel;
import org.jgroups.blocks.locking.LockService;

/**
 * JGroups based implementation of {@link WorkspaceLockService}.
 *
 * @author Anton Korneta
 */
@Singleton
public class JGroupsWorkspaceLockService implements WorkspaceLockService {

  private static final String CHANNEL_NAME = "WorkspaceLocks";

  private final LockService lockService;

  @Inject
  public JGroupsWorkspaceLockService(@Named("jgroups.config.file") String confFile) {
    try {
      final JChannel channel = new JChannel(confFile);
      this.lockService = new LockService(channel);
      channel.connect(CHANNEL_NAME);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public Unlocker readLock(String key) {
    // JGroups lock service does not contain an associated pair of read/write locks, that's why
    // there no way provide a reliable version of a read lock.
    return () -> {};
  }

  @Override
  public Unlocker writeLock(String key) {
    final Lock lock = lockService.getLock(key);
    lock.lock();
    return new UnlockerImpl(lock);
  }

  private class UnlockerImpl implements Unlocker {
    private final Lock lock;

    public UnlockerImpl(Lock lock) {
      this.lock = lock;
    }

    @Override
    public void unlock() {
      lock.unlock();
    }
  }
}
