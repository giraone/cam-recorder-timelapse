package com.giraone.camera.service.video;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class OsUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(OsUtil.class);

    public static OsCommandResult runCommand(String[] command) {
        return runCommand(command, 30);
    }

    public static OsCommandResult runCommand(String[] command, int maxWaitTimeinSeconds) {
        Process p = null;
        LOGGER.info("OsCommandResult.runCommand: {}", commandStringFromArray(command));
        final long start = System.currentTimeMillis();
        try {
            p = Runtime.getRuntime().exec(command);
            try {
                if (!p.waitFor(maxWaitTimeinSeconds, TimeUnit.SECONDS)) {
                    return new OsCommandResult(-3, "Command " + commandStringFromArray(command) + " timed out after 30 seconds!", null);
                }
            } catch (InterruptedException interruptedException) {
                LOGGER.error("Interrupt when running {}", commandStringFromArray(command), interruptedException);
                return new OsCommandResult(-2, null, interruptedException);
            }

            if (LOGGER.isDebugEnabled()) {
                final long end = System.currentTimeMillis();
                LOGGER.debug("OsCommandResult.runCommand: ExitCode = {}}, Time = {} ms", p.exitValue(), (end - start));
            }
            return new OsCommandResult(p.exitValue(), null, null);
        } catch (IOException ioe) {
            LOGGER.error("IO Error running {}", commandStringFromArray(command), ioe);
            return new OsCommandResult(-1, null, ioe);
        }
    }

    public static OsCommandResult runCommandAndReadOutput(String[] command, long maxWaitTimeInMilliseconds) {
        StringBuilder builder = new StringBuilder();
        Process p = null;
        LOGGER.info("OsCommandResult.runCommandAndReadOutput: {}", commandStringFromArray(command));
        final long start = System.currentTimeMillis();
        try {
            p = Runtime.getRuntime().exec(command);
            try {
                if (!p.waitFor(maxWaitTimeInMilliseconds, TimeUnit.MILLISECONDS)) {
                    return new OsCommandResult(-3, "Command " + commandStringFromArray(command) + " timed out after 30 seconds!", null);
                }
            } catch (InterruptedException interruptedException) {
                LOGGER.error("Interrupt when running {}", commandStringFromArray(command), interruptedException);
                return new OsCommandResult(-2, null, interruptedException);
            }

            if (LOGGER.isDebugEnabled()) {
                final long end = System.currentTimeMillis();
                LOGGER.debug("OsCommandResult.runCommandAndReadOutput: ExitCode = {}}, Time = {} ms", p.exitValue(), (end - start));
            }

            try (BufferedReader commandResult = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String s;
                while ((s = commandResult.readLine()) != null) {
                    builder.append(s);
                    builder.append("\r\n");
                }
                return new OsCommandResult(p.exitValue(), builder.toString(), null);
            }
        } catch (IOException ioe) {
            LOGGER.error("IO Error running {}", commandStringFromArray(command), ioe);
            return new OsCommandResult(-1, null, ioe);
        }
    }

    private static String commandStringFromArray(String[] command) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < command.length; i++) {
            if (i > 0) ret.append(" ");
            ret.append("\"");
            ret.append(command[i]);
            ret.append("\"");
        }
        return ret.toString();
    }

    public record OsCommandResult(int code, String output, Exception exception) {
    }
}
