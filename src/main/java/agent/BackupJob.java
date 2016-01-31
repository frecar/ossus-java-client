package agent;

import commons.ApiTrans;
import commons.Machine;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import commons.exceptions.OSSUSNoAPIConnectionException;
import commons.exceptions.OSSUSNoFTPServerConnection;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public final class BackupJob {

    private final Machine machine;
    private final List<Schedule> schedules;

    public BackupJob(
            final Machine machine
    ) throws
            ParseException,
            OSSUSNoAPIConnectionException,
            OSSUSNoFTPServerConnection {
        this.machine = machine;
        schedules = new ArrayList<>();
        getSchedules();
    }

    private void getSchedules() throws
            OSSUSNoAPIConnectionException,
            OSSUSNoFTPServerConnection {

        machine.logInfoMessage(
                "Fetching schedules from server"
        );

        List<JSONObject> jsonList = machine.apiHandler.getApiData(
                "machines/" + machine.id + "/schedules/"
        );

        JSONArray jsonArray = (JSONArray) jsonList.get(0).get("schedules");
        List<JSONObject> schedules = new ArrayList<>();

        if (jsonList.isEmpty()) {
            machine.logErrorMessage("Could not fetch schedules from server");
            return;
        } else {
            machine.logInfoMessage("Schedules fetched");
        }

        try {
            if (jsonArray != null) {
                for (Object jsonObject : jsonArray) {
                    schedules.add((JSONObject) jsonObject);
                }
            }
            addSchedules(schedules);
        } catch (ParseException e) {
            machine.logErrorMessage("Error getting schedules:\n" + e.getMessage());
        }
    }


    private void addSchedules(
            final List<JSONObject> schedules
    ) throws
            ParseException,
            OSSUSNoAPIConnectionException,
            OSSUSNoFTPServerConnection {

        for (Object o : schedules) {
            JSONObject obj = (JSONObject) o;
            machine.logInfoMessage("Adding schedule: " + obj.get("name"));
            Schedule schedule = new Schedule();

            schedule.setId((Long) obj.get(ApiTrans.SCHEDULE_ID.value));
            schedule.setName((String) obj.get(ApiTrans.SCHEDULE_NAME.value));

            schedule.setCurrentVersionInLoop(
                    (Long) obj.get(ApiTrans.SCHEDULE_CURRENT_VERSION_IN_LOOP.value)
            );

            schedule.setVersionsCount((Long) obj.get(ApiTrans.SCHEDULE_VERSIONS_COUNT.value));
            schedule.setMachine(machine);

            String nextBackupTimeString = obj.get(
                    ApiTrans.SCHEDULE_GET_NEXT_BACKUP_TIME.value
            ).toString();

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd H:m:s");
            Date nextBackupTime = formatter.parse(nextBackupTimeString);

            schedule.setNextBackupTime(nextBackupTime);

            JSONObject storage = ((JSONObject) obj.get("storage"));
            schedule.setStorage(
                    new FTPStorage(
                            machine,
                            (String) storage.get(ApiTrans.STORAGE_HOST.value),
                            (String) storage.get(ApiTrans.STORAGE_USERNAME.value),
                            (String) storage.get(ApiTrans.STORAGE_PASSWORD.value),
                            (String) storage.get(ApiTrans.STORAGE_FOLDER.value)
                    ));

            buildAndSetUploadPath(schedule, storage);

            schedule.setRunningBackup(
                    obj.get(
                            ApiTrans.SCHEDULE_RUNNING_BACKUP.value
                    ).toString().equals("true"));

            schedule.setRunningRestore(
                    obj.get(
                            ApiTrans.SCHEDULE_RUNNING_RESTORE.value
                    ).toString().equals("true"));

            JSONArray folderBackups = ((JSONArray) obj.get(
                    ApiTrans.SCHEDULE_FOLDER_BACKUPS.value));
            JSONArray sqlBackups = ((JSONArray) obj.get(
                    ApiTrans.SCHEDULE_SQL_BACKUPS.value));

            if (folderBackups != null) {
                for (Object folderBackupJson : folderBackups) {
                    FolderBackup folderBackup = new FolderBackup();
                    folderBackup.setId(((JSONObject) folderBackupJson).get("id").toString());
                    folderBackup.setPath(
                            ((JSONObject) folderBackupJson).get(
                                    "local_folder_path"
                            ).toString());
                    schedule.addFolderBackup(folderBackup);
                }
            } else {
                machine.logInfoMessage("No folders to backup");
            }

            if (sqlBackups != null) {
                for (Object sqlBackupJson : sqlBackups) {
                    SQLBackup sqlBackup = new SQLBackup();
                    sqlBackup.setId(((JSONObject) sqlBackupJson).get("id").toString());
                    sqlBackup.setHost(((JSONObject) sqlBackupJson).get("host").toString());
                    sqlBackup.setUsername(((JSONObject) sqlBackupJson).get("username").toString());
                    sqlBackup.setPassword(((JSONObject) sqlBackupJson).get("password").toString());
                    sqlBackup.setDatabase(((JSONObject) sqlBackupJson).get("database").toString());
                    sqlBackup.setType(((JSONObject) sqlBackupJson).get("type").toString());
                    sqlBackup.setPort(((JSONObject) sqlBackupJson).get("port").toString());
                    schedule.addSqlBackup(sqlBackup);
                }
            } else {
                machine.logInfoMessage("No sql databases to backup");
            }

            machine.logInfoMessage("Schedule added " + schedule.getName());
            this.schedules.add(schedule);
        }
    }

    private void buildAndSetUploadPath(
            final Schedule schedule,
            final JSONObject storage
    ) {
        String uploadPath = (String) storage.get(ApiTrans.STORAGE_FOLDER.value);

        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }

        uploadPath += storage.get(ApiTrans.STORAGE_CURRENT_DAY_FOLDER_PATH.value);
        schedule.setUploadPath(uploadPath);
    }

    public void runBackup() throws OSSUSNoAPIConnectionException {

        if (this.schedules.size() == 0) {
            machine.logWarningMessage("No schedules to run, have you set up any?");
        } else {
            for (Schedule schedule : this.schedules) {
                machine.logInfoMessage("Checking if job scheduled to run: "
                        + schedule.getName());
                if (new Date().after(schedule.getNextBackupTime())) {
                    machine.logInfoMessage("Running schedule "
                            + schedule.getName());

                    if (!schedule.getRunningBackup()) {
                        schedule.setRunningBackup(true);
                        schedule.save();

                        try {
                            schedule.runBackup();
                        } catch (OSSUSNoFTPServerConnection e) {
                            machine.logErrorMessage("Running schedule "
                                    + schedule.getName() + " failed");
                            machine.logErrorMessage(e.getMessage());
                        }

                        schedule.setRunningBackup(false);
                        schedule.save();

                    } else {
                        machine.logErrorMessage("This schedule is already running!");
                    }
                }
            }
        }
    }
}
