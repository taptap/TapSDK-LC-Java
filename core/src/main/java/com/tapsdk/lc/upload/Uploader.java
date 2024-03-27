package com.tapsdk.lc.upload;

import com.tapsdk.lc.LCException;

public interface Uploader {
  LCException execute();

  void publishProgress(int percentage);

  boolean cancel(boolean interrupt);

  boolean isCancelled();
}
