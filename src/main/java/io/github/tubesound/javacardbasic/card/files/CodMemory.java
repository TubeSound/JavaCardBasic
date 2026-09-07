package io.github.tubesound.javacardbasic.card.files;

import javacard.framework.JCSystem;
import javacard.framework.Util;

/**
 * Project-local model of COD (transient memory).
 *
 * <p>COD is not a Java Card API type. This class gives that design term an
 * explicit boundary and owns the transient arrays used by the sample.</p>
 */
public final class CodMemory {

    public static final short NO_FILE_SELECTED = (short) 0x0000;

    private static final short SELECTED_FILE_SIZE = (short) 2;
    private static final short RESET_WORK_SIZE = (short) 16;

    private final byte[] deselectState;
    private final byte[] resetWork;

    public CodMemory() {
        deselectState = JCSystem.makeTransientByteArray(
                SELECTED_FILE_SIZE, JCSystem.CLEAR_ON_DESELECT);
        resetWork = JCSystem.makeTransientByteArray(
                RESET_WORK_SIZE, JCSystem.CLEAR_ON_RESET);
    }

    public void selectFile(short fileId) {
        Util.setShort(deselectState, (short) 0, fileId);
    }

    public short getSelectedFileId() {
        return Util.getShort(deselectState, (short) 0);
    }

    public byte[] getResetWork() {
        return resetWork;
    }

    public void incrementResetCounter() {
        resetWork[0]++;
    }

    public byte getResetCounter() {
        return resetWork[0];
    }

    public void clearResetWork() {
        Util.arrayFillNonAtomic(resetWork, (short) 0, (short) resetWork.length, (byte) 0);
    }
}
