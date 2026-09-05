package io.github.tubesound.javacardbasic.host;

import com.licel.jcardsim.smartcardio.CardSimulator;
import com.licel.jcardsim.utils.AIDUtil;
import io.github.tubesound.javacardbasic.card.HelloApplet;
import java.nio.charset.StandardCharsets;
import javacard.framework.AID;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/** PC側の実行コード。ここでは通常のJavaライブラリを使える。 */
public final class HelloDemo {
    private HelloDemo() {
    }

    public static void main(String[] args) {
        CardSimulator simulator = new CardSimulator();
        // ローカル学習用の識別子。製品用AIDを決定したものではない。
        AID aid = AIDUtil.create("F0545542450101");
        simulator.installApplet(aid, HelloApplet.class);
        System.out.println("Installed HelloApplet (simulator API; not a GlobalPlatform install)");

        // SELECTは、以後どのアプレットにコマンドを届けるかを指定する。
        ResponseAPDU selected = exchange(simulator, "SELECT",
                new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aidBytes(aid)));
        requireStatus(selected, 0x9000);

        // 第5引数は受け取りたい最大データ長。17バイト = 0x11。
        ResponseAPDU hello = exchange(simulator, "HELLO",
                new CommandAPDU(0x80, 0x10, 0x00, 0x00, 17));
        requireStatus(hello, 0x9000);
        System.out.println("Text: " + new String(hello.getData(), StandardCharsets.US_ASCII));

        ResponseAPDU unknown = exchange(simulator, "Unknown instruction",
                new CommandAPDU(0x80, 0x7F, 0x00, 0x00, 17));
        requireStatus(unknown, 0x6D00);

        ResponseAPDU tooShort = exchange(simulator, "Le too small",
                new CommandAPDU(0x80, 0x10, 0x00, 0x00, 1));
        requireStatus(tooShort, 0x6C11);
    }

    private static ResponseAPDU exchange(CardSimulator simulator, String label, CommandAPDU command) {
        System.out.println("\n[" + label + "]");
        System.out.println("C: " + hex(command.getBytes()));
        ResponseAPDU response = simulator.transmitCommand(command);
        System.out.println("R: " + hex(response.getBytes()));
        System.out.printf("SW: %04X%n", response.getSW());
        return response;
    }

    private static byte[] aidBytes(AID aid) {
        byte[] buffer = new byte[16];
        int length = aid.getBytes(buffer, (short) 0) & 0xFF;
        byte[] result = new byte[length];
        System.arraycopy(buffer, 0, result, 0, length);
        return result;
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            if (result.length() != 0) {
                result.append(' ');
            }
            result.append(String.format("%02X", value & 0xFF));
        }
        return result.toString();
    }

    private static void requireStatus(ResponseAPDU response, int expected) {
        if (response.getSW() != expected) {
            throw new IllegalStateException(String.format(
                    "Expected SW=%04X, got %04X", expected, response.getSW()));
        }
    }
}
