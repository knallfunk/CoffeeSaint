class Problem
{
	String message;
	String current_state;

	Problem(String message, String current_state)
	{
		this.message = message;
		this.current_state = current_state;
	}

	String getMessage()
	{
		return message;
	}

	String getCurrent_state()
	{
		return current_state;
	}
}
