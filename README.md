# HTTP Proxy Server

## description

My solution consist of classes:

  - Proxy
    - listens for new clients (accepts new connections)
    - assigns each client to a new thread
    - loads and holds configuration
  
  - ProxyClient
    - starts as a new thread
    - reads a single request from the connection
    - extracts the host and the port of the server
    - if there is a cache file mapped to the request url, the cached version is returned
    - connects to the desired server and rewrites previously red request to it
    - reads the response and forwards it back to the client
    - if response from the server is HTTP/1.1 200 OK and the resource matches specified file types to match
    it is cached in the memory
  
  - CacheManager
    - holds references to cached files
    - manages synchronization between cacheMap.prop file and real files
    - manages loading saved cache and saving it when the program exits

  - ProxyConfig
    - holds proxy configuration
    - loads configuration
  
  - CacheCleaner
    - runs a task every interval specified in the configuration
    - task checks cache files against current time and removes them if they are expired

  - CacheFile
    - holds raw file and manages naming and expiration time of the cached file

### How it works
Basically Proxy class holds a SocketServer instnce and
waits in a loop for arriving connections.

when connection is established (accepted) a new thread is
created with the new instance of ProxyClient.

Proxy Client opens a stream from the client and reads the first line of the request.
then it checks cache and if there is a matching not expired cache file it is returned instantly to the client.

In other case ProxyClient parses url from the request and extracts host and port number of a server with the resource.

Then it connects to the server and send the exact request that it received from the client.
After that, a new stream is opened to read all the response and parallely forward it to the client and cache if possible.

At the end sockets and streams are flushed and closed.

# Configuration

default configuration:
```cnf
cache_expiration_delay=3 //in minutes
cache_clean_task_interval=3 // in minutes
proxy_port=1234
cache_directory=cache/
```

# How to

## Compile

### Linux
  - option 1 -> plain java

  ```bash
  cd /path/to/repo/
  chmod +x compile-plain
  ./compile-plain
  ```
  - option 2 -> maven

  ```bash
  cd /path/to/repo/
  chmod +x compile-maven
  ./compile-maven
  ```

### Other
sorry, i did not have time to prepare compile scripts for windows

## Run

### Linux

```bash
cd /path/to/repo/
chmod +x run
./run
```

### Other
sorry, i did not have time to prepare run scripts for windows

## Use

The easiest way in my opinion is to use Firefox web browser.
You need to go to the preferences -> general, scroll to the bottom, open settings and in the popup use **_manual proxy configuration_** with options:
  - http proxy: localhost
  - port [port specified in configuration]

Then you can open [this site](http://scratchpads.eu/explore/sites-list) in firefox to get a list of sites using http protocol.
when you open a site from this list you cn hit ctrl+shift+i to open dev tools, where you can observe pending requests in Network tab.


# What does not work
when there is a lot of requests from the browser, external server complains about too many open connections between itself and proxy and returns 429 TOO MANY REQUEST status