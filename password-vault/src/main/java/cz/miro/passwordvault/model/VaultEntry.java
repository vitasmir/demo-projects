package cz.miro.passwordvault.model;

import java.io.Serial;
import java.io.Serializable;

public final class VaultEntry implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String website;
    private final String username;
    private final String password;

    public VaultEntry(String website, String username, String password) {
        this.website = website;
        this.username = username;
        this.password = password;
    }

    public String getWebsite() {
        return website;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
