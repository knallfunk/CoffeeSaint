/* (C) 2009-2010 by folkert@vanheusden.com
   Released under the GPL2. See license.txt for details. */
package com.vanheusden.nagios;

public class Problem {
	Host host;
	Service service;
	String current_state;
	boolean hard;

	public Problem(Host host, Service service, String current_state, boolean hard) {
		this.host = host;
		this.service = service;
		this.current_state = current_state;
		this.hard = hard;
	}

	public Host getHost() {
		return host;
	}

	public Service getService() {
		return service;
	}

	public String getCurrent_state() {
		return current_state;
	}

	public boolean getHard() {
		return hard;
	}
}
