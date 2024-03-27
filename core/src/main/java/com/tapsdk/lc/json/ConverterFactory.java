package com.tapsdk.lc.json;

public interface ConverterFactory {
  retrofit2.Converter.Factory generateRetrofitConverterFactory();
  JSONParser createJSONParser();
}
