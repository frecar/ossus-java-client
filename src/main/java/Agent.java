import agent.BackupJob;
import agent.MachineStats;
import agent.Updater;
import agent.Uptime;
import commons.Machine;
import commons.exceptions.OSSUSNoAPIConnectionException;
import commons.exceptions.OSSUSNoFTPServerConnection;

import java.text.ParseException;


public class Agent {

    // Timeout Agent after 3 hours
    static final int AGENT_TIMEOUT = 10 * 60 * 60 * 1000;

    public static void main(
            final String[] args
    ) {

        final Machine machine = Agent.getMachine(args);

        Thread agentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runMain(machine);
            }
        });
        agentThread.start();

        long endTimeMillis = System.currentTimeMillis() + AGENT_TIMEOUT;
        while (agentThread.isAlive()) {
            try {
                if (System.currentTimeMillis() > endTimeMillis) {
                    machine.logErrorMessage("Timed out after " + AGENT_TIMEOUT + "ms");
                    System.exit(0);
                }
                try {
                    runProgressReport(machine);
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {
                    machine.logErrorMessage("Failed to sleep main thread for 10 seconds");
                    ignored.printStackTrace();
                    System.exit(0);
                }
            } catch (OSSUSNoAPIConnectionException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
        try {
            machine.logInfoMessage("Main thread completed");
        } catch (OSSUSNoAPIConnectionException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static void runProgressReport(
            final Machine machine
    ) throws OSSUSNoAPIConnectionException {
        long seconds = System.currentTimeMillis() / 1000 - machine.session;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        String logText = seconds + " seconds";
        if (minutes > 3) {
            logText = minutes + " minutes";
        }
        if (minutes > 60) {
            logText = hours + " hours";
        }
        machine.logInfoMessage("Agent has been running for " + logText);
    }

    public static void runMain(
            final Machine machine
    ) {
        try {
            if (machine.isBusy()) {
                machine.logWarningMessage("Agent: Machine busy, skipping!");
                System.exit(0);
            }

            Agent.reportUptime(machine);

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
                    e.printStackTrace();
                } catch (OSSUSNoFTPServerConnection | ParseException e2) {
                    machine.logErrorMessage(e2.getMessage());
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
            }
            machine.logInfoMessage("runMain thread completed");
        } catch (OSSUSNoAPIConnectionException e) {
            System.err.println("Error occured: " + e.getMessage());
        }
    }

    private static Machine getMachine(
            final String[] args
    ) {
        String settingsLocation;
        if (args.length > 0) {
            settingsLocation = args[0];
        } else {
            String home = System.getProperty("user.home");
            settingsLocation = home + "/.ossus_settings.json";
        }
        return Agent.buildMachineFromSettings(settingsLocation);
    }

    private static void reportUptime(
            final Machine machine
    ) throws
            OSSUSNoAPIConnectionException {
        machine.logInfoMessage("Starting to report uptime");
        long uptime = Uptime.getSystemUptime(machine);
        machine.logInfoMessage("Reporting " + uptime + " minutes uptime");
        machine.apiHandler.getApiData("machines/" + machine.id + "/set_uptime/" + uptime);
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
            System.exit(0);
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
            System.exit(0);
        }
        return machine;
    }
}
