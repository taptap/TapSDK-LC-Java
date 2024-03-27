package com.tapsdk.lc.network;

import com.tapsdk.lc.LCException;
import com.tapsdk.lc.Configure;
import com.tapsdk.lc.callback.GetCallback;
import com.tapsdk.lc.convertor.ObserverBuilder;
import com.tapsdk.lc.core.PaasClient;
import com.tapsdk.lc.core.StorageClient;
import com.tapsdk.lc.types.LCDate;
import com.tapsdk.lc.LCObject;
import com.tapsdk.lc.upload.FileUploadToken;
import com.tapsdk.lc.json.JSONObject;
import io.reactivex.disposables.Disposable;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import io.reactivex.Observer;

import java.util.concurrent.CountDownLatch;

public class PaasClientTest extends TestCase {
  private CountDownLatch latch = null;
  private boolean testSucceed = false;
  public PaasClientTest(String testName) {
    super(testName);
  }
  public static Test suite()
  {
    return new TestSuite( PaasClientTest.class );
  }
  @Override
  protected void setUp() throws Exception {
    Configure.initializeRuntime();
    latch = new CountDownLatch(1);
    testSucceed = false;
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void testCurrentTimeStamp() throws Exception{

    StorageClient storageClient = PaasClient.getStorageClient();
    storageClient.getServerTime().subscribe(new Observer<LCDate>() {
      public void onSubscribe(Disposable disposable) {
      }

      public void onNext(LCDate LCDate) {
        System.out.println(LCDate);
        testSucceed = true;
        latch.countDown();
      }

      public void onError(Throwable throwable) {
        System.out.println("failed! cause: " + throwable);
        latch.countDown();
      }

      public void onComplete() {

      }
    });

    latch.await();
    assertTrue(testSucceed);
  }

  public void testFetchOneObject() throws Exception{

      StorageClient storageClient = PaasClient.getStorageClient();
      GetCallback callback = new GetCallback() {
        @Override
        public void done(LCObject object, LCException e) {
          if (null != e) {

          } else {
            testSucceed = (null != object);
            System.out.println("response is:" + object.toString());
          }
          latch.countDown();
        }
      };
      storageClient.fetchObject(null, "Student", "5a7a4ac8128fe1003768d2b1", null)
              .subscribe(ObserverBuilder.buildSingleObserver(callback));
      latch.await();
      assertTrue(testSucceed);
  }

  public void testCreateUploadToken() throws Exception {

      JSONObject fileObject = JSONObject.Builder.create(null);
      StorageClient storageClient = PaasClient.getStorageClient();
      storageClient.newUploadToken(null, fileObject).subscribe(new Observer<FileUploadToken>() {
        public void onComplete() {

        }

        public void onError(Throwable throwable) {
          System.out.println("failed! cause: " + throwable);
          testSucceed = true;
          latch.countDown();
        }

        public void onNext(FileUploadToken fileUploadToken) {
          System.out.println(fileUploadToken);

          latch.countDown();
        }
        public void onSubscribe(Disposable disposable) {
          ;
        }
      });
      latch.await();
      assertTrue(testSucceed);
  }
}
