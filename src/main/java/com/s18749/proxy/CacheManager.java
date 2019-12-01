package com.s18749.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CacheManager {
  private static Properties _cacheMap = new Properties();
  private static File _cacheMapFile;
  private static Map<String, CacheFile> _cache = new HashMap<>();

  CacheManager(File dir) {
    System.out.println("INFO: loading cache: " + dir.getAbsolutePath() + "/cacheMap.prop");

    if (!dir.isDirectory())
      dir.mkdirs();

    try {
      _cacheMapFile = new File(dir.getAbsolutePath() + "/cacheMap.prop");
      if (!_cacheMapFile.exists()) {
        _cacheMapFile.createNewFile();
      } else {
        _cacheMap.load(new FileInputStream(_cacheMapFile));
      }

      List<String> toRemove = new ArrayList<>();

      _cacheMap.forEach((key, value) -> {
        File file = new File(dir.getAbsolutePath() + "/" + (String) value);

        if (file.exists()) {
          System.out.println("  " + ((String) value));
          _cache.put((String) key, new CacheFile(file));
        } else {
          System.out.println("ERR: cache file does not exist: " + (String) value);
          toRemove.add((String) value);
        }
      });

      for (String key : toRemove) {
        System.out.println("  " + key + " removed from cacheMap");
        _cacheMap.remove(key);
      }

      System.out.println("INFO: cache loaded.\n");
    } catch (IOException e) {
      System.out.println("ERR: cacheMap file does not exist or is invalid");
    }

    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

      public void run() {
        flushCacheMap();
      }
    }));
  }

  private void flushCacheMap() {
    try {
      _cacheMap.store(new FileOutputStream(_cacheMapFile), null);
    } catch (IOException e) {
      System.out.println("ERR: cannot save cacheMapFile: " + e.getMessage());
    }
  }

  public CacheFile get(String key) {
    return _cache.get(key);
  }

  public CacheFile put(String key, CacheFile file) {
    _cacheMap.setProperty(key, file.getName());
    return _cache.put(key, file);
  }

  public void remove(String key) {
    _cache.remove(key);
  }
}