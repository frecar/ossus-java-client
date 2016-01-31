import agent.BackupJob;
import agent.MachineStats;
import agent.Updater;
import commons.Machine;
import commons.exceptions.OSSUSNoAPIConnectionException;
import commons.exceptions.OSSUSNoFTPServerConnection;

import java.text.ParseException;

public class Agent {

    public static void main(
            final String[] args
    ) throws
            ParseException,
            OSSUSNoAPIConnectionException {

        String settingsLocation;

        if (args.length > 0) {
            settingsLocation = args[0];
        } else {
            String home = System.getProperty("user.home");
            settingsLocation = home + "/.ossus_settings.json";
        }

        Machine machine = Agent.buildMachineFromSettings(settingsLocation);

        if (machine.isBusy()) {
            machine.logWarningMessage("Agent: Machine busy, skipping!");
            System.exit(0);
        }

        machine.logInfoMessage("Checking if machine is busy, and set to busy if available");

        if (!machine.isBusy() && machine.changesBusyStatus(true)) {
            machine.logInfoMessage("Agent: Set busy!");

            if (!machine.isBusy()) {
                machine.logErrorMessage("Agent: Something is wrong, "
                        + "the status should be busy by now.. but is not?");
                return;
            }

            try {

                machine.logInfoMessage("Starting to report machine stats");
                reportMachineStats(machine);

                machine.logInfoMessage("Starting updater");
                new Updater(machine).run();

                machine.logInfoMessage("Starting to check for backup jobs to run");
                new BackupJob(machine).runBackup();

            } catch (OSSUSNoAPIConnectionException e) {
                System.exit(1);
            } catch (OSSUSNoFTPServerConnection e) {
                machine.logErrorMessage(e.getMessage());
            } finally {
                if (machine.changesBusyStatus(false)) {
                    machine.logInfoMessage("Agent: Set not busy!");
                } else {
                    machine.logErrorMessage("Agent: Something is wrong! "
                            + "The status should not have been not busy.");
                }
            }
        } else {
            machine.logWarningMessage("Agent: Machine busy, skipping!");
            System.exit(0);
        }
        System.exit(0);
    }

    private static void reportMachineStats(
            final Machine machine
    ) throws
            OSSUSNoAPIConnectionException {

        try {
            MachineStats machinestats = new MachineStats(machine);
            machinestats.save();
            machine.logInfoMessage("Done reporting machine stats");
        } catch (Exception e) {
            machine.logErrorMessage("Failed to report machine stats");
            machine.logErrorMessage(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Machine buildMachineFromSettings(
            final String settingsLocation
    ) {

        Machine machine = null;

        try {
            machine = Machine.buildFromSettings(settingsLocation);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return machine;
    }
}