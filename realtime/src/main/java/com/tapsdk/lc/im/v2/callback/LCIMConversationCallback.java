package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.LCIMException;

/**
 * 作为Conversation操作的回调抽象类
 */
public abstract class LCIMConversationCallback extends LCCallback<Void> {

  /**
   * Override this function with the code you want to run after the save is complete.
   *
   * @param e The exception raised by the save, or null if it succeeded.
   */
  public abstract void done(LCIMException e);

  @Override
  protected final void internalDone0(java.lang.Void returnValue, LCException e) {
    done(LCIMException.wrapperException(e));
  }
}
