package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.LCIMException;
import com.tapsdk.lc.im.v2.LCIMMessage;

import java.util.List;

public abstract class LCIMMessagesQueryCallback extends LCCallback<List<LCIMMessage>> {

  public abstract void done(List<LCIMMessage> messages, LCIMException e);

  @Override
  protected final void internalDone0(List<LCIMMessage> returnValue, LCException e) {
    done(returnValue, LCIMException.wrapperException(e));
  }
}
