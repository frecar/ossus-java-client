package commons;

import org.json.simple.JSONObject;
import commons.exceptions.OSSUSNoAPIConnectionException;

public class Version {

    public final String name;
    public final String updaterLink;
    public final String agentLink;
    public final String id;

    private Version(
            final String id,
            final String name,
            final String update_link,
            final String agentLink
    ) {
        this.id = id;
        this.name = name;
        this.updaterLink = update_link;
        this.agentLink = agentLink;
    }

    public static Version buildFromJson(
            final JSONObject json
    ) throws OSSUSNoAPIConnectionException {
        return new Version(
                json.get(OssusAPICONST.VERSION_ID).toString(),
                (String) json.get(OssusAPICONST.VERSION_NAME),
                (String) json.get(OssusAPICONST.VERSION_UPDATER_LINK), (
                String) json.get(OssusAPICONST.VERSION_AGENT_LINK)
        );
    }

    public final boolean equals(final Object o) {
        return o instanceof Version && name.equals(((Version) o).name);
    }


    public final int hashCode() {
        return name.hashCode();
    }

    public final String toString() {
        return "Ossus Version" + name;
    }
}
