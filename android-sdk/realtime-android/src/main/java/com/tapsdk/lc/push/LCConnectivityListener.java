package com.tapsdk.lc.push;

import android.content.Context;

/**
 * Created by fengjunwen on 2018/7/3.
 */

public interface LCConnectivityListener {
  void onMobile(Context context);

  void onWifi(Context context);
  void onOtherConnected(Context context);
  void onNotConnected(Context context);
}
