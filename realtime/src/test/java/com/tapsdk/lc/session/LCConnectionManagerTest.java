package  com.tapsdk.lc.session;

import  com.tapsdk.lc.Configure;
import  com.tapsdk.lc.Messages;
import  com.tapsdk.lc.command.LoginPacket;
import  com.tapsdk.lc.command.SessionControlPacket;
import junit.framework.TestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LCConnectionManagerTest extends TestCase {
  private CountDownLatch countDownLatch = null;
  private LCConnectionListener listener = new LCConnectionListener() {
    @Override
    public void onWebSocketOpen() {
      System.out.println("ConnectionListener - WebSocket opened.");
      countDownLatch.countDown();
    }

    @Override
    public void onWebSocketClose() {
      System.out.println("ConnectionListener - WebSocket closed.");
    }

    public void onMessageArriving(String peerId, Integer requestKey, Messages.GenericCommand genericCommand) {
      System.out.println("received new message! requestKey=" + requestKey);
    }

    @Override
    public void onError(Integer requestKey, Messages.ErrorCommand errorCommand) {

    }
  };

  public LCConnectionManagerTest(String testName) {
    super(testName);
    Configure.initialize();
  }

  @Override
  protected void setUp() throws Exception {
    this.countDownLatch = new CountDownLatch(1);
  }

  @Override
  protected void tearDown() throws Exception {
    this.countDownLatch = null;
  }

  public void testInitConnection() throws Exception {
    LCConnectionManager manager = LCConnectionManager.getInstance();
    manager.autoConnection();
    manager.subscribeConnectionListener("", this.listener);
    if (!manager.isConnectionEstablished()) {
      this.countDownLatch.await(12, TimeUnit.SECONDS);
    }
    assertTrue(manager.isConnectionEstablished());
  }

  public void testSwitchConnection() throws Exception {
    ;
  }

  public void testAutoReconnection() throws Exception {
    ;
  }

  public void testLogin() throws Exception {
    LCConnectionManager manager = LCConnectionManager.getInstance();
    manager.autoConnection();
    manager.subscribeConnectionListener("", this.listener);
    if (!manager.isConnectionEstablished()) {
      this.countDownLatch.await(12, TimeUnit.SECONDS);
    }
    assertTrue(manager.isConnectionEstablished());

    final int requestId = 100;
    final String installation = "d45304813cf37c6c1a2177f84aee0bb8";

    LoginPacket lp = new LoginPacket();
    lp.setAppId(Configure.TEST_APP_ID);
    lp.setInstallationId(installation);
    lp.setRequestId(requestId - 1);
    manager.sendPacket(lp);
    Thread.sleep(3000);

    SessionControlPacket scp = SessionControlPacket.genSessionCommand(
            installation, "fengjunwen", null,
            SessionControlPacket.SessionControlOp.OPEN, null,
            0, 0, requestId);
    scp.setTag("mobile");
    scp.setAppId(Configure.TEST_APP_ID);
    scp.setInstallationId(installation);
    scp.setReconnectionRequest(false);
    manager.sendPacket(scp);

    Thread.sleep(3000);

  }
}
