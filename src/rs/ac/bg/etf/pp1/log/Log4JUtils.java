package rs.ac.bg.etf.pp1.log;

import java.io.File;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class Log4JUtils {

    private static Log4JUtils logs = new Log4JUtils();

    public static Log4JUtils instance() {
        return logs;
    }


    public void configure() {
        Logger root = Logger.getRootLogger();
        root.removeAllAppenders();

        root.setLevel(Level.DEBUG);

        PatternLayout layout =
                new PatternLayout("%-5p: %m%n");

        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setTarget(ConsoleAppender.SYSTEM_OUT);
        consoleAppender.setLayout(layout);
        consoleAppender.activateOptions();

        DailyRollingFileAppender fileAppender = new DailyRollingFileAppender();
        fileAppender.setName("file");
        fileAppender.setFile("logs/mj.log");
        fileAppender.setLayout(layout);
        fileAppender.activateOptions();

        root.addAppender(fileAppender);
        root.addAppender(consoleAppender);
    }


    public void prepareLogFile(Logger root) {
        Appender appender = root.getAppender("file");

        if (!(appender instanceof FileAppender))
            return;

        FileAppender fAppender = (FileAppender) appender;

        String logFileName = fAppender.getFile();
        logFileName = logFileName.substring(0, logFileName.lastIndexOf('.'))
                + "-test.log";

        File logFile = new File(logFileName);
        File renamedFile =
                new File(logFile.getAbsolutePath() + "." + System.currentTimeMillis());

        if (logFile.exists()) {
            if (!logFile.renameTo(renamedFile))
                System.err.println("Could not rename log file!");
        }

        fAppender.setFile(logFile.getAbsolutePath());
        fAppender.activateOptions();
    }
}
