package  com.tapsdk.lc.session;

import  com.tapsdk.lc.LCException;
import  com.tapsdk.lc.LCLogger;
import  com.tapsdk.lc.callback.LCCallback;
import  com.tapsdk.lc.core.LeanCloud;
import  com.tapsdk.lc.im.LCIMOptions;
import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;

public class LCConnectionManagerExTest extends TestCase {
  private CountDownLatch countDownLatch = null;
  private boolean operationSucceed = false;
  public LCConnectionManagerExTest(String name) {
    super(name);
    LeanCloud.setLogLevel(LCLogger.Level.VERBOSE);
  }

  @Override
  protected void setUp() throws Exception {
    this.countDownLatch = new CountDownLatch(1);
    operationSucceed = false;
  }

  @Override
  protected void tearDown() throws Exception {
    this.countDownLatch = null;
    LCIMOptions.getGlobalOptions().setRtmServer("");
  }

  public void testConnectionRetry() throws Exception {
    LCIMOptions.getGlobalOptions().setRtmServer("wss://cn-n1-cell987.leancloud.cn");
    final LCConnectionManager manager = LCConnectionManager.getInstance();
    new Thread(new Runnable() {
      @Override
      public void run() {
        manager.startConnection(new LCCallback() {
          @Override
          protected void internalDone0(Object o, LCException LCException) {

          }
        });
      }
    }).start();
    try {
      Thread.sleep(1000);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    manager.startConnection(new LCCallback() {
      @Override
      protected void internalDone0(Object o, LCException LCException) {
        if (null != LCException) {
          LCException.printStackTrace();
        }
        operationSucceed = true;
        countDownLatch.countDown();
      }
    });
    countDownLatch.await();
    assertTrue(operationSucceed);
  }

  public void testMultipleConnection() throws Exception {
    LCIMOptions.getGlobalOptions().setRtmServer("wss://cn-n1-cell987.leancloud.cn");
    final LCConnectionManager manager = LCConnectionManager.getInstance();
    new Thread(new Runnable() {
      @Override
      public void run() {
        manager.startConnection(new LCCallback() {
          @Override
          protected void internalDone0(Object o, LCException LCException) {

          }
        });
      }
    }).start();
    try {
      Thread.sleep(1000);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    manager.startConnection(new LCCallback() {
      @Override
      protected void internalDone0(Object o, LCException LCException) {

      }
    });
    manager.startConnection(new LCCallback() {
      @Override
      protected void internalDone0(Object o, LCException LCException) {

      }
    });
    manager.startConnection(new LCCallback() {
      @Override
      protected void internalDone0(Object o, LCException LCException) {
        if (null != LCException) {
          LCException.printStackTrace();
        }
        operationSucceed = true;
        countDownLatch.countDown();
      }
    });
    countDownLatch.await();
    assertTrue(operationSucceed);
  }

}
