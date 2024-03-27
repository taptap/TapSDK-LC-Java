package com.tapsdk.lc.im;

public interface DatabaseDelegateFactory {
  DatabaseDelegate createInstance(String clientId);
}
