package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.LCIMException;

public abstract class LCIMConversationIterableResultCallback extends LCCallback<LCIMConversationIterableResult> {
  /**
   * 结果处理函数
   * @param iterableResult  可迭代的结果
   * @param e             异常
   */
  public abstract void done(LCIMConversationIterableResult iterableResult, LCIMException e);

  @Override
  protected final void internalDone0(LCIMConversationIterableResult iterableResult, LCException e) {
    done(iterableResult, LCIMException.wrapperException(e));
  }
}
