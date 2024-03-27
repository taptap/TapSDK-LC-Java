package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.LCIMClient;

public abstract class LCIMClientStatusCallback extends LCCallback<LCIMClient.LCIMClientStatus> {
  public abstract void done(LCIMClient.LCIMClientStatus client);

  @Override
  protected void internalDone0(LCIMClient.LCIMClientStatus status, LCException LCException) {
    done(status);
  }
}
