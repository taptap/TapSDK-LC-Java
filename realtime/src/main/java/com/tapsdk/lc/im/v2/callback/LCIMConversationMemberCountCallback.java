package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.LCIMException;

/**
 * 查询在线用户数目的回调抽象类
 */
public abstract class LCIMConversationMemberCountCallback extends LCCallback<Integer> {
  public abstract void done(Integer memberCount, LCIMException e);

  @Override
  protected final void internalDone0(Integer returnValue, LCException e) {
    done(returnValue, LCIMException.wrapperException(e));
  }
}
