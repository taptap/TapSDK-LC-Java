package com.tapsdk.lc.ops;

import com.tapsdk.lc.*;
import com.tapsdk.lc.codec.Base64;
import com.tapsdk.lc.types.LCGeoPoint;
import com.tapsdk.lc.utils.LogUtil;
import com.tapsdk.lc.utils.StringUtil;
import com.tapsdk.lc.json.JSONArray;
import com.tapsdk.lc.json.JSONObject;

import java.util.*;

//@JSONType(deserializer = BaseOperationAdapter.class, serializer = BaseOperationAdapter.class)
public abstract class BaseOperation implements ObjectFieldOperation {
  static final LCLogger LOGGER = LogUtil.getLogger(BaseOperation.class);
  static final String KEY_OP = "__op";
  static final String KEY_OBJECTS = "objects";
  static final String KEY_AMOUNT = "amount";
  static final String KEY_VALUE = "value";
  public static final String KEY_INTERNAL_ID = "__internalId";
  public static final String KEY_BODY = "body";
  public static final String KEY_PATH = "path";
  public static final String KEY_HTTP_METHOD = "method";

  protected String op = null;
  protected String field = null;
  protected Object value = null;
  protected boolean isFinal = false;

  public BaseOperation() {
  }

  public BaseOperation(String op, String field, Object value, boolean isFinal) {
    this.op = op;
    this.field = field;
    this.value = value;
    this.isFinal = isFinal;
  }

  public String getOperation() {
    return this.op;
  }
  public String getField() {
    return this.field;
  }
  public Object getValue() {
    return this.value;
  }

  public void setOp(String op) {
    this.op = op;
  }
  public void setField(String field) {
    this.field = field;
  }
  public void setValue(Object value) {
    this.value = value;
  }

  public boolean isFinal() {return this.isFinal;}
  public void setFinal(boolean isFinal) {
    this.isFinal = isFinal;
  }

  public boolean checkCircleReference(Map<LCObject, Boolean> markMap) {
    if (null == markMap) {
      return false;
    }
    return checkValueCircleReference(markMap, this.value);
  }

  private static boolean checkValueCircleReference(Map<LCObject, Boolean> markMap, Object value) {
    if (null == value || null == markMap) {
      return false;
    }
    if (value instanceof LCObject) {
      LCObject v = (LCObject)value;
      if (markMap.containsKey(v) && markMap.get(v) == true) {
        return true;
      }
      boolean rst = v.hasCircleReference(markMap);
      markMap.put(v, rst);
      return rst;
    }
    if (value instanceof Collection) {
      Collection<Object> collection = (Collection<Object>) value;
      for (Object o : collection) {
        if (checkValueCircleReference(markMap, o)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * apply operation to object, in order to generate new attribute value.
   *
   * @param obj
   * @return
   */
  public abstract Object apply(Object obj);

  public abstract Map<String, Object> encode();

  /**
   * merge with previous operations.
   *
   * @param previous
   * @return
   */
  protected ObjectFieldOperation mergeWithPrevious(ObjectFieldOperation previous) {
    return NullOperation.gInstance;
  }

  public ObjectFieldOperation merge(ObjectFieldOperation other) {
    if (null == other || other instanceof NullOperation) {
      return this;
    }
    if (isFinal) {
      // ignore all previous operations.
      return this;
    }
    return mergeWithPrevious(other);
  }

  protected Object concatCollections(Object left, Object right) {
    if (null == left || null == right) {
      return null == left? right : left;
    }
    List<Object> result = new ArrayList<Object>();
    if (left instanceof Collection) {
      result.addAll((Collection<?>) left);
    } else {
      result.add(left);
    }
    if (right instanceof Collection) {
      result.addAll((Collection<?>) right);
    } else {
      result.add(right);
    }
    try {
      if (null != result) {
        HashSet uniqueSet = new HashSet(result.size());
        for (Object o:result) {
          uniqueSet.add(o);
        }
        result = Arrays.asList(uniqueSet.toArray());
      }
    } catch (Exception ex) {
      LOGGER.w("failed to concat collections.", ex);
    }
    return result;
  }

  protected void reportIllegalOperations(ObjectFieldOperation current, ObjectFieldOperation prev) {
    LOGGER.w("illegal operations. current=" + current.getClass().getSimpleName() + ", prev=" + prev.getClass().getSimpleName());
  }

  public static Object encodeObject(Object o) {
    return encodeObject(o, false);
  }

  protected static Object encodeObject(Object o, boolean isTop) {
    if (null == o) {
      return null;
    } else if (o instanceof Map) {
      return encodeMap((Map<String, Object>)o, isTop);
    } else if (o instanceof Collection) {
      return encodeCollection((Collection)o, isTop);
    } else if (o instanceof LCObject) {
      return encodeAVObject((LCObject)o, isTop);
    } else if (o instanceof LCGeoPoint) {
      return encodeGeoPointer((LCGeoPoint) o);
    }else if (o instanceof LCACL) {
      return ((LCACL) o).toJSONObject();
    } else if (o instanceof LCFile) {
      return encodeAVFile((LCFile) o);
    } else if (o instanceof Date) {
      return encodeDate((Date) o);
    } else if (o instanceof byte[]) {
      return encodeByteArray((byte[])o);
    } else if (o instanceof JSONObject || o instanceof JSONArray) {
      return o;
    } else {
      ;
    }
    return o;
  }

  protected static Object encodeMap(Map<String, Object> map, boolean isTop) {
    Map newMap = new HashMap<String, Object>(map.size());

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      final String key = entry.getKey();
      Object o = entry.getValue();
      newMap.put(key, encodeObject(o, isTop));
    }

    return newMap;
  }

  protected static Object encodeCollection(Collection collection, boolean isTop) {
    List result = new ArrayList(collection.size());
    for (Object o: collection) {
      result.add(encodeObject(o, isTop));
    }
    return result;
  }

  protected static Object encodeAVObject(LCObject o, boolean isTop) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("className", o.getClassName());
    if (!StringUtil.isEmpty(o.getObjectId())) {
      result.put("objectId", o.getObjectId());
    }
    if (isTop) {
      result.put("__type", "Object");
      Map<String, Object> serverData = (Map<String, Object>) encodeMap(o.getServerData(), false);
      if (serverData != null && !serverData.isEmpty()) {
        result.putAll(serverData);
      }
    } else {
      result.put("__type", "Pointer");
    }
    return result;
  }

  protected static Object encodeGeoPointer(LCGeoPoint o) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("__type", "GeoPoint");
    result.put("latitude", o.getLatitude());
    result.put("longitude", o.getLongitude());
    return result;
  }

  protected static Object encodeAVFile(LCFile o) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("__type", "_File");
    result.put("metaData", o.getMetaData());
    result.put("id", o.getName());
    return result;
  }

  protected static Object encodeByteArray(byte[] o) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("__type", "Bytes");
    result.put("base64", Base64.encodeToString(o, Base64.NO_WRAP));
    return result;
  }

  public static Map<String, Object> encodeDate(Date date) {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put("__type", "Date");
    result.put("iso", StringUtil.stringFromDate(date));
    return result;
  }
}
