package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.LCIMMessage;

public abstract class LCIMMessageUpdatedCallback extends LCCallback<LCIMMessage> {

  public abstract void done(LCIMMessage message, LCException e);

  @Override
  protected void internalDone0(LCIMMessage message, LCException LCException) {
    done(message, LCException);
  }
}