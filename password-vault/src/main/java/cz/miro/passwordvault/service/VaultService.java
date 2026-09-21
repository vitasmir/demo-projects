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
        VaultContent content = new VaultContent(Map.of());
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

    public record DecryptedCredential(String website, String username, String password) {
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

        public void addOrUpdate(String website, String username, String password) {
            Map<String, VaultEntry> updated = new LinkedHashMap<>(content.getEntries());
            updated.put(normalizeWebsite(website), new VaultEntry(
                    website.trim(),
                    username,
                    password
            ));
            persist(updated);
        }

        public Optional<DecryptedCredential> findByWebsite(String website) {
            VaultEntry entry = content.getEntries().get(normalizeWebsite(website));
            if (entry == null) {
                return Optional.empty();
            }
            return Optional.of(new DecryptedCredential(
                    entry.getWebsite(),
                    entry.getUsername(),
                    entry.getPassword()
            ));
        }

        public boolean remove(String website) {
            Map<String, VaultEntry> updated = new LinkedHashMap<>(content.getEntries());
            boolean removed = updated.remove(normalizeWebsite(website)) != null;
            if (removed) {
                persist(updated);
            }
            return removed;
        }

        public List<DecryptedCredential> listEntries() {
            List<DecryptedCredential> credentials = new ArrayList<>();
            for (VaultEntry entry : content.getEntries().values()) {
                credentials.add(new DecryptedCredential(entry.getWebsite(), entry.getUsername(), entry.getPassword()));
            }
            credentials.sort(Comparator.comparing(DecryptedCredential::website, String.CASE_INSENSITIVE_ORDER));
            return credentials;
        }

        private void persist(Map<String, VaultEntry> updatedEntries) {
            content = new VaultContent(updatedEntries);
            store = new VaultStore(store.getSalt(), store.getPasswordVerifier(), encryptContent(content, key));
            repository.save(store);
        }

        private static String normalizeWebsite(String website) {
            return website == null ? "" : website.trim().toLowerCase();
        }
    }
}
