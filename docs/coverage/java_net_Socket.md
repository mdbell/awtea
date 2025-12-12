# Class: `Socket` ![Coverage](https://img.shields.io/badge/coverage-13.0%25-red)

**Full Name:** `java.net.Socket`

**Coverage:** 7 / 54 (13.0%)

```
[██████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 13.0%
```

## ✓ Implemented Methods

- `public int getSoTimeout()`
- `public java.io.InputStream getInputStream()`
- `public java.io.OutputStream getOutputStream()`
- `public void close()`
- `public void setSoTimeout(int)`
- `public void setTcpNoDelay(boolean)`

## ✗ Missing Methods

- `public boolean getKeepAlive()`
- `public boolean getOOBInline()`
- `public boolean getReuseAddress()`
- `public boolean getTcpNoDelay()`
- `public boolean isBound()`
- `public boolean isClosed()`
- `public boolean isConnected()`
- `public boolean isInputShutdown()`
- `public boolean isOutputShutdown()`
- `public int getLocalPort()`
- `public int getPort()`
- `public int getReceiveBufferSize()`
- `public int getSendBufferSize()`
- `public int getSoLinger()`
- `public int getTrafficClass()`
- `public java.lang.Object getOption(java.net.SocketOption)`
- `public java.lang.String toString()`
- `public java.net.InetAddress getInetAddress()`
- `public java.net.InetAddress getLocalAddress()`
- `public java.net.Socket setOption(java.net.SocketOption, java.lang.Object)`
- `public java.net.SocketAddress getLocalSocketAddress()`
- `public java.net.SocketAddress getRemoteSocketAddress()`
- `public java.nio.channels.SocketChannel getChannel()`
- `public java.util.Set supportedOptions()`
- `public static void setSocketImplFactory(java.net.SocketImplFactory)`
- `public void bind(java.net.SocketAddress)`
- `public void connect(java.net.SocketAddress)`
- `public void connect(java.net.SocketAddress, int)`
- `public void sendUrgentData(int)`
- `public void setKeepAlive(boolean)`
- `public void setOOBInline(boolean)`
- `public void setPerformancePreferences(int, int, int)`
- `public void setReceiveBufferSize(int)`
- `public void setReuseAddress(boolean)`
- `public void setSendBufferSize(int)`
- `public void setSoLinger(boolean, int)`
- `public void setTrafficClass(int)`
- `public void shutdownInput()`
- `public void shutdownOutput()`

## ✓ Implemented Constructors

- `public java.net.Socket(java.net.InetAddress, int)`

## ✗ Missing Constructors

- `protected java.net.Socket(java.net.SocketImpl)`
- `public java.net.Socket()`
- `public java.net.Socket(java.lang.String, int)`
- `public java.net.Socket(java.lang.String, int, boolean)`
- `public java.net.Socket(java.lang.String, int, java.net.InetAddress, int)`
- `public java.net.Socket(java.net.InetAddress, int, boolean)`
- `public java.net.Socket(java.net.InetAddress, int, java.net.InetAddress, int)`
- `public java.net.Socket(java.net.Proxy)`


[← Back to Package](java_net.md)
