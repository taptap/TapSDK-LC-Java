package com.tapsdk.lc;

import com.tapsdk.lc.ops.Utils;
import com.tapsdk.lc.utils.StringUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LCRelation<T extends LCObject> {
  private String key;

  private transient LCObject parent;

  private String targetClass;

  public LCRelation() {
    super();
  }
  public LCRelation(LCObject parent, String key) {
    this();
    this.parent = parent;
    this.key = key;
  }

  public LCRelation(String targetClass) {
    this(null, null);
    this.targetClass = targetClass;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public LCObject getParent() {
    return parent;
  }

  public void setParent(LCObject parent) {
    this.parent = parent;
  }

  public String getTargetClass() {
    return targetClass;
  }

  public void setTargetClass(String targetClass) {
    this.targetClass = targetClass;
  }

  /**
   * Adds an object to this relation.
   *
   * @param object The object to add to this relation.
   */
  public void add(T object) {
    if (object == null) throw new IllegalArgumentException("null AVObject");
    if (StringUtil.isEmpty(targetClass)) {
      targetClass = object.getClassName();
    }
    if (!StringUtil.isEmpty(targetClass) && !targetClass.equals(object.getClassName())) {
      throw new IllegalArgumentException("Could not add class '" + object.getClassName()
              + "' to this relation,expect class is '" + targetClass + "'");
    }
    parent.addRelation(object, key);
  }

  /**
   * Adds many objects to this relation.
   *
   * @param objects The objects to add to this relation.
   */
  public void addAll(Collection<T> objects) {
    if (objects != null) {
      for (T obj : objects) {
        add(obj);
      }
    }
  }

  /**
   * Removes an object from this relation.
   *
   * @param object The object to remove from this relation.
   */
  public void remove(LCObject object) {
    parent.removeRelation(object, key);
  }

  /**
   * Gets a query that can be used to query the objects in this relation.
   *
   * @return A AVQuery that restricts the results to objects in this relations.
   */
  public LCQuery<T> getQuery() {
    return this.getQuery(null);
  }

  /**
   * Gets a query that can be used to query the subclass objects in this relation.
   *
   * @param clazz The AVObject subclass.
   * @return A AVQuery that restricts the results to objects in this relations.
   */
  public LCQuery<T> getQuery(Class<T> clazz) {
    if (getParent() == null || StringUtil.isEmpty(getParent().getObjectId())) {
      throw new IllegalStateException("unable to encode an association with an unsaved AVObject");
    }

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("object", Utils.mapFromPointerObject(LCRelation.this.getParent()));
    map.put("key", LCRelation.this.getKey());

    String targetClassName = getTargetClass();
    if (StringUtil.isEmpty(targetClassName)) {
      targetClassName = getParent().getClassName();
    }
    LCQuery<T> query = new LCQuery<T>(targetClassName, clazz);
    query.addWhereItem("$relatedTo", null, map);
    if (StringUtil.isEmpty(getTargetClass())) {
      query.getParameters().put("redirectClassNameForKey", this.getKey());
    }

    return query;
  }

  /**
   * Create a query that can be used to query the parent objects in this relation.
   *
   * @param parentClassName The parent class name
   * @param relationKey The relation field key in parent
   * @param child The child object.
   * @param <M> template type.
   * @return A AVQuery that restricts the results to parent objects in this relations.
   */
  public static <M extends LCObject> LCQuery<M> reverseQuery(String parentClassName,
                                                             String relationKey, LCObject child) {
    LCQuery<M> query = new LCQuery<M>(parentClassName);
    query.whereEqualTo(relationKey, Utils.mapFromPointerObject(child));
    return query;
  }

  /**
   * Create a query that can be used to query the parent objects in this relation.
   *
   * @param theParentClazz The parent subclass.
   * @param relationKey The relation field key in parent
   * @param child The child object.
   * @param <M> template type.
   * @return A AVQuery that restricts the results to parent objects in this relations.
   */
  public static <M extends LCObject> LCQuery<M> reverseQuery(Class<M> theParentClazz,
                                                             String relationKey, LCObject child) {
    LCQuery<M> query = new LCQuery<M>(Transformer.getSubClassName(theParentClazz), theParentClazz);
    query.whereEqualTo(relationKey, Utils.mapFromPointerObject(child));
    return query;
  }

}
