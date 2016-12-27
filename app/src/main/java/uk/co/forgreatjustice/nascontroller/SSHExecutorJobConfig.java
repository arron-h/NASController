package uk.co.forgreatjustice.nascontroller;

/**
 * Created by arron on 24/01/16.
 */
interface SSHExecutorJobCallbacks
{
	void onSuccess( String message );

	void onError( String message );
}

public class SSHExecutorJobConfig
{
	public String username;
	public String password;
	public String hostIp;
	public int port;
	public SSHExecutorJobCallbacks callbacks;
	public String publicKeyPath;
	public AppConfig.SSHMethod method;

	public String command;
}
