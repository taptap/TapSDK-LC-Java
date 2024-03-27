package com.tapsdk.lc.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.types.LCNull;

public abstract class RequestEmailVerifyCallback extends LCCallback<LCNull> {
  /**
   * Override this function with the code you want to run after the request is complete.
   *
   * @param e The exception raised by the save, or null if no account is associated with the email
   *          address.
   */
  public abstract void done(LCException e);

  @Override
  protected void internalDone0(LCNull t, LCException LCException) {
    this.done(LCException);
  }
}