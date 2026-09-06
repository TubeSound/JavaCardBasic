package io.github.tubesound.javacardbasic.card;

import io.github.tubesound.javacardbasic.card.files.CodMemory;
import io.github.tubesound.javacardbasic.card.files.IefFile;
import io.github.tubesound.javacardbasic.card.files.KeyObject;
import io.github.tubesound.javacardbasic.card.files.WefFile;
import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

/** Learning applet for COD, IEF, WEF and key-object lifecycles. */
public final class FileSystemApplet extends Applet {

    private static final byte CLA_ISO = (byte) 0x00;
    private static final byte CLA_LESSON = (byte) 0x80;

    private static final byte INS_SELECT_FILE = (byte) 0xA4;
    private static final byte INS_VERIFY = (byte) 0x20;
    private static final byte INS_READ_BINARY = (byte) 0xB0;
    private static final byte INS_UPDATE_BINARY = (byte) 0xD6;
    private static final byte INS_GET_COD_STATE = (byte) 0x30;
    private static final byte INS_INCREMENT_COD = (byte) 0x31;

    private static final short FID_WEF = (short) 0x1001;
    private static final short FID_IEF = (short) 0x1002;
    private static final short WEF_CAPACITY = (short) 32;
    private static final short COD_STATE_LENGTH = (short) 3;

    private static final short SW_SECURITY_STATUS_NOT_SATISFIED = (short) 0x6982;
    private static final short SW_COMMAND_NOT_ALLOWED = (short) 0x6986;
    private static final short SW_FILE_NOT_FOUND = (short) 0x6A82;
    private static final short SW_WRONG_OFFSET = (short) 0x6B00;

    private final CodMemory cod;
    private final WefFile wef;
    private final IefFile ief;

    private FileSystemApplet() {
        cod = new CodMemory();

        // Use transient work memory while constructing persistent objects.
        byte[] work = cod.getResetWork();
        work[0] = (byte) 'H';
        work[1] = (byte) 'E';
        work[2] = (byte) 'L';
        work[3] = (byte) 'L';
        work[4] = (byte) 'O';
        work[5] = (byte) '-';
        work[6] = (byte) 'W';
        work[7] = (byte) 'E';
        work[8] = (byte) 'F';
        wef = new WefFile(FID_WEF, WEF_CAPACITY, work, (short) 0, (short) 9);

        work[0] = (byte) '1';
        work[1] = (byte) '2';
        work[2] = (byte) '3';
        work[3] = (byte) '4';
        KeyObject pin = new KeyObject((byte) 0x01, (byte) 3, (byte) 4,
                work, (short) 0, (byte) 4);
        ief = new IefFile(FID_IEF, pin);
        cod.clearResetWork();
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new FileSystemApplet().register();
    }

    @Override
    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }

        byte[] buffer = apdu.getBuffer();
        byte cla = buffer[ISO7816.OFFSET_CLA];
        byte ins = buffer[ISO7816.OFFSET_INS];

        if (cla == CLA_ISO) {
            processIso(apdu, buffer, ins);
            return;
        }
        if (cla == CLA_LESSON) {
            processLesson(apdu, buffer, ins);
            return;
        }
        ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
    }

    @Override
    public void deselect() {
        // OwnerPIN validation does not represent persistent authentication.
        ief.getKey().clearValidation();
    }

    private void processIso(APDU apdu, byte[] buffer, byte ins) {
        switch (ins) {
            case INS_SELECT_FILE:
                selectFile(apdu, buffer);
                return;
            case INS_VERIFY:
                verify(apdu, buffer);
                return;
            case INS_READ_BINARY:
                readBinary(apdu, buffer);
                return;
            case INS_UPDATE_BINARY:
                updateBinary(apdu, buffer);
                return;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void processLesson(APDU apdu, byte[] buffer, byte ins) {
        requireZeroP1P2(buffer);
        switch (ins) {
            case INS_GET_COD_STATE:
                short le = apdu.setOutgoing();
                if (le < COD_STATE_LENGTH) {
                    ISOException.throwIt((short) (ISO7816.SW_CORRECT_LENGTH_00
                            | COD_STATE_LENGTH));
                }
                Util.setShort(buffer, (short) 0, cod.getSelectedFileId());
                buffer[2] = cod.getResetCounter();
                apdu.setOutgoingLength(COD_STATE_LENGTH);
                apdu.sendBytes((short) 0, COD_STATE_LENGTH);
                return;
            case INS_INCREMENT_COD:
                cod.incrementResetCounter();
                return;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void selectFile(APDU apdu, byte[] buffer) {
        if (buffer[ISO7816.OFFSET_P1] != (byte) 0x02
                || buffer[ISO7816.OFFSET_P2] != (byte) 0x0C) {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
        short dataOffset = receiveExact(apdu, (short) 2);
        short fileId = Util.getShort(buffer, dataOffset);
        if (fileId != wef.getFileId() && fileId != ief.getFileId()) {
            ISOException.throwIt(SW_FILE_NOT_FOUND);
        }
        cod.selectFile(fileId);
    }

    private void verify(APDU apdu, byte[] buffer) {
        if (buffer[ISO7816.OFFSET_P1] != (byte) 0x00
                || buffer[ISO7816.OFFSET_P2] != (byte) 0x80) {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
        if (cod.getSelectedFileId() != ief.getFileId()) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        short dataOffset = receiveExact(apdu, (short) 4);
        KeyObject key = ief.getKey();
        if (!key.check(buffer, dataOffset, (byte) 4)) {
            ISOException.throwIt((short) (0x63C0 | key.getTriesRemaining()));
        }
    }

    private void readBinary(APDU apdu, byte[] buffer) {
        requireSelectedWef();
        short fileOffset = unsignedP1P2(buffer);
        if (fileOffset > wef.getLength()) {
            ISOException.throwIt(SW_WRONG_OFFSET);
        }
        short le = apdu.setOutgoing();
        short available = (short) (wef.getLength() - fileOffset);
        short readLength = le < available ? le : available;
        apdu.setOutgoingLength(readLength);
        wef.read(fileOffset, buffer, (short) 0, readLength);
        apdu.sendBytes((short) 0, readLength);
    }

    private void updateBinary(APDU apdu, byte[] buffer) {
        requireSelectedWef();
        if (!ief.getKey().isValidated()) {
            ISOException.throwIt(SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        short fileOffset = unsignedP1P2(buffer);
        short incomingLength = apdu.getIncomingLength();
        if (incomingLength == 0
                || fileOffset > wef.getCapacity()
                || incomingLength > (short) (wef.getCapacity() - fileOffset)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short dataOffset = receiveExact(apdu, incomingLength);
        wef.update(fileOffset, buffer, dataOffset, incomingLength);
    }

    private void requireSelectedWef() {
        short selected = cod.getSelectedFileId();
        if (selected == CodMemory.NO_FILE_SELECTED) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        if (selected != wef.getFileId()) {
            ISOException.throwIt(SW_COMMAND_NOT_ALLOWED);
        }
    }

    private short receiveExact(APDU apdu, short expectedLength) {
        if (apdu.getIncomingLength() != expectedLength) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short dataOffset = apdu.getOffsetCdata();
        short total = apdu.setIncomingAndReceive();
        while (total < expectedLength) {
            short count = apdu.receiveBytes((short) (dataOffset + total));
            if (count <= 0) {
                ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
            }
            total += count;
        }
        return dataOffset;
    }

    private void requireZeroP1P2(byte[] buffer) {
        if (buffer[ISO7816.OFFSET_P1] != (byte) 0x00
                || buffer[ISO7816.OFFSET_P2] != (byte) 0x00) {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
    }

    private short unsignedP1P2(byte[] buffer) {
        // This small lesson WEF is at most 32 bytes, so any non-zero high
        // offset byte is outside the file and must not become a negative short.
        if (buffer[ISO7816.OFFSET_P1] != (byte) 0x00) {
            ISOException.throwIt(SW_WRONG_OFFSET);
        }
        return (short) (buffer[ISO7816.OFFSET_P2] & 0xFF);
    }
}
