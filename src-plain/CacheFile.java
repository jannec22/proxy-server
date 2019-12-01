// package com.s18749.proxy;

import java.io.File;
import java.util.Date;

public class CacheFile {
  private File _file;

  public CacheFile(File file) {
    _file = file;
  }

  public CacheFile(String name) throws NullPointerException, SecurityException {
    if (!Proxy.cacheDir.exists())
      Proxy.cacheDir.mkdir();

    System.out.println("INFO: creating cache file: " + Proxy.cacheDir.getAbsolutePath() + "/" + name);

    _file = new File(
        Proxy.cacheDir.getAbsolutePath() + "/" + (new Date().getTime() + Proxy.config.cacheExpirationDelay() * 60 * 1000) + "_" + name);
  }

  public File getRawFile() {
    return _file;
  }

  public String getName() {
    return _file.getName();
  }

  public boolean exist() {
    return _file.exists();
  }
}