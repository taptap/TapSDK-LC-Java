package com.tapsdk.lc.util;

import android.webkit.MimeTypeMap;

import java.io.File;

import com.tapsdk.lc.utils.FileUtil;
import com.tapsdk.lc.utils.StringUtil;

public class AndroidMimeTypeDetector implements FileUtil.MimeTypeDetector {
  public String getMimeTypeFromUrl(String url) {
    if (!StringUtil.isEmpty(url)) {
      String extension = MimeTypeMap.getFileExtensionFromUrl(url);
      return getMimeTypeFromExtension(extension);
    }
    return "";
  }

  public String getMimeTypeFromPath(String filePath) {
    if (!StringUtil.isEmpty(filePath)) {
      String fileName = new File(filePath).getName();
      String extension = FileUtil.getExtensionFromFilename(fileName);
      if (!StringUtil.isEmpty(extension)) {
        return getMimeTypeFromExtension(extension);
      }
    }
    return "";
  }

  public String getMimeTypeFromExtension(String extension) {
    if (!StringUtil.isEmpty(extension)) {
      return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }
    return "";
  }
}
