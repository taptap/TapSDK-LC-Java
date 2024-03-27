package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.messages.LCIMRecalledMessage;

public abstract class LCIMMessageRecalledCallback extends LCCallback<LCIMRecalledMessage> {

  public abstract void done(LCIMRecalledMessage recalledMessage, LCException e);

  @Override
  protected void internalDone0(LCIMRecalledMessage LCIMRecalledMessage, LCException LCException) {
    done(LCIMRecalledMessage, LCException);
  }
}
