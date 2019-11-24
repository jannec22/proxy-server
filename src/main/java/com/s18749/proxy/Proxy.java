package com.s18749.proxy;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Proxy {
  private ServerSocket socket;
  private static HashMap<Long, Thread> clients = new HashMap<>();
  private static HashMap<String, CacheFile> cache = new HashMap<>();
  private int _port;

  public Proxy(int port) {
    _port = port;
    loadCache();
  }

  public static synchronized HashMap<String, CacheFile> getCache() {
    return cache;
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
        System.out.println("Accepted");
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

  private void loadCache(){
    System.out.println("INFO: loading cache:");
    File[] fileList = (new File("cache/")).listFiles();

    for (File file : fileList) {

      if (file.isFile()) {
        System.out.println("  " + file.getName());
        Proxy.cache.put(file.getName(), new CacheFile(file));
      }
    }

    System.out.println("INFO: cache loaded.\n");
  }

  public static void main(String[] args) {
    Proxy mapper = new Proxy(55555);

    mapper.listen();
  }
}