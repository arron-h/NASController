package uk.co.forgreatjustice.nascontroller;

import java.net.InetAddress;

/**
 * Created by arron on 24/01/16.
 */
public class DatagramFactory
{

	static private DatagramFactory _factory = new DatagramFactory();

	public Datagram createDatagram( byte[] payload, int payloadLength, InetAddress address, int port )
	{
		return new Datagram( payload, payloadLength, address, port );
	}

	static public DatagramFactory defaultFactory()
	{
		return _factory;
	}
}
