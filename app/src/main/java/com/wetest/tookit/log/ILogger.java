package com.wetest.tookit.log;

public interface ILogger {
    public void info(String t);

    public void error(String t);

    public void error(String t, Throwable e);

    public void debug(String t);

    public String getLogPosition();

    public void clearLog();
}
