/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import com.vanheusden.sockets.HTTPRequestData;
import com.vanheusden.sockets.MyHTTPServer;
import com.vanheusden.nagios.Problem;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.imageio.*;

class HTTPServer implements Runnable
{
	Config config;
	CoffeeSaint frame;
	String adapter;
	int port;
	int webServerHits, webServer404;
	boolean configNotWrittenToDisk = false;

	public HTTPServer(Config config, CoffeeSaint frame, String adapter, int port)
	{
		this.config = config;
		this.frame = frame;
		this.adapter = adapter;
		this.port = port;
	}

	public void addHTTP200(List<String> whereTo)
	{
		whereTo.add("HTTP/1.0 200 OK\r\n");
		whereTo.add("Server: " + CoffeeSaint.getVersion() + "\r\n");
		whereTo.add("Connection: close\r\n");
		whereTo.add("Content-Type: text/html\r\n");
		whereTo.add("\r\n");
	}

	public void addPageHeader(List<String> whereTo, String head)
	{
		whereTo.add("<HTML><!-- " + CoffeeSaint.getVersion() + "--><HEAD>" + head + "</HEAD><BODY><table width=\"100%\" bgcolor=\"#000000\" cellpadding=\"0\" cellspacing=\"0\"><tr><td><A HREF=\"/\"><img src=\"http://www.vanheusden.com/images/vanheusden02.jpg?source=coffeesaint\" BORDER=\"0\"></A></td></tr></table><BR>\n");
		whereTo.add("<TABLE><TR VALIGN=TOP><TD VALIGN=TOP ALIGN=LEFT WIDTH=225><IMG SRC=\"http://vanheusden.com/java/CoffeeSaint/coffeesaint.jpg?source=coffeesaint\" BORDER=\"0\" ALT=\"logo (C) Bas Schuiling\"></TD><TD ALIGN=LEFT>\n");

		whereTo.add("<BR><H1>" + CoffeeSaint.getVersion() + "</H1><BR><BR>");
	}

	public void addPageTail(List<String> whereTo, boolean mainMenu)
	{
		whereTo.add("<BR><BR><BR>");

		if (mainMenu)
			whereTo.add("<A HREF=\"/\">Back to main menu</A><BR>");

		SimpleDateFormat dateFormatter = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");

		whereTo.add(dateFormatter.format(Calendar.getInstance().getTime()) + "</TD></TR></TABLE></BODY></HTML>");
	}

	public BufferedImage createBufferedImage(Image image)
	{
		BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
		Graphics g = bufferedImage.createGraphics();

		g.drawImage(image, 0, 0, null);

		return bufferedImage;
	}

	public void sendReply_imagejpg(MyHTTPServer socket) throws Exception
	{
		try
		{
			socket.getOutputStream().write("HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type: image/jpeg\r\n\r\n".getBytes());
			frame.getImageSemaphore().acquire();
			Image img = frame.imageParameters.getImage();
			ImageIO.write(createBufferedImage(img), "jpg", socket.getOutputStream());
			socket.close();
		}
		catch(Exception e)
		{
			throw e;
		}
		finally
		{
			frame.getImageSemaphore().release();
		}
	}

	public void sendReply_cgibin_configmenu_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");
		reply.add("<FORM ACTION=\"/cgi-bin/config-do.cgi\" METHOD=\"POST\">\n");
		reply.add("<TABLE BORDER=\"1\">\n");

		reply.add("<TR><TD>Number of rows:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"nRows\" VALUE=\"" + config.getNRows() + "\"></TD></TR>\n");

		reply.add("<TR><TD>Font:</TD><TD><SELECT NAME=\"font\">");
		GraphicsEnvironment lge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		for(String fontName : lge.getAvailableFontFamilyNames())
		{
			String line = "<OPTION VALUE=\"" + fontName + "\"";

			if (fontName.equalsIgnoreCase(config.getFontName()))
				line += " SELECTED";

			line += ">" + fontName + "</OPTION>\n";

			reply.add(line);
		}
		reply.add("</SELECT></TD></TR>");
		reply.add("<TR><TD>Refresh interval:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"sleepTime\" VALUE=\"" + config.getSleepTime() + "\"></TD></TR>\n");

		reply.add("<TR><TD>Always notify:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"always_notify\" VALUE=\"on\" " + (config.getAlwaysNotify() ? "CHECKED" : "") + "></TD></TR>\n");
		reply.add("<TR><TD>Also acknowledged:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_acknowledged\" VALUE=\"on\" " + (config.getAlsoAcknowledged() ? "CHECKED" : "") + "></TD></TR>\n");
		reply.add("<TR><TD>Show counter:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"counter\" VALUE=\"on\" " + (config.getCounter() ? "CHECKED" : "") + "></TD></TR>\n");
		reply.add("<TR><TD>Text color:</TD><TD><SELECT NAME=\"textColor\">\n");
		for(ColorPair cp : config.getColors())
		{
			String line = "<OPTION VALUE=\"" + cp.getName() + "\"";
			if (config.getTextColorName().equalsIgnoreCase(cp.getName()))
				line += " SELECTED";
			line += ">" + cp.getName() + "</OPTION>\n";
			reply.add(line);
		}
		reply.add("</SELECT></TD></TR>");
		reply.add("<TR><TD>Background color:</TD><TD><SELECT NAME=\"backgroundColor\">\n");
		for(ColorPair cp : config.getColors())
		{
			String line = "<OPTION VALUE=\"" + cp.getName() + "\"";
			if (config.getBackgroundColorName().equalsIgnoreCase(cp.getName()))
				line += " SELECTED";
			line += ">" + cp.getName() + "</OPTION>\n";
			reply.add(line);
		}
		reply.add("</SELECT></TD></TR>");
		reply.add("<TR><TD>Background color OK-status:</TD><TD><SELECT NAME=\"bgColorOk\">\n");
		for(ColorPair cp : config.getColors())
		{
			String line = "<OPTION VALUE=\"" + cp.getName() + "\"";
			if (config.getBackgroundColorOkStatusName().equalsIgnoreCase(cp.getName()))
				line += " SELECTED";
			line += ">" + cp.getName() + "</OPTION>\n";
			reply.add(line);
		}
		for(String image : config.getImageUrls())
			reply.add("<TR><TD>Remove webcam:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"" + image.hashCode() + "\" VALUE=\"on\"><A HREF=\"" + image + "\" TARGET=\"_new\">" + image + "</A></TD></TR>\n");
		reply.add("<TR><TD>Add webcam:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"newWebcam\"></TD></TR>\n");
		reply.add("<TR><TD>Adapt image size:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"adapt-img\" VALUE=\"on\" " + (config.getAdaptImageSize() ? "CHECKED" : "") + "></TD></TR>\n");
		reply.add("<TR><TD>Randomize order of images:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"random-img\" VALUE=\"on\" " + (config.getRandomWebcam() ? "CHECKED" : "") + "></TD></TR>\n");
		reply.add("<TR><TD></TD><TD><INPUT TYPE=\"SUBMIT\"></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("</FORM>\n");
		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_configdo_cgi(MyHTTPServer socket, List<HTTPRequestData> requestData) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		configNotWrittenToDisk = true;

		HTTPRequestData nRows = socket.findRecord(requestData, "nRows");
		if (nRows != null && nRows.getData() != null)
		{
			int newNRows = Integer.valueOf(nRows.getData());
			if (newNRows < 3)
				reply.add("New number of rows invalid, must be >= 3.<BR>\n");
			else
			{
				System.out.println("Setting new # rows to: " + newNRows);
				config.setNRows(newNRows);
			}
		}

		HTTPRequestData font = socket.findRecord(requestData, "font");
		if (font != null && font.getData() != null)
			config.setFontName(URLDecoder.decode(font.getData(), "US-ASCII"));

		HTTPRequestData textColor = socket.findRecord(requestData, "textColor");
		if (textColor != null && textColor.getData() != null)
			config.setTextColor(textColor.getData());

		HTTPRequestData backgroundColor = socket.findRecord(requestData, "backgroundColor");
		if (backgroundColor != null && backgroundColor.getData() != null)
			config.setBackgroundColor(backgroundColor.getData());

		HTTPRequestData bgColorOk = socket.findRecord(requestData, "bgColorOk");
		if (bgColorOk != null && bgColorOk.getData() != null)
			config.setBackgroundColorOkStatus(bgColorOk.getData());

		HTTPRequestData sleepTime = socket.findRecord(requestData, "sleepTime");
		if (sleepTime != null && sleepTime.getData() != null)
		{
			int newSleepTime = Integer.valueOf(sleepTime.getData());
			if (newSleepTime < 1)
				reply.add("New refresh interval is invalid, must be >= 1<BR>\n");
			else
			{
				System.out.println("Setting sleep interval to: " + newSleepTime);
				config.setSleepTime(newSleepTime);
			}
		}

		HTTPRequestData alwaysNotify = socket.findRecord(requestData, "always_notify");
		if (alwaysNotify != null && alwaysNotify.getData() != null)
			config.setAlwaysNotify(true);
		else
			config.setAlwaysNotify(false);

		HTTPRequestData alsoAcknowledged = socket.findRecord(requestData, "also_acknowledged");
		if (alsoAcknowledged != null && alsoAcknowledged.getData() != null)
			config.setAlsoAcknowledged(true);
		else
			config.setAlsoAcknowledged(false);

		HTTPRequestData counter = socket.findRecord(requestData, "counter");
		if (counter != null && counter.getData() != null)
			config.setCounter(true);
		else
			config.setCounter(false);

		HTTPRequestData newWebcam = socket.findRecord(requestData, "newWebcam");
		if (newWebcam != null && newWebcam.getData() != null && newWebcam.getData().equals("") == false)
			config.addImageUrl(URLDecoder.decode(newWebcam.getData(), "US-ASCII"));

		for(HTTPRequestData webcam : requestData)
		{
			try
			{
				int hash = Integer.valueOf(webcam.getName());
				config.removeImageUrl(hash);
			}
			catch(NumberFormatException nfe)
			{
				// ignore
			}
		}

		HTTPRequestData adapt_img = socket.findRecord(requestData, "adapt-img");
		if (adapt_img != null && adapt_img.getData() != null)
			config.setAdaptImageSize(true);
		else
			config.setAdaptImageSize(false);

		HTTPRequestData randomize_img = socket.findRecord(requestData, "random-img");
		if (randomize_img != null && randomize_img.getData() != null)
			config.setRandomWebcam(true);
		else
			config.setRandomWebcam(false);

		reply.add("<BR>\n");
		reply.add("Form processed.<BR>\n");

		reply.add("<BR>\n");
		reply.add("<A HREF=\"/cgi-bin/config-menu.cgi\">Back to the configuration menu</A>");

		addPageTail(reply, true);

		// in case the number of rows has changed or so
		if (config.getRunGui())
			frame.paint(frame.getGraphics());

		socket.sendReply(reply);
	}

	public void sendReply_root(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");
		reply.add("<UL>\n");
		if (config.getRunGui())
			reply.add("<LI><A HREF=\"/cgi-bin/force_reload.cgi\">force reload</A>\n");
		reply.add("<LI><A HREF=\"/cgi-bin/statistics.cgi\">statistics</A>\n");
		reply.add("<LI><A HREF=\"/cgi-bin/nagios_status.cgi\">Nagios status</A>\n");
		reply.add("<LI><A HREF=\"/cgi-bin/config-menu.cgi\">Configure CoffeeSaint</A>\n");
		reply.add("<LI><A HREF=\"/cgi-bin/reload-config.cgi\">Reload configuration</A>\n");
		if (config.getConfigFilename() == null)
			reply.add("<LI>No configuration-file selected (use --config), save configuration disabled\n");
		else
		{
			String line = "<LI><A HREF=\"/cgi-bin/write-config.cgi\">Write configuration to " + config.getConfigFilename() + "</A>";
			if (configNotWrittenToDisk == true)
				line += " (changes pending!)";
			line += "\n";
			reply.add(line);
		}
		String sample = config.getProblemSound();
		if (sample != null)
			reply.add("<LI><A HREF=\"/cgi-bin/test-sound.cgi\">Test sound (" + sample + ")</A>\n");
		reply.add("</UL>\n");
		addPageTail(reply, false);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_forcereload_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		if (config.getRunGui())
			frame.paint(frame.getGraphics());

		addHTTP200(reply);
		addPageHeader(reply, "<meta http-equiv=\"refresh\" content=\"5;url=/\">");
		reply.add("Nagios status reloaded.");
		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_statistics_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");
		try
		{
			frame.getStatisticsSemaphore().acquire();
			reply.add("<TABLE>\n");
			reply.add("<TR><TD>Total number of refreshes:</TD><TD>" + CoffeeSaint.nRefreshes + "</TD></TR>\n");
			reply.add("<TR><TD>Total refresh time:</TD><TD>" + CoffeeSaint.totalRefreshTime + "</TD></TR>\n");
			reply.add("<TR><TD>Average refresh time:</TD><TD>" + (CoffeeSaint.totalRefreshTime / (double)CoffeeSaint.nRefreshes) + "</TD></TR>\n");
			reply.add("<TR><TD>Total image refresh time:</TD><TD>" + CoffeeSaint.totalImageLoadTime + "</TD></TR>\n");
			reply.add("<TR><TD>Average image refresh time:</TD><TD>" + (CoffeeSaint.totalImageLoadTime / (double)CoffeeSaint.nRefreshes) + "</TD></TR>\n");
			reply.add("<TR><TD>Total running time:</TD><TD>" + ((double)(System.currentTimeMillis() - CoffeeSaint.runningSince) / 1000.0) + "s</TD></TR>\n");
			reply.add("<TR><TD>Number of webserver hits:</TD><TD>" + webServerHits + "</TD></TR>\n");
			reply.add("<TR><TD>Number of 404 pages serverd:</TD><TD>" + webServer404 + "</TD></TR>\n");
		}
		catch(Exception e)
		{
			throw e;
		}
		finally
		{
			frame.getStatisticsSemaphore().release();
		}
		reply.add("</TABLE>\n");
		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public String htmlColorString(Color color)
	{
		return String.format("%02x", color.getRed()) + String.format("%02x", color.getGreen()) + String.format("%02x", color.getBlue());
	}

	public void sendReply_cgibin_nagiosstatus_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		reply.add("<HTML><!-- " + CoffeeSaint.getVersion() + "--><HEAD><meta http-equiv=\"refresh\" content=\"" + config.getSleepTime() + "\"></HEAD><BODY>");
		reply.add("Generated by: " + CoffeeSaint.getVersion() + "<BR>");

		try
		{
			frame.getProblemsSemaphore().acquire();
			Color bgColor = config.getBackgroundColorOkStatus();
			if (frame.getProblems().size() > 0)
				bgColor = config.getBackgroundColor();
			reply.add("<TABLE WIDTH=640 HEIGHT=400 TEXT=\"#" + htmlColorString(config.getTextColor()) + "\" BGCOLOR=\"#" + htmlColorString(bgColor) + "\">\n");
			for(Problem currentProblem : frame.getProblems())
			{
				String stateColor = htmlColorString(frame.stateToColor(currentProblem.getCurrent_state()));
				reply.add("<TR><TD BGCOLOR=\"#" + stateColor + "\" TEXT=\"#" + htmlColorString(config.getTextColor()) + "\">" + currentProblem.getMessage() + "</TD></TR>\n");
			}
			if (frame.getProblems().size() == 0)
				reply.add("<TR VALIGN=CENTER><TD ALIGN=CENTER><IMG SRC=\"/image.jpg\" BORDER=\"0\"></TD></TR>\n");
			reply.add("</TABLE>\n");
		}
		catch(Exception e)
		{
			throw e;
		}
		finally
		{
			frame.getProblemsSemaphore().release();
		}

		reply.add("</BODY>");
		reply.add("</HTML>");

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_reloadconfig_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");
		String fileName = config.getConfigFilename();

		config.clearImageList();

		if (fileName != null)
		{
			config.loadConfig(fileName);
			reply.add("Configuration re-loaded from file: " + fileName +".<BR>\n");
			if (config.getRunGui())
				frame.paint(frame.getGraphics());
		}
		else
		{
			config.setDefaultParameterValues();
			reply.add("No configuration-file selected, resetting program defaults.<BR>\n");
		}

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_testsound_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		String sample = config.getProblemSound();
		new PlayWav(sample);

		reply.add("Played audio-file " + sample);

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_writeconfig_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");
		String fileName = config.getConfigFilename();

		try
		{
			config.writeConfig(fileName);
			reply.add("Wrote configuration to file: " + fileName +".<BR>\n");
			configNotWrittenToDisk = false;
		}
		catch(Exception e)
		{
			reply.add("Problem during storing of configuration-file: " + e);
		}

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_404(MyHTTPServer socket, String url) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		reply.add("HTTP/1.0 404 Url not known\r\n");
		reply.add("Connection: close\r\n");
		reply.add("Content-Type: text/html\r\n");
		reply.add("\r\n");
		addPageHeader(reply, "");
		reply.add("URL \"" + url + "\" not known!");
		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void run()
	{
		MyHTTPServer socket;

		try
		{
			socket = new MyHTTPServer(adapter, port);

			for(;;)
			{
				try
				{
					List<HTTPRequestData> request = socket.acceptConnectionGetRequest();
					String requestType = request.get(0).getName();
					String url = request.get(0).getData().trim();
					int space = url.indexOf(" ");
					if (space != -1)
						url = url.substring(0, space);

					System.out.println("HTTP " + requestType + "-request for: " + url);

					webServerHits++;

					if (url.equals("/") || url.equals("/index.html"))
						sendReply_root(socket);
					else if (url.equals("/cgi-bin/force_reload.cgi"))
						sendReply_cgibin_forcereload_cgi(socket);
					else if (url.equals("/cgi-bin/statistics.cgi"))
						sendReply_cgibin_statistics_cgi(socket);
					else if (url.equals("/cgi-bin/nagios_status.cgi"))
						sendReply_cgibin_nagiosstatus_cgi(socket);
					else if (url.equals("/image.jpg"))
						sendReply_imagejpg(socket);
					else if (url.equals("/cgi-bin/config-menu.cgi"))
						sendReply_cgibin_configmenu_cgi(socket);
					else if (url.equals("/cgi-bin/config-do.cgi"))
					{
						List<HTTPRequestData> requestData = socket.getRequestData(request);
						sendReply_cgibin_configdo_cgi(socket, requestData);
					}
					else if (url.equals("/cgi-bin/reload-config.cgi"))
						sendReply_cgibin_reloadconfig_cgi(socket);
					else if (url.equals("/cgi-bin/write-config.cgi"))
						sendReply_cgibin_writeconfig_cgi(socket);
					else if (url.equals("/cgi-bin/test-sound.cgi"))
						sendReply_cgibin_testsound_cgi(socket);
					else
					{
						sendReply_404(socket, url);
						webServer404++;
					}
				}
				catch(Exception e)
				{
					System.err.println("Exception during command processing");
					CoffeeSaint.showException(e);
					socket.close();
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Cannot create listen socket: " + e);
			CoffeeSaint.showException(e);
			System.exit(127);
		}
	}
}
