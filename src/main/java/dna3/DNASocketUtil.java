package dna3;

import org.springframework.util.SocketUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class DNASocketUtil {
    public static ServerSocket getServerSocket(int portNumber) throws BindException, IOException {
        return new ServerSocket(portNumber);
    }

    public static ServerSocketChannel getServerSocketChannel(int portNumber) throws IOException {
        if (portNumber == 0)
            portNumber = SocketUtils.findAvailableTcpPort();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        SocketAddress port = new InetSocketAddress(portNumber);
        serverChannel.socket().bind(port);
        return serverChannel;
    }

    public static ServerSocketChannel getServerSocketChannel(int portNumber, Selector[] aSelector) throws IOException {
        if (portNumber == 0)
            portNumber = SocketUtils.findAvailableTcpPort();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        SocketAddress port = new InetSocketAddress(portNumber);
        serverChannel.socket().bind(port);
        Selector selector = Selector.open();
        serverChannel.register(selector, 16);
        aSelector[0] = selector;
        return serverChannel;
    }

    public static Socket getSocket(String hostName, int portNumber) throws BindException, IOException {
        return new Socket(hostName, portNumber);
    }

    public static Socket getSocket(InetAddress address, int portNumber) throws BindException, IOException {
        return new Socket(address, portNumber);
    }

    public static SocketChannel getSocketChannel(String hostName, int portNumber) throws BindException, IOException {
        InetAddress inetAddress = InetAddress.getByName(hostName);
        SocketChannel socketchannel = getSocketChannel(inetAddress, portNumber);
        return socketchannel;
    }

    public static SocketChannel getSocketChannel(InetAddress address, int portNumber) throws BindException, IOException {
        SocketChannel socketchannel = createSocketChannel(address, portNumber);
        while (!socketchannel.finishConnect()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException interruptedException) {}
        }
        return socketchannel;
    }

    private static SocketChannel createSocketChannel(InetAddress addr, int port) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(addr, port));
        return socketChannel;
    }

    public static void setSoTimout(ServerSocket serverSocket, int milliseconds) throws SocketException {
        serverSocket.setSoTimeout(milliseconds);
    }

    public static void setSoTimout(Socket socket, int milliseconds) throws SocketException {
        socket.setSoTimeout(milliseconds);
    }

    public static Socket accept(ServerSocket serverSocket) throws IOException {
        return serverSocket.accept();
    }

    public static SocketChannel acceptChannel(ServerSocketChannel serverSocketChannel) throws IOException {
        return serverSocketChannel.accept();
    }

    public static SocketChannel doAccept(Selector selector) throws IOException {
        SocketChannel channel = null;
        int keysCount = selector.select(10000L);
        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
        if (0 == keysCount)
            if (false == it.hasNext())
                throw new SocketTimeoutException();
        while (it.hasNext()) {
            SelectionKey sk = it.next();
            it.remove();
            if (sk.isAcceptable()) {
                ServerSocketChannel ssc = (ServerSocketChannel)sk.channel();
                channel = ssc.accept();
                channel.configureBlocking(false);
                break;
            }
        }
        return channel;
    }

    public static SocketChannel openChannel(SocketAddress address) throws IOException {
        return SocketChannel.open(address);
    }

    public static void close(ServerSocket serverSocket) throws IOException {
        serverSocket.close();
    }

    public static void closeChannel(ServerSocketChannel serverSocketChannel) throws IOException {
        serverSocketChannel.close();
    }

    public static void close(Socket socket) throws IOException {
        socket.close();
    }

    public static void closeChannel(SocketChannel socketChannel) throws IOException {
        socketChannel.close();
    }

    public static InetAddress getInetAddress(Socket socket) {
        return socket.getInetAddress();
    }

    public static InetAddress getChannelInetAddress(SocketChannel socketchannel) {
        return socketchannel.socket().getInetAddress();
    }

    public static int getPort(Socket socket) {
        return socket.getPort();
    }

    public static int getChannelPort(SocketChannel socketchannel) {
        return socketchannel.socket().getLocalPort();
    }

    public static int getServerSocketChannelPort(ServerSocketChannel serversocketchannel) {
        return serversocketchannel.socket().getLocalPort();
    }

    public static int getSoTimeout(Socket socket) throws SocketException {
        return socket.getSoTimeout();
    }

    public static InputStream getInputStream(Socket socket) throws IOException {
        return socket.getInputStream();
    }

    public static OutputStream getOutputStream(Socket socket) throws IOException {
        return socket.getOutputStream();
    }

    public static void setTcpNoDelay(Socket socket, boolean delay) throws SocketException {
        socket.setTcpNoDelay(delay);
    }

    public static void shutdownOutput(Socket socket) throws IOException {
        socket.shutdownOutput();
    }

    public static void shutdownInput(Socket socket) throws IOException {
        socket.shutdownInput();
    }

    public static boolean isClosed(Socket socket) {
        return socket.isClosed();
    }

    public static boolean isInputShutdown(Socket socket) {
        return socket.isInputShutdown();
    }

    public static boolean isOutputShutdown(Socket socket) {
        return socket.isOutputShutdown();
    }

    public static int findFreePort() {
        return SocketUtils.findAvailableTcpPort();
    }
}
