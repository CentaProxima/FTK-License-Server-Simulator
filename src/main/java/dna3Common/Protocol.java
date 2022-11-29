package dna3Common;

import ad.io.ByteArraysInputStream;
import ad.lang.StringToolkit;
import ad.shared.xml.Document;
import ad.shared.xml.Node;
import ad.shared.xml.XmlStrings;
import ad.utils.ADLogger;
import ad.utils.Native;
import dna3.Worker;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.xml.sax.SAXException;

public class Protocol {
    private SocketChannel mSocket;

    private InputStream mInputStream;

    private OutputStream mOutputStream;

    private Key mSessionKey;

    private RSAEngine mRSAEncryptCipher = new RSAEngine();

    private RSAEngine mRSADecryptCipher = new RSAEngine();

    private Cipher mAESEncryptCipher;

    private Cipher mAESDecryptCipher;

    private boolean mIsEncrypted;

    private long mTheirRelease;

    private static MultiMonitor mMultiMonitor = new MultiMonitor(5);

    private static SecureRandom mSecureRandom = new SecureRandom();

    private static final int AES_KEY_LENGTH = 16;

    private static ADLogger mLogger = ADLogger.getRootLogger();

    public Protocol() {
        mLogger.trace("Protocol");
        try {
            this.mAESEncryptCipher = Cipher.getInstance("AES/ECB/NoPadding");
            this.mAESDecryptCipher = Cipher.getInstance("AES/ECB/NoPadding");
        } catch (Exception e) {
            synchronized (System.class) {
                mLogger.warn("Protocol.Protocol: Exception: " + e);
                mLogger.debug(StringToolkit.stackTraceToString(e));
            }
        }
    }

    private byte[] processLargeRequest(int length) throws IOException, SAXException {
        mLogger.trace("Protocol.processLargeRequest");
        int initialBlockSize = 524288;
        boolean success = false;
        try {
            byte[] result = new byte[initialBlockSize];
            IO.readFully(this.mInputStream, result);
            String block = new String(result, "UTF-8");
            int fileDataIndex = block.indexOf("<FILE_DATA>");
            if (fileDataIndex == -1) {
                mLogger.debug("Protocol.processLargeRequest: Processing large block.");
                byte[] msgblock = new byte[length];
                ByteArraysInputStream byteArraysInputStream = new ByteArraysInputStream();
                byteArraysInputStream.add(new ByteArrayInputStream(result));
                FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(this.mInputStream, (length - initialBlockSize));
                byteArraysInputStream.add(fixedLengthInputStream);
                IO.readFully((InputStream)byteArraysInputStream, msgblock);
                success = true;
                return msgblock;
            }
            mLogger.debug("Protocol.processLargeRequest: Saving FILE_DATA to file.");
            String fileData = block.substring(fileDataIndex + 11);
            String xml = block.substring(0, fileDataIndex) + "</FILE>\n" + "</PARAMETERS>\n" + "</INVOKE>\n";
            Node root = Document.parse(xml, true);
            Node parametersNode = root.getNode("PARAMETERS");
            Node invokeIdNode = root.getAttribute("id");
            if (invokeIdNode != null) {
                String id = invokeIdNode.getValue();
                try {
                    if (parametersNode != null) {
                        Node fileNode = parametersNode.getNode("FILE");
                        if (fileNode != null) {
                            Node filenameNode = fileNode.getNode("FILENAME");
                            if (filenameNode != null) {
                                String filename = filenameNode.stringFromChild();
                                mLogger.debug("Protocol.processLargeRequest: File: " + filename);
                                ByteArraysInputStream byteArraysInputStream = new ByteArraysInputStream();
                                byteArraysInputStream.add(new ByteArrayInputStream(fileData.getBytes()));
                                FixedLengthInputStream fixedLengthInputStream = new FixedLengthInputStream(this.mInputStream, (length - initialBlockSize));
                                byteArraysInputStream.add(fixedLengthInputStream);
                                FileOutputStream fileOutputStream = new FileOutputStream(filename);
                                IO.base64Decode((InputStream)byteArraysInputStream, fileOutputStream);
                                fileOutputStream.close();
                                if (!filename.startsWith("adtmp!"))
                                    ADFileInputStream.invalidateChecksum(filename);
                                IO.skipFully((InputStream)byteArraysInputStream, fixedLengthInputStream.length());
                                success = true;
                            }
                        }
                    }
                } finally {
                    writeBlock(("<RESULT id=\"" + id + "\">\n" + "<METHOD>processLargeRequest</METHOD>\n" + (success ? "<SUCCESS/>\n" : "<ERROR/>\n<FAILURE/>\n") + "</RESULT>\n")
                            .getBytes("UTF-8"));
                }
            }
        } finally {
            if (!success)
                IO.skipFully(this.mInputStream, (length - initialBlockSize));
        }
        return null;
    }

    public byte[] readBlock(boolean isWorker) throws IOException, SAXException {
        synchronized (this) {
            int length, timeOut = DNASocketUtil.getSoTimeout(this.mSocket.socket());
            if (!isWorker)
                DNASocketUtil.setSoTimout(this.mSocket.socket(), 50);
            try {
                length = readInteger(this.mInputStream);
                if (length > 1048576)
                    mLogger.debug("Protocol.readBlock: readInteger read... = " + Integer.toString(length));
            } catch (SocketTimeoutException se) {
                return null;
            } finally {
                DNASocketUtil.setSoTimout(this.mSocket.socket(), timeOut);
            }
            if (length <= -1) {
                endSession();
                throw new IOException("Protocol.readBlock: End of Session!");
            }
            if (length > 1048576)
                return processLargeRequest(length);
            byte[] returnValue = new byte[length];
            IO.readFully(this.mInputStream, returnValue);
            return returnValue;
        }
    }

    public void writeBlock(byte[] block) throws IOException, IllegalBlockingModeException {
        synchronized (this) {
            writeInteger(this.mOutputStream, block.length);
            this.mOutputStream.write(block);
            this.mOutputStream.flush();
        }
    }

    public void writeFile(File file, int id) throws IOException {
        mLogger.trace("Protocol.writeFile");
        try {
            String prefix = "<INVOKE id=\"" + id + "\">\n" + "\t<CLASS>dna3.Worker</CLASS>\n" + "\t<METHOD>setFile</METHOD>\n" + "\t<PARAMETERS>\n" + "\t\t<FILE>\n" + "\t\t\t<FILENAME>" + XmlStrings.escapeString(file.getName()) + "</FILENAME>\n" + "\t\t\t<DICTIONARY/>\n" + "\t\t\t<FILE_DATA>\n";
            String postfix = "\t\t\t</FILE_DATA>\n\t\t</FILE>\n\t</PARAMETERS>\n</INVOKE>\n";
            int length = (int)(prefix.length() + IO.base64EncodeLength(file) + postfix.length());
            mMultiMonitor.acquireMonitor();
            writeInteger(this.mOutputStream, length);
            byte[] bytes = prefix.getBytes();
            this.mOutputStream.write(bytes);
            IO.base64Encode(this.mOutputStream, file);
            bytes = postfix.getBytes();
            this.mOutputStream.write(bytes);
        } catch (IOException e) {
            throw e;
        } finally {
            mMultiMonitor.releaseMonitor();
        }
    }

    protected void flush() throws IOException {
        mLogger.trace("Protocol.flush");
        this.mOutputStream.flush();
    }

    private void writeRSA(byte[] plaintext, int offset, int length) throws IOException, DataLengthException {
        mLogger.trace("Protocol.writeRSA");
        assert length > 0;
        assert length < 127;
        byte[] buffer = new byte[127];
        System.arraycopy(plaintext, offset, buffer, 1, length);
        buffer[0] = (byte)length;
        byte[] ciphertext = this.mRSAEncryptCipher.processBlock(buffer, 0, buffer.length);
        this.mOutputStream.write(ciphertext);
        for (int i = ciphertext.length; i < 128; i++)
            this.mOutputStream.write(0);
    }

    private void writeRSA(byte[] data) throws IOException, GeneralSecurityException, DataLengthException {
        mLogger.trace("Protocol.writeRSA");
        writeRSA(data, 0, data.length);
    }

    private void readRSA(byte[] plaintext, int offset, int length) throws IOException, GeneralSecurityException, DataLengthException {
        mLogger.trace("Protocol.readRSA");
        assert length <= 127;
        byte[] ciphertext = IO.readFully(this.mInputStream, 128);
        assert ciphertext.length == 128;
        ciphertext = this.mRSADecryptCipher.processBlock(ciphertext, 0, ciphertext.length);
        System.arraycopy(ciphertext, 1, plaintext, offset, length);
    }

    private void readRSA(byte[] data) throws IOException, GeneralSecurityException, DataLengthException {
        mLogger.trace("Protocol.readRSA");
        readRSA(data, 0, data.length);
    }

    private void write(byte[] data, int offset, int length, boolean encrypted) throws IOException, GeneralSecurityException, DataLengthException {
        mLogger.trace("Protocol.write");
        if (encrypted) {
            writeRSA(data, offset, length);
        } else {
            this.mOutputStream.write(data, offset, length);
        }
    }

    private void write(byte[] data, boolean encrypted) throws IOException, GeneralSecurityException, DataLengthException {
        mLogger.trace("Protocol.write");
        write(data, 0, data.length, encrypted);
    }

    private void read(byte[] data, int offset, int length, boolean encrypted) throws IOException, GeneralSecurityException, DataLengthException {
        mLogger.trace("Protocol.read");
        if (encrypted) {
            readRSA(data, offset, length);
        } else {
            IO.readFully(this.mInputStream, data, offset, length);
        }
    }

    private void read(byte[] data, boolean encrypted) throws IOException, GeneralSecurityException, DataLengthException {
        mLogger.trace("Protocol.read");
        read(data, 0, data.length, encrypted);
    }

    protected synchronized void handshake(boolean encrypted) throws IOException, ADHandshakeException, GeneralSecurityException, DataLengthException {
        mLogger.trace("Protocol.handshake");
        byte[] bytes = new byte[4];
        mSecureRandom.nextBytes(bytes);
        int number = bytesToInteger(bytes);
        write(bytes, encrypted);
        this.mOutputStream.flush();
        read(bytes, encrypted);
        integerToBytes(bytesToInteger(bytes) + 1, bytes);
        write(bytes, encrypted);
        this.mOutputStream.flush();
        read(bytes, encrypted);
        if (bytesToInteger(bytes) != number + 1)
            throw new ADHandshakeException("handshake : failure invalid handshake : " + bytesToInteger(bytes) + " != " + (number + 1));
    }

    protected synchronized void exchangeSessionKey(boolean encrypted) throws IOException, GeneralSecurityException, DataLengthException {
        mLogger.trace("Protocol.exchangeSessionKey");
        byte[] lowKey = new byte[8];
        byte[] hiKey = new byte[8];
        mSecureRandom.nextBytes(lowKey);
        write(lowKey, 0, 8, encrypted);
        this.mOutputStream.flush();
        read(hiKey, 0, 8, encrypted);
        byte[] key = new byte[16];
        if (bytesToInteger(lowKey) > bytesToInteger(hiKey)) {
            System.arraycopy(hiKey, 0, key, 0, 8);
            System.arraycopy(lowKey, 0, key, 8, 8);
        } else {
            System.arraycopy(lowKey, 0, key, 0, 8);
            System.arraycopy(hiKey, 0, key, 8, 8);
        }
        this.mSessionKey = new SecretKeySpec(key, "AES");
    }

    protected long exchangeReleases(boolean encrypted) throws IOException, GeneralSecurityException, DataLengthException {
        mLogger.trace("Protocol.exchangeReleases");
        byte[] ourRelease = new byte[8];
        longToBytes(Worker.getRelease(), ourRelease);
        byte[] theirRelease = new byte[8];
        write(ourRelease, encrypted);
        flush();
        read(theirRelease, encrypted);
        return this.mTheirRelease = bytesToLong(theirRelease);
    }

    protected boolean negotiateEncryption(boolean encrypted, boolean incoming, boolean isMasterController, boolean isSupervisor) throws IOException, ADHandshakeException, GeneralSecurityException, DataLengthException, SAXException {
        mLogger.trace("Protocol.negotiateEncryption");
        String rootpath = Native.getRootPath() + File.separator;
        if (encrypted)
            if (isMasterController) {
                if (incoming) {
                    InitializationFile initializationFile = InitializationFile.open(rootpath + "mastercontroller.ini");
                    encrypted = (initializationFile.getModulus() != null && initializationFile.getPrivateExponent() != null);
                } else {
                    assert false;
                }
            } else if (isSupervisor) {
                if (incoming) {
                    InitializationFile initializationFile = InitializationFile.open(rootpath + "supervisor.ini");
                    encrypted = initializationFile.getEncryption();
                } else {
                    InitializationFile initializationFile = null;
                    try {
                        initializationFile = InitializationFile.open(rootpath + "mastercontroller.ini");
                    } catch (IOException exception) {
                        downloadPublicKey(DNASocketUtil.getChannelInetAddress(this.mSocket), DNASocketUtil.getChannelPort(this.mSocket), true);
                        try {
                            initializationFile = InitializationFile.open(rootpath + "mastercontroller.ini");
                        } catch (IOException e) {
                            mLogger.warn("Protocol.negotiateEncryption: IOException: " + e);
                            mLogger.debug(StringToolkit.stackTraceToString(e));
                        }
                    }
                    encrypted = (initializationFile != null && initializationFile.getModulus() != null && initializationFile.getPublicExponent() != null);
                }
            } else if (incoming) {
                assert false;
            } else {
                InitializationFile initializationFile = null;
                try {
                    initializationFile = InitializationFile.open(rootpath + "worker.ini");
                } catch (IOException e1) {
                    try {
                        initializationFile = InitializationFile.open(rootpath + "supervisor.ini");
                    } catch (IOException e2) {
                        try {
                            initializationFile = InitializationFile.open(rootpath + "mastercontroller.ini");
                        } catch (IOException e3) {
                            downloadPublicKey(DNASocketUtil.getChannelInetAddress(this.mSocket), DNASocketUtil.getChannelPort(this.mSocket), false);
                            try {
                                initializationFile = InitializationFile.open(rootpath + "supervisor.ini");
                            } catch (IOException e4) {
                                mLogger.warn("Protocol.negotiateEncryption: IOException: " + e4);
                                mLogger.debug(StringToolkit.stackTraceToString(e4));
                            }
                        }
                    }
                }
                encrypted = (initializationFile != null && initializationFile.getModulus() != null && initializationFile.getPublicExponent() != null);
            }
        byte[] encryption = new byte[1];
        encryption[0] = encrypted ? (byte)1 : (byte)0;
        write(encryption, false);
        flush();
        read(encryption, false);
        synchronized (System.class) {
            mLogger.debug("Protocol.negotiateEncryption: Our side " + encrypted);
            mLogger.debug("Protocol.negotiateEncryption: Other side " + ((encryption[0] == 1) ? 1 : 0));
        }
        return (encrypted && encryption[0] == 1);
    }

    protected boolean exchangeIsGUI(boolean localIsGUI) throws IOException {
        mLogger.trace("Protocol.exchangeIsGUI");
        this.mOutputStream.write(localIsGUI ? 1 : 0);
        this.mOutputStream.flush();
        return (this.mInputStream.read() == 1);
    }

    private static int readInteger(InputStream is) throws IOException {
        byte[] buffer = new byte[4];
        IO.readFully(is, buffer);
        return bytesToInteger(buffer);
    }

    private static void writeInteger(OutputStream os, int value) throws IOException {
        byte[] buffer = new byte[4];
        integerToBytes(value, buffer);
        os.write(buffer);
    }

    public static int bytesToInteger(byte[] buffer) {
        return buffer[0] << 24 | (0xFF & buffer[1]) << 16 | (0xFF & buffer[2]) << 8 | 0xFF & buffer[3];
    }

    public static long bytesToLong(byte[] buffer) {
        return (buffer[0] << 56 | (0xFF & buffer[1]) << 48 | (0xFF & buffer[2]) << 40 | (0xFF & buffer[3]) << 32 | (0xFF & buffer[4]) << 24 | (0xFF & buffer[5]) << 16 | (0xFF & buffer[6]) << 8 | 0xFF & buffer[7]);
    }

    public static void integerToBytes(int value, byte[] buffer) {
        buffer[0] = (byte)(value >>> 24);
        buffer[1] = (byte)(value >>> 16);
        buffer[2] = (byte)(value >>> 8);
        buffer[3] = (byte)value;
    }

    public static void longToBytes(long value, byte[] buffer) {
        buffer[0] = (byte)(int)(value >>> 56L);
        buffer[1] = (byte)(int)(value >>> 48L);
        buffer[2] = (byte)(int)(value >>> 40L);
        buffer[3] = (byte)(int)(value >>> 32L);
        buffer[4] = (byte)(int)(value >>> 24L);
        buffer[5] = (byte)(int)(value >>> 16L);
        buffer[6] = (byte)(int)(value >>> 8L);
        buffer[7] = (byte)(int)value;
    }

    public long startSession(SocketChannel socket, InputStream is, OutputStream os, boolean encrypted, boolean incoming, boolean isMasterController, boolean isSupervisor, boolean localIsGUI, boolean[] remoteIsGUI) throws IOException, ADHandshakeException, GeneralSecurityException, DataLengthException, SAXException {
        this.mSocket = socket;
        this.mInputStream = is;
        this.mOutputStream = os;
        long theirRelease = 0L;
        if (negotiateEncryption(encrypted, incoming, isMasterController, isSupervisor)) {
            mLogger.debug("Protocol.startSession: Going encrypted!");
            this.mIsEncrypted = true;
            initializeRSA(incoming, isMasterController, isSupervisor);
            handshake(true);
            exchangeSessionKey(true);
            this.mAESEncryptCipher.init(1, this.mSessionKey);
            this.mAESDecryptCipher.init(2, this.mSessionKey);
            this.mInputStream = new IO.CipherInputStream(is, this.mAESDecryptCipher);
            this.mOutputStream = new IO.CipherOutputStream(os, this.mAESEncryptCipher);
        } else {
            this.mIsEncrypted = false;
            mLogger.debug("Protocol.startSession: Going unencrypted!");
        }
        boolean remoteSideIsGUI = exchangeIsGUI(localIsGUI);
        if (remoteIsGUI != null)
            remoteIsGUI[0] = remoteSideIsGUI;
        return theirRelease;
    }

    protected void endSession() throws IOException {
        writeInteger(this.mOutputStream, -1);
        this.mOutputStream.flush();
        if (this.mInputStream instanceof IO.CipherInputStream)
            this.mInputStream = ((IO.CipherInputStream)this.mInputStream).getInputStream();
        if (this.mOutputStream instanceof IO.CipherOutputStream)
            this.mOutputStream = ((IO.CipherOutputStream)this.mOutputStream).getOutputStream();
    }

    public static void downloadPublicKey(String inetAddress, int port, boolean getMasterControllerKey) throws IOException, ADHandshakeException, GeneralSecurityException, DataLengthException, SAXException, UnknownHostException {
        downloadPublicKey(InetAddress.getByName(inetAddress), port, getMasterControllerKey);
    }

    public SocketChannel getSocket() {
        return this.mSocket;
    }

    public boolean isEncrypted() {
        return this.mIsEncrypted;
    }

    public Cipher getDecryptCipher() {
        return this.mAESDecryptCipher;
    }

    public Cipher getEncryptCipher() {
        return this.mAESEncryptCipher;
    }

    public static void downloadPublicKey(InetAddress inetAddress, int port, boolean getMasterControllerKey) throws IOException, ADHandshakeException, GeneralSecurityException, DataLengthException, SAXException {
        Node root;
        SocketChannel socket = DNASocketUtil.getSocketChannel(inetAddress, port);
        Protocol protocol = new Protocol();
        protocol.startSession(socket, DNASocketUtil.getInputStream(socket.socket()), DNASocketUtil.getOutputStream(socket.socket()), false, false, false, getMasterControllerKey, false, null);
        protocol.writeBlock(("<INVOKE id='1'>\n\t<CLASS>dna3.Worker</CLASS>\n\t<METHOD>getPublicKey</METHOD>\n\t<PARAMETERS>\n" + (getMasterControllerKey ? "\t\t<MASTER_CONTROLLER/>\n" : "\t\t<SUPERVISOR/>") + "\t</PARAMETERS>\n" + "</INVOKE>\n")
                .getBytes("UTF-8"));
        do {
            byte[] block = protocol.readBlock(true);
            root = Document.parse(new ByteArrayInputStream(block), true);
        } while (!root.getName().equals("RESULT"));
        DNASocketUtil.closeChannel(socket);
        Node node = root.getNode(getMasterControllerKey ? "MASTER_CONTROLLER" : "SUPERVISOR");
        if (node != null) {
            String rootpath = Native.getRootPath() + File.separator;
            Node modulusNode = node.getNode("MODULUS");
            Node publicExponentNode = node.getNode("PUBLIC_EXPONENT");
            String filename = rootpath + (getMasterControllerKey ? "mastercontroller.ini" : "supervisor.ini");
            ADFileInputStream.invalidateChecksum(filename);
            PrintStream printstream = new PrintStream(new FileOutputStream(filename));
            printstream.println("Modulus=" + modulusNode.stringFromChild());
            printstream.println("PublicExponent=" + publicExponentNode.stringFromChild());
            printstream.close();
        }
    }

    private void initializeRSA(boolean incoming, boolean isMasterController, boolean isSupervisor) throws IOException, ADHandshakeException, GeneralSecurityException, DataLengthException, SAXException {
        String rootpath = Native.getRootPath() + File.separator;
        if (isMasterController) {
            if (incoming) {
                InitializationFile initializationFile = InitializationFile.open(rootpath + "mastercontroller.ini");
                RSAPrivateKeySpec key = new RSAPrivateKeySpec(initializationFile.getModulus(), initializationFile.getPrivateExponent());
                this.mRSAEncryptCipher.init(true, new RSAKeyParameters(true, key.getModulus(), key.getPrivateExponent()));
                this.mRSADecryptCipher.init(false, new RSAKeyParameters(false, key.getModulus(), key.getPrivateExponent()));
            } else {
                assert false;
            }
        } else if (isSupervisor) {
            if (incoming) {
                InitializationFile initializationFile = InitializationFile.open(rootpath + "supervisor.ini");
                RSAPrivateKeySpec key = new RSAPrivateKeySpec(initializationFile.getModulus(), initializationFile.getPrivateExponent());
                RSAKeyParameters rsaParameters = new RSAKeyParameters(true, key.getModulus(), key.getPrivateExponent());
                this.mRSAEncryptCipher.init(true, new RSAKeyParameters(true, key.getModulus(), key.getPrivateExponent()));
                this.mRSADecryptCipher.init(false, new RSAKeyParameters(false, key.getModulus(), key.getPrivateExponent()));
            } else {
                InitializationFile initializationFile;
                try {
                    initializationFile = InitializationFile.open(rootpath + "mastercontroller.ini");
                } catch (IOException e) {
                    downloadPublicKey(DNASocketUtil.getChannelInetAddress(this.mSocket), DNASocketUtil.getChannelPort(this.mSocket), true);
                    initializationFile = InitializationFile.open(rootpath + "mastercontroller.ini");
                }
                RSAPublicKeySpec key = new RSAPublicKeySpec(initializationFile.getModulus(), initializationFile.getPublicExponent());
                this.mRSAEncryptCipher.init(true, new RSAKeyParameters(true, key.getModulus(), key.getPublicExponent()));
                this.mRSADecryptCipher.init(false, new RSAKeyParameters(false, key.getModulus(), key.getPublicExponent()));
            }
        } else if (incoming) {
            assert false;
        } else {
            InitializationFile initializationFile;
            try {
                initializationFile = InitializationFile.open(rootpath + "worker.ini");
            } catch (IOException e1) {
                try {
                    initializationFile = InitializationFile.open(rootpath + "supervisor.ini");
                } catch (IOException e2) {
                    try {
                        initializationFile = InitializationFile.open(rootpath + "mastercontroller.ini");
                    } catch (IOException e3) {
                        downloadPublicKey(DNASocketUtil.getChannelInetAddress(this.mSocket), DNASocketUtil.getChannelPort(this.mSocket), false);
                        initializationFile = InitializationFile.open(rootpath + "supervisor.ini");
                    }
                }
            }
            RSAPublicKeySpec key = new RSAPublicKeySpec(initializationFile.getModulus(), initializationFile.getPublicExponent());
            this.mRSAEncryptCipher.init(true, new RSAKeyParameters(true, key.getModulus(), key.getPublicExponent()));
            this.mRSADecryptCipher.init(false, new RSAKeyParameters(false, key.getModulus(), key.getPublicExponent()));
        }
    }

    public void acquireMultiMonitor() {
        mMultiMonitor.acquireMonitor();
    }

    public void releaseMultiMonitor() {
        mMultiMonitor.releaseMonitor();
    }
}
