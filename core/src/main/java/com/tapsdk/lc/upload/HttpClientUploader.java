package com.tapsdk.lc.upload;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.LCLogger;
import com.tapsdk.lc.LCFile;
import com.tapsdk.lc.callback.ProgressCallback;
import com.tapsdk.lc.network.DNSDetoxicant;
import com.tapsdk.lc.utils.LogUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public abstract class HttpClientUploader implements Uploader {
  private static LCLogger logger = LogUtil.getLogger(HttpClientUploader.class);
  private OkHttpClient client = new OkHttpClient.Builder()
          .connectTimeout(15, TimeUnit.SECONDS)
          .readTimeout(10, TimeUnit.SECONDS)
          .writeTimeout(10, TimeUnit.SECONDS)
          .dns(new DNSDetoxicant())
          .build();

  ProgressCallback progressCallback;

  private volatile boolean cancelled = false;
  protected static final int DEFAULT_RETRY_TIMES = 6;

  protected synchronized OkHttpClient getOKHttpClient() {
    return client;
  }

  protected LCFile avFile = null;

  public HttpClientUploader(LCFile file, ProgressCallback progressCallback) {
    this.avFile = file;
    this.progressCallback = progressCallback;
    cancelled = false;
  }

  protected Response executeWithRetry(Request request, int retry) throws LCException {
    if (retry > 0 && !isCancelled()) {
      try {
        Response response = getOKHttpClient().newCall(request).execute();
        if (response.code() / 100 == 2) {
          return response;
        } else {
          return executeWithRetry(request, retry - 1);
        }
      } catch (IOException e) {
        return executeWithRetry(request, retry - 1);
      }
    } else {
      throw new LCException(LCException.OTHER_CAUSE, "Upload File failure");
    }
  }

  public void publishProgress(int progress) {
    if (progressCallback != null) progressCallback.internalDone(progress, null);
  }

  // ignore interrupt so far.
  public boolean cancel(boolean interrupt) {
    if (cancelled) {
      return false;
    }
    cancelled = true;
    if (interrupt) {
      interruptImmediately();
    }
    return true;
  }

  public void interruptImmediately() {
  }

  public boolean isCancelled() {
    return cancelled;
  }
}
