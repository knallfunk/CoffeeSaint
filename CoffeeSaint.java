/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import java.awt.*;
import javax.swing.*;
import java.net.URL;
import java.awt.image.*;
import java.util.*;
import java.awt.event.WindowEvent;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.Semaphore;
import java.awt.geom.Rectangle2D;
import java.awt.font.FontRenderContext;

public class CoffeeSaint extends Frame
{
	static String host = null, file = null;
	static int port = 33333;
	static int nRows = 10;
	static int sleepTime = 30;
	static NagiosVersion nagiosVersion = NagiosVersion.V3;
	static String fontName = "Courier";
	static Image img = null;
	static int imgWidth = -1, imgHeight = -1;
	static java.util.List<Pattern> prioPatterns = new ArrayList<Pattern>();
	static boolean always_notify = false, also_acknowledged = false;
	static Color backgroundColor = Color.GRAY;
	static Color fontColor = Color.BLACK;
	static String problemSound = null;
	static boolean lastState = false;	// false: no problems
	static boolean counter = false;
	static int currentCounter = 0;
	static java.util.List<String> imageFiles = new ArrayList<String>();
	static int currentImageFile = 0;

	public CoffeeSaint()
	{
		super();
	}

	private static void showException(Exception e)
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

	protected void processWindowEvent(WindowEvent e)
	{
		System.out.println("processWindowEvent: " + e);

		if (e.getNewState() == WindowEvent.WINDOW_CLOSED)
			System.exit(0);
	}

	void addProblem(java.util.List<Problem> problems, java.util.List<Problem> lessImportant, String msg, String state)
	{
		boolean important = false;

		for(Pattern currentPattern : prioPatterns)
		{
			// System.out.println("Checking " + msg + " against " + currentPattern.pattern());
			if (currentPattern.matcher(msg).matches())
			{
				important = true;
				System.out.println("important: " + msg);
				break;
			}
		}

		if (important)
			problems.add(new Problem(msg, state));
		else
			lessImportant.add(new Problem(msg, state));
	}

	void collectProblems(JavNag javNag, java.util.List<Problem> problems)
	{
		java.util.List<Problem> lessImportant = new ArrayList<Problem>();

		for(Host currentHost: javNag.getListOfHosts())
		{
			assert currentHost != null;

			if (javNag.shouldIShowHost(currentHost, always_notify, also_acknowledged))
			{
				String msg = currentHost.getHostName();
				String state = currentHost.getParameter("current_state");
				String useState = null;

				if (state.equals("0")) /* UP = OK */
					useState = "0";
				else if (state.equals("1") || state.equals("2")) /* DOWN & UNREACHABLE = CRITICAL */
					useState = "2";
				else /* all other states (including 'pending' ("3")) are WARNING */
					useState = "1";

				addProblem(problems, lessImportant, msg, useState);
			}
			else
			{
				for(Service currentService : currentHost.getServices())
				{
					assert currentService != null;
					if (javNag.shouldIShowService(currentService, always_notify, also_acknowledged))
					{
						String msg = currentHost.getHostName() + ": " + currentService.getServiceName();
						String state = currentService.getParameter("current_state");

						addProblem(problems, lessImportant, msg, state);
					}
				}
			}
		}

		for(Problem currentLessImportant : lessImportant)
		{
			problems.add(currentLessImportant);
		}
	}

	String make2Digit(String in)
	{
		String newStr = "00" + in;

		return newStr.substring(newStr.length() - 2);
	}

	int setFont(Graphics g, int rowHeight)
	{
		Font f = new Font(fontName, Font.PLAIN, rowHeight);
		g.setFont(f);
		int fullHeight = g.getFontMetrics().getHeight();
		int newHeight = (int)((double)rowHeight * ((double)rowHeight / fullHeight));
		f = new Font(fontName, Font.PLAIN, newHeight);
		g.setFont(f);

		return newHeight;
	}

	void drawRow(Graphics g, int totalNRows, String msg, int row, String state, int windowWidth, int windowHeight, int rowHeight)
	{
		if (state.equals("0") == true)
			g.setColor(Color.GREEN);
		else if (state.equals("1") == true)
			g.setColor(Color.YELLOW);
		else if (state.equals("2") == true)
			g.setColor(Color.RED);
		else
			g.setColor(backgroundColor);

		final int y = rowHeight * row;

		g.fillRect(0, y, windowWidth, rowHeight);

		g.setColor(fontColor);

		int newHeight = setFont(g, rowHeight);

		g.drawString(msg, 0, y + newHeight);
	}

	public void drawCounter(Graphics g, int windowWidth, int windowHeight, int rowHeight, int characterSize)
	{
		/* counter upto the next reload */
		int newHeight = setFont(g, rowHeight);
		Font f = new Font(fontName, Font.PLAIN, newHeight);
		String str = "" + sleepTime;
		Rectangle2D boundingRectangle = f.getStringBounds(str, 0, str.length(), new FontRenderContext(null, false, false));
		g.setFont(f);
		g.setColor(backgroundColor);
		int startX = windowWidth - (int)boundingRectangle.getWidth();
		g.fillRect(startX, 0, (int)boundingRectangle.getWidth(), (int)boundingRectangle.getHeight());
		g.setColor(fontColor);
		f = new Font(fontName, Font.PLAIN, newHeight);
		g.setFont(f);
		g.drawString("" + currentCounter, startX, newHeight);
	}

	public void drawProblems(Graphics g, int windowWidth, int windowHeight, int rowHeight, int characterSize)
	{
		try
		{
			java.util.List<Problem> problems = new ArrayList<Problem>();

			if (imageFiles.size() > 0)
			{
				System.out.println("Load image " + imageFiles.get(currentImageFile));
				if (imageFiles.get(currentImageFile).substring(0, 7).equals("http://"))
					img = Toolkit.getDefaultToolkit().createImage(new URL(imageFiles.get(currentImageFile)));
				else
					img = Toolkit.getDefaultToolkit().createImage(imageFiles.get(currentImageFile));
				new ImageIcon(img); //loads the image
				Toolkit.getDefaultToolkit().sync();
				imgWidth = img.getWidth(null);
				imgHeight = img.getHeight(null);

				currentImageFile++;
				if (currentImageFile == imageFiles.size())
					currentImageFile = 0;
			}


			final Font f = new Font(fontName, Font.PLAIN, characterSize);
			g.setFont(f);

			/* block in upper right to inform about load */
			g.setColor(Color.BLUE);
			g.fillRect(windowWidth - characterSize, 0, characterSize, characterSize);

			/* load data from nagios server */
			long startLoadTs = System.currentTimeMillis();
			JavNag javNag;
			if (host != null)
				javNag = new JavNag(host, port, nagiosVersion);
			else
				javNag = new JavNag(file, nagiosVersion);
			long endLoadTs = System.currentTimeMillis();
			System.out.println("Took " + ((double)(endLoadTs - startLoadTs) / 1000.0) + "s to load status data");

			collectProblems(javNag, problems);
			Color bgColor = (problems.size() == 0) ? Color.GREEN : backgroundColor;

			/* clear frame */
			g.setColor(bgColor);
			g.fillRect(0, 0, windowWidth, windowHeight);

			if (img != null)
			{
				int curWindowHeight = rowHeight * (nRows - 1);
				double wMul = (double)windowWidth / (double)imgWidth;
				double hMul = (double)curWindowHeight / (double)imgHeight;
				int newWidth = windowWidth, newHeight = curWindowHeight;
				if (wMul > hMul)
					newWidth = (int)((double)imgHeight * wMul);
				else
					newHeight  = (int)((double)imgWidth  * hMul);
				int putX = Math.max(0, (windowWidth / 2) - (newWidth / 2));
				int putY = Math.max(0, (curWindowHeight / 2) - (newHeight / 2)) + rowHeight;

				g.drawImage(img, putX, putY, newWidth, newHeight, null);
			}

			Totals totals = javNag.calculateStatistics();
			Calendar rightNow = Calendar.getInstance();
			String msg = "" + totals.getNCritical() + "|" + totals.getNWarning() + "|" + totals.getNOk() + " - " + totals.getNUp() + "|" + totals.getNDown() + "|" + totals.getNUnreachable() + "|" + totals.getNPending() + " - " + make2Digit("" + rightNow.get(Calendar.HOUR_OF_DAY)) + ":" + make2Digit("" + rightNow.get(Calendar.MINUTE));
			int curNRows = 0;
			drawRow(g, nRows, msg, curNRows++, (bgColor == Color.GREEN) ? "0" : "255", windowWidth, windowHeight, rowHeight);

			for(Problem currentProblem : problems)
			{
				System.out.println(currentProblem.getCurrent_state() + ": " + currentProblem.getMessage());
				drawRow(g, nRows, currentProblem.getMessage(), curNRows, currentProblem.getCurrent_state(), windowWidth, windowHeight, rowHeight);
				curNRows++;

				if (curNRows == nRows)
					break;
			}

			if (problems.size() > 0)
			{
				if (problemSound != null && lastState == false)
				{
					System.out.println("Playing sound " + problemSound);
					new PlayWav(problemSound);
				}

				lastState = true;
			}
			else
			{
				lastState = false;
			}

		}
		catch(Exception e)
		{
			showException(e);

			/* block in upper right to inform about error */
			g.setColor(Color.RED);
			g.fillRect(windowWidth - characterSize, 0, characterSize, characterSize);
		}
	}

	public void paint(Graphics g)
	{
		final int windowWidth  = getSize().width;
		final int windowHeight = getSize().height;
		final int rowHeight = windowHeight / nRows;
		final int characterSize = rowHeight - 1;

		System.out.println("*** Paint PROBLEMS " + currentCounter);
		drawProblems(g, windowWidth, windowHeight, rowHeight, characterSize);
		currentCounter = sleepTime;
	}

	public void update(Graphics g)
	{
		final int windowWidth  = getSize().width;
		final int windowHeight = getSize().height;
		final int rowHeight = windowHeight / nRows;
		final int characterSize = rowHeight - 1;

		if (currentCounter == 0)
		{
			System.out.println("*** Update PROBLEMS " + currentCounter);
			drawProblems(g, windowWidth, windowHeight, rowHeight, characterSize);
			currentCounter = sleepTime;
		}
		else if (counter)
		{
			System.out.println("*** update COUNTER " + currentCounter);
			drawCounter(g, windowWidth, windowHeight, rowHeight, characterSize);
		}

		currentCounter--;
	}

	private static void loadPrefers(String fileName)
	{
		System.out.println("Loading prefers from " + fileName);

		try
		{
			String line;
			BufferedReader in = new BufferedReader(new FileReader(fileName));

			while((line = in.readLine()) != null)
			{
				System.out.println("Added pattern: " + line);
				prioPatterns.add(Pattern.compile(line));
			}

			in.close();
		}
		catch(Exception e)
		{
			showException(e);
			System.exit(127);
		}
	}

	public static void initColors(java.util.List<ColorPair> colorPairs)
	{
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

	public static void listColors(java.util.List<ColorPair> colorPairs)
	{
		System.out.println("Known colors: ");
		for(ColorPair currentColor : colorPairs)
			System.out.println("    " + currentColor.getName());
	}

	public static Color selectColor(java.util.List<ColorPair> colorPairs, String name)
	{
		for(ColorPair currentColor : colorPairs)
		{
			if (currentColor.equals(name))
				return currentColor.getColor();
		}

		return null;
	}

	public static void showHelp()
	{
		System.out.println("--host x      Nagios host to connect to");
		System.out.println("--port x      Port via which to retrieve the Nagios status (default: " + port + ")");
		System.out.println("  OR");
		System.out.println("--file x      File to load status from");
		System.out.println("");
		System.out.println("--nrows x     Number of rows to show, must be at least 2");
		System.out.println("--interval x  Retrieve status every x seconds");
		System.out.println("--version x   Set Nagios version of statusdata. Must be either 1, 2 or 3.");
		System.out.println("--image x     Display image x on background. Can be a filename or an http-URL. One can have multiple files/url which will be shown roundrobin.");
		System.out.println("--font x      Font to use. Default is 'Courier'.");
		System.out.println("--prefer x    File to load regular expressions from which tell what problems to show with priority (on top of the others).");
		System.out.println("--also-acknowledged Display acknowledged problems as well.");
		System.out.println("--always-notify	Also display problems for which notifications are disabled.");
		System.out.println("--bgcolor x   Select a background-color, used when there's something to notify about. Default is gray.");
		System.out.println("--list-bgcolors     Show a list of available colors.");
		System.out.println("--textcolor   Text color.");
		System.out.println("--sound x     Play sound when a warning/error state starts.");
		System.out.println("--counter     Show counter decreasing upto the point that a refresh will happen.");
	}

	public static void main(String[] arg)
	{
		java.util.List<ColorPair> colorPairs = new ArrayList<ColorPair>();

		initColors(colorPairs);

		System.out.println("CoffeeSaint v0.3, (C) 2009 by folkert@vanheusden.com");

		for(int loop=0; loop<arg.length; loop++)
		{
			if (arg[loop].compareTo("--host") == 0)
				host = arg[++loop];
			else if (arg[loop].compareTo("--file") == 0)
				file = arg[++loop];
			else if (arg[loop].compareTo("--counter") == 0)
				counter = true;
			else if (arg[loop].compareTo("--sound") == 0)
				problemSound = arg[++loop];
			else if (arg[loop].compareTo("--list-bgcolors") == 0)
			{
				listColors(colorPairs);
				System.exit(0);
			}
			else if (arg[loop].compareTo("--bgcolor") == 0)
			{
				backgroundColor = selectColor(colorPairs, arg[++loop]);
				if (backgroundColor == null)
				{
					System.err.println("Color " + arg[loop] + " is not known.");
					System.exit(127);
				}
			}
			else if (arg[loop].compareTo("--textcolor") == 0)
			{
				fontColor = selectColor(colorPairs, arg[++loop]);
				if (fontColor == null)
				{
					System.err.println("Color " + arg[loop] + " is not known.");
					System.exit(127);
				}
			}
			else if (arg[loop].compareTo("--port") == 0)
				port = Integer.valueOf(arg[++loop]);
			else if (arg[loop].compareTo("--nrows") == 0)
			{
				nRows = Integer.valueOf(arg[++loop]);
				if (nRows < 2)
				{
					System.err.println("--nrows expects a value of 2 or higher");
					System.exit(127);
				}
			}
			else if (arg[loop].compareTo("--interval") == 0)
			{
				sleepTime = Integer.valueOf(arg[++loop]);
				if (sleepTime < 1)
				{
					System.err.println("--interval requires a value of 1 or higher");
					System.exit(127);
				}
			}
			else if (arg[loop].compareTo("--version") == 0)
			{
				String version = arg[++loop];

				if (version.equals("1"))
					nagiosVersion = NagiosVersion.V1;
				else if (version.equals("2"))
					nagiosVersion = NagiosVersion.V2;
				else if (version.equals("3"))
					nagiosVersion = NagiosVersion.V3;
				else
				{
					System.err.println("Invalid nagios version selected.");
					System.exit(127);
				}
			}
			else if (arg[loop].compareTo("--image") == 0)
			{
				imageFiles.add(arg[++loop]);
			}
			else if (arg[loop].compareTo("--prefer") == 0)
			{
				loadPrefers(arg[++loop]);
			}
			else if (arg[loop].compareTo("--always-notify") == 0)
			{
				always_notify = true;
			}
			else if (arg[loop].compareTo("--also-acknowledged") == 0)
			{
				also_acknowledged = true;
			}
			else if (arg[loop].compareTo("--font") == 0)
			{
				fontName = arg[++loop];
			}
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

		if (host == null && file == null)
		{
			System.err.println("You need to select a host with either --host (& --port) or --file.");
			System.exit(127);
		}
		else if (host != null && file != null)
		{
			System.err.println("--host and --file are mutual exclusive.");
			System.exit(127);
		}

		try
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
