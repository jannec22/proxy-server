package com.s18749.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

public class ProxyClient implements Runnable {
  private Socket _socket;
  private BufferedReader _in;
  private DataOutputStream _out;

  public ProxyClient(Socket socket) {
    _socket = socket;
  }

  @Override
  public void run() {
    String request = "";
    String body = "";

    String line;
    boolean firstLine = true;
    try {
      _in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));
      _out = new DataOutputStream(new BufferedOutputStream(_socket.getOutputStream()));

      boolean lastEmpty = false;
      while ((line = _in.readLine()) != null && !lastEmpty && !line.isEmpty()) {
        if (line.isEmpty())
          lastEmpty = true;
          
        if (firstLine) {
          firstLine = false;
          request = line;
        } else {
          body += line + '\n';
        }
      }

      processRequest(request, body);

    } catch (IOException e) {
      System.out.println("client> ERR: could not read client request: " + e.getMessage());

      if (_out != null) {
        byte[] responsBytes = "HTTP/1.1 500\n\n".getBytes();
        try {
          _out.write(responsBytes, 0, responsBytes.length);
          _out.flush();
        } catch (IOException ex) {
          System.out.println("ERR: could not write the response" + ex.getMessage());
        }
      }

    }
  }

  public void processRequest(String requestString, String body) throws IOException {
    System.out.println(requestString + "\n\n" + body + "\n\n");

    if (_out == null)
      return;

    String[] request = requestString.split(" ");

    if (request.length != 3 || !UrlUtil.validate(request[1])) {
      byte[] responsBytes = "HTTP/1.1 400\n\n".getBytes();
      _out.write(responsBytes, 0, responsBytes.length);
      _out.flush();
      _out.close();
      return;
    }

    CacheFile cached = Proxy.getCache().get(request[1]);
    URL url;

    if (cached == null) {
      System.out.println("Attempting to " + request[0] + ": " + request[1]);

      try {

        cached = new CacheFile(request[1]);

      } catch (NullPointerException e) {
        System.out.println("ERR: error while creating cache file: " + e.getMessage());
        cached = null;
      } catch (SecurityException e) {
        System.out.println("ERR: error while creating cache file: " + e.getMessage());
        cached = null;
      }
    }

    try {

      url = new URL(request[1]);

    } catch (MalformedURLException e) {
      System.out.println("ERR: bad url provided: " + e.getMessage());
      byte[] responsBytes = "HTTP/1.1 400\n\n".getBytes();
      _out.write(responsBytes, 0, responsBytes.length);
      _out.flush();
      _out.close();
      return;
    }

    DataOutputStream cachedWriter = null;
    try {
      if (cached != null)
        cachedWriter = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cached.getRawFile())));

      int port = url.getPort();
      if (port == -1)
        port = 80;
      System.out.println("INFO: connecting to: " + url.getHost() + ":" + port);
      Socket socket = new Socket(url.getHost(), port);

      DataOutputStream resourceHostWriter;
      try {
        resourceHostWriter = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        byte[] responsBytes = (requestString + "\n\n" + body + "\n\n").getBytes();
        resourceHostWriter.write(responsBytes, 0, responsBytes.length);
        resourceHostWriter.flush();

      } catch (IOException e) {
        System.out.println("ERR: could not read the resource");
        byte[] responsBytes = "HTTP/1.1 500\n\n".getBytes();
        _out.write(responsBytes, 0, responsBytes.length);
        _out.flush();
        _out.close();
        socket.close();
        return;
      }

      DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

      int count;
      byte[] buffer = new byte[8192]; // or 4096, or more
      while ((count = in.read(buffer)) > 0) {
        if (cachedWriter != null) {
          cachedWriter.write(buffer, 0, count);
        }
        _out.write(buffer, 0, count);
      }

      if (cachedWriter != null) {
        cachedWriter.flush();
        cachedWriter.close();
      }

      byte[] responsBytes = "\n\n".getBytes();
      _out.write(responsBytes, 0, responsBytes.length);
      _out.flush();
      _out.close();
      socket.close();

      if (cached != null)
        Proxy.getCache().put(cached.getName(), cached);

    } catch (SecurityException e) {
      byte[] responsBytes = "HTTP/1.1 500\n\n".getBytes();
      _out.write(responsBytes, 0, responsBytes.length);
      _out.flush();
      _out.close();
    } catch (FileNotFoundException e) {
      byte[] responsBytes = "HTTP/1.1 404\n\n".getBytes();
      _out.write(responsBytes, 0, responsBytes.length);
      _out.flush();
      _out.close();
    } catch (SocketTimeoutException e) {
      byte[] responsBytes = "HTTP/1.1 408\n\n".getBytes();
      _out.write(responsBytes, 0, responsBytes.length);
      _out.flush();
      _out.close();
    } catch (UnknownHostException e) {
      System.out.println("ERR: unknown host: " + url.getHost() + " -> " + e.getMessage());

      byte[] responsBytes = "HTTP/1.1 400\n\n".getBytes();
      _out.write(responsBytes, 0, responsBytes.length);
      _out.flush();
      _out.close();
    } catch (IOException e) {
      System.out.println("ERR: error while getting resource: " + e.getMessage());

      byte[] responsBytes = "HTTP/1.1 500\n\n".getBytes();
      _out.write(responsBytes, 0, responsBytes.length);
      _out.flush();
      _out.close();
    }

  }

}