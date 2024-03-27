package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.LCIMException;
import com.tapsdk.lc.im.v2.conversation.LCIMConversationMemberInfo;

import java.util.List;

/**
 * 对话成员信息查询结果回调类
 */
public abstract class LCIMConversationMemberQueryCallback extends LCCallback<List<LCIMConversationMemberInfo>> {
  /**
   * 结果处理函数
   * @param memberInfoList   结果列表
   * @param e                异常实例，正常情况下为 null。
   */
  public abstract void done(List<LCIMConversationMemberInfo> memberInfoList, LCIMException e);

  @Override
  protected final void internalDone0(List<LCIMConversationMemberInfo> returnValue, LCException e) {
    done(returnValue, LCIMException.wrapperException(e));
  }
}