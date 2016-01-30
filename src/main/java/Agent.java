import agent.BackupJob;
import agent.MachineStats;
import agent.Updater;
import commons.Machine;
import commons.exceptions.OSSUSNoAPIConnectionException;
import commons.exceptions.OSSUSNoFTPServerConnection;

import java.text.ParseException;

public class Agent {

    public static void main(final String[] args) throws ParseException, OSSUSNoAPIConnectionException {

        String settingsLocation;

        if (args.length > 0) {
            settingsLocation = args[0];
        } else {
            String home = System.getProperty("user.home");
            settingsLocation = home + "/.ossus_settings.json";
        }

        Machine machine = Agent.buildMachineFromSettings(settingsLocation);

        if (machine.isBusy()) {
            machine.log_warning("Agent: Machine busy, skipping!");
            System.exit(0);
        }

        machine.log_info("Checking if machine is busy, and set to busy if available");

        if (!machine.isBusy() && machine.changesBusyStatus(true)) {
            machine.log_info("Agent: Set busy!");

            if (!machine.isBusy()) {
                machine.log_error("Agent: Something is wrong, the status should be busy by now.. but is not?");
                return;
            }

            try {

                machine.log_info("Starting to report machine stats");
                reportMachineStats(machine);

                machine.log_info("Starting updater");
                new Updater(machine).run();

                machine.log_info("Starting to check for backup jobs to run");
                new BackupJob(machine).runBackup();

            } catch (OSSUSNoAPIConnectionException e) {
                System.exit(1);
            } catch (OSSUSNoFTPServerConnection e) {
                machine.log_error(e.getMessage());
            } finally {
                if (machine.changesBusyStatus(false)) {
                    machine.log_info("Agent: Set not busy!");
                } else {
                    machine.log_error("Agent: Something is wrong! The status should not have been not busy.");
                }
            }
        } else {
            machine.log_warning("Agent: Machine busy, skipping!");
            System.exit(0);
        }
        System.exit(0);
    }

    private static void reportMachineStats(final Machine machine) throws OSSUSNoAPIConnectionException {
        try {
            MachineStats machinestats = new MachineStats(machine);
            machinestats.save();
            machine.log_info("Done reporting machine stats");
        } catch (Exception e) {
            machine.log_error("Failed to report machine stats");
            machine.log_error(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Machine buildMachineFromSettings(final String settingsLocation) {

        Machine machine = null;

        try {
            machine = Machine.buildFromSettings(settingsLocation);
        } catch (Exception e) {
            System.err.println("Failed to load settings, is this a valid path? (" + settingsLocation + ")");
            System.exit(1);
        }

        return machine;
    }

}