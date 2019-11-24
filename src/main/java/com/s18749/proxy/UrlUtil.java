package com.s18749.proxy;

public class UrlUtil{
  public static boolean validate(String url) {
    return url.matches("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
  }
}