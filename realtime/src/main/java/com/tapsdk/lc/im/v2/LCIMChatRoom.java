package com.tapsdk.lc.im.v2;

public class LCIMChatRoom extends LCIMConversation {
  protected LCIMChatRoom(LCIMClient client, String conversationId){
    super(client, conversationId);
    setTransientForInit(true);
  }
}
