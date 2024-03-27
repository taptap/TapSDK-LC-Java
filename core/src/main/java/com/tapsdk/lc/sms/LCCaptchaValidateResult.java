package com.tapsdk.lc.sms;

import com.tapsdk.lc.annotation.JsonField;

public class LCCaptchaValidateResult {
  @JsonField("validate_token")
  private String validateToken;

  public String getValidateToken() {
    return validateToken;
  }

  public void setValidateToken(String validateToken) {
    this.validateToken = validateToken;
  }

  public String getToken() {
    return getValidateToken();
  }

  public void setToken(String validateToken) {
    setValidateToken(validateToken);
  }
}
