package dna3Common;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class InitializationFile {
    private final String mFilename;

    private BigInteger mModulus;

    private BigInteger mPublicExponent;

    private BigInteger mPrivateExponent;

    private String mMasterControllerHostName;

    private String mSupervisorHostName;

    private String mSupervisorIP;

    private int mSupervisorRecPort = 0;

    private int mSupervisorGUIPort = 0;

    private int mSupervisorDBPort = 0;

    private int mWorkerGUIPort = 0;

    private int mGUIIPCPort = 0;

    private int mGUIWIFPort = 0;

    private boolean mStopWorkersOnUserInput = true;

    private boolean mRestrictDictionaryChunksToDictionaryGroup = false;

    private int mChunkDuration = 60000;

    private int mMaxConnections = 200;

    private long mStatusReportInterval = 60000L;

    private long mResetInterval = 86400000L;

    private long mSettingsUpdateInterval = 60000L;

    private boolean mEncrypted = false;

    private static final Map<String, InitializationFile> mInitializationFiles = new HashMap<>();

    private IniFile ini = null;

    private InitializationFile(String filename) throws IOException {
        this.mFilename = filename;
        this.ini = IniFile.LoadIniFile(filename);
        this.mModulus = BigInteger.ZERO;
        String line = this.ini.getString("modulus");
        if (line != null && line.length() > 0)
            this.mModulus = new BigInteger(line, 16);
        this.mPublicExponent = BigInteger.ZERO;
        line = this.ini.getString("publicexponent");
        if (line != null && line.length() > 0)
            this.mPublicExponent = new BigInteger(line, 16);
        this.mPrivateExponent = BigInteger.ZERO;
        line = this.ini.getString("privateexponent");
        if (line != null && line.length() > 0)
            this.mPrivateExponent = new BigInteger(line, 16);
        line = this.ini.getString("mastercontrollerhostname");
        if (line != null)
            this.mMasterControllerHostName = line;
        line = this.ini.getString("supervisorhostname");
        if (line != null)
            this.mSupervisorHostName = line;
        line = this.ini.getString("supervisorip");
        if (line != null)
            this.mSupervisorIP = line;
        line = this.ini.getString("supervisorrecport");
        if (line != null && line.length() > 0)
            this.mSupervisorRecPort = Integer.parseInt(line);
        line = this.ini.getString("supervisorguiport");
        if (line != null && line.length() > 0)
            this.mSupervisorGUIPort = Integer.parseInt(line);
        line = this.ini.getString("supervisordbport");
        if (line != null && line.length() > 0)
            this.mSupervisorDBPort = Integer.parseInt(line);
        line = this.ini.getString("workerrecport");
        if (line != null && line.length() > 0)
            this.mWorkerGUIPort = Integer.parseInt(line);
        line = this.ini.getString("guiipcport");
        if (line != null && line.length() > 0)
            this.mGUIIPCPort = Integer.parseInt(line);
        line = this.ini.getString("guiwifport");
        if (line != null && line.length() > 0)
            this.mGUIWIFPort = Integer.parseInt(line);
        line = this.ini.getString("stopworkersonuserinput");
        if (line != null)
            this.mStopWorkersOnUserInput = line.equalsIgnoreCase("true");
        line = this.ini.getString("restrictdictionarychunkstodictionarygroup");
        if (line != null)
            this.mRestrictDictionaryChunksToDictionaryGroup = line.equalsIgnoreCase("true");
        line = this.ini.getString("chunkduration");
        if (line != null && line.length() > 0)
            this.mChunkDuration = Integer.parseInt(line);
        line = this.ini.getString("maxconnections");
        if (line != null && line.length() > 0)
            this.mMaxConnections = Integer.parseInt(line);
        line = this.ini.getString("statusreportinterval");
        if (line != null && line.length() > 0)
            this.mStatusReportInterval = Long.parseLong(line);
        line = this.ini.getString("resetinterval");
        if (line != null && line.length() > 0)
            this.mResetInterval = Long.parseLong(line);
        line = this.ini.getString("settingsupdateinterval");
        if (line != null && line.length() > 0)
            this.mSettingsUpdateInterval = Long.parseLong(line);
        line = this.ini.getString("encryptedcommunications");
        if (line != null)
            this.mEncrypted = line.equalsIgnoreCase("true");
        String val = "" + this.mChunkDuration;
        this.ini.setString("ChunkDuration", val, false);
        val = "" + this.mMaxConnections;
        this.ini.setString("MaxConnections", val, false);
        val = "" + this.mStatusReportInterval;
        this.ini.setString("StatusReportInterval", val, false);
        val = "" + this.mResetInterval;
        this.ini.setString("ResetInterval", val, false);
        val = "" + this.mSettingsUpdateInterval;
        this.ini.setString("SettingsUpdateInterval", val, false);
        val = (this.mStopWorkersOnUserInput == true) ? "true" : "false";
        this.ini.setString("StopWorkersOnUserInput", val, false);
        val = (this.mRestrictDictionaryChunksToDictionaryGroup == true) ? "true" : "false";
        this.ini.setString("RestrictDictionaryChunksToDictionaryGroup", val, false);
    }

    private void save() throws IOException {
        this.ini.save();
    }

    public static InitializationFile open(String filename) throws IOException {
        synchronized (mInitializationFiles) {
            InitializationFile initializationFile = mInitializationFiles.get(filename);
            if (initializationFile == null) {
                initializationFile = new InitializationFile(filename);
                mInitializationFiles.put(filename, initializationFile);
            }
            return initializationFile;
        }
    }

    public static void purge(String filename) {
        synchronized (mInitializationFiles) {
            IniFile.purgeIniFile(filename);
            mInitializationFiles.remove(filename);
        }
    }

    public static void purge(String filename, boolean noSave) {
        synchronized (mInitializationFiles) {
            IniFile.purgeIniFile(filename, noSave);
            mInitializationFiles.remove(filename);
        }
    }

    public BigInteger getModulus() {
        return this.mModulus;
    }

    public void setModulus(BigInteger modulus) throws IOException {
        if (this.mModulus == null && modulus == null)
            return;
        if (this.mModulus != null && modulus != null && this.mModulus.equals(modulus))
            return;
        this.mModulus = modulus;
        this.ini.setString("modulus", this.mModulus.toString(16));
        save();
    }

    public BigInteger getPublicExponent() {
        return this.mPublicExponent;
    }

    public void setPublicExponent(BigInteger publicExponent) throws IOException {
        if (this.mPublicExponent == null && publicExponent == null)
            return;
        if (this.mPublicExponent != null && publicExponent != null && this.mPublicExponent.equals(publicExponent))
            return;
        this.mPublicExponent = publicExponent;
        this.ini.setString("publicexponent", this.mPublicExponent.toString(16));
        save();
    }

    public BigInteger getPrivateExponent() {
        return this.mPrivateExponent;
    }

    public String getMasterControllerHostName() {
        return this.mMasterControllerHostName;
    }

    public void setMasterControllerHostName(String masterControllerHostName) throws IOException {
        if (this.mMasterControllerHostName == null && masterControllerHostName == null)
            return;
        if (this.mMasterControllerHostName != null && masterControllerHostName != null && this.mMasterControllerHostName.equals(masterControllerHostName))
            return;
        this.mMasterControllerHostName = masterControllerHostName;
        this.ini.setString("mastercontrollerhostname", this.mMasterControllerHostName);
        save();
    }

    public String getSupervisorHostName() {
        return this.mSupervisorHostName;
    }

    public void setSupervisorHostName(String supervisorHostName) throws IOException {
        if (this.mSupervisorHostName == null && supervisorHostName == null)
            return;
        if (this.mSupervisorHostName != null && supervisorHostName != null && this.mSupervisorHostName.equals(supervisorHostName))
            return;
        this.mSupervisorHostName = supervisorHostName;
        this.ini.setString("supervisorhostname", this.mSupervisorHostName);
        save();
    }

    public String getSupervisorIP() {
        return this.mSupervisorIP;
    }

    public int getMaxConnections() {
        return this.mMaxConnections;
    }

    public void setSupervisorIP(String supervisorIP) throws IOException {
        if (this.mSupervisorIP == null && supervisorIP == null)
            return;
        if (this.mSupervisorIP != null && supervisorIP != null && this.mSupervisorIP.equals(supervisorIP))
            return;
        this.mSupervisorIP = supervisorIP;
        this.ini.setString("supervisorip", this.mSupervisorIP);
        save();
    }

    public int getSupervisorRecPort() {
        return this.mSupervisorRecPort;
    }

    public void setSupervisorRecPort(int supervisorRecPort) throws IOException {
        if (this.mSupervisorRecPort == 0 && supervisorRecPort == 0)
            return;
        if (this.mSupervisorRecPort != 0 && supervisorRecPort != 0 && this.mSupervisorRecPort == supervisorRecPort)
            return;
        this.mSupervisorRecPort = supervisorRecPort;
        String recport = "" + this.mSupervisorRecPort;
        this.ini.setString("supervisorrecport", recport);
        save();
    }

    public int getSupervisorGUIPort() {
        return this.mSupervisorGUIPort;
    }

    public void setSupervisorGUIPort(int supervisorGUIPort) throws IOException {
        if (this.mSupervisorGUIPort == 0 && supervisorGUIPort == 0)
            return;
        if (this.mSupervisorGUIPort != 0 && supervisorGUIPort != 0 && this.mSupervisorGUIPort == supervisorGUIPort)
            return;
        this.mSupervisorGUIPort = supervisorGUIPort;
        String recport = "" + this.mSupervisorGUIPort;
        this.ini.setString("supervisorguiport", recport);
        save();
    }

    public int getSupervisorDBPort() {
        return this.mSupervisorDBPort;
    }

    public void setSupervisorDBPort(int supervisorDBPort) throws IOException {
        if (this.mSupervisorDBPort == 0 && supervisorDBPort == 0)
            return;
        if (this.mSupervisorDBPort != 0 && supervisorDBPort != 0 && this.mSupervisorDBPort == supervisorDBPort)
            return;
        this.mSupervisorDBPort = supervisorDBPort;
        String recport = "" + this.mSupervisorDBPort;
        this.ini.setString("supervisordbport", recport);
        save();
    }

    public int getWorkerGUIPort() {
        return this.mWorkerGUIPort;
    }

    public void setWorkerGUIPort(int workerPort) throws IOException {
        if (this.mWorkerGUIPort == 0 && workerPort == 0)
            return;
        if (this.mWorkerGUIPort != 0 && workerPort != 0 && this.mWorkerGUIPort == workerPort)
            return;
        this.mWorkerGUIPort = workerPort;
        String recport = "" + this.mWorkerGUIPort;
        this.ini.setString("workerrecport", recport);
        save();
    }

    public int getGUIIPCPort() {
        return this.mGUIIPCPort;
    }

    public void setGUIIPCPort(int GUIIPCPort) throws IOException {
        if (this.mGUIIPCPort == 0 && GUIIPCPort == 0)
            return;
        if (this.mGUIIPCPort != 0 && GUIIPCPort != 0 && this.mGUIIPCPort == GUIIPCPort)
            return;
        this.mGUIIPCPort = GUIIPCPort;
        String recport = "" + this.mGUIIPCPort;
        this.ini.setString("guiipcport", recport);
        save();
    }

    public int getGUIWIFPort() {
        return this.mGUIWIFPort;
    }

    public void setGUIWIFPort(int GUIWIFPort) throws IOException {
        if (this.mGUIWIFPort == 0 && GUIWIFPort == 0)
            return;
        if (this.mGUIWIFPort != 0 && GUIWIFPort != 0 && this.mGUIWIFPort == GUIWIFPort)
            return;
        this.mGUIWIFPort = GUIWIFPort;
        String recport = "" + this.mGUIWIFPort;
        this.ini.setString("guiwifport", recport);
        save();
    }

    public boolean getStopWorkersOnUserInput() {
        return this.mStopWorkersOnUserInput;
    }

    public void setStopWorkersOnUserInput(boolean stopWorkersOnUserInput) throws IOException {
        if (this.mStopWorkersOnUserInput != stopWorkersOnUserInput) {
            this.mStopWorkersOnUserInput = stopWorkersOnUserInput;
            String val = (this.mStopWorkersOnUserInput == true) ? "true" : "false";
            this.ini.setString("stopworkersonuserinput", val);
            save();
        }
    }

    public boolean getRestrictDictionaryChunksToDictionaryGroup() {
        return this.mRestrictDictionaryChunksToDictionaryGroup;
    }

    public void setRestrictDictionaryChunksToDictionaryGroup(boolean restrictDictionaryChunksToDictionaryGroup) throws IOException {
        if (this.mRestrictDictionaryChunksToDictionaryGroup != restrictDictionaryChunksToDictionaryGroup) {
            this.mRestrictDictionaryChunksToDictionaryGroup = restrictDictionaryChunksToDictionaryGroup;
            String val = (this.mRestrictDictionaryChunksToDictionaryGroup == true) ? "true" : "false";
            this.ini.setString("restrictdictionarychunkstodictionarygroup", val);
            save();
        }
    }

    public int getChunkDuration() {
        return this.mChunkDuration;
    }

    public void setChunkDuration(int chunkDuration) throws IOException {
        if (this.mChunkDuration != chunkDuration) {
            this.mChunkDuration = chunkDuration;
            String val = "" + this.mChunkDuration;
            this.ini.setString("chunkduration", val);
            save();
        }
    }

    public long getStatusReportInterval() {
        return this.mStatusReportInterval;
    }

    public void setStatusReportInterval(long statusReportInterval) throws IOException {
        if (this.mStatusReportInterval != statusReportInterval) {
            this.mStatusReportInterval = statusReportInterval;
            String val = "" + this.mStatusReportInterval;
            this.ini.setString("statusreportinterval", val);
            save();
        }
    }

    public long getResetInterval() {
        return this.mResetInterval;
    }

    public void setResetInterval(long resetInterval) throws IOException {
        if (this.mResetInterval != resetInterval) {
            this.mResetInterval = resetInterval;
            String val = "" + this.mResetInterval;
            this.ini.setString("resetinterval", val);
            save();
        }
    }

    public long getSettingsUpdateInterval() {
        return this.mSettingsUpdateInterval;
    }

    public void setSettingsUpdateInterval(long settingsUpdateInterval) throws IOException {
        if (this.mSettingsUpdateInterval != settingsUpdateInterval) {
            this.mSettingsUpdateInterval = settingsUpdateInterval;
            String val = "" + this.mSettingsUpdateInterval;
            this.ini.setString("settingsupdateinterval", val);
            save();
        }
    }

    public boolean getEncryption() {
        return this.mEncrypted;
    }
}
