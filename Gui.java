/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import com.vanheusden.nagios.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;

public class Gui extends JPanel implements ImageObserver, MouseListener {
	final Config config;
	final CoffeeSaint coffeeSaint;
	final Statistics statistics;
	//
	Semaphore movingPartsSemaphore = new Semaphore(1);
	BordersParameters bordersParameters = null;
	java.util.List<ScrollableContent> windowMovingParts = new ArrayList<ScrollableContent>();
	//
	Image logo = null;
	Image flappingIcon = null;

	enum DataType { HOST, SERVICE, TEXT };

	class RowData {
		Object data1, data2;
		DataType type;

		RowData(Host host, Service service) {
			data1 = (Object)host;
			data2 = (Object)service;
			if (service != null)
				type = DataType.SERVICE;
			else
			type = DataType.HOST;
		}

		RowData(String text) {
			data1 = (Object)text;
			type = DataType.TEXT;
		}

		DataType getType() {
			return type;
		}

		Object getObject1() {
			return data1;
		}

		Object getObject2() {
			return data2;
		}
	}
	RowData [][] rowData = null;
	RowData currentRow = null;

	boolean lastState = false;	// false: no problems
	// because making a frame visible already causes
	// a call to paint() so no need to do that (again)
	// at start
	long lastRefresh = System.currentTimeMillis();
	boolean firstDraw = true;

	public void mouseEntered (MouseEvent me) {} 
	public void mousePressed (MouseEvent me) {} 
	public void mouseReleased (MouseEvent me) {}  
	public void mouseExited (MouseEvent me) {}

	public void mouseClicked(MouseEvent me) {
		System.out.println("Mouse clicked: " + me);
		if (me.getButton() == MouseEvent.BUTTON1) {
			if (currentRow != null) {
				currentRow = null;
				repaint();
			}
			else if (rowData != null) {
				try {
					int rowHeight = getHeight() / config.getNRows();
					int colWidth = getWidth() / rowData[0].length;
					int row = (int)((double)me.getY() / (double)rowHeight);
					int col = (int)((double)me.getX() / (double)colWidth);
					System.out.println("ROW: " + row + " rowheight: " + rowHeight + " y: " + me.getY());
					System.out.println("COL: " + col + " colwidth: " + colWidth + " x: " + me.getX());

					currentRow = rowData[row][col];

					if (currentRow != null)
						repaint();
				}
				catch(IndexOutOfBoundsException ioobe) {
					// clicked where no row was drawn, ignore
					System.out.println("no row known");
				}
			}
		}
	}

	public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height) {
		String add = ", img: " + img + ", flags: " + flags + ", x/y: " + x + "," + y + ", width: " + width + ", height: " + height;
		if ((flags & ABORT) != 0)
			CoffeeSaint.log.add("Image aborted" + add);
		if ((flags & ALLBITS) != 0)
			CoffeeSaint.log.add("Image complete" + add);
		if ((flags & ERROR) != 0)
			CoffeeSaint.log.add("Image error" + add);
		if ((flags & FRAMEBITS) != 0)
			CoffeeSaint.log.add("Image framebits " + add);
		if ((flags & HEIGHT) != 0)
			CoffeeSaint.log.add("Image framebits " + add);
		if ((flags & PROPERTIES) != 0)
			CoffeeSaint.log.add("Image framebits " + add);
		if ((flags & SOMEBITS) != 0)
			CoffeeSaint.log.add("Image framebits " + add);
		if ((flags & WIDTH) != 0)
			CoffeeSaint.log.add("Image framebits " + add);

		// If status is not COMPLETE then we need more updates.
		return (flags & (ALLBITS|ABORT)) == 0;
	}

	public Gui(Config config, CoffeeSaint coffeeSaint, Statistics statistics) throws Exception
	{
		this.config = config;
		this.coffeeSaint = coffeeSaint;
		this.statistics = statistics;

		if (config.getLogo() != null)
		{
			String loadImage = config.getLogo();
			if (loadImage.length() >= 8 && (loadImage.substring(0, 7).equalsIgnoreCase("http://") || loadImage.substring(0, 8).equalsIgnoreCase("https://")))
				logo = Toolkit.getDefaultToolkit().createImage(new URL(loadImage));
			else
				logo = Toolkit.getDefaultToolkit().createImage(loadImage);
			new ImageIcon(logo); //loads the image
		}

		InputStream is = getClass().getClassLoader().getResourceAsStream("com/vanheusden/CoffeeSaint/flapping.png");
		flappingIcon = ImageIO.read(is);
		new ImageIcon(flappingIcon); //loads the image

		Toolkit.getDefaultToolkit().sync();
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

	int prepareRow(Graphics gTo, int windowWidth, int xStart, String msg, int row, String state, boolean hard, Color bgColor, float seeThrough, BufferedImage sparkLine, boolean addToScrollersIfNotFit, boolean isFlapping)
	{
		final int rowHeight = (getHeight() - (config.getShowHeader() ? config.getUpperRowBorderHeight() : 0))/ config.getNRows();

		String font = config.getFontName();
		if (state.equals("1"))
			font = config.getWarningFontName();
		else if (state.equals("2"))
			font = config.getCriticalFontName();

		Integer reduceWidth = null;
		if (config.getReduceTextWidth())
			reduceWidth = Math.max(0, windowWidth - (sparkLine != null ? sparkLine.getWidth() - (isFlapping && config.getShowFlappingIcon() ? rowHeight : 0): 0));
		RowParameters rowParameters = fitText(font, msg, rowHeight, reduceWidth);

		if (config.getReduceTextWidth() == false && rowParameters.getTextWidth() > windowWidth && addToScrollersIfNotFit == true)
		{
			int yPos = 0;
			if (row > 0)
				yPos = rowHeight * row + (config.getShowHeader() ? config.getUpperRowBorderHeight() : 0);
			windowMovingParts.add(new ScrollableContent(createRowImage(font, msg + " ", state, hard, bgColor, rowHeight, null, isFlapping), xStart, yPos, windowWidth));
		}
		else
		{
			drawRow(gTo, windowWidth, xStart, rowParameters, rowHeight, msg, row, state, hard, bgColor, seeThrough, sparkLine, isFlapping);
		}

		return rowParameters.getTextWidth();
	}

	void drawRow(Graphics gTo, int windowWidth, int xStart, RowParameters rowParameters, int rowHeight, String msg, int row, String state, boolean hard, Color bgColor, float seeThrough, BufferedImage sparkLine, boolean isFlapping)
	{
		if (windowWidth == 0)
			return;

		BufferedImage output = new BufferedImage(windowWidth, rowHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = output.createGraphics();

		configureRendered(g, true);

		Color stateColor = coffeeSaint.stateToColor(state, hard);
		Color gradientColor = config.getProblemRowGradient();
		if (gradientColor != null && stateColor != null) {
			configureRendered((Graphics2D)gTo, false);
			int scR = stateColor.getRed(), scG = stateColor.getGreen(), scB = stateColor.getBlue();
			int ftcR = gradientColor.getRed(), ftcG = gradientColor.getGreen(), ftcB = gradientColor.getBlue();
			double stepR = (double)(ftcR - scR) / (double)rowHeight;
			double stepG = (double)(ftcG - scG) / (double)rowHeight;
			double stepB = (double)(ftcB - scB) / (double)rowHeight;

			// double piStep = Math.PI / (double)rowHeight;
			for(int rowY = 0; rowY<rowHeight; rowY++)
			{
				//double pos = rowY * (1.0 - Math.sin(piStep * (double)rowY));
				double pos = rowY;
				int curR = Math.min(Math.max(0, scR + (int)((double)pos * stepR)), 255);
				int curG = Math.min(Math.max(0, scG + (int)((double)pos * stepG)), 255);
				int curB = Math.min(Math.max(0, scB + (int)((double)pos * stepB)), 255);

				g.setColor(new Color(curR, curG, curB));
				g.drawLine(0, rowY, windowWidth - 1, rowY);
			}
			configureRendered((Graphics2D)gTo, true);
		}
		else if (stateColor != null)
		{
			g.setColor(stateColor);
			g.fillRect(0, 0, windowWidth, rowHeight);
		}

		int flappingStartX = 0;
		if (isFlapping && config.getShowFlappingIcon()) {
			g.drawImage(flappingIcon, 0, 0, rowHeight, rowHeight, this);
			flappingStartX = rowHeight;
		}

		g.setColor(config.getTextColor());

		g.setFont(rowParameters.getAdjustedFont());

		if (rowParameters.getShrunkMore() == true)
		{
			double heightDiff = rowParameters.getHeightDiff();
			int newY = (int)(heightDiff / 2.0 + rowParameters.getAsc());

			g.drawString(msg, flappingStartX + 1, newY);
		}
		else
			g.drawString(msg, flappingStartX + 1, (int)rowParameters.getAsc());

		if (sparkLine != null)
			g.drawImage(sparkLine, windowWidth - sparkLine.getWidth(), 0, null);

		Graphics2D gTo2D = (Graphics2D)gTo;

		int x = xStart;
		int y = 0;
		if (row > 0)
			y = rowHeight * row + (config.getShowHeader() ? config.getUpperRowBorderHeight() : 0);
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

	public BufferedImage createRowImage(String font, String text, String state, boolean hard, Color bgColor, int rowHeight, Integer fitWidth, boolean isFlapping)
	{
		RowParameters rowParameters = fitText(font, text, rowHeight, fitWidth);

		BufferedImage output = new BufferedImage(rowParameters.getTextWidth(), rowHeight, BufferedImage.TYPE_INT_RGB);

		drawRow(output.createGraphics(), rowParameters.getTextWidth(), 0, rowParameters, rowHeight, text, 0, state, hard, bgColor, 1.0f, null, isFlapping);

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
		int xOffset = 0, yOffset = 0;

		BufferedImage img = createRowImage(config.getFontName(), "" + counter, "255", true, config.getBackgroundColor(), rowHeight, null, false);

		if (counterPosition == Position.UPPER_LEFT)
		{
			xOffset = 0;
			yOffset = 0;
		}
		else if (counterPosition == Position.UPPER_RIGHT)
		{
			xOffset = windowWidth - img.getWidth();
			yOffset = 0;
		}
		else if (counterPosition == Position.LOWER_LEFT)
		{
			xOffset = 0;
			yOffset = rowHeight * (config.getNRows() - 1) + (config.getShowHeader() ? config.getUpperRowBorderHeight(): 0);
		}
		else if (counterPosition == Position.LOWER_RIGHT)
		{
			xOffset = windowWidth - img.getWidth();
			yOffset = rowHeight * (config.getNRows() - 1) + (config.getShowHeader() ? config.getUpperRowBorderHeight(): 0);
		}
		else if (counterPosition == Position.CENTER)
		{
			xOffset = (windowWidth / 2) - (img.getWidth() / 2);
			yOffset = (windowHeight / 2) - (img.getHeight() / 2);
		}

		g.drawImage(img, xOffset, yOffset, img.getWidth(), img.getHeight(), this);
	}

	public void displayImage(ImageParameters [] imageParameters, int nProblems, Graphics g, int rowHeight, boolean adaptImgSize, int windowWidth, int windowHeight)
	{
		int headerOffset = config.getShowHeader() ? 1 : 0;
		int footerOffset = config.getFooter() != null ? 1 : 0;
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
			curWindowHeight = rowHeight * (config.getNRows() - (headerOffset + footerOffset + nProblems));
			offsetY = (headerOffset + footerOffset + nProblems) * rowHeight;
		}
		else if (config.getHeaderTransparency() != 1.0f)
		{
			curWindowHeight = rowHeight * config.getNRows();
			offsetY = 0;
		}
		else
		{
			curWindowHeight = rowHeight * (config.getNRows() - (headerOffset + footerOffset));
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

						Image img = imageParameters[nr].getImage();

						if (g.drawImage(img, plotX, plotY, newWidth, newHeight, this) == false)
							CoffeeSaint.log.add("drawImage " + imageParameters[nr].getImage() + " returns false");
					}
					else
					{
						// g.drawString("n/a", plotX, plotY);
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
		String error = e.toString().replaceAll("java.[a-z]*.", "");
		prepareRow(g, windowWidth, 0, "Error: " + error, config.getNRows() - 1, "2", true, Color.GRAY, 1.0f, null, false, false);
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
				int y =  config.getShowHeader() ? (bordersParameters.getRowHeight() + config.getUpperRowBorderHeight()): 0;
				g.drawLine(x, y, x, bordersParameters.getRowHeight() * verticalNRows);
			}
		}

		if (bordersParameters.getNProblems() > 0)
		{
			for(int rowsRow=0; rowsRow < verticalNRows; rowsRow++)
			{
				int drawY = bordersParameters.getRowHeight() + rowsRow * bordersParameters.getRowHeight();
				if (rowsRow > 0 && config.getShowHeader() == true)
					drawY += config.getUpperRowBorderHeight();
				g.drawLine(0, drawY, bordersParameters.getWindowWidth(), drawY);
			}

		}

		if (config.getFooter() != null)
		{
			int y =  config.getShowHeader() ? config.getUpperRowBorderHeight(): 0;
			y += (config.getNRows() - 1) * bordersParameters.getRowHeight();
			g.drawLine(0, y, bordersParameters.getWindowWidth(), y);
		}

		if (config.getShowHeader())
		{
			int drawY = bordersParameters.getRowHeight();
			g.fillRect(0, drawY, bordersParameters.getWindowWidth(), config.getUpperRowBorderHeight());
		}

		configureRendered((Graphics2D)g, true);
	}

	public void drawProblemServiceSplitLine(Graphics g, int rowHeight, int windowHeight, int nProblems)
	{
		if (config.getPutSplitAtOffset() != null)
		{
			int x = config.getPutSplitAtOffset();
			int maxY = nProblems * bordersParameters.getRowHeight(), minY = 0;
			if (config.getShowHeader() == true)
			{
				minY = config.getUpperRowBorderHeight() + rowHeight;
				maxY += minY;
			}
			maxY = Math.min(maxY, windowHeight - (config.getFooter() != null ? rowHeight : 0));

			g.setColor(config.getRowBorderColor());
			g.drawLine(x, minY, x, maxY);
		}
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
			int newLogoHeight = -1, newLogoWidth = -1;

			if (firstDraw) {
				// Let user know what app xe is using
				// load logo
				InputStream is = getClass().getClassLoader().getResourceAsStream("com/vanheusden/CoffeeSaint/footer01.png");
				Image logo = ImageIO.read(is);
				// draw
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(Color.BLACK);
				Font f = new Font("Arial", Font.PLAIN, 20);
				g.setFont(f);
				int halfX = getWidth() / 2;
				int halfY = getHeight() / 2;
				String msg1 = "CoffeeSaint " + CoffeeSaint.getVersionNr();
				int w1 = getTextWidth(f, msg1);
				g.drawString(msg1, Math.max(0, halfX - w1 / 2), halfY - 25);
				String msg2 = "(C) 2009-2010 by folkert@vanheusden.com";
				int w2 = getTextWidth(f, msg2);
				g.drawString(msg2, Math.max(0, halfX - w2 / 2), halfY + 25);
				f = new Font("Arial", Font.PLAIN, 10);
				g.setFont(f);
				String msg3 = "Please wait...";
				int w3 = getTextWidth(f, msg3);
				g.drawString(msg3, Math.max(0, halfX - w3 / 2), halfY + 50);
				//
				new ImageIcon(logo); //loads the image
                                Toolkit.getDefaultToolkit().sync();
				System.out.println("logo: " + logo.getHeight(null) + ", res: " + getHeight());
				g.drawImage(logo, halfX - logo.getWidth(null) / 2, halfY + 75, this);
				// annoy user :-D
				Thread.sleep(1000);

				firstDraw = false;
			}

			/* determine new size of logo; keep correct aspect ratio */
			if (logo != null)
			{
				int imgLogoWidth  = logo.getWidth(null);
				int imgLogoHeight = logo.getHeight(null);

				newLogoHeight = rowHeight;
				newLogoWidth  = (int)(((double)newLogoHeight / (double)imgLogoHeight) * imgLogoWidth);
			}

			/* block in upper right to inform about load */
			g.setColor(Color.BLUE);
			g.fillRect(windowWidth - rowHeight, 0, rowHeight, rowHeight);

			/* font for all texts */
			String fontName = config.getFontName();
			final Font f = new Font(fontName, Font.PLAIN, rowHeight);
			g.setFont(f);

			/* get webcam images */
			if (config.getVerbose())
				prepareRow(g, windowWidth, 0, "Loading image(s)", 0, "0", true, bgColor, 1.0f, null, false, false);
			startLoadTs = System.currentTimeMillis();
			ImageParameters [] imageParameters = coffeeSaint.loadImage(this, windowWidth, g);
			endLoadTs = System.currentTimeMillis();

			took = (double)(endLoadTs - startLoadTs) / 1000.0;

			statistics.addToTotalImageLoadTime(took);

			/* load & process nagios data */
			if (config.getVerbose())
				prepareRow(g, windowWidth, 0, "Loading Nagios data", 0, "0", true, bgColor, 1.0f, null, false, false);
			JavNag javNag = CoffeeSaint.loadNagiosData(this, windowWidth, g);
			coffeeSaint.collectPerformanceData(javNag);
			coffeeSaint.collectLatencyData(javNag);
			java.util.List<Problem> problems = CoffeeSaint.findProblems(javNag);
			coffeeSaint.learnProblemCount(problems.size());

			movingPartsSemaphore.acquireUninterruptibly();
			windowMovingParts = new ArrayList<ScrollableContent>();
			bordersParameters = null;
			movingPartsSemaphore.release();

			Calendar rightNow = Calendar.getInstance();

			/* determine background color */
			if (problems.size() == 0 && config.getBrainFileName() != null)
				bgColor = coffeeSaint.predictWithColor(rightNow);
			else if (config.getSetBgColorToState())
			{
				int state = 0;
				for(Problem currentProblem : problems)
					state = Math.max(state, Integer.valueOf(currentProblem.getCurrent_state()));

				if (state == 0)
					bgColor = config.getBackgroundColorOkStatus();
				else if (state == 1)
					bgColor = config.getWarningBgColor();
				else if (state == 2)
					bgColor = config.getCriticalBgColor();
				else
					bgColor = config.getNagiosUnknownBgColor();
			}

			/* clear frame & draw background gradient, if selected */
			g.setColor(bgColor);
			g.fillRect(0, 0, windowWidth, windowHeight);
			Color fadeToBgColor = config.getBackgroundColorFadeTo();
			if (fadeToBgColor != null)
			{
				configureRendered((Graphics2D)g, false);
				int scR = bgColor.getRed(), scG = bgColor.getGreen(), scB = bgColor.getBlue();
				int ftcR = fadeToBgColor.getRed(), ftcG = fadeToBgColor.getGreen(), ftcB = fadeToBgColor.getBlue();
				double stepR = (double)(ftcR - scR) / (double)windowHeight;
				double stepG = (double)(ftcG - scG) / (double)windowHeight;
				double stepB = (double)(ftcB - scB) / (double)windowHeight;

				for(int y = 0; y<windowHeight; y++)
				{
					double pos = y;
					int curR = Math.min(Math.max(0, scR + (int)(pos * stepR)), 255);
					int curG = Math.min(Math.max(0, scG + (int)(pos * stepG)), 255);
					int curB = Math.min(Math.max(0, scB + (int)(pos * stepB)), 255);

					g.setColor(new Color(curR, curG, curB));
					g.drawLine(0, y, windowWidth - 1, y);
				}
				configureRendered((Graphics2D)g, true);
			}

			/* webcam */
			if (imageParameters != null)
				displayImage(imageParameters, problems.size(), g, rowHeight, config.getAdaptImageSize(), windowWidth, windowHeight);

			/**** start of headers/rows/footers displaying ****/
			int curNRows = 0;
			int dummyNRows = config.getNRows() - ((config.getShowHeader() ? 1 : 0) + (config.getFooter() != null ? 1 : 0));
			int curNColumns = -1;
			if (config.getFlexibleNColumns())
				curNColumns = Math.min(config.getNProblemCols(), (problems.size() + dummyNRows - 1) / dummyNRows);
			else
				curNColumns = config.getNProblemCols();
			curNColumns = Math.max(curNColumns, 1);

			rowData = new RowData[config.getNRows()][curNColumns];

			/* header */
			if (config.getShowHeader())
			{
				String header = coffeeSaint.getScreenHeader(javNag, rightNow, problems.size() > 0);
				String stateForColor = problems.size() == 0 ? "0" : "255";
				if (config.getHeaderAlwaysBGColor())
					stateForColor = "255";
				// if (config.getHeaderTransparency() != 1.0f)
				//	stateForColor = "254";
				int xStart = 0, ww = windowWidth;;
				if (logo != null && config.getLogoPosition() == Position.UPPER_LEFT || config.getLogoPosition() == Position.UPPER_RIGHT)
				{
					if (config.getLogoPosition() == Position.UPPER_LEFT)
						xStart = newLogoWidth;

					ww -= newLogoWidth;
				}

				if (config.getScrollingHeader())
					windowMovingParts.add(new ScrollableContent(createRowImage(fontName, header + " ", stateForColor, true, bgColor, rowHeight, null, false), xStart, 0, ww));
				else
					prepareRow(g, ww, xStart, header, curNRows, stateForColor, true, bgColor, config.getHeaderTransparency(), null, false, false);

				curNRows++;

				RowData headerRow = new RowData(header);
				for(int col=0; col<curNColumns; col++)
					rowData[0][col] = headerRow;
			}

			/* problems */
			int colNr = 0;
			final int rowColWidth = windowWidth / curNColumns;
			for(Problem currentProblem : problems)
			{
				boolean isFlapping = false;
				String escapeString;
				if (currentProblem.getService() == null) {
					escapeString = config.getHostIssue();
					isFlapping = currentProblem.getHost().getParameter("is_flapping").equals("0") == false;
				}
				else {
					escapeString = config.getServiceIssue();
					isFlapping = currentProblem.getService().getParameter("is_flapping").equals("0") == false;
				}
				String output = coffeeSaint.processStringWithEscapes(escapeString, javNag, rightNow, currentProblem, problems.size() > 0, true);

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
				Character splitChar = config.getLineScrollSplitter();
				if (splitChar != null)
				{
					int splitIndex = output.indexOf(splitChar);
					if (splitIndex == -1)
					{
						prepareRow(g, rowColWidth, xStart, output, curNRows, currentProblem.getCurrent_state(), currentProblem.getHard(), bgColor, config.getTransparency(), sparkLine, config.getScrollIfNotFit(), isFlapping);
					}
					else
					{
						String before = output.substring(0, splitIndex);
						String after = output.substring(splitIndex + 1);

						int beforeWidth = prepareRow(g, rowColWidth, xStart, before, curNRows, currentProblem.getCurrent_state(), currentProblem.getHard(), bgColor, config.getTransparency(), sparkLine, config.getScrollIfNotFit(), isFlapping);

						int newX = xStart + beforeWidth;
						if (config.getPutSplitAtOffset() != null)
							newX = xStart + config.getPutSplitAtOffset();
						prepareRow(g, Math.max(0, rowColWidth - newX), newX, after, curNRows, currentProblem.getCurrent_state(), currentProblem.getHard(), bgColor, config.getTransparency(), sparkLine, config.getScrollIfNotFit(), isFlapping);
					}
				}
				else
				{
					prepareRow(g, rowColWidth, xStart, output, curNRows, currentProblem.getCurrent_state(), currentProblem.getHard(), bgColor, config.getTransparency(), sparkLine, config.getScrollIfNotFit(), isFlapping);
				}
				movingPartsSemaphore.release();

				rowData[curNRows][colNr] = new RowData(currentProblem.getHost(), currentProblem.getService());

				curNRows++;

				if (curNRows == (config.getNRows() - (config.getFooter() != null ? 1 : 0)))
				{
					curNRows = config.getShowHeader() ? 1 : 0;
					colNr++;
					if (colNr == curNColumns)
						break;
				}
			}

			/* footer */
			if (config.getFooter() != null)
			{
				String footer = coffeeSaint.processStringWithEscapes(config.getFooter(), javNag, rightNow, null, problems.size() > 0, true);
				String stateForColor = problems.size() == 0 ? "0" : "255";
				int row = config.getNRows() - 1;
				if (config.getHeaderAlwaysBGColor())
					stateForColor = "255";

				int xStart = 0, ww = windowWidth;;
				if (logo != null && config.getLogoPosition() == Position.LOWER_LEFT || config.getLogoPosition() == Position.LOWER_RIGHT)
				{
					if (config.getLogoPosition() == Position.LOWER_LEFT)
						xStart = newLogoWidth;

					ww -= newLogoWidth;
				}

				if (config.getScrollingFooter())
				{
					int y = config.getShowHeader() ? config.getUpperRowBorderHeight(): 0;
					y += row * rowHeight;
					windowMovingParts.add(new ScrollableContent(createRowImage(fontName, footer + " ", stateForColor, true, bgColor, rowHeight, null, false), xStart, y, ww));
				}
				else
					prepareRow(g, ww, xStart, footer, row, stateForColor, true, bgColor, config.getHeaderTransparency(), null, false, false);
			}

			/* no problems message */
			if (problems.size() == 0)
			{
				String okMsg = config.getNoProblemsText();
				if (okMsg != null && config.getNoProblemsTextPosition() != Position.NONE)
				{
					int x = 0;
					int y = 0;
					if (config.getShowHeader())
						y = 1;

					okMsg = coffeeSaint.processStringWithEscapes(okMsg, javNag, rightNow, null, false, true);

					BufferedImage allFineMsg = createRowImage(config.getFontName(), okMsg + " ", "0", true, config.getBackgroundColor(), rowHeight, null, false);
					int width = allFineMsg.getWidth();

					switch(config.getNoProblemsTextPosition())
					{
						case UPPER_LEFT:
							x = 0;
							break;

						case UPPER_RIGHT:
							x = Math.max(0, windowWidth - width);
							break;

						case LOWER_LEFT:
							x = 0;
							y = (windowHeight / rowHeight) - 1;
							break;

						case LOWER_RIGHT:
							y = (windowHeight / rowHeight) - 1;
							x = Math.max(0, windowWidth - width);
							break;

						case CENTER:
							y = (windowHeight / rowHeight) / 2;
							x = Math.max(0, (windowWidth / 2) - (width / 2));
							break;
					}

					if (config.getNoProblemsTextBg())
						prepareRow(g, width, x, okMsg, y, "0", true, bgColor, 1.0f, null, false, false);
					else
						prepareRow(g, width, x, okMsg, y, "254", true, bgColor, 1.0f, null, false, false);
				}
			}

			/* draw logo */
			if (logo != null)
			{
				int plotX = -1, plotY = 0;
				Position logoPosition = config.getLogoPosition();

				if (logoPosition == Position.UPPER_LEFT)
					plotX = 0;
				else if (logoPosition == Position.UPPER_RIGHT)
					plotX = Math.max(0, windowWidth - newLogoWidth);
				else if (logoPosition == Position.LOWER_LEFT)
				{
					plotX = 0;
					plotY = (config.getNRows() - 1) * rowHeight;
				}
				else if (logoPosition == Position.LOWER_RIGHT)
				{
					plotX = Math.max(0, windowWidth - newLogoWidth);
					plotY = (config.getNRows() - 1) * rowHeight;
				}
				else
					throw new Exception("Unknown logo position: " + logoPosition);

				g.drawImage(logo, plotX, plotY, newLogoWidth, newLogoHeight, this);
			}

			/* draw borders */
			if (config.getRowBorder())
			{
				bordersParameters = new BordersParameters(problems.size(), curNColumns, windowWidth, rowHeight);
				movingPartsSemaphore.acquireUninterruptibly();
				if (windowMovingParts.size() == 0)
					drawBorders((Graphics2D)g, bordersParameters);
				movingPartsSemaphore.release();
			}

			if (config.getDrawProblemServiceSplitLine())
				drawProblemServiceSplitLine(g, rowHeight, windowHeight, problems.size());

			boolean toOld = false;
			long maxAge = config.getMaxCheckAge();
			if (maxAge != -1) {
				long currentAge = javNag.findMostRecentCheckAge();
				if (currentAge > maxAge) {
					toOld = true;
					prepareRow(g, windowWidth, 0, "NAGIOS STOPPED RUNNING!", config.getNRows() / 2, "2", true, Color.RED, 1.0f, null, false, false);
				}
			}

			/* play sound */
			if (problems.size() > 0 || toOld)
			{
				if (lastState == false || toOld)
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
		catch(UnsupportedAudioFileException uafe)
		{
			g.setColor(Color.RED);
			g.fillRect(windowWidth - rowHeight, 0, rowHeight, rowHeight);
			prepareRow(g, windowWidth, 0, "Audio sample unsupported format (" + uafe + ")", config.getNRows() - 1, "2", true, Color.GRAY, 1.0f, null, false, false);
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

	int getTextWidth(final Font f, final String text) {
		Rectangle2D boundingRectangle = f.getStringBounds(text, 0, text.length(), new FontRenderContext(null, false, false));
		return (int)Math.ceil(boundingRectangle.getWidth());
	}

	public String shrinkStringToFit(String input, int width, Font f) {
		while(input.length() > 1 && getTextWidth(f, input) > width) {
			input = input.substring(0, input.length() - 1);
		}

		return input;
	}

	public void putLine(Graphics g, String font, int x, int yBase, int width, int rowHeight, String str) {
		RowParameters rowParameters = fitText(font, str, rowHeight, Integer.valueOf(width));
		g.setFont(rowParameters.getAdjustedFont());
		g.drawString(shrinkStringToFit(str, width, rowParameters.getAdjustedFont()), x, yBase + (int)rowParameters.getAsc());
	}

	String getEitherParameter(RowData r, String name) {
		String par = null;

		if (r.getType() == DataType.SERVICE)
			par = ((Service)r.getObject2()).getParameter(name);

		if (r.getType() == DataType.HOST)
			par = ((Host)r.getObject1()).getParameter(name);

		if (par == null)
			par = "?";

		return par;
	}

	public String cnvSec(String sse) {
		Long value = Long.valueOf(sse);

		java.util.Date when = new java.util.Date(value * 1000);
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		return dateFormatter.format(when.getTime());
	}

	public void initPopup(Graphics g, int useWidth, int useHeight) {
		g.setColor(Color.WHITE);
		g.fillRect(51, 51, useWidth - 2, useHeight - 2);

		g.setColor(Color.BLACK);
		g.drawRect(50, 50, useWidth, useHeight);

		g.setFont(new Font("Arial", Font.PLAIN, 10));
		g.drawString(CoffeeSaint.getVersion(), 51, useHeight - 11 + 51);
	}

	public void drawPopup(RowData r, Graphics g, int width, int height) {
		int useWidth = width - 100;
		int halfWidth = useWidth / 2;
		int useHeight = height - 100;
		int useNRows = (config.getNRows() * 3) / 2;
		int curRowHeight = useHeight / useNRows;
		String font = config.getFontName();

		try {
			int rowOffset = 0;

			initPopup(g, useWidth, useHeight);

			if (r.getType() == DataType.TEXT) {

				putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, (String)r.getObject1());

				JavNag javNag = new JavNag();
				int prevNHosts = 0, prevNServices = 0;;
				for(NagiosDataSource dataSource : config.getNagiosDataSources())
				{
					String source = "???", stats;

					long startLoadTs = System.currentTimeMillis();

					if (dataSource.getType() == NagiosDataSourceType.TCP) {
						source = dataSource.getHost() + " " + dataSource.getPort();
						javNag.loadNagiosData(dataSource.getHost(), dataSource.getPort(), dataSource.getVersion(), false);
					}
					else if (dataSource.getType() == NagiosDataSourceType.ZTCP) {
						source = dataSource.getHost() + " " + dataSource.getPort();
						javNag.loadNagiosData(dataSource.getHost(), dataSource.getPort(), dataSource.getVersion(), true);
					}
					else if (dataSource.getType() == NagiosDataSourceType.HTTP) {
						source = "" + dataSource.getURL();
						javNag.loadNagiosData(dataSource.getURL(), dataSource.getVersion(), dataSource.getUsername(), dataSource.getPassword(), config.getAllowHTTPCompression());
					}
					else if (dataSource.getType() == NagiosDataSourceType.FILE) {
						source = dataSource.getFile();
						javNag.loadNagiosData(dataSource.getFile(), dataSource.getVersion());
					}
					else if (dataSource.getType() == NagiosDataSourceType.LS) {
						source = dataSource.getHost() + " " + dataSource.getPort();
						javNag.loadNagiosDataLiveStatus(dataSource.getHost(), dataSource.getPort());
					}

					Totals totals = javNag.calculateStatistics();
					stats = "Load time: " + (System.currentTimeMillis() - startLoadTs) / 1000.0 + "s, hosts: " + (totals.getNHosts() - prevNHosts) + ", services: " + (totals.getNServices() - prevNServices);
					prevNHosts = totals.getNHosts();
					prevNServices = totals.getNServices();

					putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "Nagios source: " + source);
					putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "  " + stats);
				}
				Totals totals = javNag.calculateStatistics();

				putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "Total hosts: " + totals.getNHosts() + ", services: " + totals.getNServices());
				putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "OK: " + totals.getNOk() + ", warning: " + totals.getNWarning() + ", critical: " + totals.getNCritical());
				putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "UP: " + totals.getNUp() + ", down: " + totals.getNDown() + ", unr.: " + totals.getNUnreachable() + ", pending: " + totals.getNPending());
				String latency = "" + javNag.getAvgCheckLatency();
				if (latency.length() > 5)
					latency = latency.substring(0, 5);
				putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "Average check latency: " + latency + ", last check age: " + javNag.findMostRecentCheckAge() + "s");
				putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "Problems acked: " + totals.getNAcked() + ", flapping: " + totals.getNFlapping());
			}
			else
			{
				g.setColor(Color.BLACK);

				String which = getEitherParameter(r, "host_name");
				if (r.getType() == DataType.SERVICE)
					which += " / " + ((Service)r.getObject2()).getServiceName();
				putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, which);

				putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, getEitherParameter(r, "plugin_output"));
				putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "exec time: " + getEitherParameter(r, "check_execution_time") + ", latency: " + getEitherParameter(r, "check_latency") + ", " + getEitherParameter(r, "performance_data"));

				putLine(g, font, 51, 51 + curRowHeight * rowOffset, halfWidth, curRowHeight, "last check: " + cnvSec(getEitherParameter(r, "last_check")));
				putLine(g, font, 51 + halfWidth, 51 + curRowHeight * (rowOffset++), halfWidth, curRowHeight, "next: " + cnvSec(getEitherParameter(r, "next_check")));

				putLine(g, font, 51, 51 + curRowHeight * rowOffset, halfWidth, curRowHeight, "last state change: " + cnvSec(getEitherParameter(r, "last_state_change")));
				putLine(g, font, 51 + halfWidth, 51 + curRowHeight * (rowOffset++), halfWidth, curRowHeight, "last hard change: " + cnvSec(getEitherParameter(r, "last_hard_state_change")));

				if (r.getType() == DataType.HOST) {
					putLine(g, font, 51, 51 + curRowHeight * rowOffset, halfWidth, curRowHeight, "last up: " + cnvSec(getEitherParameter(r, "last_time_up")));
					putLine(g, font, 51 + halfWidth, 51 + curRowHeight * (rowOffset++), halfWidth, curRowHeight, "last down: " + cnvSec(getEitherParameter(r, "last_time_down")));
					putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "last time unreachable: " + cnvSec(getEitherParameter(r, "last_time_unreachable")));
				}
				else {
					putLine(g, font, 51, 51 + curRowHeight * rowOffset, halfWidth, curRowHeight, "last ok: " + cnvSec(getEitherParameter(r, "last_time_ok")));
					putLine(g, font, 51 + halfWidth, 51 + curRowHeight * (rowOffset++), halfWidth, curRowHeight, "warning: " + cnvSec(getEitherParameter(r, "last_time_warning")));
					putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "last critical: " + cnvSec(getEitherParameter(r, "last_time_critical")));
				}
				putLine(g, font, 51, 51 + curRowHeight * rowOffset, halfWidth, curRowHeight, "last notification: " + cnvSec(getEitherParameter(r, "last_notification")));
				putLine(g, font, 51 + halfWidth, 51 + curRowHeight * (rowOffset++), halfWidth, curRowHeight, "next: " + cnvSec(getEitherParameter(r, "next_notification")));
				putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "last update: " + cnvSec(getEitherParameter(r, "last_update")));
				putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "is flapping: " + getEitherParameter(r, "is_flapping") + ", state change: " + getEitherParameter(r, "percent_state_change") + "%");
				putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "acked: " + getEitherParameter(r, "problem_has_been_acknowledged") + ", active checks: " + getEitherParameter(r, "active_checks_enabled") + ", passive checks: " + getEitherParameter(r, "passive_checks_enabled"));
				String entry_time = getEitherParameter(r, "entry_time");
				if (entry_time.equals("?") == false) {
					putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "Comment entry time: " + cnvSec(entry_time) + ":");
					putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, "comment: " + getEitherParameter(r, "comment_data"));
				}

				String longPluginOutput = getEitherParameter(r, "long_plugin_output");
				while(rowOffset < useNRows && longPluginOutput != null) {
					int lf = longPluginOutput.indexOf("\n");
					String emit = "";
					if (lf == -1) {
						emit = longPluginOutput.trim();
						longPluginOutput = null;
					}
					else {
						if (lf > 0)
							emit = longPluginOutput.substring(0, lf - 1).trim();
						if ((lf + 1) < longPluginOutput.length())
							longPluginOutput = longPluginOutput.substring(lf + 1);
						else
							longPluginOutput = null;
					}
					if (emit.length() > 0)
						putLine(g, font, 51, 51 + curRowHeight * (rowOffset++), useWidth, curRowHeight, emit);
				}

				if (rowOffset < useNRows && config.getSparkLineWidth() > 0) {
					BufferedImage sparkLine = coffeeSaint.getSparkLine((Host)r.getObject1(), (Service)r.getObject2(), useWidth - 2, curRowHeight, false);
					g.drawImage(sparkLine, 51, 51 + rowOffset * curRowHeight, null);
				}
			}
		}
		catch(Exception e) {
			statistics.incExceptions();

			CoffeeSaint.showException(e);

			if (g != null)
				showCoffeeSaintProblem(e, g, width, curRowHeight);
		}
	}

	public void paintComponent(Graphics g)
	{
		System.out.println("+++ PAINT START +++ " + getWidth() + "x" + getHeight());
		// super.paintComponent(g); not needed, doing everything myself
		final Graphics2D g2d = (Graphics2D)g;
		final int rowHeight = (getHeight() - (config.getShowHeader() ? config.getUpperRowBorderHeight() : 0)) / config.getNRows();

		configureRendered(g2d, true);

		drawProblems(g, getWidth(), getHeight(), rowHeight);

		if (currentRow != null && getWidth() > 200 && getHeight() > 200) {
			drawPopup(currentRow, g, getWidth(), getHeight());
		}

		System.out.println("+++ PAINT END +++");
	}

	public Graphics gg() {
		return getGraphics();
	}

	public void guiLoop() throws Exception
	{
		final Graphics g = getGraphics();
		final Graphics2D g2d = (Graphics2D)g;
		long lastLeft = -1;
		int headerScrollerX = 0;
		double scrollTs = (double)System.currentTimeMillis() / 1000.0;

		addMouseListener(this);

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
				// g.fillRect(0, 0, getWidth(), getHeight());
				repaint();
				coffeeSaint.cleanUp();
			}

			movingPartsSemaphore.acquireUninterruptibly();
			for(ScrollableContent currentMovingPart : windowMovingParts)
			{
				currentMovingPart.scrollView(g2d, config.getScrollingPixelsPerSecond());
			}
			if (bordersParameters != null && windowMovingParts.size() > 0)
			{
				drawBorders((Graphics2D)g, bordersParameters);
				if (config.getDrawProblemServiceSplitLine())
					drawProblemServiceSplitLine(g, rowHeight, getHeight(), bordersParameters.getNProblems());
			}
			// FIXME bottomLine
			movingPartsSemaphore.release();

			Position counterPosition = config.getCounterPosition();
			if (config.getCounter() && lastLeft != left && counterPosition != null)
			{
				drawCounter(g, counterPosition, getWidth(), getHeight(), rowHeight, (int)left);
				lastLeft = left;
			}

			if (windowMovingParts.size() > 0)
				Thread.sleep(5);
			else
				Thread.sleep(1000);
		}
	}
}
