package com.tapsdk.lc.push;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import com.tapsdk.lc.LeanCloud;
import com.tapsdk.lc.json.JSON;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.LCInstallation;
import com.tapsdk.lc.LCLogger;
import com.tapsdk.lc.LCObject;
import com.tapsdk.lc.callback.LCCallback;
import com.tapsdk.lc.core.AppConfiguration;
import com.tapsdk.lc.im.AndroidInitializer;
import com.tapsdk.lc.im.DirectlyOperationTube;
import com.tapsdk.lc.im.InternalConfiguration;
import com.tapsdk.lc.im.v2.LCIMMessage;
import com.tapsdk.lc.im.v2.LCIMMessageOption;
import com.tapsdk.lc.im.v2.Conversation;
import com.tapsdk.lc.im.v2.Conversation.LCIMOperation;
import com.tapsdk.lc.im.v2.LCIMClient.LCIMClientStatus;
import com.tapsdk.lc.livequery.LCLiveQuery;
import com.tapsdk.lc.session.LCConnectionManager;
import com.tapsdk.lc.session.LCSession;
import com.tapsdk.lc.session.LCSessionManager;
import com.tapsdk.lc.util.AndroidUtil;
import com.tapsdk.lc.utils.LogUtil;
import com.tapsdk.lc.utils.StringUtil;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import static com.tapsdk.lc.im.v2.LCIMClient.LCIMClientStatus.LCIMClientStatusNone;

/**
 * PushService
 */
public class PushService extends Service {
  private static final LCLogger LOGGER = LogUtil.getLogger(PushService.class);

  static final String AV_PUSH_SERVICE_APPLICATION_ID = "AV_APPLICATION_ID";
  static final String AV_PUSH_SERVICE_DEFAULT_CALLBACK = "AV_DEFAULT_CALLBACK";
  static final String SERVICE_RESTART_ACTION = "com.avos.avoscloud.notify.action";

  // 是否需要唤醒其他同样使用 LeanCloud 服务的 app，此变量用于缓存结果，避免无意义调用
  private static boolean isNeedNotifyApplication = true;

  private LCConnectionManager connectionManager = null;
  private static Object connecting = new Object();
  private volatile static boolean isStarted = false;

  private static boolean isAutoWakeUp = true;
  static String DefaultChannelId = "";

  static volatile boolean enableForegroundService = false;
  static int foregroundIdentifier = 0;
  static Notification foregroundNotification = null;

  LCConnectivityReceiver connectivityReceiver;
  LCShutdownReceiver shutdownReceiver;
  DirectlyOperationTube directlyOperationTube;
  private Timer cleanupTimer = new Timer();

  @Override
  public void onCreate() {
    LOGGER.d("PushService#onCreate");
    super.onCreate();

    AndroidNotificationManager.getInstance().setServiceContext(this);
    if (0 == AndroidNotificationManager.getInstance().getNotificationIcon()) {
      AndroidNotificationManager.getInstance().setNotificationIcon(this.getApplicationInfo().icon);
    }

    directlyOperationTube = new DirectlyOperationTube(true);

    connectionManager = LCConnectionManager.getInstance();
    new Thread(new Runnable() {
      @Override
      public void run() {
        connectionManager.startConnection(new LCCallback() {
          @Override
          protected void internalDone0(Object o, LCException avException) {
            if (null != avException) {
              LOGGER.w("failed to start websocket connection, cause: " + avException.getMessage());
            } else {
              LOGGER.d("succeed to start websocket connection.");
            }
          }
        });
      }
    }).start();

    connectivityReceiver = new LCConnectivityReceiver(new LCConnectivityListener() {
      private volatile boolean connectionEstablished = false;

      @Override
      public void onMobile(Context context) {
        LOGGER.d("Connection resumed with Mobile...");
        connectionEstablished = true;
        connectionManager.autoConnection();
      }

      @Override
      public void onWifi(Context context) {
        LOGGER.d("Connection resumed with Wifi...");
        connectionEstablished = true;
        connectionManager.autoConnection();
      }

      public void onOtherConnected(Context context) {
        LOGGER.d("Connectivity resumed with Others");
        connectionEstablished = true;
        connectionManager.autoConnection();
      }

      @Override
      public void onNotConnected(Context context) {
        if(!connectionEstablished) {
          LOGGER.d("Connectivity isn't established yet.");
          return;
        }
        LOGGER.d("Connectivity broken");
        connectionEstablished = false;
        cleanupTimer.schedule(new TimerTask() {
          @Override
          public void run() {
            if (!connectionEstablished) {
              LOGGER.d("reset Connection now.");
              connectionManager.resetConnection();
            } else {
              LOGGER.d("Connection has been resumed");
            }
          }
        }, 3000);
      }
    });

    // add try-catch for crash as:
    // https://stackoverflow.com/questions/41670527/java-lang-securityexceptionunable-to-find-app-for-caller-android-app-applicatio
    try {
      registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    } catch (SecurityException ex) {
      LOGGER.w("failed to register CONNECTIVITY receiver. cause: " + ex.getMessage());
    }

    shutdownReceiver = new LCShutdownReceiver(new LCShutdownListener() {
      @Override
      public void onShutdown(Context context) {
        connectionManager.cleanup();
      }
    });

    // add try-catch for crash as:
    // https://stackoverflow.com/questions/41670527/java-lang-securityexceptionunable-to-find-app-for-caller-android-app-applicatio
    try {
      registerReceiver(shutdownReceiver, new IntentFilter(Intent.ACTION_SHUTDOWN));
    } catch (SecurityException ex) {
      LOGGER.w("failed to register SHUTDOWN receiver. cause: " + ex.getMessage());
    }

    isStarted = true;
  }

  @TargetApi(Build.VERSION_CODES.ECLAIR)
  @Override
  public int onStartCommand(final Intent intent, int flags, int startId) {
    LOGGER.i("PushService#onStartCommand");
    if (enableForegroundService) {
      startForeground(foregroundIdentifier, foregroundNotification);
    } else {
      notifyOtherApplication(null != intent ? intent.getAction() : null);
    }

    boolean connected = AppConfiguration.getGlobalNetworkingDetector().isConnected();
    if (connected && !connectionManager.isConnectionEstablished()) {
      LOGGER.d("networking is fine and try to start connection to leancloud.");
      synchronized (connecting) {
        connectionManager.startConnection(new LCCallback<Integer>() {
          @Override
          protected void internalDone0(Integer resultCode, LCException exception) {
            if (null == exception) {
              processIMRequests(intent);
            } else {
              LOGGER.w("failed to start connection. cause:", exception);
              processRequestsWithException(intent, exception);
            }
          }
        });
      }
    } else if (!connected) {
      LOGGER.d("network is broken, try to re-connect to leancloud for user action.");
      if (connectionManager.isConnectionEstablished()) {
        connectionManager.cleanup();
      }
      synchronized (connecting) {
        connectionManager.startConnection(new LCCallback<Integer>() {
          @Override
          protected void internalDone0(Integer resultCode, LCException exception) {
            if (null == exception) {
              processIMRequests(intent);
            } else {
              LOGGER.w("failed to start connection. cause:", exception);
              processRequestsWithException(intent, exception);
            }
          }
        });
      }
    } else {
      processIMRequests(intent);
    }

    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    LOGGER.i("PushService#onDestroy");
    connectionManager.cleanup();

    if (enableForegroundService) {
      stopForeground(true);
    } else {
      try {
        unregisterReceiver(this.connectivityReceiver);
        unregisterReceiver(this.shutdownReceiver);
      } catch (Exception ex) {
        LOGGER.w("failed to unregister CONNECTIVITY/SHUTDOWN receiver. cause: " + ex.getMessage());
      }

      isStarted = false;

      if (isAutoWakeUp && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
        try {
          LOGGER.i("Let's try to wake PushService again");
          Intent i = new Intent(LeanCloud.getContext(), PushService.class);
          i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startService(i);
        } catch (Exception ex) {
          // i have tried my best.
          LOGGER.e("failed to start PushService. cause: " + ex.getMessage());
        }
      }
    }

    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    LOGGER.d("PushService#onBind");
    return null;
  }

  /*
 * https://groups.google.com/forum/#!topic/android-developers/H-DSQ4-tiac
 * @see android.app.Service#onTaskRemoved(android.content.Intent)
 */
  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  @Override
  public void onTaskRemoved(Intent rootIntent) {
    if (isAutoWakeUp) {
      LOGGER.i("try to restart service on task Removed");

      Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
      restartServiceIntent.setPackage(getPackageName());

      if (enableForegroundService && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(restartServiceIntent);
      } else {
        PendingIntent restartServicePendingIntent =
            PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmService =
            (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 500,
            restartServicePendingIntent);
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      super.onTaskRemoved(rootIntent);
    }
  }

  /**
   * Helper function to subscribe to push notifications with the default application icon.
   *
   * @param context This is used to access local storage to cache the subscription, so it must
   *                currently be a viable context.
   * @param channel A string identifier that determines which messages will cause a push
   *                notification to be sent to this client. The channel name must start with a letter and
   *                contain only letters, numbers, dashes, and underscores.
   * @param cls     This should be a subclass of Activity. An instance of this Activity is started when
   *                the user responds to this push notification. If you are not sure what to use here,
   *                just
   *                use your application's main Activity subclass.
   */
  public static synchronized void subscribe(android.content.Context context,
                                            java.lang.String channel, java.lang.Class<? extends android.app.Activity> cls) {
    startServiceIfRequired(context, cls);
    final String finalChannel = channel;
    LCInstallation.getCurrentInstallation().addUnique("channels", finalChannel);
    _installationSaveHandler.sendMessage(Message.obtain());

    if (cls != null) {
      LCNotificationManager manager = LCPushMessageListener.getInstance().getNotificationManager();
      manager.addDefaultPushCallback(channel, cls.getName());

      // set default push callback if it's not exist yet
      if (manager.getDefaultPushCallback(LeanCloud.getApplicationId()) == null) {
        manager.addDefaultPushCallback(LeanCloud.getApplicationId(), cls.getName());
      }
    }
  }

  /**
   * 设置推送消息的Icon图标，如果没有设置，默认使用您配置里的应用图标。
   *
   * @param icon A resource ID in the application's package of the drawble to use.
   * @since 1.4.4
   */
  public static void setNotificationIcon(int icon) {
    LCPushMessageListener.getInstance().getNotificationManager().setNotificationIcon(icon);
  }

  /**
   * 设置 PushService 以前台进程的方式运行（默认是 background service）。
   * Android 前台 Service 必须要显示一个 Notification 在通知栏，详见说明：
   * https://developer.android.com/guide/components/services
   *
   * @param enableForeground enable PushService run on foreground or not.
   * @param identifier The identifier for this notification as per
   * {@link NotificationManager#notify(int, Notification)
   * NotificationManager.notify(int, Notification)}; must not be 0.
   * @param notification The Notification to be displayed.
   */
  public static void setForegroundMode(boolean enableForeground, int identifier, Notification notification) {
    enableForegroundService = enableForeground;
    foregroundIdentifier = identifier;
    foregroundNotification = notification;
  }

  /**
   * Start Service explicitly.
   * In generally, you don't need to call this method to start service manually.
   * Only for LiveQuery, while you don't use LeanPush and LeanMessage, it is mandatory to call this method
   * within Application#onCreate, otherwise you will encounter issue on `com.tapsdk.lc.websocket.AVStandardWebSocketClient.send` invocation.
   *
   * @param context context
   */
  public static void startIfRequired(android.content.Context context) {
    startServiceIfRequired(context, null);
  }

  /**
   * Provides a default Activity class to handle pushes. Setting a default allows your program to
   * handle pushes that aren't registered with a subscribe call. This can happen when your
   * application changes its subscriptions directly through the AVInstallation or via push-to-query.
   *
   * @param context This is used to access local storage to cache the subscription, so it must
   *                currently be a viable context.
   * @param cls     This should be a subclass of Activity. An instance of this Activity is started when
   *                the user responds to this push notification. If you are not sure what to use here,
   *                just
   *                use your application's main Activity subclass.
   */
  public static void setDefaultPushCallback(android.content.Context context,
                                            java.lang.Class<? extends android.app.Activity> cls) {
    LOGGER.d("setDefaultPushCallback cls=" + cls.getName());
    startServiceIfRequired(context, cls);
    LCPushMessageListener.getInstance().getNotificationManager().addDefaultPushCallback(LeanCloud.getApplicationId(), cls.getName());
  }

  /**
   * Set whether to automatically wake up PushService
   * @param isAutoWakeUp the default value is true
   */
  public static void setAutoWakeUp(boolean isAutoWakeUp) {
    PushService.isAutoWakeUp = isAutoWakeUp;
  }

  /**
   * Set default channel for Android Oreo or newer version
   * Notice: it isn"t necessary to invoke this method for any Android version before Oreo.
   *
   * @param context   context
   * @param channelId default channel id.
   */
  @TargetApi(Build.VERSION_CODES.O)
  public static void setDefaultChannelId(Context context, String channelId) {
    DefaultChannelId = channelId;
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
      // do nothing for Android versions before Ore
      return;
    }

    try {
      NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      CharSequence name = context.getPackageName();
      String description = "PushNotification";
      int importance = NotificationManager.IMPORTANCE_DEFAULT;
      android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, name, importance);
      channel.setDescription(description);
      notificationManager.createNotificationChannel(channel);
    } catch (Exception ex) {
      LOGGER.w("failed to create NotificationChannel, then perhaps PushNotification doesn't work well on Android O and newer version.");
    }
  }

  /**
   * create Notification channel.
   * @param context context instance.
   * @param channelId channel id.
   * @param channelName channel name.
   * @param description The description of the channel.
   * @param importance The importance of the channel.
   * @param enableLights flag indicating enable lights or not.
   * @param lightColor light color.
   * @param enableVibration flag indicating enable vibration or not.
   * @param vibrationPattern vibration pattern.
   */
  @TargetApi(Build.VERSION_CODES.O)
  public static void createNotificationChannel(Context context, String channelId, String channelName,
                                            String description, int importance,
                                            boolean enableLights, int lightColor,
                                            boolean enableVibration, long[] vibrationPattern) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
      // do nothing for Android versions before Ore
      return;
    }

    try {
      NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, channelName, importance);
      channel.setDescription(description);
      channel.enableLights(enableLights);
      if (enableLights) {
        channel.setLightColor(lightColor);
      }
      channel.enableVibration(enableVibration);
      if (enableVibration) {
        channel.setVibrationPattern(vibrationPattern);
      }
      notificationManager.createNotificationChannel(channel);
    } catch (Exception ex) {
      LOGGER.w("failed to create NotificationChannel, then perhaps PushNotification doesn't work well on Android O and newer version.");
    }
  }

  /**
   * Cancels a previous call to subscribe. If the user is not subscribed to this channel, this is a
   * no-op. This call does not require internet access. It returns without blocking
   *
   * @param context A currently viable Context.
   * @param channel The string defining the channel to unsubscribe from.
   */
  public static synchronized void unsubscribe(android.content.Context context,
                                              java.lang.String channel) {
    if (channel == null) {
      return;
    }
    LCPushMessageListener.getInstance().getNotificationManager().removeDefaultPushCallback(channel);
    final java.lang.String finalChannel = channel;
    if (StringUtil.isEmpty(LCInstallation.getCurrentInstallation().getObjectId())) {
      LCInstallation.getCurrentInstallation().saveInBackground().subscribe(new Observer<LCObject>() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onNext(LCObject avObject) {
          LCInstallation.getCurrentInstallation().removeAll("channels", Arrays.asList(finalChannel));
          _installationSaveHandler.sendMessage(Message.obtain());
        }

        @Override
        public void onError(Throwable e) {
          LOGGER.w(e);
        }

        @Override
        public void onComplete() {

        }
      });
    } else {
      LCInstallation.getCurrentInstallation().removeAll("channels", Arrays.asList(finalChannel));
      _installationSaveHandler.sendMessage(Message.obtain());
    }
  }

//  @TargetApi(Build.VERSION_CODES.N)
  private static void startServiceIfRequired(Context context,
                                             final java.lang.Class<? extends android.app.Activity> cls) {
    if (isStarted) {
      return;
    }

    if (context == null) {
      LOGGER.d("context is null");
      return;
    }

    if (PackageManager.PERMISSION_GRANTED != AndroidUtil.checkPermission(context, "android.permission.INTERNET")) {
      LOGGER.e("Please add <uses-permission android:name=\"android.permission.INTERNET\"/> in your AndroidManifest file");
      return;
    }

    if (!isPushServiceAvailable(context, PushService.class)) {
      LOGGER.e("Please add <service android:name=\"com.tapsdk.lc.push.PushService\"/> in your AndroidManifest file");
      return;
    }

    if (!AppConfiguration.getGlobalNetworkingDetector().isConnected()) {
      LOGGER.d( "No network available now");
    }

    AndroidInitializer.init(context);

    startService(context, cls);
  }

  private static boolean isPushServiceAvailable(Context context, final java.lang.Class cls) {
    final PackageManager packageManager = context.getPackageManager();
    final Intent intent = new Intent(context, cls);
    List resolveInfo =
        packageManager.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY);
    if (resolveInfo.size() > 0) {
      return true;
    }
    return false;
  }

  private static synchronized void startService(final Context context, final java.lang.Class cls) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        LOGGER.d( "Start service");
        try {
          Intent intent = new Intent(context, PushService.class);
          intent.putExtra(AV_PUSH_SERVICE_APPLICATION_ID, LeanCloud.getApplicationId());
          if (cls != null) {
            intent.putExtra(AV_PUSH_SERVICE_DEFAULT_CALLBACK, cls.getName());
          }
          context.startService(intent);
        } catch (Exception ex) {
          // i have tried my best.
          LOGGER.e("failed to start PushService. cause: " + ex.getMessage());
        }
      }
    }).start();
  }

  private void notifyOtherApplication(final String action) {
    if (isNeedNotifyApplication && !SERVICE_RESTART_ACTION.equals(action)) {
      // 每次 app 启动只需要唤醒一次就行了
      isNeedNotifyApplication = false;

      try {
        ServiceInfo info = getApplicationContext().getPackageManager().getServiceInfo(
            new ComponentName(getApplicationContext(), PushService.class), 0);
        if(info.exported) {
          NotifyUtil.notifyHandler.sendEmptyMessage(NotifyUtil.SERVICE_RESTART);
        }
      } catch (PackageManager.NameNotFoundException e) {
      }
    }
  }

  private void processIMRequests(Intent intent) {
    LOGGER.d("processIMRequests...");
    if (null == intent) {
      LOGGER.w("intent is null, invalid operation.");
      return;
    }
    if (Conversation.LC_CONVERSATION_INTENT_ACTION.equalsIgnoreCase(intent.getAction())) {
      processIMRequestsFromClient(intent);
    } else {
      processLiveQueryRequestsFromClient(intent);
    }
  }

  private void processIMRequestsFromClient(Intent intent) {
    LOGGER.d("processIMRequestsFromClient...");

    String clientId = intent.getExtras().getString(Conversation.INTENT_KEY_CLIENT);

    int requestId = intent.getExtras().getInt(Conversation.INTENT_KEY_REQUESTID);
    Conversation.LCIMOperation operation = LCIMOperation.getIMOperation(
        intent.getExtras().getInt(Conversation.INTENT_KEY_OPERATION));

    String keyData = intent.getExtras().getString(Conversation.INTENT_KEY_DATA);
    LCIMMessage existedMessage = null;
    Map<String, Object> param = null;
    if (!StringUtil.isEmpty(keyData)) {
      param = JSON.parseObject(keyData, Map.class);
    }
    String conversationId = intent.getExtras().getString(Conversation.INTENT_KEY_CONVERSATION);
    int convType = intent.getExtras().getInt(Conversation.INTENT_KEY_CONV_TYPE, Conversation.CONV_TYPE_NORMAL);

    switch (operation) {
      case CLIENT_OPEN:
        String tag = (String) param.get(Conversation.PARAM_CLIENT_TAG);
        String userSession = (String) param.get(Conversation.PARAM_CLIENT_USERSESSIONTOKEN);
        boolean reConnection = (boolean) param.get(Conversation.PARAM_CLIENT_RECONNECTION);
        this.directlyOperationTube.openClientDirectly(connectionManager, clientId, tag, userSession, reConnection, requestId);
        break;
      case CLIENT_DISCONNECT:
        this.directlyOperationTube.closeClientDirectly(connectionManager, clientId, requestId);
        break;
      case CLIENT_REFRESH_TOKEN:
        this.directlyOperationTube.renewSessionTokenDirectly(connectionManager, clientId, requestId);
        break;
      case CLIENT_STATUS:
        LCSession session = LCSessionManager.getInstance().getOrCreateSession(clientId, LCInstallation.getCurrentInstallation().getInstallationId(), connectionManager);
        LCIMClientStatus status = LCIMClientStatusNone;
        if (LCSession.Status.Opened != session.getCurrentStatus()) {
          status = LCIMClientStatus.LCIMClientStatusPaused;
        } else {
          status = LCIMClientStatus.LCIMClientStatusOpened;
        }
        HashMap<String, Object> bundle = new HashMap<>();
        bundle.put(Conversation.callbackClientStatus, status.getCode());
        InternalConfiguration.getOperationTube().onOperationCompletedEx(clientId, null,
            requestId, LCIMOperation.CLIENT_STATUS, bundle);
        break;
      case CLIENT_ONLINE_QUERY:
        List<String> idList = (List<String>) param.get(Conversation.PARAM_ONLINE_CLIENTS);
        this.directlyOperationTube.queryOnlineClientsDirectly(connectionManager, clientId, idList, requestId);
        break;
      case CONVERSATION_CREATION:
        List<String> members = (List<String>) param.get(Conversation.PARAM_CONVERSATION_MEMBER);
        boolean isUnique = false;
        if (param.containsKey(Conversation.PARAM_CONVERSATION_ISUNIQUE)) {
          isUnique = (boolean) param.get(Conversation.PARAM_CONVERSATION_ISUNIQUE);
        }
        boolean isTransient = false;
        if (param.containsKey(Conversation.PARAM_CONVERSATION_ISTRANSIENT)) {
          isTransient = (boolean) param.get(Conversation.PARAM_CONVERSATION_ISTRANSIENT);
        }
        boolean isTemp = false;
        if (param.containsKey(Conversation.PARAM_CONVERSATION_ISTEMPORARY)) {
          isTemp = (boolean) param.get(Conversation.PARAM_CONVERSATION_ISTEMPORARY);
        }
        int tempTTL = isTemp ? (int) param.get(Conversation.PARAM_CONVERSATION_TEMPORARY_TTL) : 0;
        Map<String, Object> attributes = (Map<String, Object>) param.get(Conversation.PARAM_CONVERSATION_ATTRIBUTE);
        directlyOperationTube.createConversationDirectly(connectionManager, clientId, members, attributes, isTransient,
            isUnique, isTemp, tempTTL, requestId);
        break;
      case CONVERSATION_QUERY:
        this.directlyOperationTube.queryConversationsDirectly(connectionManager, clientId, keyData, requestId);
        break;
      case CONVERSATION_UPDATE:
        this.directlyOperationTube.updateConversationDirectly(connectionManager, clientId, conversationId, convType, param, requestId);
        break;
      case CONVERSATION_QUIT:
      case CONVERSATION_JOIN:
      case CONVERSATION_MUTE:
      case CONVERSATION_UNMUTE:
        this.directlyOperationTube.participateConversationDirectly(connectionManager, clientId, conversationId, convType,
            param, operation, requestId);
        break;
      case CONVERSATION_ADD_MEMBER:
      case CONVERSATION_RM_MEMBER:
      case CONVERSATION_MUTE_MEMBER:
      case CONVERSATION_UNMUTE_MEMBER:
      case CONVERSATION_UNBLOCK_MEMBER:
      case CONVERSATION_BLOCK_MEMBER:
      case CONVERSATION_PROMOTE_MEMBER:
      case CONVERSATION_BLOCKED_MEMBER_QUERY:
      case CONVERSATION_MUTED_MEMBER_QUERY:
      case CONVERSATION_FETCH_RECEIPT_TIME:
      case CONVERSATION_MEMBER_COUNT_QUERY:
        this.directlyOperationTube.processMembersDirectly(connectionManager, clientId, conversationId, convType, keyData,
            operation, requestId);
        break;
      case CONVERSATION_MESSAGE_QUERY:
        this.directlyOperationTube.queryMessagesDirectly(connectionManager, clientId, conversationId, convType, keyData,
            LCIMOperation.CONVERSATION_MESSAGE_QUERY, requestId);
        break;
      case CONVERSATION_READ:
        this.directlyOperationTube.markConversationReadDirectly(connectionManager, clientId, conversationId, convType,
            param, requestId);
        break;
      case CONVERSATION_RECALL_MESSAGE:
        existedMessage = LCIMMessage.parseJSONString(keyData);
        this.directlyOperationTube.recallMessageDirectly(connectionManager, clientId, convType, existedMessage, requestId);
        break;
      case CONVERSATION_SEND_MESSAGE:
        existedMessage = LCIMMessage.parseJSONString(keyData);;
        LCIMMessageOption option = LCIMMessageOption.parseJSONString(intent.getExtras().getString(Conversation.INTENT_KEY_MESSAGE_OPTION));
        this.directlyOperationTube.sendMessageDirectly(connectionManager, clientId, conversationId, convType,
            existedMessage, option, requestId);
        break;
      case CONVERSATION_UPDATE_MESSAGE:
        existedMessage = LCIMMessage.parseJSONString(keyData);;
        LCIMMessage secondMessage = LCIMMessage.parseJSONString(intent.getExtras().getString(Conversation.INTENT_KEY_MESSAGE_EX));
        this.directlyOperationTube.updateMessageDirectly(connectionManager, clientId, convType, existedMessage, secondMessage, requestId);
        break;
      default:
        LOGGER.w("not support operation: " + operation);
        break;
    }
  }

  private void processRequestsWithException(Intent intent, LCException exception) {
    if (intent != null
        && Conversation.LC_CONVERSATION_INTENT_ACTION.equalsIgnoreCase(intent.getAction())) {
      int operationCode = intent.getExtras().getInt(Conversation.INTENT_KEY_OPERATION);
      String clientId = intent.getExtras().getString(Conversation.INTENT_KEY_CLIENT);
      String conversationId = intent.getExtras().getString(Conversation.INTENT_KEY_CONVERSATION);
      int requestId = intent.getExtras().getInt(Conversation.INTENT_KEY_REQUESTID);

      InternalConfiguration.getOperationTube().onOperationCompleted(clientId, conversationId, requestId,
          Conversation.LCIMOperation.getIMOperation(operationCode), exception);
    }
  }

  private void processLiveQueryRequestsFromClient(Intent intent) {
    if (null == intent) {
      LOGGER.w("intent is null");
      return;
    }
    String action = intent.getAction();
    if (LCLiveQuery.ACTION_LIVE_QUERY_LOGIN.equals(action)) {
      int requestId = intent.getExtras().getInt(Conversation.INTENT_KEY_REQUESTID);
      String subscriptionId = intent.getExtras().getString(LCLiveQuery.SUBSCRIBE_ID);
      this.directlyOperationTube.loginLiveQueryDirectly(connectionManager, subscriptionId, requestId);
    } else {
      LOGGER.w("unknown action: " + action);
    }
  }

  private static Handler _installationSaveHandler = new Handler(Looper.getMainLooper()) {

    public void handleMessage(Message m) {

      LCInstallation.getCurrentInstallation().saveInBackground().subscribe(new Observer<LCObject>() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onNext(LCObject avObject) {

        }

        @Override
        public void onError(Throwable e) {
          if (e != null && "already has one request sending".equals(e.getMessage())) {
            _installationSaveHandler.removeMessages(0);
            Message m = Message.obtain();
            m.what = 0;
            _installationSaveHandler.sendMessageDelayed(m, 2000);
          }
        }

        @Override
        public void onComplete() {

        }
      });
    }
  };
}
