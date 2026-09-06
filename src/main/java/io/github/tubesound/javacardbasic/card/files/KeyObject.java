package io.github.tubesound.javacardbasic.card.files;

import javacard.framework.OwnerPIN;

/** A verification-key object backed by Java Card's OwnerPIN. */
public final class KeyObject {

    private final byte keyId;
    private final OwnerPIN pin;

    public KeyObject(byte keyId, byte tryLimit, byte maxLength,
            byte[] initialValue, short initialOffset, byte initialLength) {
        this.keyId = keyId;
        pin = new OwnerPIN(tryLimit, maxLength);
        pin.update(initialValue, initialOffset, initialLength);
    }

    public byte getKeyId() {
        return keyId;
    }

    public boolean check(byte[] candidate, short offset, byte length) {
        return pin.check(candidate, offset, length);
    }

    public boolean isValidated() {
        return pin.isValidated();
    }

    public byte getTriesRemaining() {
        return pin.getTriesRemaining();
    }

    public void clearValidation() {
        pin.reset();
    }
}
