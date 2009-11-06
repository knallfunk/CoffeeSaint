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

public class CoffeeSaint extends Frame
{
	static String version = "CoffeeSaint v0.7beta-003, (C) 2009 by folkert@vanheusden.com";

	static Config config;

	static boolean lastState = false;	// false: no problems
	static int currentCounter = 0;
	//
	static Predictor predictor;
	static long lastPredictorDump = 0;
	//
	static int currentImageFile = 0;
	static Semaphore imageSemaphore = new Semaphore(1);
	static ImageParameters imageParameters;
	// deze in een apart object FIXME
	static Semaphore statisticsSemaphore = new Semaphore(1);
	static double totalRefreshTime, runningSince, totalImageLoadTime;
	static int nRefreshes;
	static Semaphore problemsSemaphore = new Semaphore(1);
	static java.util.List<Problem> problems;

	public CoffeeSaint()
	{
		super();
	}

	public java.util.List<Problem> getProblems()
	{
		return problems;
	}

	static public Semaphore getProblemsSemaphore()
	{
		return problemsSemaphore;
	}

	static public Semaphore getStatisticsSemaphore()
	{
		return statisticsSemaphore;
	}

	static public Semaphore getImageSemaphore()
	{
		return imageSemaphore;
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

	int setFont(Graphics g, int rowHeight)
	{
		Font f = new Font(config.getFontName(), Font.PLAIN, rowHeight);
		g.setFont(f);
		int fullHeight = g.getFontMetrics().getHeight();
		int newHeight = (int)((double)rowHeight * ((double)rowHeight / fullHeight));
		f = new Font(config.getFontName(), Font.PLAIN, newHeight);
		g.setFont(f);

		return newHeight;
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

	void drawRow(Graphics g, int totalNRows, String msg, int row, String state, int windowWidth, int windowHeight, int rowHeight, Color bgColor)
	{
		g.setColor(stateToColor(state));

		final int y = rowHeight * row;

		g.fillRect(0, y, windowWidth, rowHeight);

		g.setColor(config.getTextColor());

		int newHeight = setFont(g, rowHeight);

		g.drawString(msg, 0, y + newHeight);
	}

	public void drawCounter(Graphics g, int windowWidth, int windowHeight, int rowHeight, int characterSize)
	{
		/* counter upto the next reload */
		int newHeight = setFont(g, rowHeight);
		Font f = new Font(config.getFontName(), Font.PLAIN, newHeight);
		String str = "" + config.getSleepTime();
		Rectangle2D boundingRectangle = f.getStringBounds(str, 0, str.length(), new FontRenderContext(null, false, false));
		g.setFont(f);
		g.setColor(config.getBackgroundColor());
		int startX = windowWidth - (int)boundingRectangle.getWidth();
		g.fillRect(startX, 0, (int)boundingRectangle.getWidth(), (int)boundingRectangle.getHeight());
		g.setColor(config.getTextColor());
		f = new Font(config.getFontName(), Font.PLAIN, newHeight);
		g.setFont(f);
		g.drawString("" + currentCounter, startX, newHeight);
	}

	public ImageParameters loadImage() throws Exception
	{
		if (config.getNImageUrls() > 0)
		{
			Image img;
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

			currentImageFile++;
			if (currentImageFile == config.getNImageUrls())
				currentImageFile = 0;

			return new ImageParameters(img, loadImage, imgWidth, imgHeight);
		}

		return null;
	}

	public Color predictWithColor(Calendar rightNow)
	{
		Color bgColor = Color.GREEN;

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

	public void displayImage(ImageParameters imageParameters, int nProblems, Graphics g, int rowHeight, boolean adaptImgSize, int windowWidth, int windowHeight)
	{
		int curWindowHeight, offsetY;

		if (adaptImgSize)
		{
			curWindowHeight = rowHeight * (config.getNRows() - (1 + nProblems));
			offsetY = (1 + nProblems) * rowHeight;
		}
		else
		{
			curWindowHeight = rowHeight * (config.getNRows() - 1);
			offsetY = rowHeight;
		}

		if (imageParameters.getWidth() == -1 || imageParameters.getHeight() == -1)
		{
			g.setColor(Color.RED);
			String msg = "Could not load image " + imageParameters.getFileName();
			System.out.println(msg);
			g.drawString(msg, 0, windowHeight - rowHeight);
		}
		else
		{
			if (curWindowHeight > 0)
			{
				double wMul = (double)windowWidth / (double)imageParameters.getWidth();
				double hMul = (double)curWindowHeight / (double)imageParameters.getHeight();
				double multiplier = Math.min(wMul, hMul);
				int newWidth  = (int)((double)imageParameters.getWidth()  * multiplier);
				int newHeight = (int)((double)imageParameters.getHeight() * multiplier);
				int putX = Math.max(0, (windowWidth / 2) - (newWidth / 2));
				int putY = Math.max(0, (curWindowHeight / 2) - (newHeight / 2)) + offsetY;

				g.drawImage(imageParameters.getImage(), putX, putY, newWidth, newHeight, null);
			}
		}
	}

	public void showCoffeeSaintProblem(Exception e, Graphics g, int windowWidth, int characterSize, int rowHeight)
	{
		/* block in upper right to inform about error */
		g.setColor(Color.RED);
		g.fillRect(windowWidth - characterSize, 0, characterSize, characterSize);

		final String msg = "Error: " + e;
		final int characterSizeError = Math.max(10, windowWidth / msg.length());
		final Font f = new Font(config.getFontName(), Font.PLAIN, characterSizeError);
		g.setFont(f);
		final int y = rowHeight * (config.getNRows() - 1);
		g.setColor(Color.RED);
		g.fillRect(0, y, windowWidth, rowHeight);
		g.setColor(Color.MAGENTA);
		g.drawString(msg, 0, y + characterSizeError);
	}

	public void drawProblems(Graphics g, int windowWidth, int windowHeight, int rowHeight, int characterSize)
	{
		try
		{
			String loadImage = null;
			long startLoadTs, endLoadTs;
			double took;

			startLoadTs = System.currentTimeMillis();
			imageSemaphore.acquire();
			imageParameters = loadImage();
			imageSemaphore.release();
			endLoadTs = System.currentTimeMillis();
			took = (double)(endLoadTs - startLoadTs) / 1000.0;
			statisticsSemaphore.acquire();
			totalImageLoadTime += took;
			statisticsSemaphore.release();

			final Font f = new Font(config.getFontName(), Font.PLAIN, characterSize);
			g.setFont(f);

			/* block in upper right to inform about load */
			g.setColor(Color.BLUE);
			g.fillRect(windowWidth - characterSize, 0, characterSize, characterSize);

			/* load data from nagios server */
			startLoadTs = System.currentTimeMillis();
			JavNag javNag;
			if (config.getNagiosStatusHost() != null)
				javNag = new JavNag(config.getNagiosStatusHost(), config.getNagiosStatusPort(), config.getNagiosStatusVersion());
			else if (config.getNagiosStatusURL() != null)
				javNag = new JavNag(config.getNagiosStatusURL(), config.getNagiosStatusVersion());
			else
				javNag = new JavNag(config.getNagiosStatusFile(), config.getNagiosStatusVersion());
			endLoadTs = System.currentTimeMillis();
			took = (double)(endLoadTs - startLoadTs) / 1000.0;
			System.out.println("Took " + took + "s to load status data");
			statisticsSemaphore.acquire();
			totalRefreshTime += took;
			nRefreshes++;
			statisticsSemaphore.release();

			problemsSemaphore.acquire();
			problems = new ArrayList<Problem>();
			Problems.collectProblems(javNag, config.getPrioPatterns(), problems, config.getAlwaysNotify(), config.getAlsoAcknowledged());
			Color bgColor = config.getBackgroundColor();
			Calendar rightNow = Calendar.getInstance();

			if (problems.size() == 0)
				bgColor = predictWithColor(rightNow);
			if (predictor != null)
				learnProblems(rightNow, problems.size());

			/* clear frame */
			g.setColor(bgColor);
			g.fillRect(0, 0, windowWidth, windowHeight);

			imageSemaphore.acquire();
			if (imageParameters != null)
				displayImage(imageParameters, problems.size(), g, rowHeight, config.getAdaptImageSize(), windowWidth, windowHeight);
			imageSemaphore.release();

			Totals totals = javNag.calculateStatistics();
			String msg = "" + totals.getNCritical() + "|" + totals.getNWarning() + "|" + totals.getNOk() + " - " + totals.getNUp() + "|" + totals.getNDown() + "|" + totals.getNUnreachable() + "|" + totals.getNPending() + " - " + make2Digit("" + rightNow.get(Calendar.HOUR_OF_DAY)) + ":" + make2Digit("" + rightNow.get(Calendar.MINUTE));
			int curNRows = 0;
			drawRow(g, config.getNRows(), msg, curNRows++, problems.size() == 0 ? "0" : "255", windowWidth, windowHeight, rowHeight, bgColor);

			for(Problem currentProblem : problems)
			{
				System.out.println(currentProblem.getCurrent_state() + ": " + currentProblem.getMessage());
				drawRow(g, config.getNRows(), currentProblem.getMessage(), curNRows, currentProblem.getCurrent_state(), windowWidth, windowHeight, rowHeight, bgColor);
				curNRows++;

				if (curNRows == config.getNRows())
					break;
			}

			if (problems.size() > 0)
			{
				if (lastState == false)
				{
					if (config.getProblemSound() != null)
					{
						System.out.println("Playing sound " + config.getProblemSound());
						new PlayWav(config.getProblemSound());
					}

					if (config.getExec() != null)
					{
						System.out.println("Invoking " + config.getExec());
						Runtime.getRuntime().exec(config.getExec());
					}
				}

				lastState = true;
			}
			else
			{
				lastState = false;
			}
			problemsSemaphore.release();

			System.out.println("Memory usage: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)) + "MB");
		}
		catch(Exception e)
		{
			showException(e);

			showCoffeeSaintProblem(e, g, windowWidth, characterSize, rowHeight);
		}
	}

	public void paint(Graphics g)
	{
		final int windowWidth  = getSize().width;
		final int windowHeight = getSize().height;
		final int rowHeight = windowHeight / config.getNRows();
		final int characterSize = rowHeight - 1;

		System.out.println("*** Paint PROBLEMS " + currentCounter);
		drawProblems(g, windowWidth, windowHeight, rowHeight, characterSize);
		currentCounter = config.getSleepTime();
		System.out.println("Sleep time: " + currentCounter);
	}

	public void update(Graphics g)
	{
		final int windowWidth  = getSize().width;
		final int windowHeight = getSize().height;
		final int rowHeight = windowHeight / config.getNRows();
		final int characterSize = rowHeight - 1;

		if (currentCounter <= 0)
		{
			System.out.println("*** Update PROBLEMS " + currentCounter);
			drawProblems(g, windowWidth, windowHeight, rowHeight, characterSize);
			currentCounter = config.getSleepTime();
		}
		else if (config.getCounter())
		{
			System.out.println("*** update COUNTER " + currentCounter);
			drawCounter(g, windowWidth, windowHeight, rowHeight, characterSize);
		}

		currentCounter--;
	}

	public static CoffeeSaint initGraphics()
	{
		/* retrieve max window size */
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		GraphicsConfiguration [] gc = gs[0].getConfigurations();
		Rectangle r = gc[0].getBounds();

		/* create frame to draw in */
		CoffeeSaint frame = new CoffeeSaint();
		frame.setSize(r.width, r.height);
		System.out.println("Initial paint");

		frame.setVisible(true);

		frame.addWindowListener(new FrameListener(config));

		return frame;
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
		System.out.print("Known colors:");
		config.listColors();
	}

	public static void main(String[] arg)
	{
		try
		{
			statisticsSemaphore.acquire();
			runningSince = System.currentTimeMillis();
			statisticsSemaphore.release();

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

			CoffeeSaint frame = initGraphics();

			if (config.getHTTPServerListenPort() != -1)
				new Thread(new HTTPServer(config, frame, config.getHTTPServerListenAdapter(), config.getHTTPServerListenPort())).start();

			for(;;)
			{
				System.out.println("Invoke paint");

				frame.repaint();

				System.out.println("Sleep");

				Thread.sleep(1000);
			}
		}
		catch(Exception e)
		{
			showException(e);
		}
	}
}
