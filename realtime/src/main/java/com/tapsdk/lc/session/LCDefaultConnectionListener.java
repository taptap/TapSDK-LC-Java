package com.tapsdk.lc.session;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.LCLogger;
import com.tapsdk.lc.Messages;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.command.*;
import com.tapsdk.lc.command.ConversationControlPacket.ConversationControlOp;
import com.tapsdk.lc.im.InternalConfiguration;
import com.tapsdk.lc.im.v2.*;
import com.tapsdk.lc.utils.LogUtil;
import com.tapsdk.lc.utils.StringUtil;
import com.tapsdk.lc.session.PendingMessageCache.Message;
import com.tapsdk.lc.session.IMOperationQueue.Operation;
import com.tapsdk.lc.im.v2.Conversation.LCIMOperation;

import com.google.protobuf.ByteString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LCDefaultConnectionListener implements LCConnectionListener {
  private static final LCLogger LOGGER = LogUtil.getLogger(LCDefaultConnectionListener.class);
  private static final String SESSION_MESSASGE_DEPOT = "com.avos.push.session.message.";
  private static final int CODE_SESSION_SIGNATURE_FAILURE = 4102;
  private static final int CODE_SESSION_TOKEN_FAILURE = 4112;
  LCSession session;
  private final StaleMessageDepot depot;

  public LCDefaultConnectionListener(LCSession session) {
    this.session = session;
    depot = new StaleMessageDepot(SESSION_MESSASGE_DEPOT + session.getSelfPeerId());
  }

  @Override
  public void onWebSocketOpen() {
    LOGGER.d("web socket opened, send session open.");
//  if (AVSession.Status.Closed == session.getCurrentStatus()) {
    session.reopen();
//  }
  }

  @Override
  public void onWebSocketClose() {
    if (LCSession.Status.Closed != session.getCurrentStatus()) {
      try {
        session.sessionListener.onSessionPaused(session);
        // 这里给所有的消息发送失败消息
        if (session.pendingMessages != null && !session.pendingMessages.isEmpty()) {
          while (!session.pendingMessages.isEmpty()) {
            Message m = session.pendingMessages.poll();
            if (!StringUtil.isEmpty(m.cid)) {
              LCConversationHolder conversation = session.getConversationHolder(m.cid, Conversation.CONV_TYPE_NORMAL);
              InternalConfiguration.getOperationTube().onOperationCompleted(session.getSelfPeerId(), conversation.conversationId,
                      Integer.parseInt(m.id), LCIMOperation.CONVERSATION_SEND_MESSAGE,
                      new RuntimeException("Connection Lost"));
            }
          }
        }
        if (session.conversationOperationCache != null
                && !session.conversationOperationCache.isEmpty()) {
          for (Map.Entry<Integer, Operation> entry : session.conversationOperationCache.cache.entrySet()) {
            int requestId = entry.getKey();
            Operation op = session.conversationOperationCache.poll(requestId);
            InternalConfiguration.getOperationTube().onOperationCompleted(op.sessionId, op.conversationId, requestId,
                    LCIMOperation.getIMOperation(op.operation), new IllegalStateException("Connection Lost"));
          }
        }
      } catch (Exception e) {
        session.sessionListener.onError(session, e);
      }
    }
  }

  @Override
  public void onError(Integer requestKey, Messages.ErrorCommand errorCommand) {
    if (null == errorCommand) {
      return;
    }

    if (null != requestKey && requestKey != CommandPacket.UNSUPPORTED_OPERATION) {
      Operation op = session.conversationOperationCache.poll(requestKey);
      if (null != op && op.operation == LCIMOperation.CLIENT_OPEN.getCode()) {
        session.setSessionStatus(LCSession.Status.Closed);
      }
      int code = errorCommand.getCode();
      int appCode = (errorCommand.hasAppCode() ? errorCommand.getAppCode() : 0);
      String reason = errorCommand.getReason();
      LCIMOperation operation = (null != op)? LCIMOperation.getIMOperation(op.operation): null;
      InternalConfiguration.getOperationTube().onOperationCompleted(session.getSelfPeerId(), null, requestKey,
              operation, new LCIMException(code, appCode, reason));
    }

    // 如果遇到signature failure的异常,清除缓存
    if (null == requestKey) {
      int code = errorCommand.getCode();
      // 如果遇到signature failure的异常,清除缓存
      if (CODE_SESSION_SIGNATURE_FAILURE == code) {
        SessionCacheHelper.getTagCacheInstance().removeSession(session.getSelfPeerId());
      } else if (CODE_SESSION_TOKEN_FAILURE == code) {
        // 如果遇到session token 失效或者过期的情况，先是清理缓存，然后再重新触发一次自动登录
        session.updateRealtimeSessionToken("", 0);
        this.onWebSocketOpen();
      }
    }
  }

  @Override
  public void onMessageArriving(String peerId, Integer requestKey, Messages.GenericCommand command) {
    if (null == command) {
      return;
    }
    LOGGER.d("new message arriving. peerId=" + peerId + ", requestId=" + requestKey + ", command=" + command.getCmd().getNumber());
    if (command.getCmd().getNumber() == Messages.CommandType.loggedin_VALUE) {
      LOGGER.w("ignore loggedin command bcz invalid service.");
    } else {
      switch (command.getCmd().getNumber()) {
        case Messages.CommandType.direct_VALUE:
          processDirectCommand(peerId, command.getDirectMessage());
          break;
        case Messages.CommandType.session_VALUE:
          processSessionCommand(peerId, command.getOp().name(), requestKey,
                  command.getSessionMessage());
          break;
        case Messages.CommandType.ack_VALUE:
          processAckCommand(peerId, requestKey, command.getAckMessage());
          break;
        case Messages.CommandType.rcp_VALUE:
          processRcpCommand(peerId, command.getRcpMessage());
          break;
        case Messages.CommandType.conv_VALUE:
          processConvCommand(peerId, command.getOp().name(), requestKey,
                  command.getConvMessage());
          break;
        case Messages.CommandType.error_VALUE:
          processErrorCommand(peerId, requestKey, command.getErrorMessage());
          break;
        case Messages.CommandType.logs_VALUE:
          processLogsCommand(peerId, requestKey, command.getLogsMessage());
          break;
        case Messages.CommandType.unread_VALUE:
          processUnreadCommand(peerId, command.getUnreadMessage());
          break;
        case Messages.CommandType.blacklist_VALUE:
          processBlacklistCommand(peerId, command.getOp().name(), requestKey, command.getBlacklistMessage());
          break;
        case Messages.CommandType.patch_VALUE:
          if(command.getOp().equals(Messages.OpType.modify)) {
            // modify 为服务器端主动推送的 patch 消息
            processPatchCommand(peerId, true, requestKey, command.getPatchMessage());
          } else if (command.getOp().equals(Messages.OpType.modified)) {
            // modified 代表的是服务器端对于客户端请求的相应
            processPatchCommand(peerId, false, requestKey, command.getPatchMessage());
          }
          break;
        case Messages.CommandType.goaway_VALUE:
          processGoawayCommand(peerId);
          break;
        default:
          LOGGER.w("unknown command. Cmd:" + command.getCmd().getNumber());
          break;
      }
    }
  }

  private void processDirectCommand(String peerId, Messages.DirectCommand directCommand) {
    final String msg = directCommand.getMsg();
    final ByteString binaryMsg = directCommand.getBinaryMsg();
    final String fromPeerId = directCommand.getFromPeerId();
    final String conversationId = directCommand.getCid();
    final Long timestamp = directCommand.getTimestamp();
    final String messageId = directCommand.getId();
    int convType = directCommand.hasConvType()? directCommand.getConvType() : Conversation.CONV_TYPE_NORMAL;
    final boolean isTransient = directCommand.hasTransient() && directCommand.getTransient();
    final boolean hasMore = directCommand.hasHasMore() && directCommand.getHasMore();
    final long patchTimestamp = directCommand.getPatchTimestamp();
    final boolean mentionAll = directCommand.hasMentionAll() && directCommand.getMentionAll();
    final List<String> mentionList = directCommand.getMentionPidsList();

    try {
      if (!isTransient) {
        if (!StringUtil.isEmpty(conversationId)) {
          session.sendPacket(ConversationAckPacket.getConversationAckPacket(
                  session.getSelfPeerId(), conversationId, messageId));
        } else {
          session.sendPacket(genSessionAckPacket(messageId));
        }
      }

      if (depot.putStableMessage(messageId) && !StringUtil.isEmpty(conversationId)) {
        LCConversationHolder conversation = session.getConversationHolder(conversationId, convType);
        LCIMMessage message = null;
        if (StringUtil.isEmpty(msg) && null != binaryMsg) {
          message = new LCIMBinaryMessage(conversationId, fromPeerId, timestamp, -1);
          ((LCIMBinaryMessage)message).setBytes(binaryMsg.toByteArray());
        } else {
          message = new LCIMMessage(conversationId, fromPeerId, timestamp, -1);
          message.setContent(msg);
        }
        message.setMessageId(messageId);
        message.setUpdateAt(patchTimestamp);
        message.setMentionAll(mentionAll);
        message.setMentionList(mentionList);
        conversation.onMessage(message, hasMore, isTransient);
      }
    } catch (Exception e) {
      session.sessionListener.onError(session, e);
    }
  }
  private void processSessionCommand(String peerId, String op, Integer requestKey,
                                     Messages.SessionCommand command) {
    LOGGER.d("processSessionCommand. op=" + op + ",requestKey=" + requestKey);

    int requestId = (null != requestKey ? requestKey : CommandPacket.UNSUPPORTED_OPERATION);

    if (op.equals(SessionControlPacket.SessionControlOp.OPENED)) {
      try {
        LCSession.Status prevStatus = session.getCurrentStatus();
        session.setSessionStatus(LCSession.Status.Opened);


        if (LCSession.Status.Closed == prevStatus || session.conversationOperationCache.containRequest(requestId)) {
          if (requestId != CommandPacket.UNSUPPORTED_OPERATION) {
            session.conversationOperationCache.poll(requestId);
          }
          session.sessionListener.onSessionOpen(session, requestId);
        } else {
          LOGGER.d("session resumed");
          session.sessionListener.onSessionResumed(session);
        }
        if (command.hasSt() && command.hasStTtl()) {
          session.updateRealtimeSessionToken(command.getSt(), Integer.valueOf(command.getStTtl()));
        }
        if (command.hasLastPatchTime()) {
          session.updateLastPatchTime(command.getLastPatchTime(), true);
        }
      } catch (Exception e) {
        session.sessionListener.onError(session, e);
      }
    } else if (op.equals(SessionControlPacket.SessionControlOp.RENEWED_RTMTOKEN)) {
      if (command.hasSt() && command.hasStTtl()) {
        session.updateRealtimeSessionToken(command.getSt(), Integer.valueOf(command.getStTtl()));
      }
      session.sessionListener.onSessionTokenRenewed(session, requestId);
    }else if (op.equals(SessionControlPacket.SessionControlOp.QUERY_RESULT)) {
      final List<String> sessionPeerIds = command.getOnlineSessionPeerIdsList();
      session.sessionListener.onOnlineQuery(session, sessionPeerIds,
              requestId);

    } else if (op.equals(SessionControlPacket.SessionControlOp.CLOSED)) {
      if (command.hasCode()) {
        session.sessionListener.onSessionClosedFromServer(session,
                command.getCode());
      } else {
        if (requestId != CommandPacket.UNSUPPORTED_OPERATION) {
          session.conversationOperationCache.poll(requestId);
        }
        session.sessionListener.onSessionClose(session, requestId);
      }
      session.setSessionStatus(LCSession.Status.Closed);
    } else {
      LOGGER.w("unknown operation: " + op);
    }
  }
  private void processAckCommand(String peerId, Integer requestKey, Messages.AckCommand ackCommand) {
    session.setServerAckReceived(System.currentTimeMillis() / 1000);
    long timestamp = ackCommand.getT();
    final Message m = session.pendingMessages.poll(String.valueOf(requestKey));
    if (ackCommand.hasCode()) {
      // 这里是发送消息异常时的ack
      this.onAckError(requestKey, ackCommand, m);
    } else {
      if (null != m && !StringUtil.isEmpty(m.cid)) {
        LCConversationHolder conversation = session.getConversationHolder(m.cid, Conversation.CONV_TYPE_NORMAL);
        session.conversationOperationCache.poll(requestKey);
        String msgId = ackCommand.getUid();
        conversation.onMessageSent(requestKey, msgId, timestamp);
        if (m.requestReceipt) {
          m.timestamp = timestamp;
          m.id = msgId;
          MessageReceiptCache.add(session.getSelfPeerId(), msgId, m);
        }
      }
    }
  }
  private void processRcpCommand(String peerId, Messages.RcpCommand rcpCommand) {
    try {
      if (rcpCommand.hasRead() && rcpCommand.hasCid()) {
        final Long timestamp = rcpCommand.getT();
        String conversationId = rcpCommand.getCid();
        LCConversationHolder conversation = session.getConversationHolder(conversationId, Conversation.CONV_TYPE_NORMAL);
        conversation.onConversationReadAtEvent(timestamp);
      } else if (rcpCommand.hasT()) {
        final Long timestamp = rcpCommand.getT();
        final String conversationId = rcpCommand.getCid();
        int convType = Conversation.CONV_TYPE_NORMAL; // RcpCommand doesn't include convType, so we use default value.
        // Notice: it becomes a problem only when server send RcpCommand to a new device for the logined user.
        String from = rcpCommand.hasFrom()? rcpCommand.getFrom():null;

        if (!StringUtil.isEmpty(conversationId)) {
          processConversationDeliveredAt(conversationId, convType, timestamp);
          processMessageReceipt(rcpCommand.getId(), conversationId, convType, timestamp, from);
        }
      }
    } catch (Exception e) {
      session.sessionListener.onError(session, e);
    }
  }

  private void processConvCommand(String peerId, String operation, Integer requestKey,
                                  Messages.ConvCommand convCommand) {
    if (ConversationControlPacket.ConversationControlOp.QUERY_RESULT.equals(operation)) {
      Operation op = session.conversationOperationCache.poll(requestKey);
      LOGGER.d("poll operation with requestId=" + requestKey + ", result=" + op);
      if (null != op && op.operation == LCIMOperation.CONVERSATION_QUERY.getCode()) {
        String result = convCommand.getResults().getData();
        final HashMap<String, Object> bundle = new HashMap<>();
        bundle.put(Conversation.callbackData, result);
        RequestStormSuppression.getInstance().release(op, new RequestStormSuppression.RequestCallback() {
          @Override
          public void done(Operation operation) {
            LOGGER.d("[RequestSuppression] requestId=" + operation.requestId + ", selfId=" + operation.sessionId + " completed.");
            InternalConfiguration.getOperationTube().onOperationCompletedEx(operation.sessionId, null, operation.requestId,
                    LCIMOperation.CONVERSATION_QUERY, bundle);
          }
        });
      } else {
        LOGGER.w("not found requestKey: " + requestKey + ", op=" + op);
      }
    } else if (ConversationControlPacket.ConversationControlOp.QUERY_SHUTUP_RESULT.equals(operation)) {
      Operation op = session.conversationOperationCache.poll(requestKey);
      if (null != op && op.operation == LCIMOperation.CONVERSATION_MUTED_MEMBER_QUERY.getCode()) {
        List<String> result = convCommand.getMList(); // result stored in m field.
        String next = convCommand.hasNext()? convCommand.getNext() : null;
        String[] resultMembers = new String[null == result? 0 : result.size()];
        if (null != result) {
          result.toArray(resultMembers);
        }
        HashMap<String, Object> bundle = new HashMap<>();
        bundle.put(Conversation.callbackData, resultMembers);
        if (!StringUtil.isEmpty(next)) {
          bundle.put(Conversation.callbackIterableNext, next);
        }
        InternalConfiguration.getOperationTube().onOperationCompletedEx(session.getSelfPeerId(), null, requestKey,
                LCIMOperation.CONVERSATION_MUTED_MEMBER_QUERY, bundle);
      } else {
        LOGGER.w("not found requestKey: " + requestKey);
      }
    } else {
      String conversationId = null;
      int requestId = (null != requestKey ? requestKey : CommandPacket.UNSUPPORTED_OPERATION);
      LCIMOperation originOperation = null;
      if ((operation.equals(ConversationControlPacket.ConversationControlOp.ADDED)
              || operation.equals(ConversationControlOp.REMOVED)
              || operation.equals(ConversationControlOp.UPDATED)
              || operation.equals(ConversationControlOp.MEMBER_COUNT_QUERY_RESULT)
              || operation.equals(ConversationControlOp.SHUTUP_ADDED)
              || operation.equals(ConversationControlOp.MAX_READ)
              || operation.equals(ConversationControlOp.SHUTUP_REMOVED)
              || operation.equals(ConversationControlOp.MEMBER_UPDATED))
              && requestId != CommandPacket.UNSUPPORTED_OPERATION) {

        Operation op = session.conversationOperationCache.poll(requestId);
        if (null != op) {
          originOperation = LCIMOperation.getIMOperation(op.operation);
          conversationId = op.conversationId;
        } else {
          conversationId = convCommand.getCid();
        }
      } else {
        if (operation.equals(ConversationControlOp.STARTED)) {
          session.conversationOperationCache.poll(requestId);
        }
        conversationId = convCommand.getCid();
      }
      int convType = Conversation.CONV_TYPE_NORMAL;
      if (convCommand.hasTempConv() && convCommand.getTempConv()) {
        convType = Conversation.CONV_TYPE_TEMPORARY;
      } else if (convCommand.hasTransient() && convCommand.getTransient()) {
        convType = Conversation.CONV_TYPE_TRANSIENT;
      }
      if (!StringUtil.isEmpty(conversationId)) {
        LCConversationHolder conversation = session.getConversationHolder(conversationId, convType);
        conversation.processConversationCommandFromServer(originOperation, operation, requestId, convCommand);
      }
    }
  }
  private void processErrorCommand(String peerId, Integer requestKey,
                                   Messages.ErrorCommand errorCommand) {
    if (null != requestKey && requestKey != CommandPacket.UNSUPPORTED_OPERATION) {
      Operation op = session.conversationOperationCache.poll(requestKey);
      if (null != op && op.operation == LCIMOperation.CLIENT_OPEN.getCode()) {
        session.setSessionStatus(LCSession.Status.Closed);
      }
      int code = errorCommand.getCode();
      int appCode = (errorCommand.hasAppCode() ? errorCommand.getAppCode() : 0);
      String reason = errorCommand.getReason();
      LCIMOperation operation = (null != op)? LCIMOperation.getIMOperation(op.operation): null;
      InternalConfiguration.getOperationTube().onOperationCompleted(peerId, null, requestKey,
              operation, new LCIMException(code, appCode, reason));
    }

    // 如果遇到signature failure的异常,清除缓存
    if (null == requestKey) {
      int code = errorCommand.getCode();
      // 如果遇到signature failure的异常,清除缓存
      if (CODE_SESSION_SIGNATURE_FAILURE == code) {
        SessionCacheHelper.getTagCacheInstance().removeSession(session.getSelfPeerId());
      } else if (CODE_SESSION_TOKEN_FAILURE == code) {
        // 如果遇到session token 失效或者过期的情况，先是清理缓存，然后再重新触发一次自动登录
        session.updateRealtimeSessionToken("", 0);
        this.onWebSocketOpen();
      }
    }
  }

  private void processLogsCommand(String peerId, Integer requestKey,
                                  Messages.LogsCommand logsCommand) {
    if (null != requestKey && requestKey != CommandPacket.UNSUPPORTED_OPERATION) {
      Operation op = session.conversationOperationCache.poll(requestKey);
      int convType = Conversation.CONV_TYPE_NORMAL;
      if (logsCommand.getLogsCount() > 0) {
        Messages.LogItem item = logsCommand.getLogs(0);
        if (null != item && item.hasConvType()) {
          convType = item.getConvType();
        }
      }
      LCConversationHolder conversation = session.getConversationHolder(op.conversationId, convType);
      conversation.processMessages(requestKey, logsCommand.getLogsList());
    }
  }

  private void processUnreadCommand(String peerId, Messages.UnreadCommand unreadCommand) {
    session.updateLastNotifyTime(unreadCommand.getNotifTime());
    if (unreadCommand.getConvsCount() > 0) {

      List<Messages.UnreadTuple> unreadTupleList = unreadCommand.getConvsList();
      if (null != unreadTupleList) {
        for (Messages.UnreadTuple unreadTuple : unreadTupleList) {
          String msgId = unreadTuple.getMid();
          String msgContent = unreadTuple.getData();
          long ts = unreadTuple.getTimestamp();
          long updateTS = unreadTuple.getPatchTimestamp();
          String conversationId = unreadTuple.getCid();
          boolean mentioned = unreadTuple.getMentioned();
          ByteString binaryMsg = unreadTuple.getBinaryMsg();
          String from = unreadTuple.getFrom();
          int convType = unreadTuple.hasConvType()? unreadTuple.getConvType() : Conversation.CONV_TYPE_NORMAL;

          LCIMMessage message = null;
          if (StringUtil.isEmpty(msgContent) && null != binaryMsg) {
            message = new LCIMBinaryMessage(conversationId, from, ts, -1);
            ((LCIMBinaryMessage)message).setBytes(binaryMsg.toByteArray());
          } else {
            message = new LCIMMessage(conversationId, from, ts, -1);
            message.setContent(msgContent);
          }
          message.setMessageId(msgId);
          message.setUpdateAt(updateTS);

          LCConversationHolder conversation = session.getConversationHolder(conversationId, convType);
          conversation.onUnreadMessagesEvent(message, unreadTuple.getUnread(), mentioned);
        }
      }
    }
  }
  private void processBlacklistCommand(String peerId, String operation, Integer requestKey,
                                       Messages.BlacklistCommand blacklistCommand) {
    if (BlacklistCommandPacket.BlacklistCommandOp.QUERY_RESULT.equals(operation)) {
      Operation op = session.conversationOperationCache.poll(requestKey);
      if (null == op || op.operation != LCIMOperation.CONVERSATION_BLOCKED_MEMBER_QUERY.getCode()) {
        LOGGER.w("not found requestKey: " + requestKey);
      } else {
        List<String> result = blacklistCommand.getBlockedPidsList();
        String next = blacklistCommand.hasNext()? blacklistCommand.getNext() : null;
        String[] resultArray = new String[null == result ? 0: result.size()];
        if (null != result) {
          result.toArray(resultArray);
        }
        String cid = blacklistCommand.getSrcCid();
        HashMap<String, Object> bundle = new HashMap<>();
        bundle.put(Conversation.callbackData, resultArray);
        if (!StringUtil.isEmpty(next)) {
          bundle.put(Conversation.callbackIterableNext, next);
        }
        InternalConfiguration.getOperationTube().onOperationCompletedEx(session.getSelfPeerId(), cid, requestKey,
                LCIMOperation.CONVERSATION_BLOCKED_MEMBER_QUERY, bundle);
      }
    } else if (BlacklistCommandPacket.BlacklistCommandOp.BLOCKED.equals(operation)
            || BlacklistCommandPacket.BlacklistCommandOp.UNBLOCKED.equals(operation)){
      // response for block/unblock reqeust.
      String conversationId = blacklistCommand.getSrcCid();
      LCConversationHolder internalConversation = session.getConversationHolder(conversationId, Conversation.CONV_TYPE_NORMAL);
      Operation op = session.conversationOperationCache.poll(requestKey);
      if (null == op || null == internalConversation) {
        // warning.
      } else {
        LCIMOperation originOperation = LCIMOperation.getIMOperation(op.operation);
        internalConversation.onResponse4MemberBlock(originOperation, operation, requestKey, blacklistCommand);
      }
    }
  }
  private void processPatchCommand(String peerId, boolean isModifyNotification, Integer requestKey, Messages.PatchCommand patchCommand) {
    updateLocalPatchTime(isModifyNotification, patchCommand);
    if (isModifyNotification) {
      if (patchCommand.getPatchesCount() > 0) {
        for (Messages.PatchItem patchItem : patchCommand.getPatchesList()) {
          LCIMMessage message = LCIMTypedMessage.getMessage(patchItem.getCid(), patchItem.getMid(), patchItem.getData(),
                  patchItem.getFrom(), patchItem.getTimestamp(), 0, 0);
          message.setUpdateAt(patchItem.getPatchTimestamp());
          long patchCode = patchItem.hasPatchCode()? patchItem.getPatchCode() : 0;
          String patchReason = patchItem.hasPatchReason()? patchItem.getPatchReason() : null;
          LCConversationHolder conversation = session.getConversationHolder(patchItem.getCid(), Conversation.CONV_TYPE_NORMAL);
          conversation.onMessageUpdateEvent(message, patchItem.getRecall(), patchCode, patchReason);
        }
      }
    } else {
      Operation op = session.conversationOperationCache.poll(requestKey);
      LCIMOperation operation = LCIMOperation.getIMOperation(op.operation);
      HashMap<String, Object> bundle = new HashMap<>();
      bundle.put(Conversation.PARAM_MESSAGE_PATCH_TIME, patchCommand.getLastPatchTime());
      InternalConfiguration.getOperationTube().onOperationCompletedEx(session.getSelfPeerId(), null, requestKey,
              operation, bundle);
    }
  }

  private void processGoawayCommand(String peerId) {
    session.getConnectionManager().resetConnection();
    session.getConnectionManager().startConnection(new LCCallback() {
      @Override
      protected void internalDone0(Object o, LCException LCException) {
        session.reopen();
      }
    });
  }

  private void onAckError(Integer requestKey, Messages.AckCommand command, Message m) {
    Operation op = session.conversationOperationCache.poll(requestKey);
    if (op.operation == LCIMOperation.CLIENT_OPEN.getCode()) {
      session.setSessionStatus(LCSession.Status.Closed);
    }
    LCIMOperation operation = LCIMOperation.getIMOperation(op.operation);
    int code = command.getCode();
    int appCode = (command.hasAppCode() ? command.getAppCode() : 0);
    String reason = command.getReason();
    LCException error = new LCIMException(code, appCode, reason);
    InternalConfiguration.getOperationTube().onOperationCompleted(session.getSelfPeerId(), op.conversationId,
            requestKey, operation, error);
  }

  /**
   * 处理 v2 版本中 conversation 的 deliveredAt 事件
   * @param conversationId
   * @param timestamp
   */
  private void processConversationDeliveredAt(String conversationId, int convType, long timestamp) {
    LCConversationHolder conversation = session.getConversationHolder(conversationId, convType);
    conversation.onConversationDeliveredAtEvent(timestamp);
  }

  /**
   * 处理 v2 版本中 message 的 rcp 消息
   * @param msgId
   * @param conversationId
   * @param timestamp
   */
  private void processMessageReceipt(String msgId, String conversationId, int convType, long timestamp, String from) {
    Object messageCache =
            MessageReceiptCache.get(session.getSelfPeerId(), msgId);
    if (messageCache == null) {
      return;
    }
    Message m = (Message) messageCache;
    LCIMMessage msg =
            new LCIMMessage(conversationId, session.getSelfPeerId(), m.timestamp, timestamp);
    msg.setMessageId(m.id);
    msg.setContent(m.msg);
    msg.setMessageStatus(LCIMMessage.MessageStatus.StatusReceipt);
    LCConversationHolder conversation = session.getConversationHolder(conversationId, convType);
    conversation.onMessageReceipt(msg, from);
  }

  private SessionAckPacket genSessionAckPacket(String messageId) {
    SessionAckPacket sap = new SessionAckPacket();
    sap.setPeerId(session.getSelfPeerId());
    if (!StringUtil.isEmpty(messageId)) {
      sap.setMessageId(messageId);
    }

    return sap;
  }

  private void updateLocalPatchTime(boolean isModify, Messages.PatchCommand patchCommand) {
    if (isModify) {
      long lastPatchTime = 0;
      for (Messages.PatchItem item : patchCommand.getPatchesList()) {
        if (item.getPatchTimestamp() > lastPatchTime) {
          lastPatchTime = item.getPatchTimestamp();
        }
      }
      session.updateLastPatchTime(lastPatchTime);
    } else {
      session.updateLastPatchTime(patchCommand.getLastPatchTime());
    }
  }
}
