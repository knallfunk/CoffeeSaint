public enum ImageUrlType { HTTP, HTTP_MJPEG, FILE;
	public String toString()
	{
		switch(this)
		{
			case HTTP:
				return "HTTP";
			case HTTP_MJPEG:
				return "MJPEG";
			case FILE:
				return "FILE";
		}

		return null;
	}
}
