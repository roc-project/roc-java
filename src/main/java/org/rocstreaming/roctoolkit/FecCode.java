package org.rocstreaming.roctoolkit;

import java.util.function.Supplier;

/**
 * Forward Error Correction code.
 */
public enum FecCode {

    /** No FEC code.
     * Compatible with {@link Protocol#RTP RTP} protocol.
     */
    DISABLE( FecCode::getRocFecCodeDisable ),

    /**
     * Default FEC code.
     * Current default is {@link FecCode#RS8M RS8M}.
     */
    DEFAULT( FecCode::getRocFecCodeDefault ),

    /**
     * Reed-Solomon FEC code (RFC 6865) with m=8.
     * Good for small block sizes (below 256 packets).
     * Compatible with {@link Protocol#RTP_RS8M_SOURCE RTP_RS8M_SOURCE}
     * and {@link Protocol#RS8M_REPAIR RS8M_REPAIR} protocols for source
     * and repair ports.
     */
    RS8M( FecCode::getRocFecCodeRS8M ),

    /**
     * LDPC-Staircase FEC code (RFC 6816).
     * Good for large block sizes (above 1024 packets).
     * Compatible with {@link Protocol#RTP_LDPC_SOURCE RTP_LDPC_SOURCE}
     * and {@link Protocol#LDPC_REPAIR LDPC_REPAIR} protocols for source
     * and repair ports.
     */
    LDPC_STAIRCASE( FecCode::getRocFecCodeLDPCSTAIRCASE );

    private final int value;
    FecCode(Supplier<Integer> value) {
        RocLibrary.loadLibrary();
        this.value = value.get();
    }
    public int getValue() {
        return( this.value );
    }

    private static native int getRocFecCodeDisable();
    private static native int getRocFecCodeDefault();
    private static native int getRocFecCodeRS8M();
    private static native int getRocFecCodeLDPCSTAIRCASE();
}
