package ossus.agent;

import java.util.HashMap;
import java.util.Map;

import ossus.commons.Machine;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import ossus.commons.exceptions.OSSUSNoAPIConnectionException;

public class MachineStats {

    Machine machine;
    float loadAverage;
    float cpuSystem;
    float cpuUser;
    float cpuStolen;
    float cpuTotal;

    long memFree;
    long memUsed;

    double[] s = {0.0, 0.0, 0.0};

    Sigar sigar;

    public MachineStats(Machine machine) throws OSSUSNoAPIConnectionException {

        this.sigar = new Sigar();
        this.machine = machine;

        Mem meminfo = null;

        try {
            if (!System.getProperty("os.name").contains("Windows")) {
                s = sigar.getLoadAverage();
            }
        } catch (SigarException e1) {
            this.machine.log_error(e1.getMessage());
        }

        try {
            meminfo = sigar.getMem();

            CpuPerc cpuperc = sigar.getCpuPerc();

            cpuSystem = (float) (cpuperc.getSys() * 100);

            cpuUser = (float) (cpuperc.getUser() * 100);

            memFree = meminfo.getActualFree();
            memUsed = meminfo.getActualUsed();

        } catch (Exception e) {
            this.machine.log_error(e.getMessage());
        }

    }

    public void save() throws OSSUSNoAPIConnectionException {
        Map<String, String> map = new HashMap<String, String>();
        map.put("cpu_system", "" + Math.round(cpuSystem * 100) / 100.0);
        map.put("cpu_user", "" + Math.round(cpuUser * 100) / 100.0);
        map.put("cpu_stolen", "0.5");

        map.put("mem_free", "" + memFree);
        map.put("mem_used", "" + memUsed);

        map.put("load_average", "" + Math.round(s[0] * 100) / 100.0);

        this.machine.apiHandler.set_api_data("machines/" + this.machine.id + "/create_stats/", map);
    }
}