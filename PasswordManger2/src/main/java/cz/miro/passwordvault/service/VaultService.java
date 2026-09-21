package cz.miro.passwordvault.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.crypto.SecretKey;

import cz.miro.passwordvault.model.EncryptedPayload;
import cz.miro.passwordvault.model.VaultContent;
import cz.miro.passwordvault.model.VaultEntry;
import cz.miro.passwordvault.model.VaultStore;

public final class VaultService {
    public static final String DEFAULT_GROUP = "Default";

    private final VaultRepository repository;

    public VaultService(VaultRepository repository) {
        this.repository = repository;
    }

    public boolean vaultExists() {
        return repository.exists();
    }

    public VaultSession createVault(char[] masterPassword) {
        byte[] salt = CryptoUtils.newSalt();
        SecretKey key = CryptoUtils.deriveKey(masterPassword, salt);
        VaultContent content = new VaultContent(Map.of(DEFAULT_GROUP, Map.of()));
        VaultStore store = new VaultStore(salt, CryptoUtils.passwordVerifier(key), encryptContent(content, key));
        repository.save(store);
        return new VaultSession(repository, key, store, content);
    }

    public VaultSession openVault(char[] masterPassword) {
        VaultStore store = repository.load();
        SecretKey key = CryptoUtils.deriveKey(masterPassword, store.getSalt());
        if (!CryptoUtils.matchesVerifier(key, store.getPasswordVerifier())) {
            throw new IllegalArgumentException("Hlavní heslo nesouhlasí.");
        }
        VaultContent content = decryptContent(store.getEncryptedVault(), key);
        return new VaultSession(repository, key, store, content);
    }

    public record DecryptedCredential(String groupName, String serverName, String username, String password) {
    }

    private static EncryptedPayload encryptContent(VaultContent content, SecretKey key) {
        byte[] serialized = CryptoUtils.serialize(content);
        try {
            return CryptoUtils.encrypt(serialized, key);
        } finally {
            CryptoUtils.wipe(serialized);
        }
    }

    private static VaultContent decryptContent(EncryptedPayload payload, SecretKey key) {
        byte[] decrypted = CryptoUtils.decrypt(payload, key);
        try {
            return CryptoUtils.deserialize(decrypted, VaultContent.class);
        } finally {
            CryptoUtils.wipe(decrypted);
        }
    }

    public static final class VaultSession {
        private final VaultRepository repository;
        private final SecretKey key;
        private VaultStore store;
        private VaultContent content;

        private VaultSession(VaultRepository repository, SecretKey key, VaultStore store, VaultContent content) {
            this.repository = repository;
            this.key = key;
            this.store = store;
            this.content = content;
        }

        public List<String> listGroups() {
            List<String> groups = new ArrayList<>(content.getGroups().keySet());
            groups.sort(Comparator
                    .comparingInt(VaultSession::groupDepth)
                    .thenComparing((String groupName) -> !groupName.equalsIgnoreCase(DEFAULT_GROUP))
                    .thenComparing(String.CASE_INSENSITIVE_ORDER));
            return groups;
        }

        public boolean addGroup(String parentGroupName, String groupName) {
            String normalizedGroupName = normalizeGroupPath(groupName);
            String normalizedParentPath = normalizeGroupPath(parentGroupName);
            String fullGroupName = normalizedParentPath.isBlank() || DEFAULT_GROUP.equalsIgnoreCase(normalizedParentPath)
                    ? normalizedGroupName
                    : normalizedParentPath + "/" + normalizedGroupName;

            Map<String, Map<String, VaultEntry>> updatedGroups = copyGroups(content.getGroups());
            if (updatedGroups.containsKey(fullGroupName)) {
                return false;
            }

            updatedGroups.put(fullGroupName, new LinkedHashMap<>());
            persist(updatedGroups);
            return true;
        }

        public boolean renameGroup(String groupName, String newGroupName) {
            String sourceGroupName = normalizeGroupPath(groupName);
            String renamedGroupName = normalizeGroupSegment(newGroupName);
            if (sourceGroupName.isBlank() || DEFAULT_GROUP.equalsIgnoreCase(sourceGroupName)) {
                return false;
            }

            String parentPath = parentPath(sourceGroupName);
            String targetGroupName = parentPath.isBlank() || DEFAULT_GROUP.equalsIgnoreCase(parentPath)
                    ? renamedGroupName
                    : parentPath + "/" + renamedGroupName;
            return relocateGroup(sourceGroupName, targetGroupName);
        }

        public boolean moveGroup(String groupName, String targetParentGroupName) {
            String sourceGroupName = normalizeGroupPath(groupName);
            String targetParentPath = normalizeGroupPath(targetParentGroupName);
            if (sourceGroupName.isBlank() || DEFAULT_GROUP.equalsIgnoreCase(sourceGroupName)) {
                return false;
            }

            String leafName = leafName(sourceGroupName);
            String targetGroupName = targetParentPath.isBlank() || DEFAULT_GROUP.equalsIgnoreCase(targetParentPath)
                    ? leafName
                    : targetParentPath + "/" + leafName;
            return relocateGroup(sourceGroupName, targetGroupName);
        }

        public boolean removeGroup(String groupName) {
            String normalizedGroupName = normalizeGroupPath(groupName);
            if (DEFAULT_GROUP.equalsIgnoreCase(normalizedGroupName)) {
                return false;
            }

            Map<String, Map<String, VaultEntry>> updatedGroups = copyGroups(content.getGroups());
            boolean removedAny = false;
            for (String existingGroupName : new ArrayList<>(updatedGroups.keySet())) {
                if (existingGroupName.equalsIgnoreCase(normalizedGroupName)
                        || existingGroupName.toLowerCase().startsWith(normalizedGroupName.toLowerCase() + "/")) {
                    updatedGroups.remove(existingGroupName);
                    removedAny = true;
                }
            }

            if (!removedAny) {
                return false;
            }

            if (updatedGroups.isEmpty()) {
                updatedGroups.put(DEFAULT_GROUP, new LinkedHashMap<>());
            }

            persist(updatedGroups);
            return true;
        }

        private boolean relocateGroup(String sourceGroupName, String targetGroupName) {
            String normalizedSourceGroupName = normalizeGroupPath(sourceGroupName);
            String normalizedTargetGroupName = normalizeGroupPath(targetGroupName);
            if (normalizedSourceGroupName.isBlank()
                    || normalizedTargetGroupName.isBlank()
                    || normalizedSourceGroupName.equalsIgnoreCase(normalizedTargetGroupName)) {
                return false;
            }

            Map<String, Map<String, VaultEntry>> sourceGroups = content.getGroups();
            if (!sourceGroups.containsKey(normalizedSourceGroupName)) {
                return false;
            }

            Map<String, Map<String, VaultEntry>> updatedGroups = new LinkedHashMap<>();
            boolean movedAny = false;

            for (Map.Entry<String, Map<String, VaultEntry>> entry : sourceGroups.entrySet()) {
                String existingGroupName = entry.getKey();
                if (existingGroupName.equalsIgnoreCase(normalizedSourceGroupName)
                        || existingGroupName.toLowerCase().startsWith(normalizedSourceGroupName.toLowerCase() + "/")) {
                    String suffix = existingGroupName.substring(normalizedSourceGroupName.length());
                    String newGroupName = normalizedTargetGroupName + suffix;
                    LinkedHashMap<String, VaultEntry> movedEntries = new LinkedHashMap<>();
                    for (VaultEntry vaultEntry : entry.getValue().values()) {
                        movedEntries.put(vaultEntry.getServerName(), new VaultEntry(
                                newGroupName,
                                vaultEntry.getServerName(),
                                vaultEntry.getUsername(),
                                vaultEntry.getPassword()
                        ));
                    }
                    updatedGroups.put(newGroupName, movedEntries);
                    movedAny = true;
                } else {
                    updatedGroups.put(existingGroupName, new LinkedHashMap<>(entry.getValue()));
                }
            }

            if (!movedAny || updatedGroups.containsKey(normalizedTargetGroupName) && !normalizedTargetGroupName.equalsIgnoreCase(normalizedSourceGroupName)) {
                return false;
            }

            persist(updatedGroups);
            return true;
        }

        public List<DecryptedCredential> listEntries(String groupName) {
            Map<String, Map<String, VaultEntry>> groups = content.getGroups();
            String resolvedGroupName = resolveExistingGroupKey(groups, groupName).orElse(null);
            if (resolvedGroupName == null) {
                return List.of();
            }

            Map<String, VaultEntry> groupEntries = groups.get(resolvedGroupName);
            if (groupEntries == null || groupEntries.isEmpty()) {
                return List.of();
            }

            List<DecryptedCredential> credentials = new ArrayList<>();
            for (VaultEntry entry : groupEntries.values()) {
                credentials.add(new DecryptedCredential(
                        entry.getGroupName(),
                        entry.getServerName(),
                        entry.getUsername(),
                        entry.getPassword()
                ));
            }
            credentials.sort(Comparator.comparing(DecryptedCredential::serverName, String.CASE_INSENSITIVE_ORDER));
            return credentials;
        }

        public Optional<DecryptedCredential> findByGroupAndServer(String groupName, String serverName) {
            Map<String, Map<String, VaultEntry>> groups = content.getGroups();
            String resolvedGroupName = resolveExistingGroupKey(groups, groupName).orElse(null);
            if (resolvedGroupName == null) {
                return Optional.empty();
            }

            Map<String, VaultEntry> groupEntries = groups.get(resolvedGroupName);
            if (groupEntries == null || groupEntries.isEmpty()) {
                return Optional.empty();
            }

            String resolvedServerName = resolveServerKey(groupEntries, serverName).orElse(null);
            if (resolvedServerName == null) {
                return Optional.empty();
            }

            VaultEntry entry = groupEntries.get(resolvedServerName);
            return Optional.of(new DecryptedCredential(
                    entry.getGroupName(),
                    entry.getServerName(),
                    entry.getUsername(),
                    entry.getPassword()
            ));
        }

        public void addOrUpdate(String groupName, String serverName, String username, String password) {
            String normalizedGroupName = normalizeGroupPath(groupName);
            String normalizedServerName = normalizeServerName(serverName);

            Map<String, Map<String, VaultEntry>> updatedGroups = copyGroups(content.getGroups());
            String resolvedGroupName = resolveWriteGroupKey(updatedGroups, normalizedGroupName).orElse(normalizedGroupName);
            LinkedHashMap<String, VaultEntry> groupEntries = new LinkedHashMap<>(updatedGroups.getOrDefault(resolvedGroupName, Map.of()));

            String resolvedServerKey = resolveServerKey(groupEntries, normalizedServerName).orElse(null);
            if (resolvedServerKey != null && !resolvedServerKey.equals(normalizedServerName)) {
                groupEntries.remove(resolvedServerKey);
            }

            groupEntries.put(normalizedServerName, new VaultEntry(resolvedGroupName, normalizedServerName, username, password));
            updatedGroups.put(resolvedGroupName, groupEntries);
            persist(updatedGroups);
        }

        public boolean remove(String groupName, String serverName) {
            Map<String, Map<String, VaultEntry>> updatedGroups = copyGroups(content.getGroups());
            String resolvedGroupName = resolveExistingGroupKey(updatedGroups, normalizeGroupPath(groupName)).orElse(null);
            if (resolvedGroupName == null) {
                return false;
            }

            Map<String, VaultEntry> groupEntries = updatedGroups.get(resolvedGroupName);
            if (groupEntries == null || groupEntries.isEmpty()) {
                return false;
            }

            String resolvedServerKey = resolveServerKey(groupEntries, normalizeServerName(serverName)).orElse(null);
            if (resolvedServerKey == null) {
                return false;
            }

            LinkedHashMap<String, VaultEntry> updatedEntries = new LinkedHashMap<>(groupEntries);
            updatedEntries.remove(resolvedServerKey);
            updatedGroups.put(resolvedGroupName, updatedEntries);
            persist(updatedGroups);
            return true;
        }

        private void persist(Map<String, Map<String, VaultEntry>> updatedGroups) {
            content = new VaultContent(updatedGroups);
            store = new VaultStore(store.getSalt(), store.getPasswordVerifier(), encryptContent(content, key));
            repository.save(store);
        }

        private static Map<String, Map<String, VaultEntry>> copyGroups(Map<String, Map<String, VaultEntry>> source) {
            Map<String, Map<String, VaultEntry>> updated = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, VaultEntry>> groupEntry : source.entrySet()) {
                updated.put(groupEntry.getKey(), new LinkedHashMap<>(groupEntry.getValue()));
            }
            return updated;
        }

        private static Optional<String> resolveExistingGroupKey(Map<String, Map<String, VaultEntry>> groups, String groupName) {
            String normalizedGroupName = normalizeGroupPath(groupName);
            for (String existingGroupName : groups.keySet()) {
                if (existingGroupName.equalsIgnoreCase(normalizedGroupName)) {
                    return Optional.of(existingGroupName);
                }
            }
            return Optional.empty();
        }

        private static Optional<String> resolveWriteGroupKey(Map<String, Map<String, VaultEntry>> groups, String groupName) {
            return resolveExistingGroupKey(groups, groupName)
                    .or(() -> Optional.of(normalizeGroupPath(groupName)));
        }

        private static Optional<String> resolveServerKey(Map<String, VaultEntry> entries, String serverName) {
            String normalizedServerName = normalizeServerName(serverName);
            for (String existingServerName : entries.keySet()) {
                if (existingServerName.equalsIgnoreCase(normalizedServerName)) {
                    return Optional.of(existingServerName);
                }
            }
            return Optional.empty();
        }

        private static String normalizeGroupPath(String groupName) {
            if (groupName == null || groupName.isBlank()) {
                return DEFAULT_GROUP;
            }
            String[] segments = groupName.trim().split("/");
            List<String> normalizedSegments = new ArrayList<>();
            for (String segment : segments) {
                String trimmed = segment.trim();
                if (!trimmed.isEmpty()) {
                    normalizedSegments.add(trimmed);
                }
            }
            if (normalizedSegments.isEmpty()) {
                return DEFAULT_GROUP;
            }
            return String.join("/", normalizedSegments);
        }

        private static String normalizeGroupSegment(String groupName) {
            String normalized = normalizeGroupPath(groupName);
            int separatorIndex = normalized.lastIndexOf('/');
            return separatorIndex >= 0 ? normalized.substring(separatorIndex + 1) : normalized;
        }

        private static String parentPath(String groupName) {
            String normalized = normalizeGroupPath(groupName);
            int separatorIndex = normalized.lastIndexOf('/');
            return separatorIndex >= 0 ? normalized.substring(0, separatorIndex) : "";
        }

        private static String leafName(String groupName) {
            return normalizeGroupSegment(groupName);
        }

        private static int groupDepth(String groupName) {
            if (groupName == null || groupName.isBlank()) {
                return 0;
            }
            return normalizeGroupPath(groupName).split("/").length;
        }

        private static String normalizeServerName(String serverName) {
            if (serverName == null) {
                return "";
            }
            return serverName.trim();
        }
    }
}
