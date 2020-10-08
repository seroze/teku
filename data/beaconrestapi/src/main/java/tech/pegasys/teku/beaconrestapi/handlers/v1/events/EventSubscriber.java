/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.teku.beaconrestapi.handlers.v1.events;

import io.javalin.http.sse.SseClient;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.pegasys.teku.infrastructure.async.AsyncRunner;

public class EventSubscriber {
  private static final Logger LOG = LogManager.getLogger();
  static final int MAX_PENDING_EVENTS = 10;
  private final List<EventType> eventTypes;
  private final SseClient sseClient;
  private final Queue<QueuedEvent> queuedEvents;
  private final Runnable closeCallback;
  private final AtomicBoolean processingQueue;
  final AsyncRunner asyncRunner;

  public EventSubscriber(
      final List<String> eventTypes,
      final SseClient sseClient,
      final Runnable closeCallback,
      final AsyncRunner asyncRunner) {
    this.eventTypes = EventType.getTopics(eventTypes);
    this.sseClient = sseClient;
    this.closeCallback = closeCallback;
    this.queuedEvents = new ConcurrentLinkedQueue<>();
    this.processingQueue = new AtomicBoolean(false);
    this.asyncRunner = asyncRunner;
    this.sseClient.onClose(closeCallback);
  }

  public void onEvent(final EventType eventType, final String message) {
    if (!eventTypes.contains(eventType)) {
      return;
    }
    if (queuedEvents.size() < MAX_PENDING_EVENTS) {
      queuedEvents.add(QueuedEvent.of(eventType, message));
      processEventQueue();
    } else {
      LOG.trace("Closing client connection due to exceeding the pending message limit");
      sseClient.ctx.req.getAsyncContext().complete();
      closeCallback.run();
    }
  }

  public SseClient getSseClient() {
    return sseClient;
  }

  private void processEventQueue() {
    if (!processingQueue.compareAndSet(false, true)) {
      // any queue processing in progress will clear the queue, no need to run another instance
      return;
    }
    asyncRunner
        .runAsync(
            () -> {
              LOG.trace(
                  "Processing queue with {} elements for event client {}",
                  queuedEvents.size(),
                  sseClient.hashCode());
              QueuedEvent event = queuedEvents.poll();
              while (event != null) {
                sseClient.sendEvent(event.getEventType().name(), event.getMessageData());
                event = queuedEvents.poll();
              }
            })
        .alwaysRun(
            () -> {
              processingQueue.set(false);
              if (queuedEvents.size() > 0) {
                processEventQueue();
              }
            })
        .finish(
            error ->
                LOG.error(
                    "Failed to process event queue for client " + sseClient.hashCode(), error));
  }
}
