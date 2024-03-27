package com.tapsdk.lc.service;

import com.tapsdk.lc.core.*;
import com.tapsdk.lc.im.Signature;
import com.tapsdk.lc.im.v2.conversation.LCIMConversationMemberInfo;
import com.tapsdk.lc.utils.ErrorUtils;
import com.tapsdk.lc.json.JSONObject;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RealtimeClient {
  private static RealtimeClient instance = null;
  public static RealtimeClient getInstance() {
    if (null == instance) {
      synchronized (RealtimeClient.class) {
        if (null == instance) {
          instance = new RealtimeClient();
        }
      }
    }
    return instance;
  }

  private RealtimeService service = null;
  private boolean asynchronized = false;
  private AppConfiguration.SchedulerCreator defaultCreator = null;

  private RealtimeClient() {
    this.asynchronized = AppConfiguration.isAsynchronized();
    this.defaultCreator = AppConfiguration.getDefaultScheduler();
    final OkHttpClient httpClient = PaasClient.getGlobalOkHttpClient();
    AppRouter appRouter = AppRouter.getInstance();
    appRouter.getEndpoint(LeanCloud.getApplicationId(), LeanService.API).subscribe(
            new Consumer<String>() {
              @Override
              public void accept(String apiHost) throws Exception {
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(apiHost)
                        .addConverterFactory(GsonConverterFactory.create())
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .client(httpClient)
                        .build();
                service = retrofit.create(RealtimeService.class);
              }
            });
  }

  private Observable wrapObservable(Observable observable) {
    if (null == observable) {
      return null;
    }
    if (asynchronized) {
      observable = observable.subscribeOn(Schedulers.io());
    }
    if (null != defaultCreator) {
      observable = observable.observeOn(defaultCreator.create());
    }
    observable = observable.onErrorResumeNext(new Function<Throwable, ObservableSource>() {
      @Override
      public ObservableSource apply(Throwable throwable) throws Exception {
        return Observable.error(ErrorUtils.propagateException(throwable));
      }
    });
    return observable;
  }

  public Observable<Signature> createSignature(Map<String, Object> params) {
    return wrapObservable(service.createSignature(JSONObject.Builder.create(params)));
  }

  public Observable<List<LCIMConversationMemberInfo>> queryMemberInfo(Map<String, String> query, String rtmSessionToken) {
    return wrapObservable(service.queryMemberInfo(rtmSessionToken, query))
            .map(new Function<Map<String, List<Map<String, Object>>>, List<LCIMConversationMemberInfo>>() {
              @Override
              public List<LCIMConversationMemberInfo> apply(Map<String, List<Map<String, Object>>> rawResult) throws Exception {
                List<Map<String, Object>> objects = rawResult.get("results");
                List<LCIMConversationMemberInfo> result = new LinkedList<LCIMConversationMemberInfo>();
                for (Map<String, Object> object: objects) {
                  LCIMConversationMemberInfo tmp = LCIMConversationMemberInfo.createInstance(object);
                  result.add(tmp);
                }
                return result;
              }
            });
  }

  public Observable<JSONObject> subscribeLiveQuery(Map<String, Object> params) {
    return wrapObservable(service.subscribe(JSONObject.Builder.create(params))).map(new Function<Map<String, Object>, JSONObject>() {
      @Override
      public JSONObject apply(Map<String, Object> o) throws Exception {
        return AppConfiguration.getJsonParser().toJSONObject(o);
      }
    });
  }
  public Observable<JSONObject> unsubscribeLiveQuery(Map<String, Object> params) {
    return wrapObservable(service.unsubscribe(JSONObject.Builder.create(params))).map(new Function<Map<String, Object>, JSONObject>() {
      @Override
      public JSONObject apply(Map<String, Object> o) throws Exception {
        return AppConfiguration.getJsonParser().toJSONObject(o);
      }
    });
  }
}
