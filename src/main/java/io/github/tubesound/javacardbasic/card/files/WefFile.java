package io.github.tubesound.javacardbasic.card.files;

import javacard.framework.Util;

/** A working EF containing ordinary persistent data. */
public final class WefFile {

    private final short fileId;
    private final byte[] data;
    private short length;

    public WefFile(short fileId, short capacity, byte[] initialData,
            short initialOffset, short initialLength) {
        this.fileId = fileId;
        data = new byte[capacity];
        update((short) 0, initialData, initialOffset, initialLength);
    }

    public short getFileId() {
        return fileId;
    }

    public short getLength() {
        return length;
    }

    public short getCapacity() {
        return (short) data.length;
    }

    public void read(short fileOffset, byte[] destination,
            short destinationOffset, short readLength) {
        Util.arrayCopyNonAtomic(data, fileOffset, destination, destinationOffset, readLength);
    }

    public void update(short fileOffset, byte[] source,
            short sourceOffset, short updateLength) {
        Util.arrayCopy(source, sourceOffset, data, fileOffset, updateLength);
        short end = (short) (fileOffset + updateLength);
        if (end > length) {
            length = end;
        }
    }
}
