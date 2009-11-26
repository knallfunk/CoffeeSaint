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

public class Gui extends JPanel
{
	final Config config;
	final CoffeeSaint coffeeSaint;
	final Statistics statistics;

	boolean lastState = false;	// false: no problems
	// because making a frame visible already causes
	// a call to paint() so no need to do that (again)
	// at start
	long lastRefresh = System.currentTimeMillis();

	public Gui(Config config, CoffeeSaint coffeeSaint, Statistics statistics)
	{
		this.config = config;
		this.coffeeSaint = coffeeSaint;
		this.statistics = statistics;
	}

	void drawRow(Graphics g, String msg, int row, String state, Color bgColor)
	{
		final int totalNRows = config.getNRows();
		final int rowHeight = getHeight() / totalNRows;

		final int y = rowHeight * row;

		g.setColor(coffeeSaint.stateToColor(state));
		g.fillRect(0, y, getWidth(), rowHeight);

		g.setColor(config.getTextColor());

		// stuff to set the font-size and found out where to put it
		Font f = new Font(config.getFontName(), Font.PLAIN, rowHeight);
		g.setFont(f);
		FontMetrics fm = g.getFontMetrics();
		double shrink = ((double)rowHeight / (double)fm.getHeight());
		double newSize = (double)rowHeight * shrink;
		double newAsc  = (double)fm.getAscent() * shrink;
		f = f.deriveFont((float)newSize);
		g.setFont(f);

		int plotY = y + (int)newAsc;
		System.out.println("row " + row + ", " + newSize + "|" + newAsc + " -> " + plotY + " RH: " + rowHeight);
		g.drawString(msg, 0, plotY);
	}

	public void drawCounter(Graphics g, int windowWidth, int windowHeight, int rowHeight, int characterSize, int counter)
	{
		/* counter upto the next reload */
		Font f = new Font(config.getFontName(), Font.PLAIN, rowHeight);
		g.setFont(f);
		FontMetrics fm = g.getFontMetrics();
		double shrink = ((double)rowHeight / (double)fm.getHeight());
		double newSize = (double)rowHeight * shrink;
		double newAsc  = (double)fm.getAscent() * shrink;
		f = f.deriveFont((float)newSize);
		g.setFont(f);

		String str = "" + config.getSleepTime();
		Rectangle2D boundingRectangle = f.getStringBounds(str, 0, str.length(), new FontRenderContext(null, false, false));

		int startX = windowWidth - (int)boundingRectangle.getWidth();

		g.setColor(config.getBackgroundColor());
		g.fillRect(startX, 0, (int)boundingRectangle.getWidth(), (int)boundingRectangle.getHeight());

		g.setColor(config.getTextColor());
		g.drawString("" + counter, startX, (int)newAsc);
	}

	public void displayImage(ImageParameters [] imageParameters, int nProblems, Graphics g, int rowHeight, boolean adaptImgSize, int windowWidth, int windowHeight)
	{
		int headerOffset = config.getShowHeader() ? 1 : 0;
		int curWindowHeight, offsetY;
		int maxW = -1, maxH = -1, nr;

		for(nr=0; nr<Math.min(config.getCamRows() * config.getCamCols(), imageParameters.length); nr++)
		{
			if (imageParameters[nr] == null)
				continue;

			maxW = Math.max(maxW, imageParameters[nr].getWidth());
			maxH = Math.max(maxH, imageParameters[nr].getHeight());
		}

		int totalWidth  = maxW * config.getCamCols();
		int totalHeight = maxH * config.getCamRows();

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

		if (curWindowHeight > 0)
		{
			double wMul = (double)windowWidth / (double)totalWidth;
			double hMul = (double)curWindowHeight / (double)totalHeight;
			double multiplier = Math.min(wMul, hMul);
			double spacingX = maxW * multiplier;
			double spacingY = maxH * multiplier;

			int putX = Math.max(0, (windowWidth / 2) - ((int)spacingX / 2) * config.getCamCols());
			int putY = Math.max(0, (curWindowHeight / 2) - ((int)spacingY / 2) * config.getCamRows()) + offsetY;

			nr = 0;
			for(int y=0; y<config.getCamRows(); y++)
			{
				for(int x=0; x<config.getCamCols(); x++)
				{
					int plotX = putX + (int)(x * spacingX);
					int plotY = putY + (int)(y * spacingY);

					if (imageParameters[nr] != null)
					{
						int curImgWidth = imageParameters[nr].getWidth();
						int curImgHeight = imageParameters[nr].getHeight();
						int newWidth  = (int)spacingX;
						int newHeight = (int)spacingY;
						if (config.getKeepAspectRatio())
						{
							double curWMul = spacingX / curImgWidth;
							double curHMul = spacingY / curImgHeight;
							double curMultiplier = Math.min(curWMul, curHMul);
							newWidth  = (int)((double)curImgWidth  * curMultiplier);
							newHeight = (int)((double)curImgHeight * curMultiplier);
						}
						plotX += Math.max(0, spacingX - newWidth) / 2;
						plotY += Math.max(0, spacingY - newHeight) / 2;
						g.drawImage(imageParameters[nr].getImage(), plotX, plotY, newWidth, newHeight, null);
					}
					else
					{
						g.drawString("n/a", plotX, plotY);
					}

					nr++;
				}
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
			Color bgColor = config.getBackgroundColor();

			/* block in upper right to inform about load */
			g.setColor(Color.BLUE);
			g.fillRect(windowWidth - characterSize, 0, characterSize, characterSize);

			if (config.getVerbose())
				drawRow(g, "Loading image(s)", 0, "0", bgColor);
			startLoadTs = System.currentTimeMillis();
			ImageParameters [] imageParameters = coffeeSaint.loadImage(this, g);
			endLoadTs = System.currentTimeMillis();

			took = (double)(endLoadTs - startLoadTs) / 1000.0;

			statistics.addToTotalImageLoadTime(took);

			String fontName = config.getFontName();
			System.out.println("Current font name: " + fontName);
			System.out.println("Current character size: " + characterSize);
			final Font f = new Font(fontName, Font.PLAIN, characterSize);
			g.setFont(f);

			/* find the problems in the nagios data */
			if (config.getVerbose())
				drawRow(g, "Loading Nagios data", 0, "0", bgColor);
			coffeeSaint.lockProblems();
			coffeeSaint.loadNagiosData(this, g);
			coffeeSaint.findProblems();
			java.util.List<Problem> problems = coffeeSaint.getProblems();

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
				drawRow(g, header, curNRows++, problems.size() == 0 ? "0" : "255", bgColor);
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

				drawRow(g, output, curNRows, currentProblem.getCurrent_state(), bgColor);
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
		System.out.println("Window size: " + getWidth() + "x" + getHeight());
		final int rowHeight = getHeight() / config.getNRows();
		final int characterSize = Math.max(10, rowHeight - 1);

		System.out.println("*** Paint PROBLEMS ");
		drawProblems(g, getWidth(), getHeight(), rowHeight, characterSize);
	}

	public void guiLoop() throws Exception
	{
		final Graphics g = getGraphics();
		long lastLeft = -1;

		for(;;)
		{
			long now = System.currentTimeMillis();
			long left = (long)config.getSleepTime() - ((now - lastRefresh) / 1000);
			int rowHeight = getHeight() / config.getNRows();
			int characterSize = rowHeight - 1;

			if (left <= 0)
			{
				lastRefresh = now;
				System.out.println("*** Update PROBLEMS " + left);
				drawProblems(g, getWidth(), getHeight(), rowHeight, characterSize);
			}
			else if (config.getCounter() && lastLeft != left)
			{
				System.out.println("*** update COUNTER " + left);
				drawCounter(g, getWidth(), getHeight(), rowHeight, characterSize, (int)left);
				lastLeft = left;
			}

			// scroller FIXME

			Thread.sleep(1000);
		}
	}
}
