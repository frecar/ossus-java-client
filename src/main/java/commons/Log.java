package commons;

import commons.exceptions.OSSUSNoAPIConnectionException;

import java.util.HashMap;

public final class Log {

    private final APIHandler apiHandler;
    private final String id;
    private final Machine machine;

    private enum LogLevel {
        INFO("info"), ERROR("error"), WARNING("warning");
        String str;

        LogLevel(final String str) {
            this.str = str;
        }
    }

    public Log(final Machine machine, final APIHandler apiHandler, final String id) {
        this.apiHandler = apiHandler;
        this.machine = machine;
        this.id = id;
    }

    private void logMessage(
            final String text,
            final LogLevel level
    ) throws OSSUSNoAPIConnectionException {
        System.out.println(
            "Level: " + level + " Session: " + this.machine.session + " Message: " + text
        );
        final HashMap<String, String> map = new HashMap<>();
        map.put("id", "" + id);
        map.put("agent_session", "" + this.machine.session);
        map.put("type", "" + level.str);
        map.put("text", "" + text);
        this.apiHandler.setApiData("machines/" + id + "/create_log/", map);
    }

    public void logInfoMessage(final String text) throws OSSUSNoAPIConnectionException {
        logMessage(text, LogLevel.INFO);
    }

    public void logErrorMessage(final String text) throws OSSUSNoAPIConnectionException {
        logMessage(text, LogLevel.ERROR);
    }

    public void logWarningMessage(final String text) throws OSSUSNoAPIConnectionException {
        logMessage(text, LogLevel.WARNING);
    }
}
