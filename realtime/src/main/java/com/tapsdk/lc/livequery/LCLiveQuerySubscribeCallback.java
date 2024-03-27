package com.tapsdk.lc.livequery;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;

public abstract class LCLiveQuerySubscribeCallback extends LCCallback<Void> {

  public abstract void done(LCException e);

  @Override
  protected void internalDone0(Void aVoid, LCException LCException) {
    done(LCException);
  }
}
