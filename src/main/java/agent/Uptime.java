package agent;

import commons.Machine;
import commons.exceptions.OSSUSNoAPIConnectionException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Uptime {

    public static long parseWindowsTime(
            final Machine machine,
            final String dateFormat,
            final String line
    ) throws OSSUSNoAPIConnectionException {
        final SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        final Date boottime;
        try {
            boottime = format.parse(line);
        } catch (ParseException e) {
            return 0;
        }
        return TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - boottime.getTime());
    }

    private static long getSystemUptimeWindows(
            final Machine machine
    ) throws OSSUSNoAPIConnectionException {
        long uptime = 0;

        List<String> parseFormats = new ArrayList<>();
        parseFormats.add("'Statistics since' dd.MM.yyyy hh:mm");
        parseFormats.add("'Statistics since' dd.MM.yyyy hh:mm a");
        parseFormats.add("'Statistics since' dd.MM.yyyy hh:mm:ss");
        parseFormats.add("'Statistics since' dd.MM.yyyy hh:mm:ss a");

        parseFormats.add("'Statistics since' MM/dd/yyyy hh:mm");
        parseFormats.add("'Statistics since' MM/dd/yyyy hh:mm a");
        parseFormats.add("'Statistics since' MM/dd/yyyy hh:mm:ss");
        parseFormats.add("'Statistics since' MM/dd/yyyy hh:mm:ss a");

        parseFormats.add("'Statistikk siden' dd.MM.yyyy hh:mm");
        parseFormats.add("'Statistikk siden' dd.MM.yyyy hh:mm a");
        parseFormats.add("'Statistikk siden' dd.MM.yyyy hh:mm:ss");
        parseFormats.add("'Statistikk siden' dd.MM.yyyy hh:mm:ss a");

        parseFormats.add("'Statistikk siden' MM/dd/yyyy hh:mm");
        parseFormats.add("'Statistikk siden' MM/dd/yyyy hh:mm a");
        parseFormats.add("'Statistikk siden' MM/dd/yyyy hh:mm:ss");
        parseFormats.add("'Statistikk siden' MM/dd/yyyy hh:mm:ss a");

        Process uptimeProc = null;
        try {
            uptimeProc = Runtime.getRuntime().exec("net stats srv");
        } catch (IOException e) {
            machine.logErrorMessage("Failed to execute: net stats srv command");
            machine.logErrorMessage(e.getMessage());
        }

        try {
            if (uptimeProc != null) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(uptimeProc.getInputStream(), StandardCharsets.UTF_8)
                )) {
                    String data;
                    String line;
                    while (true) {
                        line = in.readLine();
                        if ("".equals(line)) {
                            continue;
                        }
                        if (line.startsWith("Statistics since")
                                || line.startsWith("Statistikk siden")) {
                            data = line;
                            break;
                        }
                    }
                    if (!data.equals("")) {
                        for (String dateFormat : parseFormats) {
                            uptime = parseWindowsTime(machine, dateFormat, data);
                            if (uptime > 0) {
                                break;
                            }
                        }
                    }
                    if (uptime == 0) {
                        machine.logErrorMessage("Unable to parse uptime information: " + data);
                    }
                }
            } else {
                machine.logErrorMessage(
                        "Failed to parse uptime information, proc information unavailable"
                );
            }
        } catch (IOException e) {
            machine.logErrorMessage("Failed to parse uptime information");
            machine.logErrorMessage(e.getMessage());
        }
        return uptime;
    }

    private static long getSystemUptimeOther(
            final Machine machine
    ) throws OSSUSNoAPIConnectionException {
        long uptime = 0;
        Process uptimeProc = null;
        try {
            uptimeProc = Runtime.getRuntime().exec("uptime");
        } catch (IOException e) {
            machine.logErrorMessage("Failed to execute uptime command");
            machine.logErrorMessage(e.getMessage());
        }

        if (uptimeProc != null) {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(uptimeProc.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line = in.readLine();
                if (line != null) {
                    Pattern parse = Pattern.compile("((\\d+) days,)?[\\s]*(\\d+):(\\d+),");
                    Matcher matcher = parse.matcher(line);
                    if (matcher.find()) {
                        String parsedDays = matcher.group(2);
                        String parsedHours = matcher.group(3);
                        String parsedMinutes = matcher.group(4);
                        int days = parsedDays != null ? Integer.parseInt(parsedDays) : 0;
                        int hours = parsedHours != null ? Integer.parseInt(parsedHours) : 0;
                        int minutes = parsedMinutes != null ? Integer.parseInt(parsedMinutes) : 0;
                        uptime = minutes + (hours * 60) + (days * 1440);
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        } else {
            machine.logErrorMessage(
                    "Failed to parse uptime information, proc information unavailable"
            );
        }
        return uptime;
    }

    public static long getSystemUptime(
            final Machine machine
    ) throws OSSUSNoAPIConnectionException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return getSystemUptimeWindows(machine);
        } else {
            return getSystemUptimeOther(machine);
        }
    }
}
