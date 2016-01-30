package agent;

import commons.GenericUpdater;
import commons.Machine;
import commons.Version;
import commons.exceptions.OSSUSNoAPIConnectionException;

import java.net.MalformedURLException;
import java.net.URL;

public final class Updater extends GenericUpdater {

    private final String updaterFileName = "Updater.jar";

    public Updater(final Machine machine) {
        super(machine);
    }

    protected Version currentVersion() {
        return machine.getCurrentUpdaterVersion();
    }

    protected Version selectedVersion() {
        return machine.selectedUpdaterVersion;
    }

    protected String versionURL() {
        return "client_versions/"
                + (machine.autoUpdate ? "current_updater/" : selectedVersion().name);
    }

    protected String outFileName() {
        return machine.agentFolder + updaterFileName;
    }

    protected URL downloadLink(
            final Version v
    ) throws OSSUSNoAPIConnectionException {
        try {
            return new URL(v.updaterLink);
        } catch (MalformedURLException e) {
            machine.logErrorMessage(e.toString());
            return null;
        }
    }

    protected void downloadDone(
            final Version newVersion
    ) {
        machine.setCurrentUpdaterVersion(newVersion);
    }
}
