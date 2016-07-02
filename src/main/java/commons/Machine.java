package commons;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import commons.exceptions.OSSUSNoAPIConnectionException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;


public final class Machine {

    public static final int MILLISECONDS_DIVIDER = 1000;
    public final String id;

    private Version currentUpdaterVersion;
    public final Version selectedUpdaterVersion;
    public final boolean autoUpdate;

    public final String serverIP;
    public final String apiUser;
    public final String apiToken;
    public String agentFolder;
    public String localTempFolder;
    public final String osSystem;
    public final String mysqlDumpCommand;
    public final String downloadsClient;
    public final Boolean forceAction;
    public final Boolean runInstall;

    public long session;

    public final Log log;

    public final APIHandler apiHandler;

    private Machine(
            final Map<String, String> settings
    ) throws ParseException, IOException, OSSUSNoAPIConnectionException {

        this.serverIP = settings.get(ApiTrans.MACHINE_SERVER_IP.value);
        this.id = settings.get(ApiTrans.MACHINE_ID.value);
        this.apiUser = settings.get(ApiTrans.MACHINE_API_USER.value);
        this.apiToken = settings.get(ApiTrans.MACHINE_API_TOKEN.value);
        this.osSystem = settings.get(ApiTrans.MACHINE_OS_SYSTEM.value);
        this.downloadsClient = settings.get(ApiTrans.MACHINE_DOWNLOADS_CLIENT.value);
        this.mysqlDumpCommand = settings.get(ApiTrans.MACHINE_MYSQL_DUMP.value);
        this.agentFolder = settings.get(ApiTrans.MACHINE_AGENT_FOLDER.value);
        this.localTempFolder = settings.get(ApiTrans.MACHINE_LOCAL_TEMP_FOLDER.value);
        this.session = System.currentTimeMillis() / MILLISECONDS_DIVIDER;

        this.apiHandler = new APIHandler(this.serverIP + "/api/", apiUser, apiToken);
        this.log = new Log(this, apiHandler, this.id);


        if (!this.localTempFolder.endsWith(System.getProperty("file.separator"))) {
            this.localTempFolder += System.getProperty("file.separator");
        }

        if (new File(this.localTempFolder).mkdirs()) {
            logErrorMessage("Created local temp folder: " + localTempFolder);
        }

        if (!this.agentFolder.endsWith(System.getProperty("file.separator"))) {
            this.agentFolder += System.getProperty("file.separator");
        }

        if (new File(this.agentFolder).mkdirs()) {
            logErrorMessage("Created local agent folder: " + agentFolder);
        }

        this.forceAction = settings.get(ApiTrans.MACHINE_FORCE_ACTION.value).equals("1");

        List<JSONObject> obj = apiHandler.getApiData("machines/" + this.id);
        JSONObject data = (JSONObject) obj.get(0).get("machine");

        this.currentUpdaterVersion = Version.buildFromJson((JSONObject) data.get(
                ApiTrans.MACHINE_CURRENT_UPDATER_VERSION.value));
        this.selectedUpdaterVersion = Version.buildFromJson((JSONObject) data.get(
                ApiTrans.MACHINE_SELECTED_UPDATER_VERSION.value));

        this.autoUpdate = (Boolean) data.get(ApiTrans.MACHINE_AUTO_UPDATE.value);
        this.runInstall = (Boolean) data.get(ApiTrans.MACHINE_RUN_INSTALL.value);

        this.setMachineExternalIp(this.getExternalIP());

    }

    public boolean isBusy() throws OSSUSNoAPIConnectionException {
        List<JSONObject> obj = apiHandler.getApiData("machines/" + this.id);
        JSONObject data = (JSONObject) obj.get(0).get("machine");

        if (data == null) {
            throw new OSSUSNoAPIConnectionException("Can't read busy status");
        }

        Boolean response = (Boolean) data.get(ApiTrans.MACHINE_IS_BUSY.value);

        if (response == null) {
            throw new OSSUSNoAPIConnectionException("Can't read busy status");
        } else {
            return response;
        }
    }

    public String getExternalIP() throws IOException {
        URL url = new URL(serverIP + "/api/ip");
        InputStream stream = url.openStream();
        InputStreamReader inputStreamReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(inputStreamReader);
        String result = in.readLine();
        in.close();
        inputStreamReader.close();
        stream.close();
        return result;
    }

    public static Machine buildFromSettings(final String settingsLocation) throws Exception {

        Map<String, String> settings = new HashMap<>();

        JSONParser parser = new JSONParser();

        FileInputStream fileInputStream = null;
        InputStreamReader inputStreamReader = null;

        JSONObject jsonObject = null;

        try {
            fileInputStream = new FileInputStream(settingsLocation);
            inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            jsonObject = (JSONObject) parser.parse(inputStreamReader);
        } finally {
            if (inputStreamReader != null) {
                inputStreamReader.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }

        final List<ApiTrans> settingsFields = asList(
                ApiTrans.MACHINE_ID,
                ApiTrans.MACHINE_SERVER_IP,
                ApiTrans.MACHINE_API_USER,
                ApiTrans.MACHINE_API_TOKEN,
                ApiTrans.MACHINE_FORCE_ACTION,
                ApiTrans.MACHINE_LOCAL_TEMP_FOLDER,
                ApiTrans.MACHINE_MYSQL_DUMP,
                ApiTrans.MACHINE_AGENT_FOLDER
        );

        for (ApiTrans field : settingsFields) {
            settings.put(field.value, (String) jsonObject.get(field.value));
        }

        return new Machine(settings);

    }

    public Version getCurrentUpdaterVersion() {
        return currentUpdaterVersion;
    }

    public void setMachineExternalIp(final String ipAddress) throws OSSUSNoAPIConnectionException {
        apiHandler.getApiData("machines/" + id + "/set_machine_external_ip/" + ipAddress);
    }

    public boolean changesBusyStatus(final boolean busy) throws OSSUSNoAPIConnectionException {
        final String isBusy = busy ? "1" : "0";
        final List<JSONObject> s = apiHandler.getApiData("machines/"
                + id + "/set_busy_updating/"
                + isBusy + "/session/" + this.session + "/"
        );

        Boolean response = (Boolean) s.get(0).get("changed_status");

        if (response == null) {
            return false;
        } else {
            return response;
        }

    }

    public void setCurrentUpdaterVersion(
            final Version currentUpdaterVersion
    ) throws OSSUSNoAPIConnectionException {
        this.currentUpdaterVersion = currentUpdaterVersion;
        apiHandler.getApiData(
                "machines/" + id + "/set_updater_version/" + currentUpdaterVersion.id
        );
    }

    public void logInfoMessage(final String text) throws OSSUSNoAPIConnectionException {
        this.log.logInfoMessage(text);
    }

    public void logErrorMessage(final String text) throws OSSUSNoAPIConnectionException {
        this.log.logErrorMessage(text);
    }

    public void logWarningMessage(final String text) throws OSSUSNoAPIConnectionException {
        this.log.logWarningMessage(text);
    }

}

