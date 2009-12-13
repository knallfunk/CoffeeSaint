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

public class Gui extends JPanel implements ImageObserver
{
	final Config config;
	final CoffeeSaint coffeeSaint;
	final Statistics statistics;
	BufferedImage currentHeader;

	boolean lastState = false;	// false: no problems
	// because making a frame visible already causes
	// a call to paint() so no need to do that (again)
	// at start
	long lastRefresh = System.currentTimeMillis();

	public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height)
	{
		String add = ", img: " + img + ", flags: " + flags + ", x/y: " + x + "," + y + ", width: " + width + ", height: " + height;
		if ((flags & ABORT) != 0)
			CoffeeSaint.log.add("Image aborted" + add);
		else if ((flags & ERROR) != 0)
			CoffeeSaint.log.add("Image error" + add);
		else if ((flags & ALLBITS) != 0)
			CoffeeSaint.log.add("Image complete" + add);
		else
			CoffeeSaint.log.add("Image ???" + add);

		// If status is not COMPLETE then we need more updates.
		return (flags & (ALLBITS|ABORT)) == 0;
	}

	public Gui(Config config, CoffeeSaint coffeeSaint, Statistics statistics)
	{
		this.config = config;
		this.coffeeSaint = coffeeSaint;
		this.statistics = statistics;
	}

	public void configureRendered(Graphics2D g, boolean enable)
	{
		if (config.getAntiAlias())
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, enable ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

		if (config.getMaxQualityGraphics())
		{
			g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, enable ? RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY : RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);

			g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, enable ? RenderingHints.VALUE_COLOR_RENDER_QUALITY : RenderingHints.VALUE_COLOR_RENDER_DEFAULT);

			g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, enable ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);

			g.setRenderingHint(RenderingHints.KEY_RENDERING, enable ? RenderingHints.VALUE_RENDER_QUALITY : RenderingHints.VALUE_RENDER_DEFAULT);
		}
	}

	void drawRow(Graphics gTo, int windowWidth, String msg, int row, String state, Color bgColor, int nCols, int colNr, float seeThrough, BufferedImage sparkLine)
	{
		System.out.println("DRAW ROW: " + msg + " " + row);
		final int totalNRows = config.getNRows();
		final int rowHeight = getHeight() / totalNRows;
		final int rowColWidth = windowWidth / nCols;
		final int xStart = rowColWidth * colNr;
		boolean shrunkMore = false;

		BufferedImage output = new BufferedImage(rowColWidth, rowHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = output.createGraphics();

		configureRendered(g, true);

		g.setColor(coffeeSaint.stateToColor(state));
		g.fillRect(0, 0, rowColWidth, rowHeight);

		g.setColor(config.getTextColor());

		String font = config.getFontName();
		if (state.equals("1"))
			font = config.getWarningFontName();
		else if (state.equals("2"))
			font = config.getCriticalFontName();

		// stuff to set the font-size and found out where to put it
		Font f = new Font(font, Font.PLAIN, rowHeight);
		g.setFont(f);
		FontMetrics fm = g.getFontMetrics();
		double shrink = ((double)rowHeight / (double)fm.getHeight());
		if (config.getReduceTextWidth())
		{
			Rectangle2D boundingRectangle = f.getStringBounds(msg, 0, msg.length(), new FontRenderContext(null, false, false));

			double newShrink = (double)(Math.max(0, rowColWidth - (sparkLine != null ? sparkLine.getWidth() : 0))) / (double)boundingRectangle.getWidth();
			if (newShrink < shrink)
			{
				shrink = newShrink;
				shrunkMore = true;
			}
		}
		double newSize = (double)rowHeight * shrink;
		double newAsc  = (double)fm.getAscent() * shrink;
		f = f.deriveFont((float)newSize);
		g.setFont(f);

		if (shrunkMore == true)
		{
			double heightDiff = (double)fm.getAscent() - newAsc;
			int newY = (int)(heightDiff / 2.0 + newAsc);

			// System.out.println("newAsc: " + newAsc + ", heightDiff: " + heightDiff + ", rowHeight: " + rowHeight + ", newy: " + newY + " " + msg);

			g.drawString(msg, 1, newY);
		}
		else
			g.drawString(msg, 1, (int)newAsc);

		if (sparkLine != null)
			g.drawImage(sparkLine, rowColWidth - sparkLine.getWidth(), 0, null);

		Graphics2D gTo2D = (Graphics2D)gTo;

		int x = xStart;
		int y = rowHeight * row;
		if (seeThrough != 1.0)
		{
			float [] scales  = { 1f, 1f, 1f, seeThrough };
			float [] offsets = { 0f, 0f, 0f, 0f   };
			gTo2D.drawImage(output, new RescaleOp(scales, offsets, null), x, y);
		}
		else
		{
			gTo2D.drawImage((Image)output, x, y, null);
		}
	}

	public BufferedImage createHeaderImage(String header, String state, Color bgColor, int rowHeight)
	{
		BufferedImage dummy = new BufferedImage(65536, rowHeight, BufferedImage.TYPE_INT_RGB);
		Graphics g2 = dummy.createGraphics();
		int imageWidth = g2.getFontMetrics(new Font(config.getFontName(), Font.PLAIN, rowHeight)).stringWidth(header);
		BufferedImage output = new BufferedImage(imageWidth, rowHeight, BufferedImage.TYPE_INT_RGB);

		drawRow(output.createGraphics(), imageWidth, header, 0, state, bgColor, 1, 0, 1.0f, null);

		return output;
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
						CoffeeSaint.log.add("Draw image: " + imageParameters[nr].getImage() + " (" + imageParameters[nr].getFileName() + ")");
						if (g.drawImage(imageParameters[nr].getImage(), plotX, plotY, newWidth, newHeight, this) == false)
							CoffeeSaint.log.add("drawImage " + imageParameters[nr].getImage() + " returns false");
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

	public void showCoffeeSaintProblem(Exception e, Graphics g, int windowWidth, int rowHeight)
	{
		CoffeeSaint.log.add("Graphics: " + g);

		/* block in upper right to inform about error */
		g.setColor(Color.RED);
		g.fillRect(windowWidth - rowHeight, 0, rowHeight, rowHeight);

		drawRow(g, windowWidth, "Error: " + e, config.getNRows() - 1, "2", Color.GRAY, 1, 0, 1.0f, null);
	}

	synchronized public void drawProblems(Graphics g, int windowWidth, int windowHeight, int rowHeight)
	{
		System.out.println(">>> DRAW PROBLEMS START <<<");
		try
		{
			String loadImage = null;
			long startLoadTs, endLoadTs;
			double took;
			Color bgColor = config.getBackgroundColor();

			/* block in upper right to inform about load */
			g.setColor(Color.BLUE);
			g.fillRect(windowWidth - rowHeight, 0, rowHeight, rowHeight);

			if (config.getVerbose())
				drawRow(g, windowWidth, "Loading image(s)", 0, "0", bgColor, 1, 0, 1.0f, null);
			startLoadTs = System.currentTimeMillis();
			ImageParameters [] imageParameters = coffeeSaint.loadImage(this, windowWidth, g);
			endLoadTs = System.currentTimeMillis();

			took = (double)(endLoadTs - startLoadTs) / 1000.0;

			statistics.addToTotalImageLoadTime(took);

			String fontName = config.getFontName();
			final Font f = new Font(fontName, Font.PLAIN, rowHeight);
			g.setFont(f);

			/* find the problems in the nagios data */
			if (config.getVerbose())
				drawRow(g, windowWidth, "Loading Nagios data", 0, "0", bgColor, 1, 0, 1.0f, null);
			JavNag javNag = CoffeeSaint.loadNagiosData(this, windowWidth, g);
			coffeeSaint.collectPerformanceData(javNag);
			java.util.List<Problem> problems = CoffeeSaint.findProblems(javNag);
			coffeeSaint.learnProblemCount(problems.size());

			Calendar rightNow = Calendar.getInstance();

			if (problems.size() == 0)
				bgColor = coffeeSaint.predictWithColor(rightNow);

			/* clear frame */
			g.setColor(bgColor);
			g.fillRect(0, 0, windowWidth, windowHeight);

			if (imageParameters != null)
				displayImage(imageParameters, problems.size(), g, rowHeight, config.getAdaptImageSize(), windowWidth, windowHeight);

			int curNRows = 0;

			if (config.getShowHeader())
			{
				String header = coffeeSaint.getScreenHeader(javNag, rightNow);
				if (config.getScrollingHeader())
					currentHeader = createHeaderImage(header, problems.size() == 0 ? "0" : "255", bgColor, rowHeight);
				else
					drawRow(g, windowWidth, header, curNRows, problems.size() == 0 ? "0" : "255", bgColor, 1, 0, 1.0f, null);
				curNRows++;
			}

			int colNr = 0;
			int dummyNRows = config.getNRows() - (config.getShowHeader() ? 1 : 0);
			int curNColumns;
			if (config.getFlexibleNColumns())
				curNColumns = Math.min(config.getNProblemCols(), (problems.size() + dummyNRows - 1) / dummyNRows);
			else
				curNColumns = config.getNProblemCols();
			for(Problem currentProblem : problems)
			{
				String escapeString;
				if (currentProblem.getService() == null)
					escapeString = config.getHostIssue();
				else
					escapeString = config.getServiceIssue();
				String output = coffeeSaint.processStringWithEscapes(escapeString, javNag, rightNow, currentProblem);

				CoffeeSaint.log.add(output);

				BufferedImage sparkLine = null;
				int sparkLineWidth = config.getSparkLineWidth();
				if (sparkLineWidth > 0)
					sparkLine = coffeeSaint.getSparkLine(currentProblem.getHost(), currentProblem.getService(), sparkLineWidth, rowHeight - (config.getRowBorder()?1:0));

				drawRow(g, windowWidth, output, curNRows, currentProblem.getCurrent_state(), bgColor, curNColumns, colNr, config.getTransparency(), sparkLine);

				curNRows++;

				if (curNRows == config.getNRows())
				{
					curNRows = config.getShowHeader() ? 1 : 0;
					colNr++;
					if (colNr == curNColumns)
						break;
				}
			}

			if (config.getRowBorder())
			{
				configureRendered((Graphics2D)g, false);

				g.setColor(config.getRowBorderColor());
				if (problems.size() > config.getNRows())
				{
					for(int rowColumns=0; rowColumns < config.getNProblemCols(); rowColumns++)
					{
						int x = (windowWidth * rowColumns) / curNColumns;
						int y =  config.getShowHeader() ? rowHeight : 0;
						g.drawLine(x, y, x, rowHeight * config.getNRows());
					}
				}

				if (problems.size() > 0)
				{
					for(int rowsRow=0; rowsRow < Math.min(config.getNRows(), problems.size() + (config.getShowHeader() ? 1 : 0)); rowsRow++)
					{
						int drawY = rowHeight + rowsRow * rowHeight;
						g.drawLine(0, drawY, windowWidth, drawY);
					}

				}

				configureRendered((Graphics2D)g, true);
			}

			if (problems.size() > 0)
			{
				if (lastState == false)
				{
					if (config.getProblemSound() != null)
					{
						CoffeeSaint.log.add("Playing sound " + config.getProblemSound());
						new PlayWav(config.getProblemSound());
					}

					if (config.getExec() != null)
					{
						CoffeeSaint.log.add("Invoking " + config.getExec());
						Runtime.getRuntime().exec(config.getExec());
					}
				}

				lastState = true;
			}
			else
			{
				lastState = false;
			}

			CoffeeSaint.log.add("Memory usage: " + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)) + "MB");
		}
		catch(Exception e)
		{
			statistics.incExceptions();

			CoffeeSaint.showException(e);

			if (g != null)
				showCoffeeSaintProblem(e, g, windowWidth, rowHeight);
		}
		System.out.println(">>> DRAW PROBLEMS END <<<");
	}

	public void paintComponent(Graphics g)
	{
		System.out.println("+++ PAINT START +++ " + getWidth() + "x" + getHeight());
		// super.paintComponent(g); not needed, doing everything myself
		final Graphics2D g2d = (Graphics2D)g;
		final int rowHeight = getHeight() / config.getNRows();

		configureRendered(g2d, true);

		drawProblems(g, getWidth(), getHeight(), rowHeight);
		System.out.println("+++ PAINT END +++");
	}

	public void guiLoop() throws Exception
	{
		final Graphics g = getGraphics();
		final Graphics2D g2d = (Graphics2D)g;
		long lastLeft = -1;
		int headerScrollerX = 0;
		double scrollTs = (double)System.currentTimeMillis() / 1000.0;

		for(;;)
		{
			long now = System.currentTimeMillis();
			long left = (long)config.getSleepTime() - ((now - lastRefresh) / 1000);
			int rowHeight = getHeight() / config.getNRows();
			int characterSize = rowHeight - 1;

			configureRendered(g2d, true);

			if (left <= 0)
			{
				lastRefresh = now;
				CoffeeSaint.log.add("*** Update PROBLEMS " + left);
				drawProblems(g, getWidth(), getHeight(), rowHeight);
				coffeeSaint.cleanUp();
			}

			if (currentHeader != null && config.getScrollingHeader())
			{
				int imgWidth = currentHeader.getWidth();
				int pixelsNeeded = getWidth() - 100;
				int pixelsAvail = imgWidth - headerScrollerX;
				int drawX = 0, sourceX = headerScrollerX;
				while(pixelsNeeded > 0)
				{
					int plotN = Math.min(pixelsNeeded, pixelsAvail);

					g2d.drawImage((Image)currentHeader, drawX, 0, drawX + pixelsAvail, rowHeight, sourceX, 0, sourceX + pixelsAvail, rowHeight, Color.GRAY, null);

					pixelsNeeded -= plotN;
					pixelsAvail -= plotN;
					drawX += plotN;
					sourceX += plotN;

					if (pixelsAvail <= 0)
					{
						pixelsAvail = imgWidth;
						sourceX = 0;
					}
				}

				double scrollTsNow = (double)System.currentTimeMillis() / 1000.0;
				double scrollMultiplier = (scrollTsNow - scrollTs) / (40.0 / 1000.0);

				headerScrollerX += (int)(((double)config.getScrollingHeaderPixelsPerSecond() / 25.0) * scrollMultiplier);
				while(headerScrollerX >= imgWidth)
					headerScrollerX -= imgWidth;

				scrollTs = scrollTsNow;
			}

			if (config.getCounter() && lastLeft != left && currentHeader == null)
			{
				drawCounter(g, getWidth(), getHeight(), rowHeight, characterSize, (int)left);
				lastLeft = left;
			}

			if (currentHeader != null && config.getScrollingHeader())
				Thread.sleep(40);
			else
				Thread.sleep(1000);
		}
	}
}
