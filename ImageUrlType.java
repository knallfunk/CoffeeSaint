public enum ImageUrlType { HTTP("HTTP"), HTTP_MJPEG("MJPEG"), FILE("FILE");
	String name;
	ImageUrlType(String s) {
		name = s;
	}
	public String toString() {
		return name;
	}
}
