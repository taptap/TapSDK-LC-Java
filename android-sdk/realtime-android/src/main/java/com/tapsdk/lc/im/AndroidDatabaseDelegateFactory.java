package com.tapsdk.lc.im;

import android.content.Context;

import com.tapsdk.lc.im.v2.AndroidDatabaseDelegate;

/**
 * Created by fengjunwen on 2018/8/9.
 */

public class AndroidDatabaseDelegateFactory implements DatabaseDelegateFactory {
  private Context context;
  public AndroidDatabaseDelegateFactory(Context context) {
    this.context = context;
  }

  public DatabaseDelegate createInstance(String clientId) {
    return new AndroidDatabaseDelegate(this.context, clientId);
  }
}
