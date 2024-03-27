package com.tapsdk.lc.im.v2;

/**
 * Message Query Interval
 * Created by fengjunwen on 2017/9/5.
 */

public class LCIMMessageInterval {
  public MessageIntervalBound startIntervalBound;
  public MessageIntervalBound endIntervalBound;

  public static class MessageIntervalBound {
    public String messageId;
    public long timestamp;
    public boolean closed;
    private MessageIntervalBound(String messageId, long timestamp, boolean closed) {
      this.messageId = messageId;
      this.timestamp = timestamp;
      this.closed = closed;
    }
  }

  /**
   * create query bound
   * @param messageId - message id
   * @param timestamp - message timestamp
   * @param closed    - included specified message flag.
   *                    true: include
   *                    false: not include.
   * @return  query interval bound instance
   */
  public static MessageIntervalBound createBound(String messageId, long timestamp, boolean closed) {
    return new MessageIntervalBound(messageId, timestamp, closed);
  }

  /**
   * query interval constructor.
   * @param start - interval start bound
   * @param end   - interval end bound
   */
  public LCIMMessageInterval(MessageIntervalBound start, MessageIntervalBound end) {
    this.startIntervalBound = start;
    this.endIntervalBound = end;
  }
}
