/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import com.vanheusden.nagios.*;

import java.awt.Color;
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
import java.util.regex.Pattern;

public class Config
{
	// parameters set/get by this object
	private java.util.List<NagiosDataSource> ndsList = new ArrayList<NagiosDataSource>();
	private int nRows;
	private int sleepTime;
	private String fontName;
	private String listenAdapter = "0.0.0.0";
	private int listenPort = -1;
	private java.util.List<Pattern> prioPatterns;
	private String prefersFilename;
	private boolean always_notify, also_acknowledged;
	private Color backgroundColor;
	private String backgroundColorName;
	private Color fontColor;
	private String fontColorName;
	private String bgColorOkStatusName;
	private Color bgColorOkStatus;
	private String problemSound = null;
	private boolean counter;
	private java.util.List<String> imageFiles = new ArrayList<String>();
	private boolean adaptImgSize;
	private String execCmd;
	private String predictorBrainFileName;
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
		always_notify = false;
		also_acknowledged = false;
		backgroundColor = Color.GRAY;
		backgroundColorName = "GRAY";
		fontColor = Color.BLACK;
		fontColorName = "BLACK";
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

		unlock();
	}

	public Config()
	{
		setDefaultParameterValues();
		initColors();
		initSortFields();
	}

	public Config(String fileName) throws Exception
	{
		setDefaultParameterValues();
		initColors();
		initSortFields();

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
			while((line = in.readLine()) != null)
			{
				int is = line.indexOf("=");
				lineNr++;
				if (is == -1)
					throw new Exception("Error on line " + lineNr + ": malformed line.");

				String name = line.substring(0, is).trim();
				String data = line.substring(is + 1).trim();

				if (name.equals("config"))
					loadConfig(data);
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
						nds = new NagiosDataSource(new URL(parameters[2]), nv);
					else if (type.equalsIgnoreCase("file"))
						nds = new NagiosDataSource(parameters[2], nv);
					else if (type.equalsIgnoreCase("tcp"))
					{
						String host = parameters[2];
						int port = Integer.valueOf(parameters[3]);
						nds = new NagiosDataSource(host, port, nv);
					}
					else
						throw new Exception("Data source-type '" + type + "' not understood.");

					addNagiosDataSource(nds);
				}
				else if (name.equals("predict"))
					setBrainFileName(data);
				else if (name.equals("exec"))
					setExec(data);
				else if (name.equals("adapt-img"))
					setAdaptImageSize(data.equalsIgnoreCase("true") ? true : false);
				else if (name.equals("random-img"))
					setRandomWebcam(data.equalsIgnoreCase("true") ? true : false);
				else if (name.equals("no-gui"))
					setRunGui(!(data.equalsIgnoreCase("true") ? true : false));
				else if (name.equals("header"))
					setHeader(data);
				else if (name.equals("host-issue"))
					setHostIssue(data);
				else if (name.equals("service-issue"))
					setServiceIssue(data);
				else if (name.equals("counter"))
					setCounter(data.equalsIgnoreCase("true") ? true : false);
				else if (name.equals("verbose"))
					setVerbose(data.equalsIgnoreCase("true") ? true : false);
				else if (name.equals("sound"))
					setProblemSound(data);
				else if (name.equals("listen-port"))
					setHTTPServerListenPort(Integer.valueOf(data));
				else if (name.equals("listen-adapter"))
					setHTTPServerListenAdapter(data);
				else if (name.equals("bgcolor"))
					setBackgroundColor(data);
				else if (name.equals("textcolor"))
					setTextColor(data);
				else if (name.equals("bgcolorok"))
					setBackgroundColorOkStatus(data);
				else if (name.equals("nrows"))
					setNRows(Integer.valueOf(data));
				else if (name.equals("interval"))
					setSleepTime(Integer.valueOf(data));
				else if (name.equals("image"))
					addImageUrl(data);
				else if (name.equals("cam-rows"))
					setCamRows(Integer.valueOf(data));
				else if (name.equals("cam-cols"))
					setCamCols(Integer.valueOf(data));
				else if (name.equals("prefer"))
					loadPrefers(data);
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
					setAlwaysNotify(data.equalsIgnoreCase("true") ? true : false);
				else if (name.equals("also-acknowledged"))
					setAlsoAcknowledged(data.equalsIgnoreCase("true") ? true : false);
				else if (name.equals("show-header"))
					setShowHeader(data.equalsIgnoreCase("true") ? true : false);
				else if (name.equals("font"))
					setFontName(data);
				else
					throw new Exception("Unknown parameter on line " + lineNr);
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

	public void writeConfig(String fileName) throws Exception
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));

		writeLine(out, "predict = " + getBrainFileName());
		if (getExec() != null)
			writeLine(out, "exec = " + getExec());
		writeLine(out, "adapt-img = " + (getAdaptImageSize() ? "true" : "false"));
		writeLine(out, "random-img = " + (getRandomWebcam() ? "true" : "false"));
		writeLine(out, "counter = " + (getCounter() ? "true" : "false"));
		if (getProblemSound() != null)
			writeLine(out, "sound = " + getProblemSound());
		writeLine(out, "listen-port = " + getHTTPServerListenPort());
		writeLine(out, "listen-adapter = " + getHTTPServerListenAdapter());
		writeLine(out, "bgcolor = " + getBackgroundColorName());
		writeLine(out, "textcolor = " + getTextColorName());
		writeLine(out, "bgcolorok = " + getBackgroundColorOkStatusName());
		writeLine(out, "nrows = " + getNRows());
		writeLine(out, "interval = " + getSleepTime());
		for(String imgUrl : getImageUrls())
			writeLine(out, "image = " + imgUrl);
		if (getPrefersFilename() != null)
			writeLine(out, "prefer = " + getPrefersFilename());
		writeLine(out, "always-notify = " + (getAlwaysNotify() ? "true" : "false"));
		writeLine(out, "also-acknowledged = " + (getAlsoAcknowledged() ? "true" : "false"));
		writeLine(out, "font = " + getFontName());
		writeLine(out, "verbose = " + (getVerbose() ? "true" : "false"));
		writeLine(out, "no-gui = " + (!getRunGui() ? "true" : "false"));
		if (getHeaderSet() == true)
			writeLine(out, "header = " + getHeader());
		writeLine(out, "show-header = " + (getShowHeader() ? "true" : "false"));
		writeLine(out, "host-issue = " + getHostIssue());
		writeLine(out, "service-issue = " + getServiceIssue());
		String sort = "";
		if (getSortOrderNumeric())
			sort += "numeric ";
		if (getSortOrderReverse())
			sort += "reverse ";
		sort += getSortOrder();
		writeLine(out, "sort-order = " + sort);

		for(NagiosDataSource dataSource : getNagiosDataSources())
		{
			String type = "?";
			if (dataSource.getType() == NagiosDataSourceType.TCP)
				type = "tcp";
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
			if (dataSource.getType() == NagiosDataSourceType.TCP)
				parameters = dataSource.getHost() + " " + dataSource.getPort();
			else if (dataSource.getType() == NagiosDataSourceType.HTTP)
				parameters = dataSource.getURL().toString();
			else if (dataSource.getType() == NagiosDataSourceType.FILE)
				parameters = dataSource.getFile();

			writeLine(out, "source = " + type + " " + version + " " + parameters);
		}

		writeLine(out, "cam-rows = " + getCamRows());
		writeLine(out, "cam-cols = " + getCamCols());

		out.close();
	}

	public String getConfigFilename()
	{
		return configFilename;
	}

	public Color selectColor(String name)
	{
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
		colorPairs.add(new ColorPair("black", Color.BLACK));
		colorPairs.add(new ColorPair("blue", Color.BLUE));
		colorPairs.add(new ColorPair("cyan", Color.CYAN));
		colorPairs.add(new ColorPair("dark_gray", Color.DARK_GRAY));
		colorPairs.add(new ColorPair("gray", Color.GRAY));
		colorPairs.add(new ColorPair("green", Color.GREEN));
		colorPairs.add(new ColorPair("light_gray", Color.LIGHT_GRAY));
		colorPairs.add(new ColorPair("magenta", Color.MAGENTA));
		colorPairs.add(new ColorPair("orange", Color.ORANGE));
		colorPairs.add(new ColorPair("pink", Color.PINK));
		colorPairs.add(new ColorPair("red", Color.RED));
		colorPairs.add(new ColorPair("white", Color.WHITE));
		colorPairs.add(new ColorPair("yellow", Color.YELLOW));
	}

	public List<ColorPair> getColors()
	{
		return colorPairs;
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
		fontColor = color;
		fontColorName = colorName;
		unlock();
	}

	public Color getTextColor()
	{
		Color copy;
		lock();
		copy = fontColor;
		unlock();
		return copy;
	}

	public String getTextColorName()
	{
		String copy;
		lock();
		copy = fontColorName;
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

	public void addImageUrl(String url)
	{
		lock();
		imageFiles.add(url);
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
                        if (ndsList.get(index).getType() == NagiosDataSourceType.TCP)
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

	public void loadPrefers(String fileName) throws Exception
	{
		lock();

		prioPatterns = new ArrayList<Pattern>();
		prefersFilename = fileName;

		try
		{
			String line;
			BufferedReader in = new BufferedReader(new FileReader(fileName));

			while((line = in.readLine()) != null)
				prioPatterns.add(Pattern.compile(line));

			in.close();
		}
		catch(Exception e)
		{
			throw e;
		}
		finally
		{
			unlock();
		}
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
}
