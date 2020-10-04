package org.rocstreaming.roctoolkit;

import java.io.IOException;

/**
 * <i><code>Receiver</code></i> receives the network packets from multiple senders, decodes audio streams
 * from them, mixes multiple streams into a single stream, and returns it to the user.
 *
 * <h3>Context</h3>
 * <p>
 * Receiver is automatically attached to a context when opened and detached from it when
 * closed. The user should not close the context until the receiver is not closed.
 *
 * Receiver work consists of two parts: packet reception and stream decoding. The
 * decoding part is performed in the receiver itself, and the reception part is
 * performed in the context network worker thread(s).
 *
 * <h3>Lifecycle</h3>
 * <p>
 * A receiver is created using {@link Receiver#Receiver Receiver()}. Then it should be bound to a single
 * or multiple local ports using {@link Receiver#bind bind()}. After that, the audio stream is
 * iteratively read from the receiver using {@link Receiver#read read()}. When the receiver is not
 * needed anymore, it is destroyed using {@link Receiver#close close()}.
 * <code>Receiver</code> class implements {@link AutoCloseable AutoCloseable} so if it is used in a
 * try-with-resources statement the object is closed automatically at the end of the statement.
 *
 * <h3>Ports</h3>
 * <p>
 * Receiver can be bound to multiple network ports of several types. Every port handles
 * packets of the specific protocol selected when the port is bound. It is allowed to
 * bind multiple ports of the same type, typically handling different protocols.
 *
 * Senders can then be connected to some or all receiver ports to transmit one or several
 * packet streams. If a sender employs FEC, it needs to be connected to a pair of
 * {@link PortType#AUDIO_SOURCE AUDIO_SOURCE} and
 * {@link PortType#AUDIO_REPAIR AUDIO_REPAIR} ports which protocols correspond
 * to the employed FEC code. Otherwise, the sender needs to be connected to a single
 * {@link PortType#AUDIO_SOURCE AUDIO_SOURCE} port.
 *
 * <h3>Sessions</h3>
 * <p>
 * Receiver creates a session object for every sender connected to it. Sessions can appear
 * and disappear at any time. Multiple sessions can be active at the same time.
 *
 * A session is identified by the sender address. A session may contain multiple packet
 * streams sent to different receiver ports. If the sender employs FEC, the session will
 * contain source and repair packet streams. Otherwise, the session will contain a single
 * source packet stream.
 *
 * A session is created automatically on the reception of the first packet from a new
 * address and destroyed when there are no packets during a timeout. A session is also
 * destroyed on other events like a large latency underrun or overrun or broken playback,
 * but if the sender continues to send packets, it will be created again shortly.
 *
 * <h3>Mixing</h3>
 * <p>
 * Receiver mixes audio streams from all currently active sessions into a single output
 * stream. The output stream continues no matter how much active sessions there are at
 * the moment. In particular, if there are no sessions, the receiver produces a stream
 * with all zeros. Sessions can be added and removed from the output stream at any time,
 * probably in the middle of a frame.
 *
 * <h3>Resampling</h3>
 * <p>
 * Every session may have a different sample rate. And even if nominally all of them are
 * of the same rate, device frequencies usually differ by a few tens of Hertz.
 *
 * Receiver compensates these differences by adjusting the rate of every session stream to
 * the rate of the receiver output stream using a per-session resampler. The frequencies
 * factor between the sender and the receiver clocks is calculated dynamically for every
 * session based on the session incoming packet queue size.
 *
 * Resampling is a quite time-consuming operation. The user can choose between completely
 * disabling resampling (at the cost of occasional underruns or overruns) or several
 * resampler profiles providing different compromises between CPU consumption and quality.
 *
 * <h3>Timing</h3>
 * <p>
 * Receiver should decode samples at a constant rate that is configured when the receiver
 * is created. There are two ways to accomplish this:
 * <ul>
 *   <li>
 *     If the user enabled the automatic timing feature, the receiver employs a CPU timer
 *     to block reads until it's time to decode the next bunch of samples according to the
 *     configured sample rate. This mode is useful when the user passes samples to a
 *     non-realtime destination, e.g. to an audio file.
 *   </li>
 *   <li>
 *     Otherwise, the samples read from the receiver are decoded immediately and the user
 *     is responsible to read samples in time. This mode is useful when the user passes
 *     samples to a realtime destination with its own clock, e.g. to an audio device.
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
 * ReceiverConfig config = new ReceiverConfig.Builder(44100,
 *                                                    ChannelSet.STEREO,
 *                                                    FrameEncoding.PCM_FLOAT)
 *                                                .automaticTiming(true)
 *                                           .build();
 * try (
 *     Context context = new Context();
 *     Receiver receiver = new Receiver(context, config);
 * ) {
 *     Address receiverSourceAddress = new Address(Family.AUTO, "0.0.0.0", 10001);
 *     Address receiverRepairAddress = new Address(Family.AUTO, "0.0.0.0", 10002);
 *     receiver.bind(PortType.AUDIO_SOURCE, Protocol.RTP_RS8M_SOURCE, receiverSourceAddress);
 *     receiver.bind(PortType.AUDIO_REPAIR, Protocol.RS8M_REPAIR, receiverRepairAddress);
 *     float[] samples = new float[2];
 *     receiver.read(samples);
 * }
 * }
 * </pre>
 *
 * @see Context
 * @see ReceiverConfig
 * @see java.lang.AutoCloseable
 */
public class Receiver extends NativeObject {

    /**
     * Validate receiver constructor parameters and open a new receiver if validation is successful.
     *
     * @param context                       should point to an opened context.
     * @param config                        should point to an initialized config.
     *
     * @return                              the native roc receiver pointer.
     * @throws IllegalArgumentException     if the arguments are invalid.
     * @throws Exception                    if an error occured when creating the receiver.
     */
    private static long validate(Context context, ReceiverConfig config) throws IllegalArgumentException, Exception {
        if (context == null || config == null) throw new IllegalArgumentException();
        return open(context.getPtr(), config);
    }

    /**
     * Open a new receiver.
     *
     * Allocates and initializes a new receiver, and attaches it to the context.
     *
     * @param context       should point to an opened context.
     * @param config        should point to an initialized config.
     *
     * @throws IllegalArgumentException if the arguments are invalid.
     * @throws Exception                if an error occured when creating the receiver.
     */
    public Receiver(Context context, ReceiverConfig config) throws IllegalArgumentException, Exception {
        super(validate(context, config), context, Receiver::close);
    }

    /**
     * Bind the receiver to a local port.
     *
     * May be called multiple times to bind multiple port. May be called at any time.
     * If <code>address</code> has zero port, the receiver is bound to a randomly chosen ephemeral
     * port. If the function succeeds, the actual port to which the receiver was bound is written
     * back to <code>address</code>.
     *
     * @param type          specifies the port type.
     * @param protocol      specifies the port protocol.
     * @param address       should point to a properly initialized address.
     *
     * @throws IllegalArgumentException     if the arguments are invalid.
     * @throws IOException                  if the address can't be bound or if there are not enough resources.
     */
    public void bind(PortType type, Protocol protocol, Address address) throws IllegalArgumentException, IOException {
        if (type == null || protocol == null || address == null) throw new IllegalArgumentException();
        bind(getPtr(), type.getValue(), protocol.getValue(), address);
    }

    /**
     * Read samples from the receiver.
     *
     * Reads network packets received on bound ports, routes packets to sessions, repairs lost
     * packets, decodes samples, resamples and mixes them, and finally stores samples into the
     * provided <code>samples</code> array.
     *
     * If the automatic timing is enabled, the function blocks until it's time to decode the
     * samples according to the configured sample rate.
     *
     * @param samples should point to an initialized <code>float</code> array which will be
     *                filled with samples.
     *
     * @throws IllegalArgumentException if the arguments are invalid.
     * @throws IOException              if there are not enough resources.
     */
    public void read(float[] samples) throws IllegalArgumentException, IOException {
        if (samples == null) throw new IllegalArgumentException();
        readFloats(getPtr(), samples);
    }

    private static native long open(long contextPtr, ReceiverConfig config) throws IllegalArgumentException, Exception;
    private native void bind(long receiverPtr, int type, int protocol, Address address) throws IllegalArgumentException, IOException;
    private native void readFloats(long receiverPtr, float[] samples) throws IOException;
    private static native void close(long receiverPtr) throws IOException;
}
