/* Released under GPL2, (C) 2009-2010 by folkert@vanheusden.com */
public enum Position { UPPER_LEFT("upper-left"), UPPER_RIGHT("upper-right"), LOWER_LEFT("lower-left"), LOWER_RIGHT("lower-right"), CENTER("center"), NONE("nowhere"), LEFT("left"), RIGHT("right");
	String name;
	Position(String s) {
		name = s;
	}
	public String toString() {
		return name;
	}
}
