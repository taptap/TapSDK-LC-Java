package com.tapsdk.lc;

import com.tapsdk.lc.auth.UserBasedTestCase;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import junit.framework.TestCase;


import java.util.concurrent.CountDownLatch;

public class LCQueryWithMasterKeyTest extends UserBasedTestCase {
  private CountDownLatch latch = null;
  private boolean testSucceed = false;
  private String currentObjectId = null;
  public LCQueryWithMasterKeyTest(String testName) {
    super(testName);
  }

  @Override
  protected void setUp() throws Exception {
    latch = new CountDownLatch(1);
    testSucceed = false;
  }

  @Override
  protected void tearDown() throws Exception {
    latch = null;
  }

  public void testQueryPointer() throws Exception {
    LCQuery query = new LCQuery("AudioItem");
    LCObject packageItem = LCObject.createWithoutData("PackageItem", "5c94b0d90237d70068873691");
    query.whereEqualTo("packageItem", packageItem);
    query.include("packageItem");
    query.limit(10);
    query.findInBackground().subscribe(new Observer() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(Object o) {
        latch.countDown();
      }

      @Override
      public void onError(Throwable throwable) {
        latch.countDown();
      }

      @Override
      public void onComplete() {

      }
    });
    latch.await();
  }
}
