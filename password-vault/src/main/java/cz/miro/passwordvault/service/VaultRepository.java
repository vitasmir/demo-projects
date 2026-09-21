package cz.miro.passwordvault.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import cz.miro.passwordvault.model.VaultStore;

public final class VaultRepository {
    private final Path vaultPath;

    public VaultRepository(Path vaultPath) {
        this.vaultPath = vaultPath;
    }

    public boolean exists() {
        return Files.exists(vaultPath);
    }

    public VaultStore load() {
        try (ObjectInputStream inputStream = new ObjectInputStream(Files.newInputStream(vaultPath))) {
            return (VaultStore) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Nepodařilo se načíst trezor: " + vaultPath, e);
        }
    }

    public void save(VaultStore store) {
        try {
            Path parent = vaultPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream outputStream = new ObjectOutputStream(Files.newOutputStream(vaultPath))) {
                outputStream.writeObject(store);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Nepodařilo se uložit trezor: " + vaultPath, e);
        }
    }

    public Path getVaultPath() {
        return vaultPath;
    }
}
