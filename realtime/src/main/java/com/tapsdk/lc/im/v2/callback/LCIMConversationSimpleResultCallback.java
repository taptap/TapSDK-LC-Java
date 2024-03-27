package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.LCIMException;

import java.util.List;

public abstract class LCIMConversationSimpleResultCallback extends LCCallback<List<String>> {
  /**
   * 结果处理函数
   * @param memberIdList  成员的 client id 列表
   * @param e             异常
   */
  public abstract void done(List<String> memberIdList, LCIMException e);

  @Override
  protected final void internalDone0(List<String> returnValue, LCException e) {
    done(returnValue, LCIMException.wrapperException(e));
  }
}