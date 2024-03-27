package com.tapsdk.lc.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.LCObject;

import java.util.List;

public abstract class FindCallback<T extends LCObject> extends LCCallback<List<T>> {
  /**
   * Override this function with the code you want to run after the fetch is complete.
   *
   * @param objects The objects matching the query, or null if it failed.
   * @param LCException The exception raised by the find, or null if it succeeded.
   */
  public abstract void done(List<T> objects, LCException LCException);

  protected final void internalDone0(List<T> returnValue, LCException e) {
    done(returnValue, e);
  }
}
