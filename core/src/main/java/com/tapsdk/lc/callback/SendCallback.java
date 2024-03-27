package com.tapsdk.lc.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.types.LCNull;

public abstract class SendCallback extends LCCallback<LCNull> {
  public abstract void done(LCException e);

  @Override
  protected final void internalDone0(LCNull t, LCException LCException) {
    this.done(LCException);
  }
}
