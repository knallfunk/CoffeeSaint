/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import java.awt.Font;

public class RowParameters
{
	private int textWidth;
	private boolean shrunkMore;
	private double newAsc;
	private Font f;
	private double heightDiff;

	public RowParameters(int textWidth, boolean shrunkMore, double newAsc, Font f, double heightDiff)
	{
		this.textWidth = textWidth;
		this.shrunkMore = shrunkMore;
		this.newAsc = newAsc;
		this.f = f;
		this.heightDiff = heightDiff;
	}

	public int getTextWidth()
	{
		return textWidth;
	}

	public boolean getShrunkMore()
	{
		return shrunkMore;
	}

	public double getAsc()
	{
		return newAsc;
	}

	public Font getAdjustedFont()
	{
		return f;
	}

	public double getHeightDiff()
	{
		return heightDiff;
	}
}
