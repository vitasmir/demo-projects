package cz.miro.passwordvault.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VaultContent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_GROUP = "Default";

    private Map<String, VaultEntry> entries;
    private Map<String, Map<String, VaultEntry>> groups;

    public VaultContent(Map<String, ? extends Map<String, VaultEntry>> groups) {
        this.groups = deepCopyGroups(groups);
        if (this.groups.isEmpty()) {
            this.groups.put(DEFAULT_GROUP, new LinkedHashMap<>());
        }
        this.entries = flattenGroups(this.groups);
    }

    public Map<String, Map<String, VaultEntry>> getGroups() {
        ensureInitialized();
        return deepCopyGroups(groups);
    }

    public List<String> getGroupNames() {
        ensureInitialized();
        return new ArrayList<>(groups.keySet());
    }

    public Map<String, VaultEntry> getGroupEntries(String groupName) {
        ensureInitialized();
        String resolvedGroupName = resolveGroupName(groupName);
        Map<String, VaultEntry> selected = groups.get(resolvedGroupName);
        return selected == null ? new LinkedHashMap<>() : new LinkedHashMap<>(selected);
    }

    @Serial
    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        ensureInitialized();
    }

    private void ensureInitialized() {
        if (groups == null || groups.isEmpty()) {
            groups = new LinkedHashMap<>();
            if (entries != null && !entries.isEmpty()) {
                groups.put(DEFAULT_GROUP, new LinkedHashMap<>(entries));
            } else {
                groups.put(DEFAULT_GROUP, new LinkedHashMap<>());
            }
        }

        if (entries == null || entries.isEmpty()) {
            entries = flattenGroups(groups);
        }
    }

    private static LinkedHashMap<String, Map<String, VaultEntry>> deepCopyGroups(Map<String, ? extends Map<String, VaultEntry>> source) {
        LinkedHashMap<String, Map<String, VaultEntry>> copy = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, ? extends Map<String, VaultEntry>> entry : source.entrySet()) {
                copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
            }
        }
        return copy;
    }

    private static LinkedHashMap<String, VaultEntry> flattenGroups(Map<String, ? extends Map<String, VaultEntry>> source) {
        LinkedHashMap<String, VaultEntry> flattened = new LinkedHashMap<>();
        if (source == null) {
            return flattened;
        }

        for (Map<String, VaultEntry> groupEntries : source.values()) {
            flattened.putAll(groupEntries);
        }
        return flattened;
    }

    private String resolveGroupName(String groupName) {
        if (groupName != null) {
            for (String existingGroupName : groups.keySet()) {
                if (existingGroupName.equalsIgnoreCase(groupName.trim())) {
                    return existingGroupName;
                }
            }
        }

        if (groups.containsKey(DEFAULT_GROUP)) {
            return DEFAULT_GROUP;
        }

        return groups.keySet().stream().findFirst().orElse(DEFAULT_GROUP);
    }
}
