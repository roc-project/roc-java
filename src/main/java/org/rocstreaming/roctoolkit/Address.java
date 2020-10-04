package org.rocstreaming.roctoolkit;

/**
 * Network address.
 *
 * Represents an Internet address, i.e. and IP address plus UDP or TCP port.
 *
 * @see Family
 */
public class Address  {

    private Family family;
    private String ip;
    private int port;

    /**
     * Initialize address.
     *
     * Parses an IP address from a string representation.
     *
     * If <code>family</code> is {@link Family#AUTO AUTO}, the address
     * family is auto-detected from the <code>ip</code> format. Otherwise, the
     * <code>ip</code> format should correspond to the <code>family</code> specified.
     *
     * When <code>Address</code> is used to bind a sender or receiver port, the
     * "0.0.0.0" <code>ip</code> may be used to bind the port to all network interfaces,
     * and the zero <code>port</code> may be used to bind the port to a randomly chosen
     * ephemeral port.
     *
     *
     * @param family    should be {@link Family#AUTO AUTO},
     *                      {@link Family#IPv4 IPv4}, or {@link Family#IPv6 IPv6}
     * @param ip        should be a string with a valid IPv4 or IPv6 address
     * @param port      should be a port number in range [0; 65536)
     *
     * @throws IllegalArgumentException if arguments are invalid
     *
     */
    public Address(Family family, String ip, int port) {
        if (family == null || ip == null) throw new IllegalArgumentException();
        this.family = family;
        this.ip = ip;
        this.port = port;
        init(family, ip, port);
    }


    /**
     * Get address family.
     *
     * @return the address family
     */
    public Family getFamily() {
        return this.family;
    }

    /**
     * Get IP address.
     *
     * @return the IP address
     */
    public String getIp() {
        return ip;
    }

    /**
     * Get address port.
     *
     * @return a non-negative port number
     */
    public int getPort() {
        return port;
    }

    private native void init(Family family, String ip, int port) throws IllegalArgumentException;
}
