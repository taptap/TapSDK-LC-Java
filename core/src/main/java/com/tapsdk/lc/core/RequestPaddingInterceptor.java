package com.tapsdk.lc.core;

import com.tapsdk.lc.LCCloud;
import com.tapsdk.lc.utils.StringUtil;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class RequestPaddingInterceptor implements Interceptor {

  public static final String HEADER_KEY_LC_APPID = "X-LC-Id";
  public static final String HEADER_KEY_LC_APPKEY = "X-LC-Key";
  public static final String HEADER_KEY_LC_HOOKKEY = "X-LC-Hook-Key";
  private static final String HEADER_KEY_LC_PROD_MODE = "X-LC-Prod";
  public static final String HEADER_KEY_LC_SIGN = "X-LC-Sign";
  private static final String HEADER_KEY_ACCEPT = "Accept";
  private static final String HEADER_KEY_CONTENT_TYPE = "Content-Type";
  private static final String HEADER_KEY_USER_AGENT = "User-Agent";
  private static final String DEFAULT_CONTENT_TYPE = "application/json";

  private static RequestSignature requestSignature = new GeneralRequestSignature();

  public static void changeRequestSignature(RequestSignature signature) {
    requestSignature = signature;
  }

  public Response intercept(Interceptor.Chain chain) throws IOException {
    Request originalRequest = chain.request();
    okhttp3.Request.Builder builder = originalRequest.newBuilder()
            .header(HEADER_KEY_LC_PROD_MODE, LCCloud.isProductionMode()?"1":"0")
            .header(HEADER_KEY_LC_APPID, LeanCloud.getApplicationId())
            .header(HEADER_KEY_LC_SIGN, requestSignature.generateSign())
            .header(HEADER_KEY_ACCEPT, DEFAULT_CONTENT_TYPE)
            .header(HEADER_KEY_CONTENT_TYPE, DEFAULT_CONTENT_TYPE)
            .header(HEADER_KEY_USER_AGENT, AppConfiguration.getUserAgent());

    if (!StringUtil.isEmpty(LeanCloud.getHookKey())) {
      builder = builder.header(HEADER_KEY_LC_HOOKKEY, LeanCloud.getHookKey());
    }

    Request newRequest = builder.build();
    return chain.proceed(newRequest);
  }
}
