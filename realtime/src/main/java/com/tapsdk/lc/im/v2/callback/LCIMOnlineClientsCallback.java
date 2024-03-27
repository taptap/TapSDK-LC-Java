package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.LCIMException;

import java.util.List;

public abstract class LCIMOnlineClientsCallback extends LCCallback<List<String>> {
  public abstract void done(List<String> object, LCIMException e);

  @Override
  protected final void internalDone0(List<String> object, LCException error) {
    this.done(object, LCIMException.wrapperException(error));
  }
}