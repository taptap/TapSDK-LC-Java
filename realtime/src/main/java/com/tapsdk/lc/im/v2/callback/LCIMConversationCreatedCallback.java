package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.LCIMConversation;
import com.tapsdk.lc.im.v2.LCIMException;

public abstract class LCIMConversationCreatedCallback extends LCCallback<LCIMConversation> {
  public abstract void done(LCIMConversation conversation, LCIMException e);

  @Override
  protected final void internalDone0(LCIMConversation returnValue, LCException e) {
    done(returnValue, LCIMException.wrapperException(e));
  }
}