/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import com.vanheusden.nagios.*;

import java.awt.*;
import javax.swing.*;
import java.net.URL;
import java.awt.image.*;
import java.util.*;
import java.awt.event.WindowEvent;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileReader;
import java.awt.geom.Rectangle2D;
import java.awt.font.FontRenderContext;
import java.io.FileNotFoundException;
import java.util.concurrent.Semaphore;

public class CoffeeSaint
{
	static String version = "CoffeeSaint v2.3-beta001, (C) 2009 by folkert@vanheusden.com";

	final public static Log log = new Log(250);

	volatile static Config config;

	Predictor predictor;
	long lastPredictorDump = 0;
	//
	static int currentImageFile = 0;
	static Semaphore imageSemaphore = new Semaphore(1);
	//
	static Statistics statistics = new Statistics();
	//
	static Semaphore problemsSemaphore = new Semaphore(1);
	static java.util.List<Problem> problems;
	static JavNag javNag;
	//
	Random random = new Random();

	public CoffeeSaint() throws Exception
	{
		if (config.getBrainFileName() != null)
		{
			predictor = new Predictor(config.getSleepTime());

			try
			{
				System.out.println("Loading brain from " + config.getBrainFileName());
				predictor.restoreBrainFromFile(config.getBrainFileName());
			}
			catch(FileNotFoundException e)
			{
				System.err.println("File " + config.getBrainFileName() + " not found, continuing(!) anyway");
			}
		}
	}

	public void cleanUp()
	{
		System.runFinalization();
		System.gc();
	}

	static public String getVersion()
	{
		return version;
	}

	public static void showException(Exception e)
	{
		log.add("Exception: " + e);
		log.add("Details: " + e.getMessage());
		log.add("Stack-trace:");
		for(StackTraceElement ste: e.getStackTrace())
		{
			log.add(" " + ste.getClassName() + ", "
					+ ste.getFileName() + ", "
					+ ste.getLineNumber() + ", "
					+ ste.getMethodName() + ", "
					+ (ste.isNativeMethod() ? "is native method" : "NOT a native method"));
		}
	}

	String make2Digit(String in)
	{
		String newStr = "00" + in;

		return newStr.substring(newStr.length() - 2);
	}

	public String hostState(String state)
	{
		if (state.equals("0")) /* UP = OK */
			return "OK";

		if (state.equals("1"))
			return "DOWN";

		if (state.equals("2"))
			return "UNREACHABLE";

		if (state.equals("3"))
			return "PENDING";

		return "?";
	}

	public String serviceState(String state)
	{
		if (state.equals("0")) /* UP = OK */
			return "OK";

		if (state.equals("1"))
			return "WARNING";

		if (state.equals("2"))
			return "CRITICAL";

		return "?";

	}

	public String stringTsToDate(String ts)
	{
		long seconds = Long.valueOf(ts);

		Calendar then = Calendar.getInstance();
		then.setTimeInMillis(seconds * 1000L);

		return "" + then.get(Calendar.YEAR) + "/" + then.get(Calendar.MONTH) + "/" + then.get(Calendar.DAY_OF_MONTH) + " " + make2Digit("" + then.get(Calendar.HOUR_OF_DAY)) + ":" + make2Digit("" + then.get(Calendar.MINUTE)) + ":" + make2Digit("" + then.get(Calendar.SECOND));
	}

	public String durationToString(long howLongInSecs)
	{
		String out = "";

		if (howLongInSecs >= 86400)
			out += "" + (howLongInSecs / 86400) + "d ";

		out += "" + make2Digit("" + ((howLongInSecs / 3600) % 24)) + ":" + make2Digit("" + ((howLongInSecs / 60) % 10)) + ":" + make2Digit("" + (howLongInSecs % 60));

		return out;
	}

	public String processStringEscapes(JavNag javNag, Totals totals, Calendar rightNow, Problem problem, String cmd)
	{
		if (cmd.equals("CRITICAL"))
			return "" + totals.getNCritical();
		if (cmd.equals("WARNING"))
			return "" + totals.getNWarning();
		if (cmd.equals("OK"))
			return "" + totals.getNOk();

		if (cmd.equals("UP"))
			return "" + totals.getNUp();
		if (cmd.equals("DOWN"))
			return "" + totals.getNDown();
		if (cmd.equals("UNREACHABLE"))
			return "" + totals.getNUnreachable();
		if (cmd.equals("PENDING"))
			return "" + totals.getNPending();

		if (cmd.equals("PREDICT"))
		{
			Double count = predictProblemCount(rightNow);
			if (count == null)
				return "?";
			String countStr = "" + count;
			int dot = countStr.indexOf(".");
			if (dot != -1)
				countStr = countStr.substring(0, dot + 2);
			return countStr;
		}

		if (cmd.equals("HISTORICAL"))
			return "" + predictor.getHistorical(rightNow);

		if (cmd.equals("H"))
			return make2Digit("" + rightNow.get(Calendar.HOUR_OF_DAY));
		if (cmd.equals("M"))
			return make2Digit("" + rightNow.get(Calendar.MINUTE));

		if (cmd.equals("HOSTNAME") && problem != null && problem.getHost() != null)
			return problem.getHost().getHostName();

		if (cmd.equals("SERVICENAME") && problem != null && problem.getService() != null)
			return problem.getService().getServiceName();

		if (cmd.equals("HOSTSTATE") && problem != null && problem.getHost() != null)
			return hostState(problem.getHost().getParameter("current_state"));

		if (cmd.equals("SERVICESTATE") && problem != null && problem.getService() != null)
			return serviceState(problem.getService().getParameter("current_state"));

		if (cmd.equals("HOSTDURATION"))
			return "" + durationToString(System.currentTimeMillis() / 1000 - Long.valueOf(problem.getHost().getParameter("last_state_change")));
		if (cmd.equals("SERVICEDURATION"))
			return "" + durationToString(System.currentTimeMillis() / 1000 - Long.valueOf(problem.getService().getParameter("last_state_change")));

		if (cmd.equals("HOSTSINCE") && problem != null && problem.getHost() != null)
			return stringTsToDate(problem.getHost().getParameter("last_state_change"));

		if (cmd.equals("SERVICESINCE") && problem != null && problem.getService() != null)
			return stringTsToDate(problem.getService().getParameter("last_state_change"));

		if (cmd.equals("HOSTFLAPPING") && problem != null && problem.getHost() != null)
			return problem.getHost().getParameter("is_flapping").equals("1") ? "FLAPPING" : "";

		if (cmd.equals("SERVICEFLAPPING") && problem != null && problem.getService() != null)
			return problem.getService().getParameter("is_flapping").equals("1") ? "FLAPPING" : "";

		if (cmd.equals("OUTPUT") && problem != null)
		{
			String output;
			if (problem.getService() != null)
				output = problem.getService().getParameter("plugin_output");
			else
				output = problem.getHost().getParameter("plugin_output");
			if (output != null)
				return output;
			return "?";
		}

		return "?" + cmd + "?";
	}

	public String processStringWithEscapes(String in, JavNag javNag, Calendar rightNow, Problem problem)
	{
		final Totals totals = javNag.calculateStatistics();
		log.add("" + totals.getNHosts() + " hosts, " + totals.getNServices() + " services");
		boolean loadingCmd = false;
		String cmd = "", output = "";

		for(int index=0; index<in.length(); index++)
		{
			if (loadingCmd)
			{
				if (!(in.charAt(index) >= 'A' && in.charAt(index) <= 'Z') && !(in.charAt(index) >= 'a' && in.charAt(index) <= 'z'))
				{
					output += processStringEscapes(javNag, totals, rightNow, problem, cmd);

					cmd = "";
					loadingCmd = false;

					if (in.charAt(index) == '%')
						loadingCmd = true;
					else
						output += in.charAt(index);
				}
				else
				{
					cmd += in.charAt(index);
				}
			}
			else
			{
				if (in.charAt(index) == '%')
					loadingCmd = true;
				else
					output += in.charAt(index);
			}
		}

		if (cmd.equals("") == false)
			output += processStringEscapes(javNag, totals, rightNow, problem, cmd);

		return output;
	}

	public String getScreenHeader(JavNag javNag, Calendar rightNow)
	{
		if (config.getNagiosDataSources().size() == 0)
			return "No Nagios servers selected!";
		else
			return processStringWithEscapes(config.getHeader(), javNag, rightNow, null);
	}

	public Color stateToColor(String state)
	{
		if (state.equals("0") == true)
			return config.getBackgroundColorOkStatus();
		else if (state.equals("1") == true)
			return config.getWarningBgColor();
		else if (state.equals("2") == true)
			return config.getCriticalBgColor();
		else if (state.equals("3") == true) // UNKNOWN STATE
			return config.getNagiosUnknownBgColor();
		else if (state.equals("255") == true)
			return config.getBackgroundColor();

		log.add("Unknown state: " + state);
		return Color.ORANGE;
	}

	public void drawLoadStatus(Gui gui, int windowWidth, Graphics g, String message)
	{
		if (config.getVerbose() && gui != null && g != null)
			gui.drawRow(g, windowWidth, message, 0, "0", config.getBackgroundColor(), 1, 0, 1.0f);
	}

	public ImageParameters [] loadImage(Gui gui, int windowWidth, Graphics g) throws Exception
	{
		int nr;
		java.util.List<String> imageUrls = config.getImageUrls();
		int nImages = imageUrls.size();
		if (nImages == 0)
			return null;

		int loadNImages = config.getCamRows() * config.getCamCols();

		ImageParameters [] result = new ImageParameters[loadNImages];
		int [] indexes = new int[loadNImages];

		imageSemaphore.acquireUninterruptibly(); // lock around 'currentImageFile'
		if (config.getRandomWebcam())
		{
			for(nr=0; nr<Math.min(nImages, loadNImages); nr++)
			{
				boolean found;
				do
				{
					found = false;
					indexes[nr] = random.nextInt(nImages);
					for(int searchIndex=0; searchIndex<nr; searchIndex++)
					{
						if (indexes[searchIndex] == indexes[nr])
						{
							found = true;
							break;
						}
					}
				}
				while(found);
			}
		}
		else
		{
			for(nr=0; nr<Math.min(nImages, loadNImages); nr++)
			{
				indexes[nr] = currentImageFile++;
				if (currentImageFile == nImages)
					currentImageFile = 0;
			}
		}
		imageSemaphore.release();

		Image [] img = new Image[loadNImages];
		for(nr=0; nr<Math.min(nImages, loadNImages); nr++)
		{
			String loadImage = imageUrls.get(indexes[nr]);
			log.add("Load image(1) " + loadImage);
			drawLoadStatus(gui, windowWidth, g, "Start load img " + loadImage);

			if (loadImage.length() >= 8 && (loadImage.substring(0, 7).equalsIgnoreCase("http://") || loadImage.substring(0, 8).equalsIgnoreCase("https://")))
				img[nr] = Toolkit.getDefaultToolkit().createImage(new URL(loadImage));
			else
				img[nr] = Toolkit.getDefaultToolkit().createImage(loadImage);
		}

		for(nr=0; nr<Math.min(nImages, loadNImages); nr++)
		{
			String loadImage = imageUrls.get(indexes[nr]);
			drawLoadStatus(gui, windowWidth, g, "Load image " + loadImage);

			new ImageIcon(img[nr]); //loads the image
			Toolkit.getDefaultToolkit().sync();

			int imgWidth = img[nr].getWidth(null);
			int imgHeight = img[nr].getHeight(null);

			result[nr] = new ImageParameters(img[nr], loadImage, imgWidth, imgHeight);
		}

		return result;
	}

	public Double predictProblemCount(Calendar rightNow)
	{
		Calendar future = Calendar.getInstance();
		future.add(Calendar.SECOND, config.getSleepTime());

		Double value = predictor.predict(rightNow, future);
		if (value != null)
			value = Math.ceil(value * 10.0) / 10.0;

		log.add("Prediction value: " + value);

		return value;
	}

	public Color predictWithColor(Calendar rightNow)
	{
		Color bgColor = config.getBackgroundColorOkStatus();

		if (predictor != null)
		{
			Double value = predictProblemCount(rightNow);
			if (value != null && value != 0.0)
			{
				log.add("Expecting " + value + " problems after next interval");
				int red = 100 + (int)(value * (100.0 / (double)config.getNRows()));
				if (red < 0)
					red = 0;
				if (red > 200)
					red = 200;
				bgColor = new Color(red, 255, 0);
			}
		}

		return bgColor;
	}

	public void dumpPredictorBrainToFile() throws Exception
	{
		log.add("Dumping brain to " + config.getBrainFileName());

		predictor.dumpBrainToFile(config.getBrainFileName());
	}

	public void learnProblems(Calendar rightNow, int nProblems) throws Exception
	{
		predictor.learn(rightNow, nProblems);

		if ((System.currentTimeMillis() - lastPredictorDump)  > 1800000)
		{
			dumpPredictorBrainToFile();

			lastPredictorDump = System.currentTimeMillis();
		}
	}

	synchronized public void loadNagiosData(Gui gui, int windowWidth, Graphics g) throws Exception
	{
		javNag = new JavNag();

		long startLoadTs = System.currentTimeMillis();

		for(NagiosDataSource dataSource : config.getNagiosDataSources())
		{
			String logStr = "Loading data from: ";
			if (dataSource.getType() == NagiosDataSourceType.TCP)
			{
				String source = dataSource.getHost() + " " + dataSource.getPort();
				logStr += source;
				drawLoadStatus(gui, windowWidth, g, "Load Nagios " + source);
				javNag.loadNagiosData(dataSource.getHost(), dataSource.getPort(), dataSource.getVersion(), false);
			}
			else if (dataSource.getType() == NagiosDataSourceType.ZTCP)
			{
				String source = dataSource.getHost() + " " + dataSource.getPort();
				logStr += source;
				drawLoadStatus(gui, windowWidth, g, "zLoad Nagios " + source);
				javNag.loadNagiosData(dataSource.getHost(), dataSource.getPort(), dataSource.getVersion(), true);
			}
			else if (dataSource.getType() == NagiosDataSourceType.HTTP)
			{
				logStr += dataSource.getURL();
				drawLoadStatus(gui, windowWidth, g, "Load Nagios " + dataSource.getURL());
				javNag.loadNagiosData(dataSource.getURL(), dataSource.getVersion(), config.getAllowHTTPCompression());
			}
			else if (dataSource.getType() == NagiosDataSourceType.FILE)
			{
				logStr += dataSource.getFile();
				drawLoadStatus(gui, windowWidth, g, "Load Nagios " + dataSource.getFile());
				javNag.loadNagiosData(dataSource.getFile(), dataSource.getVersion());
			}
			else
				throw new Exception("Unknown data-source type: " + dataSource.getType());

			logStr += " - done.";
			log.add(logStr);
		}

		long endLoadTs = System.currentTimeMillis();

		double took = (double)(endLoadTs - startLoadTs) / 1000.0;
		log.add("Took " + took + "s to load status data");

		statistics.addToTotalRefreshTime(took);
		statistics.addToNRefreshes(1);
	}

	public void findProblems() throws Exception
	{
		java.util.List<Problem> lessImportant = new ArrayList<Problem>();
		problems = new ArrayList<Problem>();

		// collect problems
		Problems.collectProblems(javNag, config.getPrioPatterns(), problems, lessImportant, config.getAlwaysNotify(), config.getAlsoAcknowledged(), config.getAlsoScheduledDowntime(), config.getAlsoSoftState(), config.getAlsoDisabledActiveChecks(), config.getShowServicesForHostWithProblems(), config.getShowFlapping(), config.getHostsFilterExclude(), config.getHostsFilterInclude(), config.getServicesFilterExclude(), config.getServicesFilterInclude());
		// sort problems
		Problems.sortList(problems, config.getSortOrder(), config.getSortOrderNumeric(), config.getSortOrderReverse());
		Problems.sortList(lessImportant, config.getSortOrder(), config.getSortOrderNumeric(), config.getSortOrderReverse());
		// and combine them
		for(Problem currentLessImportant : lessImportant)
			problems.add(currentLessImportant);

		if (predictor != null)
		{
			Calendar rightNow = Calendar.getInstance();
			learnProblems(rightNow, problems.size());
		}

		problemsSemaphore.release();
	}

	public void lockProblems()
	{
		problemsSemaphore.acquireUninterruptibly();
	}

	public void unlockProblems()
	{
		problemsSemaphore.release();
	}

	public java.util.List<Problem> getProblems()
	{
		return problems;
	}

	public JavNag getNagiosData()
	{
		return javNag;
	}

	public static void daemonLoop(CoffeeSaint coffeeSaint, Config config) throws Exception
	{
		for(;;)
		{
			Thread.sleep(config.getSleepTime() * 1000);
			coffeeSaint.cleanUp();
		}
	}

	public static void setIcon(CoffeeSaint coffeeSaint, JFrame f)
	{
		ClassLoader loader = coffeeSaint.getClass().getClassLoader();
		URL fileLocation = loader.getResource("com/vanheusden/CoffeeSaint/programIcon.png");
		Image img = Toolkit.getDefaultToolkit().getImage(fileLocation); 
		f.setIconImage(img);
	}

	public static void showHelp()
	{
		System.out.println("--source type version x  Source to retrieve from");
		System.out.println("              Type can be: http, tcp, file");
		System.out.println("              http expects an url like http://keetweej.vanheusden.com/status.dat");
		System.out.println("              tcp expects a host and portnumber, e.g.: keetweej.vanheusden.com 33333");
		System.out.println("              ztcp also expects a host and portnumber, e.g.: keetweej.vanheusden.com 33333");
		System.out.println("              file expects a file-name, e.g. /var/cache/nagios3/status.dat");
		System.out.println("              version selects the nagios-version. E.g. 1, 2 or 3");
		System.out.println("              You can add as many Nagios servers as you like");
		System.out.println("              Example: --source file 3 /var/cache/nagios3/status.dat");
		System.out.println("");
		System.out.println("--disable-http-compression Don't use gzip/deflate compression in HTTP connection - usefull for fast links as the server has less load then");
		System.out.println("--nrows x     Number of rows to show, must be at least 2");
		System.out.println("--interval x  Retrieve status every x seconds");
		System.out.println("--fullscreen  Run in fullscreen mode, e.g. without any borders");
		System.out.println("--problem-columns x  Split the screen in x columns so that it can display x * nrows");
		System.out.println("--image x     Display image x on background. Can be a filename or an http-URL. One can have multiple files/url which will be shown roundrobin");
		System.out.println("--adapt-img   Reduce image-size to fit below the listed problems");
		System.out.println("--random-img  Randomize order of images shown");
		System.out.println("--transparency x Transparency for drawing (0.0...1.0) - only usefull with background image/webcam");
		System.out.println("--font x      Font to use. Default is 'Arial'");
		System.out.println("--critical-font x  Font to use for critical problems");
		System.out.println("--warning-font x   Font to use for warning problems");
		System.out.println("--reduce-textwidth Try to fit text to the window width");
		System.out.println("--prefer x    File to load regular expressions from which tell what problems to show with priority (on top of the others)");
		System.out.println("--also-acknowledged Display acknowledged problems as well");
		System.out.println("--always-notify	Also display problems for which notifications are disabled");
		System.out.println("--also-scheduled-downtime Also display problems for which downtime has been scheduled");
		System.out.println("--also-soft-state   Also display problems that are not yet in hard state");
		System.out.println("--also-disabled-active-checks Also display problems for which active checks have been disabled");
		System.out.println("--suppress-flapping Do not show hosts that are flapping");
		System.out.println("--show-services-for-host-with-problems");
		System.out.println("--bgcolor x   Select a background-color, used when there's something to notify about. Default is gray");
		System.out.println("--list-bgcolors     Show a list of available colors");
		System.out.println("--textcolor   Text color (header and such)");
		System.out.println("--warning-textcolor Text color of warning-problems");
		System.out.println("--critical-textcolor Text color of critical-problems");
		System.out.println("--sound x     Play sound when a warning/error state starts");
		System.out.println("--counter     Show counter decreasing upto the point that a refresh will happen");
		System.out.println("--exec x      Execute program when one or more errors are shown");
		System.out.println("--predict x   File to write brain-dump to (and read from)");
		System.out.println("--config x    Load configuration from file x. This overrides all configurationsettings set previously");
		System.out.println("--create-config x    Create new configuration file with filename x");
		System.out.println("--listen-port Port to listen for the internal webserver");
		System.out.println("--listen-adapter Network interface to listen for the internal webserver");
		System.out.println("--disable-http-fileselect Do not allow web-interface to select a file to write configuration to");
		System.out.println("--header x    String to display in header. Can contain escapes, see below");
		System.out.println("--host-issue x  String defining how to format host-issues");
		System.out.println("--service-issue x  String defining how to format service-issues");
		System.out.println("--no-header   Do not display the statistics line in the upper row");
		System.out.println("--row-border  Draw a line between each row");
		System.out.println("--sort-order [y] [z] x  Sort on field x. y and z can be 'numeric' and 'reverse'");
		System.out.println("              E.g. --sort-order numeric last_state_change (= default)");
		System.out.println("--cam-cols    Number of cams per row");
		System.out.println("--cam-rows    Number of rows with cams");
		System.out.println("--ignore-aspect-ratio Grow/shrink all webcams with the same factor. In case you have webcams with different dimensions");
		System.out.println("--scrolling-header  In case there's more information to put into it than what fits on the screen");
		System.out.println("--anti-alias  Anti-alias graphics");
		System.out.println("--verbose     Show what it is doing");
		System.out.println("--warning-bg-color x Background color for warnings (yellow)");
		System.out.println("--critical-bg-color x Background color for criticals (red)");
		System.out.println("--nagios-unknown-bg-color x Background color for unknonws (magenta)");
		System.out.println("--hosts-filter-exclude x Komma-seperated list of hosts not to display");
		System.out.println("--hosts-filter-include x Komma-seperated list of hosts to display. Use in combination with --hosts-filter-exclude: will be invoked after the exclude.");
		System.out.println("--services-filter-exclude x Komma-seperated list of services not to display");
		System.out.println("--services-filter-include x Komma-seperated list of services to display. Use in combination with --services-filter-exclude: will be invoked after the exclude.");
		System.out.println("");
		System.out.print("Known colors:");
		config.listColors();
		System.out.println("");
		System.out.println("Escapes:");
		System.out.println("  %CRITICAL/%WARNING/%OK, %UP/%DOWN/%UNREACHABLE/%PENDING");
		System.out.println("  %H:%M       Current hour/minute");
		System.out.println("  %HOSTNAME/%SERVICENAME    host/service with problem");
		System.out.println("  %HOSTSTATE/%SERVICESTATE  host/service state");
		System.out.println("  %HOSTSINCE/%SERVICESINCE  since when does this host/service have a problem");
		System.out.println("  %HOSTFLAPPING/%SERVICEFLAPPING  wether the state is flapping");
		System.out.println("  %PREDICT/%HISTORICAL      ");
		System.out.println("  %HOSTDURATION/%SERVICEDURATION how long has a host/service been down");
		System.out.println("  %OUTPUT                   Plugin output");
		System.out.println("");
		System.out.println("Sort-fields:");
		config.listSortFields();
		System.out.println("");
	}

	public static void main(String[] arg)
	{
		try
		{
			statistics.setRunningSince(System.currentTimeMillis());

			System.out.println(version);
			System.out.println("");

			config = new Config();

			for(int loop=0; loop<arg.length; loop++)
			{
				String currentSwitch = arg[loop];

				try
				{
					if (arg[loop].equals("--create-config"))
					{
						config.writeConfig(arg[++loop]);
						config.setConfigFilename(arg[loop]);
					}
					else if (arg[loop].equals("--source"))
					{
						NagiosDataSource nds = null;
						NagiosVersion nv = null;
						String type = arg[++loop];
						String versionStr = arg[++loop];

						if (versionStr.equals("1"))
							nv = NagiosVersion.V1;
						else if (versionStr.equals("2"))
							nv = NagiosVersion.V2;
						else if (versionStr.equals("3"))
							nv = NagiosVersion.V3;
						else
							throw new Exception("Nagios version '" + versionStr + "' not known");

						if (type.equalsIgnoreCase("http"))
							nds = new NagiosDataSource(new URL(arg[++loop]), nv);
						else if (type.equalsIgnoreCase("file"))
							nds = new NagiosDataSource(arg[++loop], nv);
						else if (type.equalsIgnoreCase("tcp") || type.equalsIgnoreCase("ztcp"))
						{
							String host = arg[++loop];
							int port;
							try
							{
								port = Integer.valueOf(arg[++loop]);
								nds = new NagiosDataSource(host, port, nv, type.equalsIgnoreCase("ztcp"));
							}
							catch(NumberFormatException nfe)
							{
								System.err.println("--source: expecting a port-number but got '" + arg[loop] + "'");
								System.exit(127);
							}
						}
						else
							throw new Exception("Data source-type '" + type + "' not understood.");

						config.addNagiosDataSource(nds);
					}
					else if (arg[loop].equals("--sort-order"))
					{
						boolean reverse = false, numeric = false;

						for(;;)
						{
							++loop;
							if (arg[loop].equals("reverse"))
								reverse = true;
							else if (arg[loop].equals("numeric"))
								numeric = true;
							else
								break;
						}

						config.setSortOrder(arg[loop], numeric, reverse);
					}
					else if (arg[loop].equals("--no-header"))
						config.setShowHeader(false);
					else if (arg[loop].equals("--anti-alias"))
						config.setAntiAlias(true);
					else if (arg[loop].equals("--scrolling-header"))
						config.setScrollingHeader(true);
					else if (arg[loop].equals("--fullscreen"))
						config.setFullscreen(true);
					else if (arg[loop].equals("--header"))
						config.setHeader(arg[++loop]);
					else if (arg[loop].equals("--row-border"))
						config.setRowBorder(true);
					else if (arg[loop].equals("--service-issue"))
						config.setServiceIssue(arg[++loop]);
					else if (arg[loop].equals("--host-issue"))
						config.setHostIssue(arg[++loop]);
					else if (arg[loop].equals("--random-img"))
						config.setRandomWebcam(true);
					else if (arg[loop].equals("--no-gui"))
						config.setRunGui(false);
					else if (arg[loop].equals("--config"))
						config.loadConfig(arg[++loop]);
					else if (arg[loop].equals("--predict"))
						config.setBrainFileName(arg[++loop]);
					else if (arg[loop].equals("--exec"))
						config.setExec(arg[++loop]);
					else if (arg[loop].equals("--adapt-img"))
						config.setAdaptImageSize(true);
					else if (arg[loop].equals("--counter"))
						config.setCounter(true);
					else if (arg[loop].equals("--verbose"))
						config.setVerbose(true);
					else if (arg[loop].equals("--sound"))
						config.setProblemSound(arg[++loop]);
					else if (arg[loop].equals("--listen-port"))
					{
						try
						{
							config.setHTTPServerListenPort(Integer.valueOf(arg[++loop]));
						}
						catch(NumberFormatException nfe)
						{
							System.err.println("--listen-port: expecting a port-number but got '" + arg[loop] + "'");
							System.exit(127);
						}
					}
					else if (arg[loop].equals("--listen-adapter"))
						config.setHTTPServerListenAdapter(arg[++loop]);
					else if (arg[loop].equals("--list-bgcolors"))
					{
						config.listColors();
						System.exit(0);
					}
					else if (arg[loop].equals("--warning-bg-color"))
						config.setWarningBgColor(arg[++loop]);
					else if (arg[loop].equals("--critical-bg-color"))
						config.setCriticalBgColor(arg[++loop]);
					else if (arg[loop].equals("--nagios-unknown-bg-color"))
						config.setNagiosUnknownBgColor(arg[++loop]);
					else if (arg[loop].equals("--bgcolor"))
						config.setBackgroundColor(arg[++loop]);
					else if (arg[loop].equals("--textcolor"))
						config.setTextColor(arg[++loop]);
					else if (arg[loop].equals("--nrows"))
						config.setNRows(Integer.valueOf(arg[++loop]));
					else if (arg[loop].equals("--interval"))
						config.setSleepTime(Integer.valueOf(arg[++loop]));
					else if (arg[loop].equals("--image"))
						config.addImageUrl(arg[++loop]);
					else if (arg[loop].equals("--problem-columns"))
						config.setNProblemCols(Integer.valueOf(arg[++loop]));
					else if (arg[loop].equals("--cam-rows"))
						config.setCamRows(Integer.valueOf(arg[++loop]));
					else if (arg[loop].equals("--cam-cols"))
						config.setCamCols(Integer.valueOf(arg[++loop]));
					else if (arg[loop].equals("--reduce-textwidth"))
						config.setReduceTextWidth(true);
					else if (arg[loop].equals("--max-quality-graphics"))
						config.setMaxQualityGraphics(true);
					else if (arg[loop].equals("--flexible-n-columns"))
						config.setFlexibleNColumns(true);
					else if (arg[loop].equals("--disable-http-fileselect"))
						config.setDisableHTTPFileselect();
					else if (arg[loop].equals("--prefer"))
					{
						System.out.println("Loading prefers from " + arg[++loop]);
						config.loadPrefers(arg[loop]);
					}
					else if (arg[loop].equals("--always-notify"))
						config.setAlwaysNotify(true);
					else if (arg[loop].equals("--suppress-flapping"))
						config.setShowFlapping(false);
					else if (arg[loop].equals("--also-acknowledged"))
						config.setAlsoAcknowledged(true);
					else if (arg[loop].equals("--font"))
						config.setFontName(arg[++loop]);
					else if (arg[loop].equals("--critical-font"))
						config.setCriticalFontName(arg[++loop]);
					else if (arg[loop].equals("--warning-font"))
						config.setWarningFontName(arg[++loop]);
					else if (arg[loop].equals("--warning-textcolor"))
						config.setWarningTextColor(arg[++loop]);
					else if (arg[loop].equals("--critical-textcolor"))
						config.setCriticalTextColor(arg[++loop]);
					else if (arg[loop].equals("--ignore-aspect-ratio"))
						config.setKeepAspectRatio(false);
					else if (arg[loop].equals("--also-scheduled-downtime"))
						config.setAlsoScheduledDowntime(true);
					else if (arg[loop].equals("--show-services-for-host-with-problems"))
						config.setShowServicesForHostWithProblems(true);
					else if (arg[loop].equals("--also-soft-state"))
						config.setAlsoSoftState(true);
					else if (arg[loop].equals("--also-disabled-active-checks"))
						config.setAlsoDisabledActiveChecks(true);
					else if (arg[loop].equals("--disable-http-compression"))
						config.setAllowHTTPCompression(false);
					else if (arg[loop].equals("--transparency"))
						config.setTransparency(Float.valueOf(arg[++loop]));
					else if (arg[loop].equals("--hosts-filter-exclude"))
						config.setHostsFilterExclude(arg[++loop]);
					else if (arg[loop].equals("--hosts-filter-include"))
						config.setHostsFilterInclude(arg[++loop]);
					else if (arg[loop].equals("--services-filter-exclude"))
						config.setServicesFilterExclude(arg[++loop]);
					else if (arg[loop].equals("--services-filter-include"))
						config.setServicesFilterInclude(arg[++loop]);
					else if (arg[loop].equals("--version") || arg[loop].equals("-version"))
					{
						System.out.println(getVersion());
						System.exit(0);
					}
					else if (arg[loop].equals("--help") || arg[loop].equals("--h"))
					{
						showHelp();
						System.exit(0);
					}
					else
					{
						System.err.println("Parameter " + arg[loop] + " not understood.");
						showHelp();
						System.exit(127);
					}
				}
				catch(ArrayIndexOutOfBoundsException aioobe)
				{
					System.err.println(currentSwitch + ": expects more parameters than currently given");
					System.exit(127);
				}
				catch(NumberFormatException nfeGlobal)
				{
					System.err.println(currentSwitch + ": one of the parameters given should've been a number");
					System.exit(127);
				}
			}

			CoffeeSaint coffeeSaint = new CoffeeSaint();
			Gui gui = null;
			if (config.getRunGui())
			{
				System.out.println("Start gui");

				GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
				JFrame f = new JFrame();
				if (config.getFullscreen())
					f.setUndecorated(true);

				gui = new Gui(config, coffeeSaint, statistics);

				/* create frame to draw in */
				Rectangle useable = ge.getMaximumWindowBounds();
				f.setMaximizedBounds(useable);
				f.setExtendedState(f.getExtendedState() | JFrame.MAXIMIZED_BOTH);
				f.setSize(useable.width, useable.height);
				f.setContentPane(gui);

				RepaintManager.currentManager(gui).setDoubleBufferingEnabled(false);

				System.out.println("Initial paint");

				f.setTitle(getVersion());
				setIcon(coffeeSaint, f);

				f.setVisible(true);

				f.addWindowListener(new FrameListener(config, coffeeSaint));
			}

			if (config.getHTTPServerListenPort() != -1)
			{
				System.out.println("Start HTTP server");
				new Thread(new HTTPServer(config, coffeeSaint, config.getHTTPServerListenAdapter(), config.getHTTPServerListenPort(), statistics, gui)).start();
			}

			if (config.getRunGui())
			{
				System.out.println("Start gui loop");
				gui.guiLoop();
			}
			else
			{
				System.out.println("Start daemon loop");
				daemonLoop(coffeeSaint, config);
			}
		}
		catch(Exception e)
		{
			showException(e);
		}
	}
}
