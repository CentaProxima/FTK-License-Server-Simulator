package dna3Common;

import java.io.File;
import java.io.FileNotFoundException;

public class ADFileNotFoundException extends FileNotFoundException {
    private File mFile;

    public ADFileNotFoundException(String message, File file) {
        super(message);
        this.mFile = file;
    }

    public ADFileNotFoundException(File file) {
        this.mFile = file;
    }

    public File getFile() {
        return this.mFile;
    }
}
