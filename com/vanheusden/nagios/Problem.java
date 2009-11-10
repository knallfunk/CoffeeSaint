/* Released under the GPL2. See license.txt for details. */
package com.vanheusden.nagios;

public class Problem
{
	String message;
	String current_state;

	public Problem(String message, String current_state)
	{
		this.message = message;
		this.current_state = current_state;
	}

	public String getMessage()
	{
		return message;
	}

	public String getCurrent_state()
	{
		return current_state;
	}
}
