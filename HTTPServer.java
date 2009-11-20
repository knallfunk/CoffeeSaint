/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import com.vanheusden.sockets.HTTPRequestData;
import com.vanheusden.sockets.MyHTTPServer;
import com.vanheusden.nagios.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.imageio.*;

class HTTPServer implements Runnable
{
	final Config config;
	final CoffeeSaint coffeeSaint;
	final String adapter;
	final int port;
	final Statistics statistics;
	final Gui gui;
	final List<HTTPLogEntry> hosts = new ArrayList<HTTPLogEntry>();
	//
	int webServerHits, webServer404;
	boolean configNotWrittenToDisk = false;

	public HTTPServer(Config config, CoffeeSaint coffeeSaint, String adapter, int port, Statistics statistics, Gui gui)
	{
		this.config = config;
		this.coffeeSaint = coffeeSaint;
		this.adapter = adapter;
		this.port = port;
		this.statistics = statistics;
		this.gui = gui;
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
		whereTo.add("<HTML><!-- " + CoffeeSaint.getVersion() + "--><HEAD>" + head + "<link rel=\"shortcut icon\" href=\"/favicon.ico\" type=\"image/x-icon\" /></HEAD><BODY><table width=\"100%\" bgcolor=\"#000000\" cellpadding=\"0\" cellspacing=\"0\"><tr><td><A HREF=\"/\"><img src=\"/images/vanheusden02.jpg\" BORDER=\"0\"></A></td></tr></table><BR>\n");
		whereTo.add("<TABLE><TR VALIGN=TOP><TD VALIGN=TOP ALIGN=LEFT WIDTH=225><IMG SRC=\"/images/the_coffee_saint.jpg\" BORDER=\"0\" ALT=\"logo (C) Bas Schuiling\"></TD><TD ALIGN=LEFT>\n");

		whereTo.add("<BR><H1>" + CoffeeSaint.getVersion() + "</H1><BR><BR>");
	}

	public String formatDate(Calendar when)
	{
		SimpleDateFormat dateFormatter = new SimpleDateFormat("E yyyy.MM.dd  hh:mm:ss a zzz");

		return dateFormatter.format(when.getTime());
	}

	public void addPageTail(List<String> whereTo, boolean mainMenu)
	{
		whereTo.add("<BR><BR><BR>");

		if (mainMenu)
			whereTo.add("<A HREF=\"/\">Back to main menu</A><BR>");

		whereTo.add(formatDate(Calendar.getInstance()) + "</TD></TR></TABLE></BODY></HTML>");
	}

	public BufferedImage createBufferedImage(Image image)
	{
		BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
		Graphics g = bufferedImage.createGraphics();

		g.drawImage(image, 0, 0, null);

		return bufferedImage;
	}

	public void sendReply_send_file_from_jar(MyHTTPServer socket, String fileName, String mimeType) throws Exception
	{
		try
		{
			socket.getOutputStream().write(("HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type: " + mimeType + "\r\n\r\n").getBytes());
			InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
			int length = is.available();
			System.out.println("Sending " + fileName + " which is " + length + " bytes long and of type " + mimeType + ".");
			byte [] icon = new byte[length];
			while(length > 0)
			{
				int nRead = is.read(icon);
				if (nRead < 0)
					break;
				socket.getOutputStream().write(icon, 0, nRead);
				length -= nRead;
			}
			socket.close();
		}
		catch(SocketException se)
		{
			// really don't care if the transmit failed; browser
			// probably closed session
		}
	}

	public void sendReply_favicon_ico(MyHTTPServer socket) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/favicon.ico", "image/x-icon");
	}

	public void sendReply_images_configure_png(MyHTTPServer socket) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/Crystal_Clear_action_configure.png", "image/png");
	}

	public void sendReply_images_statistics_png(MyHTTPServer socket) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/Crystal_Clear_mimetype_log.png", "image/png");
	}

	public void sendReply_images_actions_png(MyHTTPServer socket) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/Crystal_Clear_action_player_play.png", "image/png");
	}

	public void sendReply_images_links_png(MyHTTPServer socket) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/Crystal_Clear_mimetype_html.png", "image/png");
	}

	public void sendReply_images_the_coffee_saint_jpg(MyHTTPServer socket) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/the_coffee_saint.jpg", "image/jpeg");
	}

	public void sendReply_images_vanheusden02_jpg(MyHTTPServer socket) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/vanheusden02.jpg", "image/jpeg");
	}

	public void sendReply_robots_txt(MyHTTPServer socket) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/robots.txt", "text/plain");
	}

	public void sendReply_imagejpg(MyHTTPServer socket) throws Exception
	{
		try
		{
			socket.getOutputStream().write("HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type: image/jpeg\r\n\r\n".getBytes());
			Image img = coffeeSaint.loadImage().getImage();
			ImageIO.write(createBufferedImage(img), "jpg", socket.getOutputStream());
			socket.close();
		}
		catch(SocketException se)
		{
			// really don't care if the transmit failed; browser
			// probably closed session
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
		reply.add("<TR><TD>Header:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"header\" VALUE=\"" + config.getHeader() + "\"></TD></TR>\n");
		reply.add("<TR><TD>Host issues:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"host-issue\" VALUE=\"" + config.getHostIssue() + "\"></TD></TR>\n");
		reply.add("<TR><TD>Service issues:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"service-issue\" VALUE=\"" + config.getServiceIssue() + "\"></TD></TR>\n");
		reply.add("<TR><TD>Show header:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"show-header\" VALUE=\"on\" " + (config.getShowHeader() ? "CHECKED" : "") + "></TD></TR>\n");
		reply.add("<TR><TD></TD><TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Submit changes\"></TD></TR>\n");
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

		HTTPRequestData header = socket.findRecord(requestData, "header");
		if (header != null && header.getData() != null)
			config.setHeader(URLDecoder.decode(header.getData(), "US-ASCII"));

		HTTPRequestData hostIssue = socket.findRecord(requestData, "host-issue");
		if (hostIssue != null && hostIssue.getData() != null)
			config.setHostIssue(URLDecoder.decode(hostIssue.getData(), "US-ASCII"));

		HTTPRequestData serviceIssue = socket.findRecord(requestData, "service-issue");
		if (serviceIssue != null && serviceIssue.getData() != null)
			config.setServiceIssue(URLDecoder.decode(serviceIssue.getData(), "US-ASCII"));

		HTTPRequestData show_header = socket.findRecord(requestData, "show-header");
		if (show_header != null && show_header.getData() != null)
			config.setShowHeader(true);
		else
			config.setShowHeader(false);

		reply.add("<BR>\n");
		reply.add("Form processed.<BR>\n");

		reply.add("<BR>\n");
		reply.add("<A HREF=\"/cgi-bin/config-menu.cgi\">Back to the configuration menu</A>");

		addPageTail(reply, true);

		// in case the number of rows has changed or so
		if (config.getRunGui())
			gui.paint(gui.getGraphics());

		socket.sendReply(reply);
	}

	public void sendReply_root(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		reply.add("<TABLE BORDER=\"1\">\n");

		// stats
		reply.add("<TR><TH ROWSPAN=\"2\" BGCOLOR=\"#99a0FF\"><IMG SRC=\"/images/statistics.png\" ALT=\"Statistics\"></TH><TD><A HREF=\"/cgi-bin/statistics.cgi\">CoffeeSaint statistics</A></TD></TR>\n");
		reply.add("<TR><TD><A HREF=\"/cgi-bin/log.cgi\">List of connecting hosts</A></TD></TR>\n");

		// configure
		reply.add("<TR><TH ROWSPAN=\"3\" BGCOLOR=\"#99b0ff\"><IMG SRC=\"/images/configure.png\" ALT=\"Configuration\"></TH><TD><A HREF=\"/cgi-bin/config-menu.cgi\">Configure CoffeeSaint</A></TD></TR>\n");
		reply.add("<TR><TD><A HREF=\"/cgi-bin/reload-config.cgi\">Reload configuration</A></TD></TR>\n");
		if (config.getConfigFilename() == null)
			reply.add("<TR><TD>No configuration-file selected (use --config), save configuration disabled</TD></TR>\n");
		else
		{
			String line = "<TR><TD><A HREF=\"/cgi-bin/write-config.cgi\">Write configuration to " + config.getConfigFilename() + "</A>";
			if (configNotWrittenToDisk == true)
				line += " (changes pending!)";
			line += "</TD></TR>\n";
			reply.add(line);
		}

		// actions
		reply.add("<TR><TH ROWSPAN=\"3\" BGCOLOR=\"#99c0ff\"><IMG SRC=\"/images/actions.png\" ALT=\"Actions\"></TH>");
		if (config.getRunGui())
			reply.add("<TD><A HREF=\"/cgi-bin/force_reload.cgi\">Force reload</A></TD>\n");
		else
			reply.add("<TD>Force reload disabled, not running GUI</TD></TR>\n");
		reply.add("</TR>\n");
		reply.add("<TR><TD><A HREF=\"/cgi-bin/nagios_status.cgi\">Nagios status</A></TD></TR>\n");
		String sample = config.getProblemSound();
		if (sample != null)
			reply.add("<TR><TD><A HREF=\"/cgi-bin/test-sound.cgi\">Test sound (" + sample + ")</A></TD></TR>\n");
		else
			reply.add("<TR><TD>No sound selected</TD></TR>\n");

		// links
		reply.add("<TR><TH ROWSPAN=\"1\" BGCOLOR=\"#99d0ff\"><IMG SRC=\"/images/links.png\" ALT=\"Links\"></TH><TD><A HREF=\"/links.html\">Links relevant to this program</A></TD></TR>\n");

		//
		reply.add("</TABLE>\n");
		addPageTail(reply, false);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_forcereload_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		if (config.getRunGui())
			gui.paint(gui.getGraphics());

		addHTTP200(reply);
		addPageHeader(reply, "<meta http-equiv=\"refresh\" content=\"5;url=/\">");
		reply.add("Nagios status reloaded.");
		addPageTail(reply, true);

		socket.sendReply(reply);
	}


	public void sendReply_links_html(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		reply.add("<H1>Links</H1>\n");
		reply.add("<TABLE>\n");
		reply.add("<TR><TD>CoffeeSaint website (for updates):</TD><TD><A HREF=\"http://vanheusden.com/java/CoffeeSaint/\">http://vanheusden.com/java/CoffeeSaint/</A></TD></TR>\n");
		reply.add("<TR><TD>Source of icons used in web-interface:</TD><TD><A HREF=\"http://commons.wikimedia.org/wiki/Crystal_Clear\">http://commons.wikimedia.org/wiki/Crystal_Clear</A></TD></TR>\n");
		reply.add("<TR><TD>Source of Nagios related software (1):</TD><TD><A HREF=\"http://nagiosexchange.org/\">http://nagiosexchange.org/</A></TD></TR>\n");
		reply.add("<TR><TD>Source of Nagios related software (2):</TD><TD><A HREF=\"http://exchange.nagios.org/\">http://exchange.nagios.org/</A></TD></TR>\n");
		reply.add("<TR><TD>Site of Nagios itself:</TD><TD><A HREF=\"http://www.nagios.org/\">http://www.nagios.org/</A></TD></TR>\n");
		// reply.add("<TR><TD></TD><TD></TD></TR>\n");
		reply.add("</TABLE>\n");

		addPageTail(reply, true);

		socket.sendReply(reply);
	}
	public void sendReply_cgibin_statistics_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		reply.add("<TABLE>\n");
		int nRefreshes = statistics.getNRefreshes();
		reply.add("<TR><TD>Total number of refreshes:</TD><TD>" + nRefreshes + "</TD></TR>\n");
		reply.add("<TR><TD>Total refresh time:</TD><TD>" + statistics.getTotalRefreshTime() + "</TD></TR>\n");
		reply.add("<TR><TD>Average refresh time:</TD><TD>" + (statistics.getTotalRefreshTime() / (double)nRefreshes) + "</TD></TR>\n");
		reply.add("<TR><TD>Total image refresh time:</TD><TD>" + statistics.getTotalImageLoadTime() + "</TD></TR>\n");
		reply.add("<TR><TD>Average image refresh time:</TD><TD>" + (statistics.getTotalImageLoadTime() / (double)nRefreshes) + "</TD></TR>\n");
		reply.add("<TR><TD>Total running time:</TD><TD>" + ((double)(System.currentTimeMillis() - statistics.getRunningSince()) / 1000.0) + "s</TD></TR>\n");
		reply.add("<TR><TD>Number of webserver hits:</TD><TD>" + webServerHits + "</TD></TR>\n");
		reply.add("<TR><TD>Number of 404 pages serverd:</TD><TD>" + webServer404 + "</TD></TR>\n");
		reply.add("<TR><TD>Number of Java exceptions:</TD><TD>" + statistics.getNExceptions() + "</TD></TR>\n");
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
		reply.add("<FONT SIZE=-1>Generated by: " + CoffeeSaint.getVersion() + "</FONT><BR><BR>");

		try
		{
			Calendar rightNow = Calendar.getInstance();

			coffeeSaint.lockProblems();
			coffeeSaint.loadNagiosData();
			coffeeSaint.findProblems();

			JavNag javNag = coffeeSaint.getNagiosData();

			if (config.getShowHeader())
			{
				String header = coffeeSaint.getScreenHeader(javNag, rightNow);
				reply.add(header + "<BR>");
			}

			Color bgColor;
			if (coffeeSaint.getProblems().size() > 0)
				bgColor = config.getBackgroundColor();
			else
				bgColor = coffeeSaint.predictWithColor(rightNow);

			reply.add("<TABLE WIDTH=640 HEIGHT=400 TEXT=\"#" + htmlColorString(config.getTextColor()) + "\" BGCOLOR=\"#" + htmlColorString(bgColor) + "\">\n");

			for(Problem currentProblem : coffeeSaint.getProblems())
			{
				String stateColor = htmlColorString(coffeeSaint.stateToColor(currentProblem.getCurrent_state()));

				String escapeString;
				if (currentProblem.getService() == null)
					escapeString = config.getHostIssue();
				else
					escapeString = config.getServiceIssue();
				String output = coffeeSaint.processStringWithEscapes(escapeString, javNag, rightNow, currentProblem);

				reply.add("<TR><TD BGCOLOR=\"#" + stateColor + "\" TEXT=\"#" + htmlColorString(config.getTextColor()) + "\">" + output + "</TD></TR>\n");
			}

			if (coffeeSaint.getProblems().size() == 0)
			{
				if (config.getNImageUrls() >= 1)
					reply.add("<TR VALIGN=CENTER><TD ALIGN=CENTER><IMG SRC=\"/image.jpg\" BORDER=\"0\"></TD></TR>\n");
				else
					reply.add("<TR VALIGN=CENTER><TD ALIGN=CENTER>All fine.</TD></TR>\n");
			}

			reply.add("</TABLE>\n");
		}
		catch(Exception e)
		{
			throw e;
		}
		finally
		{
			coffeeSaint.unlockProblems();
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
				gui.paint(gui.getGraphics());
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
			statistics.incExceptions();

			reply.add("Problem during storing of configuration-file: " + e);
		}

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_log_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		reply.add("Last connected hosts:<BR>");
		reply.add("<TABLE>");
		reply.add("<TR><TD><B>host</B></TD><TD><B>when</B></TD></TR>");
		for(int index=hosts.size() - 1; index>=0; index--)
			reply.add("<TR><TD>" + hosts.get(index).getAddress().toString().substring(1) + "</TD><TD>" + formatDate(hosts.get(index).getTimestamp()) + "</TD></TR>");
		reply.add("</TABLE>");

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
			System.out.println("Listening on " + adapter + ":" + port);
			socket = new MyHTTPServer(adapter, port);

			for(;;)
			{
				System.out.println("Waiting for connection");
				try
				{
					List<HTTPRequestData> request = socket.acceptConnectionGetRequest();
					String requestType = request.get(0).getName();
					String url = request.get(0).getData().trim();
					int space = url.indexOf(" ");
					if (space != -1)
						url = url.substring(0, space);

					InetSocketAddress remoteAddress = socket.getRemoteSocketAddress();
					System.out.println(formatDate(Calendar.getInstance()) + " HTTP " + remoteAddress.toString().substring(1) + " " + requestType + "-request for: " + url);
					//HTTPLogEntry
					int nHostsKnown = hosts.size();
					if (nHostsKnown > 0 && hosts.get(nHostsKnown - 1).getAddress().getAddress().equals(remoteAddress.getAddress()) == true)
						hosts.get(nHostsKnown - 1).updateTimestamp(Calendar.getInstance());
					else
						hosts.add(new HTTPLogEntry(remoteAddress, Calendar.getInstance()));
					if (nHostsKnown == config.getHttpRememberNHosts()) // it is actually one more due to the add in the previous line
						hosts.remove(0);
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
					else if (url.equals("/cgi-bin/log.cgi"))
						sendReply_cgibin_log_cgi(socket);
					else if (url.equals("/images/statistics.png"))
						sendReply_images_statistics_png(socket);
					else if (url.equals("/images/configure.png"))
						sendReply_images_configure_png(socket);
					else if (url.equals("/images/actions.png"))
						sendReply_images_actions_png(socket);
					else if (url.equals("/images/links.png"))
						sendReply_images_links_png(socket);
					else if (url.equals("/images/the_coffee_saint.jpg"))
						sendReply_images_the_coffee_saint_jpg(socket);
					else if (url.equals("/images/vanheusden02.jpg"))
						sendReply_images_vanheusden02_jpg(socket);
					else if (url.equals("/robots.txt"))
						sendReply_robots_txt(socket);
					else if (url.equals("/favicon.ico"))
						sendReply_favicon_ico(socket);
					else if (url.equals("/links.html"))
						sendReply_links_html(socket);
					else
					{
						sendReply_404(socket, url);
						webServer404++;
					}
				}
				catch(SocketException se)
				{
					// don't care
				}
				catch(Exception e)
				{
					statistics.incExceptions();

					System.err.println("Exception during command processing");
					CoffeeSaint.showException(e);
					socket.close();
				}
			}
		}
		catch(Exception e)
		{
			statistics.incExceptions();

			System.err.println("Cannot create listen socket: " + e);
			CoffeeSaint.showException(e);
			System.exit(127);
		}
	}
}
