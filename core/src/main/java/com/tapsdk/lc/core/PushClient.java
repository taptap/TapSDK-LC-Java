package com.tapsdk.lc.core;

import com.tapsdk.lc.service.PushService;
import com.tapsdk.lc.utils.ErrorUtils;
import com.tapsdk.lc.json.JSONObject;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import java.util.Map;

public class PushClient {
  private PushService service = null;
  private boolean asynchronized = false;
  private AppConfiguration.SchedulerCreator defaultCreator = null;

  public PushClient(PushService service, boolean asyncRequest, AppConfiguration.SchedulerCreator observerSchedulerCreator) {
    this.service = service;
    this.asynchronized = AppConfiguration.isAsynchronized();
    this.defaultCreator = AppConfiguration.getDefaultScheduler();
    PaasClient.getGlobalOkHttpClient();
  }

  public Observable<JSONObject> sendPushRequest(Map<String, Object> param) {
    return wrapObservable(service.sendPushRequest(JSONObject.Builder.create(param)));
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
}
