package com.tapsdk.lc.core;

import com.tapsdk.lc.LCLogger;
import com.tapsdk.lc.cache.SystemSetting;

import com.tapsdk.lc.network.DNSDetoxicant;
import com.tapsdk.lc.service.AppAccessEndpoint;
import com.tapsdk.lc.service.AppRouterService;
import com.tapsdk.lc.service.RTMConnectionServerResponse;
import com.tapsdk.lc.utils.LogUtil;

import com.tapsdk.lc.utils.StringUtil;
import com.tapsdk.lc.json.JSON;
import io.reactivex.Observable;

import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * app router 请求
 * https://app-router.com/2/route?appId=EDR0rD8otnmzF7zNGgLasHzi-MdYXbMMI
 */
public class AppRouter {
  private static final LCLogger LOGGER = LogUtil.getLogger(AppRouter.class);
  private static final String APP_ROUTER_HOST = "https://app-router.com";
  private static AppRouter INSTANCE = null;

  public static synchronized AppRouter getInstance() {
    if (null == INSTANCE) {
      INSTANCE = new AppRouter();
    }
    return INSTANCE;
  }

  private static final String DEFAULT_SERVER_HOST_FORMAT = "https://%s.%s.%s";
  private static final String DEFAULT_SERVER_API = LeanService.API.toString();
  private static final String DEFAULT_SERVER_STAT = LeanService.STATS.toString();
  private static final String DEFAULT_SERVER_ENGINE = LeanService.ENGINE.toString();
  private static final String DEFAULT_SERVER_PUSH = LeanService.PUSH.toString();
  private static final String DEFAULT_SERVER_RTM_ROUTER = LeanService.RTM.toString();

  private static final String DEFAULT_REGION_EAST_CHINA = "lncldapi.com";
  private static final String DEFAULT_REGION_NORTH_CHINA = "lncld.net";
  private static final String DEFAULT_REGION_NORTH_AMERICA = "lncldglobal.com";

  private static final Set<String> NorthAmericaSpecialApps = new HashSet<>();
  static {
    NorthAmericaSpecialApps.add("143mgzglqmg4d0simqtn1zswggcro2ykugj76th8l38u3cm5");
    NorthAmericaSpecialApps.add("18ry1wsn1p7808tagf2ka7sy1omna3nihe45cet0ne4xhg46");
    NorthAmericaSpecialApps.add("7az5r9i0v95acx932a518ygz7mvr26uc7e3xxaq9s389sd2o");
    NorthAmericaSpecialApps.add("8FfQwpvihLHK4htqmtEvkNrv");
    NorthAmericaSpecialApps.add("AjQYwoIyObTeEkD16v1eCq55");
    NorthAmericaSpecialApps.add("E0mVu1VMWrwBodUFWBpWzLNV");
    NorthAmericaSpecialApps.add("J0Ev9alAhaS4IdnxBA95wKgn");
    NorthAmericaSpecialApps.add("Ol0Cw6zL1xP9IIqJpiSv9uYC");
    NorthAmericaSpecialApps.add("W9BCIPx2biwKiKfUvVJtc8kF");
    NorthAmericaSpecialApps.add("YHE5exCaW7UolMFJUtHvXTUY");
    NorthAmericaSpecialApps.add("glvame9g0qlj3a4o29j5xdzzrypxvvb30jt4vnvm66klph4r");
    NorthAmericaSpecialApps.add("iuuztdrr4mj683kbsmwoalt1roaypb5d25eu0f23lrfsthgn");
    NorthAmericaSpecialApps.add("kekxwm8uz1wtgxzvv5kitsgsammjcx4lcgm5b159qia5rqo5");
    NorthAmericaSpecialApps.add("msjqtclsfmfeznwvm29dqvuwddt3cqmziszf0rjddxho8eis");
    NorthAmericaSpecialApps.add("nHptjiXlt3g8mcraXYRDpYFT");
    NorthAmericaSpecialApps.add("nf3udjhnnsbe99qg04j7oslck4w1yp2geewcy1kp6wskbu5w");
    NorthAmericaSpecialApps.add("pFcwt2MaALYf70POa7bIqe0J");
    NorthAmericaSpecialApps.add("q3er6vs0dkawy15skjeuktf7l4eam438wn5jkts2j7fpf2y3");
    NorthAmericaSpecialApps.add("tsvezhhlefbdj1jbkohynipehgtpk353sfonvbtlyxaraqxy");
    NorthAmericaSpecialApps.add("wnDg0lPt0wcYGJSiHRwHBhD4");
  }

  public static LeanCloud.REGION getAppRegion(String applicationId) {
    if (LeanCloud.getRegion() != LeanCloud.REGION.NorthChina) {
      return LeanCloud.getRegion();
    }
    if (StringUtil.isEmpty(applicationId)) {
      return LeanCloud.REGION.NorthChina;
    }
    if (applicationId.endsWith("-MdYXbMMI") || NorthAmericaSpecialApps.contains(applicationId)) {
      return LeanCloud.REGION.NorthAmerica;
    }
    if (applicationId.endsWith("-9Nh9j0Va")) {
      return LeanCloud.REGION.EastChina;
    }
    return LeanCloud.REGION.NorthChina;
  }

  private Retrofit retrofit = null;
  private AppAccessEndpoint defaultEndpoint = null;
  private AppAccessEndpoint customizedEndpoint = new AppAccessEndpoint();

  protected AppRouter() {
    OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(new LoggingInterceptor())
            .dns(new DNSDetoxicant())
            .build();
    retrofit = new Retrofit.Builder()
            .baseUrl(APP_ROUTER_HOST)
            .addConverterFactory(AppConfiguration.getRetrofitConverterFactory())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(httpClient)
            .build();
  }

  protected AppAccessEndpoint buildDefaultEndpoint(String appId) {
    if (null == appId || appId.length() <= 8) {
      return null;
    }
    AppAccessEndpoint result = new AppAccessEndpoint();
    String appIdPrefix = appId.substring(0, 8).toLowerCase();
    LeanCloud.REGION region = getAppRegion(appId);
    String lastHost = "";
    switch (region) {
      case NorthChina:
        lastHost = DEFAULT_REGION_NORTH_CHINA;
        break;
      case EastChina:
        lastHost = DEFAULT_REGION_EAST_CHINA;
        break;
      case NorthAmerica:
        lastHost = DEFAULT_REGION_NORTH_AMERICA;
        break;
      default:
        LOGGER.w("Invalid region");
        break;
    }
    result.setApiServer(String.format(DEFAULT_SERVER_HOST_FORMAT, appIdPrefix, DEFAULT_SERVER_API, lastHost));
    result.setEngineServer(String.format(DEFAULT_SERVER_HOST_FORMAT, appIdPrefix, DEFAULT_SERVER_ENGINE, lastHost));
    result.setPushServer(String.format(DEFAULT_SERVER_HOST_FORMAT, appIdPrefix, DEFAULT_SERVER_PUSH, lastHost));
    result.setRtmRouterServer(String.format(DEFAULT_SERVER_HOST_FORMAT, appIdPrefix, DEFAULT_SERVER_RTM_ROUTER, lastHost));
    result.setStatsServer(String.format(DEFAULT_SERVER_HOST_FORMAT, appIdPrefix, DEFAULT_SERVER_STAT, lastHost));
    result.setTtl(36000 + System.currentTimeMillis() / 1000);
    return result;
  }

  private Observable<String> fetchServerFromRemote(final String appId, final LeanService service) {
    return fetchServerHostsInBackground(appId).map(new Function<AppAccessEndpoint, String>() {
      @Override
      public String apply(AppAccessEndpoint appAccessEndpoint) throws Exception {
        String result = "";
        switch (service) {
          case API:
            result = appAccessEndpoint.getApiServer();
            break;
          case ENGINE:
            result = appAccessEndpoint.getEngineServer();
            break;
          case PUSH:
            result = appAccessEndpoint.getPushServer();
            break;
          case RTM:
            result = appAccessEndpoint.getRtmRouterServer();
            break;
          case STATS:
            result = appAccessEndpoint.getStatsServer();
            break;
          default:
            break;
        }
        if (!StringUtil.isEmpty(result) && !result.startsWith("http")) {
          result = "https://" + result;
        }
        return result;
      }
    });
  }

  public boolean hasFrozenEndpoint() {
    return this.customizedEndpoint.hasSpecifiedEndpoint();
  }

  public void freezeEndpoint(final LeanService service, String host) {
    this.customizedEndpoint.freezeEndpoint(service, host);
  }

  protected void clearEndpoints() {
    this.customizedEndpoint.reset();
  }

  public Observable<String> getEndpoint(final String appId, final LeanService service) {
    return getEndpoint(appId, service, false);
  }

  private Observable<String> getEndpoint(final String appId, final LeanService service, boolean forceUpdate) {
    if (StringUtil.isEmpty(appId)) {
      LOGGER.e("application id is empty.");
      return Observable.just("");
    }
    if (appId.length() <= 8) {
      LOGGER.e("application id is invalid(too short):" + appId);
      return Observable.just("");
    }
    String fixedHost = this.customizedEndpoint.getServerHost(service);
    if (!StringUtil.isEmpty(fixedHost)) {
      return Observable.just(fixedHost);
    }

    if (forceUpdate) {
      // force to update from server.
      return fetchServerFromRemote(appId, service);
    }

    if (null == this.defaultEndpoint) {
      SystemSetting setting = AppConfiguration.getDefaultSetting();
      String cachedResult = null;
      if (null != setting) {
        cachedResult = setting.getString(getPersistenceKeyZone(appId, true), appId, "");
      }
      if (!StringUtil.isEmpty(cachedResult)) {
        defaultEndpoint = JSON.parseObject(cachedResult, AppAccessEndpoint.class);
        long currentSeconds = System.currentTimeMillis() / 1000;
        if (currentSeconds > defaultEndpoint.getTtl()) {
          defaultEndpoint = null;
        }
      }
      if (null == defaultEndpoint) {
        defaultEndpoint = buildDefaultEndpoint(appId);
      }
    }

    String result = "";
    switch (service) {
      case API:
        result = this.defaultEndpoint.getApiServer();
        break;
      case ENGINE:
        result = this.defaultEndpoint.getEngineServer();
        break;
      case PUSH:
        result = this.defaultEndpoint.getPushServer();
        break;
      case RTM:
        result = this.defaultEndpoint.getRtmRouterServer();
        break;
      case STATS:
        result = this.defaultEndpoint.getStatsServer();
        break;
      default:
        break;
    }
    if (!StringUtil.isEmpty(result) && !result.startsWith("http")) {
      result = "https://" + result;
    }
    return Observable.just(result);
  }

  public Observable<AppAccessEndpoint> fetchServerHostsInBackground(final String appId) {
    AppRouterService service = retrofit.create(AppRouterService.class);
    Observable<AppAccessEndpoint> result = service.getRouter(appId);
    if (AppConfiguration.isAsynchronized()) {
      result = result.subscribeOn(Schedulers.io());
    }
    AppConfiguration.SchedulerCreator creator = AppConfiguration.getDefaultScheduler();
    if (null != creator) {
      result = result.observeOn(creator.create());
    }
    return result.map(new Function<AppAccessEndpoint, AppAccessEndpoint>() {
      @Override
      public AppAccessEndpoint apply(AppAccessEndpoint appAccessEndpoint) throws Exception {
        // save result to local cache.
        LOGGER.d(appAccessEndpoint.toString());
        AppRouter.this.defaultEndpoint = appAccessEndpoint;
        AppRouter.this.defaultEndpoint.setTtl(appAccessEndpoint.getTtl() + System.currentTimeMillis() / 1000);
        SystemSetting setting = AppConfiguration.getDefaultSetting();
        if (null != setting) {
          String endPoints = JSON.toJSONString(AppRouter.this.defaultEndpoint);
          setting.saveString(getPersistenceKeyZone(appId, true), appId, endPoints);
        }
        return AppRouter.this.defaultEndpoint;
      }
    });
  }

  private Observable<RTMConnectionServerResponse> fetchRTMServerFromRemote(final String routerHost, final String appId,
                                                                           final String installationId, int secure) {
    LOGGER.d("fetchRTMServerFromRemote. router=" + routerHost + ", appId=" + appId
            + ", installationId=" + installationId);
    Retrofit tmpRetrofit = retrofit.newBuilder().baseUrl(routerHost).build();
    AppRouterService tmpService = tmpRetrofit.create(AppRouterService.class);
    Observable<RTMConnectionServerResponse> result = tmpService.getRTMConnectionServer(appId, installationId, secure);
    if (AppConfiguration.isAsynchronized()) {
      result = result.subscribeOn(Schedulers.io());
    }
    AppConfiguration.SchedulerCreator creator = AppConfiguration.getDefaultScheduler();
    if (null != creator) {
      result = result.observeOn(creator.create());
    }
    return result.map(new Function<RTMConnectionServerResponse, RTMConnectionServerResponse>() {
      @Override
      public RTMConnectionServerResponse apply(RTMConnectionServerResponse rtmConnectionServerResponse) throws Exception {
        SystemSetting setting = AppConfiguration.getDefaultSetting();
        if (null != rtmConnectionServerResponse && null != setting) {
          rtmConnectionServerResponse.setTtl(rtmConnectionServerResponse.getTtl() + System.currentTimeMillis() / 1000);
          String cacheResult = JSON.toJSONString(rtmConnectionServerResponse);
          setting.saveString(getPersistenceKeyZone(appId, false), routerHost, cacheResult);
        }
        return rtmConnectionServerResponse;
      }
    });
  }

  public Observable<RTMConnectionServerResponse> fetchRTMConnectionServer(final String routerHost, final String appId,
                                                                          final String installationId, int secure,
                                                                          boolean forceUpdate) {
    if (!forceUpdate) {
      RTMConnectionServerResponse cachedResponse = null;
      SystemSetting setting = AppConfiguration.getDefaultSetting();
      if (null != setting) {
        String cacheServer = setting.getString(getPersistenceKeyZone(appId, false), routerHost, "");
        if (!StringUtil.isEmpty(cacheServer)) {
          try {
            cachedResponse = JSON.parseObject(cacheServer, RTMConnectionServerResponse.class);
            long currentSeconds = System.currentTimeMillis()/1000;
            if (currentSeconds > cachedResponse.getTtl()) {
              // cache is out of date.
              setting.removeKey(getPersistenceKeyZone(appId, false), routerHost);
              cachedResponse = null;
            }
            if (null != cachedResponse) {
              return Observable.just(cachedResponse);
            }
          } catch (Exception ex) {
            cachedResponse = null;
            setting.removeKey(getPersistenceKeyZone(appId, false), routerHost);
          }
        }
      }
    }
    return fetchRTMServerFromRemote(routerHost, appId, installationId, secure);
  }

  protected String getPersistenceKeyZone(String appId, boolean forAPIEndpoints) {
    if (forAPIEndpoints) {
      return "com.avos.avoscloud.approuter." + appId;
    } else {
      return "com.avos.push.router.server.cache" + appId;
    }
  }

}
