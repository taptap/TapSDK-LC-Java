package com.tapsdk.lc.im.v2.callback;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.im.v2.LCIMException;

import java.util.Map;

public abstract class LCIMCommonJsonCallback extends LCCallback<Map<String, Object>> {
  public abstract void done(Map<String, Object> result, LCIMException e);

  @Override
  protected void internalDone0(Map<String, Object> result, LCException LCException) {
    done(result, LCIMException.wrapperException(LCException));
  }
}
