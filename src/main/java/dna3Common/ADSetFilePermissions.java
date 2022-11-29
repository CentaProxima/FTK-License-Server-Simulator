package dna3Common;

import ad.lang.StringToolkit;
import ad.utils.ADLogger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.EnumSet;
import java.util.Set;

public class ADSetFilePermissions {
    private static ADLogger mLogger = ADLogger.getRootLogger();

    public static void setFilePermissions(File file) {
        mLogger.trace("ADFileInputStream.setFilePermissions");
        try {
            Path path = Paths.get(file.getAbsolutePath(), new String[0]);
            PosixFileAttributeView pf_view = Files.<PosixFileAttributeView>getFileAttributeView(path, PosixFileAttributeView.class, new java.nio.file.LinkOption[0]);
            String ext = file.getAbsolutePath().toLowerCase();
            ext = ext.substring(ext.lastIndexOf('.') + 1, ext.length());
            if (ext.equals("adf")) {
                Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OTHERS_READ, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
                pf_view.setPermissions(perms);
            } else if (ext.equals("so")) {
                Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OTHERS_READ, new PosixFilePermission[] { PosixFilePermission.OTHERS_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE });
                pf_view.setPermissions(perms);
            }
        } catch (UserPrincipalNotFoundException upnfe) {
            mLogger.warn("ADFileInputStream.setFilePermissions: UserPrincipalNotFoundException");
            mLogger.debug(StringToolkit.stackTraceToString(upnfe));
        } catch (IOException ioe) {
            mLogger.warn("ADFileInputStream.setFilePermissions: IOException");
            mLogger.debug(StringToolkit.stackTraceToString(ioe));
        }
    }
}
