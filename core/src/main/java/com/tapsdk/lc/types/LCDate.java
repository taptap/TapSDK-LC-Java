package com.tapsdk.lc.types;

import com.tapsdk.lc.utils.StringUtil;
import com.tapsdk.lc.json.JSON;
import com.tapsdk.lc.json.JSONObject;
import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class LCDate {
  public static final String DEFAULT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  private static final ThreadLocal<SimpleDateFormat> THREAD_LOCAL_DATE_FORMAT =
          new ThreadLocal<SimpleDateFormat>();

  @SerializedName("__type")
  private String type = "Date";

  private String iso = "";

  public LCDate() {
  }

  public LCDate(JSONObject obj) {
    if (null != obj) {
      this.iso = obj.getString("iso");
    }
  }
  public Date getDate() {
    SimpleDateFormat sdf = THREAD_LOCAL_DATE_FORMAT.get();
    if (null == sdf) {
      sdf = new SimpleDateFormat(DEFAULT_FORMAT);
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
      THREAD_LOCAL_DATE_FORMAT.set(sdf);
    }
    if (StringUtil.isEmpty(this.iso)) {
      return null;
    }
    try {
      Date result = sdf.parse(this.iso);
      return result;
    } catch (ParseException ex) {
      return null;
    }
  }

  public LCDate(String dateString) {
    iso = dateString;
  }

  public String getType() {
    return type;
  }

  public void setType(String __type) {
    this.type = __type;
  }

  public String getIso() {
    return iso;
  }

  public void setIso(String iso) {
    this.iso = iso;
  }

  public String toJSONString() {
    return JSON.toJSONString(this);
  }
}
