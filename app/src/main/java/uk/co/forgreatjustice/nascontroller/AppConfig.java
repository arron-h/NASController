package uk.co.forgreatjustice.nascontroller;

import android.content.SharedPreferences;

/**
 * Created by arron on 25/01/16.
 */
public class AppConfig
{
	public static String sharedPreferencesName = "nascontroller_preferences";

	public static String interfaceName = "interface_name";
	public static String targetMacAddress = "target_mac_address";
	public static String targetIpAddress = "target_ip_address";
	public static String sshUsername = "ssh_username";
	public static String sshPassword = "ssh_password";
	public static String sshPublicKeyPath = "ssh_public_key_path";
	public static String sshMethod = "ssh_method";

	public enum SSHMethod
	{
		password,
		publickey
	}
}
