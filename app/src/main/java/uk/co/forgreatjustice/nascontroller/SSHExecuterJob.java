package uk.co.forgreatjustice.nascontroller;

import android.util.Log;
import android.os.AsyncTask;

import com.jcraft.jsch.*;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

/**
 * Created by arron on 21/01/16.
 */
public class SSHExecuterJob extends AsyncTask< SSHExecutorJobConfig, Void, Boolean >
{

	private class JschLogger implements com.jcraft.jsch.Logger
	{
		public boolean isEnabled( int level )
		{
			return true;
		}

		public void log( int level, String message )
		{
			Log.i( "JSCH", message );
		}
	}

	private Session _sshSession;

	@Override
	protected Boolean doInBackground( SSHExecutorJobConfig... params )
	{
		SSHExecutorJobConfig config = params[ 0 ];

		try
		{
			JSch.setLogger( new JschLogger() );
			JSch jsch = new JSch();
			_sshSession = jsch.getSession( config.username, config.hostIp, config.port );

			if ( config.method == AppConfig.SSHMethod.password)
			{
				_sshSession.setPassword( config.password );
			}
			else
			{
				jsch.addIdentity( config.publicKeyPath, "passphrase" );
			}

			Properties prop = new Properties();
			prop.put( "StrictHostKeyChecking", "no" );
			_sshSession.setConfig( prop );

			_sshSession.setTimeout( 5000 );
			_sshSession.connect();
		} catch ( Exception e )
		{
			config.callbacks.onError( "Failed to connect: " + e.toString() );
			return false;
		}

		try
		{
			ChannelExec channelSsh = ( ChannelExec ) _sshSession.openChannel( "exec" );
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			channelSsh.setOutputStream( outputStream );

			// Execute
			channelSsh.setCommand( config.command );
			channelSsh.connect();
			channelSsh.disconnect();
		} catch ( Exception e )
		{
			config.callbacks.onError( "Failed to execute shutdown command: " + e.toString() );
			return false;
		}

		config.callbacks.onSuccess( "Executed shutdown successfully." );

		return true;
	}
}
