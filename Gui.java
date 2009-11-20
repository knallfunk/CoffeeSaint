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

public class Gui extends Frame
{
	final Config config;
	final CoffeeSaint coffeeSaint;
	final Statistics statistics;

	boolean lastState = false;	// false: no problems
	int currentCounter = 0;

	public Gui(Config config, CoffeeSaint coffeeSaint, Statistics statistics)
	{
		this.config = config;
		this.coffeeSaint = coffeeSaint;
		this.statistics = statistics;

                /* retrieve max window size */
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice[] gs = ge.getScreenDevices();
                GraphicsConfiguration [] gc = gs[0].getConfigurations();
                Rectangle r = gc[0].getBounds();

                /* create frame to draw in */
                CoffeeSaint frame = new CoffeeSaint();
                setSize(r.width, r.height);
                System.out.println("Initial paint");

                setVisible(true);

                addWindowListener(new FrameListener(config));
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

	void drawRow(Graphics g, int totalNRows, String msg, int row, String state, int windowWidth, int windowHeight, int rowHeight, Color bgColor)
	{
		g.setColor(coffeeSaint.stateToColor(state));

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

	public void displayImage(ImageParameters imageParameters, int nProblems, Graphics g, int rowHeight, boolean adaptImgSize, int windowWidth, int windowHeight)
	{
		int headerOffset = config.getShowHeader() ? 1 : 0;
		int curWindowHeight, offsetY;

		if (adaptImgSize)
		{
			curWindowHeight = rowHeight * (config.getNRows() - (headerOffset + nProblems));
			offsetY = (headerOffset + nProblems) * rowHeight;
		}
		else
		{
			curWindowHeight = rowHeight * (config.getNRows() - headerOffset);
			offsetY = rowHeight * headerOffset;
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
		System.out.println("Graphics: " + g);

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
		g.setColor(Color.BLACK);
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
			ImageParameters imageParameters = coffeeSaint.loadImage();
			endLoadTs = System.currentTimeMillis();

			took = (double)(endLoadTs - startLoadTs) / 1000.0;

			statistics.addToTotalImageLoadTime(took);

			String fontName = config.getFontName();
			System.out.println("Current font name: " + fontName);
			System.out.println("Current character size: " + characterSize);
			final Font f = new Font(fontName, Font.PLAIN, characterSize);
			g.setFont(f);

			/* block in upper right to inform about load */
			g.setColor(Color.BLUE);
			g.fillRect(windowWidth - characterSize, 0, characterSize, characterSize);

			/* find the problems in the nagios data */
			coffeeSaint.lockProblems();
			coffeeSaint.loadNagiosData();
			coffeeSaint.findProblems();
			java.util.List<Problem> problems = coffeeSaint.getProblems();

			Color bgColor = config.getBackgroundColor();
			Calendar rightNow = Calendar.getInstance();

			if (problems.size() == 0)
				bgColor = coffeeSaint.predictWithColor(rightNow);

			/* clear frame */
			g.setColor(bgColor);
			g.fillRect(0, 0, windowWidth, windowHeight);

			if (imageParameters != null)
				displayImage(imageParameters, problems.size(), g, rowHeight, config.getAdaptImageSize(), windowWidth, windowHeight);

			JavNag javNag = coffeeSaint.getNagiosData();
			System.out.println("JavNag: " + javNag);

			int curNRows = 0;

			if (config.getShowHeader())
			{
				String header = coffeeSaint.getScreenHeader(javNag, rightNow);
				drawRow(g, config.getNRows(), header, curNRows++, problems.size() == 0 ? "0" : "255", windowWidth, windowHeight, rowHeight, bgColor);
			}

			for(Problem currentProblem : problems)
			{
				String escapeString;
				if (currentProblem.getService() == null)
					escapeString = config.getHostIssue();
				else
					escapeString = config.getServiceIssue();
				String output = coffeeSaint.processStringWithEscapes(escapeString, javNag, rightNow, currentProblem);

				System.out.println(output);

				drawRow(g, config.getNRows(), output, curNRows, currentProblem.getCurrent_state(), windowWidth, windowHeight, rowHeight, bgColor);
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
			coffeeSaint.unlockProblems();

			System.out.println("Memory usage: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)) + "MB");
		}
		catch(Exception e)
		{
			statistics.incExceptions();

			CoffeeSaint.showException(e);

			if (g != null)
				showCoffeeSaintProblem(e, g, windowWidth, characterSize, rowHeight);
		}
	}

	public void paint(Graphics g)
	{
		final int windowWidth  = getSize().width;
		final int windowHeight = getSize().height;
		System.out.println("Window size: " + windowWidth + "x" + windowHeight);
		final int rowHeight = windowHeight / config.getNRows();
		final int characterSize = Math.max(10, rowHeight - 1);

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

	public void guiLoop() throws Exception
	{
		for(;;)
		{
			System.out.println("Invoke paint");

			repaint();

			Thread.sleep(1000);
		}
	}
}
