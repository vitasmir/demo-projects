package cz.miro.passwordvault.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;

public final class VaultEntry implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_GROUP = "Default";

    private String groupName;
    private String website;
    private String serverName;
    private String username;
    private String password;

    public VaultEntry(String groupName, String serverName, String username, String password) {
        this.groupName = normalizeGroupName(groupName);
        this.serverName = normalizeValue(serverName);
        this.website = this.serverName;
        this.username = normalizeValue(username);
        this.password = normalizeValue(password);
    }

    public String getGroupName() {
        return normalizeGroupName(groupName);
    }

    public String getWebsite() {
        return getServerName();
    }

    public String getServerName() {
        if (serverName == null || serverName.isBlank()) {
            return website == null ? "" : website;
        }
        return serverName;
    }

    public String getUsername() {
        return username == null ? "" : username;
    }

    public String getPassword() {
        return password == null ? "" : password;
    }

    @Serial
    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        if (serverName == null || serverName.isBlank()) {
            serverName = website == null ? "" : website.trim();
        }
        if (website == null || website.isBlank()) {
            website = serverName;
        }
        groupName = normalizeGroupName(groupName);
        username = normalizeValue(username);
        password = normalizeValue(password);
    }

    private static String normalizeGroupName(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_GROUP;
        }
        return value.trim();
    }

    private static String normalizeValue(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
