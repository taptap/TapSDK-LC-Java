package com.tapsdk.lc;

import com.tapsdk.lc.annotation.LCClassName;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LCObjectSubClassTest extends TestCase {
  private CountDownLatch latch = null;
  private boolean testSucceed = false;

  @LCClassName("Student")
  static class Student extends LCObject {
    public String getContent() {
      return getString("content");
    }
    public void setContent(String value) {
      put("content", value);
    }
  }

  public LCObjectSubClassTest(String testName) {
    super(testName);
    LCObject.registerSubclass(Student.class);
    Configure.initializeRuntime();
  }
  public static Test suite() {
    return new TestSuite(LCObjectSubClassTest.class);
  }

  @Override
  protected void setUp() throws Exception {
    latch = new CountDownLatch(1);
    testSucceed = false;
  }

  public void testSaveObject() throws Exception{
    Student student = new Student();
    student.saveInBackground().subscribe(new Observer<LCObject>() {
      public void onSubscribe(Disposable disposable) {

      }

      public void onNext(LCObject LCObject) {
        System.out.println(LCObject.toString());
        testSucceed = true;
        latch.countDown();

      }

      public void onError(Throwable throwable) {
        latch.countDown();
      }

      public void onComplete() {

      }
    });
    latch.await();
    assertTrue(testSucceed);
  }

  public void testRefreshObject() throws Exception {
    Student student = new Student();
    student.setObjectId("5a8e7d00128fe10037d2cf58");
    student.refreshInBackground().subscribe(new Observer<LCObject>() {
      public void onSubscribe(Disposable disposable) {

      }

      public void onNext(LCObject LCObject) {
        System.out.println(LCObject.toString());
        testSucceed = true;
        latch.countDown();
      }

      public void onError(Throwable throwable) {
        latch.countDown();
      }

      public void onComplete() {

      }
    });
    latch.await();
    assertTrue(testSucceed);
  }

  public void testQuery() throws Exception {
    LCQuery<Student> query = LCQuery.getQuery(Student.class);
    query.whereGreaterThan("age", 18);
    query.whereDoesNotExist("name");
    query.findInBackground().subscribe(new Observer<List<Student>>() {
      public void onSubscribe(Disposable disposable) {

      }

      public void onNext(List<Student> students) {
        for (Student s: students) {
          System.out.println(s.toString());
        }
        testSucceed = true;
        latch.countDown();
      }

      public void onError(Throwable throwable) {
        latch.countDown();
      }

      public void onComplete() {

      }
    });
    latch.await();
    assertTrue(testSucceed);
  }
}
