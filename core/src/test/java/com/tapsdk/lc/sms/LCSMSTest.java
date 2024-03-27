package com.tapsdk.lc.sms;

import com.tapsdk.lc.Configure;
import com.tapsdk.lc.LCLogger;
import com.tapsdk.lc.core.LeanCloud;
import com.tapsdk.lc.types.LCNull;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.concurrent.CountDownLatch;

public class LCSMSTest extends TestCase {
  private boolean testSuccess;
  public LCSMSTest(String name) {
    super(name);
    Configure.initializeRuntime();
  }

  public static Test suite() {
    return new TestSuite(LCSMSTest.class);
  }

  public void testNormalMobilePhone() throws Exception {
    LCSMSOption option = new LCSMSOption();
    option.setApplicationName("LeanCloud");
    option.setOperation("Register");
    option.setTemplateName("my Template");
    option.setSignatureName("LeanCloud");

    final CountDownLatch latch = new CountDownLatch(1);
    testSuccess = false;
    LCSMS.requestSMSCodeInBackground("18600345198", option).subscribe(new Observer<LCNull>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(LCNull LCNull) {
        testSuccess = true;
        latch.countDown();
      }

      @Override
      public void onError(Throwable throwable) {
        testSuccess = true;
        latch.countDown();
      }

      @Override
      public void onComplete() {
      }
    });
    latch.await();
    assertTrue(testSuccess);
  }

  public void testTooLongMobilePhone() {
    LCSMS.requestSMSCodeInBackground("218600345198", null).subscribe(new Observer<LCNull>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(LCNull LCNull) {
        fail();
      }

      @Override
      public void onError(Throwable throwable) {
        assertTrue(throwable.getMessage().equalsIgnoreCase("mobile phone number is empty or invalid"));
      }

      @Override
      public void onComplete() {

      }
    });

    LCSMS.requestSMSCodeInBackground("1+8600345198", null).subscribe(new Observer<LCNull>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(LCNull LCNull) {
        fail();
      }

      @Override
      public void onError(Throwable throwable) {
        assertTrue(throwable.getMessage().equalsIgnoreCase("mobile phone number is empty or invalid"));
      }

      @Override
      public void onComplete() {

      }
    });

    LCSMS.requestSMSCodeInBackground("+18600345198", null).subscribe(new Observer<LCNull>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(LCNull LCNull) {
        fail();
      }

      @Override
      public void onError(Throwable throwable) {
        assertTrue(throwable.getMessage().equalsIgnoreCase("smsOption is null"));
      }

      @Override
      public void onComplete() {

      }
    });

    LCSMS.requestSMSCodeInBackground("28600345198", null).subscribe(new Observer<LCNull>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(LCNull LCNull) {
        fail();
      }

      @Override
      public void onError(Throwable throwable) {
        assertTrue(throwable.getMessage().equalsIgnoreCase("mobile phone number is empty or invalid"));
      }

      @Override
      public void onComplete() {

      }
    });
  }

  public void testInternationalMobile() {

    LCSMS.requestSMSCodeInBackground("+85290337941", null).subscribe(new Observer<LCNull>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(LCNull LCNull) {
        fail();
      }

      @Override
      public void onError(Throwable throwable) {
        assertTrue(throwable.getMessage().equalsIgnoreCase("smsOption is null"));

      }

      @Override
      public void onComplete() {

      }
    });

    LCSMS.requestSMSCodeInBackground("+8619334290337941", null).subscribe(new Observer<LCNull>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(LCNull LCNull) {
        fail();
      }

      @Override
      public void onError(Throwable throwable) {
        assertTrue(throwable.getMessage().equalsIgnoreCase("smsOption is null"));

      }

      @Override
      public void onComplete() {

      }
    });
  }

  public void testCaptcha() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    testSuccess = false;
    LCCaptchaOption option = new LCCaptchaOption();
    option.setWidth(85);
    option.setHeight(40);
    LCCaptcha.requestCaptchaInBackground(option).subscribe(new Observer<LCCaptchaDigest>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(LCCaptchaDigest lcCaptchaDigest) {
        System.out.println("Succeed to got digest: " + lcCaptchaDigest.getCaptchaUrl());
        LCCaptcha.verifyCaptchaCodeInBackground("znca", lcCaptchaDigest).subscribe(new Observer<LCCaptchaValidateResult>() {
          @Override
          public void onSubscribe(Disposable disposable) {

          }

          @Override
          public void onNext(LCCaptchaValidateResult lcCaptchaValidateResult) {
            System.out.println("Succeed to got validateResult: " + lcCaptchaValidateResult);
            testSuccess = true;
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
    assertTrue(testSuccess);
  }
}
