package com.tapsdk.lc.util;

import com.tapsdk.lc.LCLogger;
import com.tapsdk.lc.utils.LogUtil;
import com.tapsdk.lc.utils.StringUtil;

import java.lang.reflect.Method;

public class FieldAttribute {
  private static final LCLogger LOGGER = LogUtil.getLogger(FieldAttribute.class);

  String fieldName;
  Method getterMethod;
  Method setterMethod;
  String aliaName;
  Class<?> fieldType;

  public FieldAttribute(String fieldName, String aliaName, Method getterMethod,
                        Method setterMethod, Class<?> type) {
    this.aliaName = aliaName;
    this.fieldName = fieldName;
    this.getterMethod = getterMethod;
    this.setterMethod = setterMethod;
    this.fieldType = type;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public Method getGetterMethod() {
    return getterMethod;
  }

  public void setGetterMethod(Method getterMethod) {
    this.getterMethod = getterMethod;
  }

  public Method getSetterMethod() {
    return setterMethod;
  }

  public void setSetterMethod(Method setterMethod) {
    this.setterMethod = setterMethod;
  }

  public String getAliaName() {
    return StringUtil.isEmpty(aliaName) ? fieldName : aliaName;
  }

  public void setAliaName(String aliaName) {
    this.aliaName = aliaName;
  }

  public Class<?> getFieldType() {
    return fieldType;
  }

  public void setFieldType(Class<?> fieldType) {
    this.fieldType = fieldType;
  }

  public Object get(Object receiver) {
    try {
      if (getterMethod != null) {
        return getterMethod.invoke(receiver);
      } else {
        throw new RuntimeException();
      }
    } catch (Exception e) {
      LOGGER.d("Failed to invoke getter:" + fieldName);
    }
    return null;
  }

  public void set(Object receiver, Object value) {
    try {
      if (setterMethod != null) {
        setterMethod.invoke(receiver, value);
      } else {
        throw new RuntimeException();
      }
    } catch (ReflectiveOperationException e) {
      LOGGER.d("Failed to invoke setter:" + fieldName);
    } catch (IllegalArgumentException ex) {
      LOGGER.d("Failed to invoke setter:" + fieldName);
    }
  }
}
