package uk.co.forgreatjustice.nascontroller;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.view.View;
import android.widget.AdapterView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by arron on 24/01/16.
 */
public class OptionsActivity extends Activity implements AdapterView.OnItemSelectedListener
{

	Spinner _interfaceSpinner;
	EditText _interfaceAddressEditText;
	EditText _targetMacAddressEditText;
	EditText _targetIpEditText;
	EditText _sshUsernameEditText;
	EditText _sshPasswordEditText;
	EditText _sshPublicKeyEditText;

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.options );

		_interfaceSpinner = ( Spinner ) findViewById( R.id.net_interface_spinner );
		_interfaceAddressEditText = ( EditText ) findViewById( R.id.interface_address );
		_targetMacAddressEditText = ( EditText ) findViewById( R.id.mac_address );
		_targetIpEditText = ( EditText ) findViewById( R.id.ip_address );
		_sshUsernameEditText = ( EditText ) findViewById( R.id.ssh_username );
		_sshPasswordEditText = ( EditText ) findViewById( R.id.ssh_password );
		_sshPublicKeyEditText = ( EditText ) findViewById( R.id.ssh_public_key_path );

		_interfaceSpinner.setOnItemSelectedListener( this );

		buildNetworkInterfaceList();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		// Load settings
		SharedPreferences prefs = getSharedPreferences( AppConfig.sharedPreferencesName, 0 );

		String desiredInterface = prefs.getString( AppConfig.interfaceName, "" );
		if ( !desiredInterface.isEmpty() )
		{
			for ( int i = 0; i < _interfaceSpinner.getCount(); ++i )
			{
				if ( _interfaceSpinner.getItemAtPosition( i ).toString().equals( desiredInterface ) )
				{
					_interfaceSpinner.setSelection( i );
					break;
				}
			}
		}

		_targetIpEditText.setText( prefs.getString( AppConfig.targetIpAddress, "" ) );
		_targetMacAddressEditText.setText( prefs.getString( AppConfig.targetMacAddress, "" ) );
		_sshUsernameEditText.setText( prefs.getString( AppConfig.sshUsername, "" ) );
		_sshPasswordEditText.setText( prefs.getString( AppConfig.sshPassword, "" ) );
		_sshPublicKeyEditText.setText( prefs.getString( AppConfig.sshPublicKeyPath, "" ) );

		AppConfig.SSHMethod sshMethod = AppConfig.SSHMethod.values()[ prefs.getInt( AppConfig.sshMethod, 0 ) ];
		if ( sshMethod == AppConfig.SSHMethod.password )
		{
			( ( RadioButton ) findViewById( R.id.ssh_auth_password ) ).setChecked( true );
			( ( RadioButton ) findViewById( R.id.ssh_auth_public_key ) ).setChecked( false );
			onShowPasswordFields( null );
		} else
		{
			( ( RadioButton ) findViewById( R.id.ssh_auth_password ) ).setChecked( false );
			( ( RadioButton ) findViewById( R.id.ssh_auth_public_key ) ).setChecked( true );
			onShowPublicKeyField( null );
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();

		// Save settings
		SharedPreferences prefs = getSharedPreferences( AppConfig.sharedPreferencesName, 0 );
		SharedPreferences.Editor editor = prefs.edit();

		String selectedInterfaceName = _interfaceSpinner.getSelectedItem().toString();
		editor.putString( AppConfig.interfaceName, selectedInterfaceName );
		editor.putString( AppConfig.targetIpAddress, _targetIpEditText.getText().toString() );
		editor.putString( AppConfig.targetMacAddress, _targetMacAddressEditText.getText().toString() );
		editor.putString( AppConfig.sshUsername, _sshUsernameEditText.getText().toString() );
		editor.putString( AppConfig.sshPublicKeyPath, _sshPublicKeyEditText.getText().toString() );
		editor.putString( AppConfig.sshPassword, _sshPasswordEditText.getText().toString() );

		AppConfig.SSHMethod sshMethod;
		if ( ( ( RadioButton ) findViewById( R.id.ssh_auth_password ) ).isChecked() )
		{
			sshMethod = AppConfig.SSHMethod.password;
		} else
		{
			sshMethod = AppConfig.SSHMethod.publickey;
		}

		editor.putInt( AppConfig.sshMethod, sshMethod.ordinal() );

		editor.commit();
	}

	private void buildNetworkInterfaceList()
	{

		ArrayList< String > spinnerArray = new ArrayList< String >();

		try
		{
			Enumeration< NetworkInterface > interfaces = NetworkInterface.getNetworkInterfaces();
			while ( interfaces.hasMoreElements() )
			{
				NetworkInterface iface = interfaces.nextElement();

				// filters out 127.0.0.1 and inactive interfaces
				if ( iface.isLoopback() || !iface.isUp() )
					continue;

				if ( iface.getInterfaceAddresses().size() > 0 )
				{
					spinnerArray.add( iface.getDisplayName() );
				}
			}
		} catch ( SocketException e )
		{
			throw new RuntimeException( e );
		}

		ArrayAdapter< String > spinnerArrayAdapter = new ArrayAdapter< String >(
				this, android.R.layout.simple_spinner_item, spinnerArray );

		spinnerArrayAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
		_interfaceSpinner.setAdapter( spinnerArrayAdapter );
	}

	private void selectIpAddress( NetworkInterface iface )
	{

		String currentAddress = "";
		Enumeration< InetAddress > addresses = iface.getInetAddresses();
		while ( addresses.hasMoreElements() )
		{
			InetAddress addr = addresses.nextElement();
			currentAddress = addr.getHostAddress();
			Log.i( "OptionsActivity", iface.getDisplayName() + " " + currentAddress );
		}

		if ( !currentAddress.isEmpty() )
		{
			_interfaceAddressEditText.setText( currentAddress );
		}
	}

	public void onItemSelected( AdapterView< ? > parent, View view, int pos, long id )
	{

		try
		{
			String ifaceName = ( String ) parent.getItemAtPosition( pos );
			NetworkInterface iface = NetworkInterface.getByName( ifaceName );

			selectIpAddress( iface );
		} catch ( SocketException e )
		{
			Log.e( "OptionsActivity", "Failed to get network interface name. " + e.toString() );
		}
	}

	public void onNothingSelected( AdapterView< ? > parent )
	{
	}

	public void onShowPasswordFields( View itemView )
	{
		toggleVisibility( _sshPublicKeyEditText, R.id.ssh_public_key_label, false );
		toggleVisibility( _sshUsernameEditText, R.id.ssh_username_label, true );
		toggleVisibility( _sshPasswordEditText, R.id.ssh_password_label, true );
	}

	public void onShowPublicKeyField( View itemView )
	{
		toggleVisibility( _sshPublicKeyEditText, R.id.ssh_public_key_label, true );
		toggleVisibility( _sshUsernameEditText, R.id.ssh_username_label, false );
		toggleVisibility( _sshPasswordEditText, R.id.ssh_password_label, false );
	}

	private void toggleVisibility( View view, int labelId, boolean visible)
	{
		view.setVisibility( visible ? View.VISIBLE : View.GONE );
		findViewById( labelId ).setVisibility( visible ? View.VISIBLE : View.GONE );
	}
}
