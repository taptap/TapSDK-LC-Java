package com.tapsdk.lc.gson;

import com.tapsdk.lc.json.ConverterFactory;
import com.tapsdk.lc.json.JSONParser;
import retrofit2.converter.gson.GsonConverterFactory;

public class GSONConverterFactory implements ConverterFactory {
  public retrofit2.Converter.Factory generateRetrofitConverterFactory() {
    return GsonConverterFactory.create(GsonWrapper.getGsonInstance());
  }

  public JSONParser createJSONParser() {
    return new GSONParser();
  }
}
