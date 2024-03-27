package com.tapsdk.lc.push;

import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.tapsdk.lc.json.JSON;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.tapsdk.lc.LCLogger;
import com.tapsdk.lc.LeanCloud;
import com.tapsdk.lc.cache.PersistenceUtil;
import com.tapsdk.lc.codec.Base64Decoder;
import com.tapsdk.lc.codec.Base64Encoder;
import com.tapsdk.lc.utils.LogUtil;
import com.tapsdk.lc.utils.StringUtil;

/**
 * Created by fengjunwen on 2018/8/7.
 */

public class NotifyUtil {
  private static LCLogger LOGGER = LogUtil.getLogger(NotifyUtil.class);

  protected static HandlerThread thread = new HandlerThread("com.avos.avoscloud.notify");
  static final int SERVICE_RESTART = 1024;
  static final String SERVICE_RESTART_ACTION = "com.avos.avoscloud.notify.action";
  static {
    thread.start();
  }

  static Handler notifyHandler = new Handler(thread.getLooper()) {
    @Override
    public void handleMessage(Message m) {
      if (m.what == SERVICE_RESTART && LeanCloud.getContext() != null) {
        this.removeMessages(SERVICE_RESTART);
        try {
          Set<String> registeredApps = getRegisteredApps();
          for (String encodedAppPackage : registeredApps) {
            String appPackage = Base64Decoder.decode(encodedAppPackage);
            if (!LeanCloud.getContext().getPackageName().equals(appPackage)) {
              Intent intent = new Intent();
              intent.setClassName(appPackage, PushService.class.getName());
              intent.setAction(SERVICE_RESTART_ACTION);
              LOGGER.d("try to start:" + appPackage + " from:"
                  + LeanCloud.getContext().getPackageName());
              try {
                LeanCloud.getContext().startService(intent);
              } catch (Exception ex) {
                LOGGER.e("failed to startService. cause: " + ex.getMessage());
              }
            }
          }
        } catch (Exception e) {

        }
        registerApp();
      }
    }
  };

  private static void registerApp() {
    Set<String> appSet = getRegisteredApps();
    if (appSet != null && LeanCloud.getContext() != null) {
      appSet.add(Base64Encoder.encode(LeanCloud.getContext().getPackageName()));
      PersistenceUtil.sharedInstance().saveContentToFile(JSON.toJSONString(appSet),
          getRegisterAppsFile());
    }
  }

  private static Set<String> getRegisteredApps() {
    if (LeanCloud.getContext() == null) {
      return null;
    }
    File registerFile = getRegisterAppsFile();
    Set<String> appSet = new HashSet<String>();
    if (registerFile.exists()) {
      String registerApps = PersistenceUtil.sharedInstance().readContentFromFile(registerFile);
      if (!StringUtil.isEmpty(registerApps)) {
        // catch parse Exception
        try {
          appSet.addAll(JSON.parseObject(registerApps, Set.class));
        } catch (Exception e) {
          LOGGER.e("getRegisteredApps", e);
        }
        return appSet;
      }
    }
    return appSet;
  }

  private static File getRegisterAppsFile() {
    File file =
        new File(Environment.getExternalStorageDirectory() + "/Android/data/leancloud/",
            "dontpanic.cp");
    if (file.exists()) {
      return file;
    } else {
      File folder =
          new File(Environment.getExternalStorageDirectory() + "/Android/data/leancloud/");
      folder.mkdirs();
      return file;
    }
  }
}
