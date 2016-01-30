package agent;

import commons.ApiTrans;
import commons.Machine;
import commons.exceptions.OSSUSNoAPIConnectionException;
import commons.exceptions.OSSUSNoFTPServerConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import static java.lang.Thread.sleep;


public final class Schedule {

    public static final int MB_SIZE = 1024;
    private String id;
    private String name;
    private Boolean runningBackup;
    private Boolean runningRestore;
    private String currentVersionInLoop;
    private String versionsCount;
    private String uploadPath;
    private Date nextBackupTime;

    private FTPStorage storage;
    private Machine machine;

    private List<FolderBackup> folderBackups = new ArrayList<FolderBackup>();
    private List<SQLBackup> sqlBackups = new ArrayList<SQLBackup>();

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Boolean getRunningBackup() {
        return runningBackup;
    }

    public void setRunningBackup(final Boolean runningBackup) {
        //this.machine.logInfoMessage("Running backup " + this.name + " " + runningBackup);
        this.runningBackup = runningBackup;
    }

    public void setRunningRestore(
            final Boolean runningRestore
    ) {
        this.runningRestore = runningRestore;
    }

    public void setCurrentVersionInLoop(
            final String string
    ) {
        this.currentVersionInLoop = string;
    }

    public void setVersionsCount(
            final String string
    ) {
        this.versionsCount = string;
    }

    public void setUploadPath(
            final String uploadPath
    ) {
        this.uploadPath = uploadPath;
    }

    public void setStorage(
            final FTPStorage storage
    ) {
        this.storage = storage;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(
            final Machine machine
    ) {
        this.machine = machine;
    }

    private int findNextCurrentVersionInLoop() {

        if (Integer.parseInt(this.currentVersionInLoop) >= Integer.parseInt(this.versionsCount)) {
            return 1;
        }

        return Integer.parseInt(this.currentVersionInLoop) + 1;

    }

    public void save()
            throws OSSUSNoAPIConnectionException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(ApiTrans.SCHEDULE_NAME.value, this.name);
        map.put(ApiTrans.SCHEDULE_RUNNING_BACKUP.value, "" + this.runningBackup);
        map.put(ApiTrans.SCHEDULE_RUNNING_RESTORE.value, "" + this.runningRestore);
        map.put(ApiTrans.SCHEDULE_CURRENT_VERSION_IN_LOOP.value,
                "" + this.findNextCurrentVersionInLoop());
        this.machine.apiHandler.setApiData("schedules/" + this.id + "/", map);
    }

    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public void createBackupEntry(
            final String start,
            final String end,
            final String fileName
    ) throws OSSUSNoAPIConnectionException {

        HashMap<String, String> map = new HashMap<>();
        map.put("schedule_id", "" + this.id);
        map.put("time_started", start);
        map.put("time_ended", end);
        map.put("uploadPath", this.uploadPath);
        map.put("file_name", fileName);

        this.machine.logInfoMessage("Create backup entry "
                + this.name + " started at "
                + start + " file name " + fileName);

        this.machine.apiHandler.setApiData("backups/" + this.machine.id + "/create_backup/", map);
    }

    public void runBackup() throws OSSUSNoAPIConnectionException, OSSUSNoFTPServerConnection {

        String start = this.getDateTime();

        FTPStorage ftpStorage = this.storage;

        String fileSeparator = System.getProperty("file.separator");
        String tempFolder = machine.localTempFolder;

        String filenameZip = "";

        if (this.getFolderBackups().size() == 0 && this.getSqlBackups().size() == 0) {
            this.machine.logWarningMessage("Schedule " + this.name
                    + " has nothing to do, skipping.");
            return;
        }

        for (FolderBackup folderBackup : this.getFolderBackups()) {
            filenameZip = zipAndUploadFolder(ftpStorage, fileSeparator, tempFolder, folderBackup);
        }

        for (SQLBackup sqlBackup : this.getSqlBackups()) {
            filenameZip = zipAndUploadSQLBackup(ftpStorage, fileSeparator, tempFolder, sqlBackup);
        }

        this.createBackupEntry(start, this.getDateTime(), filenameZip);

        this.save();

    }

    private String zipAndUploadSQLBackup(
            final FTPStorage ftpStorage,
            final String fileSeparator,
            final String tmpFolder,
            final SQLBackup sqlBackup
    ) throws OSSUSNoAPIConnectionException, OSSUSNoFTPServerConnection {

        String filenameZip;
        this.machine.logInfoMessage("Performing "
                + sqlBackup.getType() + " backup of "
                + sqlBackup.getDatabase() + " at "
                + sqlBackup.getHost());

        String folderZip = tmpFolder + sqlBackup.getDatabase() + fileSeparator;
        File f = new File(folderZip);

        String filenameBackupZip = "";

        try {

            if (f.mkdirs()) {
                this.machine.logInfoMessage("Created " + f.getName());
            }

            if (sqlBackup.getType().equals("mysql")) {
                filenameBackupZip = folderZip
                        + sqlBackup.getDatabase()
                        + ".sql";

                String executeCmd;

                //Delete old sql file if exists
                File oldSQLBackup = new File(filenameBackupZip);

                if (oldSQLBackup.exists() && !oldSQLBackup.delete()) {
                    machine.logErrorMessage("Error deleting old sql file");
                }

                executeCmd = this.machine.mysqlDumpCommand
                        + " --single-transaction --user='"
                        + sqlBackup.getUsername() + "' --host='"
                        + sqlBackup.getHost() + "' --password='"
                        + sqlBackup.getPassword() + "' "
                        + sqlBackup.getDatabase() + " > "
                        + filenameBackupZip;

                System.out.println(executeCmd);
                this.execShellCmd(executeCmd);
            } else {
                filenameBackupZip = folderZip + sqlBackup.getDatabase() + ".bak";

                //Delete old bak file if exists
                File fileBak = new File(filenameBackupZip);
                if (fileBak.exists() && !fileBak.delete()) {
                    machine.logErrorMessage("Error deleting old bak file");
                }

                Connection conn;
                Class.forName("net.sourceforge.jtds.jdbc.Driver");

                conn = DriverManager.getConnection(
                        "jdbc:jtds:sqlserver://" + sqlBackup.getHost()
                                + "; portNumber=" + sqlBackup.getPort()
                                + "; databaseName=" + sqlBackup.getDatabase(),
                        sqlBackup.getUsername(),
                        sqlBackup.getPassword());

                conn.setAutoCommit(true);
                Statement select = conn.createStatement();

                select.executeQuery("BACKUP DATABASE " + sqlBackup.getDatabase()
                        + " TO DISK='" + filenameBackupZip + "'");
                conn.close();

            }
        } catch (SQLException e) {
            machine.logErrorMessage(e.getMessage());
        } catch (ClassNotFoundException e) {
            machine.logErrorMessage(e.getMessage());
            e.printStackTrace();
        }

        filenameZip = filenameBackupZip.replace(fileSeparator, "_") + ".zip";

        try {
            Zipper.zipDir(filenameZip, folderZip, machine);
        } catch (Exception e) {
            this.machine.logErrorMessage(e.getMessage());
        }

        ftpStorage.upload(this.uploadPath, filenameZip, 0);

        try {
            if (new File(filenameZip).delete()) {
                this.machine.logInfoMessage("Deleted: " + filenameZip);
            }
        } catch (Exception e) {
            this.machine.logErrorMessage(e.getMessage());
        }

        try {
            if (new File(filenameZip).delete()) {
                this.machine.logInfoMessage("Deleted: " + filenameZip);
            }
        } catch (Exception e) {
            this.machine.logErrorMessage(e.getMessage());
        }

        return filenameZip;
    }

    private String zipAndUploadFolder(
            final FTPStorage ftpStorage,
            final String fileSeparator,
            final String tmpFolder,
            final FolderBackup folderBackup
    ) throws
            OSSUSNoAPIConnectionException,
            OSSUSNoFTPServerConnection {

        String filenameZip;
        filenameZip = folderBackup.getPath()
                .replaceAll("\\" + fileSeparator, "_")
                .replaceAll("\\:", "_")
                .replaceAll(" ", "-") + ".zip";

        this.machine.logInfoMessage("Zipping "
                + tmpFolder
                + filenameZip
                + " - "
                + folderBackup.getPath()
        );

        Zipper.zipDir(
                tmpFolder + filenameZip,
                folderBackup.getPath(),
                this.machine
        );

        File file = new File(tmpFolder + filenameZip);

        this.machine.logInfoMessage("Done zipping "
                + (file.length() / MB_SIZE / MB_SIZE)
                + " MB");

        this.machine.logInfoMessage("Uploading "
                + tmpFolder + filenameZip
                + " to " + this.uploadPath
                + " server: " + ftpStorage.client.getHost()
        );

        ftpStorage.upload(
                this.uploadPath,
                tmpFolder + filenameZip,
                0);

        this.machine.logInfoMessage(
                "Upload of " + filenameZip + " done"
        );

        if (file.delete()) {
            this.machine.logInfoMessage("Deleted: " + file.getName());
        }

        return filenameZip;
    }

    public void execShellCmd(
            final String cmd
    ) throws OSSUSNoAPIConnectionException {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(new String[]{"/bin/bash", "-c", cmd});
            //int exitValue = process.waitFor();
            BufferedReader buf = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            while (buf.readLine() != null) {
                sleep(1);
            }
        } catch (Exception e) {
            this.machine.logErrorMessage(e.getMessage());
        }
    }

    public List<FolderBackup> getFolderBackups() {
        return folderBackups;
    }

    public void addFolderBackup(
            final FolderBackup folderBackup
    ) {
        this.folderBackups.add(folderBackup);
    }

    public List<SQLBackup> getSqlBackups() {
        return sqlBackups;
    }

    public void addSqlBackup(
            final SQLBackup sqlBackup
    ) {
        this.sqlBackups.add(sqlBackup);
    }

    public Date getNextBackupTime() {
        return nextBackupTime;
    }

    public void setNextBackupTime(
            final Date nextBackupTime
    ) {
        this.nextBackupTime = nextBackupTime;
    }
}



