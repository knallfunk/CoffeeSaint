/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
import java.awt.Color;

class ColorPair
{
	String name;
	Color value;

	public ColorPair(String name, Color value)
	{
		this.name = name;
		this.value = value;
	}

	public String getName()
	{
		return name;
	}

	public boolean equals(Object o)
	{
		String str = (String)o;

		return str.equalsIgnoreCase(name);
	}

	public Color getColor()
	{
		return value;
	}
}
