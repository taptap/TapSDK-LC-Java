package com.tapsdk.lc.cache;

import com.tapsdk.lc.codec.MDFive;
import com.tapsdk.lc.core.AppConfiguration;

import java.io.File;

public class FileCache extends LocalStorage{
  private static final int MAX_FILE_BUF_SIZE = 4 * 1024 * 1024;
  private static FileCache INSTANCE = null;

  public static synchronized FileCache getIntance() {
    if (null == INSTANCE) {
      INSTANCE = new FileCache();
    }
    return INSTANCE;
  }

  private FileCache() {
    super(AppConfiguration.getFileCacheDir());
  }

  public String saveLocalFile(String name, File localFile) {
    return super.saveFile(name, localFile);
  }

  @Override
  public File getCacheFile(String url) {
    if (isDisableLocalCache()) {
      return null;
    }
    String urlMd5 = MDFive.computeMD5(url.getBytes());
    return super.getCacheFile(urlMd5);
  }
}
