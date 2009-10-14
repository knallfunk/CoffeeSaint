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

public class CoffeeSaint extends JFrame
{
	static String host = "keetweej.vanheusden.com";
	static int port = 33333;
	static int nRows = 10;
	static int sleepTime = 30;
	static NagiosVersion nagiosVersion = NagiosVersion.V3;
	static String fontName = "Courier";
	static Image img = null;
	static int imgWidth = -1, imgHeight = -1;
	static java.util.List<Pattern> prioPatterns = new ArrayList<Pattern>();
	static boolean always_notify = false, also_acknowledged = false;

	public CoffeeSaint()
	{
		super();
	}

	private static void showException(Exception e)
	{
		System.out.println("Exception: " + e);
		System.out.println("Details: " + e.getMessage());
		System.out.println("Stack-trace:");
		for(StackTraceElement ste: e.getStackTrace())
		{
			System.out.println(" " + ste.getClassName() + ", "
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

	void drawRow(Graphics g, int totalNRows, String msg, int row, String state, int windowWidth, int windowHeight)
	{
		if (state.equals("0") == true)
			g.setColor(Color.GREEN);
		else if (state.equals("1") == true)
			g.setColor(Color.YELLOW);
		else if (state.equals("2") == true)
			g.setColor(Color.RED);
		else
			g.setColor(Color.GRAY);

		int rowHeight = windowHeight / totalNRows;
		int y = rowHeight * row;

		g.fillRect(0, y, windowWidth, rowHeight);

		g.setColor(Color.BLACK);

		g.drawString(msg, 0, y + rowHeight);
	}

	public void paint(Graphics g)
	{
		System.out.println("*** paint");

		final int windowWidth  = getSize().width;
		final int windowHeight = getSize().height;
		final int characterSize = windowHeight / nRows;

		try
		{
			java.util.List<Problem> problems = new ArrayList<Problem>();

			final Font f = new Font(fontName, Font.PLAIN, characterSize - 1);
			g.setFont(f);

			/* block in upper right to inform about load */
			g.setColor(Color.BLUE);
			g.fillRect(windowWidth - characterSize, 0, characterSize, characterSize);

			/* load data from nagios server */
			long startLoadTs = System.currentTimeMillis();
			JavNag javNag = new JavNag(host, port, nagiosVersion);
			long endLoadTs = System.currentTimeMillis();
			System.out.println("Took " + ((double)(endLoadTs - startLoadTs) / 1000.0) + "s to load status data");

			collectProblems(javNag, problems);
			Color bgColor = (problems.size() == 0) ? Color.GREEN : Color.GRAY;

			/* clear frame */
			g.setColor(bgColor);
			g.fillRect(0, 0, windowWidth, windowHeight);

			if (img != null)
			{
				int rowHeight = (windowHeight / nRows);
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
			drawRow(g, nRows, msg, curNRows++, (bgColor == Color.GREEN) ? "0" : "255", windowWidth, windowHeight);

			for(Problem currentProblem : problems)
			{
				System.out.println(currentProblem.getCurrent_state() + ": " + currentProblem.getMessage());
				drawRow(g, nRows, currentProblem.getMessage(), curNRows, currentProblem.getCurrent_state(), windowWidth, windowHeight);
				curNRows++;

				if (curNRows == nRows)
					break;
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

	public static void showHelp()
	{
		System.out.println("--host x      Nagios host to connect to");
		System.out.println("--port x      Port via which to retrieve the Nagios status");
		System.out.println("--nrows x     Number of rows to show, must be at least 2");
		System.out.println("--interval x  Retrieve status every x seconds");
		System.out.println("--version x   Set Nagios version of statusdata. Must be either 1, 2 or 3.");
		System.out.println("--image x     Display image x on background. Can be a filename or an http-URL.");
		System.out.println("--font x      Font to use. Default is 'Courier'.");
		System.out.println("--prefer x    File to load regular expressions from which tell what problems to show with priority (on top of the others).");
		System.out.println("--also-acknowledged Display acknowledged problems as well.");
		System.out.println("--always-notify	Also display problems for which notifications are disabled.");
	}

	public static void main(String[] arg)
	{
		String imageFile = null;

		System.out.println("CoffeeSaint v0.1, (C) 2009 by folkert@vanheusden.com");

		for(int loop=0; loop<arg.length; loop++)
		{
			if (arg[loop].compareTo("--host") == 0)
				host = arg[++loop];
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
				imageFile = arg[++loop];
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

			for(;;)
			{
				if (imageFile != null)
				{
					if (imageFile.substring(0, 7).equals("http://"))
						img = Toolkit.getDefaultToolkit().createImage(new URL(imageFile));
					else
						img = Toolkit.getDefaultToolkit().createImage(imageFile);
					new ImageIcon(img); //loads the image
					Toolkit.getDefaultToolkit().sync();
					imgWidth = img.getWidth(null);
					imgHeight = img.getHeight(null);
				}

				frame.repaint();
				frame.setVisible(true);

				Thread.sleep(sleepTime * 1000);
			}
		}
		catch(Exception e)
		{
			showException(e);
		}
	}
}
