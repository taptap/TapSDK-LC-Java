package com.tapsdk.lc.logging;

import com.tapsdk.lc.LCLogger;

public class DummyLogger extends InternalLogger {
  protected void internalWriteLog(LCLogger.Level level, String msg) {}
  protected void internalWriteLog(LCLogger.Level level, String msg, Throwable tr) {}
  protected void internalWriteLog(LCLogger.Level level, Throwable tr) {}
}
