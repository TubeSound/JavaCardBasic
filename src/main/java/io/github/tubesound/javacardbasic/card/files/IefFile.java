package io.github.tubesound.javacardbasic.card.files;

/** An internal EF that owns a key object and is never read as ordinary data. */
public final class IefFile {

    private final short fileId;
    private final KeyObject key;

    public IefFile(short fileId, KeyObject key) {
        this.fileId = fileId;
        this.key = key;
    }

    public short getFileId() {
        return fileId;
    }

    public KeyObject getKey() {
        return key;
    }
}
