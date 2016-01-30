package agent;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import commons.Machine;
import commons.exceptions.OSSUSNoAPIConnectionException;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public final class MachineStats {

    final Machine machine;
    double cpuSystem;
    double cpuUser;

    long memFree;
    long memUsed;

    double[] s = {0.0, 0.0, 0.0};

    public static double getProcessCpuLoad() throws Exception {

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
        AttributeList list = mbs.getAttributes(name, new String[]{"ProcessCpuLoad"});

        if (list.isEmpty()) {
            return Double.NaN;
        }

        Attribute att = (Attribute) list.get(0);
        Double value = (Double) att.getValue();

        // usually takes a couple of seconds before we get real values
        if (value == -1.0) {
            return Double.NaN;
        }
        // returns a percentage value with 1 decimal point precision
        return (int) (value * 1000) / 10.0;
    }

    public MachineStats(
            final Machine machine
    ) throws OSSUSNoAPIConnectionException {

        this.machine = machine;

        try {

            cpuSystem = getProcessCpuLoad();
            cpuUser = 0;
            memFree = 0;
            memUsed = 0;

        } catch (Exception e) {
            this.machine.logErrorMessage(e.getMessage());
        }

    }

    public void save() throws OSSUSNoAPIConnectionException {
        Map<String, String> map = new HashMap<>();
        map.put("cpu_system", "" + Math.round(cpuSystem * 100) / 100.0);
        map.put("cpu_user", "" + Math.round(cpuUser * 100) / 100.0);
        map.put("cpu_stolen", "0.5");

        map.put("mem_free", "" + memFree);
        map.put("mem_used", "" + memUsed);

        map.put("load_average", "" + Math.round(s[0] * 100) / 100.0);

        this.machine.apiHandler.setApiData("machines/" + this.machine.id + "/create_stats/", map);
    }
}
