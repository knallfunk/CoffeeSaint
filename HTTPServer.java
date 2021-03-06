/* Released under GPL2, (C) 2009-2011 by folkert@vanheusden.com */
import com.vanheusden.sockets.HTTPRequestData;
import com.vanheusden.sockets.MyHTTPServer;
import com.vanheusden.nagios.*;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.lang.management.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
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
import java.util.Random;
import javax.imageio.ImageIO;

class HTTPServer implements Runnable {
	final Config config;
	final CoffeeSaint coffeeSaint;
	final Statistics statistics;
	final Gui gui;
	final List<HTTPLogEntry> hosts = new ArrayList<HTTPLogEntry>();
	//
	int webServerHits, webServer404;
	boolean configNotWrittenToDisk = false;
	final private String defaultCharset = "US-ASCII";
	final List<HTTPSession> sessions = new ArrayList<HTTPSession>();
	//
	final int maxNHostsPerPage = 50;

	public void expireSessions(List<HTTPSession> sessions) {
		long now = System.currentTimeMillis();
		long maxOld = now - (config.getWebSessionExpire() * 1000);

		for(int index=sessions.size() - 1; index >= 0; index--) {
			if (sessions.get(index).getLastUpdate() < maxOld) {
				CoffeeSaint.log.add("Session with " + sessions.get(index).getHost() + " expired.");
				sessions.remove(index);
			}
		}
	}

	public boolean sessionValid(List<HTTPSession> sessions, String host, String authCookie) {
		for(HTTPSession session : sessions) {
			if (session.getHost().equals(host) && session.getCookie().equals(authCookie))
				return true;
		}

		return false;
	}

	public HTTPServer(Config config, CoffeeSaint coffeeSaint, Statistics statistics, Gui gui) {
		this.config = config;
		this.coffeeSaint = coffeeSaint;
		this.statistics = statistics;
		this.gui = gui;
	}

	public void addHTTP200(List<String> whereTo, String cookie) {
		whereTo.add("HTTP/1.0 200 OK\r\n");
		whereTo.add("Date: " + getHTTPDate(Calendar.getInstance()) + "\r\n");
		whereTo.add("Server: " + CoffeeSaint.getVersion() + "\r\n");
		if (cookie != null) {
			System.out.println("Set-Cookie: " + cookie);
			whereTo.add("Set-Cookie: " + cookie + "\r\n");
		}
		whereTo.add("Connection: close\r\n");
		whereTo.add("Content-Type: text/html\r\n");
		whereTo.add("\r\n");
	}

	public void addPageHeader(List<String> whereTo, String head) {
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
		whereTo.add("				<a href=\"/latency-graph.html\">Latency graph</a><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/list-all.cgi\">List of hosts/services</a><br />\n");

		boolean file = false;
		for(NagiosDataSource nds : config.getNagiosDataSources()) {
			if (nds.getType() == NagiosDataSourceType.FILE) {
				file = true;
				break;
			}
		}
		if (file)
			whereTo.add("				<a href=\"/cgi-bin/nagios_status.cgi\">Problems overview</a><br />\n");
		else
			whereTo.add("				<a href=\"/applet.html\">Problems overview</a><br />\n");

		whereTo.add("				<br /><strong>Logging</strong><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/statistics.cgi\">CoffeeSaint statistics</a><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/log.cgi\">List of connecting hosts</a><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/list-log.cgi\">Show log</a><br />\n");
		whereTo.add("                           <a href=\"/cgi-bin/jvm_stats.cgi\">JVM statistics</a><br />\n");
		whereTo.add("				<br /><strong>Configuration</strong><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/config-menu.cgi\">Configure CoffeeSaint</a><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/reload-config.cgi\">Reload configuration</a><br />\n");
		whereTo.add("				<a href=\"/cgi-bin/select_configfile.cgi\">Select configuration file</a><br />\n");
                if (config.getConfigFilename() == null)
                        whereTo.add("No configuration-file selected, save disabled<br />\n");
                else {
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

	public String formatDate(Calendar when) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("E yyyy.MM.dd  hh:mm:ss a zzz");

		return dateFormatter.format(when.getTime());
	}

	public void addPageTail(List<String> whereTo, boolean mainMenu) { 
		whereTo.add("				<br />\n");
		whereTo.add("			</div>\n");
		whereTo.add("		</div>\n");
		whereTo.add("	</div>\n");
		whereTo.add("</body>\n");
		whereTo.add("</html>\n");

//		whereTo.add(formatDate(Calendar.getInstance()) + "</TD></TR></TABLE></BODY></HTML>");
	}

	public long getModificationDate(String fileName) throws Exception {
		URL url = getClass().getClassLoader().getResource(fileName);
		URLConnection urlConnection = url.openConnection();
		return urlConnection.getLastModified();
	}

	public String getHTTPDate(Calendar when) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss a zzz");

		return dateFormatter.format(when.getTime());
	}

	public String getModificationDateString(String fileName) throws Exception {
		long ts = getModificationDate(fileName);

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(ts);

		return getHTTPDate(calendar);
	}

	public void sendReply_CoffeeSaint_jar(MyHTTPServer socket) throws Exception {
		URL jarFileURL = Applet.class.getProtectionDomain().getCodeSource().getLocation();

		String reply = "HTTP/1.0 200 OK\r\n";
		reply += "Date: " + getHTTPDate(Calendar.getInstance()) + "\r\n";
		reply += "Server: " + CoffeeSaint.getVersion() + "\r\n";
		reply += "Last-Modified: Tue, 20 Jul 2011 07:56:40 GMT\r\n";
		reply += "Connection: close\r\n";
		reply += "Content-Type: application/java-archive\r\n";
		reply += "\r\n";
		socket.getOutputStream().write(reply.getBytes());

		InputStream is = jarFileURL.openStream();
		int length = is.available();
		CoffeeSaint.log.add("Sending " + jarFileURL + " which is " + length + " bytes long.");
		byte [] jar = new byte[length];
		while(length > 0) {
			int nRead = is.read(jar);
			if (nRead < 0)
				break;
			socket.getOutputStream().write(jar, 0, nRead);
			length -= nRead;
		}

		socket.close();
	}

	public void sendReply_send_file_from_jar(MyHTTPServer socket, String fileName, String mimeType, boolean headRequest/*, String cookie*/) throws Exception {
		String reply = "HTTP/1.0 200 OK\r\n";
		reply += "Date: " + getHTTPDate(Calendar.getInstance()) + "\r\n";
		reply += "Server: " + CoffeeSaint.getVersion() + "\r\n";
		// if (cookie != null)
		// 	reply += "Set-Cookie: " + cookie + "\r\n";
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

	public void sendReply_imagejpg(MyHTTPServer socket, String cookie)
	{
		String header;
		if (cookie != null)
			header = "HTTP/1.0 200 OK\r\nConnection: close\r\nSet-Cookie: " + cookie + "\r\nContent-Type: image/jpeg\r\n\r\n";
		else
			header = "HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type: image/jpeg\r\n\r\n";
		
		try
		{
			socket.getOutputStream().write(header.getBytes());
			ImageLoadingParameters ilp = coffeeSaint.startLoadingImages(null, -1, null);
			Image img = coffeeSaint.loadImage(ilp, null, -1, null)[0].getImage();
			ImageIO.write(CoffeeSaint.createBufferedImage(img), "jpg", socket.getOutputStream());
			socket.close();
		}
		catch(Exception e)
		{
			// really don't care if the transmit failed; browser
			// probably closed session
			// don't care if we could display the image or not
		}
	}

	public void sendReply_cgibin_zoomin_cgi(MyHTTPServer socket, List<HTTPRequestData> getData, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
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

	public void sendReply_cgibin_sparkline_cgi(MyHTTPServer socket, List<HTTPRequestData> getData, String cookie) throws Exception
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

			if (host != null)
			{
				String header;
				if (cookie == null)
					header = "HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type: image/png\r\n\r\n";
				else
					header = "HTTP/1.0 200 OK\r\nSet-Cookie: " + cookie + "\r\nConnection: close\r\nContent-Type: image/png\r\n\r\n";
				socket.getOutputStream().write(header.getBytes());
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

	public void sendReply_cgibin_latency_cgi(MyHTTPServer socket, List<HTTPRequestData> getData, String cookie) throws Exception
	{
		try
		{
			int width = 920;
			HTTPRequestData widthData = MyHTTPServer.findRecord(getData, "width");
			if (widthData != null && widthData.getData() != null)
				width = Integer.valueOf(widthData.getData());

			int height = 150;
			HTTPRequestData heightData = MyHTTPServer.findRecord(getData, "height");
			if (heightData != null && heightData.getData() != null)
				height = Integer.valueOf(heightData.getData());

			boolean withMetaData = true;
			HTTPRequestData withMetaDataData = MyHTTPServer.findRecord(getData, "metadata");
			if (withMetaDataData != null && withMetaDataData.getData() != null)
				withMetaData = URLDecoder.decode(withMetaDataData.getData(), defaultCharset).equalsIgnoreCase("true");

			String header;
			if (cookie == null)
				header = "HTTP/1.0 200 OK\r\nConnection: close\r\nContent-Type: image/png\r\n\r\n";
			else
				header = "HTTP/1.0 200 OK\r\nSet-Cookie: " + cookie + "\r\nConnection: close\r\nContent-Type: image/png\r\n\r\n";
			socket.getOutputStream().write(header.getBytes());
			BufferedImage sparkLine = coffeeSaint.getLatencyGraph(width, height, withMetaData);
			ImageIO.write(sparkLine, "png", socket.getOutputStream());
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

	public void colorSelectorHTML(List<String> reply, String name, String selectedColor, boolean allowNone)
	{
		reply.add("<SELECT NAME=\"" + name + "\">\n");
		if (allowNone)
			reply.add("<OPTION VALUE=\"NULL\">NONE</OPTION>\n");
		for(ColorPair cp : config.getColors())
		{
			String line = "<OPTION VALUE=\"" + cp.getName() + "\"";
			if (selectedColor != null && selectedColor.equalsIgnoreCase(cp.getName()))
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

	public void sendReply_cgibin_select_configfile_cgi(MyHTTPServer socket, String cookie) throws Exception
	{
		File dir = new File(".");
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
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
			stringSelectorHTML(reply, "config-file-list", CoffeeSaint.convertStringArrayToList(files), currentFile != null ? currentFile : "---NONE---", true);
			reply.add("</TD></TR>\n");
			reply.add("<TR><TD></TD><TD><INPUT TYPE=\"SUBMIT\" VALUE=\"Submit changes!\"></TD></TR>\n");
			reply.add("</TABLE>\n");
			reply.add("<BR>\n");

			reply.add("</FORM>\n");
		}

		addPageTail(reply, true);

		socket.sendReply(reply);
	}


	public void sendReply_cgibin_system_info(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
		addPageHeader(reply, "");

                Runtime runtime = Runtime.getRuntime();
                OperatingSystemMXBean osmxb = ManagementFactory.getOperatingSystemMXBean();
                assert osmxb != null;
                RuntimeMXBean rmxb = ManagementFactory.getRuntimeMXBean();
                assert rmxb != null;
                ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
                assert tmxb != null;

		Runtime rt = Runtime.getRuntime();
		rt.gc();

		reply.add("<TABLE>");
		reply.add("<TR><TD WIDTH=150>processors:</TD><TD>" + runtime.availableProcessors() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>system architecture:</TD><TD>" + osmxb.getArch() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>OS version:</TD><TD>" + osmxb.getVersion() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>system load:</TD><TD>" + osmxb.getSystemLoadAverage() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>JVM name:</TD><TD>" + rmxb.getName() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>JVM impl.name:</TD><TD>" + rmxb.getVmName() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>JVM vendor:</TD><TD>" + rmxb.getVmVendor() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>JVM version:</TD><TD>" + rmxb.getVmVersion() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>JVM uptime:</TD><TD>" + (rmxb.getUptime() / 1000) + "s</TD></TR>");
		reply.add("<TR><TD WIDTH=150>Boot classpath:</TD><TD>" + rmxb.getBootClassPath() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>Classpath:</TD><TD>" + rmxb.getClassPath() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>deadlocked threads:</TD><TD>" + (tmxb.findDeadlockedThreads() != null ? tmxb.findDeadlockedThreads().length : 0 ) + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>number of threads:</TD><TD>" + tmxb.getThreadCount() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>total threads created:</TD><TD>" + tmxb.getTotalStartedThreadCount() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>thread peak count:</TD><TD>" + tmxb.getPeakThreadCount() + "</TD></TR>");
		reply.add("<TR><TD WIDTH=150>total CPU time:</TD><TD>" + (tmxb.getCurrentThreadCpuTime() / 1000000000) + "s</TD></TR>");
		reply.add("<TR><TD WIDTH=150>maximum memory:</TD><TD>" + ((rt.maxMemory() + 1048575) / 1048576) + "MB</TD></TR>");
		reply.add("<TR><TD WIDTH=150>total memory:</TD><TD>" +  ((rt.totalMemory() + 1048575) / 1048576) + "MB</TD></TR>");
		reply.add("<TR><TD WIDTH=150>free memory:</TD><TD>" + ((rt.freeMemory() + 1048575) / 1048576) + "MB</TD></TR>");
		// reply.add("<TR><TD WIDTH=150></TD><TD>" +  + "</TD></TR>");

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_select_configfile_do_cgi(MyHTTPServer socket, List<HTTPRequestData> requestData, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
		addPageHeader(reply, "");

		if (config.getDisableHTTPFileselect())
		{
			reply.add("Access denied");
		}
		else
		{
			String newFileName = getField(requestData, "config-file");
			if (newFileName == null || newFileName.equals(""))
				newFileName = getField(requestData, "config-file-list");
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
		if (current != null && newValue.equals(current))
			out += " SELECTED";
		out += ">" + newValue + "</OPTION>\n";
		return out;
	}

	public void sendReply_cgibin_configmenu_cgi(MyHTTPServer socket, String cookie) throws Exception
	{
		System.out.println("sendReply_cgibin_configmenu_cgi START");
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
		addPageHeader(reply, "");

		reply.add("<FORM ACTION=\"/cgi-bin/config-do.cgi\" METHOD=\"POST\">\n");

		reply.add("<H1>Nagios handling parameters</H1>\n");
		reply.add("<TABLE>\n");
		reply.add("<TR><TD>Always notify:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"always_notify\" VALUE=\"on\" " + isChecked(config.getAlwaysNotify()) + "></TD><TD>Also display when notifications are disabled</TD></TR>\n");
		reply.add("<TR><TD>Also acknowledged:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_acknowledged\" VALUE=\"on\" " + isChecked(config.getAlsoAcknowledged()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Host problem acknowledged, show services problems:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"host_also_acknowledged\" VALUE=\"on\" " + isChecked(config.getHostAcknowledgedShowServices()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Also scheduled downtime</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_scheduled_downtime\" VALUE=\"on\" " + isChecked(config.getAlsoScheduledDowntime()) + "></TD><TD>Also display problems for which downtime has been scheduled</TD></TR>\n");
		reply.add("<TR><TD>Host scheduled downtime, show services problems:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"host_also_scheduled_downtime\" VALUE=\"on\" " + isChecked(config.getHostScheduledDowntimeShowServices()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Also soft state:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_soft_State\" VALUE=\"on\" " + isChecked(config.getAlsoSoftState()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Also disabled checks:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"also_disabled_active_checks\" VALUE=\"on\" " + isChecked(config.getAlsoDisabledActiveChecks()) + "></TD><TD>Also display problems for which active checks have been disabled</TD></TR>\n");
		reply.add("<TR><TD>Show services for host with problems:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"show_services_for_host_with_problems\" VALUE=\"on\" " + isChecked(config.getShowServicesForHostWithProblems()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Show services for host with acked/scheduled downtime:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"host_scheduled_downtime_or_ack_show_services\" VALUE=\"on\" " + isChecked(config.getHostSDOrAckShowServices()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Show flapping:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"show-flapping\" VALUE=\"on\" " + isChecked(config.getShowFlapping()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Maximum check age (to see<BR>if Nagios still runs):</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"max-check-age\" VALUE=\"" + config.getMaxCheckAge() + "\"></TD><TD>Set to -1 to disable</TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<BR>\n");

		reply.add("<H1>Network parameters</H1>\n");
		reply.add("Please note that after you click on submit, the new network-settings are applied immeditately.<BR>\n");
		reply.add("<TABLE>\n");
		String proxyHost = config.getProxyHost();
		reply.add("<TR><TD>HTTP proxy (for outbound connections) host:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"proxy-host\" VALUE=\"" + (proxyHost != null ? proxyHost : "") + "\"></TD><TD>empty to disable</TD></TR>\n");
		reply.add("<TR><TD>HTTP proxy port:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"proxy-port\" VALUE=\"" + (proxyHost != null ? "" + config.getProxyPort() : "8080") + "\"></TD><TD></TD></TR>\n");
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
		reply.add("</TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Port to listen on:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"network-port\" VALUE=\"" + config.getHTTPServerListenPort() + "\"></TD><TD></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<BR>\n");

		reply.add("<H1>Nagios server(s)</H1>\n");
		reply.add("See the <A HREF=\"http://vanheusden.com/java/CoffeeSaint/#howto\">CoffeeSaint website</A> to see what you can enter here.<BR>\n");
		reply.add("Please note that 'username' and 'password' are only for 'http-auth'.<BR>\n");
		reply.add("Also please note that http/https are NOT the urls of the Nagios web-interface but URLs of the status.dat-file. See <A HREF=\"http://vanheusden.com/java/CoffeeSaint/#url\">this page</A> for more info.<BR>\n");
		reply.add("<TABLE>\n");
		reply.add("<TR><TD><B>type</B></TD><TD><B>Nagios version</B></TD><TD><B>data source</B></TD><TD><B>remove?</B></TD><TD><B>username</B></TD><TD><B>password</B></TD><TD><B>pretty name</B></TD></TR>\n");
		for(NagiosDataSource dataSource : config.getNagiosDataSources())
		{
			String type = "?", username = "", password = "";
			if (dataSource.getType() == NagiosDataSourceType.TCP)
				type = "tcp";
			else if (dataSource.getType() == NagiosDataSourceType.ZTCP)
				type = "compressed tcp";
			else if (dataSource.getType() == NagiosDataSourceType.LS)
				type = "LiveStatus tcp socket";
			else if (dataSource.getType() == NagiosDataSourceType.HTTP)
			{
				if (dataSource.getUsername() != null)
				{
					type = "http-auth";
					username = dataSource.getUsername();
					password = dataSource.getPassword();
				}
				else
					type = "http";
			}
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
			if (dataSource.getType() == NagiosDataSourceType.TCP || dataSource.getType() == NagiosDataSourceType.ZTCP || dataSource.getType() == NagiosDataSourceType.LS)
				parameters = dataSource.getHost() + " " + dataSource.getPort();
			else if (dataSource.getType() == NagiosDataSourceType.HTTP)
				parameters = dataSource.getURL().toString();
			else if (dataSource.getType() == NagiosDataSourceType.FILE)
				parameters = dataSource.getFile();

			String pn = dataSource.getPrettyName();
			if (pn == null)
				pn = "";

			String serverString = parameters;
			reply.add("<TR><TD>" + type + "</TD><TD>" + version + "</TD><TD>" + parameters + "</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"serverid_" + serverString.hashCode() + "\" VALUE=\"on\"></TD><TD>" + username + "</TD><TD>" + password +"</TD><TD>" + pn + "</TD></TR>\n");
		}
		reply.add("<TR>\n");
		reply.add("<TD><SELECT NAME=\"server-add-type\"><OPTION VALUE=\"tcp\">TCP</OPTION><OPTION VALUE=\"ztcp\">GZIP compressed TCP</OPTION><OPTION VALUE=\"ls\">LiveStatus TCP socket</OPTION><OPTION VALUE=\"http\">HTTP</OPTION><OPTION VALUE=\"file\">FILE</OPTION></SELECT></TD>\n");
		reply.add("<TD><SELECT NAME=\"server-add-version\"><OPTION VALUE=\"1\">1</OPTION><OPTION VALUE=\"2\">2</OPTION><OPTION VALUE=\"3\" SELECTED>3</OPTION></SELECT></TD>\n");
		reply.add("<TD><INPUT TYPE=\"TEXT\" NAME=\"server-add-parameters\"></TD>\n");
		reply.add("<TD></TD>\n");
		reply.add("<TD><INPUT TYPE=\"TEXT\" NAME=\"nagios-http-username\"></TD>\n");
		reply.add("<TD><INPUT TYPE=\"PASSWORD\" NAME=\"nagios-http-password\"></TD>\n");
		reply.add("<TD><INPUT TYPE=\"TEXT\" NAME=\"nagios-pretty-name\"></TD>\n");
		reply.add("</TR>\n");
		reply.add("<TR><TD>Use HTTP compression:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"disable-http-compression\" VALUE=\"on\" " + isChecked(config.getAllowHTTPCompression()) + "></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("TCP requires an ip-address followed by a space and a port-number in the parameters field.<BR>\n");
		reply.add("<BR>\n");

		reply.add("<H1>Look and feel parameters</H1>\n");
		reply.add("<TABLE>\n");
		reply.add("<TR><TD>Refresh interval:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"sleepTime\" VALUE=\"" + config.getSleepTime() + "\"></TD><TD>&gt; 1</TD></TR>\n");
		reply.add("<TR><TD>Double buffering:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"double-buffering\" VALUE=\"on\" " + isChecked(config.getDoubleBuffering()) + "></TD><TD>Might speed-up/slow-down screen refreshes</TD></TR>\n");
		reply.add("<TR><TD>Fullscreen mode:</TD><TD><SELECT NAME=\"fullscreen\">\n");
		reply.add(selectField(config.getFullscreenName(), "none"));
		reply.add(selectField(config.getFullscreenName(), "undecorated"));
		reply.add(selectField(config.getFullscreenName(), "fullscreen"));
		reply.add(selectField(config.getFullscreenName(), "allmonitors"));
		reply.add("</SELECT></TD><TD>Requires restart of CoffeeSaint.</TD></TR>\n");
		if (config.getRunGui()) {
			reply.add("<TR><TD>Select monitor:</TD><TD><SELECT NAME=\"use-screen\">\n");
			reply.add(selectField(config.getUseScreen(), "ALL"));
			List<Monitor> monitors = CoffeeSaint.getMonitors();
			for(Monitor monitor : monitors)
				reply.add(selectField(config.getUseScreen(), monitor.getDeviceName()));
			reply.add("</SELECT></TD><TD>Requires restart of CoffeeSaint.</TD></TR>\n");
		}
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
		reply.add("<TR><TD>Minimum row height:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"min-row-height\" VALUE=\"" + config.getMinRowHeight() + "\"></TD><TD>or -1 to not auto adjust the row height</TD></TR>\n");
		reply.add("<TR><TD>Number of columns:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"problem-columns\" VALUE=\"" + config.getNProblemCols() + "\"></TD><TD>&gt;= 1</TD></TR>\n");
		reply.add("<TR><TD>Flexible number of columns:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"flexible-n-columns\" VALUE=\"on\" " + isChecked(config.getFlexibleNColumns()) + "></TD><TD>Use in combination with number of columns</TD></TR>\n");
		GraphicsEnvironment lge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		List<String> fontNames = CoffeeSaint.convertStringArrayToList(lge.getAvailableFontFamilyNames());
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
		reply.add("<TR><TD>Anti-alias:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"anti-alias\" VALUE=\"on\" " + isChecked(config.getAntiAlias()) + "></TD><TD>Slows down considerably in some situations but improves how fonts are shown</TD></TR>\n");
		reply.add("<TR><TD>Max. quality graphics:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"max-quality-graphics\" VALUE=\"on\" " + isChecked(config.getMaxQualityGraphics()) + "></TD><TD>Slows down considerably in some situations</TD></TR>\n");
		reply.add("<TR><TD>Transparency:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"transparency\" VALUE=\"" + config.getTransparency() + "\"></TD><TD>0.0...1.0 only usefull with background image/webcam, 1.0 = not transparent</TD></TR>\n");
		reply.add("<TR><TD>Header transparency:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"header-transparency\" VALUE=\"" + config.getHeaderTransparency() + "\"></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Row border:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"row-border\" VALUE=\"on\" " + isChecked(config.getRowBorder()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Header border height:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"upper-row-border-height\" VALUE=\"" + config.getUpperRowBorderHeight() + "\"></TD><TD>In case you want a thicker bar between the header and the problem list.</TD></TR>\n");
		reply.add("<TR><TD>Row border color:</TD><TD>\n");
		colorSelectorHTML(reply, "row-border-color", config.getRowBorderColorName(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Graph color:</TD><TD>\n");
		colorSelectorHTML(reply, "graph-color", config.getGraphColorName(), false);
		reply.add("</TD><TD></TD></TR>");
		// reply.add("<TR><TD>Header background color:</TD><TD>\n");
		// colorSelectorHTML(reply, "header-color", config.getHeaderColorName(), true);
		// reply.add("</TD><TD>Select 'NONE' for state-color(!).</TD></TR>");
		reply.add("<TR><TD>Text color:</TD><TD>\n");
		colorSelectorHTML(reply, "textColor", config.getTextColorName(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Warning text color:</TD><TD>\n");
		colorSelectorHTML(reply, "warningTextColor", config.getWarningTextColorName(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Critical text color:</TD><TD>\n");
		colorSelectorHTML(reply, "criticalTextColor", config.getCriticalTextColorName(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Background color:</TD><TD>\n");
		colorSelectorHTML(reply, "backgroundColor", config.getBackgroundColorName(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Background fade-to color:</TD><TD>\n");
		colorSelectorHTML(reply, "bgcolor-fade-to", config.getBackgroundColorFadeToName(), true);
		reply.add("</TD><TD>(color gradient)</TD></TR>");
		reply.add("<TR><TD>Problem bar fade-to-color:</TD><TD>\n");
		colorSelectorHTML(reply, "problem-row-gradient", config.getProblemRowGradientName(), true);
		reply.add("</TD><TD>(color gradient)</TD></TR>");
		reply.add("<TR><TD>Background color OK-status:</TD><TD>\n");
		colorSelectorHTML(reply, "bgColorOk", config.getBackgroundColorOkStatusName(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Background color warning-status (HARD):</TD><TD>\n");
		colorSelectorHTML(reply, "warning-bg-color", config.getWarningBgColorName(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Background color warning-status (SOFT):</TD><TD>\n");
		colorSelectorHTML(reply, "warning-bg-color-soft", config.getWarningBgColorNameSoft(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Background color critical-status (HARD):</TD><TD>\n");
		colorSelectorHTML(reply, "critical-bg-color", config.getCriticalBgColorName(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Background color critical-status (SOFT):</TD><TD>\n");
		colorSelectorHTML(reply, "critical-bg-color-soft", config.getCriticalBgColorNameSoft(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Background color unknown-status:</TD><TD>\n");
		colorSelectorHTML(reply, "unknown-bg-color", config.getNagiosUnknownBgColorName(), false);
		reply.add("</TD><TD></TD></TR>");
		reply.add("<TR><TD>Let bg color depend on state:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"color-bg-to-state\" VALUE=\"on\" " + isChecked(config.getSetBgColorToState()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Host issues:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"host-issue\" VALUE=\"" + config.getHostIssue() + "\"></TD><TD><A HREF=\"/help-escapes.html\" TARGET=\"_new\">List of escapes</A></TD></TR>\n");
		reply.add("<TR><TD>Service issues:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"service-issue\" VALUE=\"" + config.getServiceIssue() + "\"></TD><TD><A HREF=\"/help-escapes.html\" TARGET=\"_new\">List of escapes</A></TD></TR>\n");
		reply.add("<TR><TD>Header:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"header\" VALUE=\"" + config.getHeader() + "\"></TD><TD><A HREF=\"/help-escapes.html\" TARGET=\"_new\">List of escapes</A></TD></TR>\n");
		reply.add("<TR><TD>Footer:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"footer\" VALUE=\"" + (config.getFooter() != null ? config.getFooter() : "") + "\"></TD><TD><A HREF=\"/help-escapes.html\" TARGET=\"_new\">List of escapes</A></TD></TR>\n");
		reply.add("<TR><TD>Show header:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"show-header\" VALUE=\"on\" " + isChecked(config.getShowHeader()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Flash (blink) problems:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"flash\" VALUE=\"on\" " + isChecked(config.getFlash()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Show unknown:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"display-unknown\" VALUE=\"on\" " + isChecked(config.getDisplayUnknown()) + "></TD><TD>Display with unknown/pending state</TD></TR>\n");
		reply.add("<TR><TD>Show flapping icon:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"show-flapping-icon\" VALUE=\"on\" " + isChecked(config.getShowFlappingIcon()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Header always bg color:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"header-always-bgcolor\" VALUE=\"on\" " + isChecked(config.getHeaderAlwaysBGColor()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Scroll header:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"scrolling-header\" VALUE=\"on\" " + isChecked(config.getScrollingHeader()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Scroll footer:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"scrolling-footer\" VALUE=\"on\" " + isChecked(config.getScrollingFooter()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Scroll problems:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"scroll-if-not-fitting\" VALUE=\"on\" " + isChecked(config.getScrollIfNotFit()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Scroll pixels/sec:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"scroll-pixels-per-sec\" VALUE=\"" + config.getScrollingPixelsPerSecond() + "\"></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Text split offsets:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"scroll-splitter\" VALUE=\"" + (config.getLineScrollSplitterString() == null ? "" : "" + config.getLineScrollSplitterString()) + "\"></TD><TD>At what offsets start drawing<BR> the \\T text splitters (tab stops).</TD></TR>\n");
		reply.add("<TR><TD>Draw split-line:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"draw-problems-service-split-line\" VALUE=\"on\" " + isChecked(config.getDrawProblemServiceSplitLine()) + "></TD><TD></TD></TR>\n");
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
		reply.add("<TR><TD>Ok message:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"no-problems-text\" VALUE=\"" + (config.getNoProblemsText() != null ? config.getNoProblemsText() : "") + "\"></TD><TD>Message to show if there are no problems.</TD></TR>\n");
		reply.add("<TR><TD>Ok message position:</TD><TD><SELECT NAME=\"no-problems-text-position\">\n");
		reply.add(selectField(config.getNoProblemsTextPositionName(), "upper-left"));
		reply.add(selectField(config.getNoProblemsTextPositionName(), "upper-right"));
		reply.add(selectField(config.getNoProblemsTextPositionName(), "lower-left"));
		reply.add(selectField(config.getNoProblemsTextPositionName(), "lower-right"));
		reply.add(selectField(config.getNoProblemsTextPositionName(), "center"));
		reply.add(selectField(config.getNoProblemsTextPositionName(), "nowhere"));
		reply.add("</SELECT></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Problem message:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"state-problems-text\" VALUE=\"" + (config.getStateProblemsText() != null ? config.getStateProblemsText() : "") + "\"></TD><TD>Used in the %STATE escape string.</TD></TR>\n");
		reply.add("<TR><TD>Problem message bg color:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"no-problems-text-with-bg-color\" VALUE=\"on\" " + isChecked(config.getNoProblemsTextBg()) + "></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Logo:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"logo-url\" VALUE=\"" + (config.getLogo() != null ? config.getLogo() : "") + "\"></TD><TD>Either a pathname or an URL.</TD></TR>\n");
		reply.add("<TR><TD>Logo position:</TD><TD><SELECT NAME=\"logo-position\">\n");
		Position lp = config.getLogoPosition();
		reply.add("<OPTION VALUE=\"left\"");
		if (lp == Position.LEFT || lp == Position.UPPER_LEFT)
			reply.add(" SELECTED");
		reply.add(">left</OPTION>");
		reply.add("<OPTION VALUE=\"right\"");
		if (lp == Position.RIGHT || lp == Position.UPPER_RIGHT)
			reply.add(" SELECTED");
		reply.add(">right</OPTION>");
		reply.add("</SELECT></TD><TD></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<BR>\n");

		if (config.getDisableHTTPFileselect() == false)
		{
			reply.add("<H1>Files</H1>\n");
			reply.add("<TABLE>\n");
			reply.add("<TR><TD>File to store prediction data in:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"brain-file\" VALUE=\"" + (config.getBrainFileName() != null ? config.getBrainFileName() : "")+ "\"></TD><TD>Used for predicting problem count</TD></TR>\n");
			reply.add("<TR><TD>File to store performance data in:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"performance-data\" VALUE=\"" + (config.getPerformanceDataFileName() != null ? config.getPerformanceDataFileName() : "") + "\"></TD><TD>Used for sparklines</TD></TR>\n");
			reply.add("<TR><TD>File to store latency data in:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"latency-file\" VALUE=\"" + (config.getLatencyFile() != null ? config.getLatencyFile() : "") + "\"></TD><TD></TD></TR>\n");
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

		reply.add("<H1>Multimedia</H1>\n");
		reply.add("<H2>Warning sound</H2>\n");
		reply.add("<TABLE>\n");
		reply.add("<TR><TD>Warning sound:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"warning-sound\" VALUE=\"" + config.getProblemSound() + "\"></TD><TD>File path</TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<H2>Webcams</H2>\n");
		reply.add("Please note: regular JPEG camera's require type \"HTTP\" or \"HTTPS\". Streaming webcams require \"MJPEG\".<BR>\n");
		reply.add("<TABLE>\n");
		for(String image : config.getImageUrls())
			reply.add("<TR><TD>Remove webcam:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"webcam_" + image.hashCode() + "\" VALUE=\"on\"><A HREF=\"" + image + "\" TARGET=\"_new\">" + image + "</A></TD></TR>\n");
		reply.add("<TR><TD>Add webcam:</TD><TD><SELECT NAME=\"newWebcamType\"><OPTION VALUE=\"MJPEG\">MJPEG</OPTION><OPTION VALUE=\"HTTP\">HTTP</OPTION><OPTION VALUE=\"HTTPS\">HTTPS</OPTION><OPTION VALUE=\"FILE\">FILE</OPTION></SELECT> <INPUT TYPE=\"TEXT\" NAME=\"newWebcam\"></TD></TR>\n");
		String to;
		if (config.getWebcamTimeout() == -1)
			to = "";
		else
			to = "" + config.getWebcamTimeout();
		reply.add("<TR><TD>Loading timeout:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"webcam-timeout\" VALUE=\"" + to + "\"></TD><TD>In seconds, &gt; 1, leave empty to disable</TD></TR>\n");
		reply.add("<TR><TD>Adapt image size:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"adapt-img\" VALUE=\"on\" " + isChecked(config.getAdaptImageSize()) + "> (fit below list of problems)</TD></TR>\n");
		reply.add("<TR><TD>Randomize order of images:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"random-img\" VALUE=\"on\" " + isChecked(config.getRandomWebcam()) + "></TD></TR>\n");
		reply.add("<TR><TD>Number of columns:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"cam-cols\" VALUE=\"" + config.getCamCols() + "\"></TD></TR>\n");
		reply.add("<TR><TD>Number of rows:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"cam-rows\" VALUE=\"" + config.getCamRows() + "\"></TD></TR>\n");
		reply.add("<TR><TD>Keep aspect ratio:</TD><TD><INPUT TYPE=\"CHECKBOX\" NAME=\"keep-aspect-ratio\" VALUE=\"on\" " + isChecked(config.getKeepAspectRatio()) + "></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("<BR>\n");

		reply.add("<H1>Web-interface uthentication(s)</H1>\n");
		reply.add("<FONT SIZE=-1>Leave these fields empty to disable web-interface authentication.<BR>\n");
		reply.add("<TABLE>\n");
		reply.add("<TR><TD>Authentication timeout:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"web-expire-time\" VALUE=\"" + config.getWebSessionExpire() + "\"></TD><TD>In seconds, &gt; 1</TD></TR>\n");
		reply.add("<TR><TD>Username:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"web-username\" VALUE=\"" + (config.getWebUsername() != null ? config.getWebUsername() : "") + "\"></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>New password:</TD><TD><INPUT TYPE=\"PASSWORD\" NAME=\"web-password1\" VALUE=\"" + (config.getWebPassword() != null ? config.getWebPassword() : "") + "\"></TD><TD></TD></TR>\n");
		reply.add("<TR><TD>Confirm new password:</TD><TD><INPUT TYPE=\"PASSWORD\" NAME=\"web-password2\" VALUE=\"" + (config.getWebPassword() != null ? config.getWebPassword() : "") + "\"></TD><TD>Please repeat.</TD></TR>\n");
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

	public boolean getCheckBox(List<HTTPRequestData> requestData, String fieldName)
	{
		HTTPRequestData field = MyHTTPServer.findRecord(requestData, fieldName);
		if (field != null && field.getData() != null)
			return true;

		return false;
	}

	public String getField(List<HTTPRequestData> requestData, String fieldName)
	{
		HTTPRequestData field = MyHTTPServer.findRecord(requestData, fieldName);
		if (field != null && field.getData() != null)
			return field.getData().trim();

		return "";
	}

	public String getFieldDecoded(List<HTTPRequestData> requestData, String fieldName) throws Exception
	{
		String fieldData = getField(requestData, fieldName);
		if (fieldData == null)
			return null;

		return URLDecoder.decode(fieldData, defaultCharset);
	}

	public void sendReply_cgibin_configdo_cgi(MyHTTPServer socket, List<HTTPRequestData> requestData, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		configNotWrittenToDisk = true;

		addHTTP200(reply, cookie);
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

		HTTPRequestData minRowHeight = MyHTTPServer.findRecord(requestData, "min-row-height");
		if (minRowHeight != null && minRowHeight.getData() != null) {
			config.setMinRowHeight(Integer.valueOf(minRowHeight.getData()));
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

		HTTPRequestData headerTransparency = MyHTTPServer.findRecord(requestData, "header-transparency");
		if (headerTransparency != null && headerTransparency.getData() != null)
		{
			float newTransparency = Float.valueOf(headerTransparency.getData());
			if (newTransparency < 0.0 || newTransparency > 1.0)
				reply.add("Transparency (for header) must be between 0.0 and 1.0 (both inclusive)");
			else
				config.setHeaderTransparency(newTransparency);
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

		String maxAge = getField(requestData, "max-check-age");
		config.setMaxCheckAge(maxAge.equals("") ? -1 : Long.valueOf(maxAge));

		config.setFlexibleNColumns(getCheckBox(requestData, "flexible-n-columns"));

		config.setMaxQualityGraphics(getCheckBox(requestData, "max-quality-graphics"));

		config.setFontName(getFieldDecoded(requestData, "font"));

		config.setProxyHost(getFieldDecoded(requestData, "proxy-host"));
		config.setProxyPort(Integer.valueOf(getFieldDecoded(requestData, "proxy-port")));

		if (!config.getNoNetworkChange())
		{
			config.setHTTPServerListenAdapter(getFieldDecoded(requestData, "network-interface"));
			System.out.println("PORT: " + getFieldDecoded(requestData, "network-port") + "|");
			config.setHTTPServerListenPort(Integer.valueOf(getFieldDecoded(requestData, "network-port")));
		}

		config.setWarningFontName(getFieldDecoded(requestData, "warning-font"));

		config.setCriticalFontName(getFieldDecoded(requestData, "critical-font"));

		config.setTextColor(getField(requestData, "textColor"));
		config.setWarningTextColor(getField(requestData, "warningTextColor"));
		config.setCriticalTextColor(getField(requestData, "criticalTextColor"));
		// config.setHeaderColorName(getField(requestData, "header-color"));

		config.setBackgroundColor(getField(requestData, "backgroundColor"));
		String fadeTo = getField(requestData, "bgcolor-fade-to");
		if (fadeTo == null || fadeTo.equals("NULL") == true)
			config.setBackgroundColorFadeTo(null);
		else
			config.setBackgroundColorFadeTo(fadeTo);

		String problemBarFadeTo = getField(requestData, "problem-row-gradient");
		if (problemBarFadeTo == null || problemBarFadeTo.equals("NULL") == true)
			config.setProblemRowGradient(null);
		else
			config.setProblemRowGradient(problemBarFadeTo);

		config.setBackgroundColorOkStatus(getField(requestData, "bgColorOk"));
		config.setWarningBgColor(getField(requestData, "warning-bg-color"));
		config.setWarningBgColorSoft(getField(requestData, "warning-bg-color-soft"));
		config.setCriticalBgColor(getField(requestData, "critical-bg-color"));
		config.setCriticalBgColorSoft(getField(requestData, "critical-bg-color-soft"));
		config.setNagiosUnknownBgColor(getField(requestData, "unknown-bg-color"));
		config.setSetBgColorToState(getCheckBox(requestData, "color-bg-to-state"));

		String sleepTime = getField(requestData, "sleepTime");
		if (sleepTime != null && sleepTime.equals("") == false)
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

		String wcTo = getField(requestData, "webcam-timeout");
		int wcToVal = -1;
		if (wcTo == null || wcTo.trim().equals("") == true)
			wcToVal = -1;
		else
			wcToVal = Integer.valueOf(wcTo);
		if (wcToVal <= 0 || wcToVal > config.getSleepTime())
			wcToVal = -1;
		config.setWebcamTimeout(wcToVal);

		String webExpireTime = getField(requestData, "web-expire-time");
		if (webExpireTime != null && webExpireTime.equals("") == false)
		{
			int newWebExpireTime = Integer.valueOf(webExpireTime);
			if (newWebExpireTime < 1)
				reply.add("New authentication timeout is invalid, must be >= 1<BR>\n");
			else
				config.setWebSessionExpire(newWebExpireTime);
		}

		String sparkline_mode = getField(requestData, "sparkline-mode");
		if (sparkline_mode != null)
		{
			if (sparkline_mode.equals("avg-sd"))
				config.setSparklineGraphMode(SparklineGraphMode.AVG_SD);
			else if (sparkline_mode.equals("min-max"))
				config.setSparklineGraphMode(SparklineGraphMode.MIN_MAX);
		}

		String noProblemsText = getFieldDecoded(requestData, "no-problems-text");
		if (noProblemsText != null)
		{
			if (noProblemsText.trim().equals(""))
				config.setNoProblemsText(null);
			else
				config.setNoProblemsText(noProblemsText);
		}
		String noProblemsTextPosition = getField(requestData, "no-problems-text-position");
		if (noProblemsTextPosition != null && noProblemsTextPosition.equals("") == false)
			config.setNoProblemsTextPosition(noProblemsTextPosition);

		String stateProblemsText = getFieldDecoded(requestData, "state-problems-text");
		if (stateProblemsText != null)
		{
			if (stateProblemsText.trim().equals(""))
				config.setStateProblemsText(null);
			else
				config.setStateProblemsText(stateProblemsText);
		}

		String usernameField = getFieldDecoded(requestData, "web-username");
		String passwordField1 = getFieldDecoded(requestData, "web-password1");
		String passwordField2 = getFieldDecoded(requestData, "web-password2");
		if (usernameField != null && passwordField1 != null && passwordField2 != null)
		{
			if (passwordField1.equals(passwordField2))
			{
				usernameField = usernameField.trim();
				if (usernameField.equals(""))
					config.setWebUsername(null);
				else
					config.setWebUsername(usernameField);

				if (passwordField1.equals(""))
					config.setWebPassword(null);
				else
					config.setWebPassword(passwordField1);
			}
			else
			{
				reply.add("Passwords did not match, username+password <B>not</B> changed.<BR>\n");
			}
		}

		config.setAlwaysNotify(getCheckBox(requestData, "always_notify"));

		config.setHeaderAlwaysBGColor(getCheckBox(requestData, "header-always-bgcolor"));

		config.setAlsoAcknowledged(getCheckBox(requestData, "also_acknowledged"));

		config.setAlsoScheduledDowntime(getCheckBox(requestData, "also_scheduled_downtime"));

		config.setAlsoSoftState(getCheckBox(requestData, "also_soft_State"));

		config.setAlsoDisabledActiveChecks(getCheckBox(requestData, "also_disabled_active_checks"));

		config.setShowServicesForHostWithProblems(getCheckBox(requestData, "show_services_for_host_with_problems"));

		config.setHostSDOrAckShowServices(getCheckBox(requestData, "host_scheduled_downtime_or_ack_show_services"));

		config.setShowFlapping(getCheckBox(requestData, "show-flapping"));

		config.setShowFlappingIcon(getCheckBox(requestData, "show-flapping-icon"));

		config.setHostAcknowledgedShowServices(getCheckBox(requestData, "host_also_acknowledged"));
		config.setHostScheduledDowntimeShowServices(getCheckBox(requestData, "host_also_scheduled_downtime"));

		config.setCounter(getCheckBox(requestData, "counter"));

		String newFSMode = getField(requestData, "fullscreen");
		if (newFSMode != null)
		{
			if (newFSMode.equalsIgnoreCase("none"))
				config.setFullscreen(FullScreenMode.NONE);
			else if (newFSMode.equalsIgnoreCase("undecorated"))
				config.setFullscreen(FullScreenMode.UNDECORATED);
			else if (newFSMode.equalsIgnoreCase("fullscreen"))
				config.setFullscreen(FullScreenMode.FULLSCREEN);
			else if (newFSMode.equalsIgnoreCase("allmonitors"))
				config.setFullscreen(FullScreenMode.ALLMONITORS);
		}

		String useScreen = getFieldDecoded(requestData, "use-screen");
		if (useScreen == null || useScreen.equals("ALL"))
			config.setUseScreen(null);
		else
			config.setUseScreen(useScreen);

		String counterPosition = getField(requestData, "counter-position");
		if (counterPosition != null)
			config.setCounterPosition(counterPosition);

		String newLogo = getFieldDecoded(requestData, "logo-url");
		if (newLogo != null)
		{
			if (newLogo.trim().equals(""))
				config.setLogo(null);
			else
				config.setLogo(newLogo);
		}
		String logoPosition = getField(requestData, "logo-position");
		if (logoPosition != null && logoPosition.equals("") == false)
			config.setLogoPosition(logoPosition);

		if (config.getDisableHTTPFileselect() == false)
		{
			String brainFile = getField(requestData, "brain-file").trim();
			config.setBrainFileName(brainFile.equals("") ? null : brainFile);

			String performanceFile = getField(requestData, "performance-data").trim();
			config.setPerformanceDataFileName(performanceFile.equals("") ? null : performanceFile);

			String latencyFile = getField(requestData, "latency-file").trim();
			config.setLatencyFile(latencyFile.equals("") ? null : latencyFile);
		}

		String newWebcam = getFieldDecoded(requestData, "newWebcam");
		String newWebcamType = getFieldDecoded(requestData, "newWebcamType");
		if (newWebcam != null && newWebcam.equals("") == false && newWebcamType != null && newWebcamType.equals("") == false)
			config.addImageUrl(newWebcamType + " " + newWebcam);

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

		config.setProblemSound(getFieldDecoded(requestData, "warning-sound"));

		config.setAdaptImageSize(getCheckBox(requestData, "adapt-img"));

		config.setAllowHTTPCompression(getCheckBox(requestData, "disable-http-compression"));

		config.setAntiAlias(getCheckBox(requestData, "anti-alias"));

		config.setRandomWebcam(getCheckBox(requestData, "random-img"));

		config.setReduceTextWidth(getCheckBox(requestData, "reduce-textwidth"));

		config.setHeader(getFieldDecoded(requestData, "header"));

		String newFooter = getFieldDecoded(requestData, "footer");
		if (newFooter.trim().equals(""))
			config.setFooter(null);
		else
			config.setFooter(newFooter);

		config.setPrefers(getFieldDecoded(requestData, "prefer"));
		config.setHostsFilterExclude(getFieldDecoded(requestData, "hosts-filter-exclude-list"));
		config.setHostsFilterInclude(getFieldDecoded(requestData, "hosts-filter-include-list"));
		config.setServicesFilterExclude(getFieldDecoded(requestData, "services-filter-exclude-list"));
		config.setServicesFilterInclude(getFieldDecoded(requestData, "services-filter-include-list"));

		config.setScrollingHeader(getCheckBox(requestData, "scrolling-header"));
		config.setScrollingFooter(getCheckBox(requestData, "scrolling-footer"));
		config.setScrollIfNotFit(getCheckBox(requestData, "scroll-if-not-fitting"));

		String splitter = getFieldDecoded(requestData, "scroll-splitter");
		if (splitter != null)
		{
			splitter = splitter.trim();

			if (splitter.equals(""))
				config.setLineScrollSplitter(null);
			else
				config.setLineScrollSplitter(splitter);
		}

		config.setDrawProblemServiceSplitLine(getCheckBox(requestData, "draw-problems-service-split-line"));

		config.setNoProblemsTextBg(getCheckBox(requestData, "no-problems-text-with-bg-color"));

		String scrollSpeed = getField(requestData, "scroll-pixels-per-sec");
		if (scrollSpeed != null && scrollSpeed.trim().equals("") == false)
		{
			int newScrollSpeed = Integer.valueOf(scrollSpeed);
			if (newScrollSpeed < 1)
				reply.add("New pixels/sec-value is invalid, must be >= 1<BR>\n");
			else
				config.setScrollingPixelsPerSecond(newScrollSpeed);
		}

		config.setHostIssue(getFieldDecoded(requestData, "host-issue"));

		config.setServiceIssue(getFieldDecoded(requestData, "service-issue"));

		config.setShowHeader(getCheckBox(requestData, "show-header"));

		config.setFlash(getCheckBox(requestData, "flash"));

		boolean son = getCheckBox(requestData, "sort-order-numeric");
		boolean sor = getCheckBox(requestData, "sort-order-reverse");
		config.setSortOrder(getFieldDecoded(requestData, "sort-order"), son, sor);

		config.setVerbose(getCheckBox(requestData, "verbose"));

		config.setDisplayUnknown(getCheckBox(requestData, "display-unknown"));

		config.setDoubleBuffering(getCheckBox(requestData, "double-buffering"));

		config.setRowBorder(getCheckBox(requestData, "row-border"));

		String rowBorderHeight = getField(requestData, "upper-row-border-height");
		if (rowBorderHeight != null && rowBorderHeight.trim().equals("") == false)
		{
			int newRowBorderHeight = Integer.valueOf(rowBorderHeight);
			if (newRowBorderHeight < 1 || newRowBorderHeight > 400)
				reply.add("Invalid upper row border height. Must be &gt;= 1.<BR>\n");
			else
				config.setUpperRowBorderHeight(newRowBorderHeight);
		}

		config.setRowBorderColor(getField(requestData, "row-border-color"));
		config.setGraphColor(getField(requestData, "graph-color"));

		// add server
		String server_add_parameters = getFieldDecoded(requestData, "server-add-parameters");
		if (server_add_parameters.equals("") == false)
		{
			NagiosDataSourceType ndst = null;
			NagiosVersion nv = null;

			String type = getField(requestData, "server-add-type");
			if (type.equals("tcp"))
				ndst = NagiosDataSourceType.TCP;
			else if (type.equals("ztcp"))
				ndst = NagiosDataSourceType.ZTCP;
			else if (type.equals("ls"))
				ndst = NagiosDataSourceType.LS;
			else if (type.equals("http"))
				ndst = NagiosDataSourceType.HTTP;
			else if (type.equals("file"))
				ndst = NagiosDataSourceType.FILE;

			String pn = getField(requestData, "nagios-pretty-name");
			if (pn.equals(""))
				pn = null;

			String version = getField(requestData, "server-add-version");
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

					String result = CoffeeSaint.testPort(server_add_parameters, port);
					if (result != null)
						reply.add("Error: address " + server_add_parameters + ":" + port + " has an issue (" + result + "). Nagios server was <I>not</I> added.<BR>\n");
					else
						config.addNagiosDataSource(new NagiosDataSource(server_add_parameters, port, nv, ndst == NagiosDataSourceType.ZTCP, pn));
				}
				else if (ndst == NagiosDataSourceType.LS)
				{
					int port = 6557;
					int space = server_add_parameters.indexOf(" ");
					if (space != -1)
					{
						port = Integer.valueOf(server_add_parameters.substring(space + 1).trim());
						server_add_parameters = server_add_parameters.substring(0, space);
					}

					String result = CoffeeSaint.testPort(server_add_parameters, port);
					if (result != null)
						reply.add("Error: address " + server_add_parameters + ":" + port + " has an issue (" + result + "). Nagios server was <I>not</I> added.<BR>\n");
					else
						config.addNagiosDataSource(new NagiosDataSource(server_add_parameters, port, pn));
				}
				else if (ndst == NagiosDataSourceType.HTTP)
				{
					try
					{
						URL url = new URL(URLDecoder.decode(server_add_parameters, "US-ASCII"));
						String username = getFieldDecoded(requestData, "nagios-http-username");
						String password = getFieldDecoded(requestData, "nagios-http-password");
						boolean withAuth = username != null && password != null && username.equals("") == false && password.equals("") == false;
						if (withAuth)
							config.addNagiosDataSource(new NagiosDataSource(url, username, password, nv, pn));
						else
							config.addNagiosDataSource(new NagiosDataSource(url, nv, pn));

						String result = CoffeeSaint.testUrl(url, withAuth);
						if (result != null)
							reply.add("Warning: URL " + server_add_parameters + " seems to be unreachable! (" + result + ")<BR>\n");
					}
					catch(MalformedURLException mue)
					{
						reply.add("Error: URL " + server_add_parameters + " is not valid. Nagios-server was <I>not</> added.<BR>\n");
					}
				}
				else if (ndst == NagiosDataSourceType.FILE)
					config.addNagiosDataSource(new NagiosDataSource(server_add_parameters, nv, pn));
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

		String cam_rows = getField(requestData, "cam-rows");
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

		String cam_cols = getField(requestData, "cam-cols");
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

		config.setKeepAspectRatio(getCheckBox(requestData, "keep-aspect-ratio"));

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

	public void sendReply_applet_html(MyHTTPServer socket) throws Exception {
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, null);
		reply.add("<HTML>\n");
		reply.add("<BODY>\n");
		reply.add("<CENTER>\n");
		reply.add("<TABLE BORDER=1><TR><TD>\n");

		reply.add("<applet archive=\"/CoffeeSaint.jar\" code=Applet width=\"1024\" height=\"768\">");
		for(String [] entry : config.collectConfig()) {
			reply.add("<PARAM NAME=\"" + entry[0] + "\" VALUE=\"" + entry[1] + "\">\n");
		}
		reply.add("</applet>\n");
		reply.add("</TD></TR></TABLE>\n");
		reply.add("</CENTER>\n");
		reply.add("</BODY>\n");
		reply.add("</HTML>\n");

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_listlog_cgi(MyHTTPServer socket, String cookie) throws Exception {
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
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

	public void sendReply_cgibin_listall_cgi(MyHTTPServer socket, List<HTTPRequestData> getData, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
		addPageHeader(reply, "");

		Object [] result = CoffeeSaint.loadNagiosData(null, -1, null);
		JavNag javNag = (JavNag)result[0];
		if (result[1] != null)
			reply.add("<B>Problem load: " + (String)result[1] + "</B><BR>");
                List<Problem> problems = CoffeeSaint.findProblems(javNag);
                coffeeSaint.learnProblemCount(problems.size());
		coffeeSaint.collectPerformanceData(javNag);
		coffeeSaint.collectLatencyData(javNag);

		reply.add("<TABLE>\n");
		reply.add("<TR><TD><B>Host</B></TD><TD><B>host status</B></TD><TD><B>Service</B></TD><TD><B>service status</B></TD></TR>\n");

		List<Host> hosts = javNag.getListOfHosts();
		int nPages = (hosts.size() + maxNHostsPerPage - 1) / maxNHostsPerPage;

		int page = 1;
		if (getData != null)
		{
			HTTPRequestData pageRecord = MyHTTPServer.findRecord(getData, "page");
			if (pageRecord != null)
			{
				page = Integer.valueOf(pageRecord.getData().trim());
			}
			if (page < 1)
				page = 1;
		}
		page--;
		if (page >= nPages)
			page = nPages;

		if (nPages > 1)
		{
			reply.add("Page: ");
			for(int pageNr=1; pageNr<=nPages; pageNr++)
			{
				if (pageNr == (page + 1))
					reply.add("" + pageNr + "&nbsp;");
				else
					reply.add("<A HREF=\"/cgi-bin/list-all.cgi?page=" + pageNr + "\">" + pageNr + "</A>&nbsp;");
			}
			reply.add("<BR>\n");
		}

		int pageOffset = page * maxNHostsPerPage;
		for(int index=pageOffset; index<Math.min(pageOffset + maxNHostsPerPage, hosts.size()); index++)
		{
			Host currentHost = hosts.get(index);

			String hostState = currentHost.getParameter("state_type").equals("1") ? currentHost.getParameter("current_state") : "0";
			String htmlHostStateColor = htmlColorString(coffeeSaint.stateToColor(hostState.equals("1") ? "2" : hostState, true));

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
				String htmlServiceStateColor = htmlColorString(coffeeSaint.stateToColor(serviceState, true));

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

	public void sendReply_root(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
		addPageHeader(reply, "");
		reply.add("<br /><br /><br />Please select an action in the menu at the left.");
		addPageTail(reply, false);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_forcereload_cgi(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		if (config.getRunGui())
			gui.paint(gui.getGraphics());

		addHTTP200(reply, cookie);
		addPageHeader(reply, "<meta http-equiv=\"refresh\" content=\"5;url=/\">");
		reply.add("Nagios status reloaded.");
		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_latencygraph_html(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
		addPageHeader(reply, "");

		reply.add("<H1>Latency graph</H1>\n");
		DataInfo dataInfo = coffeeSaint.getLatencyStats();
		if (dataInfo != null)
		{
			reply.add("<IMG SRC=\"/cgi-bin/latency-graph.cgi\" BORDER=\"1\"><BR>\n");
			reply.add("<BR>\n");
			reply.add("<TABLE>\n");
			reply.add("<TR><TD>Minimum:</TD><TD>" + String.format("%.4f", dataInfo.getMin()) + "s</TD></TR>\n");
			reply.add("<TR><TD>Maximum:</TD><TD>" + String.format("%.4f", dataInfo.getMax()) + "s</TD></TR>\n");
			reply.add("<TR><TD>Average:</TD><TD>" + String.format("%.4f", dataInfo.getAvg()) + "s</TD></TR>\n");
			reply.add("<TR><TD>Standard deviation:</TD><TD>" + String.format("%.4f", dataInfo.getSd()) + "s</TD></TR>\n");
			reply.add("</TABLE>\n");
		}
		else
		{
			reply.add("No values measured yet.<BR>\n");
		}

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_links_html(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
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

	public void sendReply_cgibin_statistics_cgi(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
		addPageHeader(reply, "");

		reply.add("<TABLE>\n");
		int nRefreshes = statistics.getNRefreshes();
		reply.add("<TR><TD>Total number of refreshes:</TD><TD>" + nRefreshes + "</TD></TR>\n");
		reply.add("<TR><TD>Total refresh time:</TD><TD>" + statistics.getTotalRefreshTime() + "</TD></TR>\n");
		reply.add("<TR><TD>Average refresh time:</TD><TD>" + String.format("%.4f", statistics.getTotalRefreshTime() / nRefreshes) + "</TD></TR>\n");
		reply.add("<TR><TD>Total image refresh time:</TD><TD>" + statistics.getTotalImageLoadTime() + "</TD></TR>\n");
		reply.add("<TR><TD>Average image refresh time:</TD><TD>" + String.format("%.4f", statistics.getTotalImageLoadTime() / nRefreshes) + "</TD></TR>\n");
		reply.add("<TR><TD>Total running time:</TD><TD>" + ((System.currentTimeMillis() - statistics.getRunningSince()) / 1000.0) + "s</TD></TR>\n");
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

	public void sendReply_cgibin_nagiosstatus_cgi(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
		reply.add("<HTML><!-- " + CoffeeSaint.getVersion() + "--><HEAD><meta http-equiv=\"refresh\" content=\"" + config.getSleepTime() + "\"></HEAD><BODY>");
		reply.add("<FONT SIZE=-1>Generated by: " + CoffeeSaint.getVersion() + "</FONT><BR><BR>");

		Calendar rightNow = Calendar.getInstance();

		Object [] result = CoffeeSaint.loadNagiosData(null, -1, null);
		JavNag javNag = (JavNag)result[0];
		if (result[1] != null)
			reply.add("<B>Problem load: " + (String)result[1] + "</B><BR>");
		List<Problem> problems = CoffeeSaint.findProblems(javNag);
		coffeeSaint.learnProblemCount(problems.size());
		coffeeSaint.collectPerformanceData(javNag);
		coffeeSaint.collectLatencyData(javNag);

		if (config.getShowHeader())
		{
			String header = coffeeSaint.getScreenHeader(javNag, rightNow, problems.size() > 0);
			reply.add(header + "<BR>");
		}

		Color bgColor;
		if (problems.size() > 0)
			bgColor = config.getBackgroundColor();
		else
			bgColor = coffeeSaint.predictWithColor(rightNow);

		long maxAge = config.getMaxCheckAge();
		if (maxAge != -1) {
			long currentAge = javNag.findMostRecentCheckAge();
			if (currentAge > maxAge)
				reply.add("<FONT SIZE=+2>WARNING: Nagios has not done anything for the last " + currentAge + " seconds!</FONT><BR><BR>\n");
		}

		reply.add("<TABLE WIDTH=640 HEIGHT=400 TEXT=\"#" + htmlColorString(config.getTextColor()) + "\" BGCOLOR=\"#" + htmlColorString(bgColor) + "\">\n");

		for(Problem currentProblem : problems)
		{
			String stateColor = htmlColorString(coffeeSaint.stateToColor(currentProblem.getCurrent_state(), currentProblem.getHard()));

			String escapeString;
			if (currentProblem.getService() == null)
				escapeString = config.getHostIssue();
			else
				escapeString = config.getServiceIssue();
			Object [] dummy = coffeeSaint.processStringWithEscapes(escapeString, javNag, rightNow, currentProblem, true, true);
			String output = (String)dummy[0];

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

	public void sendReply_cgibin_reloadconfig_cgi(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
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

	public void sendReply_cgibin_testsound_cgi(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
		addPageHeader(reply, "");

		String sample = config.getProblemSound();
		new PlayWav(sample);

		reply.add("Played audio-file " + sample);

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_writeconfig_cgi(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		configNotWrittenToDisk = false;
		addHTTP200(reply, cookie);
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

	public void sendReply_cgibin_log_cgi(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
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

	public void sendReply_cgibin_performancedata_cgi(MyHTTPServer socket, List<HTTPRequestData> getData, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
		addPageHeader(reply, "");

		Object [] result = CoffeeSaint.loadNagiosData(null, -1, null);
		JavNag javNag = (JavNag)result[0];
		if (result[1] != null)
			reply.add("<B>Problem load: " + (String)result[1] + "</B><BR>");
		List<Problem> problems = CoffeeSaint.findProblems(javNag);
		coffeeSaint.learnProblemCount(problems.size());
		coffeeSaint.collectPerformanceData(javNag);
		coffeeSaint.collectLatencyData(javNag);

		List<Host> hosts = javNag.getListOfHosts();
		int nPages = (hosts.size() + maxNHostsPerPage - 1) / maxNHostsPerPage;

		int page = 1;
		if (getData != null)
		{
			HTTPRequestData pageRecord = MyHTTPServer.findRecord(getData, "page");
			if (pageRecord != null)
			{
				page = Integer.valueOf(pageRecord.getData().trim());
			}
			if (page < 1)
				page = 1;
		}
		page--;
		if (page >= nPages)
			page = nPages;

		reply.add("<H1>Performance data</H1>\n");
		if (nPages > 1)
		{
			reply.add("Page: ");
			for(int pageNr=1; pageNr<=nPages; pageNr++)
			{
				if (pageNr == (page + 1))
					reply.add("" + pageNr + "&nbsp;");
				else
					reply.add("<A HREF=\"/cgi-bin/performance-data.cgi?page=" + pageNr + "\">" + pageNr + "</A>&nbsp;");
			}
			reply.add("<BR>\n");
		}
		reply.add("<TABLE WIDTH=\"100%\">\n");
		reply.add("<TR><TD><B>host</B></TD><TD><B>service</B></TD><TD><B>parameter</B></TD><TD><B>min</B></TD><TD><B>max</B></TD><TD><B>avg</B></TD><TD><B>std.dev.</B></TD><TD><B>samples</B></TD><TD><B>sparkline</B></TD></TR>\n");
		int pageOffset = page * maxNHostsPerPage;
		for(int index=pageOffset; index<Math.min(pageOffset + maxNHostsPerPage, hosts.size()); index++)
		{
			Host currentHost = hosts.get(index);

			List<DataSource> dataSources = coffeeSaint.getPerformanceData(currentHost.getHostName(), null);
			if (dataSources != null)
			{
				for(DataSource dataSource : dataSources)
				{
					DataInfo dataInfo = dataSource.getStats();

					String host = abreviateString(currentHost.getHostName(), 16);

					if (dataInfo != null)
					{
						String sparkCol = "<TD></TD>";
						if (coffeeSaint.havePerformanceData(currentHost.getHostName(), null))
						{
							String url = graphZoomInUrl(currentHost.getHostName(), null, dataSource.getDataSourceName());
							host = "<A HREF=\"" + url + "\">" + abreviateString(currentHost.getHostName(), 16) + "</A>";
							url = sparkLineUrl(currentHost.getHostName(), null, dataSource.getDataSourceName(), 100, 15, false);
							sparkCol = "<TD><IMG SRC=\"" + url + "\" BORDER=0></TD>";
						}

						String unit = dataSource.getUnit();
						reply.add("<TR><TD>" + host + "</TD><TD></TD><TD>" + abreviateString(dataSource.getDataSourceName(), 17) + "</TD><TD>" + formatValue(dataInfo.getMin()) + unit + "</TD><TD>" + formatValue(dataInfo.getMax()) + unit + "</TD><TD>" + formatValue(dataInfo.getAvg()) + unit + "</TD><TD>" + formatValue(dataInfo.getSd()) + unit + "</TD><TD>" + dataInfo.getN() + "</TD>" + sparkCol + "</TR>\n");
					}
					else
					{
						reply.add("<TR><TD>" + host + "</TD><TD></TD><TD>" + abreviateString(dataSource.getDataSourceName(), 17) + "</TD><TD COLSPAN=\"6\">No data measured yet.</TD></TR>\n");
					}
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

						String service = abreviateString(currentService.getServiceName(), 20);

						if (dataInfo != null)
						{
							String sparkCol = "<TD></TD>";
							if (coffeeSaint.havePerformanceData(currentHost.getHostName(), currentService.getServiceName()))
							{
								String url = graphZoomInUrl(currentHost.getHostName(), currentService.getServiceName(), dataSource.getDataSourceName());
								service = "<A HREF=\"" + url + "\">" + abreviateString(currentService.getServiceName(), 20) + "</A>";
								url = sparkLineUrl(currentHost.getHostName(), currentService.getServiceName(), dataSource.getDataSourceName(), 100, 15, false);
								sparkCol = "<TD><IMG SRC=\"" + url + "\" BORDER=0></TD>";
							}

							String unit = dataSource.getUnit();
							reply.add("<TR><TD>" + abreviateString(currentHost.getHostName(), 16) + "</TD><TD>" + service + "</TD><TD>" + abreviateString(dataSource.getDataSourceName(), 17) + "</TD><TD>" + formatValue(dataInfo.getMin()) + unit + "</TD><TD>" + formatValue(dataInfo.getMax()) + unit + "</TD><TD>" + formatValue(dataInfo.getAvg()) + unit + "</TD><TD>" + formatValue(dataInfo.getSd()) + unit + "</TD><TD>" + dataInfo.getN() + "</TD>" + sparkCol + "</TR>\n");
						}
						else
						{
							reply.add("<TR><TD>" + abreviateString(currentHost.getHostName(), 16) + "</TD><TD>" + service + "</TD><TD>" + abreviateString(dataSource.getDataSourceName(), 17) + "</TD><TD COLSPAN=\"6\">No data measured yet.</TD></TR>\n");
						}
					}
				}
			}
		}
		reply.add("</TABLE>\n");

		addPageTail(reply, true);

		socket.sendReply(reply);
	}

	public void sendReply_helpescapes_html(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
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

	public void sendReply_loginhtml(MyHTTPServer socket, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
		addPageHeader(reply, "");

		reply.add("<H1>Please login</H1>\n");
		reply.add("<FORM ACTION=\"/cgi-bin/login-do.cgi\" METHOD=\"POST\">\n");
		reply.add("<TABLE>\n");
		reply.add("<TR><TD>Username:</TD><TD><INPUT TYPE=\"TEXT\" NAME=\"username\"></TD></TR>\n");
		reply.add("<TR><TD>Password:</TD><TD><INPUT TYPE=\"PASSWORD\" NAME=\"password\"></TD></TR>\n");
		reply.add("<TR><TD></TD><TD><INPUT TYPE=\"SUBMIT\"></TD></TR>\n");
		reply.add("</TABLE>\n");
		reply.add("</FORM>\n");

		addPageTail(reply, false);

		socket.sendReply(reply);
	}

	public void sendReply_cgibin_logindocgi(MyHTTPServer socket, List<HTTPRequestData> requestData, String host, String cookie) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		addHTTP200(reply, cookie);
		addPageHeader(reply, "");

		String username = getField(requestData, "username");
		String password = getField(requestData, "password");

		boolean valid = true;
		if (username == null || password == null)
			valid = false;
		else if (config.getLDAPUrl() != null) {
			if (LDAP.authenticateUser(config.getLDAPBaseDN(), username, password, config.getLDAPUrl()) == false)
				valid = false;
		}
		else {
			if (username.equals(config.getWebUsername()) == false)
				valid = false;
			if (password.equals(config.getWebPassword()) == false)
				valid = false;
		}

		reply.add("<H1>Login</H1>\n");
		if (valid)
		{
			reply.add("Login success!\n");
			CoffeeSaint.log.add("Session with " + host + " started");
			sessions.add(new HTTPSession(host, cookie));
		}
		else
		{
			reply.add("Login <B>failed</B>.<BR>\n");
			reply.add("<BR>\n");
			reply.add("<A HREF=\"/login.html\">Retry</A><BR>\n");
		}

		addPageTail(reply, false);

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

	public void sendReply_redirectTo(MyHTTPServer socket, String url) throws Exception
	{
		List<String> reply = new ArrayList<String>();

		reply.add("HTTP/1.1 302\r\n");
		reply.add("Location: " + url + "\r\n");
		reply.add("Connection: close\r\n");
		reply.add("Content-Type: text/html\r\n");
		reply.add("\r\n");

		socket.sendReply(reply);
	}

	public void logEntry(InetSocketAddress remoteAddress, String requestType, String url)
	{
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
	}

	public String mySubstring(String in, int start, int end)
	{
		end = Math.min(end, in.length());

		return in.substring(start, end);
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
					System.out.println(" + request start");
					String requestType = request.get(0).getName();
					String url = request.get(0).getData().trim();
					int space = url.indexOf(" ");
					if (space != -1)
						url = url.substring(0, space);

					expireSessions(sessions);

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
					logEntry(remoteAddress, requestType, url);

					String authCookie = null;
					HTTPRequestData cookieRD = MyHTTPServer.findRecord(request, "Cookie:");
					if (cookieRD != null)
					{
						// System.out.println("Cookie string: " + cookieRD.getData());
						authCookie = "auth=" + MyHTTPServer.getCookieData(cookieRD.getData(), "auth");
					}
					if (authCookie == null)
						authCookie = "auth=" + Math.abs(new Random(System.currentTimeMillis()).nextInt());

					if (!sessionValid(sessions, remoteAddress.getAddress().toString(), authCookie) && config.getAuthentication() == true && config.getWebUsername() != null && config.getWebPassword() != null && url.equals("/login.html") == false && url.equals("/cgi-bin/login-do.cgi") == false && mySubstring(url, 0, 8).equals("/images/") == false && url.equals("/design.css") == false)
					{
						sendReply_redirectTo(socket, "/login.html");
						continue;
					}

					boolean isHeadRequest = false;
					if (requestType.equals("HEAD"))
						isHeadRequest = true;

					if (url.charAt(0) != '/')
						url = "/" + url;

					if (url.equals("/") || url.equals("/index.html"))
						sendReply_root(socket, authCookie);
					else if (url.equals("/login.html"))
						sendReply_loginhtml(socket, authCookie);
					else if (url.equals("/cgi-bin/login-do.cgi"))
					{
						List<HTTPRequestData> requestData = socket.getRequestData(request);
						sendReply_cgibin_logindocgi(socket, requestData, remoteAddress.getAddress().toString(), authCookie);
					}
					else if (url.equals("/cgi-bin/force_reload.cgi"))
						sendReply_cgibin_forcereload_cgi(socket, authCookie);
					else if (url.equals("/cgi-bin/statistics.cgi"))
						sendReply_cgibin_statistics_cgi(socket, authCookie);
					else if (url.equals("/cgi-bin/nagios_status.cgi"))
						sendReply_cgibin_nagiosstatus_cgi(socket, authCookie);
					else if (url.equals("/image.jpg"))
						sendReply_imagejpg(socket, authCookie);
					else if (url.equals("/cgi-bin/config-menu.cgi"))
						sendReply_cgibin_configmenu_cgi(socket, authCookie);
					else if (url.equals("/cgi-bin/select_configfile.cgi"))
						sendReply_cgibin_select_configfile_cgi(socket, authCookie);
					else if (url.equals("/cgi-bin/jvm_stats.cgi"))
						sendReply_cgibin_system_info(socket, authCookie);
					else if (url.equals("/cgi-bin/select_configfile-do.cgi"))
					{
						List<HTTPRequestData> requestData = socket.getRequestData(request);
						sendReply_cgibin_select_configfile_do_cgi(socket, requestData, authCookie);
					}
					else if (url.equals("/cgi-bin/config-do.cgi"))
					{
						List<HTTPRequestData> requestData = socket.getRequestData(request);
						sendReply_cgibin_configdo_cgi(socket, requestData, authCookie);
						socket.closeServer();
						socket = null;
					}
					else if (url.equals("/cgi-bin/reload-config.cgi"))
						sendReply_cgibin_reloadconfig_cgi(socket, authCookie);
					else if (url.equals("/cgi-bin/write-config.cgi"))
						sendReply_cgibin_writeconfig_cgi(socket, authCookie);
					else if (url.equals("/cgi-bin/test-sound.cgi"))
						sendReply_cgibin_testsound_cgi(socket, authCookie);
					else if (url.equals("/cgi-bin/log.cgi"))
						sendReply_cgibin_log_cgi(socket, authCookie);
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
						sendReply_links_html(socket, authCookie);
					else if (url.equals("/help-escapes.html"))
						sendReply_helpescapes_html(socket, authCookie);
					else if (url.equals("/cgi-bin/list-all.cgi"))
						sendReply_cgibin_listall_cgi(socket, getData, authCookie);
					else if (url.equals("/cgi-bin/list-log.cgi"))
						sendReply_cgibin_listlog_cgi(socket, authCookie);
					else if (url.equals("/cgi-bin/performance-data.cgi"))
						sendReply_cgibin_performancedata_cgi(socket, getData, authCookie);
					else if (url.equals("/design.css"))
						sendReply_design_css(socket, isHeadRequest);
					else if (url.equals("/cgi-bin/sparkline.cgi"))
						sendReply_cgibin_sparkline_cgi(socket, getData, authCookie);
					else if (url.equals("/cgi-bin/graph-zoomin.cgi"))
						sendReply_cgibin_zoomin_cgi(socket, getData, authCookie);
					else if (url.equals("/cgi-bin/latency-graph.cgi"))
						sendReply_cgibin_latency_cgi(socket, getData, authCookie);
					else if (url.equals("/latency-graph.html"))
						sendReply_latencygraph_html(socket, authCookie);
					else if (url.equals("/CoffeeSaint.jar"))
						sendReply_CoffeeSaint_jar(socket);
					else if (url.equals("/applet.html"))
						sendReply_applet_html(socket);
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
				System.out.println(" + request end");
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
