package  com.tapsdk.lc.im;

import  com.tapsdk.lc.LCException;
import  com.tapsdk.lc.LCLogger;
import  com.tapsdk.lc.LCQuery;
import  com.tapsdk.lc.Configure;
import  com.tapsdk.lc.callback.LCCallback;
import  com.tapsdk.lc.core.LeanCloud;
import  com.tapsdk.lc.im.v2.LCIMClient;
import  com.tapsdk.lc.im.v2.LCIMConversation;
import  com.tapsdk.lc.im.v2.LCIMConversationsQuery;
import  com.tapsdk.lc.im.v2.LCIMException;
import  com.tapsdk.lc.im.v2.callback.LCIMClientCallback;
import  com.tapsdk.lc.im.v2.callback.LCIMConversationQueryCallback;
import  com.tapsdk.lc.session.LCConnectionManager;
import junit.framework.TestCase;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LCIMClientSmokeTest extends TestCase {
  private CountDownLatch countDownLatch = null;
  private boolean opersationSucceed = false;

  public LCIMClientSmokeTest(String name) {
    super(name);
    Configure.initialize();
    LeanCloud.setLogLevel(LCLogger.Level.DEBUG);
  }

  @Override
  protected void setUp() throws Exception {
    this.countDownLatch = new CountDownLatch(1);
    LCConnectionManager manager = LCConnectionManager.getInstance();
    final CountDownLatch tmpLatch = new CountDownLatch(1);
    manager.startConnection(new LCCallback() {
      @Override
      protected void internalDone0(Object o, LCException LCException) {
        tmpLatch.countDown();
      }
    });
    tmpLatch.await();
    opersationSucceed = false;
  }

  @Override
  protected void tearDown() throws Exception {
    this.countDownLatch = null;
  }

  public void testSimpleWorkflow() throws Exception {
    String targetClient = "" + System.currentTimeMillis();
    final LCIMClient client = LCIMClient.getInstance(targetClient);
    client.open(new LCIMClientCallback() {
      @Override
      public void done(LCIMClient tmp, LCIMException e) {
        if (null != e) {
          System.out.println("failed open client.");
          e.printStackTrace();
          countDownLatch.countDown();
        } else {
          System.out.println("succeed open client.");
          LCIMConversationsQuery query = client.getConversationsQuery();
          query.setQueryPolicy(LCQuery.CachePolicy.NETWORK_ONLY);
          query.whereEqualTo("tr", true).addAscendingOrder("createdAt")
                  .findInBackground(new LCIMConversationQueryCallback() {
                    @Override
                    public void done(List<LCIMConversation> conversations, LCIMException ex) {
                      if (null != ex) {
                        System.out.println("failed to query convs");
                        ex.printStackTrace();
                        countDownLatch.countDown();
                      } else {
                        System.out.println("succeed to query convs. results=" + conversations.toString());
                        client.close(new LCIMClientCallback() {
                          @Override
                          public void done(LCIMClient client, LCIMException e) {
                            opersationSucceed = (null == e);
                            countDownLatch.countDown();
                          }
                        });
                      }
                    }
                  });
        }
      }
    });
    countDownLatch.await();
    assertTrue(opersationSucceed);
  }
}
