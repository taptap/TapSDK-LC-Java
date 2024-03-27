package com.tapsdk.lc.livequery;

import com.tapsdk.lc.im.WindTalker;
import com.tapsdk.lc.im.v2.Conversation;
import com.tapsdk.lc.session.LCConnectionManager;
import com.tapsdk.lc.session.IMOperationQueue;

public class LiveQueryOperationDelegate {
  private static final LiveQueryOperationDelegate instance = new LiveQueryOperationDelegate();

  public static final String LIVEQUERY_DEFAULT_ID = "leancloud_livequery_default_id";

  public static LiveQueryOperationDelegate getInstance() {
    return instance;
  }

  IMOperationQueue operationCache;
  private LiveQueryOperationDelegate() {
    operationCache = new IMOperationQueue(LIVEQUERY_DEFAULT_ID);
  }

  public boolean login(String subscriptionId, int requestId) {
    // FIXME: no timeout timer for login request.
    operationCache.offer(IMOperationQueue.Operation.getOperation(
            Conversation.LCIMOperation.LIVEQUERY_LOGIN.getCode(), LIVEQUERY_DEFAULT_ID, null, requestId));
    LCConnectionManager.getInstance().sendPacket(WindTalker.getInstance().assembleLiveQueryLoginPacket(subscriptionId, requestId));
    return true;
  }

  public void ackOperationReplied(int requestId) {
    operationCache.poll(requestId);
  }
}
