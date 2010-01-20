/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */
public enum Position { UPPER_LEFT , UPPER_RIGHT, LOWER_LEFT, LOWER_RIGHT, CENTER, NONE, LEFT, RIGHT;
	public String toString()
	{
		switch(this)
		{
			case UPPER_LEFT:
				return "upper-left";
			case UPPER_RIGHT:
				return "upper-right";
			case LOWER_LEFT:
				return "lower-left";
			case LOWER_RIGHT:
				return "lower-right";
			case CENTER:
				return "center";
			case NONE:
				return "nowhere";
			case LEFT:
				return "left";
			case RIGHT:
				return "right";
		}

		return null;
	}
}
