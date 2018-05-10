package org.eclipse.che.api.logger;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.workspace.shared.dto.RuntimeIdentityDto;
import org.eclipse.che.api.workspace.shared.dto.event.MachineLogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * The goal of this class it to catch all MachineLogEvent events from error stream and dump it to
 * slf4j log.
 */
@Singleton
public class ErrorMachineLogEventLogger implements EventSubscriber<MachineLogEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(ErrorMachineLogEventLogger.class);

  @Inject private EventService eventService;

  @PostConstruct
  public void subscribe() {
    eventService.subscribe(this, MachineLogEvent.class);
  }

  @Override
  public void onEvent(MachineLogEvent event) {
    String stream = event.getStream();
    String text = event.getText();
    if (!isNullOrEmpty(stream) && "stderr".equalsIgnoreCase(stream) && !isNullOrEmpty(text)) {
      RuntimeIdentityDto identity = event.getRuntimeId();
      LOG.error(
          "Machine {} error from owner {} env {} workspace {} stream {} text {} time {} ",
          event.getMachineName(),
          identity.getOwnerId(),
          identity.getEnvName(),
          identity.getWorkspaceId(),
          event.getStream(),
          event.getText(),
          event.getTime());
    }
  }
}
