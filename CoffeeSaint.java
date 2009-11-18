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
	static String version = "CoffeeSaint v1.0, (C) 2009 by folkert@vanheusden.com";

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
		else if (state.equals("255") == true)
			return config.getBackgroundColor();

		return Color.MAGENTA;
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

	public Color predictWithColor(Calendar rightNow)
	{
		Color bgColor = config.getBackgroundColorOkStatus();

		if (predictor != null)
		{
			Calendar future = Calendar.getInstance();
			future.add(Calendar.SECOND, config.getSleepTime());

			Double value = predictor.predict(rightNow, future);
			if (value != null && value != 0.0)
			{
				System.out.println("Expecting " + value + " problems after next interval");
				int red = 15 + (int)(value * 5.0);
				if (red < 0)
					red = 0;
				if (red > 255)
					red = 255;
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

	public void loadNagiosData() throws Exception
	{
		long startLoadTs = System.currentTimeMillis();

		if (config.getNagiosStatusHost() != null)
			javNag = new JavNag(config.getNagiosStatusHost(), config.getNagiosStatusPort(), config.getNagiosStatusVersion());
		else if (config.getNagiosStatusURL() != null)
			javNag = new JavNag(config.getNagiosStatusURL(), config.getNagiosStatusVersion());
		else
			javNag = new JavNag(config.getNagiosStatusFile(), config.getNagiosStatusVersion());

		long endLoadTs = System.currentTimeMillis();

		double took = (double)(endLoadTs - startLoadTs) / 1000.0;
		System.out.println("Took " + took + "s to load status data");

		statistics.addToTotalRefreshTime(took);
		statistics.addToNRefreshes(1);
	}

	public void findProblems() throws Exception
	{
		problems = new ArrayList<Problem>();

		Problems.collectProblems(javNag, config.getPrioPatterns(), problems, config.getAlwaysNotify(), config.getAlsoAcknowledged());

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
		System.out.println("--host x      Nagios host to connect to");
		System.out.println("--port x      Port via which to retrieve the Nagios status");
		System.out.println("  OR");
		System.out.println("--file x      File to load status from");
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
				else if (arg[loop].compareTo("--host") == 0)
					config.setNagiosStatusHost(arg[++loop]);
				else if (arg[loop].compareTo("--port") == 0)
					config.setNagiosStatusPort(Integer.valueOf(arg[++loop]));
				else if (arg[loop].compareTo("--file") == 0)
					config.setNagiosStatusFile(arg[++loop]);
				else if (arg[loop].compareTo("--url") == 0)
					config.setNagiosStatusURL(arg[++loop]);
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
				else if (arg[loop].compareTo("--version") == 0)
					config.setNagiosStatusVersion(arg[++loop]);
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

			int nSet = 0;
			nSet += (config.getNagiosStatusHost() != null ? 1 : 0);
			nSet += (config.getNagiosStatusFile() != null ? 1 : 0);
			nSet += (config.getNagiosStatusURL()  != null ? 1 : 0);
			if (nSet == 0)
			{
				System.err.println("You need to select a host with either --host (& --port), a file with --file or an URL with --url.");
				System.exit(127);
			}
			else if (nSet > 1)
			{
				System.err.println("--host, --file and --url are mutual exclusive.");
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
