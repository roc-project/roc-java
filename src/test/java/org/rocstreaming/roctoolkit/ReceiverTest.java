package org.rocstreaming.roctoolkit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReceiverTest {

    private final int SAMPLE_RATE = 44100;
    private ReceiverConfig config;
    private Context context;

    ReceiverTest() {
        this.config = new ReceiverConfig.Builder(SAMPLE_RATE,
                                            ChannelSet.STEREO,
                                            FrameEncoding.PCM_FLOAT)
                                            .automaticTiming(true)
                                        .build();
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
    public void TestValidReceiverCreationAndDeinitialization() {
        assertDoesNotThrow(() -> {
            try (
                    Receiver receiver = new Receiver(context, config);
            ) {}
        });
    }

    @Test
    @SuppressWarnings("resource")
    public void TestInvalidReceiverCreation() {
        assertThrows(IllegalArgumentException.class, () -> new Receiver(null, config));
        assertThrows(IllegalArgumentException.class, () -> new Receiver(context, null));
        assertThrows(IllegalArgumentException.class, () -> {
            ReceiverConfig config = new ReceiverConfig.Builder(-1, ChannelSet.STEREO, FrameEncoding.PCM_FLOAT).build();
            new Receiver(context, config);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ReceiverConfig config = new ReceiverConfig.Builder(SAMPLE_RATE, null, FrameEncoding.PCM_FLOAT).build();
            new Receiver(context, config);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ReceiverConfig config = new ReceiverConfig.Builder(SAMPLE_RATE, ChannelSet.STEREO, null).build();
            new Receiver(context, config);
        });
    }

    @Test
    public void TestValidReceiverBind() throws Exception {
        try (
                Receiver receiver = new Receiver(context, config);
        ) {
            assertDoesNotThrow(() -> receiver.bind(PortType.AUDIO_SOURCE, Protocol.RTP_RS8M_SOURCE, new Address(Family.AUTO, "0.0.0.0", 10001)));
            assertDoesNotThrow(() -> receiver.bind(PortType.AUDIO_REPAIR, Protocol.RS8M_REPAIR, new Address(Family.AUTO, "0.0.0.0", 10002)));
        }
    }

    @Test
    public void TestReceiverBindEphemeralPort() throws Exception {
        try (
                Receiver receiver = new Receiver(context, config);
        ) {
            Address sourceAddress = new Address(Family.AUTO, "0.0.0.0", 0);
            Address repairAddress = new Address(Family.AUTO, "0.0.0.0", 0);
            receiver.bind(PortType.AUDIO_SOURCE, Protocol.RTP_RS8M_SOURCE, sourceAddress);
            receiver.bind(PortType.AUDIO_REPAIR, Protocol.RS8M_REPAIR, repairAddress);
            assertNotEquals(0, sourceAddress.getPort());
            assertNotEquals(0, repairAddress.getPort());
        }
    }

    @Test
    public void TestInvalidReceiverBind() throws Exception {
        try (
                    Receiver receiver = new Receiver(context, config);
        ) {
            assertThrows(IllegalArgumentException.class, () -> receiver.bind(null, Protocol.RTP, new Address(Family.AUTO, "0.0.0.0", 10001)));
            assertThrows(IllegalArgumentException.class, () -> receiver.bind(PortType.AUDIO_SOURCE, null, new Address(Family.AUTO, "0.0.0.0", 10001)));
            assertThrows(IllegalArgumentException.class, () -> receiver.bind(PortType.AUDIO_SOURCE, Protocol.RTP, null));
        }
    }

    @Test
    public void TestInvalidReadFloatArray() throws Exception {
        try (
                Receiver receiver = new Receiver(context, config);
        ) {
            receiver.bind(PortType.AUDIO_SOURCE, Protocol.RTP, new Address(Family.AUTO, "0.0.0.0", 10001));
            receiver.bind(PortType.AUDIO_REPAIR, Protocol.RS8M_REPAIR, new Address(Family.AUTO, "0.0.0.0", 10002));
            assertThrows(IllegalArgumentException.class, () -> receiver.read(null));
        }
    }

    @Test
    public void TestReceiverReadZeroizedArray() throws Exception {
        try (
                Receiver receiver = new Receiver(context, config);
        ) {
            receiver.bind(PortType.AUDIO_SOURCE, Protocol.RTP, new Address(Family.AUTO, "0.0.0.0", 10001));
            receiver.bind(PortType.AUDIO_REPAIR, Protocol.RS8M_REPAIR, new Address(Family.AUTO, "0.0.0.0", 10002));
            float[] samples = { 1.0f, 1.0f};
            receiver.read(samples);
            assertArrayEquals(new float[]{0.0f, 0.0f}, samples);
        }
    }
}
