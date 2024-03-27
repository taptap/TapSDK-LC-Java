package com.tapsdk.lc.session;

import com.tapsdk.lc.LCLogger;
import com.tapsdk.lc.cache.SystemSetting;
import com.tapsdk.lc.core.AppConfiguration;
import com.tapsdk.lc.im.LCIMOptions;
import com.tapsdk.lc.im.Signature;
import com.tapsdk.lc.utils.LogUtil;
import com.tapsdk.lc.utils.StringUtil;
import com.tapsdk.lc.json.JSON;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SessionCacheHelper {
  private static final LCLogger LOGGER = LogUtil.getLogger(SessionCacheHelper.class);
  private static final String SESSION_KEY = "sessionids";
  private static SessionTagCache tagCacheInstance;

  public static synchronized SessionTagCache getTagCacheInstance() {
    if (null == tagCacheInstance) {
      tagCacheInstance = new SessionTagCache();
    }
    return tagCacheInstance;
  }

  public static class SessionTagCache {
    private final static String SESSION_TAG_CACHE_KEY = "session_tag_cache_key";
    Map<String, String> cachedTagMap = Collections.synchronizedMap(new HashMap<String, String>());
    private SessionTagCache() {
      syncLocalToMemory(cachedTagMap);
    }
    private synchronized void syncTagsToLocal(Map<String, String> map) {
      if (null != map) {
        AppConfiguration.getDefaultSetting().saveString(SESSION_KEY, SESSION_TAG_CACHE_KEY, JSON.toJSONString(map));
      }
    }
    private void syncLocalToMemory(Map<String, String> map) {
      SystemSetting setting = AppConfiguration.getDefaultSetting();
      String sessionIdsString = setting.getString(SESSION_KEY, SESSION_TAG_CACHE_KEY, "{}");
      Map<String, String> sessionIds = JSON.parseObject(sessionIdsString, HashMap.class);
      if (null != sessionIds && sessionIds.isEmpty()) {
        map.clear();
        map.putAll(sessionIds);
      }
    }

    void addSession(String clientId, String tag) {
      cachedTagMap.put(clientId, tag);
      if (LCIMOptions.getGlobalOptions().isAutoOpen()) {
        syncTagsToLocal(cachedTagMap);
      }
    }
     void removeSession(String clientId) {
       if (cachedTagMap.containsKey(clientId)) {
         cachedTagMap.remove(clientId);
         if (LCIMOptions.getGlobalOptions().isAutoOpen()) {
           syncTagsToLocal(cachedTagMap);
         }
       }
     }
    public Map<String, String> getAllSession() {
      HashMap<String, String> sessionMap = new HashMap<>();
      sessionMap.putAll(cachedTagMap);
      return sessionMap;
    }
  }

  public static class SignatureCache {
    private static final String SESSION_SIGNATURE_KEY = "com.avos.avoscloud.session.signature";
    public static void addSessionSignature(String clientId, Signature signature) {
      Map<String, Signature> signatureMap = getSessionSignatures();
      signatureMap.put(clientId, signature);
      SystemSetting setting = AppConfiguration.getDefaultSetting();
      setting.saveString(SESSION_SIGNATURE_KEY, SESSION_KEY,
              JSON.toJSONString(signatureMap));
    }

    public static Signature getSessionSignature(String clientId) {
      Map<String, Signature> signatureMap = getSessionSignatures();
      return signatureMap.get(clientId);
    }
    private static Map<String, Signature> getSessionSignatures() {
      SystemSetting setting = AppConfiguration.getDefaultSetting();
      String sessionSignatureString = setting.getString(SESSION_SIGNATURE_KEY, SESSION_KEY, "{}");
      Map<String, Signature> signatureMap = JSON.parseObject(sessionSignatureString, Map.class);
      return signatureMap;
    }
  }

  static class IMSessionTokenCache {
    private static final String SESSION_TOKEN_KEY = "com.avos.avoscloud.session.token";

    /**
     * 用来缓存 sessionToken，sessionToken 用来做自动登录使用
     */
    private static final Map<String, String> imSessionTokenMap = new HashMap<>();

    static String getIMSessionToken(String clientId) {
      if (LCIMOptions.getGlobalOptions().isAutoOpen()) {
        SystemSetting setting = AppConfiguration.getDefaultSetting();
        String token = setting.getString(SESSION_TOKEN_KEY, clientId, null);
        String expiredAt = setting.getString(SESSION_TOKEN_KEY, getSessionTokenExpiredAtKey(clientId), null);
        if (!StringUtil.isEmpty(token) && !StringUtil.isEmpty(expiredAt)) {
          try {
            long expiredAtInLong = Long.parseLong(expiredAt);
            if (expiredAtInLong > System.currentTimeMillis()) {
              return token;
            }
          } catch (Exception e) {
            LOGGER.w(e);
          }
        }
      } else {
        if (imSessionTokenMap.containsKey(clientId)) {
          return imSessionTokenMap.get(clientId);
        }
      }
      return null;
    }

    /**
     * 将 sessionToken 写入缓存
     * 如果自动登录为 true，则写入本地缓存，否则只写入内存，默认写入内存的有效期为当前 app 的生命周期内
     *
     * @param clientId
     * @param realtimeSessionToken
     * @param expireInSec
     */
    static void addIMSessionToken(String clientId, String realtimeSessionToken, long expireInSec) {
      if (LCIMOptions.getGlobalOptions().isAutoOpen()) {
        SystemSetting setting = AppConfiguration.getDefaultSetting();
        setting.saveString(SESSION_TOKEN_KEY, clientId, realtimeSessionToken);
        setting.saveString(SESSION_TOKEN_KEY, getSessionTokenExpiredAtKey(clientId), String.valueOf(expireInSec));
      } else {
        imSessionTokenMap.put(clientId, realtimeSessionToken);
      }
    }

    /**
     * 删除 client 对应的 sessionToken
     *
     * @param clientId
     */
    static void removeIMSessionToken(String clientId) {
      if (LCIMOptions.getGlobalOptions().isAutoOpen()) {
        SystemSetting setting = AppConfiguration.getDefaultSetting();
        setting.removeKey(SESSION_TOKEN_KEY, clientId);
        setting.removeKey(SESSION_TOKEN_KEY, getSessionTokenExpiredAtKey(clientId));
      } else {
        imSessionTokenMap.remove(clientId);
      }
    }

    private static String getSessionTokenExpiredAtKey(String clientId) {
      return clientId + ".expiredAt";
    }
  }
}
