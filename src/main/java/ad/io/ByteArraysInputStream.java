package ad.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ByteArraysInputStream extends InputStream {
    List mInputStreams = new LinkedList();

    Iterator mIterator;

    InputStream mInputStream;

    public void add(InputStream is) {
        this.mInputStreams.add(is);
    }

    private boolean setInputStream() {
        if (this.mIterator == null)
            this.mIterator = this.mInputStreams.iterator();
        if (this.mInputStream == null)
            if (this.mIterator.hasNext())
                this.mInputStream = (InputStream) this.mIterator.next();
        return (this.mInputStream != null);
    }

    public int read() throws IOException {
        while (setInputStream()) {
            int returnValue = this.mInputStream.read();
            if (returnValue == -1) {
                this.mInputStream = null;
                continue;
            }
            return returnValue;
        }
        return -1;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        while (setInputStream()) {
            int returnValue = this.mInputStream.read(b, off, len);
            if (returnValue == -1 || returnValue == 0) {
                this.mInputStream = null;
                continue;
            }
            return returnValue;
        }
        return -1;
    }
}
