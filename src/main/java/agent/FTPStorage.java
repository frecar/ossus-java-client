package agent;

import commons.Machine;
import commons.exceptions.OSSUSNoAPIConnectionException;
import commons.exceptions.OSSUSNoFTPServerConnection;
import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;

import java.io.File;
import java.io.IOException;

public final class FTPStorage {
    public FTPClient client;
    private String homeFolder;
    private Machine machine;

    private String host;
    private String username;
    private String password;

    public FTPStorage(
            final Machine machine,
            final String host,
            final String username,
            final String password,
            final String folder
    ) throws
            OSSUSNoAPIConnectionException,
            OSSUSNoFTPServerConnection {
        this.host = host;
        this.username = username;
        this.password = password;
        this.homeFolder = folder;
        this.machine = machine;
        this.reconnect();
    }

    public void reconnect() throws
            OSSUSNoAPIConnectionException,
            OSSUSNoFTPServerConnection {
        try {
            this.machine.logInfoMessage("Connecting to FTP Server");
            this.client = new FTPClient();
            this.client.connect(this.host);
            this.client.login(this.username, this.password);
            this.machine.logInfoMessage("Successfully connected to FTP Server");
        } catch (Exception e) {
            this.machine.logErrorMessage(e.getMessage());
            this.machine.logErrorMessage("FTP Server is unavailable,"
                    + " terminating this session and set machine not busy");
            throw new OSSUSNoFTPServerConnection("FTP Server is unavailable");
        }
    }

    public void createFolder(
            final String folder
    ) throws OSSUSNoAPIConnectionException {
        try {
            this.client.changeDirectory(this.homeFolder);
        } catch (Exception e2) {
            this.machine.logErrorMessage(e2.getMessage());
        }

        String[] path = folder.split("/");
        String midPath = "";

        for (String p : path) {
            midPath += p + "/";

            try {
                this.client.changeDirectory(midPath);
            } catch (Exception e) {
                try {
                    this.client.createDirectory(midPath);
                } catch (Exception e1) {
                    this.machine.logErrorMessage(e1.getMessage());
                }
                this.machine.logWarningMessage(e.getMessage());
                this.machine.logInfoMessage("Creating folder " + midPath);
            }
        }
    }

    public void upload(
            final String destination,
            final String localFile,
            final int restartAttempts
    ) throws OSSUSNoAPIConnectionException, OSSUSNoFTPServerConnection {
        int maxRestartAttempts = 3;

        try {
            this.machine.logInfoMessage("Starting to upload "
                    + localFile + " to " + destination);

            if (restartAttempts > 0) {
                this.machine.logWarningMessage(". This is the "
                        + (restartAttempts + 1) + " attempt to for this upload");
            }

            if (restartAttempts > maxRestartAttempts) {
                this.machine.logErrorMessage("Aborting FTP upload");
                this.machine.logErrorMessage("Attempted to start upload more than "
                        + maxRestartAttempts + " times");
                throw new OSSUSNoFTPServerConnection("No connection to FTP server");
            }

            this.createFolder(destination);
            this.client.changeDirectory(destination);

            File file = new File(localFile);
            MyTransferListener listener = new MyTransferListener(
                    this.machine,
                    this.client,
                    file.length()
            );

            if (restartAttempts >= 1 && this.client.isResumeSupported()) {
                try {

                    this.client.sendCustomCommand("type i");

                    Long uploadedSize = this.client.fileSize(
                            destination + "/" + file.getName());

                    this.machine.logInfoMessage(
                            "Resuming from byte number: " + uploadedSize);
                    this.client.upload(file, uploadedSize, listener);
                } catch (Exception e) {
                    this.machine.logWarningMessage(
                            "Can not resume, restart upload: " + e.getMessage());
                    this.client.upload(file, listener);
                }
            } else {
                this.client.upload(file, listener);
            }

        } catch (FTPIllegalReplyException
                | FTPException | FTPDataTransferException
                | FTPAbortedException | IOException e) {

            this.machine.logErrorMessage(e.getMessage());
            this.machine.logWarningMessage("Restarting upload in 10 seconds");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            this.reconnect();
            this.upload(destination, localFile, restartAttempts + 1);
        }
    }
}
