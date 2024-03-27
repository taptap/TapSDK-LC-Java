package com.tapsdk.lc.network;

public interface NetworkingDetector {
  enum NetworkType {
    WIFI, Mobile, None
  }
  boolean isConnected();
  NetworkType getNetworkType();
}
