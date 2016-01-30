package agent;

public final class FolderBackup {
    private String id;
    private String path;

    public String getId() {
        return id;
    }

    public void setId(
            final String id
    ) {
        this.id = id;
    }

    public String getPath() {
        if (!path.endsWith(System.getProperty("file.separator"))) {
            path = path + System.getProperty("file.separator");
        }

        return path;
    }

    public void setPath(
            final String path
    ) {
        this.path = path;
    }
}
