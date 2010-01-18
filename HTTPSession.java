public class HTTPSession
{
	private String host;
	private String cookie;
	private long lastUpdate;

	public HTTPSession(String host, String cookie)
	{
		this.host = host;
		this.cookie = cookie;
		lastUpdate = System.currentTimeMillis();
	}

	public void heartbeat()
	{
		lastUpdate = System.currentTimeMillis();
	}

	public long getLastUpdate()
	{
		return lastUpdate;
	}

	public String getHost()
	{
		return host;
	}

	public String getCookie()
	{
		return cookie;
	}
}
