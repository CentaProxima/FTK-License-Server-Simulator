package ad.utils;

import ad.lang.StringToolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggerFactory;

public class ADLogger extends Logger {
    private static ADLogger mLogger = null;

    private static ADLoggerFactory mFactory = new ADLoggerFactory();

    private ADLogger() {
        super(null);
    }

    private ADLogger(String name) {
        super(name);
    }

    public static ADLogger getLogger(Class clazz, String appName) {
        String className = clazz.getName();
        ADLogger logger = (ADLogger)Logger.getLogger(className, mFactory);
        mLogger = logger;
        try {
            Native.loadJniLibrary();
            String logPath = System.getProperty("PR_LOG_PATH");
            if (logPath == null) {
                logPath = Native.getLogPath();
            } else {
                logPath = System.getProperty("user.home") + "/" + logPath;
                File tmplog = new File(logPath);
                tmplog.mkdirs();
            }
            String configPath = logPath;
            logPath = logPath.replaceAll("\\\\", "/");
            configPath = configPath + File.separator + ((appName == null) ? "AD" : appName) + "_logger.conf";
            createConfig(configPath, logPath + "/" + appName + ".log");
            PropertyConfigurator.configureAndWatch(configPath);
        } catch (Exception e) {
            System.out.println("ADLogger.getLogger: Exception: " + e);
        }
        logger.info("Starting " + className);
        return logger;
    }

    private static void createConfig(String configPath, String logPath) {
        File configFile = new File(configPath);
        String logLevel = "info";
        String maxFileSize = "16MB";
        String maxBackupIndex = "30";
        boolean overwrite = false;
        try {
            if (System.getProperty("ad.log.debug") != null) {
                logLevel = "debug";
                overwrite = true;
            }
        } catch (Exception exception) {}
        try {
            if (System.getProperty("ad.log.dev") != null) {
                logLevel = "trace";
                maxFileSize = "32MB";
                overwrite = true;
            }
        } catch (Exception exception) {}
        try {
            if (System.getProperty("ad.log.default") != null)
                overwrite = true;
        } catch (Exception exception) {}
        try {
            String value;
            if ((value = System.getProperty("ad.log.level")) != null) {
                logLevel = value;
                overwrite = true;
            }
        } catch (Exception exception) {}
        try {
            String value;
            if ((value = System.getProperty("ad.log.maxSize")) != null) {
                maxFileSize = value;
                overwrite = true;
            }
        } catch (Exception exception) {}
        try {
            String value;
            if ((value = System.getProperty("ad.log.maxIndex")) != null) {
                maxBackupIndex = value;
                overwrite = true;
            }
        } catch (Exception exception) {}
        if (overwrite)
            try {
                if (configFile.exists())
                    configFile.delete();
            } catch (Exception exception) {}
        if (false == configFile.exists())
            try {
                PrintStream ps = new PrintStream(configFile);
                ps.println("# AccessData-Generated log4j Default Configuration");
                ps.println("# For formatting and filtering information please go to: http://logging.apache.org/log4j/1.2/manual.html");
                ps.println("# Patterns are described here: http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html");
                ps.println("#");
                ps.println("# The logging level hierarchy is: ALL < TRACE < DEBUG < INFO < WARN < ERROR < FATAL < OFF");
                ps.println("log4j.rootLogger=" + logLevel + ", stdout, AD");
                ps.println();
                ps.println("log4j.appender.stdout=org.apache.log4j.ConsoleAppender");
                ps.println("log4j.appender.stdout.layout=org.apache.log4j.PatternLayout");
                ps.println("log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd} %d{HH:mm:ss.SSS} [%t] [%x] [%C:%L] [%p] : %m%n");
                ps.println("log4j.appender.stdout.encoding=UTF-8");
                ps.println();
                ps.println("log4j.appender.AD=org.apache.log4j.RollingFileAppender");
                ps.println("log4j.appender.AD.File=" + logPath);
                ps.println("# Allow files to be " + maxFileSize + " in size");
                ps.println("log4j.appender.AD.MaxFileSize=" + maxFileSize);
                ps.println("# Keep <n> backup files");
                ps.println("log4j.appender.AD.MaxBackupIndex=" + maxBackupIndex);
                ps.println("log4j.appender.AD.layout=org.apache.log4j.PatternLayout");
                ps.println("log4j.appender.AD.layout.ConversionPattern=%d{yyyy-MM-dd} %d{HH:mm:ss.SSS} [%t] [%x] [%C:%L] [%p] : %m%n");
                ps.println("log4j.appender.AD.encoding=UTF-8");
                ps.close();
            } catch (FileNotFoundException fnfe) {
                System.out.println("ADLogger.createConfig: FileNotFoundException: " + fnfe);
            } catch (Exception e) {
                System.out.println("ADLogger.createConfig: Exception: " + e);
            }
    }

    public void log(int level, String str) {
        log((Priority)Level.toLevel(level, Level.INFO), str);
    }

    public void entering(String str) {
        debug(str + " ---Entering");
    }

    public void leaving(String str) {
        debug(str + " ---Leaving");
    }

    public void exception(Exception e) {
        error(StringToolkit.stackTraceToString(e));
    }

    public static ADLogger getRootLogger() {
        return mLogger;
    }

    public void setLevel(int level) {
        setLevel(Level.toLevel(level, Level.INFO));
    }

    private static class ADLoggerFactory implements LoggerFactory {
        public Logger makeNewLoggerInstance(String name) {
            return new ADLogger(name);
        }
    }
}
