package com.tapsdk.lc.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.LCObject;

public abstract class RefreshCallback<T extends LCObject> extends LCCallback<T> {
  /**
   * Override this function with the code you want to run after the save is complete.
   *
   * @param object return object.
   * @param e The exception raised by the save, or null if it succeeded.
   */
  public abstract void done(T object, LCException e);

  protected final void internalDone0(T returnValue, LCException e) {
    done(returnValue, e);
  }
}