package io.github.tubesound.javacardbasic.card;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;

/** A first applet: command 00 10 00 00 04 returns the four ASCII bytes PING. */
public final class SampleApplet extends Applet {

    private static final byte INS_PING = (byte) 0x10;
    private static final short PING_LENGTH = (short) 4;

    private SampleApplet() {
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        // This lesson uses the default applet AID and no installation parameters.
        // jCardSim's two-argument installApplet() supplies no parameter bytes.
        new SampleApplet().register();
    }

    @Override
    public void process(APDU apdu) {
        // The runtime also calls process() for the SELECT command.
        if (selectingApplet()) {
            return;
        }

        byte[] buffer = apdu.getBuffer();
        if (buffer[ISO7816.OFFSET_CLA] != (byte) 0x00) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
        if (buffer[ISO7816.OFFSET_INS] != INS_PING) {
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
        if (buffer[ISO7816.OFFSET_P1] != (byte) 0x00
                || buffer[ISO7816.OFFSET_P2] != (byte) 0x00) {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }

        // PING is a case-2 command: no incoming data. Calling a receive method
        // here would incorrectly interpret P3 as Lc on T=0 cards.
        short le = apdu.setOutgoing();
        if (le < PING_LENGTH) {
            ISOException.throwIt((short) (ISO7816.SW_CORRECT_LENGTH_00 | PING_LENGTH));
        }
        apdu.setOutgoingLength(PING_LENGTH);
        buffer[0] = (byte) 0x50;
        buffer[1] = (byte) 0x49;
        buffer[2] = (byte) 0x4E;
        buffer[3] = (byte) 0x47;
        apdu.sendBytes((short) 0, PING_LENGTH);
    }
}
