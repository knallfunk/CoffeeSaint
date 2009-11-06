/* Released under GPL 2.0
 * (C) 2009 by folkert@vanheusden.com
 */
package com.vanheusden.sockets;

public class HTTPRequestData
{
	String name, data;

	public HTTPRequestData(String name, String data)
	{
		this.name = name;
		this.data = data;
	}

	public String getName()
	{
		return name;
	}

	public String getData()
	{
		return data;
	}
}
