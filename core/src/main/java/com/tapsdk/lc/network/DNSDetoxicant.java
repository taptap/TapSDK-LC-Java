package com.tapsdk.lc.network;

import com.tapsdk.lc.core.AppConfiguration;
import com.tapsdk.lc.utils.StringUtil;
import okhttp3.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DNSDetoxicant implements Dns {
  static final long TWENTY_MIN_IN_MILLS = 20 * 60 * 1000L;
  static final String AVOS_SERVER_HOST_ZONE = "avoscloud_server_host_zone";
  public static final String EXPIRE_TIME = ".expireTime";
  static final String DNSPOD_HOST = System.getProperty("dnspodHost","119.29.29.29");

  // DNS 请求的超时时间设置为两秒
  private static final int DNS_REQUEST_TIME_OUT = 2 * 1000;

  public List<InetAddress> lookup(String hostname) throws UnknownHostException {
    if (StringUtil.isEmpty(hostname)) {
      throw new UnknownHostException("hostname is empty");
    }
    try {
      InetAddress[] addresses = InetAddress.getAllByName(hostname);
      return Arrays.asList(addresses);
    } catch (UnknownHostException e) {
      try {
        String response = getCacheDNSResult(hostname);
        boolean isCacheValid = !StringUtil.isEmpty(response);
        if (!isCacheValid) {
          response = getIPByHostSync(hostname);
        }
        InetAddress[] addresses = getIPAddress(hostname, response);
        if (!isCacheValid) {
          cacheDNS(hostname, response);
        }
        return Arrays.asList(addresses);
      } catch (Exception e1) {
        throw new UnknownHostException();
      }
    }
  }
  public static String getIPByHostSync(String host) throws Exception {
    HttpUrl httpUrl = new HttpUrl.Builder().scheme("http").host(DNSPOD_HOST)
            .addPathSegment("d").addQueryParameter("dn", host).build();

    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder.connectTimeout(DNS_REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
    builder.dns(Dns.SYSTEM);
    OkHttpClient okHttpClient = builder.build();
    Request request = new Request.Builder().url(httpUrl).get().build();

    try {
      Response response = okHttpClient.newCall(request).execute();
      if (null != response && response.isSuccessful() && null != response.body()) {
        return response.body().string();
      } else {
        return "";
      }
    } catch (IOException e) {
      return "";
    }
  }

  private void cacheDNS(String host, String response) {
    AppConfiguration.getDefaultSetting().saveString(AVOS_SERVER_HOST_ZONE, host, response);
    AppConfiguration.getDefaultSetting().saveString(AVOS_SERVER_HOST_ZONE,
            host + EXPIRE_TIME, String.valueOf(System.currentTimeMillis() + TWENTY_MIN_IN_MILLS));
  }

  private String getCacheDNSResult(String url) {
    String response = AppConfiguration.getDefaultSetting().getString(AVOS_SERVER_HOST_ZONE, url, null);
    String expiredAt = AppConfiguration.getDefaultSetting().getString(AVOS_SERVER_HOST_ZONE,
            url + EXPIRE_TIME, "0");

    if (!StringUtil.isEmpty(response) && System.currentTimeMillis() < Long.parseLong(expiredAt)) {
      return response;
    } else {
      return null;
    }
  }

  private static InetAddress[] getIPAddress(String url, String response) throws Exception {
    String[] ips = response.split(";");
    InetAddress[] addresses = new InetAddress[ips.length];
    Constructor constructor =
            InetAddress.class.getDeclaredConstructor(int.class, byte[].class, String.class);
    constructor.setAccessible(true);
    for (int i = 0; i < ips.length; i++) {
      String ip = ips[i];
      String[] ipSegment = ip.split("\\.");
      if (ipSegment.length == 4) {
        byte[] ipInBytes =
                {(byte) Integer.parseInt(ipSegment[0]), (byte) Integer.parseInt(ipSegment[1]),
                        (byte) Integer.parseInt(ipSegment[2]), (byte) Integer.parseInt(ipSegment[3])};
        addresses[i] = (InetAddress) constructor.newInstance(2, ipInBytes, url);
      }
    }
    return addresses;
  }

}
