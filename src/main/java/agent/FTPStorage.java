package agent;

import it.sauronsoftware.ftp4j.*;
import commons.Machine;
import commons.exceptions.OSSUSNoAPIConnectionException;
import commons.exceptions.OSSUSNoFTPServerConnection;

import java.io.File;
import java.io.IOException;

public class FTPStorage {
    FTPClient client;
    String homeFolder;
    Machine machine;

    String host;
    String username;
    String password;

    public FTPStorage(Machine machine, String host, String username, String password, String folder)
            throws OSSUSNoAPIConnectionException, OSSUSNoFTPServerConnection {
        this.host = host;
        this.username = username;
        this.password = password;
        this.homeFolder = folder;
        this.machine = machine;
        this.reconnect();
    }

    public void reconnect() throws OSSUSNoAPIConnectionException, OSSUSNoFTPServerConnection {
        try {
            this.machine.log_info("Connecting to FTP Server");
            this.client = new FTPClient();
            this.client.connect(this.host);
            this.client.login(this.username, this.password);
            this.machine.log_info("Successfully connected to FTP Server");
        } catch (Exception e) {
            this.machine.log_error(e.getMessage());
            this.machine.log_error("FTP Server is unavailable, terminating this session and set machine not busy");
            throw new OSSUSNoFTPServerConnection("FTP Server is unavailable");
        }
    }

    public void createFolder(String folder) throws OSSUSNoAPIConnectionException {
        try {
            this.client.changeDirectory(this.homeFolder);
        } catch (Exception e2) {
            this.machine.log_error(e2.getMessage());
        }

        String[] path = folder.split("/");
        String mid_path = "";

        for (String p : path) {
            mid_path += p + "/";

            try {
                this.client.changeDirectory(mid_path);
            } catch (Exception e) {
                try {
                    this.client.createDirectory(mid_path);
                } catch (Exception e1) {
                    this.machine.log_error(e1.getMessage());
                }
                this.machine.log_error(e.getMessage());
                this.machine.log_info("Creating folder " + mid_path);
            }
        }
    }

    public void upload(String destination, String local_file, int restart_attempts)
            throws OSSUSNoAPIConnectionException, OSSUSNoFTPServerConnection {
        int max_restart_attempts = 3;

        try {
            this.machine.log_info("Starting to upload " + local_file + " to " + destination);

            if (restart_attempts > 0) {
                this.machine.log_warning(". This is the " + (restart_attempts + 1) + " attempt to for this upload");
            }

            if (restart_attempts > max_restart_attempts) {
                this.machine.log_error("Aborting FTP upload");
                this.machine.log_error("Attempted to start upload more than " + max_restart_attempts + " times");
                throw new OSSUSNoFTPServerConnection("No connection to FTP server");
            }

            this.createFolder(destination);
            this.client.changeDirectory(destination);

            File file = new File(local_file);
            MyTransferListener listener = new MyTransferListener(this.machine, this.client, file.length());

            if (restart_attempts >= 1 && this.client.isResumeSupported()) {
                try {

                    this.client.sendCustomCommand("type i");

                    Long uploaded_size = this.client.fileSize(destination + "/" + file.getName());

                    this.machine.log_info("Resuming from byte number: " + uploaded_size);
                    this.client.upload(file, uploaded_size, listener);
                } catch (Exception e) {
                    this.machine.log_warning("Can not resume, restart upload: " + e.getMessage());
                    this.client.upload(file, listener);
                }
            } else {
                this.client.upload(file, listener);
            }

        } catch (FTPIllegalReplyException |
                FTPException | FTPDataTransferException | FTPAbortedException | IOException e) {

            this.machine.log_error(e.getMessage());
            this.machine.log_warning("Restarting upload in 10 seconds");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            this.reconnect();
            this.upload(destination, local_file, restart_attempts + 1);
        }
    }
}