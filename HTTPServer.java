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
import java.net.URL;
import java.net.URLConnection;
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
		whereTo.add("Date: " + getHTTPDate(Calendar.getInstance()) + "\r\n");
		whereTo.add("Server: " + CoffeeSaint.getVersion() + "\r\n");
		whereTo.add("Connection: close\r\n");
		whereTo.add("Content-Type: text/html\r\n");
		whereTo.add("\r\n");
	}

	public void addPageHeader(List<String> whereTo, String head)
	{
		whereTo.add("<HTML><!-- " + CoffeeSaint.getVersion() + "--><HEAD>" + head + "<link rel=\"shortcut icon\" href=\"/favicon.ico\" type=\"image/x-icon\" /><link href=\"/stylesheet.css\" rel=\"stylesheet\" media=\"screen\"></HEAD><BODY><table width=\"100%\" bgcolor=\"#000000\" cellpadding=\"0\" cellspacing=\"0\"><tr><td><A HREF=\"/\"><img src=\"/images/vanheusden02.jpg\" BORDER=\"0\"></A></td></tr></table><BR>\n");
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

	public long getModificationDate(String fileName) throws Exception
	{
		URL url = getClass().getClassLoader().getResource(fileName);
		URLConnection urlConnection = url.openConnection();
		return urlConnection.getLastModified();
	}

	public String getHTTPDate(Calendar when)
	{
		SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss a zzz");

		return dateFormatter.format(when.getTime());
	}

	public String getModificationDateString(String fileName) throws Exception
	{
		long ts = getModificationDate(fileName);

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(ts);

		return getHTTPDate(calendar);
	}

	public void sendReply_send_file_from_jar(MyHTTPServer socket, String fileName, String mimeType, boolean headRequest) throws Exception
	{
		try
		{
			String reply = "HTTP/1.0 200 OK\r\n";
			reply += "Date: " + getHTTPDate(Calendar.getInstance()) + "\r\n";
			reply += "Server: " + CoffeeSaint.getVersion() + "\r\n";
			reply += "Last-Modified: " + getModificationDateString(fileName) + "\r\n";
			reply += "Connection: close\r\n";
			reply += "Content-Type: " + mimeType + "\r\n";
			reply += "\r\n";
			socket.getOutputStream().write(reply.getBytes());

			if (!headRequest)
			{
				InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
				int length = is.available();
				CoffeeSaint.log.add("Sending " + fileName + " which is " + length + " bytes long and of type " + mimeType + ".");
				byte [] icon = new byte[length];
				while(length > 0)
				{
					int nRead = is.read(icon);
					if (nRead < 0)
						break;
					socket.getOutputStream().write(icon, 0, nRead);
					length -= nRead;
				}
			}
			socket.close();
		}
		catch(SocketException se)
		{
			// really don't care if the transmit failed; browser
			// probably closed session
		}
	}

	public void sendReply_favicon_ico(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/favicon.ico", "image/x-icon", headRequest);
	}

	public void sendReply_images_configure_png(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/Crystal_Clear_action_configure.png", "image/png", headRequest);
	}

	public void sendReply_stylesheet_css(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/stylesheet.css", "text/css", headRequest);
	}

	public void sendReply_images_statistics_png(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/Crystal_Clear_mimetype_log.png", "image/png", headRequest);
	}

	public void sendReply_images_actions_png(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/Crystal_Clear_action_player_play.png", "image/png", headRequest);
	}

	public void sendReply_images_links_png(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/Crystal_Clear_mimetype_html.png", "image/png", headRequest);
	}

	public void sendReply_images_the_coffee_saint_jpg(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/the_coffee_saint.jpg", "image/jpeg", headRequest);
	}

	public void sendReply_images_vanheusden02_jpg(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/vanheusden02.jpg", "image/jpeg", headRequest);
	}

	public void sendReply_robots_txt(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/robots.txt", "text/plain", headRequest);
	}

	public void sendReply_imagejpg(MyHTTPServer socket) throws Exception
	{
		try
		{
			socket.getOutputStream().write("HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type: image/jpeg\r\n\r\n".getBytes());
			Image img = coffeeSaint.loadImage(null, -1, null)[0].getImage();
			ImageIO.write(createBufferedImage(img), "jpg", socket.getOutputStream());
			socket.close();
		}
		catch(SocketException se)
		{
			// really don't care if the transmit failed; browser
			// probably closed session
		}
	}

	public String isChecked(boolean checked)
	{
		if (checked)
			return "CHECKED";

		return "";
	}

	public void colorSelectorHTML(List<String> reply, String name, String selectedColor)
	{
		reply.add("<SELECT NAME=\"" + name + "\">\n");
		for(ColorPair cp : config.getColors())
		{
			String line = "<OPTION VALUE=\"" + cp.getName() + "\"";
			if (selectedColor.equalsIgnoreCase(cp.getName()))
				line += " SELECTED";
			line += ">" + cp.getName() + "</OPTION>\n";
			reply.add(line);
		}
		reply.add("</SELECT>");
	}

	public void stringSelectorHTML(List<String> reply, String name, List<String> list, String selected)
	{
		reply.add("<SELECT NAME=\"" + name + "\">\n");
		for(String option : list)
		{
			String line = "<OPTION VALUE=\"" + option + "\"";

			if (option.equalsIgnoreCase(selected))
				line += " SELECTED";

			line += ">" + option + "</OPTION>\n";

			reply.add(line);
		}
		reply.add("</SELECT>\n");
	}

	public List<String> convertStringArrayToList(String [] array)
	{
		List<String> list = new ArrayList<String>();

		for(int index=0; index<array.length; index++)
			list.add(array[index]);

		return list;
	}

	public void sendReply_cgibin_configmenu_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		reply.add("<FORM ACTION=\"/cgi-bin/config-do.cgi\" METHOD=\"POST\">\n");

		reply.add("<H2>Nagios handling parameters</H2>\n");
		reply.add("<TABLE CLASS=\"b\">\n");
		reply.add("<TR><TD>Always notify:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"always_notify\" VALUE=\"on\" " + isChecked(config.getAlwaysNotify()) + "></TD><TD>Also display when notifications are disabled</TD></TR>\n");
		reply.add("<TR><TD>Also acknowledged:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_acknowledged\" VALUE=\"on\" " + isChecked(config.getAlsoAcknowledged()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Also scheduled downtime</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_scheduled_downtime\" VALUE=\"on\" " + isChecked(config.getAlsoScheduledDowntime()) + "></TD><TD>Also display problems for which downtime has been scheduled</TD></TR>\n");
		reply.add("<TR><TD>Also soft state:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_soft_State\" VALUE=\"on\" " + isChecked(config.getAlsoSoftState()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Also disabled checks:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_disabled_active_checks\" VALUE=\"on\" " + isChecked(config.getAlsoDisabledActiveChecks()) + "></TD><TD>Also display problems for which active checks have been disabled</TD></TR>\n");
		reply.add("<TR><TD>Show services for host with problems:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"show_services_for_host_with_problems\" VALUE=\"on\" " + isChecked(config.getShowServicesForHostWithProblems()) + "></TD><TD></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<BR>\n");

		reply.add("<H2>Look and feel parameters</H2>\n");
		reply.add("<TABLE CLASS=\"b\">\n");

		reply.add("<TR><TD>Number of rows:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"nRows\" VALUE=\"" + config.getNRows() + "\"></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Number of columns:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"problem-columns\" VALUE=\"" + config.getNProblemCols() + "\"></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Flexible number of columns:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"flexible-n-columns\" VALUE=\"on\" " + isChecked(config.getFlexibleNColumns()) + "></TD><TD>Use in combination with number of columns</TD></TR>\n");

		GraphicsEnvironment lge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		List<String> fontNames = convertStringArrayToList(lge.getAvailableFontFamilyNames());
		reply.add("<TR><TD>Font:</TD><TD>");
		stringSelectorHTML(reply, "font", fontNames, config.getFontName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Warning font:</TD><TD>");
		stringSelectorHTML(reply, "warning-font", fontNames, config.getWarningFontName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Critical font:</TD><TD>");
		stringSelectorHTML(reply, "critical-font", fontNames, config.getCriticalFontName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Refresh interval:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"sleepTime\" VALUE=\"" + config.getSleepTime() + "\"></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Reduce text width to fit to screen:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"reduce-textwidth\" VALUE=\"on\" " + isChecked(config.getReduceTextWidth()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Anti-alias:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"anti-alias\" VALUE=\"on\" " + isChecked(config.getAntiAlias()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Show counter:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"counter\" VALUE=\"on\" " + isChecked(config.getCounter()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Verbose:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"verbose\" VALUE=\"on\" " + isChecked(config.getVerbose()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Row border:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"row-border\" VALUE=\"on\" " + isChecked(config.getRowBorder()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Row border color:</TD><TD>\n");
		colorSelectorHTML(reply, "row-border-color", config.getRowBorderColorName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Text color:</TD><TD>\n");
		colorSelectorHTML(reply, "textColor", config.getTextColorName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Warning text color:</TD><TD>\n");
		colorSelectorHTML(reply, "warningTextColor", config.getWarningTextColorName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Critical text color:</TD><TD>\n");
		colorSelectorHTML(reply, "criticalTextColor", config.getCriticalTextColorName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Background color:</TD><TD>\n");
		colorSelectorHTML(reply, "backgroundColor", config.getBackgroundColorName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Background color OK-status:</TD><TD>\n");
		colorSelectorHTML(reply, "bgColorOk", config.getBackgroundColorOkStatusName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Host issues:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"host-issue\" VALUE=\"" + config.getHostIssue() + "\"></TD><TD><A HREF=\"/help-escapes.html\" TARGET=\"_new\">List of escapes</A></TD></TR>\n");
		reply.add("<TR><TD>Service issues:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"service-issue\" VALUE=\"" + config.getServiceIssue() + "\"></TD><TD><A HREF=\"/help-escapes.html\" TARGET=\"_new\">List of escapes</A></TD></TR>\n");
		reply.add("<TR><TD>Header:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"header\" VALUE=\"" + config.getHeader() + "\"></TD><TD><A HREF=\"/help-escapes.html\" TARGET=\"_new\">List of escapes</A></TD></TR>\n");
		reply.add("<TR><TD>Show header:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"show-header\" VALUE=\"on\" " + isChecked(config.getShowHeader()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Scroll header:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"scrolling-header\" VALUE=\"on\" " + isChecked(config.getScrollingHeader()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Scroll pixels/sec:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"scroll-pixels-per-sec\" VALUE=\"" + config.getScrollingHeaderPixelsPerSecond() + "\"></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Sort order:</TD><TD>\n");
		stringSelectorHTML(reply, "sort-order", config.getSortFields(), config.getSortOrder());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Sort numeric:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"sort-order-numeric\" VALUE=\"on\" " + isChecked(config.getSortOrderNumeric()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Sort reverse:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"sort-order-reverse\" VALUE=\"on\" " + isChecked(config.getSortOrderReverse()) + "></TD><TD></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<BR>\n");

		reply.add("<H2>Nagios server(s)</H2>\n");
		reply.add("<TABLE CLASS=\"b\">\n");
		reply.add("<TR><TD><B>type</B></TD><TD><B>Nagios version</B></TD><TD><B>data source</B></TD><TD><B>remove?</B></TD></TR>\n");
		for(NagiosDataSource dataSource : config.getNagiosDataSources())
		{
			String type = "?";
			if (dataSource.getType() == NagiosDataSourceType.TCP)
				type = "tcp";
			else if (dataSource.getType() == NagiosDataSourceType.ZTCP)
				type = "ztcp";
			else if (dataSource.getType() == NagiosDataSourceType.HTTP)
				type = "http";
			else if (dataSource.getType() == NagiosDataSourceType.FILE)
				type = "file";

			String version = "?";
			if (dataSource.getVersion() == NagiosVersion.V1)
				version = "1";
			else if (dataSource.getVersion() == NagiosVersion.V2)
				version = "2";
			else if (dataSource.getVersion() == NagiosVersion.V3)
				version = "3";

			String parameters = "?";
			if (dataSource.getType() == NagiosDataSourceType.TCP || dataSource.getType() == NagiosDataSourceType.ZTCP)
				parameters = dataSource.getHost() + " " + dataSource.getPort();
			else if (dataSource.getType() == NagiosDataSourceType.HTTP)
				parameters = dataSource.getURL().toString();
			else if (dataSource.getType() == NagiosDataSourceType.FILE)
				parameters = dataSource.getFile();

			String serverString = parameters;
			reply.add("<TR><TD>" + type + "</TD><TD>" + version + "</TD><TD>" + parameters + "</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"serverid_" + serverString.hashCode() + "\" VALUE=\"on\"></TD></TR>\n");
		}
		reply.add("<TR>\n");
		reply.add("<TD><SELECT NAME=\"server-add-type\"><OPTION VALUE=\"tcp\">TCP</OPTION><OPTION VALUE=\"ztcp\">compressed tcp</OPTION><OPTION VALUE=\"http\">HTTP</OPTION><OPTION VALUE=\"file\">FILE</OPTION></SELECT></TD>\n");
		reply.add("<TD><SELECT NAME=\"server-add-version\"><OPTION VALUE=\"1\">1</OPTION><OPTION VALUE=\"2\">2</OPTION><OPTION VALUE=\"3\">3</OPTION></SELECT></TD>\n");
		reply.add("<TD><INPUT TYPE=\"TEXT\" NAME=\"server-add-parameters\"></TD>\n");
		reply.add("</TR>\n");
		reply.add("</TABLE>\n");
		reply.add("TCP requires an ip-address followed by a space and a port-number in the parameters field.<BR>\n");
		reply.add("<BR>\n");

		reply.add("<H2>Webcams</H2>\n");
		reply.add("<TABLE CLASS=\"b\">\n");
		for(String image : config.getImageUrls())
			reply.add("<TR><TD>Remove webcam:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"webcam_" + image.hashCode() + "\" VALUE=\"on\"><A HREF=\"" + image + "\" TARGET=\"_new\">" + image + "</A></TD></TR>\n");
		reply.add("<TR><TD>Add webcam:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"newWebcam\"></TD></TR>\n");
		reply.add("<TR><TD>Adapt image size:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"adapt-img\" VALUE=\"on\" " + isChecked(config.getAdaptImageSize()) + "> (fit below list of problems)</TD></TR>\n");
		reply.add("<TR><TD>Randomize order of images:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"random-img\" VALUE=\"on\" " + isChecked(config.getRandomWebcam()) + "></TD></TR>\n");
		reply.add("<TR><TD>Number of columns:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"cam-cols\" VALUE=\"" + config.getCamCols() + "\"></TD></TR>\n");
		reply.add("<TR><TD>Number of rows:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"cam-rows\" VALUE=\"" + config.getCamRows() + "\"></TD></TR>\n");
		reply.add("<TR><TD>Keep aspect ratio:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"keep-aspect-ratio\" VALUE=\"on\" " + isChecked(config.getKeepAspectRatio()) + "></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<BR>\n");

		reply.add("<H2>Submit changes</H2>\n");
		reply.add("<INPUT TYPE=\"SUBMIT\" VALUE=\"Submit changes!\"><BR>\n");
		reply.add("<BR>\n");

		reply.add("</FORM>\n");

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public boolean getCheckBox(MyHTTPServer socket, List<HTTPRequestData> requestData, String fieldName)
	{
		HTTPRequestData field = socket.findRecord(requestData, fieldName);
		if (field != null && field.getData() != null)
			return true;

		return false;
	}

	public String getField(MyHTTPServer socket, List<HTTPRequestData> requestData, String fieldName)
	{
		HTTPRequestData field = socket.findRecord(requestData, fieldName);
		if (field != null && field.getData() != null)
			return field.getData().trim();

		return "";
	}

	public String getFieldDecoded(MyHTTPServer socket, List<HTTPRequestData> requestData, String fieldName) throws Exception
	{
		return URLDecoder.decode(getField(socket, requestData, fieldName), "US-ASCII");
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
				CoffeeSaint.log.add("Setting new # rows to: " + newNRows);
				config.setNRows(newNRows);
			}
		}

		HTTPRequestData nCols = socket.findRecord(requestData, "problem-columns");
		if (nCols != null && nCols.getData() != null)
		{
			int newNCols = Integer.valueOf(nCols.getData());
			if (newNCols < 1)
				reply.add("New number of rows invalid, must be >= 1.<BR>\n");
			else
			{
				CoffeeSaint.log.add("Setting new # rows to: " + newNCols);
				config.setNProblemCols(newNCols);
			}
		}

		config.setFlexibleNColumns(getCheckBox(socket, requestData, "flexible-n-columns"));

		config.setFontName(getFieldDecoded(socket, requestData, "font"));

		config.setWarningFontName(getFieldDecoded(socket, requestData, "warning-font"));

		config.setCriticalFontName(getFieldDecoded(socket, requestData, "critical-font"));

		config.setTextColor(getField(socket, requestData, "textColor"));
		config.setWarningTextColor(getField(socket, requestData, "warningTextColor"));
		config.setCriticalTextColor(getField(socket, requestData, "criticalTextColor"));

		config.setBackgroundColor(getField(socket, requestData, "backgroundColor"));

		config.setBackgroundColorOkStatus(getField(socket, requestData, "bgColorOk"));

		String sleepTime = getField(socket, requestData, "sleepTime");
		if (sleepTime.equals("") == false)
		{
			int newSleepTime = Integer.valueOf(sleepTime);
			if (newSleepTime < 1)
				reply.add("New refresh interval is invalid, must be >= 1<BR>\n");
			else
			{
				CoffeeSaint.log.add("Setting sleep interval to: " + newSleepTime);
				config.setSleepTime(newSleepTime);
			}
		}

		config.setAlwaysNotify(getCheckBox(socket, requestData, "always_notify"));

		config.setAlsoAcknowledged(getCheckBox(socket, requestData, "also_acknowledged"));

		config.setAlsoScheduledDowntime(getCheckBox(socket, requestData, "also_scheduled_downtime"));

		config.setAlsoSoftState(getCheckBox(socket, requestData, "also_soft_State"));

		config.setAlsoDisabledActiveChecks(getCheckBox(socket, requestData, "also_disabled_active_checks"));

		config.setShowServicesForHostWithProblems(getCheckBox(socket, requestData, "show_services_for_host_with_problems"));

		config.setCounter(getCheckBox(socket, requestData, "counter"));

		String newWebcam = getFieldDecoded(socket, requestData, "newWebcam");
		if (newWebcam.equals("") == false)
			config.addImageUrl(newWebcam);

		for(HTTPRequestData webcam : requestData)
		{
			try
			{
				String fieldName = webcam.getName();
				if (fieldName.substring(0, 7).equals("webcam_"))
				{
					int hash = Integer.valueOf(fieldName.substring(7));
					config.removeImageUrl(hash);
				}
			}
			catch(IndexOutOfBoundsException ioobe)
			{
				// ignore, probably not a webcam-field
			}
		}

		config.setAdaptImageSize(getCheckBox(socket, requestData, "adapt-img"));

		config.setAntiAlias(getCheckBox(socket, requestData, "anti-alias"));

		config.setRandomWebcam(getCheckBox(socket, requestData, "random-img"));

		config.setReduceTextWidth(getCheckBox(socket, requestData, "reduce-textwidth"));

		config.setHeader(getFieldDecoded(socket, requestData, "header"));

		config.setScrollingHeader(getCheckBox(socket, requestData, "scrolling-header"));

		String scrollSpeed = getField(socket, requestData, "scroll-pixels-per-sec");
		if (scrollSpeed.equals("") == false)
		{
			int newScrollSpeed = Integer.valueOf(scrollSpeed);
			if (newScrollSpeed < 1)
				reply.add("New pixels/sec-value is invalid, must be >= 1<BR>\n");
			else
				config.setScrollingHeaderPixelsPerSecond(newScrollSpeed);
		}

		config.setHostIssue(getFieldDecoded(socket, requestData, "host-issue"));

		config.setServiceIssue(getFieldDecoded(socket, requestData, "service-issue"));

		config.setShowHeader(getCheckBox(socket, requestData, "show-header"));

		boolean son = getCheckBox(socket, requestData, "sort-order-numeric");
		boolean sor = getCheckBox(socket, requestData, "sort-order-reverse");
		config.setSortOrder(getFieldDecoded(socket, requestData, "sort-order"), son, sor);

		config.setVerbose(getCheckBox(socket, requestData, "verbose"));

		config.setRowBorder(getCheckBox(socket, requestData, "row-border"));
		config.setRowBorderColor(getField(socket, requestData, "row-border-color"));

		// add server
		String server_add_parameters = getFieldDecoded(socket, requestData, "server-add-parameters");
		if (server_add_parameters.equals("") == false)
		{
			NagiosDataSourceType ndst = null;
			NagiosVersion nv = null;

			String type = getField(socket, requestData, "server-add-type");
			if (type.equals("tcp"))
				ndst = NagiosDataSourceType.TCP;
			else if (type.equals("ztcp"))
				ndst = NagiosDataSourceType.ZTCP;
			else if (type.equals("http"))
				ndst = NagiosDataSourceType.HTTP;
			else if (type.equals("file"))
				ndst = NagiosDataSourceType.FILE;

			String version = getField(socket, requestData, "server-add-version");
			if (version.equals("1"))
				nv = NagiosVersion.V1;
			else if (version.equals("2"))
				nv = NagiosVersion.V2;
			else if (version.equals("3"))
				nv = NagiosVersion.V3;

			if (ndst == null && nv == null)
			{
				reply.add("Field missing or invalid data in field for server-add.<BR>\n");
			}
			else
			{
				if (ndst == NagiosDataSourceType.TCP || ndst == NagiosDataSourceType.ZTCP)
				{
					int port = 33333;
					int space = server_add_parameters.indexOf(" ");
					if (space != -1)
					{
						port = Integer.valueOf(server_add_parameters.substring(space + 1).trim());
						server_add_parameters = server_add_parameters.substring(0, space);
					}

					config.addNagiosDataSource(new NagiosDataSource(server_add_parameters, port, nv, ndst == NagiosDataSourceType.ZTCP));
				}
				else if (ndst == NagiosDataSourceType.HTTP)
					config.addNagiosDataSource(new NagiosDataSource(new URL(URLDecoder.decode(server_add_parameters, "US-ASCII")), nv));
				else if (ndst == NagiosDataSourceType.FILE)
					config.addNagiosDataSource(new NagiosDataSource(server_add_parameters, nv));
			}
		}

		for(HTTPRequestData server : requestData)
		{
			try
			{
				String fieldName = server.getName();
				if (fieldName.substring(0, 9).equals("serverid_"))
				{
					int hash = Integer.valueOf(fieldName.substring(9));
					config.removeServer(hash);
				}
			}
			catch(IndexOutOfBoundsException ioobe)
			{
				// ignore, probably not a webcam-field
			}
		}

		String cam_rows = getField(socket, requestData, "cam-rows");
		if (cam_rows.equals("") == false)
		{
			int camRows = Integer.valueOf(cam_rows);
			if (camRows < 1)
				camRows = 1;
			else if (camRows > 8)
				camRows = 8;

			CoffeeSaint.log.add("Setting number of cam-rows to: " + camRows);
			config.setCamRows(camRows);
		}

		String cam_cols = getField(socket, requestData, "cam-cols");
		if (cam_cols.equals("") == false)
		{
			int camCols = Integer.valueOf(cam_cols);
			if (camCols < 1)
				camCols = 1;
			else if (camCols > 8)
				camCols = 8;

			CoffeeSaint.log.add("Setting number of cam-cols to: " + camCols);
			config.setCamCols(camCols);
		}

		config.setKeepAspectRatio(getCheckBox(socket, requestData, "keep-aspect-ratio"));

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

	public void sendReply_cgibin_listlog_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		reply.add("<H2>Log</H2>");
		reply.add("<PRE>\n");
		List<String> log = CoffeeSaint.log.get();
		for(int index=log.size()-1; index>=0; index--)
			reply.add(log.get(index) + "\n");
		reply.add("</PRE>\n");

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_listall_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		reply.add("<TABLE CLASS=\"b\">\n");
		reply.add("<TR><TD><B>Host</B></TD><TD><B>host status</B></TD><TD><B>Service</B></TD><TD><B>service status</B></TD></TR>\n");

		try
		{
			coffeeSaint.lockProblems();
			coffeeSaint.loadNagiosData(null, -1, null);
			JavNag javNag = coffeeSaint.getNagiosData();

			for(Host currentHost : javNag.getListOfHosts())
			{
				String hostState = currentHost.getParameter("state_type").equals("1") ? currentHost.getParameter("current_state") : "0";
				String htmlHostStateColor = htmlColorString(coffeeSaint.stateToColor(hostState.equals("1") ? "2" : hostState));

				reply.add("<TR><TD>" + currentHost.getHostName() + "</TD><TD BGCOLOR=\"#" + htmlHostStateColor + "\">" + coffeeSaint.hostState(hostState) + "</TD>");

				boolean first = true;
				for(Service currentService : currentHost.getServices())
				{
					String serviceState = currentService.getParameter("state_type").equals("1") ? currentService.getParameter("current_state") : "0";
					String htmlServiceStateColor = htmlColorString(coffeeSaint.stateToColor(serviceState));

					if (first)
					{
						first = false;
						reply.add("<TD>" + currentService.getServiceName() + "</TD><TD BGCOLOR=\"#" + htmlServiceStateColor + "\">" + coffeeSaint.serviceState(serviceState) + "</TD></TR>\n");
					}
					else
					{
						reply.add("<TR><TD></TD><TD></TD><TD>" + currentService.getServiceName() + "</TD><TD BGCOLOR=\"#" + htmlServiceStateColor + "\">" + coffeeSaint.serviceState(serviceState) + "</TD></TR>\n");
					}
				}
			}
		}
		catch(Exception e)
		{
			throw e;
		}
		finally
		{
			coffeeSaint.unlockProblems();
		}
		reply.add("</TABLE>\n");

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_root(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		reply.add("<TABLE CLASS=\"b\">\n");

		// stats
		reply.add("<TR><TH ROWSPAN=\"4\"><IMG SRC=\"/images/statistics.png\" ALT=\"Statistics\"></TH><TD><A HREF=\"/cgi-bin/statistics.cgi\">CoffeeSaint statistics</A></TD></TR>\n");
		reply.add("<TR><TD><A HREF=\"/cgi-bin/log.cgi\">List of connecting hosts</A></TD></TR>\n");
		reply.add("<TR><TD><A HREF=\"/cgi-bin/list-all.cgi\">List of hosts/services</A></TD></TR>\n");
		reply.add("<TR><TD><A HREF=\"/cgi-bin/list-log.cgi\">Show log</A></TD></TR>\n");

		// configure
		reply.add("<TR><TH ROWSPAN=\"3\"><IMG SRC=\"/images/configure.png\" ALT=\"Configuration\"></TH><TD><A HREF=\"/cgi-bin/config-menu.cgi\">Configure CoffeeSaint</A></TD></TR>\n");
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
		reply.add("<TR><TH ROWSPAN=\"3\"><IMG SRC=\"/images/actions.png\" ALT=\"Actions\"></TH>");
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
		reply.add("<TR><TH ROWSPAN=\"1\"><IMG SRC=\"/images/links.png\" ALT=\"Links\"></TH><TD><A HREF=\"/links.html\">Links relevant to this program</A></TD></TR>\n");

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
		reply.add("<TABLE CLASS=\"b\">\n");
		reply.add("<TR><TD>CoffeeSaint website (for updates):</TD><TD><A HREF=\"http://vanheusden.com/java/CoffeeSaint/\">http://vanheusden.com/java/CoffeeSaint/</A></TD></TR>\n");
		reply.add("<TR><TD>Source of icons used in web-interface:</TD><TD><A HREF=\"http://commons.wikimedia.org/wiki/Crystal_Clear\">http://commons.wikimedia.org/wiki/Crystal_Clear</A></TD></TR>\n");
		reply.add("<TR><TD>Source of Nagios related software (1):</TD><TD><A HREF=\"http://nagiosexchange.org/\">http://nagiosexchange.org/</A></TD></TR>\n");
		reply.add("<TR><TD>Source of Nagios related software (2):</TD><TD><A HREF=\"http://exchange.nagios.org/\">http://exchange.nagios.org/</A></TD></TR>\n");
		reply.add("<TR><TD>Site of Nagios itself:</TD><TD><A HREF=\"http://www.nagios.org/\">http://www.nagios.org/</A></TD></TR>\n");
		reply.add("<TR><TD>Stylesheet generator:</TD><TD><A HREF=\"http://www.somacon.com/p141.php\">http://www.somacon.com/p141.php</A></TD></TR>\n");
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

		reply.add("<TABLE CLASS=\"b\">\n");
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
			coffeeSaint.loadNagiosData(null, -1, null);
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

			reply.add("<TABLE CLASS=\"b\" WIDTH=640 HEIGHT=400 TEXT=\"#" + htmlColorString(config.getTextColor()) + "\" BGCOLOR=\"#" + htmlColorString(bgColor) + "\">\n");

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
				if (config.getImageUrls().size() >= 1)
					reply.add("<TR VALIGN=CENTER><TD ALIGN=CENTER><IMG SRC=\"/image.jpg\" BORDER=\"0\"></TD></TR>\n");
				else if (config.getNagiosDataSources().size() == 0)
					reply.add("<TR VALIGN=CENTER><TD ALIGN=CENTER>NO NAGIOS SERVERS SELECTED!</TD></TR>\n");
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
		reply.add("<TABLE CLASS=\"b\">");
		reply.add("<TR><TD><B>host</B></TD><TD><B>when</B></TD></TR>");
		for(int index=hosts.size() - 1; index>=0; index--)
			reply.add("<TR><TD>" + hosts.get(index).getAddress().toString().substring(1) + "</TD><TD>" + formatDate(hosts.get(index).getTimestamp()) + "</TD></TR>");
		reply.add("</TABLE>");

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_helpescapes_html(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		reply.add("<H2>Escapes</H2>\n");
		reply.add("<PRE>\n");
		reply.add("  %CRITICAL/%WARNING/%OK, %UP/%DOWN/%UNREACHABLE/%PENDING\n");
		reply.add("  %H:%M       Current hour/minute\n");
		reply.add("  %HOSTNAME/%SERVICENAME    host/service with problem\n");
		reply.add("  %HOSTSTATE/%SERVICESTATE  host/service state\n");
		reply.add("  %HOSTSINCE/%SERVICESINCE  since when does this host/service have a problem\n");
		reply.add("  %HOSTFLAPPING/%SERVICEFLAPPING  wether the state is flapping\n");
		reply.add("  %PREDICT/%HISTORICAL      \n");
		reply.add("  %HOSTDURATION/%SERVICEDURATION how long has a host/service been down\n");
		reply.add("  %OUTPUT                   Plugin output\n");
		reply.add("</PRE>\n");

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
		MyHTTPServer socket = null;

		try
		{
			for(;;)
			{
				CoffeeSaint.log.add("Waiting for connection");
				try
				{
					if (socket == null)
					{
						CoffeeSaint.log.add("Listening on " + adapter + ":" + port);

						socket = new MyHTTPServer(adapter, port);
					}

					List<HTTPRequestData> request = socket.acceptConnectionGetRequest();
					if (request.size() == 0)
						continue;
					String requestType = request.get(0).getName();
					String url = request.get(0).getData().trim();
					int space = url.indexOf(" ");
					if (space != -1)
						url = url.substring(0, space);

					InetSocketAddress remoteAddress = socket.getRemoteSocketAddress();
					CoffeeSaint.log.add("HTTP " + remoteAddress.toString().substring(1) + " " + requestType + "-request for: " + url);
					//HTTPLogEntry
					int nHostsKnown = hosts.size();
					if (nHostsKnown > 0 && hosts.get(nHostsKnown - 1).getAddress().getAddress().equals(remoteAddress.getAddress()) == true)
						hosts.get(nHostsKnown - 1).updateTimestamp(Calendar.getInstance());
					else
						hosts.add(new HTTPLogEntry(remoteAddress, Calendar.getInstance()));
					if (nHostsKnown == config.getHttpRememberNHosts()) // it is actually one more due to the add in the previous line
						hosts.remove(0);
					webServerHits++;

					boolean isHeadRequest = false;
					if (requestType.equals("HEAD"))
						isHeadRequest = true;

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
						sendReply_images_statistics_png(socket, isHeadRequest);
					else if (url.equals("/images/configure.png"))
						sendReply_images_configure_png(socket, isHeadRequest);
					else if (url.equals("/images/actions.png"))
						sendReply_images_actions_png(socket, isHeadRequest);
					else if (url.equals("/images/links.png"))
						sendReply_images_links_png(socket, isHeadRequest);
					else if (url.equals("/images/the_coffee_saint.jpg"))
						sendReply_images_the_coffee_saint_jpg(socket, isHeadRequest);
					else if (url.equals("/images/vanheusden02.jpg"))
						sendReply_images_vanheusden02_jpg(socket, isHeadRequest);
					else if (url.equals("/robots.txt"))
						sendReply_robots_txt(socket, isHeadRequest);
					else if (url.equals("/favicon.ico"))
						sendReply_favicon_ico(socket, isHeadRequest);
					else if (url.equals("/links.html"))
						sendReply_links_html(socket);
					else if (url.equals("/help-escapes.html"))
						sendReply_helpescapes_html(socket);
					else if (url.equals("/cgi-bin/list-all.cgi"))
						sendReply_cgibin_listall_cgi(socket);
					else if (url.equals("/cgi-bin/list-log.cgi"))
						sendReply_cgibin_listlog_cgi(socket);
					else if (url.equals("/stylesheet.css"))
						sendReply_stylesheet_css(socket, isHeadRequest);
					else
					{
						sendReply_404(socket, url);
						webServer404++;
					}
				}
				catch(Exception e)
				{
					if (!(e instanceof SocketException))
						statistics.incExceptions();

					CoffeeSaint.log.add("Exception during command processing");
					CoffeeSaint.showException(e);
					if (socket != null)
					{
						socket.close();
						socket = null;
					}
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
