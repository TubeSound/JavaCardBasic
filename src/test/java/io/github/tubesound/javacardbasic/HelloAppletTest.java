package io.github.tubesound.javacardbasic;

import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import io.github.tubesound.javacardbasic.card.HelloApplet;
import java.nio.charset.StandardCharsets;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloAppletTest {
    private CardSimulator simulator;

    @BeforeEach
    void installAndSelect() {
        simulator = new CardSimulator();
        simulator.installApplet(AIDUtil.create("F0545542450101"), HelloApplet.class);
        assertStatusOnly(0x9000, simulator.transmitCommand(new CommandAPDU(new byte[] {
            0x00, (byte) 0xA4, 0x04, 0x00, 0x07, (byte) 0xF0, 0x54, 0x55, 0x42, 0x45, 0x01, 0x01
        })));
    }

    @Test
    void returnsGreetingWithExactLe() {
        assertGreeting(send(0x80, 0x10, 0, 0, 17));
    }

    @Test
    void shortApduLeZeroMeansUpTo256Bytes() {
        // CommandAPDUのne=256は、短いAPDUではLe=00として符号化される。
        CommandAPDU command = new CommandAPDU(0x80, 0x10, 0, 0, 256);
        assertArrayEquals(new byte[] {(byte) 0x80, 0x10, 0, 0, 0}, command.getBytes());
        assertGreeting(simulator.transmitCommand(command));
    }

    @Test
    void unsupportedClassReturnsStatusWithoutData() {
        assertStatusOnly(0x6E00, send(0x00, 0x10, 0, 0, 17));
    }

    @Test
    void unsupportedInstructionReturnsStatusWithoutData() {
        assertStatusOnly(0x6D00, send(0x80, 0x7F, 0, 0, 17));
    }

    @ParameterizedTest
    @CsvSource({"1,0", "0,1"})
    void nonzeroParametersAreRejected(int p1, int p2) {
        assertStatusOnly(0x6A86, send(0x80, 0x10, p1, p2, 17));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 16})
    void insufficientLeReportsRequiredLength(int le) {
        assertStatusOnly(0x6C11, send(0x80, 0x10, 0, 0, le));
    }

    @Test
    void aNewCommandWorksAfterAnError() {
        assertStatusOnly(0x6C11, send(0x80, 0x10, 0, 0, 1));
        assertGreeting(send(0x80, 0x10, 0, 0, 17));
        assertGreeting(send(0x80, 0x10, 0, 0, 17));
    }

    private ResponseAPDU send(int cla, int ins, int p1, int p2, int le) {
        return simulator.transmitCommand(new CommandAPDU(cla, ins, p1, p2, le));
    }

    private static void assertGreeting(ResponseAPDU response) {
        assertEquals(0x9000, response.getSW());
        assertArrayEquals("Hello, Java Card!".getBytes(StandardCharsets.US_ASCII), response.getData());
    }

    private static void assertStatusOnly(int sw, ResponseAPDU response) {
        assertEquals(sw, response.getSW());
        assertEquals(0, response.getData().length);
    }
}
