/* Released under the GPL2. See license.txt for details. */
package com.vanheusden.nagios;

public class Problem
{
	Host host;
	Service service;
	String current_state;

	public Problem(Host host, Service service, String current_state)
	{
		this.host = host;
		this.service = service;
		this.current_state = current_state;
	}

	public Host getHost()
	{
		return host;
	}

	public Service getService()
	{
		return service;
	}

	public String getCurrent_state()
	{
		return current_state;
	}
}
