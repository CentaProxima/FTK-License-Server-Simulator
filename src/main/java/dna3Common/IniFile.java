package dna3Common;

import ad.utils.ADLogger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IniFile {
    private static final HashMap<String, IniFile> mpIniFiles = new HashMap<>();

    private String mFilename = null;

    private HashMap<String, String> mpKeyValues = new HashMap<>();

    private int refcount = 1;

    private boolean dirty = false;

    private static final ADLogger mLogger = ADLogger.getRootLogger();

    static IniFile LoadIniFile(String fileName) throws IOException {
        IniFile inf = mpIniFiles.get(fileName);
        if (inf == null) {
            inf = new IniFile(fileName);
            mpIniFiles.put(fileName, inf);
        } else {
            inf.refcount++;
        }
        return inf;
    }

    static void purgeIniFile(String fileName) {
        purgeIniFile(fileName, false);
    }

    static void purgeIniFile(String fileName, boolean bNoSave) {
        IniFile inf = mpIniFiles.remove(fileName);
        if (inf == null)
            return;
        if (inf.dirty && !bNoSave)
            inf.save();
        if (inf.refcount-- != 0)
            mpIniFiles.put(fileName, inf);
    }

    public boolean save() {
        boolean bRes = true;
        File file = new File(this.mFilename);
        File bakFile = new File(this.mFilename + "-back");
        bakFile.delete();
        file.renameTo(bakFile);
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.mFilename)));
            Iterator<Map.Entry<String, String>> it = this.mpKeyValues.entrySet().iterator();
            while (it.hasNext() == true) {
                Map.Entry<String, String> st = it.next();
                StringBuilder pair = new StringBuilder(st.getKey());
                pair.append("=");
                pair.append(st.getValue());
                bufferedWriter.write(pair.toString());
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (FileNotFoundException e) {
            mLogger.warn("IniFile.save: FileNotFoundException: " + e);
            bRes = false;
        } catch (IOException e) {
            mLogger.warn("IniFile.save: IOException: " + e);
            bRes = false;
        }
        if (bRes) {
            bakFile.delete();
            this.dirty = false;
        }
        return bRes;
    }

    private void load() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(this.mFilename)));
        this.mpKeyValues = new HashMap<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            int idx = line.indexOf(";");
            if (idx > -1)
                line = line.substring(0, idx);
            idx = line.indexOf("=");
            if (idx > -1) {
                String key = line.substring(0, idx).toLowerCase();
                String value = line.substring(idx + 1).trim();
                this.mpKeyValues.put(key, value);
            }
        }
        bufferedReader.close();
    }

    private IniFile(String fileName) throws IOException {
        this.mFilename = fileName;
        if (!(new File(fileName)).exists()) {
            save();
            return;
        }
        load();
    }

    public String getString(String key) {
        String str = "";
        try {
            load();
            String sKey = key.toLowerCase();
            str = this.mpKeyValues.get(sKey);
        } catch (IOException e) {
            mLogger.warn("IniFile.getString: IOException: " + e);
        }
        return str;
    }

    public long getValue(String key) {
        String strVal = "";
        try {
            load();
            String sKey = key.toLowerCase();
            strVal = this.mpKeyValues.get(sKey);
        } catch (IOException e) {
            mLogger.warn("IniFile.getValue: IOException: " + e);
        }
        return Long.parseLong(strVal);
    }

    public long getValue(String key, long defVal) {
        long lRes = defVal;
        try {
            load();
            String sKey = key.toLowerCase();
            String strVal = this.mpKeyValues.get(sKey);
            if (strVal != null)
                lRes = Long.parseLong(strVal);
        } catch (IOException e) {
            mLogger.warn("IniFile.getValue: IOException: " + e);
        }
        return lRes;
    }

    public void setString(String key, String str) {
        setString(key, str, true);
    }

    public void setString(String key, String str, boolean bSave) {
        try {
            load();
            String sKey = key.toLowerCase();
            this.mpKeyValues.remove(sKey);
            this.mpKeyValues.put(sKey, str);
            this.dirty = true;
            if (bSave)
                save();
        } catch (IOException e) {
            mLogger.warn("IniFile.setString: IOException: " + e);
        }
    }
}
