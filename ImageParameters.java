/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import java.awt.Image;

class ImageParameters
{
	Image img;
	String fileName;
	int width, height;

	ImageParameters(Image img, String fileName, int width, int height)
	{
		this.img = img;
		this.fileName = fileName;
		this.width = width;
		this.height = height;
	}

	Image getImage()
	{
		return img;
	}

	String getFileName()
	{
		return fileName;
	}

	int getWidth()
	{
		return width;
	}

	int getHeight()
	{
		return height;
	}
}
