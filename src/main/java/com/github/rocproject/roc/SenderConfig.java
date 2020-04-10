package com.github.rocproject.roc;

/**
 * Sender configuration.
 *
 * SenderConfig object can be instantiated with {@link SenderConfig.Builder SenderConfig.Builder} objects.
 *
 * @see Sender
 * @see SenderConfig.Builder
 */
public class SenderConfig {

    private int frameSampleRate;
    private ChannelSet frameChannels;
    private FrameEncoding frameEncoding;
    private int packetSampleRate;
    private ChannelSet packetChannels;
    private PacketEncoding packetEncoding;
    private long packetLength;
    private int packetInterleaving;
    private int automaticTiming;
    private ResamplerProfile resamplerProfile;
    private FecCode fecCode;
    private int fecBlockSourcePackets;
    private int fecBlockRepairPackets;

    private SenderConfig(int frameSampleRate, ChannelSet frameChannels, FrameEncoding frameEncoding,
                         int packetSampleRate, ChannelSet packetChannels, PacketEncoding packetEncoding,
                         long packetLength, int packetInterleaving, int automaticTiming,
                         ResamplerProfile resamplerProfile, FecCode fecCode, int fecBlockSourcePackets,
                         int fecBlockRepairPackets) {
        this.frameSampleRate = frameSampleRate;
        this.frameChannels = frameChannels;
        this.frameEncoding = frameEncoding;
        this.packetSampleRate = packetSampleRate;
        this.packetChannels = packetChannels;
        this.packetEncoding = packetEncoding;
        this.packetLength = packetLength;
        this.packetInterleaving = packetInterleaving;
        this.automaticTiming = automaticTiming;
        this.resamplerProfile = resamplerProfile;
        this.fecCode = fecCode;
        this.fecBlockSourcePackets = fecBlockSourcePackets;
        this.fecBlockRepairPackets = fecBlockRepairPackets;
    }

    /**
     *  Builder class for {@link SenderConfig SenderConfig} objects
     *
     * @see SenderConfig
     */
    public static class Builder {
        private int frameSampleRate;
        private ChannelSet frameChannels;
        private FrameEncoding frameEncoding;
        private int packetSampleRate;
        private ChannelSet packetChannels;
        private PacketEncoding packetEncoding;
        private long packetLength;
        private int packetInterleaving;
        private int automaticTiming;
        private ResamplerProfile resamplerProfile;
        private FecCode fecCode;
        private int fecBlockSourcePackets;
        private int fecBlockRepairPackets;

        /**
         * Create a Builder object for building {@link SenderConfig SenderConfig}
         *
         * @param frameSampleRate   The rate of the samples in the frames passed to sender.
         *                          Number of samples per channel per second.
         *                          If <code>frameSampleRate</code> and <code>packetSampleRate</code>
         *                          are different, resampler should be enabled.
         * @param frameChannels     The channel set in the frames passed to sender.
         * @param frameEncoding     The sample encoding in the frames passed to sender.
         */
        public Builder(int frameSampleRate, ChannelSet frameChannels, FrameEncoding frameEncoding) {
            this.frameSampleRate = frameSampleRate;
            this.frameChannels = frameChannels;
            this.frameEncoding = frameEncoding;
        }

        /**
         * @param   packetSampleRate The rate of the samples in the packets generated by sender.
         *                           Number of samples per channel per second.
         *                           If zero, default value is used.
         * @return  this Builder
         */
        public Builder packetSampleRate(int packetSampleRate) {
            this.packetSampleRate = packetSampleRate;
            return this;
        }

        /**
         * @param packetChannels     The channel set in the packets generated by sender.
         *                           If null, default value is used.
         * @return this Builder
         */
        public Builder packetChannels(ChannelSet packetChannels) {
            this.packetChannels = packetChannels;
            return this;
        }

        /**
         * @param packetEncoding    The sample encoding in the packets generated by sender.
         *                          If null, default value is used.
         * @return this Builder
         */
        public Builder packetEncoding(PacketEncoding packetEncoding) {
            this.packetEncoding = packetEncoding;
            return this;
        }

        /**
         * @param packetLength      The length of the packets produced by sender, in nanoseconds.
         *                          Number of nanoseconds encoded per packet.
         *                          The samples written to the sender are buffered until the full
         *                          packet is accumulated or the sender is flushed or closed.
         *                          Larger number reduces packet overhead but also increases latency.
         *                          If zero, default value is used.
         * @return this Builder
         */
        public Builder packetLength(long packetLength) {
            this.packetLength = packetLength;
            return this;
        }

        /**
         * @param packetInterleaving Enable packet interleaving.
         *                           If non-zero, the sender shuffles packets before sending them. This
         *                           may increase robustness but also increases latency.
         * @return this Builder
         */
        public Builder packetInterleaving(int packetInterleaving) {
            this.packetInterleaving = packetInterleaving;
            return this;
        }

        /**
         * @param automaticTiming   Enable automatic timing.
         *                          If non-zero, the sender write operation restricts the write rate
         *                          according to the frame_sample_rate parameter. If zero, no
         *                          restrictions are applied.
         * @return this Builder
         */
        public Builder automaticTiming(int automaticTiming) {
            this.automaticTiming = automaticTiming;
            return this;
        }

        /**
         * @param resamplerProfile  Resampler profile to use.
         *                          If non-null, the sender employs resampler if the frame sample rate
         *                          differs from the packet sample rate.
         * @return this Builder
         */
        public Builder resamplerProfile(ResamplerProfile resamplerProfile) {
            this.resamplerProfile = resamplerProfile;
            return this;
        }

        /**
         * @param fecCode           FEC code to use.
         *                          If non-null, the sender employs a FEC codec to generate redundant
         *                          packets which may be used on receiver to restore lost packets.
         *                          This requires both sender and receiver to use two separate source
         *                          and repair ports.
         * @return this Builder
         */
        public Builder fecCode(FecCode fecCode) {
            this.fecCode = fecCode;
            return this;
        }

        /**
         * @param fecBlockSourcePackets Number of source packets per FEC block.
         *                              Used if some FEC code is selected.
         *                              Larger number increases robustness but also increases latency.
         *                              If zero, default value is used.
         * @return this Builder
         */
        public Builder fecBlockSourcePackets(int fecBlockSourcePackets) {
            this.fecBlockSourcePackets = fecBlockSourcePackets;
            return this;
        }

        /**
         * @param fecBlockRepairPackets Number of repair packets per FEC block.
         *                              Used if some FEC code is selected.
         *                              Larger number increases robustness but also increases traffic.
         *                              If zero, default value is used.
         * @return this Builder
         */
        public Builder fecBlockRepairPackets(int fecBlockRepairPackets) {
            this.fecBlockRepairPackets = fecBlockRepairPackets;
            return this;
        }

        /**
         *  Build the {@link SenderConfig SenderConfig} object with <code>Builder</code> parameters.
         * @return the new {@link SenderConfig SenderConfig}
         */
        public SenderConfig build() {
            return new SenderConfig(frameSampleRate, frameChannels, frameEncoding, packetSampleRate,
                                    packetChannels, packetEncoding, packetLength, packetInterleaving,
                                    automaticTiming, resamplerProfile, fecCode, fecBlockSourcePackets,
                                    fecBlockRepairPackets);
        }
    }

    public int getFrameSampleRate() {
        return frameSampleRate;
    }

    public void setFrameSampleRate(int frameSampleRate) {
        this.frameSampleRate = frameSampleRate;
    }

    public ChannelSet getFrameChannels() {
        return frameChannels;
    }

    public void setFrameChannels(ChannelSet frameChannels) {
        this.frameChannels = frameChannels;
    }

    public FrameEncoding getFrameEncoding() {
        return frameEncoding;
    }

    public void setFrameEncoding(FrameEncoding frameEncoding) {
        this.frameEncoding = frameEncoding;
    }

    public int getPacketSampleRate() {
        return packetSampleRate;
    }

    public void setPacketSampleRate(int packetSampleRate) {
        this.packetSampleRate = packetSampleRate;
    }

    public ChannelSet getPacketChannels() {
        return packetChannels;
    }

    public void setPacketChannels(ChannelSet packetChannels) {
        this.packetChannels = packetChannels;
    }

    public PacketEncoding getPacketEncoding() {
        return packetEncoding;
    }

    public void setPacketEncoding(PacketEncoding packetEncoding) {
        this.packetEncoding = packetEncoding;
    }

    public long getPacketLength() {
        return packetLength;
    }

    public void setPacketLength(long packetLength) {
        this.packetLength = packetLength;
    }

    public int getPacketInterleaving() {
        return packetInterleaving;
    }

    public void setPacketInterleaving(int packetInterleaving) {
        this.packetInterleaving = packetInterleaving;
    }

    public int getAutomaticTiming() {
        return automaticTiming;
    }

    public void setAutomaticTiming(int automaticTiming) {
        this.automaticTiming = automaticTiming;
    }

    public ResamplerProfile getResamplerProfile() {
        return resamplerProfile;
    }

    public void setResamplerProfile(ResamplerProfile resamplerProfile) {
        this.resamplerProfile = resamplerProfile;
    }

    public FecCode getFecCode() {
        return fecCode;
    }

    public void setFecCode(FecCode fecCode) {
        this.fecCode = fecCode;
    }

    public int getFecBlockSourcePackets() {
        return fecBlockSourcePackets;
    }

    public void setFecBlockSourcePackets(int fecBlockSourcePackets) {
        this.fecBlockSourcePackets = fecBlockSourcePackets;
    }

    public int getFecBlockRepairPackets() {
        return fecBlockRepairPackets;
    }

    public void setFecBlockRepairPackets(int fecBlockRepairPackets) {
        this.fecBlockRepairPackets = fecBlockRepairPackets;
    }
}
