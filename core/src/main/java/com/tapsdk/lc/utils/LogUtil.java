package com.tapsdk.lc.utils;

import com.tapsdk.lc.LCLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LogUtil {
  private static Map<String, LCLogger> loggerCache = new ConcurrentHashMap<>();

  public static LCLogger getLogger(Class clazz) {
    if (null == clazz) {
      return null;
    }
    if (loggerCache.containsKey(clazz.getCanonicalName())) {
      return loggerCache.get(clazz.getCanonicalName());
    }
    LCLogger ret = new LCLogger(clazz.getSimpleName());
    loggerCache.put(clazz.getCanonicalName(), ret);
    return ret;
  }
}
