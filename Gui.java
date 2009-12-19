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
	//
	Semaphore movingPartsSemaphore = new Semaphore(1);
	ScrollableContent currentHeader = new ScrollableContent(0, 0, 0);
	BordersParameters bordersParameters = null;
	java.util.List<ScrollableContent> windowMovingParts = new ArrayList<ScrollableContent>();

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

	void prepareRow(Graphics gTo, int windowWidth, int xStart, String msg, int row, String state, Color bgColor, float seeThrough, BufferedImage sparkLine, boolean addToScrollersIfNotFit)
	{
		final int rowHeight = getHeight() / config.getNRows();

		String font = config.getFontName();
		if (state.equals("1"))
			font = config.getWarningFontName();
		else if (state.equals("2"))
			font = config.getCriticalFontName();

		Integer reduceWidth = null;
		if (config.getReduceTextWidth())
			reduceWidth = Math.max(0, windowWidth - (sparkLine != null ? sparkLine.getWidth() : 0));
		RowParameters rowParameters = fitText(font, msg, rowHeight, reduceWidth);

		if (config.getReduceTextWidth() == false && rowParameters.getTextWidth() > windowWidth && addToScrollersIfNotFit == true)
		{
			windowMovingParts.add(new ScrollableContent(createRowImage(font, msg, state, bgColor, rowHeight, null), xStart, rowHeight * row, windowWidth));
		}
		else
		{
			drawRow(gTo, windowWidth, xStart, rowParameters, rowHeight, msg, row, state, bgColor, seeThrough, sparkLine);
		}
	}

	void drawRow(Graphics gTo, int windowWidth, int xStart, RowParameters rowParameters, int rowHeight, String msg, int row, String state, Color bgColor, float seeThrough, BufferedImage sparkLine)
	{
		System.out.println("DRAW ROW: " + msg + " " + row + " | " + rowParameters.getShrunkMore());

		BufferedImage output = new BufferedImage(windowWidth, rowHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = output.createGraphics();

		configureRendered(g, true);

		g.setColor(coffeeSaint.stateToColor(state));
		g.fillRect(0, 0, windowWidth, rowHeight);

		g.setColor(config.getTextColor());

		g.setFont(rowParameters.getAdjustedFont());

		if (rowParameters.getShrunkMore() == true)
		{
			double heightDiff = rowParameters.getHeightDiff();
			int newY = (int)(heightDiff / 2.0 + rowParameters.getAsc());

			g.drawString(msg, 1, newY);
		}
		else
			g.drawString(msg, 1, (int)rowParameters.getAsc());

		if (sparkLine != null)
			g.drawImage(sparkLine, windowWidth - sparkLine.getWidth(), 0, null);

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

	public BufferedImage createRowImage(String font, String header, String state, Color bgColor, int rowHeight, Integer fitWidth)
	{
		RowParameters rowParameters = fitText(font, header + " ", rowHeight, fitWidth);

		BufferedImage output = new BufferedImage(rowParameters.getTextWidth(), rowHeight, BufferedImage.TYPE_INT_RGB);

		drawRow(output.createGraphics(), rowParameters.getTextWidth(), 0, rowParameters, rowHeight, header, 0, state, bgColor, 1.0f, null);

		return output;
	}

	public static RowParameters fitText(String font, String text, int rowHeight, Integer fitWidth)
	{
		Graphics g2 = new BufferedImage(10, rowHeight, BufferedImage.TYPE_INT_RGB).createGraphics();
		Font f = new Font(font, Font.PLAIN, rowHeight);
		g2.setFont(f);
		FontMetrics fm = g2.getFontMetrics();
		double shrink = ((double)rowHeight / (double)fm.getHeight());
		boolean shrunkMore = false;
		if (fitWidth != null)
		{
			Rectangle2D boundingRectangle = f.getStringBounds(text, 0, text.length(), new FontRenderContext(null, false, false));

			double newShrink = (double)Math.max(0, fitWidth) / (double)boundingRectangle.getWidth();
			if (newShrink < shrink)
			{
				shrink = newShrink;
				shrunkMore = true;
			}
		}
		double newSize = (double)rowHeight * shrink;
		double newAsc  = (double)fm.getAscent() * shrink;
		double heightDiff = (double)fm.getAscent() - newAsc;
		f = f.deriveFont((float)newSize);
		g2.setFont(f);
		fm = g2.getFontMetrics();

		int textWidth = fm.stringWidth(text + " ");

		return new RowParameters(textWidth, shrunkMore, newAsc, f, heightDiff);
	}

	public void drawCounter(Graphics g, Position counterPosition, int windowWidth, int windowHeight, int rowHeight, int counter)
	{
		int xOffset = 0, yOffset = 0, yBase = 0;

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

		if (counterPosition == Position.UPPER_LEFT)
		{
			xOffset = 0;
			yBase = 0;
			yOffset = (int)newAsc;
		}
		else if (counterPosition == Position.UPPER_RIGHT)
		{
			xOffset = windowWidth - (int)boundingRectangle.getWidth();
			yBase = 0;
			yOffset = (int)newAsc;
		}
		else if (counterPosition == Position.LOWER_LEFT)
		{
			xOffset = 0;
			yBase = windowHeight - rowHeight;
			yOffset = windowHeight - rowHeight + (int)newAsc;
		}
		else if (counterPosition == Position.LOWER_RIGHT)
		{
			xOffset = windowWidth - (int)boundingRectangle.getWidth();
			yBase = windowHeight - rowHeight;
			yOffset = windowHeight - rowHeight + (int)newAsc;
		}
		else if (counterPosition == Position.CENTER)
		{
			xOffset = (windowWidth / 2) - (int)(boundingRectangle.getWidth() / 2.0);
			yBase = (windowHeight / 2) - (rowHeight / 2);
			yOffset = (windowHeight / 2) - (rowHeight  / 2) + (int)newAsc;
		}

		g.setColor(config.getBackgroundColor());
		g.fillRect(xOffset, yBase, (int)boundingRectangle.getWidth(), (int)boundingRectangle.getHeight());

		g.setColor(config.getTextColor());
		g.drawString("" + counter, xOffset, yOffset);
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

		prepareRow(g, windowWidth, 0, "Error: " + e, config.getNRows() - 1, "2", Color.GRAY, 1.0f, null, false);
	}

	public void drawBorders(Graphics2D g, BordersParameters bordersParameters)
	{
		configureRendered(g, false);

		int verticalNRows = Math.min(config.getNRows(), bordersParameters.getNProblems() + (config.getShowHeader() ? 1 : 0));

		g.setColor(config.getRowBorderColor());
		if (bordersParameters.getNColumns() > 1)
		{
			for(int rowColumns=0; rowColumns < bordersParameters.getNColumns(); rowColumns++)
			{
				int x = (bordersParameters.getWindowWidth() * rowColumns) / bordersParameters.getNColumns();
				int y =  config.getShowHeader() ? bordersParameters.getRowHeight() : 0;
				g.drawLine(x, y, x, bordersParameters.getRowHeight() * verticalNRows);
			}
		}

		if (bordersParameters.getNProblems() > 0)
		{
			for(int rowsRow=0; rowsRow < verticalNRows; rowsRow++)
			{
				int drawY = bordersParameters.getRowHeight() + rowsRow * bordersParameters.getRowHeight();
				g.drawLine(0, drawY, bordersParameters.getWindowWidth(), drawY);
			}

		}

		configureRendered((Graphics2D)g, true);
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

			movingPartsSemaphore.acquireUninterruptibly();
			windowMovingParts = new ArrayList<ScrollableContent>();
			bordersParameters = null;
			movingPartsSemaphore.release();

			/* block in upper right to inform about load */
			g.setColor(Color.BLUE);
			g.fillRect(windowWidth - rowHeight, 0, rowHeight, rowHeight);

			if (config.getVerbose())
				prepareRow(g, windowWidth, 0, "Loading image(s)", 0, "0", bgColor, 1.0f, null, false);
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
				prepareRow(g, windowWidth, 0, "Loading Nagios data", 0, "0", bgColor, 1.0f, null, false);
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
				{
					movingPartsSemaphore.acquireUninterruptibly();
					currentHeader.setImage(createRowImage(fontName, header, problems.size() == 0 ? "0" : "255", bgColor, rowHeight, null));
					movingPartsSemaphore.release();
				}
				else
					prepareRow(g, windowWidth, 0, header, curNRows, problems.size() == 0 ? "0" : "255", bgColor, 1.0f, null, false);
				curNRows++;
			}

			int colNr = 0;
			int dummyNRows = config.getNRows() - (config.getShowHeader() ? 1 : 0);
			int curNColumns;
			if (config.getFlexibleNColumns())
				curNColumns = Math.min(config.getNProblemCols(), (problems.size() + dummyNRows - 1) / dummyNRows);
			else
				curNColumns = config.getNProblemCols();
			curNColumns = Math.max(curNColumns, 1);
			final int rowColWidth = windowWidth / curNColumns;
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
				{
					Service currentService = currentProblem.getService();
					sparkLine = coffeeSaint.getSparkLine(currentProblem.getHost().getHostName(), currentService != null ? currentService.getServiceName() : null, sparkLineWidth, rowHeight - (config.getRowBorder()?1:0), false);
				}

				int xStart = rowColWidth * colNr;

				movingPartsSemaphore.acquireUninterruptibly();
				prepareRow(g, rowColWidth, xStart, output, curNRows, currentProblem.getCurrent_state(), bgColor, config.getTransparency(), sparkLine, config.getScrollIfNotFit());
				movingPartsSemaphore.release();

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
				bordersParameters = new BordersParameters(problems.size(), curNColumns, windowWidth, rowHeight);
				movingPartsSemaphore.acquireUninterruptibly();
				if (windowMovingParts.size() == 0)
					drawBorders((Graphics2D)g, bordersParameters);
				movingPartsSemaphore.release();
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

		currentHeader = new ScrollableContent(0, 0, getWidth());

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

			movingPartsSemaphore.acquireUninterruptibly();
			if (currentHeader.hasImage() && config.getScrollingHeader() && config.getShowHeader())
				currentHeader.scrollView(g2d, config.getScrollingPixelsPerSecond());
			for(ScrollableContent currentMovingPart : windowMovingParts)
			{
				currentMovingPart.scrollView(g2d, config.getScrollingPixelsPerSecond());
			}
			if (bordersParameters != null)
				drawBorders((Graphics2D)g, bordersParameters);
			movingPartsSemaphore.release();

			Position counterPosition = config.getCounterPosition();
			if (config.getCounter() && lastLeft != left && counterPosition != null)
			{
				drawCounter(g, counterPosition, getWidth(), getHeight(), rowHeight, (int)left);
				lastLeft = left;
			}

			if ((currentHeader.hasImage() && config.getScrollingHeader()) || windowMovingParts.size() != 0)
				Thread.sleep(5);
			else
				Thread.sleep(1000);
		}
	}
}
