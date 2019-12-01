// package com.s18749.proxy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class CacheCleaner implements Runnable {

  private File _directory;

  CacheCleaner(File dir) {
    _directory = dir;
  }

  public void start() {
    ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    service.scheduleAtFixedRate(new Thread(this), Proxy.config.cacheCleanTaskInterval(), Proxy.config.cacheCleanTaskInterval(), TimeUnit.MINUTES);
  }

  public void run() {
    System.out.println("STARTING CACHE CLEANING TASK");
    if (_directory == null || !_directory.exists())
      return;
    Long timestamp = new Date().getTime();

    try {

      List<File> files = Files //
          .find(_directory.toPath(), 1, (path, attributes) -> filterByName.test(path, timestamp)) //
          .map(path -> path.toFile()) //
          .filter(File::isFile) //
          .collect(Collectors.toList());

      for (File file : files) {
        System.out.println("FILE: " + file.getName() + " is expired, deleting...");
        file.delete();
      }

      Proxy.reloadCache();

    } catch (IOException e) {
      System.out.println("ERR: could not read ore delete some files");
    }
    System.out.println("CACHE CLEANED");
  }

  public static final BiPredicate<Path, Long> filterByName = (Path path, Long now) -> {
    if (path.getFileName().toString().equals("cacheMap.prop"))
      return false;

    String[] splittedPath = path.getFileName().toString().split("_");
    if (splittedPath.length != 2)
      return true; // delete

    try {
      long expireDate = Long.parseLong(splittedPath[0]);

      return expireDate < now;
    } catch (NumberFormatException e) {
      return true; // delete
    }
  };
}