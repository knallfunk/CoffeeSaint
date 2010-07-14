/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import com.vanheusden.nagios.*;

import java.net.URL;

public class NagiosDataSource
{
	private NagiosDataSourceType type = NagiosDataSourceType.error;
	private String host = null;
	private int port = -1;
	private NagiosVersion version = NagiosVersion.V1;
	private URL url = null;
	private String file = null;
	private String username = null, password = null;

	public NagiosDataSource(String host, int port, NagiosVersion version, boolean compressed)
	{
		this.type = compressed ? NagiosDataSourceType.ZTCP : NagiosDataSourceType.TCP;
		this.host = host;
		this.port = port;
		this.version = version;
	}

	public NagiosDataSource(URL url, NagiosVersion version)
	{
		this.type = NagiosDataSourceType.HTTP;
		this.url = url;
		this.version = version;
	}

	public NagiosDataSource(URL url, String username, String password, NagiosVersion version)
	{
		this.type = NagiosDataSourceType.HTTP;
		this.url = url;
		this.version = version;
		this.username = username;
		this.password = password;
	}

	public NagiosDataSource(String file, NagiosVersion version)
	{
		this.type = NagiosDataSourceType.FILE;
		this.file = file;
		this.version = version;
	}

	// livestatus
	public NagiosDataSource(String host, int port)
	{
		this.type = NagiosDataSourceType.LS;
		this.host = host;
		this.port = port;
		this.version = NagiosVersion.V3;
	}

	public String getUsername()
	{
		return username;
	}

	public String getPassword()
	{
		return password;
	}

	public NagiosDataSourceType getType()
	{
		return type;
	}

	public String getHost()
	{
		assert type == NagiosDataSourceType.TCP || type == NagiosDataSourceType.ZTCP;
		return host;
	}

	public int getPort()
	{
		assert type == NagiosDataSourceType.TCP || type == NagiosDataSourceType.ZTCP;
		return port;
	}

	public NagiosVersion getVersion()
	{
		return version;
	}

	public URL getURL()
	{
		assert type == NagiosDataSourceType.HTTP;
		return url;
	}

	public String getFile()
	{
		assert type == NagiosDataSourceType.FILE;
		return file;
	}
}
