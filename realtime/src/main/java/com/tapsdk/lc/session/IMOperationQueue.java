package com.tapsdk.lc.session;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.command.CommandPacket;
import com.tapsdk.lc.im.LCIMOptions;
import com.tapsdk.lc.im.BackgroundThreadpool;
import com.tapsdk.lc.im.InternalConfiguration;
import com.tapsdk.lc.im.v2.Conversation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class IMOperationQueue {
  public static class Operation {
    int requestId;
    int operation;
    String sessionId;
    String conversationId;
    String identifier;

    public static Operation getOperation(int operation, String sessionId, String conversationId,
                                         int requestId) {
      Operation op = new Operation();
      op.conversationId = conversationId;
      op.sessionId = sessionId;
      op.operation = operation;
      op.requestId = requestId;
      return op;
    }


    public String getIdentifier() {
      return identifier;
    }

    public void setIdentifier(String identifier) {
      this.identifier = identifier;
    }
  }

  static ConcurrentMap<Integer, Runnable> timeoutCache =
          new ConcurrentHashMap<Integer, Runnable>();
  Map<Integer, Operation> cache = new ConcurrentHashMap<>();
  PersistentQueue<Operation> operationQueue;

  public IMOperationQueue(String key) {
    operationQueue =
            new PersistentQueue<Operation>("operation.queue." + key, Operation.class);
    setupCache();
  }

  private void setupCache() {
    for (Operation op : operationQueue) {
      if (op.requestId != CommandPacket.UNSUPPORTED_OPERATION) {
        cache.put(op.requestId, op);
      }
    }
  }

  public void offer(final Operation op) {
    if (op.requestId != CommandPacket.UNSUPPORTED_OPERATION) {
      cache.put(op.requestId, op);
      Runnable timeoutTask = new Runnable() {

        @Override
        public void run() {
          Operation polledOP = poll(op.requestId);
          if (polledOP != null) {
            Conversation.LCIMOperation operation = Conversation.LCIMOperation.getIMOperation(polledOP.operation);
            InternalConfiguration.getOperationTube().onOperationCompleted(polledOP.sessionId, polledOP.conversationId,
                    polledOP.requestId, operation, new LCException(LCException.TIMEOUT, "Timeout Exception"));
          }
        }
      };
      timeoutCache.put(op.requestId, timeoutTask);
      BackgroundThreadpool.getInstance().executeDelayed(timeoutTask, LCIMOptions.getGlobalOptions().getTimeoutInSecs());
    }
    operationQueue.offer(op);
  }

  public Operation poll(int requestId) {
    if (requestId != CommandPacket.UNSUPPORTED_OPERATION && cache.get(requestId) != null) {
      Operation returnValue = cache.get(requestId);
      cache.remove(requestId);
      operationQueue.remove(returnValue);
      Runnable timeoutTask = timeoutCache.get(requestId);
      timeoutCache.remove(requestId);
      if (timeoutTask != null) {
        BackgroundThreadpool.getInstance().removeScheduledTask(timeoutTask);
      }
      return returnValue;
    }
    return this.poll();
  }

  public boolean containRequest(int requestId) {
    return cache.get(requestId) != null;
  }
  
  public Operation poll() {
    return operationQueue.poll();
  }

  public void clear() {
    operationQueue.clear();
    cache.clear();
  }

  public boolean isEmpty() {
    return operationQueue.isEmpty();
  }
}
