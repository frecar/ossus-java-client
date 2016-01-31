package commons;

import org.json.simple.JSONObject;
import commons.exceptions.OSSUSNoAPIConnectionException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public abstract class GenericUpdater {

    public static final int BYTE_SIZE_DOWNLOAD = 8192;
    protected final Machine machine;
    private final APIHandler apiHandler;

    protected GenericUpdater(final Machine machine) {
        this.machine = machine;
        this.apiHandler = machine.apiHandler;
    }

    protected abstract Version currentVersion();

    protected abstract Version selectedVersion();

    protected abstract String outFileName();

    protected abstract String versionURL();

    protected abstract URL downloadLink(final Version v)
            throws OSSUSNoAPIConnectionException;

    protected abstract void downloadDone(
            final Version newVersion
    ) throws OSSUSNoAPIConnectionException;

    public final void run() throws OSSUSNoAPIConnectionException {

        machine.logInfoMessage("Fetching version information from server");
        Version selected = getSelectedVersion();

        if (!machine.autoUpdate && currentVersion().equals(selectedVersion())) {
            machine.logInfoMessage("This machine is set to not auto update");
            machine.logInfoMessage("This machine is running the selected version "
                    + selectedVersion());
            return;
        }

        if (selected == null) {
            machine.logErrorMessage("Failed to fetch version information");
            return;
        }

        if (!selected.equals(currentVersion())) {
            machine.logInfoMessage("New version detected: " + selected.name);
            if (downloadVersion(selected)) {
                downloadDone(selected);
                machine.logInfoMessage("Set new " + selected.name);
            } else {
                machine.logErrorMessage("New version was (probably) not downloaded...");
            }
        } else {
            machine.logInfoMessage("Current updater up to date, no need to update");
        }

        machine.logInfoMessage("Completed update check of updater");

    }

    private Version getSelectedVersion() throws OSSUSNoAPIConnectionException {
        if (!machine.autoUpdate) {
            return selectedVersion();
        }

        final List<JSONObject> jsonData = apiHandler.getApiData(versionURL());

        if (jsonData.size() != 1) {
            return null;
        }

        return Version.buildFromJson(
                (JSONObject) jsonData.get(0).get("client_version")
        );
    }

    private boolean downloadVersion(final Version version)
            throws OSSUSNoAPIConnectionException {
        machine.logInfoMessage("Downloading new version");
        machine.logInfoMessage("Current version: " + currentVersion());
        machine.logInfoMessage("New version: " + version);

        URL jarURL = downloadLink(version);
        if (jarURL != null) {
            try {
                readAndSaveJar(jarURL);
            } catch (Exception e) {
                machine.logErrorMessage(e.toString());
                return false;
            }
        } else {
            machine.logErrorMessage("Download link was null. strange.");
            return false;
        }
        return true;
    }

    private void readAndSaveJar(
            final URL jarURL
    ) throws IOException, OSSUSNoAPIConnectionException {

        if (jarURL == null) {
            throw new IOException("URL argument is null");
        }

        BufferedOutputStream fileOut = createUpdaterFile();
        BufferedInputStream in = new BufferedInputStream(jarURL.openStream());

        machine.logInfoMessage("Starting download new version of: " + outFileName());

        int read, total = 1;
        byte[] buff = new byte[BYTE_SIZE_DOWNLOAD];   // todo: whats a good size
        while ((read = in.read(buff)) != -1) {
            total += read;
            fileOut.write(buff, 0, read);
        }

        fileOut.flush();
        machine.logInfoMessage("Done downloading new version of: " + outFileName());
        machine.logInfoMessage("Total of " + total + " bytes read");
        fileOut.close();
        in.close();

    }

    private BufferedOutputStream createUpdaterFile()
            throws IOException, OSSUSNoAPIConnectionException {

        final File jarFile = new File(outFileName());

        if (!jarFile.exists()) {
            machine.logInfoMessage("No file exists..: " + outFileName());
        } else {
            machine.logInfoMessage("Delete old version of: " + outFileName());
            if (new File(outFileName()).delete()) {
                machine.logInfoMessage(outFileName() + " deleted");
            } else {
                machine.logErrorMessage(outFileName() + " could not be deleted");
            }
        }

        machine.logInfoMessage("Creating local file: " + outFileName());

        if (!jarFile.createNewFile()) {
            throw new IOException("Could not create file " + jarFile);
        }

        machine.logInfoMessage("local file created: " + outFileName());
        return new BufferedOutputStream(new FileOutputStream(jarFile));
    }
}
