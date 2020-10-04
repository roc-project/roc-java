package org.rocstreaming.roctoolkit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static java.lang.Math.sin;
import static org.junit.jupiter.api.Assertions.*;

public class SenderTest {

    private final int SAMPLE_RATE = 44100;
    private final int SINE_RATE = 440;
    private final int SINE_SAMPLES = (SAMPLE_RATE * 5);
    private final int BUFFER_SIZE = 100;
    private SenderConfig config;
    private float[] samples;
    private Context context;

    private void gensine(float[] samples) {
        double t = 0d;
        for (int i = 0; i < samples.length / 2; i++) {
            float s = (float) sin(2 * 3.14159265359 * SINE_RATE / SAMPLE_RATE * t);
            /* Fill samples for left and right channels. */
            samples[i * 2] = s;
            samples[i * 2 + 1] = -s;
            t += 1;
        }
    }

    SenderTest() {
        this.config = new SenderConfig.Builder(SAMPLE_RATE,
                                                ChannelSet.STEREO,
                                                FrameEncoding.PCM_FLOAT)
                                                .automaticTiming(true)
                                        .build();
        this.samples = new float[BUFFER_SIZE];
        gensine(this.samples);
    }

    @BeforeAll
    public static void beforeAll() {
        Logger.setLevel(LogLevel.NONE);
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        this.context = new Context();
    }

    @AfterEach
    public void afterEach() throws Exception {
        this.context.close();
    }

    @Test
    public void TestValidSenderCreationAndDeinitialization() {
        assertDoesNotThrow(() -> {
            try (
                    Sender sender = new Sender(context, config);
            ) {}
        });
    }

    @Test
    @SuppressWarnings("resource")
    public void TestInvalidSenderCreation() {
        assertThrows(IllegalArgumentException.class, () -> new Sender(null, config));
        assertThrows(IllegalArgumentException.class, () -> new Sender(context, null));
        assertThrows(IllegalArgumentException.class, () -> {
            SenderConfig config = new SenderConfig.Builder(-1, ChannelSet.STEREO, FrameEncoding.PCM_FLOAT).build();
            new Sender(context, config);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            SenderConfig config = new SenderConfig.Builder(SAMPLE_RATE, null, FrameEncoding.PCM_FLOAT).build();
            new Sender(context, config);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            SenderConfig config = new SenderConfig.Builder(SAMPLE_RATE, ChannelSet.STEREO, null).build();
            new Sender(context, config);
        });
    }

    @Test
    public void TestValidSenderBind() throws Exception {
        try (
                Sender sender = new Sender(context, config);
        ) {
            assertDoesNotThrow(() -> sender.bind(new Address(Family.AUTO, "0.0.0.0", 0)));
        }
    }

    @Test
    public void TestSenderBindEphemeralPort() throws Exception {
        try (
                Sender sender = new Sender(context, config);
        ) {
            Address senderAddress = new Address(Family.AUTO, "0.0.0.0", 0);
            sender.bind(senderAddress);
            assertNotEquals(0, senderAddress.getPort());
        }
    }

    @Test
    public void TestInvalidSenderBind() throws Exception {
        try (
                Sender sender = new Sender(context, config);
        ) {
            assertThrows(IllegalArgumentException.class, () -> sender.bind(null));
            assertThrows(IOException.class, () -> {
                sender.bind(new Address(Family.AUTO, "0.0.0.0", 0));
                sender.bind(new Address(Family.AUTO, "0.0.0.0", 0));
            });
        }
    }

    @Test
    public void TestValidSenderConnect() throws Exception {
        try (
                Sender sender = new Sender(context, config);
        ) {
            assertDoesNotThrow(() -> {
                sender.bind(new Address(Family.AUTO, "0.0.0.0", 0));
                sender.connect(PortType.AUDIO_SOURCE, Protocol.RTP_RS8M_SOURCE, new Address(Family.AUTO, "127.0.0.1", 10001));
                sender.connect(PortType.AUDIO_REPAIR, Protocol.RS8M_REPAIR, new Address(Family.AUTO, "127.0.0.1", 10002));
            });
        }
    }

    @Test
    public void TestInvalidSenderConnect() throws Exception {
        try (
            Sender sender = new Sender(context, config);
        ) {
            sender.bind(new Address(Family.AUTO, "0.0.0.0", 0));
            assertThrows(IllegalArgumentException.class, () -> {
                sender.connect(null, Protocol.RTP_RS8M_SOURCE, new Address(Family.AUTO, "127.0.0.1", 10001));
            });
            assertThrows(IllegalArgumentException.class, () -> {
                sender.connect(PortType.AUDIO_SOURCE, null, new Address(Family.AUTO, "127.0.0.1", 10001));
            });
            assertThrows(IllegalArgumentException.class, () -> {
                sender.connect(PortType.AUDIO_SOURCE, Protocol.RTP_RS8M_SOURCE, null);
            });
        }
    }

    @Test
    public void TestValidSenderWriteFloatArray() throws Exception {
        try (
            Sender sender = new Sender(context, config);
        ) {
            sender.bind(new Address(Family.AUTO, "0.0.0.0", 0));
            sender.connect(PortType.AUDIO_SOURCE, Protocol.RTP_RS8M_SOURCE, new Address(Family.AUTO, "127.0.0.1", 10001));
            sender.connect(PortType.AUDIO_REPAIR, Protocol.RS8M_REPAIR, new Address(Family.AUTO, "127.0.0.1", 10002));
            for (int i = 0; i < SINE_SAMPLES / BUFFER_SIZE; i++) {
                assertDoesNotThrow(() -> sender.write(samples));
            }
        }
    }

    @Test
    public void TestInvalidSenderWriteFloatArray() throws Exception {
        try (
                Sender sender = new Sender(context, config);
        ) {
            assertThrows(IOException.class, () -> sender.write(samples)); // write before bind
            sender.bind(new Address(Family.AUTO, "0.0.0.0", 0));
            assertThrows(IOException.class, () -> sender.write(samples)); // write before connect
            sender.connect(PortType.AUDIO_SOURCE, Protocol.RTP_RS8M_SOURCE, new Address(Family.AUTO, "127.0.0.1", 10001));
            sender.connect(PortType.AUDIO_REPAIR, Protocol.RS8M_REPAIR, new Address(Family.AUTO, "127.0.0.1", 10002));
            assertThrows(IllegalArgumentException.class, () -> sender.write(null));
        }
    }

    @Test
    public void TestInvalidConnectAfterWrite() throws Exception {
        try (
                Sender sender = new Sender(context, config);
        ) {
            sender.bind(new Address(Family.AUTO, "0.0.0.0", 0));
            assertThrows(IOException.class, () -> {
                sender.connect(PortType.AUDIO_SOURCE, Protocol.RTP_RS8M_SOURCE, new Address(Family.AUTO, "127.0.0.1", 10001));
                sender.connect(PortType.AUDIO_REPAIR, Protocol.RS8M_REPAIR, new Address(Family.AUTO, "127.0.0.1", 10002));
                sender.write(samples);
                sender.connect(PortType.AUDIO_SOURCE, Protocol.RTP_RS8M_SOURCE, new Address(Family.AUTO, "127.0.0.1", 10001));
            });
        }
    }
}
