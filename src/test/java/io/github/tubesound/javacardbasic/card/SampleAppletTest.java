package io.github.tubesound.javacardbasic.card;

import com.licel.jcardsim.base.Simulator;
import com.licel.jcardsim.utils.AIDUtil;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javacard.framework.AID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleAppletTest {
    private Simulator simulator;
    private AID appletAID;

    @BeforeEach
    void setUp() {
        simulator = new Simulator();
        appletAID = AIDUtil.create("F0545542450101");
        simulator.installApplet(appletAID, SampleApplet.class);
        assertTrue(simulator.selectApplet(appletAID), "Applet must accept SELECT");
    }

    @Test
    void pingReturnsFourBytesAndSuccess() {
        assertPing(new byte[] {0x00, 0x10, 0x00, 0x00, 0x04});
    }

    @Test
    void pingAcceptsLe256() {
        assertPing(new byte[] {0x00, 0x10, 0x00, 0x00, 0x00});
    }

    @Test
    void unsupportedClaReturns6E00() {
        assertStatus(0x6E00, new byte[] {(byte) 0x80, 0x10, 0x00, 0x00, 0x04});
    }

    @Test
    void unsupportedInsReturns6D00() {
        assertStatus(0x6D00, new byte[] {0x00, 0x7F, 0x00, 0x00, 0x04});
    }

    @Test
    void nonzeroP1Returns6A86() {
        assertStatus(0x6A86, new byte[] {0x00, 0x10, 0x01, 0x00, 0x04});
    }

    @Test
    void nonzeroP2Returns6A86() {
        assertStatus(0x6A86, new byte[] {0x00, 0x10, 0x00, 0x01, 0x04});
    }

    @Test
    void shortLeReturns6C04() {
        assertStatus(0x6C04, new byte[] {0x00, 0x10, 0x00, 0x00, 0x03});
    }

    @Test
    void reselectReturnsSuccessWithoutPingData() {
        assertStatus(0x9000, AIDUtil.select(appletAID));
    }

    @Test
    void pingStillWorksAfterAnError() {
        assertStatus(0x6D00, new byte[] {0x00, 0x7F, 0x00, 0x00, 0x04});
        assertPing(new byte[] {0x00, 0x10, 0x00, 0x00, 0x04});
    }

    private void assertPing(byte[] command) {
        byte[] response = transmit(command);
        assertEquals(0x9000, statusWord(response));
        assertArrayEquals("PING".getBytes(StandardCharsets.US_ASCII), responseData(response));
    }

    private void assertStatus(int expected, byte[] command) {
        byte[] response = transmit(command);
        assertEquals(expected, statusWord(response));
        assertEquals(0, responseData(response).length, "Errors must not return stale response data");
    }

    private byte[] transmit(byte[] command) {
        byte[] response = simulator.transmitCommand(command);
        assertTrue(response.length >= 2, "Response must contain SW1 and SW2");
        return response;
    }

    private int statusWord(byte[] response) {
        int offset = response.length - 2;
        return ((response[offset] & 0xFF) << 8) | (response[offset + 1] & 0xFF);
    }

    private byte[] responseData(byte[] response) {
        return Arrays.copyOf(response, response.length - 2);
    }
}
