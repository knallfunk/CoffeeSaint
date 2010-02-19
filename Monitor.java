/* Released under GPL2, (C) 2009-2010 by folkert@vanheusden.com */
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;

public class Monitor
{
	GraphicsDevice device;
	GraphicsConfiguration configuration;
	String deviceName;
	Rectangle bounds;

	public Monitor(final GraphicsDevice device, final GraphicsConfiguration configuration, final String deviceName, final Rectangle bounds)
	{
		this.device = device;
		this.configuration = configuration;
		this.deviceName = deviceName;
		this.bounds = bounds;
	}

	public Rectangle getBounds()
	{
		return bounds;
	}

	public String getDeviceName()
	{
		return deviceName;
	}

	public GraphicsDevice getGraphicsDevice()
	{
		return device;
	}

	public GraphicsConfiguration getGraphicsConfiguration()
	{
		return configuration;
	}
}
