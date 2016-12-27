package uk.co.forgreatjustice.nascontroller;

import android.os.AsyncTask;
import android.util.Log;

import java.net.*;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Created by arron on 21/01/16.
 */
public class WakeOnLanJob extends AsyncTask< WakeOnLanJobConfig, Void, Boolean >
{

	private int kMACAddressLength = 6;
	private int kWOLPayloadMultiplier = 16;
	private int kBroadcastAddress = 255;

	private int kPort = 9;

	@Override
	protected Boolean doInBackground( WakeOnLanJobConfig... params )
	{
		WakeOnLanJobConfig wakeOnLanJobConfig = params[ 0 ];

		return wakeUp( wakeOnLanJobConfig );
	}

	public boolean wakeUp( WakeOnLanJobConfig config )
	{

		byte[] macAddressBytes = getByteArray( config.macAddress, config.callbacks );
		if ( macAddressBytes == null )
		{
			config.callbacks.onError( "No MAC address provided." );
			return false;
		}

		byte[] payloadBytes = new byte[ kMACAddressLength + kWOLPayloadMultiplier * macAddressBytes.length ];

		InetAddress address;
		try
		{
			String broadcastAddress = getBroadcastAddress( config.interfaceName, config.callbacks );
			if ( broadcastAddress.isEmpty() )
			{
				config.callbacks.onError( "Could not resolve broadcast address." );
				return false;
			}

			address = InetAddress.getByName( broadcastAddress );
		} catch ( UnknownHostException e )
		{
			config.callbacks.onError( "Failed to translate broadcast address. Try a different interface." );
			return false;
		}

		Arrays.fill( payloadBytes, ( byte ) 0xFF );
		for ( int i = 6; i < payloadBytes.length; i += macAddressBytes.length )
		{
			System.arraycopy( macAddressBytes, 0, payloadBytes, i, macAddressBytes.length );
		}

		Datagram datagram = config.datagramFactory.createDatagram( payloadBytes, payloadBytes.length, address, kPort );

		if ( !datagram.initialiseSocket() )
		{
			config.callbacks.onError( "Failed to initialise socket." );
			return false;
		}

		if ( !datagram.sendOneShot() )
		{
			config.callbacks.onError( "Failed to send WOL packet." );
			return false;
		}

		config.callbacks.onSuccess( "Waking up " + config.macAddress );

		return true;
	}

	private String getBroadcastAddress( String interfaceName, WakeOnLanJobCallbacks callbacks )
	{

		String currentAddress = "";
		try
		{
			NetworkInterface iface = NetworkInterface.getByName( interfaceName );

			Enumeration< InetAddress > addresses = iface.getInetAddresses();
			while ( addresses.hasMoreElements() )
			{
				InetAddress addr = addresses.nextElement();
				currentAddress = addr.getHostAddress();
			}
		} catch ( SocketException e )
		{
			callbacks.onError( "Socket exception: " + e.toString() );
			return "";
		}

		int lastSep = currentAddress.lastIndexOf( '.' );
		if ( lastSep < 0 )
		{
			callbacks.onError( "Malformed IP address when forming broadcast address" );
			return "";
		}

		String broadcastAddress = currentAddress.substring( 0, lastSep );
		broadcastAddress += "." + Integer.toString( kBroadcastAddress );

		return broadcastAddress;
	}

	private byte[] getByteArray( String macAddress, WakeOnLanJobCallbacks callbacks )
	{
		byte[] bytes = new byte[ kMACAddressLength ];

		String[] hexStrings = macAddress.split( "(\\:|\\-)" );
		if ( hexStrings.length != kMACAddressLength )
		{
			callbacks.onError( "MAC address must be 6 groups of hexadecimal digits" );
			return null;
		}

		try
		{
			for ( int i = 0; i < kMACAddressLength; ++i )
			{
				bytes[ i ] = ( byte ) Integer.parseInt( hexStrings[ i ], 16 );
			}
		} catch ( NumberFormatException e )
		{
			callbacks.onError( "Invalid hexadecimal digit in MAC address." );
			return null;
		}

		return bytes;
	}
}
