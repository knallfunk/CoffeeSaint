/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class ScrollableContent {
	protected static final int scrollSleep = 5;
	protected BufferedImage imageData;
	protected int curPos;
	protected int x, y;
	protected int ww;
	protected boolean isFlash;
	protected boolean isScroll;
	protected int flashCounter = 0;
	//
	double scrollTs;

	public static int getScrollSleep() {
		return scrollSleep;
	}

	public ScrollableContent(int screenX, int screenY, int windowWidth, boolean isFlash, boolean isScroll) {
		curPos = 0;
		scrollTs = System.currentTimeMillis() / 1000.0;
		x = screenX;
		y = screenY;
		ww = windowWidth;
		this.isFlash = isFlash;
		this.isScroll = isScroll;
	}

	public ScrollableContent(BufferedImage newImage, int screenX, int screenY, int windowWidth, boolean isFlash, boolean isScroll) {
		curPos = 0;
		scrollTs = System.currentTimeMillis() / 1000.0;
		imageData = newImage;
		x = screenX;
		y = screenY;
		ww = windowWidth;
		this.isFlash = isFlash;
		this.isScroll = isScroll;
	}

	int getX() {
		return x;
	}

	int getY() {
		return y;
	}

	public void setImage(BufferedImage newImage) {
		imageData = newImage;
		adjustCurPos();
	}

	public boolean hasImage() {
		return imageData != null;
	}

	public int getCurPos() {
		return curPos;
	}

	protected void adjustCurPos() {
		while(curPos >= imageData.getWidth())
			curPos -= imageData.getWidth();
	}

	public void scrollView(Graphics2D g2d, int scrollSpeed) {
		int flashInterval = (1000 / scrollSleep) / 2;
		boolean doFlash = false;
		flashCounter++;
		if (flashCounter >= flashInterval) {
			flashCounter = 0;
			doFlash = true;
		}

		if (isFlash && doFlash) {
			int width = imageData.getWidth();
			int height = imageData.getHeight();
			int [] pixels = imageData.getRGB(0, 0, width, height, null, 0, width);
			for(int index=0; index<pixels.length; index++)
				pixels[index] = ~pixels[index];
			imageData.setRGB(0, 0, width, height, pixels, 0, width);
		}

		if (isScroll) {
			int imgWidth = imageData.getWidth();
			int pixelsNeeded = ww;
			int pixelsAvail = imgWidth - curPos;
			int drawX = x, sourceX = curPos;

			while(pixelsNeeded > 0) {
				int plotN = Math.min(pixelsNeeded, pixelsAvail);

				g2d.drawImage(imageData, drawX, y, drawX + plotN, y + imageData.getHeight(), sourceX, 0, sourceX + plotN, imageData.getHeight(), Color.GRAY, null);

				pixelsNeeded -= plotN;
				pixelsAvail -= plotN;
				drawX += plotN;
				sourceX += plotN;

				if (pixelsAvail <= 0) {
					pixelsAvail = imgWidth;
					sourceX = 0;
				}
			}

			double scrollTsNow = (double)System.currentTimeMillis() / 1000.0;
			double scrollMultiplier = (scrollTsNow - scrollTs) / (5.0 / 1000.0);
			// double scrollMultiplier = 1.0;

			curPos += Math.max(1, (int)(((double)scrollSpeed / (1000.0 / 5.0)) * scrollMultiplier));

			adjustCurPos();

			scrollTs = scrollTsNow;
		}
		else if (doFlash) {
			int useN = Math.min(ww, imageData.getWidth());
			g2d.drawImage(imageData, x, y, x + useN, y + imageData.getHeight(), 0, 0, useN, imageData.getHeight(), Color.GRAY, null);
		}
	}
}
