package dna3;

public class Worker {
    public static native int getRelease();

    public static native int getReleaseYear();

    public static native int getReleaseMonth();

    public static native int getReleaseDay();

    public static String nativeInvoke(String block) {
        String sRet = invoke(block);
        return sRet;
    }

    private static native String invoke(String paramString);
}
