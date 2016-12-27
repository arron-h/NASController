/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package uk.co.forgreatjustice.nascontroller;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

import android.app.AlertDialog;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends Activity
{
	/**
	 * Called when the activity is first created.
	 */
	interface StatusCallback
	{
		void isReachable(boolean reachable);
	}

	private class StatusJobConfig
	{
		StatusCallback callback;
		String targetIpAddress;
	}

	private class StatusJob extends AsyncTask< StatusJobConfig, Void, Boolean >
	{
		@Override
		protected Boolean doInBackground( StatusJobConfig... params )
		{
			while( !isCancelled() )
			{
				StatusJobConfig config = params[ 0 ];

				try
				{
					boolean isReachable = InetAddress.getByName( config.targetIpAddress ).isReachable( 1000 );
					config.callback.isReachable( isReachable );
				}
				catch( Exception e )
				{
				}

				try
				{
					Thread.sleep( 2000 );
				}
				catch ( InterruptedException e ) {}
			}

			return true;
		}
	}

	private static final String TAG = "MainActivity";
	private StatusJob _statusJob;

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		_statusJob = new StatusJob();
	}

	private void showErrorDialog( final String error )
	{
		MainActivity.this.runOnUiThread( new Runnable()
		{
			public void run()
			{
				AlertDialog.Builder dialog = new AlertDialog.Builder( MainActivity.this );
				dialog.setTitle( "ERROR" );
				dialog.setMessage( error );
				dialog.setNeutralButton( "OK", null );
				dialog.create().show();
			}
		} );
	}

	public void showSuccessDialog( final String message )
	{
		MainActivity.this.runOnUiThread( new Runnable()
		{
			public void run()
			{
				int duration = Toast.LENGTH_SHORT;
				Toast toast = Toast.makeText( getApplicationContext(), message, duration );
				toast.show();
			}
		} );
	}

	public void onStart( View view )
	{
		Log.i( TAG, "onStart" );

		SharedPreferences prefs = getSharedPreferences( AppConfig.sharedPreferencesName, 0 );

		WakeOnLanJobConfig config = new WakeOnLanJobConfig();
		config.macAddress = prefs.getString( AppConfig.targetMacAddress, "" );
		config.interfaceName = prefs.getString( AppConfig.interfaceName, "" );
		config.datagramFactory = DatagramFactory.defaultFactory();

		config.callbacks = new WakeOnLanJobCallbacks()
		{
			@Override
			public void onSuccess( String message )
			{
				showSuccessDialog( message );
			}

			@Override
			public void onError( String message )
			{
				showErrorDialog( message );
			}
		};

		new WakeOnLanJob().executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, config );
	}

	public void onShutdown( View view )
	{
		Log.i( TAG, "onShutdown" );

		SharedPreferences prefs = getSharedPreferences( AppConfig.sharedPreferencesName, 0 );

		SSHExecutorJobConfig config = new SSHExecutorJobConfig();
		config.username = prefs.getString( AppConfig.sshUsername, "" );
		config.password = prefs.getString( AppConfig.sshPassword, "" );
		config.hostIp = prefs.getString( AppConfig.targetIpAddress, "" );
		config.port = 22;
		config.command = "sudo pm-suspend";
		config.publicKeyPath = prefs.getString( AppConfig.sshPublicKeyPath, "" );
		config.method = AppConfig.SSHMethod.values()[ prefs.getInt( AppConfig.sshMethod, 0 ) ];

		config.callbacks = new SSHExecutorJobCallbacks()
		{
			@Override
			public void onSuccess( String message )
			{
				showSuccessDialog( message );
			}

			@Override
			public void onError( String message )
			{
				showErrorDialog( message );
			}
		};

		new SSHExecuterJob().executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, config );
	}

	public void onOptions( View view )
	{
		Intent intent = new Intent( this, OptionsActivity.class );
		startActivity( intent );
	}

	private void handleReachableStatus( final boolean reachable )
	{
		MainActivity.this.runOnUiThread( new Runnable()
		{
			public void run()
			{
				if ( reachable )
				{
					findViewById( R.id.button_start ).setEnabled( false );
					findViewById( R.id.button_shutdown ).setEnabled( true );
				} else
				{
					findViewById( R.id.button_start ).setEnabled( true );
					findViewById( R.id.button_shutdown ).setEnabled( false );
				}
			}
		} );
	}

	private void startMonitorJob()
	{
		SharedPreferences prefs = getSharedPreferences( AppConfig.sharedPreferencesName, 0 );
		final String targetIpAddress = prefs.getString( AppConfig.targetIpAddress, "" );

		if ( targetIpAddress.isEmpty() )
		{
			return;
		}

		// Try determine state of the target machine
		StatusJobConfig config = new StatusJobConfig();
		config.targetIpAddress = targetIpAddress;
		config.callback = new StatusCallback()
		{
			public void isReachable(boolean isReachable) { handleReachableStatus( isReachable ); }
		};

		_statusJob.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, config );
	}

	@Override
	public void onResume()
	{
		super.onResume();

		startMonitorJob();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		_statusJob.cancel( true );
	}
}
