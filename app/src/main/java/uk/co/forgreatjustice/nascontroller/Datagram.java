package uk.co.forgreatjustice.nascontroller;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by arron on 24/01/16.
 */

public class Datagram
{

	private DatagramPacket _packet;
	private DatagramSocket _socket;

	public Datagram( byte[] payload, int payloadLength, InetAddress address, int port )
	{
		_packet = new DatagramPacket( payload, payloadLength, address, port );
	}

	public boolean initialiseSocket()
	{
		try
		{
			_socket = new DatagramSocket();
		} catch ( SocketException e )
		{
			Log.e( "NASController", "Failed to bind to socket. " + e.toString() );
			return false;
		}

		return true;
	}

	public boolean sendOneShot()
	{
		try
		{
			_socket.send( _packet );
			_socket.close();
		} catch ( IOException e )
		{
			Log.e( "NASController", "Failed to send packet. " + e.toString() );
			return false;
		}

		return true;
	}
}
