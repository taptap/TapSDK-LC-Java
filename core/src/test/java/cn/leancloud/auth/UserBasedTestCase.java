package cn.leancloud.auth;

import cn.leancloud.Configure;
import cn.leancloud.LCUser;
import cn.leancloud.utils.StringUtil;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;

public class UserBasedTestCase extends TestCase {
  protected String username = null;
  protected String passwd = null;
  protected Exception runningException = null;

  public UserBasedTestCase(String name) {
    super(name);
    Configure.initializeRuntime();
  }

  protected void setAuthUser(String username, String passwd) {
    this.username = username;
    this.passwd = passwd;
  }

  protected void clearCurrentAuthenticatedUser() {
    LCUser.changeCurrentUser(null, true);
  }

  @Override
  protected void setUp() throws Exception {
    runningException = null;
    if (StringUtil.isEmpty(this.username) || StringUtil.isEmpty(this.passwd)) {
      return;
    }
    final CountDownLatch latch = new CountDownLatch(1);
    LCUser.logIn(this.username, this.passwd).subscribe(new Observer<LCUser>() {
      @Override
      public void onSubscribe(@NotNull Disposable disposable) {

      }

      @Override
      public void onNext(@NotNull LCUser lcUser) {
        latch.countDown();
      }

      @Override
      public void onError(@NotNull Throwable throwable) {
        runningException = new Exception(throwable);
        latch.countDown();
      }

      @Override
      public void onComplete() {

      }
    });
    latch.await();
    if (null != this.runningException) {
      throw this.runningException;
    }
  }

  @Override
  protected void tearDown() throws Exception {
    clearCurrentAuthenticatedUser();
  }
}
