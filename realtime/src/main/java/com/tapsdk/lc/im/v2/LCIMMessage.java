package com.tapsdk.lc.im.v2;

import com.tapsdk.lc.codec.Base64Decoder;
import com.tapsdk.lc.codec.Base64Encoder;
import com.tapsdk.lc.im.LCIMOptions;
import com.tapsdk.lc.json.JSON;

import com.tapsdk.lc.utils.StringUtil;

import java.util.*;

public class LCIMMessage {
  protected String conversationId;
  protected String content;
  protected byte[] bytes;
  protected String from;
  protected long timestamp;
  protected long deliveredAt;
  protected long readAt;
  protected long updateAt;

  protected List<String> mentionList = null;
  protected boolean mentionAll = false;
  protected String currentClient = null;

  private boolean isTransient = false;

  protected String messageId;
  protected String uniqueToken;

  protected MessageStatus status;
  protected MessageIOType ioType;

  public LCIMMessage() {
    this(null, null);
  }

  public LCIMMessage(String conversationId, String from) {
    this(conversationId, from, 0, 0);
  }

  public LCIMMessage(String conversationId, String from, long timestamp, long deliveredAt) {
    this(conversationId, from, timestamp, deliveredAt, 0);
  }

  public LCIMMessage(String conversationId, String from, long timestamp, long deliveredAt, long readAt) {
    this.ioType = MessageIOType.TypeOut;
    this.status = MessageStatus.StatusNone;
    this.conversationId = conversationId;
    this.from = from;
    this.timestamp = timestamp;
    this.deliveredAt = deliveredAt;
    this.readAt = readAt;
  }

  /**
   * 获取当前聊天对话对应的id
   *
   * 对应的是AVOSRealtimeConversations表中的objectId
   *
   * @return conversation id.
   * @since 3.0
   */
  public String getConversationId() {
    return conversationId;
  }

  /**
   * 设置消息所在的conversationId，本方法一般用于从反序列化时
   *
   * @param conversationId conversation id.
   */
  public void setConversationId(String conversationId) {
    this.conversationId = conversationId;
  }

  /**
   * 获取消息体的内容
   *
   * @return message content.
   * @since 3.0
   */
  public String getContent() {
    return content;
  }

  /**
   * 设置消息体的内容
   *
   * @param content message content.
   * @since 3.0
   */
  public void setContent(String content) {
    this.content = content;
  }

  /**
   * 获取消息的发送者
   *
   * @return message sender
   */
  public String getFrom() {
    return from;
  }

  /**
   * 设置消息的发送者
   *
   * @param from message sender
   * @since 3.7.3
   */
  public void setFrom(String from) {
    this.from = from;
  }

  /**
   * 获取消息发送的时间
   *
   * @return message send timestamp
   */
  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * @deprecated Please use {@link #getDeliveredAt()}
   * 获取消息成功到达接收方的时间
   *
   * @return message receipt timestamp
   */
  public long getReceiptTimestamp() {
    return getDeliveredAt();
  }

  /**
   * @param receiptTimestamp message receipt timestamp
   * @deprecated Please use {@link #setDeliveredAt(long)}
   */
  public void setReceiptTimestamp(long receiptTimestamp) {
    setDeliveredAt(receiptTimestamp);
  }

  /**
   * 设置消息成功到达接收方的时间
   * @@param deliveredAt message delivered timestamp
   */
  void setDeliveredAt(long deliveredAt) {
    this.deliveredAt = deliveredAt;
  }

  /**
   * 获取消息成功到达接收方的时间
   * @return message delivered timestamp
   */
  public long getDeliveredAt() {
    return deliveredAt;
  }

  public void setReadAt(long readAt) {
    this.readAt = readAt;
  }

  public long getReadAt() {
    return readAt;
  }

  /**
   * set the update time of the message
   * @param updateAt message updated timestamp
   * @deprecated please use setUpdatedAt instead of.
   */
  public void setUpdateAt(long updateAt) {
    this.updateAt = updateAt;
  }

  /**
   * set the update time of the message
   * @param updateAt message updated timestamp
   */
  public void setUpdatedAt(long updateAt) {
    this.updateAt = updateAt;
  }

  /**
   * get the update time of the message
   * @return message updated timestamp
   * @deprecated please use getUpdatedAt instead of.
   */
  public long getUpdateAt() {
    return updateAt;
  }

  /**
   * get the update time of the message
   * @return message updated timestamp
   */
  public long getUpdatedAt() {
    return updateAt;
  }

  /**
   * 设置消息当前的状态，本方法一般用于从反序列化时
   *
   * @param status message status
   */
  public void setMessageStatus(MessageStatus status) {
    this.status = status;
  }

  /**
   * 获取消息当前的状态
   *
   * @return message status
   */

  public MessageStatus getMessageStatus() {
    return this.status;
  }

  /**
   * 获取消息IO类型
   *
   * @return message io type
   */
  public MessageIOType getMessageIOType() {
    return ioType;
  }

  /**
   * 设置消息的IO类型，本方法一般用于反序列化
   *
   * @param ioType message io type
   */
  public void setMessageIOType(MessageIOType ioType) {
    this.ioType = ioType;
  }

  /**
   * 获取消息的全局Id
   *
   * 这个id只有在发送成功或者收到消息时才会有对应的值
   *
   * @return message id
   */
  public String getMessageId() {
    return messageId;
  }

  /**
   * 仅仅是用于反序列化消息时使用，请不要在其他时候使用
   *
   * @param messageId message id
   */
  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }
  /**
   * 判断消息里面是否 mention 了当前用户
   * @return flag indicating message mentions current user or not.
   */
  public boolean mentioned() {
    return isMentionAll() || (null != mentionList && mentionList.contains(currentClient));
  }

  /**
   * 设置 mention 用户列表
   * @param peerIdList mention peer id list
   */
  public void setMentionList(List<String> peerIdList) {
    this.mentionList = peerIdList;
  }

  /**
   * 获取 mention 用户列表
   * @return mention peer id list
   */
  public List<String> getMentionList() {
    return this.mentionList;
  }

  /**
   * 获取 mention 用户列表的字符串（逗号分隔）
   * @return mention peer id list string
   */
  //@JSONField(serialize = false, deserialize = false)
  public String getMentionListString() {
    if (null == this.mentionList) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < this.mentionList.size(); i++) {
      builder.append(this.mentionList.get(i));
      if (i != this.mentionList.size() - 1) {
        builder.append(",");
      }
    }
    return builder.toString();
  }

  /**
   * 设置 mention 用户列表字符串（逗号分隔），功能与 #setMentionList(List peerIdList) 相同，两者调用一个即可。
   * @param content mention peer id list string
   */
  //@JSONField(serialize = false, deserialize = false)
  public void setMentionListString(String content) {
    if (StringUtil.isEmpty(content)) {
      this.mentionList = null;
    } else {
      this.mentionList = new ArrayList<>();
      String[] peerIdArray = content.split("[,\\s]");
      for (String peer: peerIdArray) {
        if (!StringUtil.isEmpty(peer)) {
          this.mentionList.add(peer);
        }
      }
    }
  }

  /**
   * 判断是否 mention 了所有人
   * @return flag indicating mentioned all or not
   */
  public boolean isMentionAll() {
    return mentionAll;
  }

  /**
   * 设置是否 mention 所有人
   * @param mentionAll flag indicating mentioned all or not
   */
  public void setMentionAll(boolean mentionAll) {
    this.mentionAll = mentionAll;
  }

  void setCurrentClient(String clientId) {
    this.currentClient = clientId;
  }

  protected synchronized void generateUniqueToken() {
    if (StringUtil.isEmpty(uniqueToken)) {
      uniqueToken = UUID.randomUUID().toString();
    }
  }

  public void setUniqueToken(String uniqueToken) {
    this.uniqueToken = uniqueToken;
  }

  public String getUniqueToken() {
    return uniqueToken;
  }

  /**
   * set transient flag.
   *
   * @param isTransient transient flag.
   * @notice  add this method just for flutter sdk.
   */
  void setTransient(boolean isTransient) {
    this.isTransient = isTransient;
  }

  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = result * prime + ((null == conversationId)? 0 : conversationId.hashCode());
    result = result * prime + ((null == content)? 0 : content.hashCode());
    result = result * prime + ((null == from)? 0 : from.hashCode());
    result = result * prime + ((null == messageId)? 0 : messageId.hashCode());
    result = result * prime + ((null == mentionList)? 0 : mentionList.hashCode());
    result = result * prime + ((null == uniqueToken)? 0 : uniqueToken.hashCode());
    result = result * prime + (int)timestamp;
    result = result * prime + (int)deliveredAt;
    result = result * prime + (int)readAt;
    result = result * prime + (int)updateAt;
    result = result * prime + (mentionAll? 17 : 0);
    result = result * prime + ioType.hashCode();
    result = result * prime + status.hashCode();

    return result;
  }

  public boolean equals(Object other) {
    if (null == other) {
      return false;
    }
    if (this == other) {
      return true;
    }
    if (!(other instanceof LCIMMessage)) {
      return false;
    }
    LCIMMessage otherMsg = (LCIMMessage) other;
    return StringUtil.equals(conversationId, otherMsg.conversationId)
            && StringUtil.equals(content, otherMsg.content)
            && StringUtil.equals(from, otherMsg.from) && (timestamp == otherMsg.timestamp)
            && (deliveredAt == otherMsg.deliveredAt) && (readAt == otherMsg.readAt) && (updateAt == otherMsg.updateAt)
            && (getMessageStatus() == otherMsg.getMessageStatus())
            && (getMessageIOType() == otherMsg.getMessageIOType())
            && StringUtil.equals(messageId, otherMsg.messageId)
            && StringUtil.equals(mentionList, otherMsg.mentionList)
            && (mentionAll == otherMsg.mentionAll)
            && StringUtil.equals(uniqueToken, otherMsg.uniqueToken);
  }

  public Map<String, Object> dumpRawData() {
    Map<String, Object> result = new HashMap<>();
    if (!StringUtil.isEmpty(conversationId)) {
      result.put("conversationId", conversationId);
    }
    if (!StringUtil.isEmpty(messageId)) {
      result.put("id", messageId);
    }
    if (!StringUtil.isEmpty(from)) {
      result.put("from", from);
    }
    if (!StringUtil.isEmpty(currentClient)) {
      result.put("clientId", currentClient);
    }
    if (!StringUtil.isEmpty(uniqueToken)) {
      result.put("uniqueToken", uniqueToken);
    }
    if (this.timestamp > 0) {
      result.put("timestamp", this.timestamp);
    }
    if (this.updateAt > 0) {
      result.put("patchTimestamp", this.updateAt);
    }
    if (this.deliveredAt > 0) {
      result.put("ackAt", this.deliveredAt);
    }
    if (this.readAt > 0) {
      result.put("readAt", this.readAt);
    }
    if (this.isTransient) {
      result.put("transient", true);
    }
    result.put("io", ioType.getIOType());
    result.put("status", status.getStatusCode());

    result.put("mentionAll", this.mentionAll);
    if (null != this.mentionList && this.mentionList.size() > 0) {
      result.put("mentionPids", this.mentionList);
    }

    if (null != bytes && this instanceof LCIMBinaryMessage) {
      if (LCIMOptions.getGlobalOptions().isWrapMessageBinaryBufferAsString()) {
        result.put("binaryMsg", Base64Encoder.encode(bytes));
      } else {
        result.put("binaryMsg", bytes);
      }
    } else if (this instanceof LCIMTypedMessage) {
      result.put("typeMsgData", JSON.parseObject(getContent(), Map.class));
    } else {
      String content = getContent();
      if (!StringUtil.isEmpty(content)) {
        result.put("msg", content);
      }
    }
    return result;
  }

  public String toJSONString() {
    return JSON.toJSONString(this.dumpRawData());
  }

  public static LCIMMessage parseJSON(Map<String, Object> jsonObject) {
    if (null == jsonObject) {
      return null;
    }
    LCIMMessage message;
    if (jsonObject.containsKey("binaryMsg")) {
      LCIMBinaryMessage binaryMessage = new LCIMBinaryMessage();
      Object binValue = jsonObject.get("binaryMsg");
      if (binValue instanceof String) {
        String binString = (String) jsonObject.get("binaryMsg");
        binaryMessage.setBytes(Base64Decoder.decodeToBytes(binString));
      } else if (binValue instanceof byte[]){
        binaryMessage.setBytes((byte[]) binValue);
      } else if (binValue instanceof List) {
        Object[] a = (Object[]) ((List<Integer>) binValue).toArray();
        byte[] b = new byte[a.length];
        for (int i = 0; i < a.length; i ++) {
          b[i] = ((Integer)a[i]).byteValue();
        }
        binaryMessage.setBytes(b);
      }
      message = binaryMessage;
    } else if (jsonObject.containsKey("typeMsgData")) {
      message = new LCIMMessage();
      Object typedMsgData = jsonObject.get("typeMsgData");
      if (typedMsgData instanceof String) {
        message.setContent((String) typedMsgData);
      } else {
        message.setContent(JSON.toJSONString(typedMsgData));
      }
    } else {
      message = new LCIMMessage();
      message.setContent((String) jsonObject.get("msg"));
    }
    if (jsonObject.containsKey("clientId")) {
      message.setCurrentClient((String) jsonObject.get("clientId"));
    }
    if (jsonObject.containsKey("uniqueToken")) {
      message.setUniqueToken((String) jsonObject.get("uniqueToken"));
    }
    if (jsonObject.containsKey("io")) {
      message.setMessageIOType(MessageIOType.getMessageIOType((int)jsonObject.get("io")));
    }
    if (jsonObject.containsKey("status")) {
      message.setMessageStatus(MessageStatus.getMessageStatus((int)jsonObject.get("status")));
    }
    if (jsonObject.containsKey("timestamp")) {
      message.setTimestamp(((Number) jsonObject.get("timestamp")).longValue());
    }
    if (jsonObject.containsKey("ackAt")) {
      message.setDeliveredAt(((Number) jsonObject.get("ackAt")).longValue());
    }
    if (jsonObject.containsKey("readAt")) {
      message.setReadAt(((Number) jsonObject.get("readAt")).longValue());
    }
    if (jsonObject.containsKey("patchTimestamp")) {
      message.setUpdateAt(((Number) jsonObject.get("patchTimestamp")).longValue());
    }
    if (jsonObject.containsKey("mentionAll")) {
      message.setMentionAll((boolean)jsonObject.get("mentionAll"));
    }
    if (jsonObject.containsKey("mentionPids")) {
      message.setMentionList((List<String>) jsonObject.get("mentionPids"));
    }
    if (jsonObject.containsKey("id")) {
      message.setMessageId((String) jsonObject.get("id"));
    }
    if (jsonObject.containsKey("from")) {
      message.setFrom((String) jsonObject.get("from"));
    }
    if (jsonObject.containsKey("conversationId")) {
      message.setConversationId((String) jsonObject.get("conversationId"));
    }
    if (jsonObject.containsKey("typeMsgData")) {
      message = LCIMMessageManager.parseTypedMessage(message);
    }
    if (jsonObject.containsKey("transient")) {
      message.setTransient((Boolean) jsonObject.get("transient"));
    }
    return message;
  }

  public static LCIMMessage parseJSONString(String content) {
    if (StringUtil.isEmpty(content)) {
      return null;
    }

    Map<String, Object> json = JSON.parseObject(content, Map.class);
    return parseJSON(json);
  }

  public enum MessageStatus {
    StatusNone(0), StatusSending(1), StatusSent(2),
    StatusReceipt(3), StatusFailed(4), StatusRecalled(5);
    int statusCode;

    MessageStatus(int status) {
      this.statusCode = status;
    }

    public int getStatusCode() {
      return statusCode;
    }

    public static MessageStatus getMessageStatus(int statusCode) {
      switch (statusCode) {
        case 0:
          return StatusNone;
        case 1:
          return StatusSending;
        case 2:
          return StatusSent;
        case 3:
          return StatusReceipt;
        case 4:
          return StatusFailed;
        case 5:
          return StatusRecalled;
        default:
          return null;
      }
    }
  }


  public enum MessageIOType {
    /**
     * 标记收到的消息
     */
    TypeIn(1),
    /**
     * 标记发送的消息
     */
    TypeOut(2);
    int ioType;

    MessageIOType(int type) {
      this.ioType = type;
    }

    public int getIOType() {
      return ioType;
    }

    public static MessageIOType getMessageIOType(int type) {
      switch (type) {
        case 1:
          return TypeIn;
        case 2:
          return TypeOut;
      }
      return TypeOut;
    }
  }
}
