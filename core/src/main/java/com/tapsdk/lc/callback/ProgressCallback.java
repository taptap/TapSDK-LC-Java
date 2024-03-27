package com.tapsdk.lc.callback;

import com.tapsdk.lc.LCException;

public abstract class ProgressCallback extends LCCallback<Integer> {
  public abstract void done(Integer percentDone);

  /**
   * Override this function with your desired callback.
   * @param returnValue actual progress value.
   * @param e exception.
   */
  protected final void internalDone0(Integer returnValue, LCException e) {
    done(returnValue);
  }
}
