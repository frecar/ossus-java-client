package ossus.commons;

import org.json.simple.JSONObject;
import ossus.commons.exceptions.OSSUSNoAPIConnectionException;

/*
    TODO: The server could send information saying wheter teh version running is the newest/current or not, together
    with the other Machine / version data when initializing.
 */

import java.io.*;
import java.net.URL;
import java.util.List;

abstract public class GenericUpdater {

    protected final Machine machine;
    private final APIHandler api_handler;

    protected GenericUpdater(Machine machine) {
        this.machine = machine;
        this.api_handler = machine.apiHandler;
    }

    protected abstract Version current_version();

    protected abstract Version selected_version();

    protected abstract String out_file_name();

    protected abstract String version_url();

    protected abstract URL download_link(Version v) throws OSSUSNoAPIConnectionException;

    protected abstract void download_done(Version new_version);

    public void run() throws OSSUSNoAPIConnectionException {

        if (!machine.auto_update && current_version().equals(selected_version())) {
            return;
        }

        machine.log_info("Fetching version information from server");
        Version selected = getSelectedVersion();

        if (selected == null) {
            machine.log_error("Failed to fetch version information");
            return;
        }

        if (!selected.equals(current_version())) {
            machine.log_info("New version detected: " + selected.name);
            if (download_version(selected)) {
                download_done(selected);
                machine.log_info("Set new " + selected.name);
            } else {
                machine.log_error("New version was (probably) not downloaded...");
            }
        } else {
            machine.log_info("Current updater up to date, no need to update");
        }

        machine.log_info("Completed update check of updater");

    }

    private Version getSelectedVersion() throws OSSUSNoAPIConnectionException {
        if (!machine.auto_update) return selected_version();

        List<JSONObject> json_data = api_handler.get_api_data(version_url());
        if (json_data.size() != 1) {
            return null;
        }

        JSONObject version_data = (JSONObject) json_data.get(0).get("client_version");
        return Version.buildFromJson(version_data, null);
    }

    private boolean download_version(Version version) throws OSSUSNoAPIConnectionException {
        machine.log_info("Downloading new version");
        machine.log_info("Current version: " + current_version());
        machine.log_info("New version: " + version);

        URL jar_url = download_link(version);
        if (jar_url != null) try {
            readAndSaveJar(jar_url);
        } catch (Exception e) {
            machine.log_error(e.toString());
            return false;
        }
        else {
            machine.log_error("Download link was null. strange.");
            return false;
        }
        return true;
    }

    private void readAndSaveJar(URL jar_url) throws IOException, OSSUSNoAPIConnectionException {
        if (jar_url == null) throw new IOException("URL argument is null");  // not really an ioexception, fix?

        BufferedOutputStream file_out = create_updater_file();
        BufferedInputStream in = new BufferedInputStream(jar_url.openStream());

        machine.log_info("Starting download new version of: " + out_file_name());

        int read, total = 1;
        byte[] buff = new byte[8192];   // todo: whats a good size
        while ((read = in.read(buff)) != -1) {
            total += read;
            file_out.write(buff, 0, read);
        }


        file_out.flush();
        machine.log_info("Done downloading new version of: " + out_file_name() + ". Total of " + total + "bytes read");
        file_out.close();
        in.close();

    }

    private BufferedOutputStream create_updater_file() throws IOException, OSSUSNoAPIConnectionException {

        File jar_file = new File(out_file_name());

        if (!jar_file.exists()) {
            machine.log_info("No file exists..: " + out_file_name());
        } else {
            machine.log_info("Delete old version of: " + out_file_name());
            if (new File(out_file_name()).delete()) {
                machine.log_info(out_file_name() + " deleted");
            } else {
                machine.log_error(out_file_name() + " could not be deleted");
            }
        }

        machine.log_info("Creating local file: " + out_file_name());
        boolean created = jar_file.createNewFile();
        if (!created) throw new IOException("Could not create file " + jar_file);

        machine.log_info("local file created: " + out_file_name());
        return new BufferedOutputStream(new FileOutputStream(jar_file));
    }

}
