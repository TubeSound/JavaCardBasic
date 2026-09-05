package io.github.tubesound.javacardbasic.client;

import com.oracle.javacard.ams.AMService;
import com.oracle.javacard.ams.AMServiceFactory;
import com.oracle.javacard.ams.AMSession;
import com.oracle.javacard.ams.config.AID;
import com.oracle.javacard.ams.config.CAPFile;
import com.oracle.javacard.ams.script.APDUScript;
import com.oracle.javacard.ams.script.ScriptFailedException;
import com.oracle.javacard.ams.script.Scriptable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

/** Oracle JCDK SimulatorへCAPを配備し、第1回のAPDU契約を検証するPC側コード。 */
public final class HelloClient {
    private static final String ISD_AID = "aid:A000000151000000";
    private static final String PACKAGE_AID = "aid:F05455424501";
    private static final String APPLET_AID = "aid:F0545542450101";
    private static final byte[] APPLET_AID_BYTES = AID.from(APPLET_AID).toBytes();
    private static final byte[] GREETING = "Hello, Java Card!".getBytes(StandardCharsets.US_ASCII);
    private static final String DEFAULT_HOST = "socket:localhost:9025";

    private HelloClient() {
    }

    public static void main(String[] args) throws Exception {
        Arguments options = Arguments.parse(args);
        CAPFile cap = CAPFile.from(options.capPath().toString());
        Properties properties = loadProperties(options.propertiesPath());

        AMService service = AMServiceFactory.getInstance("GP2.2");
        service.setProperties(properties);

        AMSession deploy = service.openSession(ISD_AID)
                .load(PACKAGE_AID, cap.getBytes())
                .install(PACKAGE_AID, APPLET_AID, APPLET_AID)
                .close();
        AMSession undeploy = service.openSession(ISD_AID)
                .uninstall(APPLET_AID)
                .unload(PACKAGE_AID)
                .close();

        CardTerminal terminal = getTerminal(options.host());
        if (!terminal.waitForCardPresent(10_000)) {
            throw new CardException("Oracle Simulatorへの接続が10秒以内に確立しませんでした: " + options.host());
        }

        Card card = terminal.connect("*");
        System.out.println("Connected: " + terminal.getName());
        System.out.println("ATR: " + hex(card.getATR().getBytes()));

        Exception failure = null;
        try {
            runManagement(card.getBasicChannel(), deploy);
            runContract(card.getBasicChannel());
        } catch (Exception thrown) {
            failure = thrown;
            throw thrown;
        } finally {
            Exception cleanupFailure = null;
            try {
                runManagement(card.getBasicChannel(), undeploy);
            } catch (Exception thrown) {
                cleanupFailure = thrown;
            }
            try {
                card.disconnect(true);
            } catch (Exception thrown) {
                if (cleanupFailure == null) {
                    cleanupFailure = thrown;
                } else {
                    cleanupFailure.addSuppressed(thrown);
                }
            }
            if (cleanupFailure != null) {
                if (failure == null) {
                    throw cleanupFailure;
                }
                failure.addSuppressed(cleanupFailure);
            }
        }
        System.out.println("All APDU checks passed.");
    }

    private static void runManagement(CardChannel channel, AMSession session) throws ScriptFailedException {
        new ContractScript().appendStep(session).execute(channel);
    }

    private static void runContract(CardChannel channel) throws ScriptFailedException {
        CommandAPDU select = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, APPLET_AID_BYTES);
        CommandAPDU hello = new CommandAPDU(0x80, 0x10, 0x00, 0x00, GREETING.length);
        CommandAPDU maximumShortLe = new CommandAPDU(0x80, 0x10, 0x00, 0x00, 256);
        CommandAPDU unsupportedClass = new CommandAPDU(0x00, 0x10, 0x00, 0x00, GREETING.length);
        CommandAPDU unsupportedInstruction = new CommandAPDU(0x80, 0x7F, 0x00, 0x00, GREETING.length);
        CommandAPDU incorrectParameters = new CommandAPDU(0x80, 0x10, 0x01, 0x00, GREETING.length);
        CommandAPDU insufficientLe = new CommandAPDU(0x80, 0x10, 0x00, 0x00, GREETING.length - 1);

        new ContractScript()
                .expect(select, ExpectedResponse.status(0x9000))
                .expect(hello, ExpectedResponse.dataAndStatus(GREETING, 0x9000))
                .expect(maximumShortLe, ExpectedResponse.dataAndStatus(GREETING, 0x9000))
                .expect(unsupportedClass, ExpectedResponse.status(0x6E00))
                .expect(unsupportedInstruction, ExpectedResponse.status(0x6D00))
                .expect(incorrectParameters, ExpectedResponse.status(0x6A86))
                .expect(insufficientLe, ExpectedResponse.status(0x6C11))
                .execute(channel);
    }

    private static Properties loadProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        return properties;
    }

    private static CardTerminal getTerminal(String connection) throws Exception {
        if (connection.startsWith("socket:")) {
            String[] parts = connection.split(":", 3);
            if (parts.length != 3) {
                throw new IllegalArgumentException("socket接続はsocket:<host>:<port>で指定します");
            }
            TerminalFactory factory = TerminalFactory.getInstance(
                    "SocketCardTerminalFactoryType",
                    List.of(new InetSocketAddress(parts[1], Integer.parseInt(parts[2]))),
                    "SocketCardTerminalProvider");
            return firstTerminal(factory);
        }
        if (connection.startsWith("pcsc:")) {
            String readerName = connection.substring("pcsc:".length());
            return TerminalFactory.getDefault().terminals().list().stream()
                    .filter(terminal -> terminal.getName().equals(readerName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("PC/SC readerが見つかりません: " + readerName));
        }
        throw new IllegalArgumentException("接続はsocket:<host>:<port>またはpcsc:<reader name>で指定します");
    }

    private static CardTerminal firstTerminal(TerminalFactory factory) throws CardException {
        List<CardTerminal> terminals = factory.terminals().list();
        if (terminals.isEmpty()) {
            throw new CardException("Oracle Socket ProviderがTerminalを返しませんでした");
        }
        return terminals.get(0);
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(String.format("%02X", value & 0xFF));
        }
        return result.toString();
    }

    private record Arguments(Path capPath, Path propertiesPath, String host) {
        private static Arguments parse(String[] args) {
            Path cap = null;
            Path properties = null;
            String host = DEFAULT_HOST;
            for (String argument : args) {
                if (argument.startsWith("--cap=")) {
                    cap = Path.of(argument.substring("--cap=".length()));
                } else if (argument.startsWith("--props=")) {
                    properties = Path.of(argument.substring("--props=".length()));
                } else if (argument.startsWith("--host=")) {
                    host = argument.substring("--host=".length());
                } else {
                    throw new IllegalArgumentException("不明な引数: " + argument);
                }
            }
            if (cap == null || properties == null) {
                throw new IllegalArgumentException("--cap=<CAP file>と--props=<properties file>が必要です");
            }
            return new Arguments(cap.toAbsolutePath(), properties.toAbsolutePath(), host);
        }
    }

    private record ExpectedResponse(byte[] data, int statusWord) {
        private static ExpectedResponse status(int statusWord) {
            return new ExpectedResponse(new byte[0], statusWord);
        }

        private static ExpectedResponse dataAndStatus(byte[] data, int statusWord) {
            return new ExpectedResponse(data.clone(), statusWord);
        }

        private boolean matches(ResponseAPDU response) {
            return response.getSW() == statusWord && java.util.Arrays.equals(response.getData(), data);
        }
    }

    private static final class ContractScript extends APDUScript {
        private final Map<CommandAPDU, ExpectedResponse> expected = new IdentityHashMap<>();
        private ExpectedResponse current;

        private ContractScript appendStep(Scriptable<CardChannel, CommandAPDU, ResponseAPDU> step) {
            super.append(step);
            return this;
        }

        private ContractScript expect(CommandAPDU command, ExpectedResponse response) {
            expected.put(command, response);
            super.append(command);
            return this;
        }

        private List<ResponseAPDU> execute(CardChannel channel) throws ScriptFailedException {
            return super.run(channel, command -> {
                current = expected.get(command);
                System.out.println("C: " + hex(command.getBytes()));
                return command;
            }, response -> {
                System.out.println("R: " + hex(response.getBytes()));
                boolean mismatch = current != null && !current.matches(response);
                current = null;
                return mismatch;
            });
        }
    }
}
