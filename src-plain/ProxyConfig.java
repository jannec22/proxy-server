// package com.s18749.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class ProxyConfig {
  private static Properties props = new Properties();

  ProxyConfig(File configFile) {
    try {
      // default props
      props.setProperty("cache_expiration_delay", "3");
      props.setProperty("cache_clean_task_interval", "3");
      props.setProperty("proxy_port", "1234");
      props.setProperty("cache_directory", "cache/");

      props.load(new FileInputStream(configFile));

      System.out.println("Proxy configuration:\n");
      props.forEach((key, val) -> {
        System.out.println("  " + (String) key + ": " + (String) val);
      });
      System.out.println("\n");

    } catch (FileNotFoundException e) {
      System.out.println("ERR: config file not found, running on default props");
      // System.exit(-1);
    } catch (IOException e) {
      System.out.println("ERR: could not read config file");
      // System.exit(-2);
    }
  }

  public long cacheExpirationDelay() {
    return Long.parseLong(props.getProperty("cache_expiration_delay"));
  }

  public long cacheCleanTaskInterval() {
    return Long.parseLong(props.getProperty("cache_clean_task_interval"));
  }

  public int proxyPort() {
    return Integer.parseInt(props.getProperty("proxy_port"));
  }

  public String cacheDirectory(){
    return props.getProperty("cache_directory");
  }
}