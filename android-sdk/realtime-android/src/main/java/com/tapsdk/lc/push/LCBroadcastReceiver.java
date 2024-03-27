package com.tapsdk.lc.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tapsdk.lc.LCLogger;
import com.tapsdk.lc.utils.LogUtil;

/**
 * Created by fengjunwen on 2018/7/3.
 */

public class LCBroadcastReceiver extends BroadcastReceiver {
  private static final LCLogger LOGGER = LogUtil.getLogger(LCBroadcastReceiver.class);

  @Override
  public void onReceive(Context context, Intent intent) {
    // intent完全有可能是null的情况，就太糟糕了
    // 难道刚刚开机的时候移动ISP还没有识别出来的时候就不去尝试连接了么？
    // if (AVUtils.isConnected(context)) {
    try {
      context.startService(new Intent(context, com.tapsdk.lc.push.PushService.class));
    } catch (Exception ex) {
      LOGGER.e("failed to start PushService. cause: " + ex.getMessage());
    }
    // }
  }
}
