/* Released under GPL2, (C) 2009 by folkert@vanheusden.com */

public enum FullScreenMode { NONE("none"), UNDECORATED("undecorated"), FULLSCREEN("fullscreen"), ALLMONITORS("allmonitors");
	String name;
	FullScreenMode(String s) {
		name = s;
	}
	public String toString() {
		return name;
	}
 };
