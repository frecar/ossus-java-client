package ossus.agent;

import ossus.commons.Machine;
import ossus.commons.exceptions.OSSUSNoAPIConnectionException;
import ossus.commons.exceptions.OSSUSNoFTPServerConnection;

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


public class Schedule {

    private String id;
    private String name;
    private Boolean running_backup;
    private Boolean running_restore;
    private String current_version_in_loop;
    private String versions_count;
    private String upload_path;
    private Date get_next_backup_time;

    private FTPStorage storage;
    private Machine machine;

    private List<FolderBackup> folderBackups = new ArrayList<FolderBackup>();
    private List<SQLBackup> sqlBackups = new ArrayList<SQLBackup>();

    public String getId() {

        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getRunning_backup() {
        return running_backup;
    }

    public void setRunning_backup(Boolean running_backup) {
        //this.machine.log_info("Running backup " + this.name + " " + running_backup);
        this.running_backup = running_backup;
    }

    public Boolean getRunning_restore() {
        return running_restore;
    }

    public void setRunning_restore(Boolean running_restore) {
        this.running_restore = running_restore;
    }

    public void setCurrent_version_in_loop(String string) {
        this.current_version_in_loop = string;
    }

    public void setVersionsCount(String string) {
        this.versions_count = string;
    }

    public String getUpload_path() {
        return upload_path;
    }

    public void setUpload_path(String upload_path) {
        this.upload_path = upload_path;
    }

    public FTPStorage getStorage() {
        return storage;
    }

    public void setStorage(FTPStorage storage) {
        this.storage = storage;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    private int find_next_current_version_in_loop() {

        if (Integer.parseInt(this.current_version_in_loop) >= Integer.parseInt(this.versions_count)) {
            return 1;
        }

        return Integer.parseInt(this.current_version_in_loop) + 1;

    }

    public void save() throws OSSUSNoAPIConnectionException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("name", this.name);
        map.put("running_backup", "" + this.running_backup);
        map.put("running_restore", "" + this.running_restore);
        map.put("current_version_in_loop", "" + this.find_next_current_version_in_loop());
        this.machine.apiHandler.set_api_data("schedules/" + this.id + "/", map);
    }

    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public void createBackupEntry(String start, String end, String file_name) throws OSSUSNoAPIConnectionException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("schedule_id", "" + this.id);
        map.put("time_started", start);
        map.put("time_ended", end);
        map.put("upload_path", this.upload_path);
        map.put("file_name", file_name);

        this.machine.log_info("Create backup entry " + this.name + " started at " + start + " file name " + file_name);
        this.machine.apiHandler.set_api_data("backups/" + this.machine.id + "/create_backup/", map);
    }

    public void runBackup() throws OSSUSNoAPIConnectionException, OSSUSNoFTPServerConnection {

        String start = this.getDateTime();

        FTPStorage ftpStorage = this.storage;

        String file_separator = System.getProperty("file.separator");
        String tmp_folder = machine.get_local_temp_folder();

        String filename_zip = "";

        if (this.getFolderBackups().size() == 0 && this.getSqlBackups().size() == 0) {
            this.machine.log_warning("Schedule " + this.name + " has nothing to do, skipping.");
            return;
        }

        for (FolderBackup folderBackup : this.getFolderBackups()) {
            filename_zip = zipAndUploadFolder(ftpStorage, file_separator, tmp_folder, folderBackup);
        }

        for (SQLBackup sqlBackup : this.getSqlBackups()) {
            filename_zip = zipAndUploadSQLBackup(ftpStorage, file_separator, tmp_folder, sqlBackup);
        }

        this.createBackupEntry(start, this.getDateTime(), filename_zip);

        this.save();

    }

    private String zipAndUploadSQLBackup(FTPStorage ftpStorage,
                                         String file_separator,
                                         String tmp_folder,
                                         SQLBackup sqlBackup) throws OSSUSNoAPIConnectionException, OSSUSNoFTPServerConnection {
        String filename_zip;
        this.machine.log_info("Performing " + sqlBackup.getType() + " backup of " + sqlBackup.getDatabase() + " at " + sqlBackup.getHost());
        String folder_zip = tmp_folder + sqlBackup.getDatabase() + file_separator;
        File f = new File(folder_zip);

        String filename_backup_zip = "";

        try {
            f.mkdirs();
            if (sqlBackup.getType().equals("mysql")) {
                filename_backup_zip = folder_zip + sqlBackup.getDatabase() + ".sql";
                String executeCmd;

                //Delete old sql file if exists
                File file_bak = new File(filename_backup_zip);
                if (file_bak.exists() && !file_bak.delete()) {
                    machine.log_error("Error deleting old sql file");
                }

                executeCmd = this.machine.mysql_dump + " --single-transaction --user='" + sqlBackup.getUsername() + "' --host='" + sqlBackup.getHost() + "' --password='" + sqlBackup.getPassword() + "' " + sqlBackup.getDatabase() + " > " + filename_backup_zip;
                System.out.println(executeCmd);
                this.execShellCmd(executeCmd);
            } else {
                filename_backup_zip = folder_zip + sqlBackup.getDatabase() + ".bak";

                //Delete old bak file if exists
                File file_bak = new File(filename_backup_zip);
                if (file_bak.exists() && !file_bak.delete()) {
                    machine.log_error("Error deleting old bak file");
                }

                Connection conn;
                Class.forName("net.sourceforge.jtds.jdbc.Driver");

                conn = DriverManager.getConnection("jdbc:jtds:sqlserver://" + sqlBackup.getHost() + "; portNumber=" + sqlBackup.getPort() + "; databaseName=" + sqlBackup.getDatabase(), sqlBackup.getUsername(), sqlBackup.getPassword());
                conn.setAutoCommit(true);
                Statement select = conn.createStatement();

                select.executeQuery("BACKUP DATABASE " + sqlBackup.getDatabase() + " TO DISK='" + filename_backup_zip + "'");
                conn.close();

            }
        } catch (SQLException e) {
            machine.log_error(e.getMessage());
        } catch (ClassNotFoundException e) {
            machine.log_error(e.getMessage());
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        filename_zip = filename_backup_zip.replace(file_separator, "_") + ".zip";

        try {
            Zipper.zipDir(filename_zip, folder_zip, machine);
        } catch (Exception e) {
            this.machine.log_error(e.getMessage());
        }

        ftpStorage.upload(this.upload_path, filename_zip, 0);

        try {
            new File(filename_zip).delete();
        } catch (Exception e) {
            this.machine.log_error(e.getMessage());
        }

        try {
            new File(filename_zip).delete();
        } catch (Exception e) {
            this.machine.log_error(e.getMessage());
        }
        return filename_zip;
    }

    private String zipAndUploadFolder(FTPStorage ftpStorage,
                                      String file_separator,
                                      String tmp_folder,
                                      FolderBackup folderBackup) throws OSSUSNoAPIConnectionException, OSSUSNoFTPServerConnection {
        String filename_zip;
        filename_zip = folderBackup.getPath().replaceAll("\\" + file_separator, "_").replaceAll("\\:", "_").replaceAll(" ", "-") + ".zip";

        this.machine.log_info("Zipping " + tmp_folder + filename_zip + " - " + folderBackup.getPath());
        Zipper.zipDir(tmp_folder + filename_zip, folderBackup.getPath(), this.machine);

        File file = new File(tmp_folder + filename_zip);

        this.machine.log_info("Done zipping " + (file.length() / 1024 / 1024) + " MB");
        this.machine.log_info("Uploading " + tmp_folder + filename_zip + " to " + this.upload_path + " server: " + ftpStorage.client.getHost());
        ftpStorage.upload(this.upload_path, tmp_folder + filename_zip, 0);
        this.machine.log_info("Upload of " + filename_zip + " done");

        file.delete();

        return filename_zip;
    }

    public void execShellCmd(String cmd) throws OSSUSNoAPIConnectionException {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(new String[]{"/bin/bash", "-c", cmd});
            //int exitValue = process.waitFor();
            BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (buf.readLine() != null) {
                sleep(1);
            }
        } catch (Exception e) {
            this.machine.log_error(e.getMessage());
        }
    }

    public List<FolderBackup> getFolderBackups() {
        return folderBackups;
    }

    public void addFolderBackup(FolderBackup folderBackup) {
        this.folderBackups.add(folderBackup);
    }

    public List<SQLBackup> getSqlBackups() {
        return sqlBackups;
    }

    public void addSqlBackup(SQLBackup sqlBackup) {
        this.sqlBackups.add(sqlBackup);
    }

    public Date get_next_backup_time() {
        return get_next_backup_time;
    }

    public void set_next_backup_time(Date get_next_backup_time) {
        this.get_next_backup_time = get_next_backup_time;
    }
}



