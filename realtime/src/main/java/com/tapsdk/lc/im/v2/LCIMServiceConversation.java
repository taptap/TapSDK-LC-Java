package com.tapsdk.lc.im.v2;

public class LCIMServiceConversation extends LCIMConversation {
  protected LCIMServiceConversation(LCIMClient client, String conversationId) {
    super(client, conversationId);
    setSystem(true);
  }
}
