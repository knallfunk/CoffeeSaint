/* Released under GPL2, (C) 2009-2011 by folkert@vanheusden.com */
import com.vanheusden.nagios.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class Config
{
	// parameters set/get by this object
	private java.util.List<NagiosDataSource> ndsList = new ArrayList<NagiosDataSource>();
	private int nRows;
	private int sleepTime;
	private String fontName, criticalFontName, warningFontName;
	private String listenAdapter = "0.0.0.0";
	private int listenPort = -1;
	private java.util.List<Pattern> prioPatterns;
	private String prioPatternsList;
	private java.util.List<Pattern> hostsFilterExclude;
	private java.util.List<Pattern> hostsFilterInclude;
	private java.util.List<Pattern> servicesFilterExclude;
	private java.util.List<Pattern> servicesFilterInclude;
	private String hostsFilterExcludeList;
	private String hostsFilterIncludeList;
	private String servicesFilterExcludeList;
	private String servicesFilterIncludeList;
	private String prefersFilename;
	private boolean always_notify, also_acknowledged;
	private Color backgroundColor;
	private String backgroundColorName;
	private Color textColor, warningTextColor, criticalTextColor;
	private String textColorName, warningTextColorName, criticalTextColorName;
	private Color warningBgColor, warningBgColorSoft, criticalBgColor, criticalBgColorSoft, nagiosUnknownBgColor;
	private String warningBgColorName, warningBgColorNameSoft, criticalBgColorName, criticalBgColorNameSoft, nagiosUnknownBgColorName;
	private boolean bgColorToState;
	private String bgColorOkStatusName;
	private Color bgColorOkStatus;
	private String problemSound = null;
	private boolean counter;
	private java.util.List<String> imageFiles = new ArrayList<String>();
	private java.util.List<ImageUrlType> iut = new ArrayList<ImageUrlType>();
	private boolean adaptImgSize;
	private String execCmd;
	private String predictorBrainFileName;
	private String performanceDataFileName;
	private boolean gui = true;
	private boolean randomWebcam;
	private String header;
	private boolean headerSet = false;
	private String issueHost, issueService;
	private boolean showHeader;
	private int httpRememberNHosts;
	private boolean sortNumeric, sortReverse;
	private String sortOrder;
	private int camRows, camCols;
	private boolean verbose;
	private FullScreenMode fullscreen;
	private boolean keepAspectRatio;
	private boolean scrollingHeader;
	private boolean scrollingFooter;
	private int scrollingPixelsPerSecond;
	private boolean reduceTextWidth;
	private boolean rowBorder;
	private Color rowBorderColor;
	private String rowBorderColorName;
	private boolean antiAlias;
	private boolean alsoScheduledDowntime;
	private boolean alsoSoftState;
	private boolean alsoDisabledActiveChecks;
	private boolean showProblemHostServices;
	private int problemCols;
	private boolean flexibleNColumns;
	private boolean maxQualityGraphics;
	private boolean allowCompression;
	private float transparency, headerTransparency;
	private boolean disableHTTPFileselect = false;
	private boolean showFlapping;
	private int sparkLineWidth;
	private SparklineGraphMode sparklineGraphMode;
	private boolean noNetworkChange = false;
	private Color graphColor;
	private String graphColorName;
	private boolean scrollIfNotFit;
	private Position counterPosition;
	private List<Integer> lineScrollSplitter;
	private String lineScrollSplitterString;
	private String noProblemsText;
	private Position noProblemsTextPosition;
	private boolean authenticate;
	private int webExpireTime;
	private String webUsername, webPassword;
	private String latencyFile;
	private String logo;
	private Integer putSplitAtOffset;
	private String problemStateString;
	private boolean headerAlwaysBGColor;
	private Position logoPosition;
	private int upperRowBorderHeight;
	private Color bgColorFadeTo;
	private String bgColorFadeToName;
	private Color problemRowGradient;
	private String problemRowGradientName;
	private boolean drawProblemServiceSplitLine;
	private boolean allowAllSSL;
	private String useScreen;
	private String footer;
	private boolean noProbTextBg;
	private boolean host_scheduled_downtime_show_services, host_acknowledged_show_services;
	private boolean host_scheduled_downtime_or_ack_show_services;
	private long maxCheckAge;
	private boolean showFlappingIcon;
	private boolean doubleBuffering;
	private boolean displayUnknown;
	private boolean displayDown;
	private int webcamTimeout;
	private boolean flash;
	private int minRowHeight;
	private String ldapBaseDn = null, ldapUrl = null;
	// global lock shielding all parameters
	private Semaphore configSemaphore = new Semaphore(1);
	//
	String configFilename;
	//
	private List<ColorPair> colorPairs;
	private List<String> sortFields;

	public List<String> getSortFields()
	{
		return sortFields;
	}

	private void lock()
	{
		configSemaphore.acquireUninterruptibly();
	}

	private void unlock()
	{
		configSemaphore.release();
	}

	public void setDefaultParameterValues()
	{
		lock();

		nRows = 10;
		sleepTime = 30;
		fontName = "Arial";
		criticalFontName = "Arial";
		warningFontName = "Arial";
		always_notify = false;
		also_acknowledged = false;
		backgroundColor = Color.GRAY;
		backgroundColorName = "gray";
		textColor = Color.BLACK;
		textColorName = "black";
		warningTextColor = Color.BLACK;
		warningTextColorName = "black";
		criticalTextColor = Color.BLACK;
		criticalTextColorName = "black";
		counter = false;
		adaptImgSize = false;
		randomWebcam = false;
		bgColorOkStatus = Color.GREEN;
		bgColorOkStatusName = "GREEN";
		header = "Cr %CRITICAL, Wa %WARNING, Un %UNREACHABLE, Dn %DOWN, %H:%M";
		headerSet = false;
		issueHost = "%HOSTNAME";
		issueService = "%HOSTNAME: %SERVICENAME";
		showHeader = true;
		httpRememberNHosts = 10;
		sortNumeric = true;
		sortReverse = false;
		sortOrder = "last_state_change";
		camRows = 1;
		camCols = 1;
		verbose = false;
		keepAspectRatio = true;
		scrollingHeader = false;
		scrollingPixelsPerSecond = 100;
		reduceTextWidth = false;
		rowBorder = false;
		rowBorderColor = Color.BLACK;
		rowBorderColorName = "black";
		antiAlias = false;
		alsoScheduledDowntime = false;
		alsoSoftState = false;
		alsoDisabledActiveChecks = false;
		showProblemHostServices = false;
		problemCols = 1;
		flexibleNColumns = false;
		warningBgColor = Color.YELLOW;
		warningBgColorName = "yellow";
		warningBgColorNameSoft = "lightyellow";
		warningBgColorSoft = selectColor(warningBgColorNameSoft);
		criticalBgColor = Color.RED;
		criticalBgColorName = "red";
		criticalBgColorNameSoft = "lightred";
		criticalBgColorSoft = selectColor(criticalBgColorNameSoft);
		nagiosUnknownBgColor = Color.MAGENTA;
		nagiosUnknownBgColorName = "magenta";
		allowCompression = true;
		transparency = 1.0f;
		headerTransparency = 1.0f;
		showFlapping = true;
		sparkLineWidth = 0;
		sparklineGraphMode = SparklineGraphMode.AVG_SD;
		graphColor = new Color(0x59432E);
		graphColorName = "CoffeeSaint";
		scrollIfNotFit = false;
		counterPosition = Position.LOWER_RIGHT;
		lineScrollSplitter = new ArrayList<Integer>();
		noProblemsText = null;
		noProblemsTextPosition = Position.CENTER;
		authenticate = true;
		webExpireTime = 30 * 60;
		webUsername = null;
		webPassword = null;
		latencyFile = null;
		logo = null;
		logoPosition = Position.RIGHT;
		putSplitAtOffset = null;
		problemStateString = "Current number of problems: %TOTALISSUES";
		headerAlwaysBGColor = false;
		bgColorToState = false;
		upperRowBorderHeight = 1;
		bgColorFadeTo = null;
		bgColorFadeToName = null;
		drawProblemServiceSplitLine = false;
		allowAllSSL = false;
		useScreen = null;
		footer = null;
		noProbTextBg = true;
		host_scheduled_downtime_show_services = true;
		host_acknowledged_show_services = true;
		host_scheduled_downtime_or_ack_show_services = true;
		maxCheckAge = -1;
		fullscreen = FullScreenMode.NONE;
		showFlappingIcon = false;
		doubleBuffering = false;
		displayUnknown = true;
		displayDown = true;
		webcamTimeout = -1;
		flash = false;
		minRowHeight = -1;
		ldapBaseDn = null;
		ldapUrl = null;
		unlock();
	}

	public Config()
	{
		initColors();
		initSortFields();
		setDefaultParameterValues();
	}

	public Config(String fileName) throws Exception
	{
		initColors();
		initSortFields();
		setDefaultParameterValues();

		loadConfig(fileName);
	}

	private void initSortFields()
	{
		sortFields = new ArrayList<String>();
		sortFields.add("current_state");
		sortFields.add("last_check");
		sortFields.add("last_state_change");
		sortFields.add("problem_has_been_acknowledged");
		sortFields.add("last_time_up");
		sortFields.add("last_time_down");
		sortFields.add("last_time_unreachable");
		sortFields.add("last_notification");
		sortFields.add("current_notification_number");
		sortFields.add("notifications_enabled");
		sortFields.add("event_handler_enabled");
		sortFields.add("active_checks_enabled");
		sortFields.add("flap_detection_enabled");
		sortFields.add("is_flapping");
		sortFields.add("percent_state_change");
		sortFields.add("scheduled_downtime_depth");
		sortFields.add("failure_prediction_enabled");
		sortFields.add("process_performance_data");
		sortFields.add("plugin_output");
		sortFields.add("state_type");
		sortFields.add("host_name");
		Collections.sort(sortFields);
	}

	public void setConfigFilename(String fileName)
	{
		lock();
		configFilename = fileName;
		unlock();
	}

	boolean isIsTrue(String str) {
		return str.equalsIgnoreCase("true") ? true : false;
	}

	private String getFromArray(String [] array, int nr) {
		if (array.length <= nr)
			return null;

		return array[nr];
	}

	public void loadAppletParameters(Applet a) throws Exception {
		String data = null;

		data = a.getParameter("source");
		if (data != null)
		{
			String [] parameters = data.split(" ");
			NagiosDataSource nds = null;
			NagiosVersion nv = null;
			String type = parameters[0];
			String versionStr = parameters[1];

			if (versionStr.equals("1"))
				nv = NagiosVersion.V1;
			else if (versionStr.equals("2"))
				nv = NagiosVersion.V2;
			else if (versionStr.equals("3"))
				nv = NagiosVersion.V3;
			else
				throw new Exception("Nagios version '" + versionStr + "' not known.");

			if (type.equalsIgnoreCase("http"))
				nds = new NagiosDataSource(new URL(parameters[2]), nv, getFromArray(parameters, 3));
			else if (type.equalsIgnoreCase("http-auth"))
				nds = new NagiosDataSource(new URL(parameters[2]), parameters[3], parameters[4], nv, getFromArray(parameters, 5));
			else if (type.equalsIgnoreCase("file"))
				nds = new NagiosDataSource(parameters[2], nv, getFromArray(parameters, 3));
			else if (type.equalsIgnoreCase("tcp") || type.equalsIgnoreCase("ztcp"))
			{
				String host = parameters[2];
				int port = Integer.valueOf(parameters[3]);
				nds = new NagiosDataSource(host, port, nv, type.equalsIgnoreCase("ztcp"), getFromArray(parameters, 4));
			}
			else
				throw new Exception("Data source-type '" + type + "' not understood.");

			addNagiosDataSource(nds);
		}

		data = a.getParameter("splitter");
		if (data != null) {
			if (data.equalsIgnoreCase("none") || data.length() < 1)
				setLineScrollSplitter(null);
			else
				setLineScrollSplitter(data);
		}

		data = a.getParameter("counter-position");
		if (data != null)
			setCounterPosition(data);
		data = a.getParameter("logo-position");
		if (data != null)
			setLogoPosition(data);
		data = a.getParameter("footer");
		if (data != null)
			setFooter(data);

		data = a.getParameter("proxy-host");
		if (data != null && data.equals("") == false)
			setProxyHost(data);
		data = a.getParameter("proxy-port");
		if (data != null)
			setProxyPort(Integer.valueOf(data));

		data = a.getParameter("adapt-img");
		if (data != null)
			setAdaptImageSize(isIsTrue(data));
		data = a.getParameter("flash");
		if (data != null)
			setFlash(isIsTrue(data));
		data = a.getParameter("min-row-height");
		if (data != null)
			setMinRowHeight(Integer.valueOf(data));
		data = a.getParameter("ldap-base-dn");
		if (data != null)
			setLDAPBaseDN(data);
		data = a.getParameter("ldap-url");
		if (data != null)
			setLDAPUrl(data);
		data = a.getParameter("header");
		if (data != null)
			setHeader(data);
		data = a.getParameter("host-issue");
		if (data != null)
			setHostIssue(data);
		data = a.getParameter("max-check-age");
		if (data != null)
			setMaxCheckAge(Long.valueOf(data));
		data = a.getParameter("sparkline-width");
		if (data != null)
			setSparkLineWidth(Integer.valueOf(data));
		data = a.getParameter("sparkline-graph-mode");
		if (data != null)
		{
			if (data.equals("avg-sd"))
				setSparklineGraphMode(SparklineGraphMode.AVG_SD);
			else if (data.equals("min-max"))
				setSparklineGraphMode(SparklineGraphMode.MIN_MAX);
			else
				throw new Exception("sparkline-graph-mode " + data + " unknown");
		}
		data = a.getParameter("service-issue");
		if (data != null)
			setServiceIssue(data);
		data = a.getParameter("logo");
		if (data != null)
		 	setLogo(data);
		data = a.getParameter("counter");
		if (data != null)
			setCounter(isIsTrue(data));
		data = a.getParameter("flexible-n-columns");
		if (data != null)
			setFlexibleNColumns(isIsTrue(data));
		data = a.getParameter("row-border");
		if (data != null)
			setRowBorder(isIsTrue(data));
		data = a.getParameter("anti-alias");
		if (data != null)
			setAntiAlias(isIsTrue(data));
		data = a.getParameter("draw-problems-service-split-line");
		if (data != null)
			setDrawProblemServiceSplitLine(isIsTrue(data));
		data = a.getParameter("no-problems-text-with-bg-color");
		if (data != null)
			setNoProblemsTextBg(isIsTrue(data));
		data = a.getParameter("max-quality-graphics");
		if (data != null)
			setMaxQualityGraphics(isIsTrue(data));
		data = a.getParameter("row-border-color");
		if (data != null)
			setRowBorderColor(data);
		// else if (name.equals("sound"))
		// 	setProblemSound(data);
		data = a.getParameter("color-bg-to-state");
		if (data != null)
			setSetBgColorToState(isIsTrue(data));
		data = a.getParameter("problem-columns");
		if (data != null)
			setNProblemCols(Integer.valueOf(data));
		data = a.getParameter("web-expire-time");
		if (data != null)
			setWebSessionExpire(Integer.valueOf(data));
		data = a.getParameter("listen-port");
		if (data != null)
			setHTTPServerListenPort(Integer.valueOf(data));
		data = a.getParameter("upper-row-border-height");
		if (data != null)
			setUpperRowBorderHeight(Integer.valueOf(data));
		data = a.getParameter("listen-adapter");
		if (data != null)
			setHTTPServerListenAdapter(data);
		data = a.getParameter("bgcolor");
		if (data != null)
			setBackgroundColor(data);
		data = a.getParameter("bgcolor-fade-to");
		if (data != null)
			setBackgroundColorFadeTo(data);
		data = a.getParameter("problem-row-gradient");
		if (data != null)
			setProblemRowGradient(data);
		data = a.getParameter("reduce-textwidth");
		if (data != null)
			setReduceTextWidth(isIsTrue(data));
		data = a.getParameter("also-scheduled-downtime");
		if (data != null)
			setAlsoScheduledDowntime(isIsTrue(data));
		data = a.getParameter("header-always-bgcolor");
		if (data != null)
			setHeaderAlwaysBGColor(isIsTrue(data));

		data = a.getParameter("show-flapping");
		if (data != null)
			setShowFlapping(isIsTrue(data));

		data = a.getParameter("double-buffering");
		if (data != null)
			setDoubleBuffering(isIsTrue(data));

		data = a.getParameter("show-flapping-icon");
		if (data != null)
			setShowFlappingIcon(isIsTrue(data));
		data = a.getParameter("also-soft-state");
		if (data != null)
			setAlsoSoftState(isIsTrue(data));
		data = a.getParameter("also-disabled-active-checks");
		if (data != null)
			setAlsoDisabledActiveChecks(isIsTrue(data));
		data = a.getParameter("show-services-for-host-with-problems");
		if (data != null)
			setShowServicesForHostWithProblems(isIsTrue(data));
		data = a.getParameter("host-scheduled-downtime-show-services");
		if (data != null)
			setHostScheduledDowntimeShowServices(isIsTrue(data));
		data = a.getParameter("host-acknowledged-show-services");
		if (data != null)
			setHostAcknowledgedShowServices(isIsTrue(data));
		data = a.getParameter("textcolor");
		if (data == null)
			data = a.getParameter("text-color");
		if (data != null)
			setTextColor(data);
		data = a.getParameter("warning-textcolor");
		if (data != null)
			setWarningTextColor(data);
		data = a.getParameter("critical-textcolor");
		if (data != null)
			setCriticalTextColor(data);
		data = a.getParameter("graph-color");
		if (data != null)
			setGraphColor(data);

		data = a.getParameter("hosts-filter-exclude");
		if (data != null)
		{
			if (data.trim().equals("") == false)
				setHostsFilterExclude(data);
		}
		data = a.getParameter("hosts-filter-include");
		if (data != null)
		{
			if (data.trim().equals("") == false)
				setHostsFilterInclude(data);
		}
		data = a.getParameter("services-filter-exclude");
		if (data != null)
		{
			if (data.trim().equals("") == false)
				setServicesFilterExclude(data);
		}
		data = a.getParameter("services-filter-include");
		if (data != null)
		{
			if (data.trim().equals("") == false)
				setServicesFilterInclude(data);
		}

		data = a.getParameter("bgcolorok");
		if (data != null)
			setBackgroundColorOkStatus(data);
		data = a.getParameter("nrows");
		if (data != null)
			setNRows(Integer.valueOf(data));
		data = a.getParameter("interval");
		if (data != null)
			setSleepTime(Integer.valueOf(data));

		data = a.getParameter("webcam-timeout");
		if (data != null)
			setWebcamTimeout(Integer.valueOf(data));

		data = a.getParameter("scrolling-header");
		if (data != null)
			setScrollingHeader(isIsTrue(data));
		data = a.getParameter("scrolling-footer");
		if (data != null)
			setScrollingFooter(isIsTrue(data));
		data = a.getParameter("scroll-pixels-per-sec");
		if (data != null)
			setScrollingPixelsPerSecond(Integer.valueOf(data));
		data = a.getParameter("transparency");
		if (data != null)
			setTransparency(Float.valueOf(data));
		data = a.getParameter("header-transparency");
		if (data != null)
			setHeaderTransparency(Float.valueOf(data));
		data = a.getParameter("image");
		if (data != null)
			addImageUrl(data);
		// else if (name.equals("cam-rows"))
		// 	setCamRows(Integer.valueOf(data));
		// else if (name.equals("cam-cols"))
		// 	setCamCols(Integer.valueOf(data));
		data = a.getParameter("ignore-aspect-ratio");
		if (data != null)
			setKeepAspectRatio(!(isIsTrue(data)));
		data = a.getParameter("sort-order");
		if (data != null)
		{
			String field = null;
			boolean numeric = false, reverse = false;
			String [] fields = data.split(" ");
			for(int index=0; index<fields.length; index++)
			{
				if (fields[index].equals("numeric"))
					numeric = true;
				else if (fields[index].equals("reverse"))
					reverse = true;
				else
					field = fields[index];
			}
			setSortOrder(field, numeric, reverse);
		}
		data = a.getParameter("always-notify");
		if (data != null)
			setAlwaysNotify(isIsTrue(data));
		data = a.getParameter("also-acknowledged");
		if (data != null)
			setAlsoAcknowledged(isIsTrue(data));
		data = a.getParameter("show-header");
		if (data != null)
			setShowHeader(isIsTrue(data));
		data = a.getParameter("scroll-if-not-fitting");
		if (data != null)
			setScrollIfNotFit(isIsTrue(data));
		data = a.getParameter("font");
		if (data != null)
			setFontName(data);
		data = a.getParameter("critical-font");
		if (data != null)
			setCriticalFontName(data);
		data = a.getParameter("warning-font");
		if (data != null)
			setWarningFontName(data);
		data = a.getParameter("warning-bg-color");
		if (data != null)
			setWarningBgColor(data);
		data = a.getParameter("critical-bg-color");
		if (data != null)
			setCriticalBgColor(data);

		data = a.getParameter("warning-bg-color-soft");
		if (data != null)
			setWarningBgColorSoft(data);
		data = a.getParameter("critical-bg-color-soft");
		if (data != null)
			setCriticalBgColorSoft(data);

		data = a.getParameter("nagios-unknown-bg-color");
		if (data != null)
			setNagiosUnknownBgColor(data);
		data = a.getParameter("disable-http-compression");
		if (data != null)
			setAllowHTTPCompression(isIsTrue(data) ? false : true);
		data = a.getParameter("no-problems-text");
		if (data != null)
			setNoProblemsText(data);
		data = a.getParameter("state-problems-text");
		if (data != null)
			setStateProblemsText(data);
		data = a.getParameter("host-scheduled-downtime-or-ack-show-services");
		if (data != null)
			setHostSDOrAckShowServices(isIsTrue(data));
		data = a.getParameter("no-problems-text-position");
		if (data != null)
			setNoProblemsTextPosition(data);
		data = a.getParameter("web-username");
		if (data != null)
			setWebUsername(data);
		data = a.getParameter("web-password");
		if (data != null)
			setWebPassword(data);

		data = a.getParameter("display-unknown");
		if (data != null)
			setDisplayUnknown(isIsTrue(data));
		data = a.getParameter("display-down");
		if (data != null)
			setDisplayDown(isIsTrue(data));
	}

	public void loadConfig(String fileName) throws Exception
	{
		lock();
		configFilename = fileName;
		ndsList = new ArrayList<NagiosDataSource>();
		unlock();

		BufferedReader in;
		try
		{
			in = new BufferedReader(new FileReader(fileName));

			String line;
			int lineNr = 0;
			try
			{
				while((line = in.readLine()) != null)
				{
					lineNr++;
					if (line.trim().length() == 0 || line.substring(0, 1).equals("#"))
						continue;

					int is = line.indexOf("=");
					if (is == -1)
						throw new Exception("Error on line " + lineNr + ": malformed line.");

					String name = line.substring(0, is).trim();
					String data = line.substring(is + 1).trim();

					boolean isTrue = data.equalsIgnoreCase("true") ? true : false;

					if (name.equals("config"))
						loadConfig(data);
					else if (name.equals("allow-all-ssl"))
						setAllowAllSSL(isTrue);
					else if (name.equals("source"))
					{
						String [] parameters = data.split(" ");
						NagiosDataSource nds = null;
						NagiosVersion nv = null;
						String type = parameters[0];
						String versionStr = parameters[1];

						if (versionStr.equals("1"))
							nv = NagiosVersion.V1;
						else if (versionStr.equals("2"))
							nv = NagiosVersion.V2;
						else if (versionStr.equals("3"))
							nv = NagiosVersion.V3;
						else
							throw new Exception("Nagios version '" + versionStr + "' not known.");

						if (type.equalsIgnoreCase("http"))
							nds = new NagiosDataSource(new URL(parameters[2]), nv, getFromArray(parameters, 3));
						else if (type.equalsIgnoreCase("http-auth"))
							nds = new NagiosDataSource(new URL(parameters[2]), parameters[3], parameters[4], nv, getFromArray(parameters, 5));
						else if (type.equalsIgnoreCase("file"))
							nds = new NagiosDataSource(parameters[2], nv, getFromArray(parameters, 3));
						else if (type.equalsIgnoreCase("tcp") || type.equalsIgnoreCase("ztcp"))
						{
							String host = parameters[2];
							int port = Integer.valueOf(parameters[3]);
							nds = new NagiosDataSource(host, port, nv, type.equalsIgnoreCase("ztcp"), getFromArray(parameters, 4));
						}
						else if (type.equalsIgnoreCase("ls"))
						{
							String host = parameters[2];
							int port = Integer.valueOf(parameters[3]);
							nds = new NagiosDataSource(host, port, getFromArray(parameters, 4));
							if (nv != NagiosVersion.V3)
								throw new Exception("LiveStatus source only accepts version 3");
						}
						else
							throw new Exception("Data source-type '" + type + "' not understood.");

						addNagiosDataSource(nds);
					}
					else if (name.equals("predict"))
						setBrainFileName(data);
					else if (name.equals("scroll-splitter") || name.equals("splitter"))
					{
						if (data.equalsIgnoreCase("none") || data.length() < 1)
							setLineScrollSplitter(null);
						else
							setLineScrollSplitter(data);
					}
					else if (name.equals("counter-position"))
						setCounterPosition(data);
					else if (name.equals("logo-position"))
						setLogoPosition(data);
					else if (name.equals("exec"))
						setExec(data);
					else if (name.equals("fullscreen"))
					{
						if (data.equalsIgnoreCase("none") || data.equalsIgnoreCase("false"))
							setFullscreen(FullScreenMode.NONE);
						else if (data.equalsIgnoreCase("undecorated") || data.equalsIgnoreCase("true"))
							setFullscreen(FullScreenMode.UNDECORATED);
						else if (data.equalsIgnoreCase("fullscreen"))
							setFullscreen(FullScreenMode.FULLSCREEN);
						else if (data.equalsIgnoreCase("allmonitors"))
							setFullscreen(FullScreenMode.ALLMONITORS);
						else
							throw new Exception("Fullscreen mode " + data + " not recognized");
					}
					else if (name.equals("use-screen") && data.equals("") == false && data.equals("null") == false)
						setUseScreen(data);
					else if (name.equals("footer"))
						setFooter(data);
					else if (name.equals("double-buffering"))
						setDoubleBuffering(isTrue);
					else if (name.equals("adapt-img"))
						setAdaptImageSize(isTrue);
					else if (name.equals("display-unknown"))
						setDisplayUnknown(isTrue);
					else if (name.equals("display-down"))
						setDisplayDown(isTrue);
					else if (name.equals("random-img"))
						setRandomWebcam(isTrue);
					else if (name.equals("no-gui"))
						setRunGui(!(isTrue));
					else if (name.equals("min-row-height"))
						setMinRowHeight(Integer.valueOf(data));
					else if (name.equals("header"))
						setHeader(data);
					else if (name.equals("host-issue"))
						setHostIssue(data);
					else if (name.equals("max-check-age"))
						setMaxCheckAge(Long.valueOf(data));
					else if (name.equals("sparkline-width"))
						setSparkLineWidth(Integer.valueOf(data));
					else if (name.equals("sparkline-graph-mode"))
					{
						if (data.equals("avg-sd"))
							setSparklineGraphMode(SparklineGraphMode.AVG_SD);
						else if (data.equals("min-max"))
							setSparklineGraphMode(SparklineGraphMode.MIN_MAX);
						else
							throw new Exception("sparkline-graph-mode " + data + " unknown");
					}
					else if (name.equals("service-issue"))
						setServiceIssue(data);
					else if (name.equals("logo"))
						setLogo(data);
					else if (name.equals("counter"))
						setCounter(isTrue);
					else if (name.equals("flexible-n-columns"))
						setFlexibleNColumns(isTrue);
					else if (name.equals("flash"))
						setFlash(isTrue);
					else if (name.equals("verbose"))
						setVerbose(isTrue);
					else if (name.equals("row-border"))
						setRowBorder(isTrue);
					else if (name.equals("anti-alias"))
						setAntiAlias(isTrue);
					else if (name.equals("draw-problems-service-split-line"))
						setDrawProblemServiceSplitLine(isTrue);
					else if (name.equals("no-problems-text-with-bg-color"))
						setNoProblemsTextBg(isTrue);
					else if (name.equals("max-quality-graphics"))
						setMaxQualityGraphics(isTrue);
					else if (name.equals("row-border-color"))
						setRowBorderColor(data);
					else if (name.equals("sound"))
						setProblemSound(data);
					else if (name.equals("proxy-host"))
						setProxyHost(data);
					else if (name.equals("proxy-port"))
						setProxyPort(Integer.valueOf(data));
					else if (name.equals("color-bg-to-state"))
						setSetBgColorToState(isTrue);
					else if (name.equals("problem-columns"))
						setNProblemCols(Integer.valueOf(data));
					else if (name.equals("web-expire-time"))
						setWebSessionExpire(Integer.valueOf(data));
					else if (name.equals("listen-port"))
						setHTTPServerListenPort(Integer.valueOf(data));
					else if (name.equals("upper-row-border-height"))
						setUpperRowBorderHeight(Integer.valueOf(data));
					else if (name.equals("listen-adapter"))
						setHTTPServerListenAdapter(data);
					else if (name.equals("latency-file"))
						setLatencyFile(data);
					else if (name.equals("bgcolor"))
						setBackgroundColor(data);
					else if (name.equals("bgcolor-fade-to"))
						setBackgroundColorFadeTo(data);
					else if (name.equals("problem-row-gradient"))
						setProblemRowGradient(data);
					else if (name.equals("reduce-textwidth"))
						setReduceTextWidth(isTrue);
					else if (name.equals("also-scheduled-downtime"))
						setAlsoScheduledDowntime(isTrue);
					else if (name.equals("header-always-bgcolor"))
						setHeaderAlwaysBGColor(isTrue);
					else if (name.equals("show-flapping"))
						setShowFlapping(isTrue);
					else if (name.equals("show-flapping-icon"))
						setShowFlappingIcon(isTrue);
					else if (name.equals("also-soft-state"))
						setAlsoSoftState(isTrue);
					else if (name.equals("also-disabled-active-checks"))
						setAlsoDisabledActiveChecks(isTrue);
					else if (name.equals("show-services-for-host-with-problems"))
						setShowServicesForHostWithProblems(isTrue);
					else if (name.equals("host-scheduled-downtime-show-services"))
						setHostScheduledDowntimeShowServices(isTrue);
					else if (name.equals("host-acknowledged-show-services"))
						setHostAcknowledgedShowServices(isTrue);
					else if (name.equals("textcolor") || name.equals("text-color"))
						setTextColor(data);
					else if (name.equals("warning-textcolor"))
						setWarningTextColor(data);
					else if (name.equals("critical-textcolor"))
						setCriticalTextColor(data);
					else if (name.equals("graph-color"))
						setGraphColor(data);

					else if (name.equals("hosts-filter-exclude"))
					{
						if (data.trim().equals("") == false)
							setHostsFilterExclude(data);
					}
					else if (name.equals("hosts-filter-include"))
					{
						if (data.trim().equals("") == false)
							setHostsFilterInclude(data);
					}
					else if (name.equals("services-filter-exclude"))
					{
						if (data.trim().equals("") == false)
							setServicesFilterExclude(data);
					}
					else if (name.equals("services-filter-include"))
					{
						if (data.trim().equals("") == false)
							setServicesFilterInclude(data);
					}

					else if (name.equals("bgcolorok"))
						setBackgroundColorOkStatus(data);
					else if (name.equals("nrows"))
						setNRows(Integer.valueOf(data));
					else if (name.equals("interval"))
						setSleepTime(Integer.valueOf(data));
					else if (name.equals("webcam-timeout"))
						setWebcamTimeout(Integer.valueOf(data));
					else if (name.equals("scrolling-header"))
						setScrollingHeader(isTrue);
					else if (name.equals("scrolling-footer"))
						setScrollingFooter(isTrue);
					else if (name.equals("scroll-pixels-per-sec"))
						setScrollingPixelsPerSecond(Integer.valueOf(data));
					else if (name.equals("transparency"))
						setTransparency(Float.valueOf(data));
					else if (name.equals("header-transparency"))
						setHeaderTransparency(Float.valueOf(data));
					else if (name.equals("image"))
						addImageUrl(data);
					else if (name.equals("cam-rows"))
						setCamRows(Integer.valueOf(data));
					else if (name.equals("cam-cols"))
						setCamCols(Integer.valueOf(data));
					else if (name.equals("prefer"))
						setPrefers(data);
					else if (name.equals("ignore-aspect-ratio"))
						setKeepAspectRatio(!(isTrue));
					else if (name.equals("sort-order"))
					{
						String field = null;
						boolean numeric = false, reverse = false;
						String [] fields = data.split(" ");
						for(int index=0; index<fields.length; index++)
						{
							if (fields[index].equals("numeric"))
								numeric = true;
							else if (fields[index].equals("reverse"))
								reverse = true;
							else
								field = fields[index];
						}
						setSortOrder(field, numeric, reverse);
					}
					else if (name.equals("always-notify"))
						setAlwaysNotify(isTrue);
					else if (name.equals("also-acknowledged"))
						setAlsoAcknowledged(isTrue);
					else if (name.equals("show-header"))
						setShowHeader(isTrue);
					else if (name.equals("scroll-if-not-fitting"))
						setScrollIfNotFit(isTrue);
					else if (name.equals("font"))
						setFontName(data);
					else if (name.equals("critical-font"))
						setCriticalFontName(data);
					else if (name.equals("warning-font"))
						setWarningFontName(data);
					else if (name.equals("warning-bg-color"))
						setWarningBgColor(data);
					else if (name.equals("critical-bg-color"))
						setCriticalBgColor(data);
					else if (name.equals("warning-bg-color-soft"))
						setWarningBgColorSoft(data);
					else if (name.equals("critical-bg-color-soft"))
						setCriticalBgColorSoft(data);
					else if (name.equals("nagios-unknown-bg-color"))
						setNagiosUnknownBgColor(data);
					else if (name.equals("disable-http-compression"))
						setAllowHTTPCompression(isTrue ? false : true);
					else if (name.equals("performance-data-filename"))
						setPerformanceDataFileName(data);
					else if (name.equals("no-problems-text"))
						setNoProblemsText(data);
					else if (name.equals("state-problems-text"))
						setStateProblemsText(data);
					else if (name.equals("host-scheduled-downtime-or-ack-show-services"))
						setHostSDOrAckShowServices(isTrue);
					else if (name.equals("no-problems-text-position"))
						setNoProblemsTextPosition(data);
					else if (name.equals("web-username"))
						setWebUsername(data);
					else if (name.equals("web-password"))
						setWebPassword(data);
					else if (name.equals("ldap-base-dn"))
						setLDAPBaseDN(data);
					else if (name.equals("ldap-url"))
						setLDAPUrl(data);
					else
						throw new Exception("Unknown parameter on line " + lineNr);
				}
			}
			catch(ArrayIndexOutOfBoundsException aioobe)
			{
				System.err.println("Please check line " + lineNr + " of configuration-file " + fileName + ": a parameter may be missing");
				System.exit(127);
			}
			catch(NumberFormatException nfeGlobal)
			{
				System.err.println("Please check line " + lineNr + " of configuration-file " + fileName + ": one of the parameters ought to be a number");
				System.exit(127);
			}

			in.close();
		}
		catch(FileNotFoundException e)
		{
			System.err.println("File " + fileName + " not found. Please use --create-config to create a new configuration file.");
			System.exit(127);
		}
	}

	void writeLine(BufferedWriter out, String line) throws Exception
	{
		out.write(line, 0, line.length());
		out.newLine();
	}

	public List<String []> collectConfig() {
		List<String []> output = new ArrayList<String []>();

		output.add(new String [] { "allow-all-ssl", (getAllowAllSSL() ? "true" : "false")});
		if (getBrainFileName() != null)
			output.add(new String [] { "predict", getBrainFileName()});
		if (getExec() != null)
			output.add(new String [] { "exec", getExec()});
		output.add(new String [] { "adapt-img", (getAdaptImageSize() ? "true" : "false")});
		output.add(new String [] { "random-img", (getRandomWebcam() ? "true" : "false")});
		output.add(new String [] { "counter", (getCounter() ? "true" : "false")});
		if (getProblemSound() != null)
			output.add(new String [] { "sound", getProblemSound()});
		output.add(new String [] { "listen-port", "" + getHTTPServerListenPort()});
		output.add(new String [] { "listen-adapter", getHTTPServerListenAdapter()});
		output.add(new String [] { "bgcolor", getBackgroundColorName()});
		if (getBackgroundColorFadeTo() != null)
			output.add(new String [] { "bgcolor-fade-to", getBackgroundColorFadeToName()});
		if (getProblemRowGradient() != null)
			output.add(new String [] { "problem-row-gradient", getProblemRowGradientName()});
		if (getPerformanceDataFileName() != null)
			output.add(new String [] { "performance-data-filename", getPerformanceDataFileName()});
		output.add(new String [] { "textcolor", getTextColorName()});
		output.add(new String [] { "warning-textcolor", getWarningTextColorName()});
		output.add(new String [] { "critical-textcolor", getCriticalTextColorName()});
		output.add(new String [] { "bgcolorok", getBackgroundColorOkStatusName()});
		output.add(new String [] { "nrows", "" + getNRows()});
		output.add(new String [] { "flexible-n-columns", (getFlexibleNColumns() ? "true" : "false")});
		output.add(new String [] { "no-problems-text-with-bg-color", (getNoProblemsTextBg() ? "true" : "false")});
		output.add(new String [] { "host-scheduled-downtime-show-services", (getHostScheduledDowntimeShowServices() ? "true" : "false")});
		output.add(new String [] { "host-acknowledged-show-services", (getHostAcknowledgedShowServices() ? "true" : "false")});
		output.add(new String [] { "host-scheduled-downtime-or-ack-show-services", (getHostSDOrAckShowServices() ? " true" : "false")});
		output.add(new String [] { "interval", "" + getSleepTime()});
		output.add(new String [] { "webcam-timeout", "" + getWebcamTimeout()});
		List<String> iu = getImageUrls();
		List<ImageUrlType> iut = getImageUrlTypes();
		for(int index=0; index<iu.size(); index++)
			output.add(new String [] { "image", iut.get(index) + " " + iu.get(index)});
		output.add(new String [] { "prefer", getPrefersList()});
		output.add(new String [] { "always-notify", (getAlwaysNotify() ? "true" : "false")});
		output.add(new String [] { "also-acknowledged", (getAlsoAcknowledged() ? "true" : "false")});
		output.add(new String [] { "display-unknown", (getDisplayUnknown() ? "true" : "false")});
		output.add(new String [] { "display-down", (getDisplayDown() ? "true" : "false")});
		output.add(new String [] { "min-row-height", "" + getMinRowHeight()});
		output.add(new String [] { "font", getFontName()});
		output.add(new String [] { "critical-font", getCriticalFontName()});
		output.add(new String [] { "warning-font", getWarningFontName()});
		output.add(new String [] { "verbose", (getVerbose() ? "true" : "false")});
		output.add(new String [] { "double-buffering", (getDoubleBuffering() ? "true" : "false")});
		output.add(new String [] { "anti-alias", (getAntiAlias() ? "true" : "false")});
		output.add(new String [] { "max-quality-graphics", (getMaxQualityGraphics() ? "true" : "false")});
		output.add(new String [] { "row-border", (getRowBorder() ? "true" : "false")});
		output.add(new String [] { "draw-problems-service-split-line", (getDrawProblemServiceSplitLine() ? "true" : "false")});
		output.add(new String [] { "row-border-color", getRowBorderColorName()});
		output.add(new String [] { "max-check-age", "" + getMaxCheckAge()});
		output.add(new String [] { "upper-row-border-height", "" + getUpperRowBorderHeight()});
		output.add(new String [] { "graph-color", getGraphColorName()});
		output.add(new String [] { "no-gui", (!getRunGui() ? "true" : "false")});
		output.add(new String [] { "fullscreen", getFullscreenName()});
		if (getUseScreen() != null && getUseScreen().equals("") == false)
			output.add(new String [] { "use-screen", getUseScreen()});
		output.add(new String [] { "reduce-textwidth", (getReduceTextWidth() ? "true" : "false")});
		if (getHeaderSet() == true)
			output.add(new String [] { "header", getHeader()});
		if (getFooter() != null)
			output.add(new String [] { "footer", getFooter()});
		output.add(new String [] { "show-header", (getShowHeader() ? "true" : "false")});
		output.add(new String [] { "scrolling-header", (getScrollingHeader() ? "true" : "false")});
		output.add(new String [] { "scrolling-footer", (getScrollingFooter() ? "true" : "false")});
		output.add(new String [] { "scroll-pixels-per-sec", "" + getScrollingPixelsPerSecond()});
		output.add(new String [] { "host-issue", getHostIssue()});
		output.add(new String [] { "service-issue", getServiceIssue()});
		output.add(new String [] { "transparency", "" + getTransparency()});
		output.add(new String [] { "header-transparency", "" + getHeaderTransparency()});
		output.add(new String [] { "hosts-filter-exclude", getHostsFilterExcludeList()});
		output.add(new String [] { "hosts-filter-include", getHostsFilterIncludeList()});
		output.add(new String [] { "services-filter-exclude", getServicesFilterExcludeList()});
		output.add(new String [] { "services-filter-include", getServicesFilterIncludeList()});
		output.add(new String [] { "scroll-if-not-fitting", (getScrollIfNotFit() ? "true" : "false")});
		output.add(new String [] { "splitter", getLineScrollSplitterString()});
		output.add(new String [] { "counter-position", getCounterPositionName()});
		output.add(new String [] { "sparkline-width", "" + getSparkLineWidth()});
		output.add(new String [] { "header-always-bgcolor", (getHeaderAlwaysBGColor() ? "true" : "false")});
		if (getLogo() != null)
			output.add(new String [] { "logo", getLogo()});
		if (getLogoPosition() != null)
			output.add(new String [] { "logo-position", getLogoPositionName()});
		output.add(new String [] { "web-expire-time", "" + getWebSessionExpire()});
		if (getLatencyFile() != null)
			output.add(new String [] { "latency-file", getLatencyFile()});
		if (getNoProblemsText() != null)
			output.add(new String [] { "no-problems-text", getNoProblemsText()});
		if (getStateProblemsText() != null)
			output.add(new String [] { "state-problems-text", getStateProblemsText()});
		output.add(new String [] { "no-problems-text-position", getNoProblemsTextPositionName()});
		String sparkMode = "";
		if (getSparklineGraphMode() == SparklineGraphMode.AVG_SD)
			sparkMode += "avg-sd";
		else if (getSparklineGraphMode() == SparklineGraphMode.MIN_MAX)
			sparkMode += "min-max";
		output.add(new String [] { "sparkline-graph-mode", sparkMode });
		String sort = "";
		if (getSortOrderNumeric())
			sort += "numeric ";
		if (getSortOrderReverse())
			sort += "reverse ";
		sort += getSortOrder();
		output.add(new String [] { "sort-order", sort});
		output.add(new String [] { "also-scheduled-downtime", (getAlsoScheduledDowntime() ? "true" : "false")});
		output.add(new String [] { "also-soft-state", (getAlsoSoftState() ? "true" : "false")});
		output.add(new String [] { "also-disabled-active-checks", (getAlsoDisabledActiveChecks() ? "true" : "false")});
		output.add(new String [] { "show-services-for-host-with-problems", (getShowServicesForHostWithProblems() ? "true" : "false")});
		output.add(new String [] { "show-flapping", (getShowFlapping() ? "true" : "false")});
		output.add(new String [] { "show-flapping-icon", (getShowFlappingIcon() ? "true" : "false")});
		output.add(new String [] { "flash", "" + getFlash()});
		output.add(new String [] { "problem-columns", "" + getNProblemCols()});
		if (getWebUsername() != null)
			output.add(new String [] { "web-username", getWebUsername()});
		if (getWebPassword() != null)
			output.add(new String [] { "web-password", getWebPassword()});
		if (ldapBaseDn != null)
			output.add(new String [] { "ldap-base-dn", ldapBaseDn});
		if (ldapUrl != null)
			output.add(new String [] { "ldap-url", ldapUrl});

		for(NagiosDataSource dataSource : getNagiosDataSources())
		{
			String type = "?";
			if (dataSource.getType() == NagiosDataSourceType.TCP)
				type = "tcp";
			else if (dataSource.getType() == NagiosDataSourceType.ZTCP)
				type = "ztcp";
			else if (dataSource.getType() == NagiosDataSourceType.LS)
				type = "ls";
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
			if (dataSource.getType() == NagiosDataSourceType.TCP || dataSource.getType() == NagiosDataSourceType.ZTCP || dataSource.getType() == NagiosDataSourceType.LS)
				parameters = dataSource.getHost() + " " + dataSource.getPort();
			else if (dataSource.getType() == NagiosDataSourceType.HTTP)
			{
				parameters = dataSource.getURL().toString();
				if (dataSource.getUsername() != null && dataSource.getPassword() != null)
				{
					parameters += " " + dataSource.getUsername() + " " + dataSource.getPassword();
					type = "http-auth";
				}
			}
			else if (dataSource.getType() == NagiosDataSourceType.FILE)
				parameters = dataSource.getFile();

			output.add(new String [] { "source", type + " " + version + " " + parameters});
		}

		output.add(new String [] { "cam-rows", "" + getCamRows()});
		output.add(new String [] { "cam-cols", "" + getCamCols()});
		output.add(new String [] { "ignore-aspect-ratio", (!getKeepAspectRatio() ? "true" : "false")});
		output.add(new String [] { "warning-bg-color", getWarningBgColorName()});
		output.add(new String [] { "critical-bg-color", getCriticalBgColorName()});
		output.add(new String [] { "warning-bg-color-soft", getWarningBgColorNameSoft()});
		output.add(new String [] { "critical-bg-color-soft", getCriticalBgColorNameSoft()});
		output.add(new String [] { "nagios-unknown-bg-color", getNagiosUnknownBgColorName()});
		output.add(new String [] { "disable-http-compression", (!getAllowHTTPCompression() ? "true" : "false")});
		output.add(new String [] { "color-bg-to-state", (getSetBgColorToState() ? "true" : "false")});

		if (getProxyHost() != null) {
			output.add(new String [] { "proxy-host", getProxyHost()});
			output.add(new String [] { "proxy-port", "" + getProxyPort()});
		}

		return output;
	}

	public void writeConfig(String fileName) throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));

		List<String []> conf = collectConfig();
		for(String [] entry : conf)
			writeLine(out, entry[0] + " = " + entry[1]);

		out.close();
	}

	public String getConfigFilename()
	{
		return configFilename;
	}

	public Color selectColor(String name)
	{
		if (name.equals("NULL"))
			return null;

		for(ColorPair currentColor : colorPairs)
		{
			if (currentColor.equals(name))
				return currentColor.getColor();
		}

		return null;
	}

	public void initColors()
	{
		colorPairs = new ArrayList<ColorPair>();
		colorPairs.add(new ColorPair("aliceblue", 0xf0f8ff));
		colorPairs.add(new ColorPair("antiquewhite", 0xfaebd7));
		colorPairs.add(new ColorPair("aqua", 0x00ffff));
		colorPairs.add(new ColorPair("aquamarine", 0x7fffd4));
		colorPairs.add(new ColorPair("azure", 0xf0ffff));
		colorPairs.add(new ColorPair("beige", 0xf5f5dc));
		colorPairs.add(new ColorPair("bisque", 0xffe4c4));
		colorPairs.add(new ColorPair("black", Color.BLACK));
		colorPairs.add(new ColorPair("blanchedalmond", 0xffebcd));
		colorPairs.add(new ColorPair("blue", Color.BLUE));
		colorPairs.add(new ColorPair("blueviolet", 0x8a2be2));
		colorPairs.add(new ColorPair("brown", 0xa52a2a));
		colorPairs.add(new ColorPair("burlywood", 0xdeb887));
		colorPairs.add(new ColorPair("cadetblue", 0x5f9ea0));
		colorPairs.add(new ColorPair("chartreuse", 0x7fff00));
		colorPairs.add(new ColorPair("chocolate", 0xd2691e));
		colorPairs.add(new ColorPair("CoffeeSaint", 0x59432E));
		colorPairs.add(new ColorPair("coral", 0xff7f50));
		colorPairs.add(new ColorPair("cornflowerblue", 0x6495ed));
		colorPairs.add(new ColorPair("cornsilk", 0xfff8dc));
		colorPairs.add(new ColorPair("crimson", 0xdc143c));
		colorPairs.add(new ColorPair("cyan", 0x00ffff));
		colorPairs.add(new ColorPair("cyan", Color.CYAN));
		colorPairs.add(new ColorPair("darkblue", 0x00008b));
		colorPairs.add(new ColorPair("darkcyan", 0x008b8b));
		colorPairs.add(new ColorPair("darkgoldenrod", 0xb8860b));
		colorPairs.add(new ColorPair("darkgray", 0xa9a9a9));
		colorPairs.add(new ColorPair("dark_gray", Color.DARK_GRAY));
		colorPairs.add(new ColorPair("darkgreen", 0x006400));
		colorPairs.add(new ColorPair("darkkhaki", 0xbdb76b));
		colorPairs.add(new ColorPair("darkmagenta", 0x8b008b));
		colorPairs.add(new ColorPair("darkolivegreen", 0x556b2f));
		colorPairs.add(new ColorPair("darkorange", 0xff8c00));
		colorPairs.add(new ColorPair("darkorchid", 0x9932cc));
		colorPairs.add(new ColorPair("darkred", 0x8b0000));
		colorPairs.add(new ColorPair("darksalmon", 0xe9967a));
		colorPairs.add(new ColorPair("darkseagreen", 0x8fbc8f));
		colorPairs.add(new ColorPair("darkslateblue", 0x483d8b));
		colorPairs.add(new ColorPair("darkslategray", 0x2f4f4f));
		colorPairs.add(new ColorPair("darkturquoise", 0x00ced1));
		colorPairs.add(new ColorPair("darkviolet", 0x9400d3));
		colorPairs.add(new ColorPair("deeppink", 0xff1493));
		colorPairs.add(new ColorPair("deepskyblue", 0x00bfff));
		colorPairs.add(new ColorPair("dimgray", 0x696969));
		colorPairs.add(new ColorPair("dodgerblue", 0x1e90ff));
		colorPairs.add(new ColorPair("firebrick", 0xb22222));
		colorPairs.add(new ColorPair("floralwhite", 0xfffaf0));
		colorPairs.add(new ColorPair("forestgreen", 0x228b22));
		colorPairs.add(new ColorPair("fuchsia", 0xff00ff));
		colorPairs.add(new ColorPair("gainsboro", 0xdcdcdc));
		colorPairs.add(new ColorPair("ghostwhite", 0xf8f8ff));
		colorPairs.add(new ColorPair("gold", 0xffd700));
		colorPairs.add(new ColorPair("goldenrod", 0xdaa520));
		colorPairs.add(new ColorPair("gray", Color.GRAY));
		colorPairs.add(new ColorPair("green", Color.GREEN));
		colorPairs.add(new ColorPair("greenyellow", 0xadff2f));
		colorPairs.add(new ColorPair("honeydew", 0xf0fff0));
		colorPairs.add(new ColorPair("hotpink", 0xff69b4));
		colorPairs.add(new ColorPair("indianred", 0xcd5c5c));
		colorPairs.add(new ColorPair("indigo", 0x4b0082));
		colorPairs.add(new ColorPair("ivory", 0xfffff0));
		colorPairs.add(new ColorPair("khaki", 0xf0e68c));
		colorPairs.add(new ColorPair("lavender", 0xe6e6fa));
		colorPairs.add(new ColorPair("lavenderblush", 0xfff0f5));
		colorPairs.add(new ColorPair("lawngreen", 0x7cfc00));
		colorPairs.add(new ColorPair("lemonchiffon", 0xfffacd));
		colorPairs.add(new ColorPair("lightblue", 0xadd8e6));
		colorPairs.add(new ColorPair("lightcoral", 0xf08080));
		colorPairs.add(new ColorPair("lightcyan", 0xe0ffff));
		colorPairs.add(new ColorPair("lightgoldenrodyellow", 0xfafad2));
		colorPairs.add(new ColorPair("light_gray", Color.LIGHT_GRAY));
		colorPairs.add(new ColorPair("lightgreen", 0x90ee90));
		colorPairs.add(new ColorPair("lightgrey", 0xd3d3d3));
		colorPairs.add(new ColorPair("lightpink", 0xffb6c1));
		colorPairs.add(new ColorPair("lightred", 0xee9090));
		colorPairs.add(new ColorPair("lightsalmon", 0xffa07a));
		colorPairs.add(new ColorPair("lightseagreen", 0x20b2aa));
		colorPairs.add(new ColorPair("lightskyblue", 0x87cefa));
		colorPairs.add(new ColorPair("lightslategray", 0x778899));
		colorPairs.add(new ColorPair("lightsteelblue", 0xb0c4de));
		colorPairs.add(new ColorPair("lightyellow", 0xffffe0));
		colorPairs.add(new ColorPair("lime", 0x00ff00));
		colorPairs.add(new ColorPair("limegreen", 0x32cd32));
		colorPairs.add(new ColorPair("linen", 0xfaf0e6));
		colorPairs.add(new ColorPair("magenta", Color.MAGENTA));
		colorPairs.add(new ColorPair("maroon", 0x800000));
		colorPairs.add(new ColorPair("mediumauqamarine", 0x66cdaa));
		colorPairs.add(new ColorPair("mediumblue", 0x0000cd));
		colorPairs.add(new ColorPair("mediumorchid", 0xba55d3));
		colorPairs.add(new ColorPair("mediumpurple", 0x9370d8));
		colorPairs.add(new ColorPair("mediumseagreen", 0x3cb371));
		colorPairs.add(new ColorPair("mediumslateblue", 0x7b68ee));
		colorPairs.add(new ColorPair("mediumspringgreen", 0x00fa9a));
		colorPairs.add(new ColorPair("mediumturquoise", 0x48d1cc));
		colorPairs.add(new ColorPair("mediumvioletred", 0xc71585));
		colorPairs.add(new ColorPair("midnightblue", 0x191970));
		colorPairs.add(new ColorPair("mintcream", 0xf5fffa));
		colorPairs.add(new ColorPair("mistyrose", 0xffe4e1));
		colorPairs.add(new ColorPair("moccasin", 0xffe4b5));
		colorPairs.add(new ColorPair("navajowhite", 0xffdead));
		colorPairs.add(new ColorPair("navy", 0x000080));
		colorPairs.add(new ColorPair("oldlace", 0xfdf5e6));
		colorPairs.add(new ColorPair("olive", 0x808000));
		colorPairs.add(new ColorPair("olivedrab", 0x688e23));
		colorPairs.add(new ColorPair("orange", Color.ORANGE));
		colorPairs.add(new ColorPair("orangered", 0xff4500));
		colorPairs.add(new ColorPair("orchid", 0xda70d6));
		colorPairs.add(new ColorPair("palegoldenrod", 0xeee8aa));
		colorPairs.add(new ColorPair("palegreen", 0x98fb98));
		colorPairs.add(new ColorPair("paleturquoise", 0xafeeee));
		colorPairs.add(new ColorPair("palevioletred", 0xd87093));
		colorPairs.add(new ColorPair("papayawhip", 0xffefd5));
		colorPairs.add(new ColorPair("peachpuff", 0xffdab9));
		colorPairs.add(new ColorPair("peru", 0xcd853f));
		colorPairs.add(new ColorPair("pink", Color.PINK));
		colorPairs.add(new ColorPair("plum", 0xdda0dd));
		colorPairs.add(new ColorPair("powderblue", 0xb0e0e6));
		colorPairs.add(new ColorPair("purple", 0x800080));
		colorPairs.add(new ColorPair("red", Color.RED));
		colorPairs.add(new ColorPair("rosybrown", 0xbc8f8f));
		colorPairs.add(new ColorPair("royalblue", 0x4169e1));
		colorPairs.add(new ColorPair("saddlebrown", 0x8b4513));
		colorPairs.add(new ColorPair("salmon", 0xfa8072));
		colorPairs.add(new ColorPair("sandybrown", 0xf4a460));
		colorPairs.add(new ColorPair("seagreen", 0x2e8b57));
		colorPairs.add(new ColorPair("seashell", 0xfff5ee));
		colorPairs.add(new ColorPair("sienna", 0xa0522d));
		colorPairs.add(new ColorPair("silver", 0xc0c0c0));
		colorPairs.add(new ColorPair("skyblue", 0x87ceeb));
		colorPairs.add(new ColorPair("slateblue", 0x6a5acd));
		colorPairs.add(new ColorPair("slategray", 0x708090));
		colorPairs.add(new ColorPair("snow", 0xfffafa));
		colorPairs.add(new ColorPair("springgreen", 0x00ff7f));
		colorPairs.add(new ColorPair("steelblue", 0x4682b4));
		colorPairs.add(new ColorPair("tan", 0xd2b48c));
		colorPairs.add(new ColorPair("teal", 0x008080));
		colorPairs.add(new ColorPair("thistle", 0xd8bfd8));
		colorPairs.add(new ColorPair("tomato", 0xff6347));
		colorPairs.add(new ColorPair("turquoise", 0x40e0d0));
		colorPairs.add(new ColorPair("violet", 0xee82ee));
		colorPairs.add(new ColorPair("wheat", 0xf5deb3));
		colorPairs.add(new ColorPair("white", 0xffffff));
		colorPairs.add(new ColorPair("whitesmoke", 0xf5f5f5));
		colorPairs.add(new ColorPair("yellow", Color.YELLOW));
		colorPairs.add(new ColorPair("yellowgreen", 0x9acd32));
	}

	public List<ColorPair> getColors()
	{
		return colorPairs;
	}

	public void listFonts()
	{
		GraphicsEnvironment lge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Font [] fonts = lge.getAllFonts();

		System.out.println("Known fonts: ");
		for(Font font : fonts)
		{
			System.out.println("\t" + font.getFontName());
		}
	}

	public void listColors()
	{
		System.out.println("Known colors: ");
		for(ColorPair currentColor : colorPairs)
			System.out.println("    " + currentColor.getName());
	}

	public void listSortFields()
	{
		for(String current : sortFields)
			System.out.println("    " + current);
	}

	public void setHostIssue(String string)
	{
		lock();
		this.issueHost = string;
		unlock();
	}

	public String getHostIssue()
	{
		String copy;
		lock();
		copy = issueHost;
		unlock();
		return copy;
	}

	public void setNoProblemsText(String string)
	{
		lock();
		this.noProblemsText = string;
		unlock();
	}

	public String getNoProblemsText()
	{
		String copy;
		lock();
		copy = noProblemsText;
		unlock();
		return copy;
	}

	public void setServiceIssue(String string)
	{
		lock();
		this.issueService = string;
		unlock();
	}

	public String getServiceIssue()
	{
		String copy;
		lock();
		copy = issueService;
		unlock();
		return copy;
	}

	public boolean getHeaderSet()
	{
		boolean copy;
		lock();
		copy = headerSet;
		unlock();
		return copy;
	}

	public void setHeader(String header)
	{
		lock();
		this.header = header;
		headerSet = true;
		unlock();
	}

	public String getHeader()
	{
		String copy;
		lock();
		copy = header;
		unlock();
		return copy;
	}

	public void setShowHeader(boolean show)
	{
		lock();
		showHeader = show;
		unlock();
	}

	public boolean getShowHeader()
	{
		boolean copy;
		lock();
		copy = showHeader;
		unlock();
		return copy;
	}

	public void setRandomWebcam(boolean random)
	{
		lock();
		randomWebcam = random;
		unlock();
	}

	public boolean getRandomWebcam()
	{
		boolean copy;
		lock();
		copy = randomWebcam;
		unlock();
		return copy;
	}

	public void setRunGui(boolean runGui)
	{
		lock();
		gui = runGui;
		unlock();
	}

	public boolean getRunGui()
	{
		boolean copy;
		lock();
		copy = gui;
		unlock();
		return copy;
	}

	public void setBrainFileName(String fileName)
	{
		lock();
		predictorBrainFileName = fileName;
		unlock();

		if (getHeaderSet() == false)
			header = "Cr %CRITICAL, Wa %WARNING, Un %UNREACHABLE, Dn %DOWN, %PREDICT, %H:%M";
	}

	public String getBrainFileName()
	{
		String copy;
		lock();
		copy = predictorBrainFileName;
		unlock();
		return copy;
	}

	public void setPerformanceDataFileName(String fileName)
	{
		lock();
		performanceDataFileName = fileName;
		unlock();
	}

	public String getPerformanceDataFileName()
	{
		String copy;
		lock();
		copy = performanceDataFileName;
		unlock();
		return copy;
	}

	public void setExec(String exec)
	{
		lock();
		execCmd = exec;
		unlock();
	}

	public String getExec()
	{
		String copy;
		lock();
		copy = execCmd;
		unlock();
		return copy;
	}

	public void setAdaptImageSize(boolean what)
	{
		lock();
		adaptImgSize = what;
		unlock();
	}

	public boolean getAdaptImageSize()
	{
		boolean copy;
		lock();
		copy = adaptImgSize;
		unlock();
		return copy;
	}

	public void setHttpRememberNHosts(int n)
	{
		lock();
		this.httpRememberNHosts = n;
		unlock();
	}

	public int getHttpRememberNHosts()
	{
		int copy;
		lock();
		copy = httpRememberNHosts;
		unlock();
		return copy;
	}

	public void setCounter(boolean doCounter)
	{
		lock();
		counter = doCounter;
		unlock();
	}

	public boolean getCounter()
	{
		boolean copy;
		lock();
		copy = counter;
		unlock();
		return copy;
	}

	public void setProblemSound(String problemSound)
	{
		lock();
		this.problemSound = problemSound;
		unlock();
	}

	public String getProblemSound()
	{
		String copy;
		lock();
		copy = problemSound;
		unlock();
		return copy;
	}

	public void setHTTPServerListenAdapter(String listenAdapter)
	{
		lock();
		this.listenAdapter = listenAdapter;
		unlock();
	}

	public String getHTTPServerListenAdapter()
	{
		String copy;
		lock();
		copy = listenAdapter;
		unlock();
		return copy;
	}

	public void setHTTPServerListenPort(int listenPort)
	{
		lock();
		this.listenPort = listenPort;
		unlock();
	}

	public int getHTTPServerListenPort()
	{
		int copy;
		lock();
		copy = listenPort;
		unlock();
		return copy;
	}

	public void setBackgroundColorOkStatus(String colorName) throws Exception
	{
		Color color = selectColor(colorName);
		if (color == null)
			throw new Exception("Color " + colorName + " is not known.");
		lock();
		bgColorOkStatus = color;
		bgColorOkStatusName = colorName;
		unlock();
	}

	public Color getBackgroundColorOkStatus()
	{
		Color copy;
		lock();
		copy = bgColorOkStatus;
		unlock();
		return copy;
	}

	public String getBackgroundColorOkStatusName()
	{
		String copy;
		lock();
		copy = bgColorOkStatusName;
		unlock();
		return copy;
	}

	public void setBackgroundColor(String colorName) throws Exception
	{
		Color color = selectColor(colorName);
		if (color == null)
			throw new Exception("Color " + colorName + " is not known.");
		lock();
		backgroundColor = color;
		backgroundColorName = colorName;
		unlock();
	}

	public Color getBackgroundColor()
	{
		Color copy;
		lock();
		copy = backgroundColor;
		unlock();
		return copy;
	}

	public String getBackgroundColorName()
	{
		String copy;
		lock();
		copy = backgroundColorName;
		unlock();
		return copy;
	}

	public void setTextColor(String colorName) throws Exception
	{
		Color color = selectColor(colorName);
		if (color == null)
			throw new Exception("Color " + colorName + " is not known.");
		lock();
		textColor = color;
		textColorName = colorName;
		unlock();
	}

	public Color getTextColor()
	{
		Color copy;
		lock();
		copy = textColor;
		unlock();
		return copy;
	}

	public String getTextColorName()
	{
		String copy;
		lock();
		copy = textColorName;
		unlock();
		return copy;
	}

	public void setNRows(int nRows)
	{
		lock();
		this.nRows = nRows;
		unlock();
	}

	public int getNRows()
	{
		int copy;
		lock();
		copy = nRows;
		unlock();
		return copy;
	}

	public void setSleepTime(int sleepTime)
	{
		lock();
		this.sleepTime = sleepTime;
		unlock();
	}

	public int getSleepTime()
	{
		int copy;
		lock();
		copy = sleepTime;
		unlock();
		return copy;
	}

	public void clearImageList()
	{
		imageFiles = new ArrayList<String>();
	}

	public void addImageUrl(String urlStr, ImageUrlType type)
	{
		lock();
		imageFiles.add(urlStr);
		iut.add(type);
		unlock();
	}

	public void addImageUrl(String url) throws Exception
	{
		ImageUrlType type = ImageUrlType.FILE;

		int space = url.indexOf(" ");
		if (space != -1) {
			String typeStr = url.substring(0, space);
			url = url.substring(space).trim();

			if (typeStr.equalsIgnoreCase("MJPEG"))
				type = ImageUrlType.HTTP_MJPEG;
			else if (typeStr.equalsIgnoreCase("HTTP") || typeStr.equalsIgnoreCase("HTTPS"))
				type = ImageUrlType.HTTP;
			else if (typeStr.equalsIgnoreCase("FILE"))
				type = ImageUrlType.FILE;
			else
				throw new Exception("Unknown image-type: " + typeStr);
		}
		else
		{
			if (url.length() >= 8 && (url.substring(0, 7).equalsIgnoreCase("http://") || url.substring(0, 8).equalsIgnoreCase("https://")))
				type = ImageUrlType.HTTP;
		}
		lock();
		imageFiles.add(url);
		iut.add(type);
		unlock();
	}

	public void removeImageUrl(String url)
	{
		lock();
		for(int index=0; index<imageFiles.size(); index++)
		{
			if (imageFiles.get(index).equals(url))
			{
				imageFiles.remove(index);
				iut.remove(index);
				break;
			}
		}
		unlock();
	}

	public void removeImageUrl(int hash)
	{
		lock();
		for(int index=0; index<imageFiles.size(); index++)
		{
			if (imageFiles.get(index).hashCode() == hash)
			{
				imageFiles.remove(index);
				iut.remove(index);
				break;
			}
		}
		unlock();
	}

	public void removeServer(int hash)
	{
		lock();
		for(int index=0; index<ndsList.size(); index++)
		{

			String parameters = "?";
			if (ndsList.get(index).getType() == NagiosDataSourceType.TCP || ndsList.get(index).getType() == NagiosDataSourceType.ZTCP)
				parameters = ndsList.get(index).getHost() + " " + ndsList.get(index).getPort();
			else if (ndsList.get(index).getType() == NagiosDataSourceType.HTTP)
				parameters = ndsList.get(index).getURL().toString();
			else if (ndsList.get(index).getType() == NagiosDataSourceType.FILE)
				parameters = ndsList.get(index).getFile();

			String serverString = parameters;

			if (serverString.hashCode() == hash)
			{
				ndsList.remove(index);
				break;
			}
		}
		unlock();
	}

	public List<String> getImageUrls()
	{
		List<String> copy = new ArrayList<String>();
		lock();
		for(String current : imageFiles)
			copy.add(current);
		unlock();
		return copy;
	}

	public String getPrefersFilename()
	{
		return prefersFilename;
	}

	public void setServicesFilterExclude(String services) throws Exception
	{
		java.util.List<Pattern> dummy = setFilter(services);
		lock();
		servicesFilterExclude = dummy;
		servicesFilterExcludeList = services;
		unlock();
	}

	public List<Pattern> getServicesFilterExclude()
	{
		List<Pattern> copy;
		lock();
		copy = servicesFilterExclude;
		unlock();
		return copy;
	}

	public String getServicesFilterExcludeList()
	{
		String copy;
		lock();
		copy = servicesFilterExcludeList != null ? servicesFilterExcludeList : "";
		unlock();
		return copy;
	}

	public void setServicesFilterInclude(String services) throws Exception
	{
		java.util.List<Pattern> dummy = setFilter(services);
		lock();
		servicesFilterInclude = dummy;
		servicesFilterIncludeList = services;
		unlock();
	}

	public List<Pattern> getServicesFilterInclude()
	{
		List<Pattern> copy;
		lock();
		copy = servicesFilterInclude;
		unlock();
		return copy;
	}

	public String getServicesFilterIncludeList()
	{
		String copy;
		lock();
		copy = servicesFilterIncludeList != null ? servicesFilterIncludeList : "";
		unlock();
		return copy;
	}

	java.util.List<Pattern> setFilter(String input)
	{
		java.util.List<Pattern> output = new ArrayList<Pattern>();
		String [] elementsArray = input.split(",");

		for(String element : elementsArray)
		{
			System.out.println("setFilter: " + element);
			output.add(Pattern.compile(element));
		}

		return output;
	}

	public void setHostsFilterExclude(String hosts) throws Exception
	{
		java.util.List<Pattern> dummy = setFilter(hosts);
		lock();
		hostsFilterExclude = dummy;
		hostsFilterExcludeList = hosts;
		unlock();
	}

	public List<Pattern> getHostsFilterExclude()
	{
		List<Pattern> copy;
		lock();
		copy = hostsFilterExclude;
		unlock();
		return copy;
	}

	public String getHostsFilterExcludeList()
	{
		String copy;
		lock();
		copy = hostsFilterExcludeList != null ? hostsFilterExcludeList : "";
		unlock();
		return copy;
	}

	public void setHostsFilterInclude(String hosts) throws Exception
	{
		java.util.List<Pattern> dummy = setFilter(hosts);
		lock();
		hostsFilterInclude = dummy;
		hostsFilterIncludeList = hosts;
		unlock();
	}

	public List<Pattern> getHostsFilterInclude()
	{
		List<Pattern> copy;
		lock();
		copy = hostsFilterInclude;
		unlock();
		return copy;
	}

	public String getHostsFilterIncludeList()
	{
		String copy;
		lock();
		copy = hostsFilterIncludeList != null ? hostsFilterIncludeList : "";
		unlock();
		return copy;
	}

	public void setPrefers(String patterns)
	{
		lock();
		prioPatterns = setFilter(patterns);
		prioPatternsList = patterns;
		unlock();
	}

	public List<Pattern> getPrefers()
	{
		List<Pattern> copy;
		lock();
		copy = prioPatterns;
		unlock();
		return copy;
	}

	public String getPrefersList()
	{
		String copy;
		lock();
		copy = prioPatternsList != null ? prioPatternsList : "";
		unlock();
		return copy;
	}

	public List<Pattern> getPrioPatterns()
	{
		List<Pattern> copy;
		lock();
		copy = prioPatterns;
		unlock();
		return copy;
	}

	public void setAlwaysNotify(boolean doNotify)
	{
		lock();
		always_notify = doNotify;
		unlock();
	}

	public boolean getAlwaysNotify()
	{
		boolean copy;
		lock();
		copy = always_notify;
		unlock();
		return copy;
	}

	public void setAlsoAcknowledged(boolean doAcked)
	{
		lock();
		also_acknowledged = doAcked;
		unlock();
	}

	public boolean getAlsoAcknowledged()
	{
		boolean copy;
		lock();
		copy = also_acknowledged;
		unlock();
		return copy;
	}

	public void setFontName(String fontName)
	{
		lock();
		this.fontName = fontName;
		unlock();
	}

	public String getFontName()
	{
		String copy;
		lock();
		copy = fontName;
		unlock();
		return copy;
	}

	public void setCriticalFontName(String fontName)
	{
		lock();
		this.criticalFontName = fontName;
		unlock();
	}

	public String getCriticalFontName()
	{
		String copy;
		lock();
		copy = criticalFontName;
		unlock();
		return copy;
	}

	public void setWarningFontName(String fontName)
	{
		lock();
		this.warningFontName = fontName;
		unlock();
	}

	public String getWarningFontName()
	{
		String copy;
		lock();
		copy = warningFontName;
		unlock();
		return copy;
	}

	public void setSortOrder(String order, boolean numeric, boolean reverse)
	{
		lock();
		this.sortOrder = order;
		this.sortNumeric = numeric;
		this.sortReverse = reverse;
		unlock();
	}

	public String getSortOrder()
	{
		String copy;
		lock();
		copy = sortOrder;
		unlock();
		return copy;
	}

	public boolean getSortOrderNumeric()
	{
		boolean copy;
		lock();
		copy = sortNumeric;
		unlock();
		return copy;
	}

	public boolean getSortOrderReverse()
	{
		boolean copy;
		lock();
		copy = sortReverse;
		unlock();
		return copy;
	}

	public void addNagiosDataSource(NagiosDataSource nds)
	{
		lock();
		ndsList.add(nds);
		unlock();
	}

	public java.util.List<NagiosDataSource> getNagiosDataSources()
	{
		java.util.List<NagiosDataSource> copy = new ArrayList<NagiosDataSource>();
		lock();
		for(NagiosDataSource current : ndsList)
			copy.add(current);
		unlock();
		return copy;
	}

	public void setCamRows(int rows)
	{
		lock();
		this.camRows = rows;
		unlock();
	}

	public int getCamRows()
	{
		int copy;
		lock();
		copy = camRows;
		unlock();
		return copy;
	}

	public void setCamCols(int cols)
	{
		lock();
		this.camCols = cols;
		unlock();
	}

	public int getCamCols()
	{
		int copy;
		lock();
		copy = camCols;
		unlock();
		return copy;
	}

	public void setVerbose(boolean verbose)
	{
		lock();
		this.verbose = verbose;
		unlock();
	}

	public boolean getVerbose()
	{
		boolean copy;
		lock();
		copy = verbose;
		unlock();
		return copy;
	}

	public void setFullscreen(FullScreenMode fullscreen)
	{
		lock();
		this.fullscreen = fullscreen;
		unlock();
	}

	public FullScreenMode getFullscreen()
	{
		FullScreenMode copy;
		lock();
		copy = fullscreen;
		unlock();
		return copy;
	}

	public String getFullscreenName()
	{
		String copy;
		lock();
		copy = "" + fullscreen;
		unlock();
		return copy;
	}

	public void setKeepAspectRatio(boolean kar)
	{
		lock();
		this.keepAspectRatio = kar;
		unlock();
	}

	public boolean getKeepAspectRatio()
	{
		boolean copy;
		lock();
		copy = keepAspectRatio;
		unlock();
		return copy;
	}

	public void setScrollingHeader(boolean sh) {
		lock();
		this.scrollingHeader = sh;
		unlock();
	}

	public boolean getScrollingHeader() {
		boolean copy;
		lock();
		copy = scrollingHeader;
		unlock();
		return copy;
	}

	public void setScrollingPixelsPerSecond(int pps)
	{
		lock();
		this.scrollingPixelsPerSecond = pps;
		unlock();
	}

	public int getScrollingPixelsPerSecond()
	{
		int copy;
		lock();
		copy = scrollingPixelsPerSecond;
		unlock();
		return copy;
	}

	public void setReduceTextWidth(boolean rtw)
	{
		lock();
		this.reduceTextWidth = rtw;
		unlock();
	}

	public boolean getReduceTextWidth()
	{
		boolean copy;
		lock();
		copy = reduceTextWidth;
		unlock();
		return copy;
	}

	public void setWarningTextColor(String colorName) throws Exception
	{
		Color color = selectColor(colorName);
		if (color == null)
			throw new Exception("Color " + colorName + " is not known.");
		lock();
		warningTextColor = color;
		warningTextColorName = colorName;
		unlock();
	}

	public Color getWarningTextColor()
	{
		Color copy;
		lock();
		copy = warningTextColor;
		unlock();
		return copy;
	}

	public String getWarningTextColorName()
	{
		String copy;
		lock();
		copy = warningTextColorName;
		unlock();
		return copy;
	}

	public void setCriticalTextColor(String colorName) throws Exception
	{
		Color color = selectColor(colorName);
		if (color == null)
			throw new Exception("Color " + colorName + " is not known.");
		lock();
		criticalTextColor = color;
		criticalTextColorName = colorName;
		unlock();
	}

	public Color getCriticalTextColor()
	{
		Color copy;
		lock();
		copy = criticalTextColor;
		unlock();
		return copy;
	}

	public String getCriticalTextColorName()
	{
		String copy;
		lock();
		copy = criticalTextColorName;
		unlock();
		return copy;
	}

	public void setRowBorder(boolean rb)
	{
		lock();
		this.rowBorder = rb;
		unlock();
	}

	public boolean getRowBorder()
	{
		boolean copy;
		lock();
		copy = rowBorder;
		unlock();
		return copy;
	}

	public void setRowBorderColor(String colorName) throws Exception
	{
		Color color = selectColor(colorName);
		if (color == null)
			throw new Exception("Color " + colorName + " is not known.");
		lock();
		rowBorderColor = color;
		rowBorderColorName = colorName;
		unlock();
	}

	public Color getRowBorderColor()
	{
		Color copy;
		lock();
		copy = rowBorderColor;
		unlock();
		return copy;
	}

	public String getRowBorderColorName()
	{
		String copy;
		lock();
		copy = rowBorderColorName;
		unlock();
		return copy;
	}

	public void setAntiAlias(boolean aa)
	{
		lock();
		this.antiAlias = aa;
		unlock();
	}

	public boolean getAntiAlias()
	{
		boolean copy;
		lock();
		copy = antiAlias;
		unlock();
		return copy;
	}

	public boolean getAlsoScheduledDowntime()
	{
		boolean copy;
		lock();
		copy = alsoScheduledDowntime;
		unlock();
		return copy;
	}

	public void setAlsoScheduledDowntime(boolean asd)
	{
		lock();
		alsoScheduledDowntime = asd;
		unlock();
	}

	public boolean getAlsoSoftState()
	{
		boolean copy;
		lock();
		copy = alsoSoftState;
		unlock();
		return copy;
	}

	public void setAlsoSoftState(boolean ass)
	{
		lock();
		alsoSoftState = ass;
		unlock();
	}

	public boolean getAlsoDisabledActiveChecks()
	{
		boolean copy;
		lock();
		copy = alsoDisabledActiveChecks;
		unlock();
		return copy;
	}

	public void setAlsoDisabledActiveChecks(boolean adas)
	{
		lock();
		alsoDisabledActiveChecks = adas;
		unlock();
	}

	public boolean getShowServicesForHostWithProblems() {
		boolean copy;
		lock();
		copy = showProblemHostServices;
		unlock();
		return copy;
	}

	public void setShowServicesForHostWithProblems(boolean ssfhwp) {
		lock();
		showProblemHostServices = ssfhwp;
		unlock();
	}

	public void setNProblemCols(int n) {
		lock();
		this.problemCols = n;
		unlock();
	}

	public int getNProblemCols() {
		int copy;
		lock();
		copy = problemCols;
		unlock();
		return copy;
	}

	public boolean getFlexibleNColumns() {
		boolean copy;
		lock();
		copy = flexibleNColumns;
		unlock();
		return copy;
	}

	public void setFlexibleNColumns(boolean fnc)
	{
		lock();
		flexibleNColumns = fnc;
		unlock();
	}

	public void setCriticalBgColor(String colorName) throws Exception
	{
		Color color = selectColor(colorName);
		if (color == null)
			throw new Exception("Color " + colorName + " is not known.");
		lock();
		criticalBgColor = color;
		criticalBgColorName = colorName;
		unlock();
	}

	public Color getCriticalBgColor()
	{
		Color copy;
		lock();
		copy = criticalBgColor;
		unlock();
		return copy;
	}

	public String getCriticalBgColorName()
	{
		String copy;
		lock();
		copy = criticalBgColorName;
		unlock();
		return copy;
	}

	public void setCriticalBgColorSoft(String colorName) throws Exception
	{
		Color color = selectColor(colorName);
		if (color == null)
			throw new Exception("Color " + colorName + " is not known.");
		lock();
		criticalBgColorSoft = color;
		criticalBgColorNameSoft = colorName;
		unlock();
	}

	public Color getCriticalBgColorSoft()
	{
		Color copy;
		lock();
		copy = criticalBgColorSoft;
		unlock();
		return copy;
	}

	public String getCriticalBgColorNameSoft()
	{
		String copy;
		lock();
		copy = criticalBgColorNameSoft;
		unlock();
		return copy;
	}

	public void setWarningBgColor(String colorName) throws Exception
	{
		Color color = selectColor(colorName);
		if (color == null)
			throw new Exception("Color " + colorName + " is not known.");
		lock();
		warningBgColor = color;
		warningBgColorName = colorName;
		unlock();
	}

	public Color getWarningBgColor()
	{
		Color copy;
		lock();
		copy = warningBgColor;
		unlock();
		return copy;
	}

	public String getWarningBgColorName()
	{
		String copy;
		lock();
		copy = warningBgColorName;
		unlock();
		return copy;
	}

	public void setWarningBgColorSoft(String colorName) throws Exception
	{
		Color color = selectColor(colorName);
		if (color == null)
			throw new Exception("Color " + colorName + " is not known.");
		lock();
		warningBgColorSoft = color;
		warningBgColorNameSoft = colorName;
		unlock();
	}

	public Color getWarningBgColorSoft()
	{
		Color copy;
		lock();
		copy = warningBgColorSoft;
		unlock();
		return copy;
	}

	public String getWarningBgColorNameSoft()
	{
		String copy;
		lock();
		copy = warningBgColorNameSoft;
		unlock();
		return copy;
	}

	public void setNagiosUnknownBgColor(String colorName) throws Exception
	{
		Color color = selectColor(colorName);
		if (color == null)
			throw new Exception("Color " + colorName + " is not known.");
		lock();
		nagiosUnknownBgColor = color;
		nagiosUnknownBgColorName = colorName;
		unlock();
	}

	public Color getNagiosUnknownBgColor()
	{
		Color copy;
		lock();
		copy = nagiosUnknownBgColor;
		unlock();
		return copy;
	}

	public String getNagiosUnknownBgColorName()
	{
		String copy;
		lock();
		copy = nagiosUnknownBgColorName;
		unlock();
		return copy;
	}

	public boolean getMaxQualityGraphics()
	{
		boolean copy;
		lock();
		copy = maxQualityGraphics;
		unlock();
		return copy;
	}

	public void setMaxQualityGraphics(boolean mqg)
	{
		lock();
		maxQualityGraphics = mqg;
		unlock();
	}

	public void setAllowHTTPCompression(boolean allow)
	{
		lock();
		allowCompression = allow;
		unlock();
	}

	public boolean getAllowHTTPCompression()
	{
		boolean copy;
		lock();
		copy = allowCompression;
		unlock();
		return copy;
	}

	public float getTransparency()
	{
		return transparency;
	}

	public void setTransparency(float t)
	{
		lock();
		transparency = t;
		unlock();
	}

	public float getHeaderTransparency()
	{
		return headerTransparency;
	}

	public void setHeaderTransparency(float t)
	{
		lock();
		headerTransparency = t;
		unlock();
	}

	public void setDisableHTTPFileselect()
	{
		lock();
		disableHTTPFileselect = true;
		unlock();
	}

	public boolean getDisableHTTPFileselect()
	{
		boolean copy;
		lock();
		copy = disableHTTPFileselect;
		unlock();
		return copy;
	}

	public void setShowFlapping(boolean sf)
	{
		lock();
		showFlapping = sf;
		unlock();
	}

	public boolean getShowFlapping()
	{
		boolean copy;
		lock();
		copy = showFlapping;
		unlock();
		return copy;
	}

	public void setSparkLineWidth(int width)
	{
		lock();
		sparkLineWidth = width;
		unlock();
	}

	public int getSparkLineWidth()
	{
		int copy;
		lock();
		copy = sparkLineWidth;
		unlock();
		return copy;
	}

	public SparklineGraphMode getSparklineGraphMode()
	{
		SparklineGraphMode copy;
		lock();
		copy = sparklineGraphMode;
		unlock();
		return copy;
	}

	public void setSparklineGraphMode(SparklineGraphMode newMode)
	{
		lock();
		sparklineGraphMode = newMode;
		unlock();
	}

	public void setNoNetworkChange(boolean value)
	{
		lock();
		noNetworkChange = value;
		unlock();
	}

	public boolean getNoNetworkChange()
	{
		boolean copy;
		lock();
		copy = noNetworkChange;
		unlock();
		return copy;
	}

	public void setGraphColor(String colorName) throws Exception
	{
		Color color = selectColor(colorName);
		if (color == null)
			throw new Exception("Color " + colorName + " is not known.");
		lock();
		graphColor = color;
		graphColorName = colorName;
		unlock();
	}

	public Color getGraphColor()
	{
		Color copy;
		lock();
		copy = graphColor;
		unlock();
		return copy;
	}

	public String getGraphColorName()
	{
		String copy;
		lock();
		copy = graphColorName;
		unlock();
		return copy;
	}

	public boolean getScrollIfNotFit()
	{
		boolean copy;
		lock();
		copy = scrollIfNotFit;
		unlock();
		return copy;
	}

	public void setScrollIfNotFit(boolean what)
	{
		lock();
		scrollIfNotFit = what;
		unlock();
	}

	public void setCounterPosition(String where) throws Exception
	{
		Position newPosition = null;
		if (where.equalsIgnoreCase("upper-left"))
			newPosition = Position.UPPER_LEFT;
		else if (where.equalsIgnoreCase("upper-right"))
			newPosition = Position.UPPER_RIGHT;
		else if (where.equalsIgnoreCase("lower-left"))
			newPosition = Position.LOWER_LEFT;
		else if (where.equalsIgnoreCase("lower-right"))
			newPosition = Position.LOWER_RIGHT;
		else if (where.equalsIgnoreCase("center"))
			newPosition = Position.CENTER;
		else if (where.equalsIgnoreCase("nowhere"))
			newPosition = Position.NONE;
		if (newPosition == null)
			throw new Exception("Position " + where + " is not understood");
		lock();
		counterPosition = newPosition;
		unlock();
	}

	public Position getCounterPosition()
	{
		Position copy;
		lock();
		copy = counterPosition;
		unlock();
		return copy;
	}

	public String getCounterPositionName()
	{
		Position copy;
		lock();
		copy = counterPosition;
		unlock();
		return copy.toString();
	}

	public String getLineScrollSplitterString() {
		String copy;
		lock();
		copy = lineScrollSplitterString;
		unlock();
		return copy;
	}

	public List<Integer> getLineScrollSplitter() {
		List<Integer> copy;
		lock();
		copy = lineScrollSplitter;
		unlock();
		assert copy != null;
		return copy;
	}

	public void setLineScrollSplitter(String what) {
		lock();
		lineScrollSplitterString = what;
		lineScrollSplitter = new ArrayList<Integer>();
		if (what != null) {
			String [] parts = what.split(" ");
			for(String cur : parts)
				lineScrollSplitter.add(Integer.valueOf(cur));
		}
		unlock();
	}

	public void setNoProblemsTextPosition(String where) throws Exception {
		Position newPosition = null;
		if (where.equalsIgnoreCase("upper-left"))
			newPosition = Position.UPPER_LEFT;
		else if (where.equalsIgnoreCase("upper-right"))
			newPosition = Position.UPPER_RIGHT;
		else if (where.equalsIgnoreCase("lower-left"))
			newPosition = Position.LOWER_LEFT;
		else if (where.equalsIgnoreCase("lower-right"))
			newPosition = Position.LOWER_RIGHT;
		else if (where.equalsIgnoreCase("center"))
			newPosition = Position.CENTER;
		else if (where.equalsIgnoreCase("nowhere"))
			newPosition = Position.NONE;
		if (newPosition == null)
			throw new Exception("Position " + where + " is not understood");
		lock();
		noProblemsTextPosition = newPosition;
		unlock();
	}

	public Position getNoProblemsTextPosition()
	{
		Position copy;
		lock();
		copy = noProblemsTextPosition;
		unlock();
		return copy;
	}

	public String getNoProblemsTextPositionName()
	{
		Position copy;
		lock();
		copy = noProblemsTextPosition;
		unlock();
		return copy.toString();
	}

	public void setAuthentication(boolean doAuth)
	{
		lock();
		authenticate = doAuth;
		unlock();
	}

	public boolean getAuthentication() {
		boolean copy;
		lock();
		copy = authenticate;
		unlock();
		return copy;
	}

	public int getWebSessionExpire() {
		int copy;
		lock();
		copy = webExpireTime;
		unlock();
		return copy;
	}

	public void setWebSessionExpire(int to) {
		lock();
		webExpireTime = to;
		unlock();
	}

	public String getWebUsername() {
		String copy;
		lock();
		copy = webUsername;
		unlock();
		return copy;
	}

	public void setWebUsername(String newName) {
		lock();
		webUsername = newName;
		unlock();
	}

	public String getWebPassword() {
		String copy;
		lock();
		copy = webPassword;
		unlock();
		return copy;
	}

	public void setWebPassword(String newPassword) {
		lock();
		webPassword = newPassword;
		unlock();
	}

	public String getLatencyFile() {
		String copy;
		lock();
		copy = latencyFile;
		unlock();
		return copy;
	}

	public void setLatencyFile(String lf) {
		lock();
		latencyFile = lf;
		unlock();
	}

	public void setLogo(String file) {
		lock();
		logo = file;
		unlock();
	}

	public String getLogo() {
		String copy;
		lock();
		copy = logo;
		unlock();
		return copy;
	}

	public void setStateProblemsText(String string) {
		lock();
		problemStateString = string;
		unlock();
	}

	public String getStateProblemsText() {
		String copy;
		lock();
		copy = problemStateString;
		unlock();
		return copy;
	}

	public boolean getHeaderAlwaysBGColor() {
		boolean copy;
		lock();
		copy = headerAlwaysBGColor;
		unlock();
		return copy;
	}

	public void setHeaderAlwaysBGColor(boolean when) {
		lock();
		headerAlwaysBGColor = when;
		unlock();
	}

	public boolean getSetBgColorToState() {
		boolean copy;
		lock();
		copy = bgColorToState;
		unlock();
		return copy;
	}

	public void setSetBgColorToState(boolean state) {
		lock();
		bgColorToState = state;
		unlock();
	}

	public Position getLogoPosition() {
		Position copy;
		lock();
		copy = logoPosition;
		unlock();
		return copy;
	}

	public String getLogoPositionName() {
		Position copy;
		lock();
		copy = logoPosition;
		unlock();
		return copy.toString();
	}

	public void setLogoPosition(Position newPosition) throws Exception {
		if (newPosition == Position.CENTER)
			throw new Exception("Logo position cannot be center");
		lock();
		logoPosition = newPosition;
		unlock();
	}

	public void setLogoPosition(String newPosition) throws Exception {
		Position value = null;
		if (newPosition.equalsIgnoreCase("left") || newPosition.equalsIgnoreCase("upper-left"))
			value = Position.UPPER_LEFT;
		else if (newPosition.equalsIgnoreCase("right") || newPosition.equalsIgnoreCase("upper-right"))
			value = Position.UPPER_RIGHT;
		else if (newPosition.equalsIgnoreCase("lower-left"))
			value = Position.LOWER_LEFT;
		else if (newPosition.equalsIgnoreCase("lower-right"))
			value = Position.LOWER_RIGHT;
		else
			throw new Exception("Logo position cannot be " + newPosition);
		lock();
		logoPosition = value;
		unlock();
	}

	public int getUpperRowBorderHeight() {
		int copy;
		lock();
		copy = upperRowBorderHeight;
		unlock();
		return copy;
	}

	public void setUpperRowBorderHeight(int height) {
		lock();
		upperRowBorderHeight = height;
		unlock();
	}

	public Color getBackgroundColorFadeTo() {
		Color copy;
		lock();
		copy = bgColorFadeTo;
		unlock();
		return copy;
	}

	public void setBackgroundColorFadeTo(String colorName) throws Exception {
		if (colorName == null) {
			lock();
			bgColorFadeTo = null;
			bgColorFadeToName = null;
			unlock();
		}
		else {
			Color color = selectColor(colorName);
			if (color == null)
				throw new Exception("Color " + colorName + " is not known.");
			lock();
			bgColorFadeTo = color;
			bgColorFadeToName = colorName;
			unlock();
		}
	}

	public String getBackgroundColorFadeToName() {
		String copy;
		lock();
		copy = bgColorFadeToName;
		unlock();
		return copy;
	}

	public Color getProblemRowGradient() {
		Color copy;
		lock();
		copy = problemRowGradient;
		unlock();
		return copy;
	}

	public void setProblemRowGradient(String colorName) throws Exception {
		if (colorName == null) {
			lock();
			problemRowGradient = null;
			problemRowGradientName = null;
			unlock();
		}
		else {
			Color color = selectColor(colorName);
			if (color == null)
				throw new Exception("Color " + colorName + " is not known.");
			lock();
			problemRowGradient = color;
			problemRowGradientName = colorName;
			unlock();
		}
	}

	public String getProblemRowGradientName() {
		String copy;
		lock();
		copy = problemRowGradientName;
		unlock();
		return copy;
	}

	public boolean getDrawProblemServiceSplitLine() {
		boolean copy;
		lock();
		copy = drawProblemServiceSplitLine;
		unlock();
		return copy;
	}

	public void setDrawProblemServiceSplitLine(boolean on) {
		lock();
		drawProblemServiceSplitLine = on;
		unlock();
	}

	public boolean getAllowAllSSL() {
		boolean copy;
		lock();
		copy = allowAllSSL;
		unlock();
		return copy;
	}

	public void setAllowAllSSL(boolean newSetting) {
		lock();
		allowAllSSL = newSetting;
		unlock();

		CoffeeSaint.allowAllSSL();
	}

	public String getUseScreen() {
		String copy;
		lock();
		copy = useScreen;
		unlock();
		return copy;
	}

	public void setUseScreen(String screen) {
		lock();
		useScreen = screen;
		unlock();
	}

	public String getFooter() {
		String copy;
		lock();
		copy = footer;
		unlock();
		return copy;
	}

	public void setFooter(String line) {
		lock();
		footer = line;
		unlock();
	}

	public void setScrollingFooter(boolean sh) {
		lock();
		this.scrollingFooter = sh;
		unlock();
	}

	public boolean getScrollingFooter() {
		boolean copy;
		lock();
		copy = scrollingFooter;
		unlock();
		return copy;
	}

	public boolean getNoProblemsTextBg() {
		boolean copy;
		lock();
		copy = noProbTextBg;
		unlock();
		return copy;
	}

	public void setNoProblemsTextBg(boolean bg) {
		lock();
		noProbTextBg = bg;
		unlock();
	}

	public boolean getHostScheduledDowntimeShowServices() {
		boolean copy;
		lock();
		copy = host_scheduled_downtime_show_services;
		unlock();
		return copy;
	}

	public void setHostScheduledDowntimeShowServices(boolean setting) {
		lock();
		host_scheduled_downtime_show_services = setting;
		unlock();
	}

	public boolean getHostAcknowledgedShowServices() {
		boolean copy;
		lock();
		copy = host_acknowledged_show_services;
		unlock();
		return copy;
	}

	public void setHostAcknowledgedShowServices(boolean setting) {
		lock();
		host_acknowledged_show_services = setting;
		unlock();
	}

	public boolean getHostSDOrAckShowServices() {
		boolean copy;
		lock();
		copy = host_scheduled_downtime_or_ack_show_services;
		unlock();
		return copy;
	}

	public void setHostSDOrAckShowServices(boolean setting) {
		lock();
		host_scheduled_downtime_or_ack_show_services = setting;
		unlock();
	}

	public void setMaxCheckAge(long setting) {
		lock();
		maxCheckAge = setting;
		unlock();
	}

	public long getMaxCheckAge() {
		long copy;
		lock();
		copy = maxCheckAge;
		unlock();
		return copy;
	}

	public List<ImageUrlType> getImageUrlTypes() {
		// FIXME make a copy, this is not threadsafe
		List<ImageUrlType> iutCopy;
		lock();
		iutCopy = iut;
		unlock();
		return iutCopy;
	}

	public boolean getShowFlappingIcon() {
		boolean copy;
		lock();
		copy = showFlappingIcon;
		unlock();
		return copy;
	}

	public void setShowFlappingIcon(boolean value) {
		lock();
		showFlappingIcon = value;
		unlock();
	}

	public void setProxyHost(String host) {
		Properties properties = System.getProperties();
		properties.put("http.proxyHost", host);
	}

	public void setProxyPort(int port) {
		Properties properties = System.getProperties();
		properties.put("http.proxyPort", "" + port);
	}

	public String getProxyHost() {
		Properties properties = System.getProperties();
		return (String)properties.get("http.proxyHost");
	}

	public int getProxyPort() {
		Properties properties = System.getProperties();
		return Integer.valueOf((String)properties.get("http.proxyPort"));
	}

	public void setDoubleBuffering(boolean to) {
		lock();
		doubleBuffering = to;
		unlock();
	}

	public boolean getDoubleBuffering() {
		boolean copy;
		lock();
		copy = doubleBuffering;
		unlock();
		return copy;
	}

	public void setDisplayUnknown(boolean du) {
		lock();
		displayUnknown = du;
		unlock();
	}

	public boolean getDisplayUnknown() {
		boolean copy;
		lock();
		copy = displayUnknown;
		unlock();
		return copy;
	}

	public void setDisplayDown(boolean dd) {
		lock();
		displayDown = dd;
		unlock();
	}

	public boolean getDisplayDown() {
		boolean copy;
		lock();
		copy = displayDown;
		unlock();
		return copy;
	}

	public void setWebcamTimeout(int to) {
		lock();
		webcamTimeout = to;
		unlock();
	}

	public int getWebcamTimeout() {
		int copy;
		lock();
		copy = webcamTimeout;
		unlock();
		return copy;
	}

	public void setFlash(boolean f) {
		lock();
		flash = f;
		unlock();
	}

	public boolean getFlash() {
		boolean copy;
		lock();
		copy = flash;
		unlock();
		return copy;
	}

	public int getMinRowHeight() {
		int copy;
		lock();
		copy = minRowHeight;
		unlock();
		return copy;
	}

	public void setMinRowHeight(int value) {
		lock();
		minRowHeight = value;
		unlock();
	}

	public void setLDAPBaseDN(String value) {
		lock();
		ldapBaseDn = value;
		unlock();
	}

	public String getLDAPBaseDN() {
		String copy;
		lock();
		copy = ldapBaseDn;
		unlock();
		return copy;
	}

	public void setLDAPUrl(String value) {
		lock();
		ldapUrl = value;
		unlock();
	}

	public String getLDAPUrl() {
		String copy;
		lock();
		copy = ldapUrl;
		unlock();
		return copy;
	}
}
