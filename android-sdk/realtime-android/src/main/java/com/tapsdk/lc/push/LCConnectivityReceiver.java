package com.tapsdk.lc.push;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.tapsdk.lc.util.AndroidUtil;
import com.tapsdk.lc.utils.LogUtil;

/**
 * Created by fengjunwen on 2018/7/3.
 */

public class LCConnectivityReceiver extends BroadcastReceiver {
  private final LCConnectivityListener listener;
  private boolean connectivityBroken = false;

  public LCConnectivityReceiver(LCConnectivityListener listener) {
    this.listener = listener;
  }

  public boolean isConnectivityBroken() {
    return connectivityBroken;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (null == this.listener || null == context) {
      return;
    }
    int hasPermission = AndroidUtil.checkPermission(context, Manifest.permission.ACCESS_NETWORK_STATE);
    if (PackageManager.PERMISSION_GRANTED != hasPermission) {
      LogUtil.getLogger(LCConnectivityReceiver.class).w("android.Manifest.permission.ACCESS_NETWORK_STATE is not granted.");
      return;
    }

    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

    try {
      NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
      if (null == activeNetwork || !activeNetwork.isConnected()) {
        this.listener.onNotConnected(context);
        connectivityBroken = true;
        return;
      }
      connectivityBroken = false;
      if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
        this.listener.onMobile(context);
      } else if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
        this.listener.onWifi(context);
      } else {
        this.listener.onOtherConnected(context);
      }
    } catch (Exception ex) {
      ;
    }
  }
}
