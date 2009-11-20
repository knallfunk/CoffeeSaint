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
	static String version = "CoffeeSaint v1.4-beta001, (C) 2009 by folkert@vanheusden.com";

	static Config config;

	static Predictor predictor;
	static long lastPredictorDump = 0;
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

	public CoffeeSaint()
	{
	}

	static public Predictor getPredictor()
	{
		return predictor;
	}

	static public String getVersion()
	{
		return version;
	}

	public static void showException(Exception e)
	{
		System.err.println("Exception: " + e);
		System.err.println("Details: " + e.getMessage());
		System.err.println("Stack-trace:");
		for(StackTraceElement ste: e.getStackTrace())
		{
			System.err.println(" " + ste.getClassName() + ", "
					+ ste.getFileName() + ", "
					+ ste.getLineNumber() + ", "
					+ ste.getMethodName() + ", "
					+ (ste.isNativeMethod() ?
						"is native method" : "NOT a native method"));
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
System.out.println(ts + ": " + (seconds * 1000L));

		return "" + then.get(Calendar.YEAR) + "/" + then.get(Calendar.MONTH) + "/" + then.get(Calendar.DAY_OF_MONTH) + " " + make2Digit("" + then.get(Calendar.HOUR_OF_DAY)) + ":" + make2Digit("" + then.get(Calendar.MINUTE)) + ":" + make2Digit("" + then.get(Calendar.SECOND));
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
		return processStringWithEscapes(config.getHeader(), javNag, rightNow, null);
	}

	public Color stateToColor(String state)
	{
		if (state.equals("0") == true)
			return Color.GREEN;
		else if (state.equals("1") == true)
			return Color.YELLOW;
		else if (state.equals("2") == true)
			return Color.RED;
		else if (state.equals("3") == true) // UNKNOWN STATE
			return Color.MAGENTA;
		else if (state.equals("255") == true)
			return config.getBackgroundColor();

		System.out.println("Unknown state: " + state);
		return Color.ORANGE;
	}

	public ImageParameters loadImage() throws Exception
	{
		imageSemaphore.acquireUninterruptibly();

		ImageParameters result = null;
		int nImages = config.getNImageUrls();

		if (nImages > 0)
		{
			Image img;

			if (config.getRandomWebcam())
			{
				int newImg = 0;

				do
				{
					newImg = random.nextInt(nImages);
				}
				while(newImg == currentImageFile && nImages != 1);
				currentImageFile = newImg;
			}

			String loadImage = config.getImageUrl(currentImageFile);
			if (loadImage == null) // no images in list
				return null;

			System.out.println("Load image " + loadImage);

			if (loadImage.substring(0, 7).equals("http://"))
				img = Toolkit.getDefaultToolkit().createImage(new URL(loadImage));
			else
				img = Toolkit.getDefaultToolkit().createImage(loadImage);
			new ImageIcon(img); //loads the image
			Toolkit.getDefaultToolkit().sync();

			int imgWidth = img.getWidth(null);
			int imgHeight = img.getHeight(null);

			if (!config.getRandomWebcam())
			{
				currentImageFile++;
				if (currentImageFile >= nImages)
					currentImageFile = 0;
			}

			result = new ImageParameters(img, loadImage, imgWidth, imgHeight);
		}

		imageSemaphore.release();

		return result;
	}

	public Double predictProblemCount(Calendar rightNow)
	{
		Calendar future = Calendar.getInstance();
		future.add(Calendar.SECOND, config.getSleepTime());

		Double value = predictor.predict(rightNow, future);
		if (value != null)
			value = Math.ceil(value * 10.0) / 10.0;

		System.out.println("Prediction value: " + value);

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
				System.out.println("Expecting " + value + " problems after next interval");
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

	public void learnProblems(Calendar rightNow, int nProblems) throws Exception
	{
		predictor.learn(rightNow, nProblems);

		if ((System.currentTimeMillis() - lastPredictorDump)  > 1800000)
		{
			System.out.println("Dumping brain to " + config.getBrainFileName());

			predictor.dumpBrainToFile(config.getBrainFileName());

			lastPredictorDump = System.currentTimeMillis();
		}
	}

	synchronized public void loadNagiosData() throws Exception
	{
		javNag = new JavNag();

		long startLoadTs = System.currentTimeMillis();

		for(NagiosDataSource dataSource : config.getNagiosDataSources())
		{
			System.out.print("Loading data from: ");
			if (dataSource.getType() == NagiosDataSourceType.TCP)
			{
				System.out.print(dataSource.getHost() + " " + dataSource.getPort());
				javNag.loadNagiosData(dataSource.getHost(), dataSource.getPort(), dataSource.getVersion());
			}
			else if (dataSource.getType() == NagiosDataSourceType.HTTP)
			{
				System.out.print(dataSource.getURL());
				javNag.loadNagiosData(dataSource.getURL(), dataSource.getVersion());
			}
			else if (dataSource.getType() == NagiosDataSourceType.FILE)
			{
				System.out.print(dataSource.getFile());
				javNag.loadNagiosData(dataSource.getFile(), dataSource.getVersion());
			}
			else
				throw new Exception("Unknown data-source type: " + dataSource.getType());

			System.out.println(" - done.");
		}

		long endLoadTs = System.currentTimeMillis();

		double took = (double)(endLoadTs - startLoadTs) / 1000.0;
		System.out.println("Took " + took + "s to load status data");

		statistics.addToTotalRefreshTime(took);
		statistics.addToNRefreshes(1);
	}

	public void findProblems() throws Exception
	{
		java.util.List<Problem> lessImportant = new ArrayList<Problem>();
		problems = new ArrayList<Problem>();

		// collect problems
		Problems.collectProblems(javNag, config.getPrioPatterns(), problems, lessImportant, config.getAlwaysNotify(), config.getAlsoAcknowledged());
		// sort problems
		Problems.sortList(problems, config.getSortOrder(), config.getSortOrderNumeric());
		Problems.sortList(lessImportant, config.getSortOrder(), config.getSortOrderNumeric());
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
		}
	}

	public static void showHelp()
	{
		System.out.println("--source type version x  Source to retrieve from.");
		System.out.println("              Type can be: http, tcp, file");
		System.out.println("              http expects an url like http://keetweej.vanheusden.com/status.dat");
		System.out.println("              tcp expects a host and portnumber, e.g.: keetweej.vanheusden.com 33333");
		System.out.println("              file expects a file-name, e.g. /var/cache/nagios3/status.dat");
		System.out.println("              version selects the nagios-version. E.g. 1, 2 or 3.");
		System.out.println("              You can add as many Nagios servers as you like.");
		System.out.println("              Example: --source file 3 /var/cache/nagios3/status.dat");
		System.out.println("");
		System.out.println("--nrows x     Number of rows to show, must be at least 2");
		System.out.println("--interval x  Retrieve status every x seconds");
		System.out.println("--version x   Set Nagios version of statusdata. Must be either 1, 2 or 3.");
		System.out.println("--image x     Display image x on background. Can be a filename or an http-URL. One can have multiple files/url which will be shown roundrobin.");
		System.out.println("--adapt-img   Reduce image-size to fit below the listed problems.");
		System.out.println("--random-img  Randomize order of images shown");
		System.out.println("--font x      Font to use. Default is 'Courier'.");
		System.out.println("--prefer x    File to load regular expressions from which tell what problems to show with priority (on top of the others).");
		System.out.println("--also-acknowledged Display acknowledged problems as well.");
		System.out.println("--always-notify	Also display problems for which notifications are disabled.");
		System.out.println("--bgcolor x   Select a background-color, used when there's something to notify about. Default is gray.");
		System.out.println("--list-bgcolors     Show a list of available colors.");
		System.out.println("--textcolor   Text color.");
		System.out.println("--sound x     Play sound when a warning/error state starts.");
		System.out.println("--counter     Show counter decreasing upto the point that a refresh will happen.");
		System.out.println("--exec x      Execute program when one or more errors are shown.");
		System.out.println("--predict x   File to write brain-dump to (and read from).");
		System.out.println("--config x    Load configuration from file x. This overrides all configurationsettings set previously.");
		System.out.println("--create-config x    Create new configuration file with filename x.");
		System.out.println("--listen-port Port to listen for the internal webserver.");
		System.out.println("--listen-adapter Network interface to listen for the internal webserver.");
		System.out.println("--header x    String to display in header. Can contain escapes, see below.");
		System.out.println("--host-issue x  String defining how to format host-issues.");
		System.out.println("--service-issue x  String defining how to format service-issues.");
		System.out.println("--no-header   Do not display the statistics line in the upper row.");
		System.out.println("--sort-order x  Sort on field x.");
		System.out.println(" OR");
		System.out.println("--sort-order-numeric x  Sort on field x, numeric.");
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
				if (arg[loop].compareTo("--create-config") == 0)
				{
					config.writeConfig(arg[++loop]);
					config.setConfigFilename(arg[loop]);
				}
				else if (arg[loop].compareTo("--source") == 0)
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
						throw new Exception("Nagios version '" + versionStr + "' not known.");

					if (type.equalsIgnoreCase("http"))
						nds = new NagiosDataSource(new URL(arg[++loop]), nv);
					else if (type.equalsIgnoreCase("file"))
						nds = new NagiosDataSource(arg[++loop], nv);
					else if (type.equalsIgnoreCase("tcp"))
					{
						String host = arg[++loop];
						int port = Integer.valueOf(arg[++loop]);
						nds = new NagiosDataSource(host, port, nv);
					}
					else
						throw new Exception("Data source-type '" + type + "' not understood.");

					config.addNagiosDataSource(nds);
				}
				else if (arg[loop].compareTo("--sort-order") == 0)
					config.setSortOrder(arg[++loop], false);
				else if (arg[loop].compareTo("--sort-order-numeric") == 0)
					config.setSortOrder(arg[++loop], true);
				else if (arg[loop].compareTo("--no-header") == 0)
					config.setShowHeader(false);
				else if (arg[loop].compareTo("--header") == 0)
					config.setHeader(arg[++loop]);
				else if (arg[loop].compareTo("--service-issue") == 0)
					config.setServiceIssue(arg[++loop]);
				else if (arg[loop].compareTo("--host-issue") == 0)
					config.setHostIssue(arg[++loop]);
				else if (arg[loop].compareTo("--random-img") == 0)
					config.setRandomWebcam(true);
				else if (arg[loop].compareTo("--no-gui") == 0)
					config.setRunGui(false);
				else if (arg[loop].compareTo("--config") == 0)
					config.loadConfig(arg[++loop]);
				else if (arg[loop].compareTo("--predict") == 0)
					config.setBrainFileName(arg[++loop]);
				else if (arg[loop].compareTo("--exec") == 0)
					config.setExec(arg[++loop]);
				else if (arg[loop].compareTo("--adapt-img") == 0)
					config.setAdaptImageSize(true);
				else if (arg[loop].compareTo("--counter") == 0)
					config.setCounter(true);
				else if (arg[loop].compareTo("--sound") == 0)
					config.setProblemSound(arg[++loop]);
				else if (arg[loop].compareTo("--listen-port") == 0)
					config.setHTTPServerListenPort(Integer.valueOf(arg[++loop]));
				else if (arg[loop].compareTo("--listen-adapter") == 0)
					config.setHTTPServerListenAdapter(arg[++loop]);
				else if (arg[loop].compareTo("--list-bgcolors") == 0)
				{
					config.listColors();
					System.exit(0);
				}
				else if (arg[loop].compareTo("--bgcolor") == 0)
					config.setBackgroundColor(arg[++loop]);
				else if (arg[loop].compareTo("--textcolor") == 0)
					config.setTextColor(arg[++loop]);
				else if (arg[loop].compareTo("--nrows") == 0)
					config.setNRows(Integer.valueOf(arg[++loop]));
				else if (arg[loop].compareTo("--interval") == 0)
					config.setSleepTime(Integer.valueOf(arg[++loop]));
				else if (arg[loop].compareTo("--image") == 0)
					config.addImageUrl(arg[++loop]);
				else if (arg[loop].compareTo("--prefer") == 0)
				{
					System.out.println("Loading prefers from " + arg[++loop]);
					config.loadPrefers(arg[loop]);
				}
				else if (arg[loop].compareTo("--always-notify") == 0)
					config.setAlwaysNotify(true);
				else if (arg[loop].compareTo("--also-acknowledged") == 0)
					config.setAlsoAcknowledged(true);
				else if (arg[loop].compareTo("--font") == 0)
					config.setFontName(arg[++loop]);
				else if (arg[loop].compareTo("--help") == 0 || arg[loop].compareTo("--h") == 0 )
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

			if (config.getNagiosDataSources().size() == 0)
			{
				System.err.println("You need to select at least one Nagios data-source.");
				System.exit(127);
			}

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

			CoffeeSaint coffeeSaint = new CoffeeSaint();
			Gui gui = null;
			if (config.getRunGui())
			{
				System.out.println("Start gui");
				gui = new Gui(config, coffeeSaint, statistics);
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
