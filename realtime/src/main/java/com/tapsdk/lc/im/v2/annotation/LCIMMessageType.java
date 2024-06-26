package com.tapsdk.lc.im.v2.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LCIMMessageType {
  int type();

  int TEXT_MESSAGE_TYPE = -1;
  int IMAGE_MESSAGE_TYPE = -2;
  int AUDIO_MESSAGE_TYPE = -3;
  int VIDEO_MESSAGE_TYPE = -4;
  int LOCATION_MESSAGE_TYPE = -5;
  int FILE_MESSAGE_TYPE = -6;
  int RECALLED_MESSAGE_TYPE = -127;
}
