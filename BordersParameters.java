/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */

public class BordersParameters
{
	protected int nProblems, nColumns, windowWidth, rowHeight;

	public BordersParameters(int nProblems, int nColumns, int windowWidth, int rowHeight)
	{
		this.nProblems = nProblems;
		this.nColumns = nColumns;
		this.windowWidth = windowWidth;
		this.rowHeight = rowHeight;
	}

	public int getNProblems()
	{
		return nProblems;
	}

	public int getNColumns()
	{
		return nColumns;
	}

	public int getWindowWidth()
	{
		return windowWidth;
	}

	public int getRowHeight()
	{
		return rowHeight;
	}
}
