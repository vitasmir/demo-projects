package cz.miro.passwordvault.model;

import java.io.Serial;
import java.io.Serializable;

public final class VaultStore implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final byte[] salt;
    private final byte[] passwordVerifier;
    private final EncryptedPayload encryptedVault;

    public VaultStore(byte[] salt, byte[] passwordVerifier, EncryptedPayload encryptedVault) {
        this.salt = salt.clone();
        this.passwordVerifier = passwordVerifier.clone();
        this.encryptedVault = encryptedVault;
    }

    public byte[] getSalt() {
        return salt.clone();
    }

    public byte[] getPasswordVerifier() {
        return passwordVerifier.clone();
    }

    public EncryptedPayload getEncryptedVault() {
        return encryptedVault;
    }
}
