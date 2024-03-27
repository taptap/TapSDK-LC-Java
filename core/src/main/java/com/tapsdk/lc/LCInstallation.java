package com.tapsdk.lc;

import com.tapsdk.lc.annotation.LCClassName;
import com.tapsdk.lc.cache.PersistenceUtil;
import com.tapsdk.lc.codec.MDFive;
import com.tapsdk.lc.core.AppConfiguration;
import com.tapsdk.lc.core.LeanCloud;
import com.tapsdk.lc.utils.LogUtil;
import com.tapsdk.lc.utils.StringUtil;

import java.io.File;
import java.util.TimeZone;
import java.util.UUID;

@LCClassName("_Installation")
public final class LCInstallation extends LCObject {
  public static final String CLASS_NAME = "_Installation";

  private static final LCLogger LOGGER = LogUtil.getLogger(LCInstallation.class);
  static final String INSTALLATION = "installation";
  private static final String DEVICETYPETAG = "deviceType";
  private static final String CHANNELSTAG = "channel";
  private static final String INSTALLATIONIDTAG = "installationId";
  private static final String TIMEZONE = "timeZone";
  public static final String REGISTRATION_ID = "registrationId";
  public static final String VENDOR = "vendor";
  private static String DEFAULT_DEVICETYPE = "android";
  private static volatile LCInstallation currentInstallation;

  public LCInstallation() {
    super(CLASS_NAME);
    this.totallyOverwrite = true;
    initialize();
    this.endpointClassName = "installations";
  }

  protected LCInstallation(LCObject obj) {
    this.objectId = obj.getObjectId();
    this.acl = obj.getACL();
    this.serverData = obj.getServerData();
    this.totallyOverwrite = true;
    this.endpointClassName = "installations";
  }

  public static LCInstallation getCurrentInstallation() {
    if (null == currentInstallation) {
      synchronized (LCInstallation.class) {
        if (null == currentInstallation) {
          currentInstallation = createInstanceFromLocal(INSTALLATION);
        }
      }
    }
    return currentInstallation;
  }

  private static File getCacheFile() {
    String cacheBase = AppConfiguration.getImportantFileDir();
    if (StringUtil.isEmpty(cacheBase)) {
      return null;
    }
    return new File(cacheBase, LeanCloud.getSimplifiedAppId() + INSTALLATION);
  }

  protected static LCInstallation createInstanceFromLocal(String fileName) {
    File installationFile = getCacheFile();

    String newInstallationId = genInstallationId();

    if (null != installationFile) {
      LOGGER.d("installation cache file path: " + installationFile.getAbsolutePath());
      if (!installationFile.exists()) {
        String cacheBase = AppConfiguration.getImportantFileDir();
        File oldInstallationFile = new File(cacheBase, INSTALLATION);
        if (oldInstallationFile.exists()) {
          boolean tmp = oldInstallationFile.renameTo(installationFile);
          if (!tmp) {
            LOGGER.w("failed to rename installation cache file.");
          }
        }
      }

      if (installationFile.exists()) {
        String json = PersistenceUtil.sharedInstance().readContentFromFile(installationFile);
        if (!StringUtil.isEmpty(json)) {
          if (json.indexOf("{") >= 0) {
            try {
              currentInstallation = (LCInstallation) LCObject.parseLCObject(json);
              currentInstallation.totallyOverwrite = true;
            } catch (Exception ex) {
              LOGGER.w("failed to parse local installation data.", ex);
            }
          } else {
            if (json.length() == UUID_LEN) {
              // old sdk version.
              newInstallationId = json;
            }
          }
        } else {
          LOGGER.d("installation cache file is empty, create new instance.");
        }
      }
    }
    if (null == currentInstallation) {
      String json = String.format("{ \"_version\":\"5\",\"className\":\"_Installation\"," +
                      "\"serverData\":{\"@type\":\"java.util.concurrent.ConcurrentHashMap\"," +
                      "\"deviceType\":\"android\",\"installationId\":\"%s\"," +
                      "\"timeZone\":\"%s\"}}",
              newInstallationId, timezone());
      PersistenceUtil.sharedInstance().saveContentToFile(json, installationFile);
      LOGGER.d("create-ahead installation with json: " + json);
      try {
        currentInstallation = (LCInstallation) LCObject.parseLCObject(json);
        currentInstallation.totallyOverwrite = true;
      } catch (Exception ex) {
        LOGGER.w("failed to parse create-ahead installation string.", ex);
        currentInstallation = new LCInstallation();
        currentInstallation.setInstallationId(newInstallationId);
      }
    }
    return currentInstallation;
  }

  public static void changeDeviceType(String deviceType) {
    DEFAULT_DEVICETYPE = deviceType;
  }

  private static String deviceType() {
    return DEFAULT_DEVICETYPE;
  }
  private static String timezone() {
    TimeZone defaultTimezone = TimeZone.getDefault();
    return defaultTimezone.getID();
  }

  private void initialize() {
    if (currentInstallation != null) {
      this.put(INSTALLATIONIDTAG, currentInstallation.getInstallationId());
    } else {
      String installationId = genInstallationId();
      if (!StringUtil.isEmpty(installationId)) {
        this.put(INSTALLATIONIDTAG, installationId);
      }
    }
    this.put(DEVICETYPETAG, deviceType());
    this.put(TIMEZONE, timezone());
  }

  private static String genInstallationId() {
    // app的包名
    String packageName = AppConfiguration.getApplicationPackageName();
    String additionalStr = UUID.randomUUID().toString();
    return MDFive.computeMD5(packageName + additionalStr);
  }

  public String getInstallationId() {
    return this.getString(INSTALLATIONIDTAG);
  }

  public static LCQuery<LCInstallation> getQuery() {
    LCQuery<LCInstallation> query = new LCQuery<LCInstallation>(CLASS_NAME);
    return query;
  }

  @Override
  protected void onSaveSuccess() {
    super.onSaveSuccess();
    updateCurrentInstallationCache();
  }

  @Override
  protected void onDataSynchronized() {
    super.onDataSynchronized();
    updateCurrentInstallationCache();
  }

  void updateCurrentInstallationCache() {
    if (currentInstallation == this) {
      File installationFile = getCacheFile();
      String jsonString = currentInstallation.toJSONString();
      PersistenceUtil.sharedInstance().saveContentToFile(jsonString, installationFile);
    }
  }

  void setInstallationId(String installationId) {
    this.put(INSTALLATIONIDTAG, installationId);
  }
}
