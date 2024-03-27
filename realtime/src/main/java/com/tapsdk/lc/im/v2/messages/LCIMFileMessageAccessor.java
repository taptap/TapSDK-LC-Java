package com.tapsdk.lc.im.v2.messages;

import com.tapsdk.lc.callback.SaveCallback;
import com.tapsdk.lc.im.InternalConfiguration;

import java.io.File;
import java.util.Map;

public class LCIMFileMessageAccessor {
  public static void upload(LCIMFileMessage message, SaveCallback callback) {
    message.upload(callback);
  }

  public static Map<String, Object> mediaInfo(File file) {
    return InternalConfiguration.getFileMetaAccessor().mediaInfo(file);
  }

  public static String getMimeType(String url) {
    return InternalConfiguration.getFileMetaAccessor().getMimeType(url);
  }

  public static Map<String, Object> getImageMeta(File file) {
    return InternalConfiguration.getFileMetaAccessor().getImageMeta(file);
  }
}
