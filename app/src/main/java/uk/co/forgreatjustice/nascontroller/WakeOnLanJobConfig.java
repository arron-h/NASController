package uk.co.forgreatjustice.nascontroller;

/**
 * Created by arron on 24/01/16.
 */
interface WakeOnLanJobCallbacks
{
	void onSuccess( String message );

	void onError( String message );
}

public class WakeOnLanJobConfig
{
	public String macAddress;
	public String interfaceName;
	public DatagramFactory datagramFactory;

	public WakeOnLanJobCallbacks callbacks;
}
