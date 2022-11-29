package dna3Common;

import ad.lang.StringToolkit;
import ad.utils.ADLogger;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public class ADFileInputStream extends FileInputStream {
    private static final Map<File, String> mChecksums = new TreeMap<>();

    private static final ADLogger mLogger = ADLogger.getRootLogger();

    public ADFileInputStream(File file) throws IOException, NoSuchAlgorithmException {
        this(file, (String)null);
    }

    public ADFileInputStream(File file, String checksum) throws IOException, NoSuchAlgorithmException {
        super(getLocalFile(file, checksum));
    }

    public ADFileInputStream(String filename) throws IOException, NoSuchAlgorithmException {
        this(new File(filename), (String)null);
    }

    public ADFileInputStream(String filename, String checksum) throws IOException, NoSuchAlgorithmException {
        this(new File(filename), checksum);
    }

    public static void replace(String filename, byte[] bytes) throws IOException {
        replace(new File(filename), bytes, 0, bytes.length);
    }

    public static void replace(String filename, byte[] bytes, int offset, int length) throws IOException {
        replace(new File(filename), bytes, offset, length);
    }

    public static void replace(File file, byte[] bytes) throws IOException {
        replace(file, bytes, 0, bytes.length);
    }

    public static void replace(File file, byte[] bytes, int offset, int length) throws IOException {
        mLogger.trace("ADFileInputStream.replace");
        mLogger.debug("ADFileInputStream.replace: name: " + file.getName() + " offset: " + offset + " length: " + length + " bytes length: " + bytes.length);
        synchronized (mChecksums) {
            invalidateChecksum(file);
            File backupFile = new File(file.getParent(), "backup-" + file.getName());
            try {
                if (backupFile.exists())
                    if (!backupFile.delete())
                        backupFile = File.createTempFile(file.getParent() + "backup-" + file.getName(), "", new File("."));
                if (!file.renameTo(backupFile))
                    file.delete();
            } catch (Exception exception) {}
            IO.writeFile(file, bytes, offset, length);
            try {
                String os_name = System.getProperty("os.name").toLowerCase();
                String java_ver = System.getProperty("java.version");
                int java_ver_ = Integer.parseInt(java_ver.substring(2, 3));
                mLogger.debug("ADFileInputStream.replace: os.name: " + os_name + " java.version: " + java_ver + " num ver: " + java_ver_);
                if (os_name.contains("linux") && java_ver_ >= 7)
                    ADSetFilePermissions.setFilePermissions(file);
            } catch (Exception e) {
                mLogger.warn(StringToolkit.stackTraceToString(e));
            }
        }
    }

    public static File getLocalFile(File file, String checksum) throws IOException, NoSuchAlgorithmException, ADFileNotFoundException, NullPointerException {
        mLogger.trace("ADFileInputStream.getLocalFile enter");
        mLogger.trace("ADFileInputStream.getLocalFile: file: " + file.getPath());
        if (checksum != null) {
            String localchk = getChecksum(file);
            if (!localchk.equals(checksum)) {
                invalidateChecksum(file);
                if (!getChecksum(file).equals(checksum)) {
                    mLogger.trace("ADFileInputStream.getLocalFile: checksum mismatch: localchk: " + localchk + " - " + checksum);
                    throw new ADFileNotFoundException("ADFileInputStream.java : getLocalFile failure : checksum mismatch : need to download file : " + file.getPath(), file);
                }
            }
        }
        if (file == null)
            throw new NullPointerException();
        if (file.exists())
            return file;
        File f = new File(file.getName());
        if (f == null)
            throw new NullPointerException();
        if (f.exists())
            return f;
        mLogger.trace("ADFileInputStream.getLocalFile: file not found");
        throw new ADFileNotFoundException("ADFileInputStream.java : getLocalFile failure : file not found : need to download file : " + file.getPath(), file);
    }

    public static File getLocalFile(File file) throws IOException, NoSuchAlgorithmException, ADFileNotFoundException, NullPointerException {
        return getLocalFile(file, (String)null);
    }

    public static File getLocalFile(String filename) throws IOException, NoSuchAlgorithmException, ADFileNotFoundException, NullPointerException {
        return getLocalFile(new File(filename), (String)null);
    }

    public static File getLocalFile(String filename, String checksum) throws IOException, NoSuchAlgorithmException, ADFileNotFoundException, NullPointerException {
        return getLocalFile(new File(filename), checksum);
    }

    public static String getChecksum(File file) throws IOException, NoSuchAlgorithmException, NullPointerException {
        if (file == null)
            throw new NullPointerException();
        file = getLocalFile(file, (String)null);
        assert mChecksums != null;
        synchronized (mChecksums) {
            String checksum = mChecksums.get(file);
            if (checksum == null) {
                ADFileInputStream fileInputStream = new ADFileInputStream(file, (String)null);
                checksum = fileInputStream.getSHA1();
                fileInputStream.close();
                Object previous = mChecksums.put(file, checksum);
                assert previous == null;
            }
            return checksum;
        }
    }

    public String getMD5() throws IOException, NoSuchAlgorithmException {
        return hash("MD5");
    }

    public String getSHA1() throws IOException, NoSuchAlgorithmException {
        return hash("SHA-1");
    }

    public static String getChecksum(String filename) throws IOException, NoSuchAlgorithmException {
        return getChecksum(new File(filename));
    }

    public static void invalidateChecksum(File file) throws IOException {
        synchronized (mChecksums) {
            mChecksums.remove(file);
        }
    }

    public static void invalidateChecksum(String filename) throws IOException {
        invalidateChecksum(new File(filename));
    }

    private String hash(String algorithm) throws IOException, NoSuchAlgorithmException {
        int BUFFER_SIZE = 1048576;
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        try {
            while (true) {
                byte[] buffer = new byte[1048576];
                int amountRead = read(buffer, 0, 1048576);
                if (amountRead == -1)
                    break;
                messageDigest.update(buffer, 0, amountRead);
            }
        } catch (EOFException eOFException) {}
        return IO.toHex(messageDigest.digest());
    }

    public static void main(String[] files) throws IOException, NoSuchAlgorithmException {
        for (int i = 0; i < files.length; i++) {
            String localFile = files[i];
            ADFileInputStream fileInputStream = new ADFileInputStream(localFile);
            String mMD5 = fileInputStream.getMD5();
            fileInputStream.close();
            fileInputStream = new ADFileInputStream(localFile);
            String mSHA1 = fileInputStream.getSHA1();
            fileInputStream.close();
            System.out.println("file : " + localFile);
            System.out.println("md5 : " + mMD5);
            System.out.println("SHA1 : " + mSHA1);
        }
    }
}
