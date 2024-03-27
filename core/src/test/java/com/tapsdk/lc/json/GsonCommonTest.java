package com.tapsdk.lc.json;

import com.tapsdk.lc.LCObject;
import com.tapsdk.lc.gson.GsonObject;
import com.tapsdk.lc.gson.GsonWrapper;
import com.tapsdk.lc.gson.MapDeserializerDoubleAsIntFix;
import com.tapsdk.lc.gson.NumberDeserializerDoubleAsIntFix;
import com.tapsdk.lc.service.AppAccessEndpoint;
import com.tapsdk.lc.sms.LCCaptchaDigest;
import com.tapsdk.lc.sms.LCCaptchaValidateResult;
import com.tapsdk.lc.upload.FileUploadToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import junit.framework.TestCase;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GsonCommonTest extends TestCase {
  public GsonCommonTest(String name) {
    super(name);
  }

  public void testIllegalData() {
    String content = "{\"_lctype\":103,\"_lcfile\":{\"metaData\":{\"duration\":2.56,\"size\":7244,\"format\":null},\"objId\":\"613707e0f1f73f70cfa19048\",\"url\":\"https:\\/\\/f.letsniyan.com\\/20flALVeg8GiJntS1n1RHYCDT7BptaFt\\/record_1630996438180\"},\"_lctext\":null,\"_lcattrs\":{\"toUserDestroyed\":false,\"fromUserDestroyed\":true}}";
    Gson gson = new GsonBuilder().serializeNulls()
            .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE)
            .registerTypeAdapter(new TypeToken<Map<String, Object>>(){}.getType(),  new MapDeserializerDoubleAsIntFix())
            .registerTypeAdapter(Map.class,  new MapDeserializerDoubleAsIntFix())
            .setLenient().create();
    Map<String, Object> object = gson.fromJson(content, Map.class);
    System.out.println(object);
    Object lcType = object.get("_lctype");
    System.out.println(lcType);
  }
  private void parseData(String s) {
    Object parsedObject = null;
    try {
      JSONObject tmpObject = JSON.parseObject(s);
      if (null != tmpObject && tmpObject.containsKey("result")) {
        parsedObject = tmpObject.get("result");
      } else {
        parsedObject = tmpObject;
      }
    } catch (Exception exception) {
      // compatible for existing cache data.
      parsedObject = JSON.parse(s);
    }
    if (parsedObject instanceof Number) {
      parsedObject = NumberDeserializerDoubleAsIntFix.parsePrecisionNumber((Number) parsedObject);
    }
    assertTrue(null != parsedObject);
    System.out.println(parsedObject);
  }
  public void testSinglePrimitives() {
    String s = "432423423485";
    parseData(s);
    s = "String from hello";
    parseData(s);
    s = "{'test': true}";
    parseData(s);
    s = "{'result': true}";
    parseData(s);
    s = "[43243, 433, 2]";
    parseData(s);
  }

  public void testPrimitives() {
    System.out.println(GsonWrapper.parseObject("test", String.class));
    System.out.println(GsonWrapper.parseObject("100", Integer.class));
    System.out.println(GsonWrapper.parseObject("100", Short.class));
    System.out.println(GsonWrapper.parseObject("19.9", Double.class));
    System.out.println(GsonWrapper.parseObject("1993472843", Long.class));
    System.out.println(GsonWrapper.parseObject("false", Boolean.class));
    System.out.println(GsonWrapper.parseObject("true", Boolean.class));
//    System.out.println(GsonWrapper.parseObject('t', Byte.class));
//    System.out.println(GsonWrapper.parseObject('r', Character.class));
    System.out.println(GsonWrapper.parseObject("12987.83245", Float.class));

    String text = "{\"devices\":[\n" +
            "\t{\n" +
            "\t\t\"CURRENT_TEMPERATURE\":\"255.255\",\n" +
            "\t\t\"STAT_MODE\":\n" +
            "\t\t\t{\n" +
            "\t\t\t\"MANUAL_OFF\":true,\n" +
            "\t\t\t\"TIMECLOCK\":true\n" +
            "\t\t\t},\n" +
            "\t\t\"TIMER\":false,\n" +
            "\t\t\"device\":\"Watering System\"\n" +
            "\t}\n" +
            "]}";
    Object json = GsonWrapper.parseObject(text, Map.class);
    System.out.println(json.toString());
  }

  public void testDeserializeNestedJson() {
    String text = "{\"devices\":[\n" +
            "\t{\n" +
            "\t\t\"CURRENT_TEMPERATURE\":\"255.255\",\n" +
            "\t\t\"STAT_MODE\":\n" +
            "\t\t\t{\n" +
            "\t\t\t\"MANUAL_OFF\":true,\n" +
            "\t\t\t\"TIMECLOCK\":true\n" +
            "\t\t\t},\n" +
            "\t\t\"TIMER\":false,\n" +
            "\t\t\"device\":\"Watering System\"\n" +
            "\t}\n" +
            "]}";
    Object json = GsonWrapper.parseObject(text);
    System.out.println(json.toString());
  }

  public void testSerializeHashMap() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("className", "Student");
    map.put("version", 5);
    map.put("@type", LCObject.class.getName());

    Map<String, Object> data = new HashMap<String, Object>();
    data.put("@type", HashMap.class.getName());
    data.put("age", 5);
    data.put("address", "Beijing City");
    map.put("serverData", data);
    System.out.println(GsonWrapper.toJsonString(map));
  }

  public void testJSONObjectSerialize() {
    JSONObject object = JSONObject.Builder.create(null);
    object.put("className", "Student");
    object.put("version", 5);
    String objectString = GsonWrapper.getGsonInstance().toJson(object);
    System.out.println("object jsonString: " + objectString);
    JSONObject other = GsonWrapper.getGsonInstance().fromJson(objectString, JSONObject.class);

    assertEquals(object.getInteger("version"), other.getInteger("version"));
    assertEquals(object.getString("className"), other.getString("className"));
  }

  public void testDoubleAndLong() {
    String draft = "[ {\"id\":4077395,\"field_id\":242566,\"body\":\"\"},\n" +
            "  {\"id\":4077398,\"field_id\":242569,\"body\":[[273019,0],[273020,1],[273021,0]]},\n" +
            "  {\"id\":4077399,\"field_id\":242570,\"body\":[[273022,0],[273023,1],[273024,0]]}\n" +
            "]";
    ArrayList<Map<String, Object>> responses;
    Type ResponseList = new TypeToken<ArrayList<Map<String, Object>>>() {}.getType();
    responses = GsonWrapper.getGsonInstance().fromJson(draft, ResponseList);
    System.out.println(responses.toString());
  }

  public void testObjectDeserialization() {
    String input = "{\"result\":{\"objectId\":\"637233023f103a0cf936caf8\",\"billId\":\"202211142022269827\"," +
            "\"state\":-1,\"size\":5,\"price\":10.6,\"payPrice\":0,\"isPaid\":false," +
            "\"createdAt\":\"2022-11-14T12:22:26.122Z\",\"isCommented\":false,\"purpose\":\"用于小说配音\"," +
            "\"user\":{\"objectId\":\"5e5462617796d9006a09c795\",\"name\":\"方冶（全天接单主页加微）\"," +
            "\"iconUrl\":\"http://file2.i7play.com/j6ptbHjPSAaBurvOYMPRgevtTzbwXYNF/CROP_20221112164622689.jpg\"," +
            "\"id\":2230377,\"wx\":\"ZmZxy-18\",\"qq\":\"\",\"isVip\":true,\"is18\":true},\"isBu\":false}} ";
    Type ResponseMap = new TypeToken<Map<String, Object>>() {}.getType();
    Map<String, Object> result = GsonWrapper.getGsonInstance().fromJson(input, ResponseMap);
    Object data = result.get("result");
    JsonElement jsonElement = GsonWrapper.getGsonInstance().toJsonTree(data);
    System.out.println(jsonElement);
    if (data instanceof Map) {
      Map<String,Object> dataMap = (Map<String,Object>)data;
      Object priceObj = dataMap.get("price");
      System.out.println(priceObj);
    }
    System.out.println(result.toString());
  }

  public void testNumberParser() {
    Number numbers[] = {3, 4.5, 5.0, -0, 0.0, 0.0002, -0.0002, -5, -6.5};
    for (Number num: numbers) {
      System.out.println("original: " + num + ", floor: " + Math.floor(num.doubleValue()) + ", ceil: " + Math.ceil(num.doubleValue())
              + ", parsed: " + NumberDeserializerDoubleAsIntFix.parsePrecisionNumber(num));
    }
  }

  public void testAppAccessEndpoint() {
    String text = "{\"ttl\":3600,\"stats_server\":\"https://stats_server\", \"push_server\": \"https://push_server\"," +
            " \"rtm_router_server\": \"https://rtm_router_server\", \"api_server\": \"https://api_server\"," +
            " \"engine_server\": \"https://engine_server\"}";
    AppAccessEndpoint endpoint = JSON.parseObject(text, AppAccessEndpoint.class);
    System.out.println(JSON.toJSONString(endpoint));
    assertTrue(endpoint.getTtl() == 3600);
    assertTrue(endpoint.getApiServer().endsWith("api_server"));
    assertTrue(endpoint.getEngineServer().endsWith("engine_server"));
    assertTrue(endpoint.getPushServer().endsWith("push_server"));
    assertTrue(endpoint.getRtmRouterServer().endsWith("rtm_router_server"));
    assertTrue(endpoint.getStatsServer().endsWith("stats_server"));
  }

  public void testFileUploadToken() {
    String text = "{}";
    FileUploadToken token = JSON.parseObject(text, FileUploadToken.class);
    System.out.println(JSON.toJSONString(token));
    assertTrue(null != token);

    text = "{\"bucket\":\"value_bucket\",\"objectId\":\"value_objectId\",\"upload_url\":\"value_upload_url\",\"provider\":\"value_provider\",\"token\":\"value_token\",\"url\":\"value_url\",\"key\":\"value_key\"}";
    token = JSON.parseObject(text, FileUploadToken.class);
    System.out.println(JSON.toJSONString(token));
    assertTrue(null != token);
    assertTrue(token.getToken().endsWith("token"));
    assertTrue(token.getKey().endsWith("key"));
    assertTrue(token.getObjectId().endsWith("objectId"));
    assertTrue(token.getBucket().endsWith("bucket"));
    assertTrue(token.getUploadUrl().endsWith("upload_url"));
    assertTrue(token.getUrl().endsWith("url"));
    assertTrue(token.getProvider().endsWith("provider"));
  }

  public void testAVCaptchaDigest() {
    String text = "{\"captcha_token\":\"fhaeifhepfewifh\", \"captcha_url\": \"https://captcha_url\"}";
    LCCaptchaDigest digest = JSON.parseObject(text, LCCaptchaDigest.class);
    System.out.println(JSON.toJSONString(digest));
    assertTrue(digest.getCaptchaUrl().startsWith("https"));
  }

  public void testFileUploadTokenWithMetaData() {
    String text = "{\"bucket\":\"gzx1fl2o\",\"createdAt\":\"2023-05-30T08:31:18.398Z\",\"key\":\"gamesaves/sJxbh9G4nFoGuwwoY8gpyd6dj9vbeAwM/logo.png\",\"metaData\":{\"_checksum\":\"ce80d83f138fd1c805f59d0b0dbb6405\",\"_name\":\"logo.png\",\"prefix\":\"gamesaves\",\"size\":38651},\"mime_type\":\"image/png\",\"name\":\"logo.png\",\"objectId\":\"6475b4568bcc5a2a1dc23aab\",\"provider\":\"qiniu\",\"token\":\"bOJAZVDET_Z11xes0ufp39ao_Tie7mrGqecKRkUf:FrRYGS9OZbrylDbN7kXEPo8s_ts=:eyJzY29wZSI6Imd6eDFmbDJvOmdhbWVzYXZlcy9zSnhiaDlHNG5Gb0d1d3dvWThncHlkNmRqOXZiZUF3TS9sb2dvLnBuZyIsImRlYWRsaW5lIjoxNjg1NDM5MDc4LCJpbnNlcnRPbmx5IjoxfQ==\",\"upload_url\":\"https://upload.qiniup.com\",\"url\":\"https://gzx1fl2o.tds1.tapfiles.cn/gamesaves/sJxbh9G4nFoGuwwoY8gpyd6dj9vbeAwM/logo.png\"}";
    FileUploadToken fileUploadToken = JSON.parseObject(text, FileUploadToken.class);
    assertTrue(null != fileUploadToken);
    System.out.println(fileUploadToken.toString());
    System.out.println(JSON.toJSONString(fileUploadToken));
  }

  public void testCaptchaValidateResult() {
    String text = "{\"validate_token\":\"value_bucket\"}";
    LCCaptchaValidateResult result = JSON.parseObject(text, LCCaptchaValidateResult.class);
    System.out.println(JSON.toJSONString(result));
    assertTrue(result.getValidateToken().equals("value_bucket"));
  }
}
