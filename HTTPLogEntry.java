import java.net.InetSocketAddress;
import java.util.Calendar;

public class HTTPLogEntry
{
	InetSocketAddress address;
	Calendar when;

	public HTTPLogEntry(InetSocketAddress address, Calendar when)
	{
		this.address = address;
		this.when = when;
	}

	public InetSocketAddress getAddress()
	{
		return address;
	}

	public Calendar getTimestamp()
	{
		return when;
	}

	public void updateTimestamp(Calendar now)
	{
		when = now;
	}
}
