package com.tapsdk.lc;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import junit.framework.TestCase;


import java.util.concurrent.CountDownLatch;

public class LCCachedUserTest extends TestCase {
  private boolean operationSucceed = false;
  public static final String USERNAME = "jfeng";
  public static final String PASSWORD = "FER$@$@#Ffwe";
  private final LCUser targetUser;
  public LCCachedUserTest(String name) {
    super(name);
    Configure.initializeRuntime();
    targetUser = LCUserTest.loginOrSignin(USERNAME, PASSWORD, LCUserTest.EMAIL);
  }
  @Override
  protected void setUp() throws Exception {
    operationSucceed = false;
  }

  @Override
  protected void tearDown() throws Exception {
    LCUser.logOut();
  }

  public void testQueryUser() throws Exception {
    String userSessionToken = targetUser.getSessionToken();
    final CountDownLatch latch = new CountDownLatch(1);
    LCUser.becomeWithSessionToken(userSessionToken).refreshInBackground().subscribe(new Observer<LCObject>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(LCObject lcObject) {
        System.out.println("refresh result: "+ lcObject.toJSONString());
        LCFile iconFile = lcObject.getLCFile("icon");
        if (null != iconFile) {
          System.out.println("icon result: " + iconFile.toJSONString());
        }
        LCUser.changeCurrentUser((LCUser) lcObject, true);
        operationSucceed = true;
        latch.countDown();
      }

      @Override
      public void onError(Throwable throwable) {
        if (throwable.getMessage().indexOf("Could not find user.") >= 0) {
          operationSucceed = true;
        }
        latch.countDown();
      }

      @Override
      public void onComplete() {

      }
    });
    latch.await();
    assertEquals(true, operationSucceed);
  }
}
