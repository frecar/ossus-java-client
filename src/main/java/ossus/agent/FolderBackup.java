package ossus.agent;

public class FolderBackup {
    private String id;
    private String path;
    private Schedule schedule;

    public Schedule getSchedule() {
        return this.schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        if (!path.endsWith(System.getProperty("file.separator"))) {
            path = path + System.getProperty("file.separator");
        }

        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}