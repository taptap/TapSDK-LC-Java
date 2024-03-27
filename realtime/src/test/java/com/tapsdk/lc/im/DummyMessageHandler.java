package  com.tapsdk.lc.im;

import  com.tapsdk.lc.im.v2.LCIMClient;
import  com.tapsdk.lc.im.v2.LCIMConversation;
import  com.tapsdk.lc.im.v2.LCIMMessage;
import  com.tapsdk.lc.im.v2.LCIMMessageHandler;

public class DummyMessageHandler extends LCIMMessageHandler {
  @Override
  public void onMessage(LCIMMessage message, LCIMConversation conversation, LCIMClient client) {
    System.out.println("conversation(" + conversation.getConversationId() + ") receiving message: " + message.toJSONString());
  }

  @Override
  public void onMessageReceipt(LCIMMessage message, LCIMConversation conversation, LCIMClient client) {
    System.out.println("conversation(" + conversation.getConversationId() + ") receiving receipt message: " + message.toJSONString());
  }
}
