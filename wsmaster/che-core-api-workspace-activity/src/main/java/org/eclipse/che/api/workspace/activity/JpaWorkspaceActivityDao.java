/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.workspace.activity;

import static java.util.Objects.requireNonNull;

import com.google.inject.persist.Transactional;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.workspace.server.event.BeforeWorkspaceRemovedEvent;
import org.eclipse.che.core.db.cascade.CascadeEventSubscriber;

/**
 * JPA workspaces expiration times storage.
 *
 * @author Max Shaposhnik (mshaposh@redhat.com)
 */
@Singleton
public class JpaWorkspaceActivityDao implements WorkspaceActivityDao {

  @Inject private Provider<EntityManager> managerProvider;

  @Override
  public void setExpiration(WorkspaceExpiration expiration) throws ServerException {
    setExpirationTime(expiration.getWorkspaceId(), expiration.getExpiration());
  }

  @Override
  public void setExpirationTime(String workspaceId, long expirationTime) throws ServerException {
    requireNonNull(workspaceId, "Required non-null workspace id");
    doUpdate(workspaceId, a -> a.setExpiration(expirationTime));
  }

  @Override
  public void removeExpiration(String workspaceId) throws ServerException {
    requireNonNull(workspaceId, "Required non-null workspace id");
    doUpdateOptionally(workspaceId, a -> a.setExpiration(null));
  }

  @Override
  public List<String> findExpired(long timestamp) throws ServerException {
    try {
      return doFindExpired(timestamp);
    } catch (RuntimeException x) {
      throw new ServerException(x.getLocalizedMessage(), x);
    }
  }

  @Override
  @Transactional
  public void removeActivity(String workspaceId) throws ServerException {
    EntityManager em = managerProvider.get();
    try {
      WorkspaceActivity activity = em.find(WorkspaceActivity.class, workspaceId);
      if (activity != null) {
        em.remove(activity);
        em.flush();
      }
    } catch (RuntimeException x) {
      throw new ServerException(x.getLocalizedMessage(), x);
    }
  }

  @Override
  public void setCreatedTime(String workspaceId, long createdTimestamp) throws ServerException {
    requireNonNull(workspaceId, "Required non-null workspace id");
    doUpdate(
        workspaceId,
        a -> {
          a.setCreated(createdTimestamp);

          // We might just have created the activity record and we need to initialize the status
          // to something. Since a created workspace is implicitly stopped, let's record it like
          // that.
          // If any status change event was already captured, the status would have been set
          // accordingly already.
          if (a.getStatus() == null) {
            a.setStatus(WorkspaceStatus.STOPPED);
          }
        });
  }

  @Override
  public void setStatusChangeTime(String workspaceId, WorkspaceStatus status, long timestamp)
      throws ServerException {
    requireNonNull(workspaceId, "Required non-null workspace id");

    Consumer<WorkspaceActivity> update;
    switch (status) {
      case RUNNING:
        update =
            a -> {
              a.setStatus(status);
              a.setLastRunning(timestamp);
            };
        break;
      case STARTING:
        update =
            a -> {
              a.setStatus(status);
              a.setLastStarting(timestamp);
            };
        break;
      case STOPPED:
        update =
            a -> {
              a.setStatus(status);
              a.setLastStopped(timestamp);
            };
        break;
      case STOPPING:
        update =
            a -> {
              a.setStatus(status);
              a.setLastStopping(timestamp);
            };
        break;
      default:
        throw new IllegalStateException("Unhandled workspace status: " + status);
    }

    doUpdate(workspaceId, update);
  }

  @Override
  public List<String> findInStatusSince(long timestamp, WorkspaceStatus status)
      throws ServerException {
    String queryName = "WorkspaceActivity.get" + firstUpperCase(status.name()) + "Since";

    try {
      return managerProvider
          .get()
          .createNamedQuery(queryName, WorkspaceActivity.class)
          .setParameter("time", timestamp)
          .getResultList()
          .stream()
          .map(WorkspaceActivity::getWorkspaceId)
          .collect(Collectors.toList());
    } catch (RuntimeException e) {
      throw new ServerException(e.getLocalizedMessage(), e);
    }
  }

  @Override
  public WorkspaceActivity findActivity(String workspaceId) throws ServerException {
    try {
      EntityManager em = managerProvider.get();
      return em.find(WorkspaceActivity.class, workspaceId);
    } catch (RuntimeException x) {
      throw new ServerException(x.getLocalizedMessage(), x);
    }
  }

  @Transactional
  protected void doUpdate(String workspaceId, Consumer<WorkspaceActivity> updater)
      throws ServerException {
    doUpdate(false, workspaceId, updater);
  }

  @Transactional
  protected void doUpdateOptionally(String workspaceId, Consumer<WorkspaceActivity> updater)
      throws ServerException {
    doUpdate(true, workspaceId, updater);
  }

  private void doUpdate(boolean optional, String workspaceId, Consumer<WorkspaceActivity> updater)
      throws ServerException {

    try {
      EntityManager em = managerProvider.get();
      WorkspaceActivity activity = em.find(WorkspaceActivity.class, workspaceId);
      if (activity == null) {
        if (optional) {
          return;
        }
        activity = new WorkspaceActivity();
        activity.setWorkspaceId(workspaceId);

        updater.accept(activity);

        em.persist(activity);
      } else {
        updater.accept(activity);

        em.merge(activity);
        em.flush();
      }

    } catch (RuntimeException x) {
      throw new ServerException(x.getLocalizedMessage(), x);
    }
  }

  @Transactional
  protected List<String> doFindExpired(long timestamp) {
    return managerProvider
        .get()
        .createNamedQuery("WorkspaceActivity.getExpired", WorkspaceActivity.class)
        .setParameter("expiration", timestamp)
        .getResultList()
        .stream()
        .map(WorkspaceActivity::getWorkspaceId)
        .collect(Collectors.toList());
  }

  @Singleton
  public static class RemoveExpirationBeforeWorkspaceRemovedEventSubscriber
      extends CascadeEventSubscriber<BeforeWorkspaceRemovedEvent> {

    @Inject private EventService eventService;

    @Inject private WorkspaceActivityDao workspaceActivityDao;

    @PostConstruct
    public void subscribe() {
      eventService.subscribe(this, BeforeWorkspaceRemovedEvent.class);
    }

    @Override
    public void onCascadeEvent(BeforeWorkspaceRemovedEvent event) throws Exception {
      workspaceActivityDao.removeActivity(event.getWorkspace().getId());
    }
  }

  private static String firstUpperCase(String str) {
    return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
  }
}
