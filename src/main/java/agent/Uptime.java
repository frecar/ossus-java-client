package agent;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Uptime {


    private static long parseWindowsTime(String dateFormat, String line) {
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        Date boottime;
        try {
            boottime = format.parse(line);
        } catch (ParseException e) {
            return 0;
        }
        return (System.currentTimeMillis() - boottime.getTime()) / 1000;
    }

    private static long getSystemUptimeWindows() throws IOException {
        long uptime = 0;

        List<String> parseFormst = new ArrayList<>();
        parseFormst.add("'Statistics since' MM/dd/yyyy hh:mm:ss a");
        parseFormst.add("'Statistics since' MM/dd/yyyy hh:mm:ss");
        parseFormst.add("'Statistics since' MM/dd/yyyy hh:mm");
        parseFormst.add("'Statistics since' dd/MM/yyyy hh:mm:ss a");
        parseFormst.add("'Statistics since' dd/MM/yyyy hh:mm:ss");
        parseFormst.add("'Statistics since' dd/MM/yyyy hh:mm");

        Process uptimeProc = Runtime.getRuntime().exec("net stats srv");

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(uptimeProc.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String data = "";
            String line;
            while (in.readLine() != null) {
                line = in.readLine();
                if (line.startsWith("Statistics since")) {
                    data = line;
                }
            }
            if (!data.equals("")) {
                for (String dateFormat : parseFormst) {
                    uptime = parseWindowsTime(dateFormat, data);
                    if (uptime > 0) {
                        break;
                    }
                }
            }
        }
        return uptime;
    }

    private static long getSystemUptimeOther() throws IOException {
        long uptime = 0;
        Process uptimeProc = Runtime.getRuntime().exec("uptime");

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
        }
        return uptime;
    }

    public static long getSystemUptime() throws IOException, ParseException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return getSystemUptimeWindows();
        } else {
            return getSystemUptimeOther();
        }
    }
}
