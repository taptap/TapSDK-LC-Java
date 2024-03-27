package com.tapsdk.lc.util;

public interface WeakConcurrentHashMapListener<K, V> {
  void notifyOnAdd(K key, V value);
  void notifyOnRemoval(K key, V value);
}
