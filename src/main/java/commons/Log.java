package commons;

import commons.exceptions.OSSUSNoAPIConnectionException;

import java.util.HashMap;

public class Log {

	APIHandler apiHandler;
	String id;

    private enum LogLevel {
        INFO("info"), ERROR("error"), WARNING("warning");
        String str;
        LogLevel(String str) {
            this.str = str;
        }
    }

	public Log(APIHandler apiHandler, String id) {
		this.apiHandler = apiHandler;
		this.id = id;
	}

    private void log_msg(String text, LogLevel level) throws OSSUSNoAPIConnectionException {
        System.out.println("Level: " + level + " message: " + text);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("id", ""+ id);
        map.put("type", ""+level.str);
        map.put("text", ""+text);
		this.apiHandler.set_api_data("machines/"+ id +"/create_log/", map);
    }

	public void log_info(String text) throws OSSUSNoAPIConnectionException {
        log_msg(text, LogLevel.INFO);
	}

	public void log_error(String text) throws OSSUSNoAPIConnectionException {
        log_msg(text, LogLevel.ERROR);
	}

	public void log_warning(String text) throws OSSUSNoAPIConnectionException {
        log_msg(text, LogLevel.WARNING);
    }
}