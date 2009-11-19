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
import java.util.List;
import java.util.regex.Pattern;

public class Config
{
	// parameters set/get by this object
	private String host = null, file = null;
	private URL url = null;
	private int port = 33333;
	private int nRows;
	private int sleepTime;
	private NagiosVersion nagiosVersion = NagiosVersion.V3;
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
	// global lock shielding all parameters
	private Semaphore configSemaphore = new Semaphore(1);
	//
	String configFilename;
	//
	private List<ColorPair> colorPairs;

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

		unlock();
	}

	public Config()
	{
		setDefaultParameterValues();
		initColors();
	}

	public Config(String fileName) throws Exception
	{
		setDefaultParameterValues();
		initColors();

		loadConfig(fileName);
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

				if (name.compareTo("config") == 0)
					loadConfig(data);
				else if (name.compareTo("predict") == 0)
					setBrainFileName(data);
				else if (name.compareTo("exec") == 0)
					setExec(data);
				else if (name.compareTo("adapt-img") == 0)
					setAdaptImageSize(data.equalsIgnoreCase("true") ? true : false);
				else if (name.compareTo("random-img") == 0)
					setRandomWebcam(data.equalsIgnoreCase("true") ? true : false);
				else if (name.compareTo("no-gui") == 0)
					setRunGui(!(data.equalsIgnoreCase("true") ? true : false));
				else if (name.compareTo("host") == 0)
					setNagiosStatusHost(data);
				else if (name.compareTo("header") == 0)
					setHeader(data);
				else if (name.compareTo("host-issue") == 0)
					setHostIssue(data);
				else if (name.compareTo("service-issue") == 0)
					setServiceIssue(data);
				else if (name.compareTo("port") == 0)
					setNagiosStatusPort(Integer.valueOf(data));
				else if (name.compareTo("file") == 0)
					setNagiosStatusFile(data);
				else if (name.compareTo("url") == 0)
					setNagiosStatusURL(data);
				else if (name.compareTo("counter") == 0)
					setCounter(data.equalsIgnoreCase("true") ? true : false);
				else if (name.compareTo("sound") == 0)
					setProblemSound(data);
				else if (name.compareTo("listen-port") == 0)
					setHTTPServerListenPort(Integer.valueOf(data));
				else if (name.compareTo("listen-adapter") == 0)
					setHTTPServerListenAdapter(data);
				else if (name.compareTo("bgcolor") == 0)
					setBackgroundColor(data);
				else if (name.compareTo("textcolor") == 0)
					setTextColor(data);
				else if (name.compareTo("bgcolorok") == 0)
					setBackgroundColorOkStatus(data);
				else if (name.compareTo("nrows") == 0)
					setNRows(Integer.valueOf(data));
				else if (name.compareTo("interval") == 0)
					setSleepTime(Integer.valueOf(data));
				else if (name.compareTo("version") == 0)
					setNagiosStatusVersion(data);
				else if (name.compareTo("image") == 0)
					addImageUrl(data);
				else if (name.compareTo("prefer") == 0)
					loadPrefers(data);
				else if (name.compareTo("always-notify") == 0)
					setAlwaysNotify(data.equalsIgnoreCase("true") ? true : false);
				else if (name.compareTo("also-acknowledged") == 0)
					setAlsoAcknowledged(data.equalsIgnoreCase("true") ? true : false);
				else if (name.compareTo("show-header") == 0)
					setShowHeader(data.equalsIgnoreCase("true") ? true : false);
				else if (name.compareTo("font") == 0)
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
		if (getNagiosStatusHost() != null)
		{
			writeLine(out, "host = " + getNagiosStatusHost());
			writeLine(out, "port = " + getNagiosStatusPort());
		}
		if (getNagiosStatusFile() != null)
			writeLine(out, "file = " + getNagiosStatusFile());
		if (getNagiosStatusURL() != null)
			writeLine(out, "url = " + getNagiosStatusURL());
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
		if (getNagiosStatusVersion() == NagiosVersion.V1)
			writeLine(out, "version = 1");
		else if (getNagiosStatusVersion() == NagiosVersion.V2)
			writeLine(out, "version = 2");
		else if (getNagiosStatusVersion() == NagiosVersion.V3)
			writeLine(out, "version = 3");
		for(String imgUrl : getImageUrls())
			writeLine(out, "image = " + imgUrl);
		if (getPrefersFilename() != null)
			writeLine(out, "prefer = " + getPrefersFilename());
		writeLine(out, "always-notify = " + (getAlwaysNotify() ? "true" : "false"));
		writeLine(out, "also-acknowledged = " + (getAlsoAcknowledged() ? "true" : "false"));
		writeLine(out, "font = " + getFontName());
		writeLine(out, "no-gui = " + (!getRunGui() ? "true" : "false"));
		if (getHeaderSet() == true)
			writeLine(out, "header = " + getHeader());
		writeLine(out, "show-header = " + (getShowHeader() ? "true" : "false"));
		writeLine(out, "host-issue = " + getHostIssue());
		writeLine(out, "service-issue = " + getServiceIssue());

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

	public void setNagiosStatusHost(String host)
	{
		lock();
		this.host = host;
		unlock();
	}

	public String getNagiosStatusHost()
	{
		String copy;
		lock();
		copy = host;
		unlock();
		return copy;
	}

	public void setNagiosStatusPort(int port)
	{
		lock();
		this.port = port;
		unlock();
	}

	public int getNagiosStatusPort()
	{
		int copy;
		lock();
		copy = port;
		unlock();
		return copy;
	}

	public void setNagiosStatusFile(String file)
	{
		lock();
		this.file = file;
		unlock();
	}

	public String getNagiosStatusFile()
	{
		String copy;
		lock();
		copy = file;
		unlock();
		return copy;
	}

	public void setNagiosStatusURL(String url) throws Exception
	{
		lock();
		this.url = new URL(url);
		unlock();
	}

	public URL getNagiosStatusURL()
	{
		URL copy;
		lock();
		copy = url;
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

	public void setNagiosStatusVersion(String version) throws Exception
	{
		NagiosVersion nv = null;

		if (version.equals("1"))
			nv = NagiosVersion.V1;
		else if (version.equals("2"))
			nv = NagiosVersion.V2;
		else if (version.equals("3"))
			nv = NagiosVersion.V3;
		else
			throw new Exception("Invalid nagios version selected (" + version + ").");

		lock();
		this.nagiosVersion = nv;
		unlock();
	}

	public NagiosVersion getNagiosStatusVersion()
	{
		NagiosVersion copy;
		lock();
		copy = nagiosVersion;
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

	public int getNImageUrls()
	{
		int n;
		lock();
		n = imageFiles.size();
		unlock();
		return n;
	}

	public String getImageUrl(int index)
	{
		String copy;
		lock();
		if (imageFiles.size() == 0)
			copy = null;
		else
			copy = imageFiles.get(index % imageFiles.size());
		unlock();
		return copy;
	}

	public List<String> getImageUrls()
	{
		List<String> copy;
		lock();
		copy = imageFiles;
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
		System.out.println("set font name: " + fontName);
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
}
