package com.tapsdk.lc.im;

import com.tapsdk.lc.LCLogger;
import com.tapsdk.lc.Messages;
import com.tapsdk.lc.command.CommandPacket;
import com.tapsdk.lc.command.LiveQueryLoginPacket;
import com.tapsdk.lc.command.PushAckPacket;
import com.tapsdk.lc.command.SessionControlPacket;
import com.tapsdk.lc.core.LeanCloud;
import com.tapsdk.lc.utils.LogUtil;
import com.google.protobuf.InvalidProtocolBufferException;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WindTalker {
  private static final LCLogger LOGGER = LogUtil.getLogger(WindTalker.class);
  private static WindTalker instance = null;
  static AtomicInteger acu = new AtomicInteger(-65536);
  public static WindTalker getInstance() {
    if (null == instance) {
      instance = new WindTalker();
    }
    return instance;
  }
  private WindTalker() {
    ;
  }

  public static int getNextIMRequestId() {
    int val = acu.incrementAndGet();
    if (val > 65535) {
      while (val > 65535 && !acu.compareAndSet(val, -65536)) {
        val = acu.get();
      }
      return val;
    } else {
      return val;
    }
  }

  public CommandPacket assembleSessionOpenPacket(String deviceId, String clientId, String tag, String sessionToken, long lastNotifyTime,
                                                 long lastPatchTime, boolean reConnect, Integer requestId) {
    SessionControlPacket scp = SessionControlPacket.genSessionCommand(
            deviceId, clientId, null, SessionControlPacket.SessionControlOp.OPEN,
            null, lastNotifyTime, lastPatchTime, requestId);
    scp.setSessionToken(sessionToken);
    scp.setReconnectionRequest(reConnect);
    scp.setAppId(LeanCloud.getApplicationId());
    return scp;
  }

  public CommandPacket assembleSessionOpenPacket(String deviceId, String clientId, String tag, Signature signature, long lastNotifyTime,
                                                 long lastPatchTime, boolean reConnect, int requestId) {
    SessionControlPacket scp = SessionControlPacket.genSessionCommand(
            deviceId, clientId, null,
            SessionControlPacket.SessionControlOp.OPEN, signature,
            lastNotifyTime, lastPatchTime, requestId);
    scp.setTag(tag);
//    if (LCIMOptions.getGlobalOptions().isDisableAutoLogin4Push() || LCIMClient.getClientsCount() > 1) {
      scp.setAppId(LeanCloud.getApplicationId());
//    }
    scp.setReconnectionRequest(reConnect);
    return scp;
  }

  public CommandPacket assembleSessionPacket(String deviceId, String selfId, List<String> peers,
                                             String op, Signature signature, Integer requestId) {
    SessionControlPacket scp = SessionControlPacket.genSessionCommand(deviceId, selfId, peers, op, signature, requestId);
    return scp;
  }

  public CommandPacket assemblePushAckPacket(String installationId, List<String> messageIds) {
    PushAckPacket pushAckPacket = new PushAckPacket();
    pushAckPacket.setInstallationId(installationId);
    pushAckPacket.setMessageIds(messageIds);
    return pushAckPacket;
  }

  public CommandPacket assembleLiveQueryLoginPacket(String subscriptionId, int requestId) {
    LiveQueryLoginPacket lp = new LiveQueryLoginPacket();
    lp.setSubscribeId(subscriptionId);
    lp.setClientTs(System.currentTimeMillis());
    if (0 != requestId) {
      lp.setRequestId(requestId);
    }
    return lp;
  }
  public Messages.GenericCommand disassemblePacket(ByteBuffer bytes) {
    try {
      return Messages.GenericCommand.parseFrom(bytes);
    } catch (InvalidProtocolBufferException ex) {
      LOGGER.e("failed to disassemble packet.", ex);
      return null;
    }
  }
}
