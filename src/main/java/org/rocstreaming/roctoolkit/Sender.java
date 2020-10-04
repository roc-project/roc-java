package org.rocstreaming.roctoolkit;

import java.io.IOException;

/**
 * <i><code>Sender</code></i> gets an audio stream from the user, encodes it into network packets, and
 * transmits them to a remote receiver.
 *
 * <h3>Context</h3>
 * <p>
 * Sender is automatically attached to a context when opened and detached from it when
 * closed. The user should not close the context until the sender is not closed.
 *
 * Sender work consists of two parts: stream encoding and packet transmission. The
 * encoding part is performed in the sender itself, and the transmission part is
 * performed in the context network worker thread(s).
 *
 * <h3>Lifecycle</h3>
 * <p>
 * A sender is created using {@link Sender#Sender Sender()}. Then it should be bound to a local port
 * using {@link Sender#bind bind()} and connected to a single or multiple remote receiver ports
 * using {@link Sender#connect connect()}. After that, the audio stream is iteratively written to the
 * sender using {@link Sender#write write()}. When the sender is not needed anymore, it is
 * destroyed using {@link Sender#close close()}.
 * <code>Sender</code> class implements {@link AutoCloseable AutoCloseable} so if it is used in a
 * try-with-resources statement the object is closed automatically at the end of the statement.
 *
 * <h3>Ports</h3>
 * <p>
 * The user is responsible for connecting the sender to all necessary receiver ports
 * and selecting the same port types and protocols as at the receiver side.
 *
 * Currently, two configurations are possible:
 * <ul>
 *   <li>
 *     If FEC is disabled, a single port of type {@link PortType#AUDIO_SOURCE AUDIO_SOURCE}
 *     should be connected. The only supported protocol in this case is
 *     {@link Protocol#RTP RTP}. This port will be used to send audio packets.
 *   </li>
 *   <li>
 *     If FEC is enabled, two ports of types {@link PortType#AUDIO_SOURCE AUDIO_SOURCE}
 *     and {@link PortType#AUDIO_REPAIR AUDIO_REPAIR} should be connected. These ports
 *     will be used to send audio packets and redundant data for audio packets, respectively. The supported
 *     protocols in this case depend on the selected FEC code. For example, if
 *     {@link FecCode#RS8M RS8M} is used, the corresponding protocols would be
 *     {@link Protocol#RTP_RS8M_SOURCE RTP_RSM8_SOURCE} and
 *     {@link Protocol#RS8M_REPAIR RSM8_REPAIR}.
 *   </li>
 * </ul>
 *
 * <h3>Resampling</h3>
 * <p>
 * If the sample rate of the user frames and the sample rate of the network packets are
 * different, the sender employs resampler to convert one rate to another.
 *
 * Resampling is a quite time-consuming operation. The user can choose between completely
 * disabling resampling (and so use the same rate for frames and packets) or several
 * resampler profiles providing different compromises between CPU consumption and quality.
 *
 * <h3>Timing</h3>
 * <p>
 * Sender should encode samples at a constant rate that is configured when the sender
 * is created. There are two ways to accomplish this:
 * <ul>
 *   <li>
 *     If the user enabled the automatic timing feature, the sender employs a CPU timer
 *     to block writes until it's time to encode the next bunch of samples according to the
 *     configured sample rate. This mode is useful when the user gets samples from a
 *     non-realtime source, e.g. from an audio file.
 *   </li>
 *   <li>
 *     Otherwise, the samples written to the sender are encoded immediately and the user
 *     is responsible to write samples in time. This mode is useful when the user gets
 *     samples from a realtime source with its own clock, e.g. from an audio device.
 *     Automatic clocking should not be used in this case because the audio device and the
 *     CPU might have slightly different clocks, and the difference will eventually lead
 *     to an underrun or an overrun.
 *   </li>
 * </ul>
 *
 * <h3>Thread-safety</h3>
 * <p>
 * Can be used concurrently
 *
 * <h3>Example</h3>
 * <pre>
 * {@code
 * SenderConfig config = new SenderConfig.Builder(44100,
 *                                                    ChannelSet.STEREO,
 *                                                    FrameEncoding.PCM_FLOAT)
 *                                                    .automaticTiming(true)
 *                                                .resamplerProfile(ResamplerProfile.DISABLE)
 *                                                .fecCode(FecCode.RS8M)
 *                                           .build();
 * try (
 *     Context context = new Context();
 *     Sender sender = new Sender(context, config);
 * ) {
 *     Address senderAddress         = new Address(Family.AUTO, "0.0.0.0", 0);
 *     Address receiverSourceAddress = new Address(Family.AUTO, "127.0.0.1", 10001);
 *     Address receiverRepairAddress = new Address(Family.AUTO, "127.0.0.1", 10002);
 *     sender.bind(senderAddress);
 *     sender.connect(PortType.AUDIO_SOURCE, Protocol.RTP_RS8M_SOURCE, receiverSourceAddress);
 *     sender.connect(PortType.AUDIO_REPAIR, Protocol.RS8M_REPAIR, receiverRepairAddress);
 *     float[] samples = new float[] {2.0f, -2.0f};
 *     sender.write(samples);
 * }
 * }
 * </pre>
 *
 * @see Context
 * @see SenderConfig
 * @see java.lang.AutoCloseable
 */
public class Sender extends NativeObject {

    /**
     * Validate sender constructor parameters and open a new sender if validation is successful.
     *
     * @param context                       should point to an opened context.
     * @param config                        should point to an initialized config.
     *
     * @return                              the native roc sender pointer.
     *
     * @throws IllegalArgumentException     if the arguments are invalid.
     * @throws Exception                    if an error occured when creating the sender.
     */
    private static long validate(Context context, SenderConfig config) throws IllegalArgumentException, Exception {
        if (context == null || config == null) throw new IllegalArgumentException();
        return open(context.getPtr(), config);
    }

    /**
     * Open a new sender.
     * Allocates and initializes a new sender, and attaches it to the context.
     *
     * @param context   should point to an opened context.
     * @param config    should point to an initialized config.
     *
     * @throws IllegalArgumentException if the arguments are invalid.
     * @throws Exception                if an error occured when creating the sender.
     */
    public Sender(Context context, SenderConfig config) throws IllegalArgumentException, Exception {
        super(validate(context, config), context, Sender::close);
    }

    /**
     * Bind the sender to a local port.
     * Should be called exactly once before calling {@link Sender#write write()} first time.
     *
     * If <code>address</code> has zero port, the sender is bound to a randomly chosen ephemeral
     * port. If the function succeeds, the actual port to which the sender was bound is written
     * back to <code>address</code>.
     *
     * @param address       should point to a properly initialized address.
     *
     * @throws IllegalArgumentException if the arguments are invalid.
     * @throws IOException              if the sender is already bound, or the address can't be bound
     *                                  or there are not enough resources.
     */
    public void bind(Address address) throws IllegalArgumentException, IOException {
        bind(getPtr(), address);
    }

    /**
     * Connect the sender to a remote receiver port.
     *
     * Should be called one or multiple times before calling
     * {@link Sender#write write()} first time. The <code>portType</code> and <code>protocol</code>
     * should be the same as they are set at the receiver for this port.
     *
     * @param portType          specifies the receiver port type.
     * @param protocol          specifies the receiver port protocol.
     * @param address           should point to a properly initialized address.
     *
     * @throws IllegalArgumentException     if the arguments are invalid.
     * @throws IOException                  if {@link Sender#write write()} was already called.
     */
    public void connect(PortType portType, Protocol protocol, Address address) throws IllegalArgumentException,
                                                                                      IOException {
        if (portType == null || protocol == null || address == null) throw new IllegalArgumentException();
        connect(getPtr(), portType.getValue(), protocol.getValue(), address);
    }

    /**
     * Encode samples to packets and transmit them to the receiver.
     *
     * Encodes samples to packets and enqueues them for transmission by the context network
     * worker thread. Should be called after {@link Sender#bind bind()} and
     * {@link Sender#connect connect()}.
     *
     * If the automatic timing is enabled, the function blocks until it's time to encode the
     * samples according to the configured sample rate. The function returns after encoding
     * and enqueuing the packets, without waiting when the packets are actually transmitted.
     *
     * @param samples       array of samples to send.
     *
     * @throws IllegalArgumentException if the arguments are invalid.
     * @throws IOException  if the sender is not bound or connected or if there are not
     *                      enough resources.
     */
    public void write(float[] samples) throws IllegalArgumentException, IOException {
        if (samples == null) throw new IllegalArgumentException();
        writeFloats(getPtr(), samples);
    }

    private static native long open(long contextPtr, SenderConfig config) throws IllegalArgumentException, Exception;
    private native void bind(long senderPtr, Address address) throws IllegalArgumentException, IOException;
    private native void connect(long senderPtr, int portType, int protocol, Address address) throws IOException;
    private native void writeFloats(long senderPtr, float[] samples) throws IOException;
    private static native void close(long senderPtr) throws IOException;
}
