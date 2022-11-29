package dna3Common;

import ad.utils.ADLogger;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class IO {
    private static ADLogger mLogger = ADLogger.getRootLogger();

    private static char[] base64EncodingTable = new char[] {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
            'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
            'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', '+', '/' };

    private static byte[] base64DecodingTable = new byte[] {
            Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE,
            Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE,
            Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE,
            Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE,
            Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, 62, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, 63, 52, 53,
            54, 55, 56, 57, 58, 59, 60, 61, Byte.MIN_VALUE, Byte.MIN_VALUE,
            Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, 0, 1, 2, 3, 4,
            5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
            25, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, 26, 27, 28,
            29, 30, 31, 32, 33, 34, 35, 36, 37, 38,
            39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
            49, 50, 51, Byte.MIN_VALUE };

    private static final int maximumCharactersOnLine = 72;

    public static String indent(int depth) {
        String i = "";
        while (depth-- > 0)
            i = i + '\t';
        return i;
    }

    public static void readFully(InputStream is, byte[] buffer) throws IOException {
        readFully(is, buffer, 0, buffer.length);
    }

    public static void skipFully(InputStream is, long n) throws IOException {
        while (n > 0L) {
            long result = is.skip(n);
            if (result >= 0L) {
                n -= result;
                continue;
            }
            assert false;
        }
    }

    public static void readFully(InputStream is, byte[] buffer, int offset, int length) throws IOException {
        while (length > 0) {
            int readReturnValue = is.read(buffer, offset, length);
            if (readReturnValue == 0)
                mLogger.trace("IO.readFully: Return value from is.read is 0");
            if (readReturnValue == -1) {
                mLogger.trace("IO.readFully: Return value from is.read is -1");
                throw new IOException("IO.readFully: return value from is.read is -1");
            }
            offset += readReturnValue;
            length -= readReturnValue;
        }
    }

    public static byte[] readFully(InputStream is, int length) throws IOException {
        byte[] buffer = new byte[length];
        readFully(is, buffer);
        return buffer;
    }

    public static byte[] readFile(File file, String checksum) throws IOException, NoSuchAlgorithmException {
        assert file.length() < 2147483647L;
        assert file.length() >= 0L;
        byte[] data = new byte[(int)file.length()];
        ADFileInputStream fileInputStream = new ADFileInputStream(file, checksum);
        readFully(fileInputStream, data);
        fileInputStream.close();
        return data;
    }

    public static byte[] readFile(String filename, String checksum) throws IOException, NoSuchAlgorithmException {
        return readFile(new File(filename), checksum);
    }

    public static byte[] readFile(File file) throws IOException, NoSuchAlgorithmException {
        return readFile(file, (String)null);
    }

    public static byte[] readFile(String filename) throws IOException, NoSuchAlgorithmException {
        return readFile(filename, (String)null);
    }

    public static void writeFile(String filename, byte[] data) throws IOException {
        writeFile(new File(filename), data, 0, data.length);
    }

    public static void writeFile(File file, byte[] data) throws IOException {
        writeFile(file, data, 0, data.length);
    }

    public static void writeFile(File file, String data) throws IOException {
        writeFile(file, data.getBytes("UTF-8"));
    }

    public static void writeFile(String filename, String data) throws IOException {
        writeFile(new File(filename), data.getBytes("UTF-8"));
    }

    public static void writeFile(String filename, byte[] data, int offset, int length) throws IOException {
        writeFile(new File(filename), data, offset, length);
    }

    public static void writeFile(File file, byte[] data, int offset, int length) throws IOException {
        ADFileInputStream.invalidateChecksum(file);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(data, offset, length);
        fileOutputStream.close();
    }

    public static String base64Encode(byte[] bytes) {
        return base64Encode(bytes, 0, bytes.length);
    }

    public static long base64EncodeLength(File file) {
        long length = file.length();
        return (length + 2L) / 3L * 4L + ((length + 2L) / 3L * 4L + 72L - 1L) / 72L;
    }

    public static void base64Encode(OutputStream outputStream, File file) throws FileNotFoundException, IOException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        int charactersOnLine = 0;
        int index = 0;
        byte[] bytes = new byte[3];
        long length = file.length();
        long encodedLength = (length + 2L) / 3L * 4L;
        for (index = 0; (index + 2) < length; index += 3) {
            inputStream.read(bytes);
            outputStream.write(base64EncodingTable[0x3F & bytes[0] >>> 2]);
            outputStream.write(base64EncodingTable[0x3F & (bytes[0] << 4 | (0xFF & bytes[1]) >>> 4)]);
            outputStream.write(base64EncodingTable[0x3F & (bytes[1] << 2 | (0xFF & bytes[2]) >>> 6)]);
            outputStream.write(base64EncodingTable[0x3F & bytes[2]]);
            charactersOnLine += 4;
            if (charactersOnLine >= 72) {
                outputStream.write(10);
                charactersOnLine = 0;
            }
        }
        if ((index + 1) == length) {
            inputStream.read(bytes, 0, 1);
            outputStream.write(base64EncodingTable[0x3F & bytes[0] >>> 2]);
            outputStream.write(base64EncodingTable[0x3F & bytes[0] << 4]);
            outputStream.write(61);
            outputStream.write(61);
            charactersOnLine += 4;
            if (charactersOnLine >= 72) {
                outputStream.write(10);
                charactersOnLine = 0;
            }
        }
        if ((index + 2) == length) {
            inputStream.read(bytes, 0, 2);
            outputStream.write(base64EncodingTable[0x3F & bytes[0] >>> 2]);
            outputStream.write(base64EncodingTable[0x3F & (bytes[0] << 4 | (0xFF & bytes[1]) >>> 4)]);
            outputStream.write(base64EncodingTable[0x3F & bytes[1] << 2]);
            outputStream.write(61);
            charactersOnLine += 4;
            if (charactersOnLine >= 72)
                outputStream.write(10);
        }
        if (encodedLength % 72L != 0L)
            outputStream.write(10);
        inputStream.close();
    }

    public static String base64Encode(byte[] bytes, int offset, int length) {
        int encodedLength = (length + 2) / 3 * 4;
        int numberOfLines = (encodedLength + 72 - 1) / 72;
        char[] returnValue = new char[encodedLength + numberOfLines];
        int charactersOnLine = 0;
        int returnValueIndex = 0;
        int index;
        for (index = offset; index + 2 < length + offset; index += 3) {
            returnValue[returnValueIndex++] = base64EncodingTable[0x3F & bytes[index] >>> 2];
            returnValue[returnValueIndex++] = base64EncodingTable[0x3F & (bytes[index] << 4 | (0xFF & bytes[index + 1]) >>> 4)];
            returnValue[returnValueIndex++] = base64EncodingTable[0x3F & (bytes[index + 1] << 2 | (0xFF & bytes[index + 2]) >>> 6)];
            returnValue[returnValueIndex++] = base64EncodingTable[0x3F & bytes[index + 2]];
            charactersOnLine += 4;
            if (charactersOnLine >= 72) {
                returnValue[returnValueIndex++] = '\n';
                charactersOnLine = 0;
            }
        }
        if (index + 1 == length) {
            returnValue[returnValueIndex++] = base64EncodingTable[0x3F & bytes[index] >>> 2];
            returnValue[returnValueIndex++] = base64EncodingTable[0x3F & bytes[index] << 4];
            returnValue[returnValueIndex++] = '=';
            returnValue[returnValueIndex++] = '=';
            charactersOnLine += 4;
            if (charactersOnLine >= 72) {
                returnValue[returnValueIndex++] = '\n';
                charactersOnLine = 0;
            }
        }
        if (index + 2 == length) {
            returnValue[returnValueIndex++] = base64EncodingTable[0x3F & bytes[index] >>> 2];
            returnValue[returnValueIndex++] = base64EncodingTable[0x3F & (bytes[index] << 4 | (0xFF & bytes[index + 1]) >>> 4)];
            returnValue[returnValueIndex++] = base64EncodingTable[0x3F & bytes[index + 1] << 2];
            returnValue[returnValueIndex++] = '=';
            charactersOnLine += 4;
            if (charactersOnLine >= 72)
                returnValue[returnValueIndex++] = '\n';
        }
        if (encodedLength % 72 != 0)
            returnValue[returnValueIndex++] = '\n';
        assert returnValueIndex == encodedLength + numberOfLines;
        return new String(returnValue);
    }

    public static byte[] base64Decode(byte[] data) throws IOException {
        return base64Decode(data, 0, data.length);
    }

    static byte[] base64Decode(byte[] data, int dataOffset, int len) throws IOException {
        byte[] bytes = new byte[(len * 3 + 2) / 4];
        int index = 0;
        byte[] buffer = new byte[4];
        int[] length = new int[1];
        length[0] = len;
        while (length[0] > 0) {
            int lengthBefore = length[0];
            switch (fillBuffer(data, dataOffset, buffer, 0, length)) {
                case 1:
                    throw new IOException("IO.base64Decode: fillBuffer failure : expected different data");
                case 2:
                    bytes[index++] = (byte)(buffer[0] << 2 | buffer[1] >>> 4);
                    break;
                case 3:
                    bytes[index++] = (byte)(buffer[0] << 2 | buffer[1] >>> 4);
                    bytes[index++] = (byte)(buffer[1] << 4 | buffer[2] >>> 2);
                    break;
                case 4:
                    bytes[index++] = (byte)(buffer[0] << 2 | buffer[1] >>> 4);
                    bytes[index++] = (byte)(buffer[1] << 4 | buffer[2] >>> 2);
                    bytes[index++] = (byte)(buffer[2] << 6 | buffer[3]);
                    break;
            }
            dataOffset += lengthBefore - length[0];
        }
        byte[] returnValue = new byte[index];
        System.arraycopy(bytes, 0, returnValue, 0, index);
        return returnValue;
    }

    static void base64Decode(InputStream is, OutputStream os) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(os);
        byte[] buffer = new byte[4];
        boolean looping = true;
        while (looping) {
            switch (fillBuffer(is, buffer)) {
                case 0:
                    looping = false;
                case 1:
                    throw new IOException("IO.base64Decode: fillBuffer failure : expected different data");
                case 2:
                    bos.write(buffer[0] << 2 | buffer[1] >>> 4);
                    looping = false;
                case 3:
                    bos.write(buffer[0] << 2 | buffer[1] >>> 4);
                    bos.write(buffer[1] << 4 | buffer[2] >>> 2);
                    looping = false;
                case 4:
                    bos.write(buffer[0] << 2 | buffer[1] >>> 4);
                    bos.write(buffer[1] << 4 | buffer[2] >>> 2);
                    bos.write(buffer[2] << 6 | buffer[3]);
            }
        }
        bos.flush();
    }

    static int fillBuffer(InputStream is, byte[] buffer) throws IOException {
        int count = 0;
        while (count < 4) {
            int b = is.read();
            if (b == -1)
                break;
            if ((b >= 65 && b <= 90) || (b >= 97 && b <= 122) || (b >= 48 && b <= 57) || b == 43 || b == 47) {
                buffer[count] = base64DecodingTable[0xFF & b];
                count++;
                continue;
            }
            if (b == 61 || b == 60)
                break;
        }
        return count;
    }

    static int fillBuffer(byte[] data, int dataOffset, byte[] buffer, int bufferOffset, int[] length) {
        int count = 0;
        while (count < 4) {
            if (length[0] == 0)
                break;
            if ((data[dataOffset] >= 65 && data[dataOffset] <= 90) || (data[dataOffset] >= 97 && data[dataOffset] <= 122) || (data[dataOffset] >= 48 && data[dataOffset] <= 57) || data[dataOffset] == 43 || data[dataOffset] == 47) {
                buffer[bufferOffset] = base64DecodingTable[0xFF & data[dataOffset]];
                bufferOffset++;
                count++;
            } else if (data[dataOffset] == 61) {
                length[0] = 0;
                break;
            }
            dataOffset++;
            length[0] = length[0] - 1;
        }
        return count;
    }

    static class CipherInputStream extends InputStream {
        private InputStream mInputStream;

        private Cipher mCipher;

        private ByteBuffer mByteBuffer = ByteBuffer.allocate(16);

        public CipherInputStream(InputStream s, Cipher cipher) {
            this.mInputStream = s;
            this.mCipher = cipher;
            this.mByteBuffer.limit(0);
        }

        public int read() throws IOException {
            if (this.mByteBuffer.position() >= this.mByteBuffer.limit())
                underflow();
            return this.mByteBuffer.get();
        }

        public int read(byte[] data, int offset, int length) throws IOException {
            if (length > 0) {
                int remaining = this.mByteBuffer.remaining();
                if (remaining == 0) {
                    underflow();
                    remaining = this.mByteBuffer.remaining();
                }
                if (length <= remaining) {
                    this.mByteBuffer.get(data, offset, length);
                    return length;
                }
                this.mByteBuffer.get(data, offset, remaining);
                return remaining;
            }
            return 0;
        }

        public InputStream getInputStream() {
            return this.mInputStream;
        }

        private void underflow() throws IOException {
            try {
                IO.readFully(this.mInputStream, this.mByteBuffer.array());
                int returnValue = this.mCipher.doFinal(this.mByteBuffer.array(), 0, 16, this.mByteBuffer.array());
                assert returnValue == 16;
                this.mByteBuffer.limit(1);
                byte length = this.mByteBuffer.get(0);
                if (length < 0 || length >= 16)
                    throw new IOException();
                this.mByteBuffer.limit(length + 1);
                this.mByteBuffer.position(1);
            } catch (GeneralSecurityException exception) {
                throw new IOException(exception.toString());
            }
        }
    }

    static class CipherOutputStream extends OutputStream {
        private OutputStream mOutputStream;

        private Cipher mCipher;

        private ByteBuffer mByteBuffer = ByteBuffer.allocate(16);

        public CipherOutputStream(OutputStream s, Cipher cipher) {
            this.mOutputStream = s;
            this.mCipher = cipher;
            this.mByteBuffer.position(1);
            this.mByteBuffer.limit(this.mByteBuffer.capacity());
        }

        public void write(int b) throws IOException {
            if (!this.mByteBuffer.hasRemaining())
                overflow();
            this.mByteBuffer.put((byte)b);
        }

        public void write(byte[] data, int offset, int length) throws IOException {
            while (length > 0) {
                int remaining = this.mByteBuffer.remaining();
                if (length <= remaining) {
                    this.mByteBuffer.put(data, offset, length);
                    return;
                }
                if (remaining > 0) {
                    this.mByteBuffer.put(data, offset, remaining);
                    offset += remaining;
                    length -= remaining;
                }
                overflow();
            }
        }

        public void close() throws IOException {
            flush();
        }

        public void flush() throws IOException {
            overflow();
            this.mOutputStream.flush();
        }

        public OutputStream getOutputStream() {
            return this.mOutputStream;
        }

        private void overflow() throws IOException {
            if (this.mByteBuffer.position() > 1)
                try {
                    assert this.mByteBuffer.position() > 0 && this.mByteBuffer.position() <= 16;
                    this.mByteBuffer.put(0, (byte)(this.mByteBuffer.position() - 1));
                    int returnValue = this.mCipher.doFinal(this.mByteBuffer.array(), 0, 16, this.mByteBuffer.array());
                    assert returnValue == 16;
                    this.mOutputStream.write(this.mByteBuffer.array());
                    this.mByteBuffer.position(1);
                } catch (GeneralSecurityException exception) {
                    throw new IOException(exception.toString());
                }
        }
    }

    public static String toHex(byte[] buffer) {
        String returnValue = "";
        for (int i = 0; i < buffer.length; i++)
            returnValue = returnValue + pretty(Integer.toHexString(buffer[i]));
        return returnValue;
    }

    private static String pretty(String in) {
        if (in.length() > 2)
            return in.substring(in.length() - 2);
        if (in.length() == 0)
            return "00";
        if (in.length() == 1)
            return "0" + in;
        return in;
    }

    public static int hash(Object o) {
        if (o instanceof Integer)
            return o.hashCode();
        return o.toString().hashCode();
    }

    public static Object[] split(String s, String match) {
        Set<Character> set = new HashSet();
        for (int i = 0; i < match.length(); i++)
            set.add(new Character(match.charAt(i)));
        int position = 0;
        int lastPosition = 0;
        List<String> ll = new LinkedList();
        while (position <= s.length()) {
            while (position == s.length() || (position < s.length() && set.contains(new Character(s.charAt(position))))) {
                if (lastPosition != position) {
                    ll.add(s.substring(lastPosition, position));
                    lastPosition = position;
                }
                position++;
                lastPosition++;
            }
            position++;
        }
        return ll.toArray();
    }

    public static Object[] split(String s, char match) {
        int position = 0;
        int lastPosition = 0;
        List<String> ll = new LinkedList();
        while (position <= s.length()) {
            while (position == s.length() || (position < s.length() && s.charAt(position) == match)) {
                if (lastPosition != position) {
                    ll.add(s.substring(lastPosition, position));
                    lastPosition = position;
                }
                position++;
                lastPosition++;
            }
            position++;
        }
        return ll.toArray();
    }

    public static String getHostName(String hostAndPort) {
        if (hostAndPort != null && hostAndPort.lastIndexOf(':') != -1)
            return hostAndPort.substring(0, hostAndPort.lastIndexOf(':'));
        return hostAndPort;
    }

    public static int getPortNumber(String hostAndPort, int defaultPortNumber) {
        if (hostAndPort != null && hostAndPort.lastIndexOf(':') != -1)
            return Integer.parseInt(hostAndPort.substring(hostAndPort.lastIndexOf(':') + 1).trim());
        return defaultPortNumber;
    }

    public static String libprefix() {
        String osn = System.getProperty("os.name");
        if (osn != null) {
            osn = osn.toUpperCase();
            if (osn.startsWith("WINDOWS"))
                return "";
            if (osn.startsWith("LINUX"))
                return "lib";
            if (osn.startsWith("MAC"))
                return "lib";
        }
        return "";
    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[2048];
        while (true) {
            int result = inputStream.read(buffer);
            if (result <= 0)
                return;
            outputStream.write(buffer, 0, result);
        }
    }

    private static byte[] readFully(InputStream is) throws IOException {
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        while (true) {
            int readReturnValue = is.read(buffer);
            if (readReturnValue == 0)
                return os.toByteArray();
            if (readReturnValue == -1)
                return os.toByteArray();
            os.write(buffer, 0, readReturnValue);
        }
    }

    public static void main(String[] arguments) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        if (arguments != null && arguments.length > 0) {
            if (arguments.length > 0 && arguments[0].equals("-decode")) {
                byte[] inputBuffer;
                if (arguments.length > 1) {
                    inputBuffer = readFile(arguments[1]);
                } else {
                    inputBuffer = readFully(System.in);
                }
                byte[] b = base64Decode(inputBuffer);
                System.out.print(new String(b));
                return;
            }
            if (arguments.length > 0 && arguments[0].equals("-encode")) {
                byte[] inputBuffer;
                if (arguments.length > 1) {
                    inputBuffer = readFile(arguments[1]);
                } else {
                    inputBuffer = readFully(System.in);
                }
                String encoding = base64Encode(inputBuffer);
                System.out.print(encoding);
                return;
            }
            int AES_KEY_LENGTH = 16;
            byte[] key = new byte[16];
            Key sessionKey = new SecretKeySpec(key, "AES");
            Cipher AESEncryptCipher = Cipher.getInstance("AES/ECB/NoPadding");
            Cipher AESDecryptCipher = Cipher.getInstance("AES/ECB/NoPadding");
            AESEncryptCipher.init(1, sessionKey);
            AESDecryptCipher.init(2, sessionKey);
            InputStream is = new FileInputStream(arguments[0]);
            OutputStream os = new CipherOutputStream(new FileOutputStream("f.out"), AESEncryptCipher);
            byte[] buffer = new byte[1024];
            while (true) {
                int numberRead = is.read(buffer);
                if (numberRead <= 0)
                    break;
                os.write(buffer, 0, numberRead);
            }
        }
    }
}
