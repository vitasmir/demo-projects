package cz.miro.passwordvault.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class VaultContent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, VaultEntry> entries;

    public VaultContent(Map<String, VaultEntry> entries) {
        this.entries = new LinkedHashMap<>(entries);
    }

    public Map<String, VaultEntry> getEntries() {
        return new LinkedHashMap<>(entries);
    }
}
