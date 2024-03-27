package com.tapsdk.lc.sign;

import com.tapsdk.lc.core.RequestSignature;

public class SecureRequestSignature implements RequestSignature {

  public String generateSign() {
    return NativeSignHelper.generateRequestAuth();
  }
}
