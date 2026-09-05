package io.github.tubesound.javacardbasic.card;

import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import java.nio.charset.StandardCharsets;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javacard.framework.AID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleAppletTest {

    private CardSimulator simulator;
    private AID appletAID;

    @BeforeEach
    void setUp() {
        simulator = new CardSimulator();
        appletAID = AIDUtil.create("F0545542450101");
        // No installation data: this also covers the register() regression.
        simulator.installApplet(appletAID, SampleApplet.class);
        assertTrue(simulator.selectApplet(appletAID), "Applet must accept SELECT");
    }

    @Test
    void pingReturnsFourBytesAndSuccess() {
        // Five-byte short APDU: CLA INS P1 P2 Le. Le=4 is explicit.
        assertPing(new CommandAPDU(0x00, 0x10, 0x00, 0x00, 4));
    }

    @Test
    void pingAcceptsLe256() {
        // In a short APDU, encoded Le=00 means 256 bytes, not zero.
        assertPing(new CommandAPDU(0x00, 0x10, 0x00, 0x00, 256));
    }

    @Test
    void unsupportedClaReturns6E00() {
        assertStatus(0x6E00, new CommandAPDU(0x80, 0x10, 0x00, 0x00, 4));
    }

    @Test
    void unsupportedInsReturns6D00() {
        assertStatus(0x6D00, new CommandAPDU(0x00, 0x7F, 0x00, 0x00, 4));
    }

    @Test
    void nonzeroP1Returns6A86() {
        assertStatus(0x6A86, new CommandAPDU(0x00, 0x10, 0x01, 0x00, 4));
    }

    @Test
    void nonzeroP2Returns6A86() {
        assertStatus(0x6A86, new CommandAPDU(0x00, 0x10, 0x00, 0x01, 4));
    }

    @Test
    void shortLeReturns6C04() {
        assertStatus(0x6C04, new CommandAPDU(0x00, 0x10, 0x00, 0x00, 3));
    }

    @Test
    void reselectReturnsSuccessWithoutPingData() {
        assertStatus(0x9000, new CommandAPDU(AIDUtil.select(appletAID)));
    }

    @Test
    void pingStillWorksAfterAnError() {
        assertStatus(0x6D00, new CommandAPDU(0x00, 0x7F, 0x00, 0x00, 4));
        assertPing(new CommandAPDU(0x00, 0x10, 0x00, 0x00, 4));
    }

    private void assertPing(CommandAPDU command) {
        ResponseAPDU response = simulator.transmitCommand(command);
        assertEquals(0x9000, response.getSW());
        assertArrayEquals("PING".getBytes(StandardCharsets.US_ASCII), response.getData());
    }

    private void assertStatus(int expected, CommandAPDU command) {
        ResponseAPDU response = simulator.transmitCommand(command);
        assertEquals(expected, response.getSW());
        assertEquals(0, response.getData().length, "Errors must not return stale response data");
    }
}
