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

class FileSystemAppletTest {

    private static final byte[] SELECT_WEF = {
        0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, 0x10, 0x01
    };
    private static final byte[] SELECT_IEF = {
        0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, 0x10, 0x02
    };

    private Simulator simulator;
    private AID appletAID;

    @BeforeEach
    void setUp() {
        simulator = new Simulator();
        appletAID = AIDUtil.create("F0545542450201");
        simulator.installApplet(appletAID, FileSystemApplet.class);
        assertTrue(simulator.selectApplet(appletAID));
    }

    @Test
    void selectedWefCanBeReadByOffset() {
        assertStatus(0x9000, SELECT_WEF);
        byte[] response = transmit(new byte[] {0x00, (byte) 0xB0, 0x00, 0x06, 0x03});

        assertEquals(0x9000, statusWord(response));
        assertArrayEquals("WEF".getBytes(StandardCharsets.US_ASCII), responseData(response));
    }

    @Test
    void readRequiresASelectedWef() {
        assertStatus(0x6985, new byte[] {0x00, (byte) 0xB0, 0x00, 0x00, 0x08});
        assertStatus(0x9000, SELECT_IEF);
        assertStatus(0x6986, new byte[] {0x00, (byte) 0xB0, 0x00, 0x00, 0x08});
    }

    @Test
    void unknownFileIdReturns6A82() {
        assertStatus(0x6A82,
                new byte[] {0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, 0x7F, 0x01});
    }

    @Test
    void failedVerificationDecrementsTries() {
        assertStatus(0x9000, SELECT_IEF);
        assertStatus(0x63C2,
                new byte[] {0x00, 0x20, 0x00, (byte) 0x80, 0x04, '0', '0', '0', '0'});
    }

    @Test
    void updateRequiresKeyVerification() {
        assertStatus(0x9000, SELECT_WEF);
        assertStatus(0x6982,
                new byte[] {0x00, (byte) 0xD6, 0x00, 0x00, 0x02, 'O', 'K'});
    }

    @Test
    void verifiedUpdateSurvivesDeselectAndReset() {
        verifyPin();
        assertStatus(0x9000, SELECT_WEF);
        assertStatus(0x9000, new byte[] {
            0x00, (byte) 0xD6, 0x00, 0x00, 0x08,
            'J', 'A', 'V', 'A', '-', 'E', 'F', '!'
        });

        AID otherAID = AIDUtil.create("F0545542450101");
        simulator.installApplet(otherAID, SampleApplet.class);
        assertTrue(simulator.selectApplet(otherAID));
        assertTrue(simulator.selectApplet(appletAID));

        assertStatus(0x6985, new byte[] {0x00, (byte) 0xB0, 0x00, 0x00, 0x08});
        assertStatus(0x9000, SELECT_WEF);
        byte[] response = transmit(new byte[] {0x00, (byte) 0xB0, 0x00, 0x00, 0x08});
        assertArrayEquals("JAVA-EF!".getBytes(StandardCharsets.US_ASCII), responseData(response));
        assertEquals(0x9000, statusWord(response));

        assertStatus(0x6982,
                new byte[] {0x00, (byte) 0xD6, 0x00, 0x00, 0x01, '!'});

        simulator.reset();
        assertTrue(simulator.selectApplet(appletAID));
        assertStatus(0x9000, SELECT_WEF);
        response = transmit(new byte[] {0x00, (byte) 0xB0, 0x00, 0x00, 0x08});
        assertArrayEquals("JAVA-EF!".getBytes(StandardCharsets.US_ASCII), responseData(response));
        assertEquals(0x9000, statusWord(response));
    }

    @Test
    void codDeselectAndResetAreasHaveDifferentLifetimes() {
        assertStatus(0x9000, SELECT_WEF);
        assertStatus(0x9000, new byte[] {(byte) 0x80, 0x31, 0x00, 0x00});
        assertCodState(0x1001, 1);

        AID otherAID = AIDUtil.create("F0545542450101");
        simulator.installApplet(otherAID, SampleApplet.class);
        assertTrue(simulator.selectApplet(otherAID));
        assertTrue(simulator.selectApplet(appletAID));
        assertCodState(0x0000, 1);

        simulator.reset();
        assertTrue(simulator.selectApplet(appletAID));
        assertCodState(0x0000, 0);
    }

    private void verifyPin() {
        assertStatus(0x9000, SELECT_IEF);
        assertStatus(0x9000,
                new byte[] {0x00, 0x20, 0x00, (byte) 0x80, 0x04, '1', '2', '3', '4'});
    }

    private void assertCodState(int selectedFileId, int resetCounter) {
        byte[] response = transmit(new byte[] {(byte) 0x80, 0x30, 0x00, 0x00, 0x03});
        assertEquals(0x9000, statusWord(response));
        assertArrayEquals(new byte[] {
            (byte) (selectedFileId >>> 8), (byte) selectedFileId, (byte) resetCounter
        }, responseData(response));
    }

    private void assertStatus(int expected, byte[] command) {
        byte[] response = transmit(command);
        assertEquals(expected, statusWord(response));
        assertEquals(0, responseData(response).length);
    }

    private byte[] transmit(byte[] command) {
        byte[] response = simulator.transmitCommand(command);
        assertTrue(response.length >= 2);
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
