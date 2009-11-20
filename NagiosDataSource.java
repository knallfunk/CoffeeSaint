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

	public NagiosDataSource(String host, int port, NagiosVersion version)
	{
		this.type = NagiosDataSourceType.TCP;
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

	public NagiosDataSource(String file, NagiosVersion version)
	{
		this.type = NagiosDataSourceType.FILE;
		this.file = file;
		this.version = version;
	}

	public NagiosDataSourceType getType()
	{
		return type;
	}

	public String getHost()
	{
		assert type == NagiosDataSourceType.TCP;
		return host;
	}

	public int getPort()
	{
		assert type == NagiosDataSourceType.TCP;
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
