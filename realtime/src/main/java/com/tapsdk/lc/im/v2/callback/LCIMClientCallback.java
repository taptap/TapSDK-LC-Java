package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.LCIMClient;
import com.tapsdk.lc.im.v2.LCIMException;

public abstract class LCIMClientCallback extends LCCallback<LCIMClient> {
  public abstract void done(LCIMClient client, LCIMException e);

  @Override
  protected void internalDone0(LCIMClient client, LCException LCException) {
    done(client, LCIMException.wrapperException(LCException));
  }
}
