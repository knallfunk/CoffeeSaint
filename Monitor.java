/* Released under GPL2, (C) 2009-2010 by folkert@vanheusden.com */
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import javax.swing.JFrame;

public class Monitor
{
	GraphicsDevice device;
	GraphicsConfiguration configuration;
	String deviceName;
	Rectangle bounds;
	JFrame f;

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

	public void setJFrame(final JFrame frame)
	{
		f = frame;
	}

	public JFrame getJFrame()
	{
		return f;
	}
}
