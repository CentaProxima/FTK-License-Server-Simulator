package dna3;

import ad.lang.StringToolkit;
import ad.shared.xml.Document;
import ad.shared.xml.InvalidNodeTypeException;
import ad.shared.xml.Node;
import ad.utils.ADLogger;
import dna3Common.ADHandshakeException;
import dna3Common.DNASocketUtil;
import dna3Common.Protocol;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.xml.sax.SAXException;

public class Communicator {
    private static final ADLogger mLogger = ADLogger.getRootLogger();

    private static int invokeID = 0;

    private String ip;

    private int port;

    private boolean encrypted;

    protected SocketChannel mSocket;

    protected Protocol mProtocol = new Protocol();

    protected boolean mIncoming;

    protected boolean mIsMasterController;

    protected boolean mIsSupervisor;

    protected boolean mIsPasswordRecoveryToolkit;

    protected Date mLastContactTime = null;

    protected InetAddress mRemoteInetAddress = null;

    protected long mTheirRelease;

    protected boolean mRemoteIsGUI;

    protected Thread mReadBlocksThread;

    protected Communicator me = null;

    protected SortedMap<Node, Node> mWaitedForReceivedBlocks = new TreeMap<>();

    protected Node mWaitedForReceivedBlocksPurgeID = null;

    protected Thread mProcessBlocksThread;

    protected SortedMap<Node, Node> mSentBlocks = new TreeMap<>();

    protected Node mSentBlocksPurgeID = null;

    protected SortedMap<Node, Node> mReceivedBlocks = new TreeMap<>();

    protected Node mReceivedBlocksPurgeID = null;

    protected static Object mBlockIdentifierLock = new Object();

    protected static int mBlockIdentifier = 0;

    protected Thread mPurgeThread;

    protected ActionListener actionListener = null;

    private final HashMap<String, AWTEventListener> mpInfoListeners = new HashMap<>();

    protected Communicator(String hostAddress, String hostName, int port, SocketChannel socket, boolean incoming, boolean isMasterController, boolean isSupervisor, boolean isPasswordRecoveryToolkit, Date lastContactTime) {
        this.mSocket = socket;
        this.mIncoming = incoming;
        this.mIsMasterController = isMasterController;
        this.mIsSupervisor = isSupervisor;
        this.mIsPasswordRecoveryToolkit = isPasswordRecoveryToolkit;
        this.mLastContactTime = lastContactTime;
        this.ip = hostAddress;
        this.port = port;
    }

    public boolean hasSocket() {
        return (null != this.mSocket);
    }

    public static Communicator addCommunicator(SocketChannel socket, boolean incoming, boolean isMasterController, boolean isSupervisor, boolean isPasswordRecoveryToolkit) {
        Communicator thisComm = null;
        if (null != socket) {
            String ip = socket.socket().getInetAddress().getHostAddress();
            int port = socket.socket().getPort();
            thisComm = addCommunicator(socket, incoming, isMasterController, isSupervisor, isPasswordRecoveryToolkit, ip, port);
        }
        return thisComm;
    }

    public static Communicator addCommunicator(SocketChannel socket, boolean incoming, boolean isMasterController, boolean isSupervisor, boolean isPasswordRecoveryToolkit, String ip, int port) {
        boolean tcpNoDelay = false;
        if (null != socket)
            try {
                long nTheirRelease = exchangeReleases(socket);
            } catch (IOException ioe) {
                mLogger.warn("Communicator.addCommunicator: IOException: " + ioe.getMessage());
                mLogger.error(StringToolkit.stackTraceToString(ioe));
                return null;
            }
        try {
            if (null != socket)
                if (tcpNoDelay) {
                    DNASocketUtil.setTcpNoDelay(socket.socket(), tcpNoDelay);
                } else {
                    DNASocketUtil.setTcpNoDelay(socket.socket(), false);
                }
            String hostAddress = ip;
            String hostName = ip;
            if (socket != null) {
                hostAddress = socket.socket().getInetAddress().getHostAddress();
                hostName = socket.socket().getInetAddress().getHostName();
            }
            Communicator communicator = new Communicator(hostAddress, hostName, port, socket, incoming, isMasterController, isSupervisor, isPasswordRecoveryToolkit, null);
            return communicator;
        } catch (Exception e) {
            synchronized (System.class) {
                mLogger.warn("Communicator.addCommunicator: Exception: " + e);
                mLogger.error(StringToolkit.stackTraceToString(e));
            }
            return null;
        }
    }

    private String waitForHelper(Node node) throws SAXException, IOException, InterruptedException, InvalidNodeTypeException {
        if (node != null) {
            String returnValue = processBlock(node, false, true);
            if (returnValue == null || returnValue.length() == 0)
                returnValue = node.toXML(0);
            return returnValue;
        }
        return null;
    }

    private Node waitFor(Node id) throws InterruptedException {
        do {
            this.mWaitedForReceivedBlocks.wait(1000L);
            if (this.mWaitedForReceivedBlocks.get(id) != null) {
                Node node = this.mWaitedForReceivedBlocks.remove(id);
                return node;
            }
        } while (this.mSocket.isConnected());
        return null;
    }

    private void expressInterestIn(Node id) {
        synchronized (this.mWaitedForReceivedBlocks) {
            this.mWaitedForReceivedBlocks.put(id, null);
            this.mWaitedForReceivedBlocks.notifyAll();
        }
    }

    private static int nextId() {
        synchronized (mBlockIdentifierLock) {
            return ++mBlockIdentifier;
        }
    }

    private static String id(Node value) {
        if (value != null)
            return " id=\"" + value.getValue() + "\"";
        return "";
    }

    private void findInvokes(Node n, List<Node> l) {
        if (n.getName().equals("INVOKE"))
            l.add(n);
        Iterator<Node> i;
        for (i = n.getNodes("INVOKE").iterator(); i.hasNext();)
            l.add(i.next());
        for (i = n.getNodes("RESULT").iterator(); i.hasNext();)
            findInvokes(i.next(), l);
    }

    private void sendBlockHelper(Node block) throws IOException, IllegalBlockingModeException {
        byte[] xml = block.toXML(0).getBytes("UTF-8");
        this.mProtocol.writeBlock(xml);
    }

    private String sendBlock(Node block, boolean wait) throws IOException, SAXException, InterruptedException, InvalidNodeTypeException, ClosedChannelException, IllegalBlockingModeException {
        List<Node> invokes = new ArrayList<>();
        findInvokes(block, invokes);
        if (invokes.size() > 0) {
            for (Iterator<Node> i = invokes.iterator(); i.hasNext(); ) {
                Node invokeNode = i.next();
                Node invokeId = invokeNode.getAttribute("id");
                if (invokeId == null) {
                    invokeId = new Node(nextId());
                    invokeNode.setAttribute("id", invokeId);
                }
                synchronized (this.mSentBlocks) {
                    this.mSentBlocks.put(invokeId, invokeNode);
                }
            }
            if (wait) {
                List<Node> nodes = new ArrayList<>();
                synchronized (this.mWaitedForReceivedBlocks) {
                    Iterator<Node> iterator1;
                    for (iterator1 = invokes.iterator(); iterator1.hasNext(); ) {
                        Node invokeNode = iterator1.next();
                        Node invokeId = invokeNode.getAttribute("id");
                        expressInterestIn(invokeId);
                    }
                    sendBlockHelper(block);
                    if (!this.mSocket.isConnected())
                        return null;
                    for (iterator1 = invokes.iterator(); iterator1.hasNext(); ) {
                        Node invokeNode = iterator1.next();
                        Node invokeId = invokeNode.getAttribute("id");
                        Node node = waitFor(invokeId);
                        if (node != null)
                            nodes.add(node);
                    }
                }
                StringBuilder returnValue = new StringBuilder();
                for (Iterator<Node> iterator = nodes.iterator(); iterator.hasNext(); ) {
                    Node node = iterator.next();
                    String waitForHelperReturnValue = waitForHelper(node);
                    if (waitForHelperReturnValue != null)
                        returnValue.append(waitForHelperReturnValue);
                }
                return returnValue.toString();
            }
        }
        sendBlockHelper(block);
        return null;
    }

    private String processBlock(Node block, boolean spawn, boolean wait) throws SAXException, IOException, InterruptedException, InvalidNodeTypeException {
        String name = block.getName();
        if (name.equals("RESULT"))
            return processResult(block, spawn, wait);
        if (name.equals("INFORMATION")) {
            String res = processInformation(block);
            sendInvoke(res, false);
            return "";
        }
        if (block.getName().equals("INVOKE")) {
            Node id = block.getAttribute("id");
            int nID = Integer.parseInt(id.getValue());
            if (block.hasNode("INFORMATION")) {
                String res = processInformation(block);
                Node resBlock = Document.parse(res, true);
                sendBlockHelper(resBlock);
                return "";
            }
            mLogger.error("Communicator.processBlock: unsupported invoke: block: '" + block.toXML(1) + "'");
            String xmlError = "<RESULT id=\"" + nID + "\">\n\t<ERROR>Invoke unsupported</ERROR>\n</RESULT>";
            Node errBlock = Document.parse(xmlError, true);
            sendBlockHelper(errBlock);
            return xmlError;
        }
        assert false;
        return null;
    }

    private String processInformation(Node infoBlock) throws SAXException, IOException, InterruptedException, InvalidNodeTypeException {
        Node info = infoBlock.getNode("INFORMATION");
        Node id = infoBlock.getAttribute("id");
        StringBuffer result = new StringBuffer("<RESULT" + id(id) + ">\n");
        result.append("<SUCCESS/>\n</RESULT>");
        String listenerID = (info.getAttribute("type") != null) ? info.getAttribute("type").getValue() : "";
        String infoString = (info.getAttribute("info") != null) ? info.getAttribute("info").getValue() : "";
        AWTEventListener listener = this.mpInfoListeners.get(listenerID);
        if (null != listener) {
            InfoEvent evt = new InfoEvent(this, 0, infoString);
            listener.eventDispatched(evt);
        }
        return result.toString();
    }

    public void addInfoListener(AWTEventListener listener, String type) {
        this.mpInfoListeners.put(type, listener);
    }

    public void removeInfoListener(String type) {
        this.mpInfoListeners.remove(type);
    }

    private String processResult(Node block, boolean spawn, boolean wait) throws SAXException, IOException, InterruptedException, InvalidNodeTypeException {
        Node id = block.getAttribute("id");
        Node sentBlock = null;
        if (id != null)
            synchronized (this.mSentBlocks) {
                sentBlock = this.mSentBlocks.remove(id);
            }
        StringBuilder result = new StringBuilder();
        Iterator<Node> i;
        for (i = block.getNodes("RESULT").iterator(); i.hasNext(); ) {
            String processResultReturnValue = processResult(i.next(), spawn, wait);
            result.append(processResultReturnValue);
            if (processResultReturnValue != null && processResultReturnValue.contains("ERROR"))
                if (Document.parse(processResultReturnValue, true).hasNode("ERROR"))
                    sentBlock = null;
        }
        for (i = block.getNodes("INVOKE").iterator(); i.hasNext();)
            i.next();
        for (i = block.getNodes("INFORMATION").iterator(); i.hasNext(); ) {
            Node info = i.next();
            String listenerID = (info.getAttribute("ID") != null) ? info.getAttribute("ID").getValue() : "";
            String infoString = (info.getAttribute("Info") != null) ? info.getAttribute("Info").getValue() : "";
            System.out.println("ID=" + listenerID + "\nInfo=" + infoString);
        }
        if (result.length() > 0 && block.hasNode("ERROR") && sentBlock != null)
            return sendBlock(sentBlock, wait);
        return result.toString();
    }

    public void init(boolean encrypted) throws ADHandshakeException {
        init(encrypted, false);
    }

    public void init(boolean encrypted, boolean localIsGUI) throws ADHandshakeException {
        try {
            synchronized (this.mProtocol) {
                this.mRemoteInetAddress = DNASocketUtil.getChannelInetAddress(this.mSocket);
                boolean[] remoteIsGUI = new boolean[1];
                InputStream inputStream = DNASocketUtil.getInputStream(this.mSocket.socket());
                OutputStream outputStream = DNASocketUtil.getOutputStream(this.mSocket.socket());
                this.mTheirRelease = this.mProtocol.startSession(this.mSocket, new BufferedInputStream(inputStream), new BufferedOutputStream(outputStream), encrypted, this.mIncoming, this.mIsMasterController, this.mIsSupervisor, localIsGUI, remoteIsGUI);
                this.mRemoteIsGUI = remoteIsGUI[0];
                if (this.mTheirRelease < 0L) {
                    close();
                    Thread.sleep(-this.mTheirRelease);
                    System.exit(0);
                }
            }
            this.encrypted = encrypted;
        } catch (ADHandshakeException e) {
            throw e;
        } catch (Exception e) {
            synchronized (System.class) {
                mLogger.warn("Communicator.init: Exception: " + e);
                mLogger.error(StringToolkit.stackTraceToString(e));
            }
        }
    }

    public void close() throws IOException {
        SocketChannel socket = this.mSocket;
        if (socket != null) {
            DNASocketUtil.closeChannel(socket);
            this.mSocket = null;
        }
    }

    public void stop() {
        String whatami = this.mIncoming ? (this.mIsSupervisor ? "(Supervisor)" : "(MasterController)") : "(Worker)";
        mLogger.debug("Worker.stop: Stopping worker! (" + whatami + ")");
        if (null != this.mReadBlocksThread)
            this.mReadBlocksThread.interrupt();
        if (null != this.mProcessBlocksThread)
            this.mProcessBlocksThread.interrupt();
    }

    public String sendInvoke(String className, String method, String instance, String parameters, boolean wait) {
        String params = (parameters == null) ? "" : ("\n\t<PARAMETERS>\n\t\t" + parameters + "\n\t</PARAMETERS>");
        String xml = "<CLASS>" + className + "</CLASS>" + "\n\t<METHOD>" + method + "</METHOD>" + "\n\t<INSTANCE>" + instance + "</INSTANCE>" + params;
        return sendInvoke(xml, wait);
    }

    public String sendInvoke(String className, String method, String parameters, boolean wait) {
        String xml = "<CLASS>" + className + "</CLASS>" + "\n\t<METHOD>" + method + "</METHOD>" + "\n\t<PARAMETERS>\n\t\t" + parameters + "\n\t</PARAMETERS>";
        return sendInvoke(xml, wait);
    }

    public String sendInvoke(String className, String method, boolean wait) {
        String xml = "<CLASS>" + className + "</CLASS>" + "\n\t<METHOD>" + method + "</METHOD>";
        return sendInvoke(xml, wait);
    }

    public String sendInvoke(String xml, boolean wait) {
        String result = null;
        if (this.mSocket != null && this.mSocket.isConnected()) {
            xml = "<INVOKE id=\"" + ++invokeID + "\">\n\t" + xml + "\n</INVOKE>";
            try {
                Node node = Document.parse(xml, true);
                result = sendBlock(node, wait);
            } catch (SAXException e) {
                mLogger.warn("Communicator.sendInvoke: SAXException: " + e.toString());
            } catch (IOException e) {
                mLogger.warn("Communicator.sendInvoke: IOException: " + e.toString());
                result = "<IO_ERROR/>";
                if (null != this.actionListener)
                    this.actionListener.actionPerformed(new ActionEvent(this, 101, "Connection Reset"));
            } catch (InterruptedException e) {
                mLogger.warn("Communicator.sendInvoke: InterruptedException: " + e.toString());
            } catch (InvalidNodeTypeException e) {
                mLogger.warn("Communicator.sendInvoke: InvalidNodeTypeException: " + e.toString());
            } catch (IllegalBlockingModeException ibme) {
                mLogger.warn("Communicator.sendInvoke: IllegalBlockingModeException: " + ibme.toString());
            }
        } else {
            result = "<NOT_CONNECTED/>";
            if (null != this.actionListener)
                this.actionListener.actionPerformed(new ActionEvent(this, 101, "Connection Reset"));
        }
        return result;
    }

    public void start() throws IOException, SAXException, InterruptedException, InvalidNodeTypeException {
        this.me = this;
        (this.mReadBlocksThread = new Thread("Communicator_ReadBlocksThread") {
            public void run() {
                int tryCount = 0;
                try {
                    Communicator.this.mLastContactTime = new Date();
                    while (!interrupted()) {
                        byte[] block;
                        sleep(50L);
                        try {
                            block = Communicator.this.mProtocol.readBlock(false);
                            tryCount = 0;
                        } catch (SocketException se) {
                            tryCount++;
                            if (tryCount > 2)
                                throw se;
                            synchronized (System.class) {
                                Communicator.mLogger.warn("Communicator.start.readBlocksThread: Protocol.readBlock threw an exception");
                                Communicator.mLogger.error(se.toString());
                                Communicator.mLogger.error("Communicator.start.readBlocksThread: Trying again...");
                            }
                            continue;
                        } catch (ClosedChannelException|IllegalBlockingModeException cce) {
                            Communicator.this.mProcessBlocksThread.interrupt();
                            return;
                        }
                        if (block == null)
                            continue;
                        Communicator.this.mLastContactTime = new Date();
                        Node node = Document.parse(new ByteArrayInputStream(block), true);
                        if (node.getName().equals("INFORMATION"));
                        Node id = node.getAttribute("id");
                        if (id != null) {
                            synchronized (Communicator.this.mWaitedForReceivedBlocks) {
                                if (node.getName().equals("RESULT") && Communicator.this.mWaitedForReceivedBlocks.containsKey(id)) {
                                    Communicator.this.mWaitedForReceivedBlocks.put(id, node);
                                    Communicator.this.mWaitedForReceivedBlocks.notifyAll();
                                    continue;
                                }
                            }
                            synchronized (Communicator.this.mReceivedBlocks) {
                                Communicator.this.mReceivedBlocks.put(id, node);
                                Communicator.this.mReceivedBlocks.notifyAll();
                            }
                            continue;
                        }
                        assert false;
                    }
                } catch (SocketException e) {
                    byte[] block = {0};
                    Communicator.mLogger.warn("Communicator.start.readBlocksThread: SocketException: " + block);
                    if (Communicator.this.actionListener != null)
                        Communicator.this.actionListener.actionPerformed(new ActionEvent(this, 101, "Connection Reset"));
                } catch (Exception e) {
                    synchronized (System.class) {
                        Communicator.mLogger.warn("Communicator.start.readBlocksThread: Exception: " + e);
                        Communicator.mLogger.error(StringToolkit.stackTraceToString(e));
                    }
                }
                Communicator.this.stop();
                try {
                    DNASocketUtil.shutdownOutput(Communicator.this.mSocket.socket());
                    DNASocketUtil.shutdownInput(Communicator.this.mSocket.socket());
                    Communicator.this.mSocket.close();
                    Communicator.this.mSocket = null;
                } catch (Exception e) {
                    Communicator.mLogger.error("Communicator.start.readBlocksThread: Socket.shutdownOutput threw an exception.\n" + e);
                } finally {
                    Communicator.mLogger.error("Communicator.start.Communicator_ReadBlocksThread: Stopping !!!!! ");
                }
            }
        }).start();
        (this.mProcessBlocksThread = new Thread("Communicator_ProcessBlocksThread") {
            public void run() {
                try {
                    while (!interrupted()) {
                        Node block = null;
                        synchronized (Communicator.this.mReceivedBlocks) {
                            Iterator<Node> i = Communicator.this.mReceivedBlocks.keySet().iterator();
                            while (true) {
                                if (i.hasNext()) {
                                    Thread.yield();
                                    Node id = i.next();
                                    if (Communicator.this.mReceivedBlocks.get(id) != null) {
                                        block = Communicator.this.mReceivedBlocks.remove(id);
                                        break;
                                    }
                                    continue;
                                }
                                Communicator.this.mReceivedBlocks.wait(200L);
                                break;
                            }
                        }
                        if (block != null)
                            Communicator.this.processBlock(block, true, false);
                    }
                } catch (InterruptedException interruptedException) {

                } catch (Exception e) {
                    synchronized (System.class) {
                        Communicator.mLogger.warn("Communicator.start.processBlocksThread: Exception: " + e);
                        Communicator.mLogger.error(e.getStackTrace());
                    }
                }
                Communicator.this.stop();
                Communicator.mLogger.trace("Communicator.start.Communicator_ProcessBlocksThread: Stopping !!!!! ");
            }
        }).start();
        this.mPurgeThread = new Thread("Communicator_PurgeThread") {
            public void run() {
                try {
                    while (!interrupted()) {
                        Communicator.this.mSentBlocksPurgeID = new Node(Communicator.nextId());
                        synchronized (Communicator.this.mSentBlocks) {
                            Communicator.this.mSentBlocks.put(Communicator.this.mSentBlocksPurgeID, null);
                        }
                        Communicator.this.mReceivedBlocksPurgeID = new Node(Communicator.nextId());
                        synchronized (Communicator.this.mReceivedBlocks) {
                            Communicator.this.mReceivedBlocks.put(Communicator.this.mReceivedBlocksPurgeID, null);
                        }
                        Communicator.this.mWaitedForReceivedBlocksPurgeID = new Node(Communicator.nextId());
                        synchronized (Communicator.this.mWaitedForReceivedBlocks) {
                            Communicator.this.mWaitedForReceivedBlocks.put(Communicator.this.mWaitedForReceivedBlocksPurgeID, null);
                            Communicator.this.mWaitedForReceivedBlocks.notifyAll();
                        }
                        if (!interrupted())
                            sleep(Communicator.getPurgeOldBlocksPollSleep());
                        synchronized (Communicator.this.mSentBlocks) {
                            SortedMap<Node, Node> headMap = Communicator.this.mSentBlocks.headMap(Communicator.this.mSentBlocksPurgeID);
                            List<Node> blocksToRemove = new ArrayList<>();
                            Iterator<Node> i;
                            for (i = headMap.keySet().iterator(); i.hasNext();)
                                blocksToRemove.add(i.next());
                            for (i = blocksToRemove.iterator(); i.hasNext(); ) {
                                Node blockID = i.next();
                                Communicator.this.mSentBlocks.remove(blockID);
                            }
                            Communicator.this.mSentBlocks.remove(Communicator.this.mSentBlocksPurgeID);
                        }
                        synchronized (Communicator.this.mReceivedBlocks) {
                            SortedMap<Node, Node> headMap = Communicator.this.mReceivedBlocks.headMap(Communicator.this.mReceivedBlocksPurgeID);
                            List<Node> blocksToRemove = new ArrayList<>();
                            Iterator<Node> i;
                            for (i = headMap.keySet().iterator(); i.hasNext();)
                                blocksToRemove.add(i.next());
                            for (i = blocksToRemove.iterator(); i.hasNext(); ) {
                                Node blockID = i.next();
                                Communicator.this.mReceivedBlocks.remove(blockID);
                            }
                            Communicator.this.mReceivedBlocks.remove(Communicator.this.mReceivedBlocksPurgeID);
                        }
                        synchronized (Communicator.this.mWaitedForReceivedBlocks) {
                            SortedMap<Node, Node> headMap = Communicator.this.mWaitedForReceivedBlocks.headMap(Communicator.this.mWaitedForReceivedBlocksPurgeID);
                            List<Node> blocksToRemove = new ArrayList<>();
                            Iterator<Node> i;
                            for (i = headMap.keySet().iterator(); i.hasNext();)
                                blocksToRemove.add(i.next());
                            for (i = blocksToRemove.iterator(); i.hasNext(); ) {
                                Node blockID = i.next();
                                Communicator.this.mWaitedForReceivedBlocks.remove(blockID);
                            }
                            Communicator.this.mWaitedForReceivedBlocks.remove(Communicator.this.mWaitedForReceivedBlocksPurgeID);
                        }
                    }
                } catch (InterruptedException interruptedException) {

                } catch (Exception e) {
                    synchronized (System.class) {
                        Communicator.mLogger.warn("Communicator.start.purgeThread: Exception: " + e);
                        Communicator.mLogger.error(e.getStackTrace());
                    }
                } finally {
                    System.gc();
                    Communicator.mLogger.trace("Communicator.start.Communicator_PurgeThread: Stopping !!!!! ");
                }
            }
        };
        this.mPurgeThread.setDaemon(true);
        this.mPurgeThread.start();
    }

    private static long getPurgeOldBlocksPollSleep() {
        return 86400000L;
    }

    public void addActionListener(ActionListener listener) {
        this.actionListener = listener;
    }

    private static long exchangeReleases(SocketChannel socket) throws IOException {
        byte[] ourRelease = new byte[8];
        Protocol.longToBytes(Worker.getRelease(), ourRelease);
        byte[] theirRelease = new byte[8];
        OutputStream out = DNASocketUtil.getOutputStream(socket.socket());
        out.write(ourRelease);
        out.flush();
        InputStream in = DNASocketUtil.getInputStream(socket.socket());
        int nLen = 8;
        int offset = 0;
        while (nLen > 0) {
            int read = in.read(theirRelease, offset, nLen);
            offset += read;
            nLen -= read;
        }
        return Protocol.bytesToLong(theirRelease);
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public boolean isEncrypted() {
        return this.encrypted;
    }

    public static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = ifaces.nextElement();
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress())
                            return inetAddr;
                        if (candidateAddress == null)
                            candidateAddress = inetAddr;
                    }
                }
            }
            if (candidateAddress != null)
                return candidateAddress;
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null)
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }
}
