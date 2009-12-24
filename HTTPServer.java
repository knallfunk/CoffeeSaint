/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import com.vanheusden.sockets.HTTPRequestData;
import com.vanheusden.sockets.MyHTTPServer;
import com.vanheusden.nagios.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

class HTTPServer implements Runnable
{
	final Config config;
	final CoffeeSaint coffeeSaint;
	final Statistics statistics;
	final Gui gui;
	final List<HTTPLogEntry> hosts = new ArrayList<HTTPLogEntry>();
	final GraphicsDevice gd;
	final JFrame frame;
	//
	int webServerHits, webServer404;
	boolean configNotWrittenToDisk = false;
	final private String defaultCharset = "US-ASCII";

	public HTTPServer(Config config, CoffeeSaint coffeeSaint, Statistics statistics, Gui gui, GraphicsDevice gd, JFrame frame)
	{
		this.config = config;
		this.coffeeSaint = coffeeSaint;
		this.statistics = statistics;
		this.gui = gui;
		this.gd = gd;
		this.frame = frame;
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
		whereTo.add("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
		whereTo.add("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" >\n");
		whereTo.add("<head>\n");
		whereTo.add("<meta http-equiv=\"content-type\" content=\"text/html; charset=iso-8859-1\" />\n");
		whereTo.add("<meta name=\"author\" content=\"Bastiaan Schuiling Konings\" />\n");
		whereTo.add("<meta name=\"keywords\" content=\"coffeesaint admin\" />\n");
		whereTo.add("<meta name=\"description\" content=\"CoffeeSaint remote configuration panel\" /\n");
		whereTo.add("	<title>CoffeeSaint admin</title>\n");
		whereTo.add("	<script type=\"text/javascript\"></script>\n");
		whereTo.add("<link href=\"/design.css\" rel=\"stylesheet\" media=\"screen\">\n");
		if (head != null && head.equals("") == false)
			whereTo.add(head);
		whereTo.add("</head>\n");
		whereTo.add("<body id=\"css-coffeesaint\" BGCOLOR=\"#ffffff\">\n");
		whereTo.add("	<div id=\"coffee\"><img src=\"/images/coffee.png\" /></div>\n");
		whereTo.add("	<div id=\"container\">\n");
		whereTo.add("		<div id=\"column_navigation\">\n");
		whereTo.add("			<div id=\"navigation\">\n");
		whereTo.add("				<strong>Nagios</strong><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/performance-data.cgi\">Performance data</a><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/list-all.cgi\">List of hosts/services</a><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/nagios_status.cgi\">Problems overview</a><br />\n");
		whereTo.add("				<br /><strong>Logging</strong><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/statistics.cgi\">CoffeeSaint statistics</a><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/log.cgi\">List of connecting hosts</a><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/list-log.cgi\">Show log</a><br />\n");
		whereTo.add("				<br /><strong>Configuration</strong><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/config-menu.cgi\">Configure CoffeeSaint</a><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/reload-config.cgi\">Reload configuration</a><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/select_configfile.cgi\">Select configuration file</a><br />\n");
                if (config.getConfigFilename() == null)
                        whereTo.add("No configuration-file selected, save disabled<br />\n");
                else
                {
                        String line = "<A HREF=\"/cgi-bin/write-config.cgi\">Write config to " + config.getConfigFilename() + "</A>";
                        if (configNotWrittenToDisk == true)
                                line += " (changes pending!)";
                        line += "<br />\n";
                        whereTo.add(line);
                }
		whereTo.add("				<br /><strong>Actions</strong><br />\n");
                if (config.getRunGui())
                        whereTo.add("<A HREF=\"/cgi-bin/force_reload.cgi\">Force reload</A><br />\n");
                else
                        whereTo.add("Force reload disabled, not running GUI<br />\n");
                String sample = config.getProblemSound();
                if (sample != null)
                        whereTo.add("<A HREF=\"/cgi-bin/test-sound.cgi\">Test sound (" + sample + ")</A><br />\n");
                else
                        whereTo.add("No sound selected<br />\n");
		whereTo.add("				<br /><strong>Links</strong><br />\n");
		whereTo.add("				<a href=\"/links.html\">Links relevant to this program</a><br />\n");
		whereTo.add("				<br />\n");
		whereTo.add("				<br />\n");
		whereTo.add("				<a href=\"http://www.vanheusden.com\" target=\"_blank\"><img src=\"/images/footer01.png\" border=\"0\" /></a>\n");
		whereTo.add("			</div>\n");
		whereTo.add("		</div>\n");
		whereTo.add("		<div id=\"column_main\">\n");
		whereTo.add("			<img src=\"/images/title01.png\" />\n");
		whereTo.add("			<font size=\"5\">" + CoffeeSaint.getVersionNr() + "</font>\n");
		whereTo.add("			<div id=\"main\">\n");
	}

	public String formatDate(Calendar when)
	{
		SimpleDateFormat dateFormatter = new SimpleDateFormat("E yyyy.MM.dd  hh:mm:ss a zzz");

		return dateFormatter.format(when.getTime());
	}

	public void addPageTail(List<String> whereTo, boolean mainMenu)
	{ 
		whereTo.add("				<br />\n");
		whereTo.add("			</div>\n");
		whereTo.add("		</div>\n");
		whereTo.add("	</div>\n");
		whereTo.add("</body>\n");
		whereTo.add("</html>\n");

//		whereTo.add(formatDate(Calendar.getInstance()) + "</TD></TR></TABLE></BODY></HTML>");
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

	public void sendReply_favicon_ico(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/favicon.ico", "image/x-icon", headRequest);
	}

	public void sendReply_images_bg01_png(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/bg01.png", "image/png", headRequest);
	}

	public void sendReply_images_coffee_png(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/coffee.png", "image/png", headRequest);
	}

	public void sendReply_images_footer01_png(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/footer01.png", "image/png", headRequest);
	}

	public void sendReply_images_saint01_png(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/saint01.png", "image/png", headRequest);
	}

	public void sendReply_images_title01_png(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/title01.png", "image/png", headRequest);
	}

	public void sendReply_design_css(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/design.css", "text/css", headRequest);
	}

	public void sendReply_robots_txt(MyHTTPServer socket, boolean headRequest) throws Exception
	{
		sendReply_send_file_from_jar(socket, "com/vanheusden/CoffeeSaint/robots.txt", "text/plain", headRequest);
	}

	public void sendReply_imagejpg(MyHTTPServer socket)
	{
		try
		{
			socket.getOutputStream().write("HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type: image/jpeg\r\n\r\n".getBytes());
			Image img = coffeeSaint.loadImage(null, -1, null)[0].getImage();
			ImageIO.write(createBufferedImage(img), "jpg", socket.getOutputStream());
			socket.close();
		}
		catch(Exception e)
		{
			// really don't care if the transmit failed; browser
			// probably closed session
			// don't care if we could display the image or not
		}
	}

	public void sendReply_cgibin_zoomin_cgi(MyHTTPServer socket, List<HTTPRequestData> getData) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		int width = 920;
		int height = 150;

		String host = null;
		HTTPRequestData hostData = MyHTTPServer.findRecord(getData, "host");
		if (hostData != null && hostData.getData() != null)
			host = URLDecoder.decode(hostData.getData(), defaultCharset);

		String service = null;
		HTTPRequestData serviceData = MyHTTPServer.findRecord(getData, "service");
		if (serviceData != null && serviceData.getData() != null)
			service = URLDecoder.decode(serviceData.getData(), defaultCharset);

		String dataSource = null;
		HTTPRequestData dataSourceData = MyHTTPServer.findRecord(getData, "dataSource");
		if (dataSourceData != null && dataSourceData.getData() != null)
			dataSource = URLDecoder.decode(dataSourceData.getData(), defaultCharset);

		String heading = host;
		if (service != null)
			heading += " | " + service;
		if (dataSource != null)
			heading += " | " + dataSource;

		reply.add("<H1>" + heading + "</H1>");
		reply.add("<IMG SRC=\"" + sparkLineUrl(host, service, dataSource, width, height, true) + "\" BORDER=\"1\"><BR>");

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_sparkline_cgi(MyHTTPServer socket, List<HTTPRequestData> getData) throws Exception
	{
		try
		{
			int width = 400;
			HTTPRequestData widthData = MyHTTPServer.findRecord(getData, "width");
			if (widthData != null && widthData.getData() != null)
				width = Integer.valueOf(widthData.getData());

			int height = 240;
			HTTPRequestData heightData = MyHTTPServer.findRecord(getData, "height");
			if (heightData != null && heightData.getData() != null)
				height = Integer.valueOf(heightData.getData());

			String host = null;
			HTTPRequestData hostData = MyHTTPServer.findRecord(getData, "host");
			if (hostData != null && hostData.getData() != null)
				host = URLDecoder.decode(hostData.getData(), defaultCharset);

			String service = null;
			HTTPRequestData serviceData = MyHTTPServer.findRecord(getData, "service");
			if (serviceData != null && serviceData.getData() != null)
				service = URLDecoder.decode(serviceData.getData(), defaultCharset);

			String dataSource = null;
			HTTPRequestData dataSourceData = MyHTTPServer.findRecord(getData, "dataSource");
			if (dataSourceData != null && dataSourceData.getData() != null)
				dataSource = URLDecoder.decode(dataSourceData.getData(), defaultCharset);

			boolean withMetaData = false;
			HTTPRequestData withMetaDataData = MyHTTPServer.findRecord(getData, "metadata");
			if (withMetaDataData != null && withMetaDataData.getData() != null)
				withMetaData = URLDecoder.decode(withMetaDataData.getData(), defaultCharset).equalsIgnoreCase("true");

			System.out.println("" + width + "x" + height + ", " + host + " | " + service + " | " + dataSource);
			if (host != null)
			{
				socket.getOutputStream().write("HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type: image/png\r\n\r\n".getBytes());
				BufferedImage sparkLine = coffeeSaint.getSparkLine(host, service, dataSource, width, height, withMetaData);
				ImageIO.write(sparkLine, "png", socket.getOutputStream());
			}
		}
		catch(Exception e)
		{
			// really don't care if the transmit failed; browser
			// probably closed session
			// don't care if we could display the image or not
		}
		finally
		{
			socket.close();
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

	public void stringSelectorHTML(List<String> reply, String name, List<String> list, String selected, boolean addEmpty) throws Exception
	{
		reply.add("<SELECT NAME=\"" + name + "\">\n");
		if (addEmpty)
			reply.add("<OPTION VALUE=\"\"></OPTION>\n");
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

	public void sendReply_cgibin_select_configfile_cgi(MyHTTPServer socket) throws Exception
	{
		File dir = new File(".");
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		if (config.getDisableHTTPFileselect())
		{
			reply.add("Access denied");
		}
		else
		{
			reply.add("<FORM ACTION=\"/cgi-bin/select_configfile-do.cgi\" METHOD=\"POST\">\n");
			reply.add("Current path: <B>" + dir.getAbsolutePath() + "</B><BR><BR>\n");

			String currentFile = config.getConfigFilename();
			reply.add("<TABLE>\n");
			reply.add("<TR><TD>Configuration file:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"config-file\" VALUE=\"" + (currentFile != null?currentFile:"") + "\"></TD></TR>\n");
			reply.add("<TR><TD>or select from:</TD><TD>\n");
			FilenameFilter fileFilter = new FilenameFilter() {
					public boolean accept(File dir, String name)
					{
						return !new File(name).isDirectory();
					}
				};
			String [] files = dir.list(fileFilter);
			Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);
			stringSelectorHTML(reply, "config-file-list", convertStringArrayToList(files), currentFile != null ? currentFile : "---NONE---", true);
			reply.add("</TD></TR>\n");
			reply.add("<TR><TD></TD><TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Submit changes!\"></TD></TR>\n");
			reply.add("</TABLE>\n");
			reply.add("<BR>\n");

			reply.add("</FORM>\n");
		}

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_select_configfile_do_cgi(MyHTTPServer socket, List<HTTPRequestData> requestData) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		if (config.getDisableHTTPFileselect())
		{
			reply.add("Access denied");
		}
		else
		{
			String newFileName = getField(socket, requestData, "config-file");
			if (newFileName == null || newFileName.equals(""))
				newFileName = getField(socket, requestData, "config-file-list");
			if (newFileName != null && newFileName.equals("") == false)
			{
				config.setConfigFilename(newFileName);
				reply.add("Configuration filename set to <B>" + newFileName + "</B>");
			}
		}

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public String selectField(String current, String newValue)
	{
		String out = "<OPTION VALUE=\"" + newValue + "\"";
		if (newValue.equals(current))
			out += " SELECTED";
		out += ">" + newValue + "</OPTION>\n";
		return out;
	}

	public void sendReply_cgibin_configmenu_cgi(MyHTTPServer socket) throws Exception
	{
		System.out.println("sendReply_cgibin_configmenu_cgi START");
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		reply.add("<FORM ACTION=\"/cgi-bin/config-do.cgi\" METHOD=\"POST\">\n");

		reply.add("<H1>Nagios handling parameters</H1>\n");
		reply.add("<TABLE>\n");
		reply.add("<TR><TD>Always notify:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"always_notify\" VALUE=\"on\" " + isChecked(config.getAlwaysNotify()) + "></TD><TD>Also display when notifications are disabled</TD></TR>\n");
		reply.add("<TR><TD>Also acknowledged:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_acknowledged\" VALUE=\"on\" " + isChecked(config.getAlsoAcknowledged()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Also scheduled downtime</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_scheduled_downtime\" VALUE=\"on\" " + isChecked(config.getAlsoScheduledDowntime()) + "></TD><TD>Also display problems for which downtime has been scheduled</TD></TR>\n");
		reply.add("<TR><TD>Also soft state:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_soft_State\" VALUE=\"on\" " + isChecked(config.getAlsoSoftState()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Also disabled checks:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_disabled_active_checks\" VALUE=\"on\" " + isChecked(config.getAlsoDisabledActiveChecks()) + "></TD><TD>Also display problems for which active checks have been disabled</TD></TR>\n");
		reply.add("<TR><TD>Show services for host with problems:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"show_services_for_host_with_problems\" VALUE=\"on\" " + isChecked(config.getShowServicesForHostWithProblems()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Show flapping:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"show-flapping\" VALUE=\"on\" " + isChecked(config.getShowFlapping()) + "></TD><TD></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<BR>\n");

		reply.add("<H1>Network parameters</H1>\n");
		reply.add("Please note that after you click on submit, the new network-settings are applied immeditately.<BR>\n");
		reply.add("<TABLE>\n");
		reply.add("<TR><TD>Network interface to listen on:</TD><TD><SELECT NAME=\"network-interface\"><OPTION VALUE=\"0.0.0.0\"" + (config.getHTTPServerListenAdapter().equals("0.0.0.0") == true ? " SELECTED" : "") + ">All interfaces</OPTION>");
		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
		for (NetworkInterface netint : Collections.list(nets))
		{
			Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
			for (InetAddress inetAddress : Collections.list(inetAddresses))
			{
				String address = inetAddress.toString().substring(1);
				int percent = address.indexOf("%");
				if (percent != -1)
					address = address.substring(0, percent);
				reply.add("<OPTION VALUE\"" + address + "\"" + (config.getHTTPServerListenAdapter().equals(address) == true ? " SELECTED" : "") + ">" + address + "</OPTION>");
			}
		}
		reply.add("</TD></TR>\n");
		reply.add("<TR><TD>Port to listen on:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"network-port\" VALUE=\"" + config.getHTTPServerListenPort() + "\"></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<BR>\n");

		reply.add("<H1>Nagios server(s)</H1>\n");
		reply.add("<TABLE>\n");
		reply.add("<TR><TD><B>type</B></TD><TD><B>Nagios version</B></TD><TD><B>data source</B></TD><TD><B>remove?</B></TD></TR>\n");
		for(NagiosDataSource dataSource : config.getNagiosDataSources())
		{
			String type = "?";
			if (dataSource.getType() == NagiosDataSourceType.TCP)
				type = "tcp";
			else if (dataSource.getType() == NagiosDataSourceType.ZTCP)
				type = "compressed tcp";
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
		reply.add("<TR><TD>Use HTTP compression:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"disable-http-compression\" VALUE=\"on\" " + isChecked(config.getAllowHTTPCompression()) + "></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("TCP requires an ip-address followed by a space and a port-number in the parameters field.<BR>\n");
		reply.add("<BR>\n");

		reply.add("<H1>Look and feel parameters</H1>\n");
		reply.add("<TABLE>\n");
		reply.add("<TR><TD>Refresh interval:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"sleepTime\" VALUE=\"" + config.getSleepTime() + "\"></TD><TD>&gt; 1</TD></TR>\n");
//		String fsMode = "";
//		if (gd == null)
//			fsMode = "Running headless";
//		else if (gd.isFullScreenSupported() == false)
//			fsMode = "Not supported";
		reply.add("<TR><TD>Fullscreen mode:</TD><TD><SELECT NAME=\"fullscreen\">\n");
		reply.add(selectField(config.getFullscreenName(), "none"));
		reply.add(selectField(config.getFullscreenName(), "undecorated"));
		reply.add(selectField(config.getFullscreenName(), "fullscreen"));
//		reply.add("</SELECT></TD><TD>" + fsMode + "</TD></TR>\n");
		reply.add("</SELECT></TD><TD>Requires restart of CoffeeSaint.</TD></TR>\n");
		reply.add("<TR><TD>Show counter:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"counter\" VALUE=\"on\" " + isChecked(config.getCounter()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Counter position:</TD><TD><SELECT NAME=\"counter-position\">\n");
		reply.add(selectField(config.getCounterPositionName(), "upper-left"));
		reply.add(selectField(config.getCounterPositionName(), "upper-right"));
		reply.add(selectField(config.getCounterPositionName(), "lower-left"));
		reply.add(selectField(config.getCounterPositionName(), "lower-right"));
		reply.add(selectField(config.getCounterPositionName(), "center"));
		reply.add("</SELECT></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Verbose:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"verbose\" VALUE=\"on\" " + isChecked(config.getVerbose()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Number of rows:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"nRows\" VALUE=\"" + config.getNRows() + "\"></TD><TD>&gt;= 3</TD></TR>\n");
		reply.add("<TR><TD>Number of columns:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"problem-columns\" VALUE=\"" + config.getNProblemCols() + "\"></TD><TD>&gt;= 1</TD></TR>\n");
		reply.add("<TR><TD>Flexible number of columns:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"flexible-n-columns\" VALUE=\"on\" " + isChecked(config.getFlexibleNColumns()) + "></TD><TD>Use in combination with number of columns</TD></TR>\n");
		GraphicsEnvironment lge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		List<String> fontNames = convertStringArrayToList(lge.getAvailableFontFamilyNames());
		reply.add("<TR><TD>Font:</TD><TD>");
		stringSelectorHTML(reply, "font", fontNames, config.getFontName(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Warning font:</TD><TD>");
		stringSelectorHTML(reply, "warning-font", fontNames, config.getWarningFontName(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Critical font:</TD><TD>");
		stringSelectorHTML(reply, "critical-font", fontNames, config.getCriticalFontName(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Reduce text width to fit to screen:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"reduce-textwidth\" VALUE=\"on\" " + isChecked(config.getReduceTextWidth()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Anti-alias:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"anti-alias\" VALUE=\"on\" " + isChecked(config.getAntiAlias()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Max. quality graphics:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"max-quality-graphics\" VALUE=\"on\" " + isChecked(config.getMaxQualityGraphics()) + "></TD><TD>Slows down and difference is small</TD></TR>\n");
		reply.add("<TR><TD>Transparency:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"transparency\" VALUE=\"" + config.getTransparency() + "\"></TD><TD>0.0...1.0 only usefull with background image/webcam</TD></TR>\n");
		reply.add("<TR><TD>Row border:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"row-border\" VALUE=\"on\" " + isChecked(config.getRowBorder()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Row border color:</TD><TD>\n");
		colorSelectorHTML(reply, "row-border-color", config.getRowBorderColorName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Graph color:</TD><TD>\n");
		colorSelectorHTML(reply, "graph-color", config.getGraphColorName());
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
		reply.add("<TR><TD>Background color warning-status:</TD><TD>\n");
		colorSelectorHTML(reply, "warning-bg-color", config.getWarningBgColorName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Background color critical-status:</TD><TD>\n");
		colorSelectorHTML(reply, "critical-bg-color", config.getCriticalBgColorName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Background color unknown-status:</TD><TD>\n");
		colorSelectorHTML(reply, "unknown-bg-color", config.getNagiosUnknownBgColorName());
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Host issues:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"host-issue\" VALUE=\"" + config.getHostIssue() + "\"></TD><TD><A HREF=\"/help-escapes.html\" TARGET=\"_new\">List of escapes</A></TD></TR>\n");
		reply.add("<TR><TD>Service issues:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"service-issue\" VALUE=\"" + config.getServiceIssue() + "\"></TD><TD><A HREF=\"/help-escapes.html\" TARGET=\"_new\">List of escapes</A></TD></TR>\n");
		reply.add("<TR><TD>Header:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"header\" VALUE=\"" + config.getHeader() + "\"></TD><TD><A HREF=\"/help-escapes.html\" TARGET=\"_new\">List of escapes</A></TD></TR>\n");
		reply.add("<TR><TD>Show header:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"show-header\" VALUE=\"on\" " + isChecked(config.getShowHeader()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Scroll header:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"scrolling-header\" VALUE=\"on\" " + isChecked(config.getScrollingHeader()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Scroll problems:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"scroll-if-not-fitting\" VALUE=\"on\" " + isChecked(config.getScrollIfNotFit()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Problems scroll splitter:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"scroll-splitter\" VALUE=\"" + (config.getLineScrollSplitter() == null ? "" : "" + config.getLineScrollSplitter()) + "\"></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Scroll pixels/sec:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"scroll-pixels-per-sec\" VALUE=\"" + config.getScrollingPixelsPerSecond() + "\"></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Sort order:</TD><TD>\n");
		stringSelectorHTML(reply, "sort-order", config.getSortFields(), config.getSortOrder(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Sort numeric:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"sort-order-numeric\" VALUE=\"on\" " + isChecked(config.getSortOrderNumeric()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Sort reverse:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"sort-order-reverse\" VALUE=\"on\" " + isChecked(config.getSortOrderReverse()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Sparkline size:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"sparkline-size\" VALUE=\"" + config.getSparkLineWidth() +  "\"></TD><TD>In number of pixels, 0 to disable</TD></TR>\n");
		reply.add("<TR><TD>Sparkline draw mode:</TD><TD><SELECT NAME=\"sparkline-mode\">");
		reply.add("<OPTION VALUE=\"avg-sd\"" + (config.getSparklineGraphMode() == SparklineGraphMode.AVG_SD ? " SELECTED" : "") + ">scale to average &amp; standard deviation</OPTION>\n");
		reply.add("<OPTION VALUE=\"min-max\"" + (config.getSparklineGraphMode() == SparklineGraphMode.MIN_MAX ? " SELECTED" : "") + ">scale between min and max</OPTION>\n");
		reply.add("</SELECT></TD><TD></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<BR>\n");

		if (config.getDisableHTTPFileselect() == false)
		{
			reply.add("<H1>Files</H1>\n");
			reply.add("<TABLE>\n");
			reply.add("<TR><TD>File to store prediction data in:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"brain-file\" VALUE=\"" + (config.getBrainFileName() != null ? config.getBrainFileName() : "")+ "\"></TD><TD>Used for predicting problem count</TD></TR>\n");
			reply.add("<TR><TD>File to store performance data in:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"performance-data\" VALUE=\"" + (config.getPerformanceDataFileName() != null ? config.getPerformanceDataFileName() : "") + "\"></TD><TD>Used for sparklines</TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<BR>\n");
		}

		reply.add("<H1>Filters</H1>\n");
		reply.add("<TABLE>\n");
		reply.add("<TR><TD>Hosts to place at the top:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"prefer\" VALUE=\"" + config.getPrefersList() + "\"></TD><TD>Comma-seperated list (can be regular expressions)</TD></TR>\n");
		reply.add("<TR><TD>Hosts filter exclude list:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"hosts-filter-exclude-list\" VALUE=\"" + config.getHostsFilterExcludeList() + "\"></TD><TD>Comma-seperated list (can be regular expressions)</TD></TR>\n");
		reply.add("<TR><TD>Hosts filter include list:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"hosts-filter-include-list\" VALUE=\"" + config.getHostsFilterIncludeList() + "\"></TD><TD>(are applied after processing the exclude list)</TD></TR>\n");
		reply.add("<TR><TD>Services filter exclude list:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"services-filter-exclude-list\" VALUE=\"" + config.getServicesFilterExcludeList() + "\"></TD><TD>See host-filter comments</TD></TR>\n");
		reply.add("<TR><TD>Services filter include list:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"services-filter-include-list\" VALUE=\"" + config.getServicesFilterIncludeList() + "\"></TD><TD></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<BR>\n");

		reply.add("<H1>Webcams</H1>\n");
		reply.add("<TABLE>\n");
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

		reply.add("<H1>Submit changes</H1>\n");
		reply.add("<INPUT TYPE=\"SUBMIT\" VALUE=\"Submit changes!\"><BR>\n");
		reply.add("<BR>\n");

		reply.add("</FORM>\n");

		addPageTail(reply, true);

		socket.sendReply(reply);
		System.out.println("sendReply_cgibin_configmenu_cgi END");
	}

	public boolean getCheckBox(MyHTTPServer socket, List<HTTPRequestData> requestData, String fieldName)
	{
		HTTPRequestData field = MyHTTPServer.findRecord(requestData, fieldName);
		if (field != null && field.getData() != null)
			return true;

		return false;
	}

	public String getField(MyHTTPServer socket, List<HTTPRequestData> requestData, String fieldName)
	{
		HTTPRequestData field = MyHTTPServer.findRecord(requestData, fieldName);
		if (field != null && field.getData() != null)
			return field.getData().trim();

		return "";
	}

	public String getFieldDecoded(MyHTTPServer socket, List<HTTPRequestData> requestData, String fieldName) throws Exception
	{
		return URLDecoder.decode(getField(socket, requestData, fieldName), defaultCharset);
	}

	public void sendReply_cgibin_configdo_cgi(MyHTTPServer socket, List<HTTPRequestData> requestData) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		configNotWrittenToDisk = true;

		addHTTP200(reply);
		addPageHeader(reply, "");

		HTTPRequestData nRows = MyHTTPServer.findRecord(requestData, "nRows");
		if (nRows != null && nRows.getData() != null)
		{
			int newNRows = Integer.valueOf(nRows.getData());
			if (newNRows < 3)
				reply.add("New number of rows invalid, must be >= 3.<BR>\n");
			else
				config.setNRows(newNRows);
		}

		HTTPRequestData nCols = MyHTTPServer.findRecord(requestData, "problem-columns");
		if (nCols != null && nCols.getData() != null)
		{
			int newNCols = Integer.valueOf(nCols.getData());
			if (newNCols < 1)
				reply.add("New number of rows invalid, must be >= 1.<BR>\n");
			else
				config.setNProblemCols(newNCols);
		}

		HTTPRequestData transparency = MyHTTPServer.findRecord(requestData, "transparency");
		if (transparency != null && transparency.getData() != null)
		{
			float newTransparency = Float.valueOf(transparency.getData());
			if (newTransparency < 0.0 || newTransparency > 1.0)
				reply.add("Transparency must be between 0.0 and 1.0 (both inclusive)");
			else
				config.setTransparency(newTransparency);
		}

		HTTPRequestData sparkline_size = MyHTTPServer.findRecord(requestData, "sparkline-size");
		if (sparkline_size != null && sparkline_size.getData() != null)
		{
			int newSparklineSize = Integer.valueOf(sparkline_size.getData());
			if (newSparklineSize < 0 || newSparklineSize >= 1000)
				reply.add("Transparency must be between 0 (inclusive) and 1000");
			else
				config.setSparkLineWidth(newSparklineSize);
		}

		config.setFlexibleNColumns(getCheckBox(socket, requestData, "flexible-n-columns"));

		config.setMaxQualityGraphics(getCheckBox(socket, requestData, "max-quality-graphics"));

		config.setFontName(getFieldDecoded(socket, requestData, "font"));

		if (!config.getNoNetworkChange())
		{
			config.setHTTPServerListenAdapter(getFieldDecoded(socket, requestData, "network-interface"));
			System.out.println("PORT: " + getFieldDecoded(socket, requestData, "network-port") + "|");
			config.setHTTPServerListenPort(Integer.valueOf(getFieldDecoded(socket, requestData, "network-port")));
		}

		config.setWarningFontName(getFieldDecoded(socket, requestData, "warning-font"));

		config.setCriticalFontName(getFieldDecoded(socket, requestData, "critical-font"));

		config.setTextColor(getField(socket, requestData, "textColor"));
		config.setWarningTextColor(getField(socket, requestData, "warningTextColor"));
		config.setCriticalTextColor(getField(socket, requestData, "criticalTextColor"));

		config.setBackgroundColor(getField(socket, requestData, "backgroundColor"));

		config.setBackgroundColorOkStatus(getField(socket, requestData, "bgColorOk"));
		config.setWarningBgColor(getField(socket, requestData, "warning-bg-color"));
		config.setCriticalBgColor(getField(socket, requestData, "critical-bg-color"));
		config.setNagiosUnknownBgColor(getField(socket, requestData, "unknown-bg-color"));

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

		String sparkline_mode = getField(socket, requestData, "sparkline-mode");
		if (sparkline_mode != null)
		{
			if (sparkline_mode.equals("avg-sd"))
				config.setSparklineGraphMode(SparklineGraphMode.AVG_SD);
			else if (sparkline_mode.equals("min-max"))
				config.setSparklineGraphMode(SparklineGraphMode.MIN_MAX);
		}

		config.setAlwaysNotify(getCheckBox(socket, requestData, "always_notify"));

		config.setAlsoAcknowledged(getCheckBox(socket, requestData, "also_acknowledged"));

		config.setAlsoScheduledDowntime(getCheckBox(socket, requestData, "also_scheduled_downtime"));

		config.setAlsoSoftState(getCheckBox(socket, requestData, "also_soft_State"));

		config.setAlsoDisabledActiveChecks(getCheckBox(socket, requestData, "also_disabled_active_checks"));

		config.setShowServicesForHostWithProblems(getCheckBox(socket, requestData, "show_services_for_host_with_problems"));

		config.setShowFlapping(getCheckBox(socket, requestData, "show-flapping"));

		config.setCounter(getCheckBox(socket, requestData, "counter"));

		String newFSMode = getField(socket, requestData, "fullscreen");
		if (newFSMode != null)
		{
//			if (newFSMode.equalsIgnoreCase("fullscreen"))
//				gd.setFullScreenWindow(frame);
//			else
//				gd.setFullScreenWindow(null);

			if (newFSMode.equalsIgnoreCase("none"))
				config.setFullscreen(FullScreenMode.NONE);
			else if (newFSMode.equalsIgnoreCase("undecorated"))
				config.setFullscreen(FullScreenMode.UNDECORATED);
			else if (newFSMode.equalsIgnoreCase("fullscreen"))
				config.setFullscreen(FullScreenMode.FULLSCREEN);
		}

		String counterPosition = getField(socket, requestData, "counter-position");
		if (counterPosition != null)
			config.setCounterPosition(counterPosition);

		if (config.getDisableHTTPFileselect() == false)
		{
			String brainFile = getField(socket, requestData, "brain-file").trim();
			config.setBrainFileName(brainFile.equals("") ? null : brainFile);

			String performanceFile = getField(socket, requestData, "performance-data").trim();
			config.setPerformanceDataFileName(performanceFile.equals("") ? null : performanceFile);
		}

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

		config.setAllowHTTPCompression(getCheckBox(socket, requestData, "disable-http-compression"));

		config.setAntiAlias(getCheckBox(socket, requestData, "anti-alias"));

		config.setRandomWebcam(getCheckBox(socket, requestData, "random-img"));

		config.setReduceTextWidth(getCheckBox(socket, requestData, "reduce-textwidth"));

		config.setHeader(getFieldDecoded(socket, requestData, "header"));

		config.setPrefers(getFieldDecoded(socket, requestData, "prefer"));
		config.setHostsFilterExclude(getFieldDecoded(socket, requestData, "hosts-filter-exclude-list"));
		config.setHostsFilterInclude(getFieldDecoded(socket, requestData, "hosts-filter-include-list"));
		config.setServicesFilterExclude(getFieldDecoded(socket, requestData, "services-filter-exclude-list"));
		config.setServicesFilterInclude(getFieldDecoded(socket, requestData, "services-filter-include-list"));

		config.setScrollingHeader(getCheckBox(socket, requestData, "scrolling-header"));
		config.setScrollIfNotFit(getCheckBox(socket, requestData, "scroll-if-not-fitting"));

		String splitter = getFieldDecoded(socket, requestData, "scroll-splitter");
		if (splitter != null)
		{
			splitter = splitter.trim();

			if (splitter.equals(""))
				config.setLineScrollSplitter(null);
			else
				config.setLineScrollSplitter(splitter.charAt(0));
		}

		String scrollSpeed = getField(socket, requestData, "scroll-pixels-per-sec");
		if (scrollSpeed != null && scrollSpeed.trim().equals("") == false)
		{
			int newScrollSpeed = Integer.valueOf(scrollSpeed);
			if (newScrollSpeed < 1)
				reply.add("New pixels/sec-value is invalid, must be >= 1<BR>\n");
			else
				config.setScrollingPixelsPerSecond(newScrollSpeed);
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
		config.setGraphColor(getField(socket, requestData, "graph-color"));

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
		if (config.getHTTPServerListenAdapter().equals("0.0.0.0") == false)
		{
			String listenAddress = "http://" + config.getHTTPServerListenAdapter() + ":" + config.getHTTPServerListenPort() + "/";
			reply.add("If you changed the networking parameters, please connect to <A HREF=\"" + listenAddress + "\">" + listenAddress + "</A><BR>\n");
		}

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

		reply.add("<H1>Log</H1>");
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

		reply.add("<TABLE>\n");
		reply.add("<TR><TD><B>Host</B></TD><TD><B>host status</B></TD><TD><B>Service</B></TD><TD><B>service status</B></TD></TR>\n");

		JavNag javNag = CoffeeSaint.loadNagiosData(null, -1, null);
                List<Problem> problems = CoffeeSaint.findProblems(javNag);
                coffeeSaint.learnProblemCount(problems.size());
		coffeeSaint.collectPerformanceData(javNag);

		for(Host currentHost : javNag.getListOfHosts())
		{
			String hostState = currentHost.getParameter("state_type").equals("1") ? currentHost.getParameter("current_state") : "0";
			String htmlHostStateColor = htmlColorString(coffeeSaint.stateToColor(hostState.equals("1") ? "2" : hostState));

			String host;
			if (coffeeSaint.havePerformanceData(currentHost.getHostName(), null))
			{
				String url = graphZoomInUrl(currentHost.getHostName(), null, null);
				host = "<A HREF=\"" + url + "\">" + abreviateString(currentHost.getHostName(), 16) + "</A>";
			}
			else
				host = currentHost.getHostName();

			reply.add("<TR><TD>" + host + "</TD><TD BGCOLOR=\"#" + htmlHostStateColor + "\">" + coffeeSaint.hostState(hostState) + "</TD>");

			boolean first = true;
			for(Service currentService : currentHost.getServices())
			{
				String serviceState = currentService.getParameter("state_type").equals("1") ? currentService.getParameter("current_state") : "0";
				String htmlServiceStateColor = htmlColorString(coffeeSaint.stateToColor(serviceState));

				String service;
				if (coffeeSaint.havePerformanceData(currentHost.getHostName(), currentService.getServiceName()))
				{
					String url = graphZoomInUrl(currentHost.getHostName(), currentService.getServiceName(), null);
					service = "<A HREF=\"" + url + "\">" + abreviateString(currentService.getServiceName(), 20) + "</A>";
				}
				else
					service = currentService.getServiceName();

				if (first)
				{
					first = false;
					reply.add("<TD>" + service + "</TD><TD BGCOLOR=\"#" + htmlServiceStateColor + "\">" + coffeeSaint.serviceState(serviceState) + "</TD></TR>\n");
				}
				else
				{
					reply.add("<TR><TD></TD><TD></TD><TD>" + currentService.getServiceName() + "</TD><TD BGCOLOR=\"#" + htmlServiceStateColor + "\">" + coffeeSaint.serviceState(serviceState) + "</TD></TR>\n");
				}
			}
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
		reply.add("<br /><br /><br />Please select an action in the menu at the left.");
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
		reply.add("<TR><TD>Designer of the logo/web-interface looks:</TD><TD><A HREF=\"http://www.properlydecent.com/\">http://www.properlydecent.com/</A></TD></TR>\n");
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
		reply.add("<TR><TD>Average refresh time:</TD><TD>" + String.format("%.4f", statistics.getTotalRefreshTime() / (double)nRefreshes) + "</TD></TR>\n");
		reply.add("<TR><TD>Total image refresh time:</TD><TD>" + statistics.getTotalImageLoadTime() + "</TD></TR>\n");
		reply.add("<TR><TD>Average image refresh time:</TD><TD>" + String.format("%.4f", statistics.getTotalImageLoadTime() / (double)nRefreshes) + "</TD></TR>\n");
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

		Calendar rightNow = Calendar.getInstance();

		JavNag javNag = CoffeeSaint.loadNagiosData(null, -1, null);
		List<Problem> problems = CoffeeSaint.findProblems(javNag);
		coffeeSaint.learnProblemCount(problems.size());
		coffeeSaint.collectPerformanceData(javNag);

		if (config.getShowHeader())
		{
			String header = coffeeSaint.getScreenHeader(javNag, rightNow);
			reply.add(header + "<BR>");
		}

		Color bgColor;
		if (problems.size() > 0)
			bgColor = config.getBackgroundColor();
		else
			bgColor = coffeeSaint.predictWithColor(rightNow);

		reply.add("<TABLE WIDTH=640 HEIGHT=400 TEXT=\"#" + htmlColorString(config.getTextColor()) + "\" BGCOLOR=\"#" + htmlColorString(bgColor) + "\">\n");

		for(Problem currentProblem : problems)
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

		if (problems.size() == 0)
		{
			if (config.getImageUrls().size() >= 1)
				reply.add("<TR VALIGN=CENTER><TD ALIGN=CENTER><IMG SRC=\"/image.jpg\" BORDER=\"0\"></TD></TR>\n");
			else if (config.getNagiosDataSources().size() == 0)
				reply.add("<TR VALIGN=CENTER><TD ALIGN=CENTER>NO NAGIOS SERVERS SELECTED!</TD></TR>\n");
			else
				reply.add("<TR VALIGN=CENTER><TD ALIGN=CENTER>All fine.</TD></TR>\n");
		}

		reply.add("</TABLE>\n");

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
			if (config.getRunGui() && gui != null)
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

		configNotWrittenToDisk = false;
		addHTTP200(reply);
		addPageHeader(reply, "");
		String fileName = config.getConfigFilename();

		try
		{
			config.writeConfig(fileName);
			reply.add("Wrote configuration to file: " + fileName +".<BR>\n");
		}
		catch(Exception e)
		{
			statistics.incExceptions();

			reply.add("Problem during storing of configuration-file: " + e);
			configNotWrittenToDisk = true;
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

	public String sparkLineUrl(String host, String service, String dataSource, int width, int height, boolean withMetaData) throws Exception
	{
		String url = "/cgi-bin/sparkline.cgi?width=" + width + "&height=" + height + "&host=" + URLEncoder.encode(host, defaultCharset);
		if (service != null)
			url += "&service=" + URLEncoder.encode(service, defaultCharset);
		if (dataSource != null)
			url += "&dataSource=" + URLEncoder.encode(dataSource, defaultCharset);
		url += "&metadata=" + (withMetaData ? "true" : "false");

		return url;
	}

	public String graphZoomInUrl(String host, String service, String dataSource) throws Exception
	{
		String url = "/cgi-bin/graph-zoomin.cgi?host=" + URLEncoder.encode(host, defaultCharset);
		if (service != null)
			url += "&service=" + URLEncoder.encode(service, defaultCharset);
		if (dataSource != null)
			url += "&dataSource=" + URLEncoder.encode(dataSource, defaultCharset);
		url += "&metadata=true";

		return url;
	}

	public String formatValue(double value)
	{
		if (value == Math.floor(value))
			return String.format("%.0f", value);

		return String.format("%.4f", value);
	}

	public String abreviateString(String in, int maxLen)
	{
		int len = in.length();
		if (len > maxLen)
		{
			int halfLenRight = maxLen / 2;
			int halfLenLeft  = maxLen - halfLenRight;

			halfLenLeft -= 1;
			halfLenRight -= 2;

			return in.substring(0, halfLenLeft) + "..." + in.substring(len - halfLenRight);
		}

		return in;
	}

	public void sendReply_cgibin_performancedata_cgi(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		JavNag javNag = CoffeeSaint.loadNagiosData(null, -1, null);
		List<Problem> problems = CoffeeSaint.findProblems(javNag);
		coffeeSaint.learnProblemCount(problems.size());
		coffeeSaint.collectPerformanceData(javNag);

		reply.add("<H1>Performance data</H1>\n");
		reply.add("<TABLE WIDTH=\"100%\">\n");
		reply.add("<TR><TD><B>host</B></TD><TD><B>service</B></TD><TD><B>parameter</B></TD><TD><B>min</B></TD><TD><B>max</B></TD><TD><B>avg</B></TD><TD><B>std.dev.</B></TD><TD><B>samples</B></TD><TD><B>sparkline</B></TD></TR>\n");
		for(Host currentHost : javNag.getListOfHosts())
		{
			List<DataSource> dataSources = coffeeSaint.getPerformanceData(currentHost.getHostName(), null);
			if (dataSources != null)
			{
				for(DataSource dataSource : dataSources)
				{
					DataInfo dataInfo = dataSource.getStats();

					String host, sparkCol = "<TD></TD>";
					if (coffeeSaint.havePerformanceData(currentHost.getHostName(), null))
					{
						String url = graphZoomInUrl(currentHost.getHostName(), null, dataSource.getDataSourceName());
						host = "<A HREF=\"" + url + "\">" + abreviateString(currentHost.getHostName(), 16) + "</A>";
						url = sparkLineUrl(currentHost.getHostName(), null, dataSource.getDataSourceName(), 100, 15, false);
						sparkCol = "<TD><IMG SRC=\"" + url + "\" BORDER=0></TD>";
					}
					else
						host = abreviateString(currentHost.getHostName(), 16);

					String unit = dataSource.getUnit();
					reply.add("<TR><TD>" + host + "</TD><TD></TD><TD>" + abreviateString(dataSource.getDataSourceName(), 17) + "</TD><TD>" + formatValue(dataInfo.getMin()) + unit + "</TD><TD>" + formatValue(dataInfo.getMax()) + unit + "</TD><TD>" + formatValue(dataInfo.getAvg()) + unit + "</TD><TD>" + formatValue(dataInfo.getSd()) + unit + "</TD><TD>" + dataInfo.getN() + "</TD>" + sparkCol + "</TR>\n");
				}
			}
			for(Service currentService : currentHost.getServices())
			{
				dataSources = coffeeSaint.getPerformanceData(currentHost.getHostName(), currentService.getServiceName());
				if (dataSources != null)
				{
					for(DataSource dataSource : dataSources)
					{
						DataInfo dataInfo = dataSource.getStats();

						String service, sparkCol = "<TD></TD>";
						if (coffeeSaint.havePerformanceData(currentHost.getHostName(), currentService.getServiceName()))
						{
							String url = graphZoomInUrl(currentHost.getHostName(), currentService.getServiceName(), dataSource.getDataSourceName());
							service = "<A HREF=\"" + url + "\">" + abreviateString(currentService.getServiceName(), 20) + "</A>";
							url = sparkLineUrl(currentHost.getHostName(), currentService.getServiceName(), dataSource.getDataSourceName(), 100, 15, false);
							sparkCol = "<TD><IMG SRC=\"" + url + "\" BORDER=0></TD>";
						}
						else
							service = abreviateString(currentService.getServiceName(), 20);

						String unit = dataSource.getUnit();
						reply.add("<TR><TD>" + abreviateString(currentHost.getHostName(), 16) + "</TD><TD>" + service + "</TD><TD>" + abreviateString(dataSource.getDataSourceName(), 17) + "</TD><TD>" + formatValue(dataInfo.getMin()) + unit + "</TD><TD>" + formatValue(dataInfo.getMax()) + unit + "</TD><TD>" + formatValue(dataInfo.getAvg()) + unit + "</TD><TD>" + formatValue(dataInfo.getSd()) + unit + "</TD><TD>" + dataInfo.getN() + "</TD>" + sparkCol + "</TR>\n");
					}
				}
			}
		}
		reply.add("</TABLE>\n");

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_helpescapes_html(MyHTTPServer socket) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply);
		addPageHeader(reply, "");

		reply.add("<H1>Escapes</H1>\n");
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
						CoffeeSaint.log.add("Listening on " + config.getHTTPServerListenAdapter() + ":" + config.getHTTPServerListenPort());

						socket = new MyHTTPServer(config.getHTTPServerListenAdapter(), config.getHTTPServerListenPort());
					}

					List<HTTPRequestData> request = socket.acceptConnectionGetRequest();
					if (request.size() == 0)
						continue;
					String requestType = request.get(0).getName();
					String url = request.get(0).getData().trim();
					int space = url.indexOf(" ");
					if (space != -1)
						url = url.substring(0, space);

					List<HTTPRequestData> getData = null;
					int questionMark = url.indexOf("?");
					if (questionMark != -1)
					{
						String parameters = "";
						if (questionMark < url.length() - 1)
							parameters = url.substring(questionMark + 1);
						url = url.substring(0, questionMark);
						getData = MyHTTPServer.splitHTTPLine(parameters);
					}

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

					if (url.charAt(0) != '/')
						url = "/" + url;

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
					else if (url.equals("/cgi-bin/select_configfile.cgi"))
						sendReply_cgibin_select_configfile_cgi(socket);
					else if (url.equals("/cgi-bin/select_configfile-do.cgi"))
					{
						List<HTTPRequestData> requestData = socket.getRequestData(request);
						sendReply_cgibin_select_configfile_do_cgi(socket, requestData);
					}
					else if (url.equals("/cgi-bin/config-do.cgi"))
					{
						List<HTTPRequestData> requestData = socket.getRequestData(request);
						sendReply_cgibin_configdo_cgi(socket, requestData);
						socket.closeServer();
						socket = null;
					}
					else if (url.equals("/cgi-bin/reload-config.cgi"))
						sendReply_cgibin_reloadconfig_cgi(socket);
					else if (url.equals("/cgi-bin/write-config.cgi"))
						sendReply_cgibin_writeconfig_cgi(socket);
					else if (url.equals("/cgi-bin/test-sound.cgi"))
						sendReply_cgibin_testsound_cgi(socket);
					else if (url.equals("/cgi-bin/log.cgi"))
						sendReply_cgibin_log_cgi(socket);
					else if (url.equals("/images/bg01.png"))
						sendReply_images_bg01_png(socket, isHeadRequest);
					else if (url.equals("/images/coffee.png"))
						sendReply_images_coffee_png(socket, isHeadRequest);
					else if (url.equals("/images/footer01.png"))
						sendReply_images_footer01_png(socket, isHeadRequest);
					else if (url.equals("/images/saint01.png"))
						sendReply_images_saint01_png(socket, isHeadRequest);
					else if (url.equals("/images/title01.png"))
						sendReply_images_title01_png(socket, isHeadRequest);
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
					else if (url.equals("/cgi-bin/performance-data.cgi"))
						sendReply_cgibin_performancedata_cgi(socket);
					else if (url.equals("/design.css"))
						sendReply_design_css(socket, isHeadRequest);
					else if (url.equals("/cgi-bin/sparkline.cgi"))
						sendReply_cgibin_sparkline_cgi(socket, getData);
					else if (url.equals("/cgi-bin/graph-zoomin.cgi"))
						sendReply_cgibin_zoomin_cgi(socket, getData);
					else
					{
						sendReply_404(socket, url);
						webServer404++;
					}
				}
				catch(SocketException se)
				{
					CoffeeSaint.log.add("Exception: " + se);
					try
					{
						socket.close();
					}
					catch(SocketException se2)
					{
						CoffeeSaint.log.add("Could not close socket after SocketException, continuing");
					}
				}
				catch(Exception e)
				{
					statistics.incExceptions();

					CoffeeSaint.log.add("Exception during command processing");
					CoffeeSaint.showException(e);
					if (socket != null)
					{
						socket.closeServer();
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
