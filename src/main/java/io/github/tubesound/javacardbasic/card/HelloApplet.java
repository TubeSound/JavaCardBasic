package io.github.tubesound.javacardbasic.card;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;

/**
 * 第1回: データ部のない短いAPDU (Case 2S) に文字列を返す。
 * カード側のコード。main() や System.out は使わず、実行環境から呼ばれる。
 */
public final class HelloApplet extends Applet {
    private static final byte CLA_BASIC = (byte) 0x80;
    private static final byte INS_HELLO = (byte) 0x10;

    private final byte[] greeting;

    private HelloApplet() {
        // Java Cardでは文字列もバイト列として扱う。ここではASCII。
        // コマンドを受け取るたびに new せず、インストール時に確保する。
        greeting = new byte[] {
            'H', 'e', 'l', 'l', 'o', ',', ' ', 'J', 'a', 'v', 'a', ' ', 'C', 'a', 'r', 'd', '!'
        };
        register();
    }

    /** インストール時に実行環境が呼ぶ。この教材では追加の設定値を使わない。 */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new HelloApplet();
    }

    /** 選択時や、その後のコマンド受信時に実行環境が呼ぶ。 */
    @Override
    public void process(APDU apdu) {
        // SELECTは実行環境が処理し、選択されたアプレットにも通知する。
        // この通知をHELLO命令として解釈しない。
        if (selectingApplet()) {
            return;
        }

        // このバッファは実行環境が管理する。フィールドに保存しない。
        byte[] buffer = apdu.getBuffer();
        if (buffer[ISO7816.OFFSET_CLA] != CLA_BASIC) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
        if (buffer[ISO7816.OFFSET_INS] != INS_HELLO) {
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
        if (buffer[ISO7816.OFFSET_P1] != 0 || buffer[ISO7816.OFFSET_P2] != 0) {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }

        short length = (short) greeting.length;
        short expectedLength = apdu.setOutgoing();
        if (expectedLength < length) {
            // 6Cxx: 必要な応答データ長をxxで伝える (初期値は17 = 0x11)。
            ISOException.throwIt((short) (ISO7816.SW_CORRECT_LENGTH_00 | length));
        }

        apdu.setOutgoingLength(length);
        Util.arrayCopyNonAtomic(greeting, (short) 0, buffer, (short) 0, length);
        apdu.sendBytes((short) 0, length);
        // 正常終了時の9000は実行環境が付ける。文字列には含めない。
    }
}
