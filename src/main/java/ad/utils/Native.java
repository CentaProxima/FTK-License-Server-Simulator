package ad.utils;

public class Native {
    private static boolean mJniLoaded = false;

    private static final ADLogger mLogger = ADLogger.getRootLogger();

    public static native boolean isLoaded(String paramString);

    public static native int killApp(String paramString);

    public static native int getProductBitness();

    public static native String getDataBasePath();

    public static native String getDataPath();

    public static native String getDictionariesPath();

    public static native String getLevelsPath();

    public static native String getLogPath();

    public static native String getModulesPath();

    public static native String getProfilesPath();

    public static native String getRootPath();

    public static native void initNative();

    public static String getLibraryPrefix() {
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

    public static String getLibraryArch() {
        String retval = "";
        String osn = System.getProperty("os.name");
        String osa = System.getProperty("os.arch");
        int bitness = getProductBitness();
        if (osn != null && osa != null) {
            osn = osn.toUpperCase();
            osa = osa.toUpperCase();
            if (bitness == 0)
                bitness = 32;
            if (osn.startsWith("WINDOWS")) {
                if (bitness == 64) {
                    retval = "-amd64";
                } else {
                    retval = "-i386";
                }
            } else if (osn.startsWith("LINUX")) {
                if (osa.startsWith("PPC")) {
                    if (bitness == 64) {
                        retval = "-ppc64";
                    } else {
                        retval = "-ppc32";
                    }
                } else if (bitness == 64) {
                    retval = "-amd64";
                } else {
                    retval = "-i386";
                }
            } else if (osn.startsWith("MAC")) {
                retval = "-mac";
            }
        }
        return retval;
    }

    public static String getLibrarySuffix(boolean withArch, boolean withExt) {
        String osn = System.getProperty("os.name");
        String arch = (withArch == true) ? getLibraryArch() : "";
        String ext = "";
        if (true == withExt)
            if (osn != null) {
                osn = osn.toUpperCase();
                if (osn.startsWith("WINDOWS")) {
                    ext = ".dll";
                } else if (osn.startsWith("LINUX")) {
                    ext = ".so";
                } else if (osn.startsWith("MAC")) {
                    ext = ".dylib";
                }
            }
        return arch + ext;
    }

    boolean isJniLoaded() {
        synchronized (Native.class) {
            return mJniLoaded;
        }
    }

    public static void loadJniLibrary() throws Exception {
        synchronized (Native.class) {
            if (!mJniLoaded) {
                String library = "recovery_jni";
                try {
                    System.loadLibrary(library);
                    System.out.println("Native.loadJniLibrary: Successfully loaded '" + library + "'.");
                } catch (Exception e) {
                    System.err.println("Failed to load '" + library + "'.");
                    System.err.println("Native.loadJniLibrary: Exception: " + e.toString());
                    throw e;
                }
                try {
                    initNative();
                    mJniLoaded = true;
                } catch (Exception e) {
                    System.err.println("Failed to initialize '" + library + "'.");
                    System.err.println("Native.loadJniLibrary: Exception: " + e.toString());
                    throw e;
                }
            }
        }
    }
}
