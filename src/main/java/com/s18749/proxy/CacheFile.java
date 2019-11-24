package com.s18749.proxy;

import java.io.File;

public class CacheFile {
  public static File cacheDir = new File("cache/");
  private File _file;
  private String _name;

  public CacheFile(File file){
    _file = file;
  }
  
  public CacheFile(String name) throws NullPointerException, SecurityException {
    if(!cacheDir.exists()) cacheDir.mkdir();
    _name = name;
    System.out.println("INFO: creating cache file: " + cacheDir.getAbsolutePath() + "/" + Integer.toString(name.hashCode()));

    _file = new File(cacheDir.getAbsolutePath() + "/" + Integer.toString(name.hashCode()));
  }

  public File getRawFile(){
    return _file;
  }

  public String getName() {
    return _name;
  }

}