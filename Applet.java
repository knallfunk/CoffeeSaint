/* Released under GPL2, (C) 2010 by folkert@vanheusden.com */
import com.vanheusden.nagios.*;

import javax.swing.JApplet;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.swing.JFrame;

public class Applet extends JApplet implements Runnable
{
	final static String rcsId = "$Id: Applet.java,v 1.4 2010-11-08 11:56:55 folkert Exp $";
	Thread mainLoop;
	JFrame	frame;
	Gui gui;
	Config config;

	public void paint(final Graphics g)
	{
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			public Object run() {
				if (gui != null)
					gui.paintComponent(g);
				return null;
			}
		});
	}

	public String getAppletInfo()
	{
		return CoffeeSaint.getVersion();
	}

	public void init()
	{
		System.out.println("Please wait while initializing...");

		try {
			getContentPane().setLayout(new BorderLayout());

			config = new Config();
			final Applet a = this;

			AccessController.doPrivileged(new PrivilegedAction<Object>() {
				public Object run() {
					try {
						config.loadAppletParameters(a);
					}
					catch(Exception e) {
						CoffeeSaint.showException(e);
					}
					return null;
				}});

			CoffeeSaint.config = config;
			CoffeeSaint cs = new CoffeeSaint();

			gui = new Gui(config, cs, cs.statistics);
			getContentPane().add(gui, BorderLayout.CENTER);

			if (mainLoop == null)
				mainLoop = new Thread(this);
		}
		catch(Exception e) {
			CoffeeSaint.showException(e);
		}
	}

	public void run()
	{
		for(;;) {
			AccessController.doPrivileged(new PrivilegedAction<Object>() {
				public Object run() {
					try {
						gui.guiLoop();
					}
					catch(Exception e) {
						CoffeeSaint.showException(e);
					}
					return null;
				}
			});
		}
	}

	public void start()
	{
		if (mainLoop != null)
			mainLoop.start();
	}

	public void stop()
	{
		Thread copy = mainLoop;
		mainLoop = null;
		copy.interrupt();
	}
}
