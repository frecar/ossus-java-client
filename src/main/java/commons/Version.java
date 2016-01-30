package commons;

import org.json.simple.JSONObject;
import commons.exceptions.OSSUSNoAPIConnectionException;

public final class Version {

    public final String name;
    public final String updaterLink;
    public final String agentLink;
    public final String id;

    private Version(
            final String id,
            final String name,
            final String updateLink,
            final String agentLink
    ) {
        this.id = id;
        this.name = name;
        this.updaterLink = updateLink;
        this.agentLink = agentLink;
    }

    public static Version buildFromJson(
            final JSONObject json
    ) throws OSSUSNoAPIConnectionException {
        return new Version(
                json.get(ApiTrans.VERSION_ID.value).toString(),
                (String) json.get(ApiTrans.VERSION_NAME.value),
                (String) json.get(ApiTrans.VERSION_UPDATER_LINK.value), (
                String) json.get(ApiTrans.VERSION_AGENT_LINK.value)
        );
    }

    public boolean equals(final Object o) {
        return o instanceof Version && name.equals(((Version) o).name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        return "Ossus Version" + name;
    }
}
