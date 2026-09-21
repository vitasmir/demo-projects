package cz.miro.passwordvault.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;

public final class EncryptedPayload implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final byte[] iv;
    private final byte[] cipherText;

    public EncryptedPayload(byte[] iv, byte[] cipherText) {
        this.iv = Arrays.copyOf(iv, iv.length);
        this.cipherText = Arrays.copyOf(cipherText, cipherText.length);
    }

    public byte[] getIv() {
        return Arrays.copyOf(iv, iv.length);
    }

    public byte[] getCipherText() {
        return Arrays.copyOf(cipherText, cipherText.length);
    }
}
