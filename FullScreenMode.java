public enum FullScreenMode { NONE, UNDECORATED, FULLSCREEN;
	public String toString()
	{
		switch(this)
		{
			case NONE:
				return "none";
			case UNDECORATED:
				return "undecorated";
			case FULLSCREEN:
				return "fullscreen";
			default:
				return "?";
		}
	}
 };
