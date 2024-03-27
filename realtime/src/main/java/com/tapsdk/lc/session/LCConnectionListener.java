package com.tapsdk.lc.session;

import com.tapsdk.lc.Messages;

public interface LCConnectionListener {
  void onWebSocketOpen();

  void onWebSocketClose();

  void onMessageArriving(String peerId, Integer requestKey, Messages.GenericCommand genericCommand);

  void onError(Integer requestKey, Messages.ErrorCommand errorCommand);
}
