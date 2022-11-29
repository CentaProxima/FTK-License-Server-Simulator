package dna3Common;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FixedLengthInputStream extends FilterInputStream {
    long mLength;

    public FixedLengthInputStream(InputStream is, long length) {
        super(is);
        this.mLength = length;
    }

    public int read() throws IOException {
        if (this.mLength <= 0L)
            return -1;
        int result = this.in.read();
        if (result >= 0)
            this.mLength--;
        return result;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (this.mLength <= 0L)
            return -1;
        if (len > this.mLength)
            len = (int)this.mLength;
        int result = this.in.read(b, off, len);
        if (result >= 0)
            this.mLength -= result;
        return result;
    }

    public long skip(long n) throws IOException {
        if (n > this.mLength)
            n = this.mLength;
        return this.in.skip(n);
    }

    public long length() {
        return this.mLength;
    }
}
