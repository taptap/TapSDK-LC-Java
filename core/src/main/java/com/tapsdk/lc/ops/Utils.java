package com.tapsdk.lc.ops;

import java.util.*;

import com.tapsdk.lc.*;
import com.tapsdk.lc.gson.ObjectDeserializer;
import com.tapsdk.lc.types.LCGeoPoint;
import com.tapsdk.lc.utils.StringUtil;

import com.tapsdk.lc.codec.Base64;
import com.tapsdk.lc.json.JSONObject;

public class Utils {
  private static final String typeTag = "__type";

  public static Map<String, Object> createPointerArrayOpMap(String key, String op,
                                                            Collection<LCObject> objects) {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("__op", op);
    List<Map<String, ?>> list = new ArrayList<Map<String, ?>>();
    for (LCObject obj : objects) {
      list.add(mapFromPointerObject(obj));
    }
    map.put("objects", list);
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(key, map);
    return result;
  }

  public static Map<String, Object> mapFromPointerObject(LCObject object) {
    return mapFromAVObject(object, false);
  }

  public static Map<String, Object> mapFromGeoPoint(LCGeoPoint point) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(typeTag, "GeoPoint");
    result.put("latitude", point.getLatitude());
    result.put("longitude", point.getLongitude());
    return result;
  }

  public static LCGeoPoint geoPointFromMap(Map<String, Object> map) {
    double la = ((Number) map.get("latitude")).doubleValue();
    double lo = ((Number) map.get("longitude")).doubleValue();
    LCGeoPoint point = new LCGeoPoint(la, lo);
    return point;
  }

  public static LCObject parseObjectFromMap(Map<String, Object> map) {
    LCObject LCObject = Transformer.objectFromClassName((String) map.get("className"));
    map.remove("__type");
    LCObject.resetServerData(map);
    return LCObject;
  }

  public static byte[] dataFromMap(Map<String, Object> map) {
    String value = (String) map.get("base64");
    return Base64.decode(value, Base64.NO_WRAP);
  }
  public static Date dateFromMap(Map<String, Object> map) {
    String value = (String) map.get("iso");
    return StringUtil.dateFromString(value);
  }


  public static Map<String, Object> mapFromDate(Date date) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(typeTag, "Date");
    result.put("iso", StringUtil.stringFromDate(date));
    return result;
  }

  public static Map<String, Object> mapFromByteArray(byte[] data) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(typeTag, "Bytes");
    result.put("base64", Base64.encodeToString(data, Base64.NO_WRAP));
    return result;
  }

  public static Map<String, Object> mapFromFile(LCFile file) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("__type", "_File");
    result.put("metaData", file.getMetaData());
    result.put("id", file.getName());
    return result;
  }

  public static Map<String, Object> mapFromAVObject(LCObject object, boolean topObject) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("className", object.internalClassName());

    if (!StringUtil.isEmpty(object.getObjectId())) {
      result.put("objectId", object.getObjectId());
    }
    if (!topObject) {
      result.put("__type", "Pointer");
    } else {
      result.put("__type", "Object");

      Map<String, Object> serverData = getParsedMap(object.getServerData(), false);
      if (serverData != null && !serverData.isEmpty()) {
        result.putAll(serverData);
      }
    }
    return result;
  }

  public static Map<String, Object> getParsedMap(Map<String, Object> map) {
    return getParsedMap(map, false);
  }

  public static Map<String, Object> getParsedMap(Map<String, Object> object, boolean topObject) {
    Map newMap = new HashMap<String, Object>(object.size());

    for (Map.Entry<String, Object> entry : object.entrySet()) {
      final String key = entry.getKey();
      Object o = entry.getValue();
      newMap.put(key, getParsedObject(o, topObject));
    }

    return newMap;
  }

  public static List getParsedList(Collection list) {
    List newList = new ArrayList(list.size());

    for (Object o : list) {
      newList.add(getParsedObject(o));
    }

    return newList;
  }

  public static List getParsedList(Collection object, boolean topObject) {
    if (!topObject) {
      return getParsedList(object);
    } else {
      List newList = new ArrayList(object.size());

      for (Object o : object) {
        newList.add(getParsedObject(o, true));
      }

      return newList;
    }
  }

  public static Object getParsedObject(Object object) {
    return getParsedObject(object, false);
  }

  public static Object getParsedObject(Object object, boolean topObject) {
    if (object == null) {
      return null;
    } else if (object instanceof Map) {
      return getParsedMap((Map<String, Object>) object, topObject);
    } else if (object instanceof Collection) {
      return getParsedList((Collection) object, topObject);
    } else if (object instanceof LCObject) {
      if (!topObject) {
        return mapFromPointerObject((LCObject) object);
      } else {
        return mapFromAVObject((LCObject) object, true);
      }
    } else if (object instanceof LCGeoPoint) {
      return mapFromGeoPoint((LCGeoPoint) object);
    } else if (object instanceof Date) {
      return mapFromDate((Date) object);
    } else if (object instanceof byte[]) {
      return mapFromByteArray((byte[]) object);
    } else if (object instanceof LCFile) {
      return mapFromFile((LCFile) object);
    } else {
      return object;
    }
  }

  public static Map<String, Object> createArrayOpMap(String key, String op, Collection<?> objects) {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("__op", op);
    List<Object> array = new ArrayList<Object>();
    for (Object obj : objects) {
      array.add(getParsedObject(obj));
    }
    map.put("objects", array);
    Map<String, Object> ops = new HashMap<String, Object>();
    ops.put(key, map);
    return ops;
  }

  public static LCRelation objectFromRelationMap(Map<String, Object> map) {
    String className = (String) map.get("className");
    return new LCRelation(className);
  }

  public static LCFile fileFromMap(Map<String, Object> map) {
    LCFile file = new LCFile("", "");
    file.resetServerData(map);
    Object metadata = map.get("metaData");
    if (metadata != null && metadata instanceof Map) {
      file.getMetaData().putAll((Map) metadata);
    }
    return file;
  }

  public static List getObjectFrom(Collection list) {
    List newList = new ArrayList();

    for (Object obj : list) {
      newList.add(getObjectFrom(obj));
    }

    return newList;
  }

  public static Object getObjectFrom(Map<String, Object> map) {
    Object type = map.get("__type");
    if (null == type || !(type instanceof String)) {
      if(map.containsKey(LCObject.KEY_CLASSNAME) && map.containsKey(ObjectDeserializer.KEY_SERVERDATA)) {
        // support new version jsonString of AVObject.
        String className = map.containsKey(LCObject.KEY_CLASSNAME)?
                (String) map.get(LCObject.KEY_CLASSNAME) : (String) map.get("@type");
        LCObject LCObject = Transformer.objectFromClassName(className);
        Map<String, Object> serverData = (Map) map.get(ObjectDeserializer.KEY_SERVERDATA);
        if (serverData.containsKey(LCObject.KEY_CLASSNAME)) {
          serverData.remove(LCObject.KEY_CLASSNAME);
        }
        Map<String, Object> decodedValues = new HashMap<>();
        for(Map.Entry<String, Object> entry: serverData.entrySet()) {
          String k = entry.getKey();
          Object v = entry.getValue();
          if (v instanceof String || v instanceof Number || v instanceof Boolean || v instanceof Byte || v instanceof Character) {
            // primitive type
            decodedValues.put(k, v);
          } else if (v instanceof Map || v instanceof JSONObject) {
            decodedValues.put(k, Utils.getObjectFrom(v));
          } else if (v instanceof Collection) {
            decodedValues.put(k, Utils.getObjectFrom(v));
          } else if (null != v) {
            decodedValues.put(k, v);
          }
        }
        LCObject.resetServerData(decodedValues);
        return LCObject;
      } else if (map.containsKey("@type") && map.get("@type") instanceof String) {
        String classType = (String) map.get("@type");
        if (null != classType && classType.startsWith("cn.leancloud.")) {
          try {
            LCObject LCObject = (LCObject) Class.forName(classType).newInstance();
            map.remove("@type");
            Map<String, Object> decodedValues = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
              String k = entry.getKey();
              Object v = entry.getValue();
              if (v instanceof String || v instanceof Number || v instanceof Boolean || v instanceof Byte || v instanceof Character) {
                // primitive type
                decodedValues.put(k, v);
              } else if (v instanceof Map || v instanceof JSONObject) {
                decodedValues.put(k, Utils.getObjectFrom(v));
              } else if (v instanceof Collection) {
                decodedValues.put(k, Utils.getObjectFrom(v));
              } else if (null != v) {
                decodedValues.put(k, v);
              }
            }
            LCObject.resetServerData(decodedValues);
            return LCObject;
          } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          } catch (InstantiationException e) {
            e.printStackTrace();
          }
        }
      }
      Map<String, Object> newMap = new HashMap<String, Object>(map.size());

      for (Map.Entry<String, Object> entry : map.entrySet()) {
        final String key = entry.getKey();
        Object o = entry.getValue();
        newMap.put(key, getObjectFrom(o));
      }
      return newMap;
    }
    map.remove("__type");
    if (type.equals("Pointer") || type.equals("Object")) {
      LCObject LCObject = Transformer.objectFromClassName((String) map.get("className"));
//      avObject.resetServerData(map);
      map.remove("className");
      for (Map.Entry<String, Object> entry: map.entrySet()) {
        String k = entry.getKey();
        Object v = entry.getValue();
        if (v instanceof String || v instanceof Number || v instanceof Boolean || v instanceof Byte || v instanceof Character) {
          // primitive type
          LCObject.getServerData().put(k, v);
        } else if (v instanceof Map || v instanceof JSONObject) {
          LCObject.getServerData().put(k, Utils.getObjectFrom(v));
        } else if (v instanceof Collection) {
          LCObject.getServerData().put(k, Utils.getObjectFrom(v));
        } else if (null != v) {
          LCObject.getServerData().put(k, v);
        }
      }
      return LCObject;
    } else if (type.equals("GeoPoint")) {
      return geoPointFromMap(map);
    } else if (type.equals("Bytes")) {
      return dataFromMap(map);
    } else if (type.equals("Date")) {
      return dateFromMap(map);
    } else if (type.equals("Relation")) {
      return objectFromRelationMap(map);
    } else if (type.equals("File")) {
      return fileFromMap(map);
    }
    return map;
  }

  public static Object getObjectFrom(Object obj) {
    if (obj instanceof Collection) {
      return getObjectFrom((Collection) obj);
    } else if (obj instanceof Map || obj instanceof JSONObject) {
      return getObjectFrom((Map<String, Object>) obj);
    }

    return obj;
  }

  public static Map<String, Object> makeCompletedRequest(String internalId, String path, String method, Map<String, Object> param) {
    if (null == param || StringUtil.isEmpty(path) || StringUtil.isEmpty(method)) {
      return null;
    }
    param.put(BaseOperation.KEY_INTERNAL_ID, internalId);

    Map<String, Object> topParams = new HashMap<String, Object>();
    topParams.put(BaseOperation.KEY_BODY, param);
    topParams.put(BaseOperation.KEY_PATH, path);
    topParams.put(BaseOperation.KEY_HTTP_METHOD, method);
    return topParams;
  }

}
