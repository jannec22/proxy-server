// package com.s18749.proxy;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyClient implements Runnable {
  private static long clientCount = 0;
  private Socket _socket;
  private BufferedReader _in;
  private PrintWriter _out;

  public ProxyClient(Socket socket) {
    _socket = socket;
  }

  @Override
  public void run() {
    System.out.println("CLIENT CONNECTED, active clients: " + ++clientCount);

    try {
      _in = new BufferedReader(new InputStreamReader(_socket.getInputStream()));

      try {
        _out = new PrintWriter(_socket.getOutputStream());
      } catch (IOException e) {
        System.out.println("ERR: could not open write stream to the client: " + e.getMessage());
        _in.close();
        return;
      }

      processRequest(_in.readLine());

      _in.close();
      _socket.close();

    } catch (SocketException e) {
      e.printStackTrace();
      System.out.println("ERR: browser closed the connection");
    } catch (IOException e) {
      System.out.println("ERR: could not read client request: " + e.getMessage());
    }
    System.out.println("CLIENT DISCONNECTED, active clients: " + --clientCount);
  }

  public void processRequest(String requestString) {
    if (requestString == null || requestString.isEmpty()) {
      System.out.println("ERR: request string is empty");
      return;
    }

    if (requestString.startsWith("CONNECT")) {
      System.out.println("ignoring CONNECT request");
      return;
    }

    if (_out == null) {
      System.out.println("could not open write stream to the browser");
      return;
    }

    String[] request = requestString.split(" ");

    if (request.length != 3 || !UrlUtil.validate(request[1])) {
      System.out.println("request is invalid: " + requestString);
      writeResponse("HTTP/1.1 400\n\n");
      return;
    }

    CacheFile cached = Proxy.getCache().get(request[1]);
    URL url;

    Pattern pattern = Pattern.compile(
        ".*(txt|jpg|jpeg|png|css|js|html|htm|gif|avi|doc|exe|mid|midi|mp3|mp4|mpg|mpeg|mov|qt|pdf|ram|rar|tiff|wav|zip|tar|jar|woff|ttf|woff2).*",
        Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(request[1]);

    if (cached == null || (cached != null && !cached.exist())) {
      // System.out.println("Attempting to " + request[0] + ": " + request[1]);

      if (cached != null)
        Proxy.getCache().remove(cached.getName());

      if (matcher.matches() || request[1].endsWith("/")) {
        try {

          cached = new CacheFile(
              UUID.randomUUID().toString() + "." + (request[1].endsWith("/") ? "index.html" : matcher.group(1)));

        } catch (NullPointerException e) {
          System.out.println("ERR: error while creating cache file: " + e.getMessage());
          cached = null;
        } catch (SecurityException e) {
          System.out.println("ERR: error while creating cache file: " + e.getMessage());
          cached = null;
        }
      } else {
        System.out.println("INFO: not caching request: " + request[1]);
      }
    } else {
      System.out.println("file is cached -> " + cached.getName());

      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cached.getRawFile())));

        String line;
        while ((line = reader.readLine()) != null) {
          _out.println(line);
        }

        _out.println("\n\n");
        _out.flush();

        reader.close();
        System.out.println("INFO: written cached response");
        return;
      } catch (SocketException e) {
        System.out.println("ERR: server closed the connection");
      } catch (IOException e) {
        System.out.println("ERR: could not write cached response: " + e.getMessage());
      }
    }

    try {

      url = new URL(request[1]);

    } catch (MalformedURLException e) {
      System.out.println("ERR: bad url provided: " + e.getMessage());

      writeResponse("HTTP/1.1 400\n\n");
      return;
    }

    PrintWriter cachedWriter = null;

    if (cached != null) {
      try {
        cachedWriter = new PrintWriter(new FileOutputStream(cached.getRawFile()));
      } catch (FileNotFoundException e) {
        System.out.println("WARN: could not found cache file: " + e.getMessage());
      } catch (SecurityException e) {
        System.out.println("WARN: could not read cache file: " + e.getMessage());
      }
    }

    int port = url.getPort();
    if (port == -1)
      port = 80;
    // System.out.println("INFO: connecting to: " + url.getHost() + ":" + port);

    try (Socket socket = new Socket(url.getHost(), port)) {

      PrintWriter resourceHostWriter;
      BufferedReader in;

      try {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      } catch (IOException e) {
        System.out.println("ERR: could not establish read connection to the server");
        return;
      }

      try {
        resourceHostWriter = new PrintWriter(socket.getOutputStream());
        resourceHostWriter.println(requestString + "\n\n");
        resourceHostWriter.flush();
      } catch (IOException e) {
        System.out.println("ERR: could not establish connection to the server: " + e.getMessage());
        return;
      }

      try {
        String line;
        boolean is200 = false;

        while ((line = in.readLine()) != null) {
          if (cachedWriter != null) {
            if(is200 || line.contains("HTTP/1.1 200 OK")) {
              is200 = true;
              cachedWriter.println(line);
            }
          }
          _out.println(line);
        }

        if (!socket.isClosed()) {
          _out.println("\n\n");
          _out.flush();
        } else {
          System.out.println("ERR: connection has been closed while writing the request");
          return;
        }

        if (cached != null)
          Proxy.getCache().put(request[1], cached);

        if (cachedWriter != null) {
          cachedWriter.flush();
          cachedWriter.close();
        }
      } catch (SocketException e) {
        System.out.println("ERR: connection has been closed while writing the request " + e.getMessage());
        return;
      } catch (IOException e) {
        System.out.println("ERR: IO exception: " + e.getMessage());
        return;
      }

      in.close();
      resourceHostWriter.close();
      socket.close();
      System.out.println("INFO: written response");

    } catch (SocketException e) {
      System.out.println("ERR: server closed the connection");
    } catch (UnknownHostException e) {
      System.out.println("ERR: unknown host: " + url.getHost() + " -> " + e.getMessage());

      writeResponse("HTTP/1.1 400\n\n");
    } catch (IllegalArgumentException e) {
      System.out.println("ERR: wrong parameters: " + e.getMessage());

      writeResponse("HTTP/1.1 400\n\n");
    } catch (SecurityException e) {
      System.out.println("ERR: security exception while fetching the resource: " + e.getMessage());

      writeResponse("HTTP/1.1 500\n\n");
    } catch (IOException e) {
      System.out.println("ERR: error while getting resource: " + e.getMessage());
    }
  }

  private void writeResponse(String response) {

    if (_out != null && !_socket.isClosed()) {
      _out.println(response + "\n\n");
      _out.flush();
      _out.close();
    } else {
      System.out.println("ERR: could not write the response: socket is closed");
    }
  }

}