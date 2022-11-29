package ad.lang;

public class StringToolkit {
    private static final char[] HEX_TABLE = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f' };

    public static String stackTraceToString(Exception e) {
        if (null == e)
            return "";
        return e.getMessage() + "\n" + stackTraceToString(e.getStackTrace());
    }

    public static String stackTraceToString(StackTraceElement[] ste) {
        if (null == ste)
            return "";
        StringBuilder retval = new StringBuilder();
        for (int i = 0; i < ste.length; i++) {
            retval.append(ste[i].toString());
            retval.append('\n');
        }
        return retval.toString();
    }

    public static String indent(int depth) {
        return indent(depth, '\t');
    }

    public static String indent(int depth, char character) {
        StringBuilder retval = new StringBuilder(depth);
        while (depth-- > 0)
            retval.append(character);
        return retval.toString();
    }

    public static String getHexString(byte[] raw) {
        char[] retval = new char[2 * raw.length];
        int j = 0;
        for (char b = Character.MIN_VALUE; b < raw.length; b = (char)(b + 1)) {
            int i = b & 0xFF;
            retval[j++] = HEX_TABLE[i >>> 4];
            retval[j++] = HEX_TABLE[i & 0xF];
        }
        return new String(retval);
    }
}
