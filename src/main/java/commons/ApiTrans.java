package commons;


public enum ApiTrans {

    VERSION_ID("id"),
    VERSION_NAME("name"),
    VERSION_AGENT_LINK("agent_link"),
    VERSION_UPDATER_LINK("updater_link"),

    MACHINE_SERVER_IP("server_ip"),
    MACHINE_ID("id"),
    MACHINE_API_USER("api_user"),
    MACHINE_API_TOKEN("api_token"),
    MACHINE_OS_SYSTEM("os_system"),
    MACHINE_DOWNLOADS_CLIENT("downloads_client"),
    MACHINE_MYSQL_DUMP("mysql_dump"),
    MACHINE_AGENT_FOLDER("agent_folder"),
    MACHINE_LOCAL_TEMP_FOLDER("local_temp_folder"),
    MACHINE_FORCE_ACTION("force_action"),
    MACHINE_AUTO_UPDATE("auto_update"),
    MACHINE_RUN_INSTALL("run_install"),
    MACHINE_IS_BUSY("is_busy"),

    MACHINE_CURRENT_UPDATER_VERSION("current_updater_version"),
    MACHINE_SELECTED_UPDATER_VERSION("selected_updater_version"),

    SCHEDULE_NAME("name"),
    SCHEDULE_CURRENT_VERSION_IN_LOOP("current_version_in_loop"),
    SCHEDULE_ID("id"),
    SCHEDULE_VERSIONS_COUNT("versions_count"),
    SCHEDULE_GET_NEXT_BACKUP_TIME("get_next_backup_time"),
    SCHEDULE_RUNNING_RESTORE("running_restore"),
    SCHEDULE_RUNNING_BACKUP("running_backup"),
    SCHEDULE_FOLDER_BACKUPS("folder_backups"),
    SCHEDULE_SQL_BACKUPS("sql_backups"),

    STORAGE_HOST("host"),
    STORAGE_USERNAME("username"),
    STORAGE_PASSWORD("password"),
    STORAGE_FOLDER("folder"),
    STORAGE_CURRENT_DAY_FOLDER_PATH("current_day_folder_path"),

    BACKUP_ENTRY_TIME_STARTED("time_started"),
    BACKUP_ENTRY_TIME_ENDED("time_ended"),
    BACKUP_ENTRY_UPLOAD_PATH("upload_path"),
    BACKUP_ENTRY_FILE_NAME("file_name"),
    BACKUP_ENTRY_SCHEDULE_ID("schedule_id");

    public final String value;

    ApiTrans(final String value) {
        this.value = value;
    }

}
