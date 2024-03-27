package com.tapsdk.lc.ops;

import com.tapsdk.lc.LCObject;
import java.util.Map;

public interface ObjectFieldOperation {
  String getOperation();
  String getField();
  Object getValue();

  boolean checkCircleReference(Map<LCObject, Boolean> markMap);

  /**
   * apply operation to object, in order to generate new attribute value.
   *
   * @param obj
   * @return
   */
  Object apply(Object obj);

  /**
   * merge with previous operations.
   *
   * @param previous
   * @return
   */
  ObjectFieldOperation merge(ObjectFieldOperation previous);

  /**
   * encode operation to commit into cloud server.
   *
   * @return
   */
  Map<String, Object> encode();
}
