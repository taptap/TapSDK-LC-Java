package com.tapsdk.lc;

import com.tapsdk.lc.auth.UserBasedTestCase;
import com.tapsdk.lc.utils.StringUtil;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import junit.framework.Test;
import junit.framework.TestSuite;


import java.util.concurrent.CountDownLatch;

public class LCUserSerializerTest extends UserBasedTestCase {
  private CountDownLatch latch = null;
  private boolean testSucceed = false;
  private String testUserObjectId = null;

  public LCUserSerializerTest(String name) {
    super(name);
    Configure.initializeRuntime();
  }

  public static Test suite() {
    return new TestSuite(LCUserSerializerTest.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    testSucceed = false;
    latch = new CountDownLatch(1);
    if (null != LCUser.getCurrentUser()) {
      testUserObjectId = LCUser.getCurrentUser().getObjectId();
    } else {
      testUserObjectId = null;
    }
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testUserFetch() throws Exception {
    if (StringUtil.isEmpty(testUserObjectId)) {
      LCUser targetUser = LCUserTest.loginOrSignin(LCUserTest.USERNAME,LCUserTest.PASSWORD, LCUserTest.EMAIL);
      testUserObjectId = targetUser.getObjectId();
    }
    final LCUser user = LCObject.createWithoutData(LCUser.class, testUserObjectId);
    user.fetchInBackground("author, kuolie, black").subscribe(new Observer<LCObject>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(LCObject object) {
        System.out.println(user);
        LCUser.changeCurrentUser(user, true);
        testSucceed = true;
        latch.countDown();
      }

      @Override
      public void onError(Throwable throwable) {
        throwable.printStackTrace();
        latch.countDown();
      }

      @Override
      public void onComplete() {

      }
    });
    latch.await();
    assertTrue(testSucceed);
  }

  public void testDeserializedUser() throws Exception {
    String jsonString = "{ \"_version\":\"5\",\"className\":\"_User\"," +
            "\"serverData\":{\"@type\":\"java.util.concurrent.ConcurrentHashMap\",\"signDate\":new Date(1594310400000)," +
            "\"username\":\"变音小助手\",\"siOffDate\":new Date(1594310400000)}}";
    LCUser user = (LCUser) LCObject.parseLCObject(jsonString);
    assertTrue(null != user);

    try {
      // gson doesnot support "new Date".
      jsonString = "{ \"_version\":\"5\",\"className\":\"_User\"," +
              "\"serverData\":{\"@type\":\"java.util.concurrent.ConcurrentHashMap\",\"signDate\":new Date(1594310400000)," +
              "\"sessionToken\":[new Date(1594310200000), new Date(1594310420000)],\"username\":\"变音小助手\",\"siOffDate\":new Date(1594310400000)}}";
      user = (LCUser) LCObject.parseLCObject(jsonString);
      assertTrue(null != user);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}
