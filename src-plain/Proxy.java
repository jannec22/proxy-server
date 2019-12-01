// package com.s18749.proxy;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Proxy {
  private ServerSocket socket;

  public static File cacheDir;
  public static CacheManager cacheManager;
  public static ProxyConfig config;
  private static HashMap<Long, Thread> clients = new HashMap<>();
  private int _port;

  public Proxy() {
    config = new ProxyConfig(new File("configuration.cnf"));
    cacheDir = new File(config.cacheDirectory());
    cacheManager = new CacheManager(cacheDir);
    _port = config.proxyPort();

    CacheCleaner cleaner = new CacheCleaner(cacheDir);
    cleaner.start();
  }

  public static CacheManager getCache() {
    return cacheManager;
  }

  public void listen() {
    Socket client = null;

    try {
      socket = new ServerSocket(_port);
    } catch (IOException e) {
      System.out.println("ERR: Could not listen: " + e.getMessage());
      System.exit(-1);
    }

    System.out.println("Proxy listens on port: " + socket.getLocalPort());

    while (true) {
      try {
        client = socket.accept();
        // System.out.println("Accepted");
      } catch (IOException e) {
        System.out.println("Accept failed");
        System.exit(-1);
      }

      ProxyClient proxyClient = new ProxyClient(client);
      Thread clientThread = new Thread(proxyClient);
      clientThread.start();
      clients.put(clientThread.getId(), clientThread);
    }
  }

  public static void reloadCache() {
    cacheManager = new CacheManager(cacheDir);
  }

  public static void main(String[] args) {
    Proxy mapper = new Proxy();

    mapper.listen();
  }
}