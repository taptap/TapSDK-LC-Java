package  com.tapsdk.lc.websocket;

import  com.tapsdk.lc.LCLogger;
import  com.tapsdk.lc.Configure;
import  com.tapsdk.lc.core.LeanCloud;
import  com.tapsdk.lc.command.LoginPacket;
import  com.tapsdk.lc.command.SessionControlPacket;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import okhttp3.Response;
import okio.ByteString;

public class OKWebSocketClientTest extends TestCase {
  private OKWebSocketClient client = null;
  public OKWebSocketClientTest(String testname) {
    super(testname);
    LeanCloud.setRegion(LeanCloud.REGION.NorthChina);
    LeanCloud.setLogLevel(LCLogger.Level.VERBOSE);
    LeanCloud.initialize(Configure.TEST_APP_ID, Configure.TEST_APP_KEY);
  }

  public static Test suite()
  {
    return new TestSuite( OKWebSocketClientTest.class );
  }

  public void testConnect() throws Exception {
    String wsUrl = "wss://cn-n1-cell5.leancloud.cn";
    client = new OKWebSocketClient(new WsStatusListener(){
      public void onOpen(Response response) {
        try {
          System.out.println("websockdet opened! responseCode=" + response.code() + ", body=" + response.message());
        } catch (Exception ex) {
          ex.printStackTrace();
        }
        final int requestId = 100;
        final String installation = "d45304813cf37c6c1a2177f84aee0bb8";

        LoginPacket lp = new LoginPacket();
        lp.setAppId(Configure.TEST_APP_ID);
        lp.setInstallationId(installation);
        lp.setRequestId(requestId - 1);
        System.out.println("uplink: " + lp.getGenericCommand().toString());
        client.sendMessage(ByteString.of(lp.getGenericCommand().toByteString().asReadOnlyByteBuffer()));
      }
      public void onMessage(String text) {

      }
      public void onMessage(ByteString bytes) {

      }
      public void onReconnect() {

      }
      public void onClosing(int code, String reason) {

      }
      public void onClosed(int code, String reason) {

      }
      public void onFailure(Throwable t, Response response) {

      }
    }, true);
    client.connect(wsUrl);
    Thread.sleep(10000);
    client.close();
    Thread.sleep(3000);
  }


  public void testSessionOpen() throws Exception {
    String wsUrl = "wss://cn-n1-cell5.leancloud.cn";
    client = new OKWebSocketClient(new WsStatusListener(){
      public void onOpen(Response response) {
        try {
          System.out.println("websockdet opened! responseCode=" + response.code() + ", body=" + response.message());
        } catch (Exception ex) {
          ex.printStackTrace();
        }
        final int requestId = 100;
        final String installation = "d45304813cf37c6c1a2177f84aee0bb8";

        SessionControlPacket scp = SessionControlPacket.genSessionCommand(
                installation, "fengjunwen", null,
                SessionControlPacket.SessionControlOp.OPEN, null,
                0, 0, requestId);
        scp.setTag("mobile");
        scp.setAppId(Configure.TEST_APP_ID);
        scp.setInstallationId(installation);
        scp.setReconnectionRequest(false);
        System.out.println("uplink: " + scp.getGenericCommand().toString());
        client.sendMessage(ByteString.of(scp.getGenericCommand().toByteString().asReadOnlyByteBuffer()));
        System.out.println("send open command.");

      }
      public void onMessage(String text) {

      }
      public void onMessage(ByteString bytes) {

      }
      public void onReconnect() {

      }
      public void onClosing(int code, String reason) {

      }
      public void onClosed(int code, String reason) {

      }
      public void onFailure(Throwable t, Response response) {

      }
    }, true);
    client.connect(wsUrl);
    Thread.sleep(12000);
    client.close();
    Thread.sleep(3000);
  }
}
