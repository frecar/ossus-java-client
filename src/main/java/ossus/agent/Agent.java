package ossus.agent;

import ossus.commons.Machine;
import ossus.commons.exceptions.OSSUSNoAPIConnectionException;
import ossus.install.Installer;

import java.text.ParseException;

public class Agent {

    private static void InstallSigarLibrary(Machine machine) throws OSSUSNoAPIConnectionException {

        if (machine.run_install) {
            machine.log_info("Installing Sigar library");
            try {
                new Installer().runInstall();
            } catch (Exception e) {

                try {

                    e.printStackTrace();
                    machine.log_error("Error running install");

                    if (machine.changesBusyStatus(false)) {
                        machine.log_info("Agent: Set not busy!");
                    } else {
                        machine.log_error("Agent: Something is wrong, " +
                                "the install failed and not I'm not able to set busy to false.");
                    }

                } catch (Exception e2) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            machine.log_info("Successfully installed Sigar library");
        }

    }

    private static Machine buildMachineFromSettings(String settingsLocation) {

        Machine machine = null;

        try {
            machine = Machine.buildFromSettings(settingsLocation);
        } catch (Exception e) {
            System.err.println("Failed to load settings, is this a valid path? (" + settingsLocation + ")");
            System.exit(1);
        }

        if (machine == null) {
            System.out.println("Failed to load settings and build machine object");
            System.exit(1);
        }

        return machine;

    }


    public static void main(String[] args) throws ParseException, OSSUSNoAPIConnectionException {

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
            return;
        }

        machine.log_info("Checking if machine is busy, and set to busy if available");

        if (!machine.isBusy() && machine.changesBusyStatus(true)) {
            machine.log_info("Agent: Set busy!");

            if (!machine.isBusy()) {
                machine.log_error("Agent: Something is wrong, the status should be busy by now.. but is not?");
                return;
            }

            try {

                Agent.InstallSigarLibrary(machine);

                try {
                    machine.log_info("Starting to report machine stats");
                    MachineStats machinestats = new MachineStats(machine);
                    machinestats.save();
                    machine.log_info("Done reporting machine stats");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }

                machine.log_info("Starting updater");
                new Updater(machine).run();

                machine.log_info("Starting to check for backup jobs to run");
                new BackupJob(machine).runBackup();

            } catch (Exception e) {
                machine.log_error(e.toString());
            } finally {
                if (machine.changesBusyStatus(false)) {
                    machine.log_info("Agent: Set not busy!");
                } else {
                    machine.log_error("Agent: Something is wrong! The status should have changed back to not busy."
                    );
                }
            }
        } else {
            System.exit(0);
        }
        System.exit(0);
    }
}