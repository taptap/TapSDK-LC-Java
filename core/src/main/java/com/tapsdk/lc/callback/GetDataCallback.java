package com.tapsdk.lc.callback;

import com.tapsdk.lc.LCException;

public abstract class GetDataCallback extends LCCallback<byte[]> {
  public abstract void done(byte[] data, LCException e);

  protected final void internalDone0(byte[] returnValue, LCException e) {
    done(returnValue, e);
  }
}