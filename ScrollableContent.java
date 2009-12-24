/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class ScrollableContent
{
	protected BufferedImage imageData;
	protected int curPos;
	protected int x, y;
	protected int ww;
	//
	double scrollTs;

	public ScrollableContent(int screenX, int screenY, int windowWidth)
	{
		curPos = 0;
		scrollTs = System.currentTimeMillis() / 1000.0;
		x = screenX;
		y = screenY;
		ww = windowWidth;
	}

	public ScrollableContent(BufferedImage newImage, int screenX, int screenY, int windowWidth)
	{
		curPos = 0;
		scrollTs = System.currentTimeMillis() / 1000.0;
		imageData = newImage;
		x = screenX;
		y = screenY;
		ww = windowWidth;
	}

	public void setImage(BufferedImage newImage)
	{
		imageData = newImage;
		adjustCurPos();
	}

	public boolean hasImage()
	{
		return imageData != null;
	}

	public int getCurPos()
	{
		return curPos;
	}

	protected void adjustCurPos()
	{
		while(curPos >= imageData.getWidth())
			curPos -= imageData.getWidth();
	}

	public void scrollView(Graphics2D g2d, int scrollSpeed)
	{
		int imgWidth = imageData.getWidth();
		int pixelsNeeded = ww;
		int pixelsAvail = imgWidth - curPos;
		int drawX = x, sourceX = curPos;
		while(pixelsNeeded > 0)
		{
			int plotN = Math.min(pixelsNeeded, pixelsAvail);

			g2d.drawImage(imageData, drawX, y, drawX + plotN, y + imageData.getHeight(), sourceX, 0, sourceX + plotN, imageData.getHeight(), Color.GRAY, null);

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
		double scrollMultiplier = (scrollTsNow - scrollTs) / (5.0 / 1000.0);
		// double scrollMultiplier = 1.0;

		curPos += Math.max(1, (int)(((double)scrollSpeed / (1000.0 / 5.0)) * scrollMultiplier));

		adjustCurPos();

		scrollTs = scrollTsNow;
	}
}
