/* Released under the GPL2. See license.txt for details. */
package com.vanheusden.nagios;

import java.net.Socket;
import java.net.URL;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileReader;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Class JavNag is the main class for obtaining and processing Nagios statusses.
 * It can retrieve the status from a Nagios status-file or from a (TCP-)socket.
 *
 * @author	Folkert van Heusden
 * @version	%I%, %G%
 * @since	0.1 
 */
public class JavNag
{
	List<Host> hosts = new ArrayList<Host>();

	private Host addAndOrFindHost(String hostName)
	{
		for(Host currentHost : hosts)
		{
			if (currentHost.getHostName().equals(hostName))
				return currentHost;
		}

		Host newHost = new Host(hostName);

		hosts.add(newHost);

		return newHost;
	}

	private void addHostParameterEntry(Host host, String serviceParameterName, String serviceParameterValue)
	{
		host.addParameter(serviceParameterName, serviceParameterValue);
	}

	private void addServiceEntry(Service service, String serviceParameterName, String serviceParameterValue)
	{
		service.addParameter(new ParameterEntry(serviceParameterName, serviceParameterValue));
	}

	private void addFromNagios1(List<String> fileDump) throws Exception
	{
		for(String currentLine : fileDump)
		{
			if (currentLine.length() == 0)
				continue;
			if (currentLine.substring(0, 1).equals("#")) // only the first line should have this comment
				continue;

			String [] elements = currentLine.split(";");
			if (elements.length < 2)
				continue;
			int space = elements[0].indexOf(" ");
			if (space == -1)
				throw new Exception("Invalid line: first field should contain space (" + currentLine + ")");
			String type = elements[0].substring(space + 1);
			String timeStamp = elements[0].substring(0, space);
			String hostName = elements[1];

			if (type.equals("HOST"))
			{
				if (elements.length != 21)
					throw new Exception("Expecting 21 for a HOST-line, got " + elements.length + ": " + currentLine);

				Host host = addAndOrFindHost(hostName);
				int current_state = 255;
				if (elements[2].equals("UP") || elements[2].equals("OK"))
					current_state = 0;
				else
					current_state = 2;
				addHostParameterEntry(host, "current_state", "" + current_state);
				addHostParameterEntry(host, "last_check", elements[3]);
				addHostParameterEntry(host, "last_state_change", elements[4]);
				addHostParameterEntry(host, "problem_has_been_acknowledged", elements[5]);
				addHostParameterEntry(host, "last_time_up", elements[6]);
				addHostParameterEntry(host, "last_time_down", elements[7]);
				addHostParameterEntry(host, "last_time_unreachable", elements[8]);
				addHostParameterEntry(host, "last_notification", elements[9]);
				addHostParameterEntry(host, "current_notification_number", elements[10]);
				addHostParameterEntry(host, "notifications_enabled", elements[11]);
				addHostParameterEntry(host, "event_handler_enabled", elements[12]);
				addHostParameterEntry(host, "active_checks_enabled", elements[13]); /* in 2.0 it has been split up in passive and active */
				addHostParameterEntry(host, "flap_detection_enabled", elements[14]);
				addHostParameterEntry(host, "is_flapping", elements[15]);
				addHostParameterEntry(host, "percent_state_change", elements[16]);
				addHostParameterEntry(host, "scheduled_downtime_depth", elements[17]);
				addHostParameterEntry(host, "failure_prediction_enabled", elements[18]);
				addHostParameterEntry(host, "process_performance_data", elements[19]);
				addHostParameterEntry(host, "plugin_output", elements[20]);

				addHostParameterEntry(host, "state_type", "1"); // version 1 doesn't set this so always assume hard state
				// FIXME addHostParameterEntry(host, "performance_data", ""); RETRIEVE FROM PLUGIN_OUTPUT
			}
			else if (type.equals("SERVICE"))
			{
				if (elements.length != 31 && elements.length != 32)
					throw new Exception("Expecting 21 for a SERVICE-line, got " + elements.length + ": " + currentLine);

				Host host = addAndOrFindHost(hostName);
				Service service = host.addAndOrFindService(elements[2]);
				int current_state = 255;
				if (elements[3].equals("OK"))
					current_state = 0;
				else if (elements[3].equals("WARNING"))
					current_state = 1;
				else if (elements[3].equals("CRITICAL"))
					current_state = 2;
				else if (elements[3].equals("UNKNOWN") || elements[3].equals("PENDING"))
					current_state = 3;
				addServiceEntry(service, "current_state", "" + current_state);
				addServiceEntry(service, "retry_number", elements[4]);
				String stateType = "0";
				if (elements[5].equals("HARD"))
					stateType = "1";
				else if (elements[5].equals("SOFT"))
					stateType = "0";
				else
					stateType = elements[5];
				addServiceEntry(service, "state_type", stateType);
				addServiceEntry(service, "last_check", elements[6]);
				addServiceEntry(service, "next_check", elements[7]);
				addServiceEntry(service, "check_type", elements[8]);
				addServiceEntry(service, "active_checks_enabled", elements[9]);
				addServiceEntry(service, "accept_passive_checks", elements[10]);
				addServiceEntry(service, "event_handler_enabled", elements[11]);
				addServiceEntry(service, "last_state_change", elements[12]);
				addServiceEntry(service, "problem_has_been_acknowledged", elements[13]);
				addServiceEntry(service, "last_hard_state", elements[14]);
				addServiceEntry(service, "last_time_ok", elements[15]);
				addServiceEntry(service, "last_time_unknown", elements[16]);
				addServiceEntry(service, "last_time_warning", elements[17]);
				addServiceEntry(service, "last_time_critical", elements[18]);
				addServiceEntry(service, "last_notification", elements[19]);
				addServiceEntry(service, "current_notification_number", elements[20]);
				addServiceEntry(service, "notifications_enabled", elements[21]);
				addServiceEntry(service, "check_latency", elements[22]);
				addServiceEntry(service, "check_execution_time", elements[23]);
				addServiceEntry(service, "flap_detection_enabled", elements[24]);
				addServiceEntry(service, "is_flapping", elements[25]);
				addServiceEntry(service, "percent_state_change", elements[26]);
				addServiceEntry(service, "scheduled_downtime_depth", elements[27]);
				addServiceEntry(service, "failure_prediction_enabled", elements[28]);
				addServiceEntry(service, "process_performance_date", elements[29]);
				addServiceEntry(service, "obsess_over_service", elements[30]);
				assert elements.length == 31 || elements.length == 32;
				if (elements.length == 32) // in case of missing plugin output
					addServiceEntry(service, "plugin_output", elements[31]);
				else
					addServiceEntry(service, "plugin_output", "");
				// FIXME addHostParameterEntry(host, "performance_data", ""); RETRIEVE FROM PLUGIN_OUTPUT
			}
		}
	}

	private void addFromNagios2And3(List<String> fileDump) throws Exception
	{
		Host host = null;
		Service service = null;
		LineType lineType = LineType.ignore;

		for(String currentLine : fileDump)
		{
			if (currentLine.indexOf("{") != -1)
			{
				String type = null;
				int space = currentLine.indexOf(" ");
				if (space != -1)
					type = currentLine.substring(0, space);

				if (type == null)
					lineType = LineType.ignore;
				else if (type.equals("hoststatus"))
					lineType = LineType.host;
				else if (type.equals("servicestatus"))
					lineType = LineType.service;
				else
					lineType = LineType.ignore;

				host = null;
				service = null;
			}
			else if (lineType != LineType.ignore)
			{
				String parameter = null, value = null;
				int isIndex = currentLine.indexOf("=");

				if (isIndex == -1)
				{
					if (currentLine.indexOf("}") != -1)
						lineType = LineType.ignore;
				}
				else
				{
					parameter = currentLine.substring(0, isIndex).trim();
					value = currentLine.substring(isIndex + 1).trim();
				}

				if (parameter != null && value != null)
				{
					if (parameter.equals("host_name"))
						host = addAndOrFindHost(value);
					else if (parameter.equals("service_description"))
						service = host.addAndOrFindService(value);
					else if (lineType == LineType.host && host != null)
						addHostParameterEntry(host, parameter, value);
					else if (lineType == LineType.service && host != null && service != null)
						addServiceEntry(service, parameter, value);
					else
						throw new Exception("expected host_name/service_description to be at the start of a definition, got line: " + currentLine);
				}
			}
		}
	}

	/**
	 * Returns a list of hosts found in the Nagios status.
	 *
	 * @return	A list of Host-objects.
	 */
	public List<Host> getListOfHosts()
	{
		return hosts;
	}

	/**
	 * Returns a Host-object by the hostname given. This hostname is the hostname as found in the Nagios configuration. E.g. hosts.cfg
	 *
	 * @param hostName	Hostname.
	 * @return		A Host object.
	 */
	public Host getHost(String hostName)
	{
		for(Host currentHost : hosts)
		{
			if (currentHost.getHostName().equals(hostName))
				return currentHost;
		}

		return null;
	}

	/**
	 * calculateStatistics returns a Totals object with statistics-totals. E.g. total number
	 * of services with critical/warning/ok states.
	 *
	 * @return Totals	Object with totals.
	 */
	public Totals calculateStatistics()
	{
		int nCritical = 0, nWarning = 0, nOk = 0;
		int nUp = 0, nDown = 0, nUnreachable = 0, nPending = 0;
		int nHosts = 0, nServices = 0;
		int nStateUnknownHost = 0, nStateUnknownService = 0;

		for(Host currentHost : hosts)
		{
			String current_state = currentHost.getParameter("current_state");
			if (current_state == null)
			{
				nStateUnknownHost++;
				continue;
			}

			if (current_state.equals("0"))
				nUp++;
			else if (current_state.equals("1"))
				nDown++;
			else if (current_state.equals("2"))
				nUnreachable++;
			else if (current_state.equals("3"))
				nPending++;

			nHosts++;

			for(Service currentService : currentHost.getServices())
			{
				current_state = currentService.getParameter("current_state");
				if (current_state == null)
				{
					nStateUnknownService++;
					continue;
				}

				if (current_state.equals("0"))
					nOk++;
				else if (current_state.equals("1"))
					nWarning++;
				else if (current_state.equals("2"))
					nCritical++;

				nServices++;
			}
		}

		return new Totals(nCritical, nWarning, nOk, nUp, nDown, nUnreachable, nPending, nHosts, nServices, nStateUnknownHost, nStateUnknownService);
	}

	/**
	 * Returns the number of hosts loaded.
	 *
	 * @return		count
	 */
	public int getNumberOfHosts()
	{
		return hosts.size();
	}

	/**
	 * Find a host by hostname.
	 *
	 * @return	Host object
	 */
	public Host findHostByHostName(String hostName)
	{
		for(Host currentHost : hosts)
		{
			if (currentHost.getHostName().equals(hostName))
				return currentHost;
		}

		return null;
	}

	/**
	 * Checks if a host is down and if it is if it should be shown.
	 * That is decided by looking at parameters like state_type and checks enabled.
	 *
	 * @param host			The host to check.
	 * @param always_notify		Also return true when notifications_enabled is set to false (in Nagios).
	 * @param also_acknowledged	Also return true when the problem has been acknowledged in Nagios.
	 * @return 			true/false
	 */
	public boolean shouldIShowHost(Host host, boolean always_notify, boolean also_acknowledged, boolean also_scheduled_downtime, boolean also_soft_state, boolean also_disabled_active_checks, boolean show_flapping)
	{
		if (host.getParameters().size() == 0)
			return false;

		if (!show_flapping && host.getParameter("is_flapping").equals("1") == true)
			return false;

		if (!also_soft_state && host.getParameter("state_type").equals("0") == true) // if SOFT, do not show
			return false;

		if (host.getParameter("current_state").equals("0") == true) // if OK do not show
			return false;

		// if active_checks are not enabled and passive checks neither, do not show
		if (!also_disabled_active_checks && host.getParameter("active_checks_enabled").equals("0") == true && host.getParameter("passive_checks_enabled").equals("0") == true)
			return false;

		// downtime_depth == 0, do not show
		if (!also_scheduled_downtime && Double.valueOf(host.getParameter("scheduled_downtime_depth")) != 0.0)
		{
			System.out.println("scheduled_downtime_depth " + host.getParameter("scheduled_downtime_depth"));
			return false;
		}

		// notifications disabled, do not show
		if (!always_notify && host.getParameter("notifications_enabled").equals("0") == true)
			return false;

		// is has been acknowledged, do not show
		if (!also_acknowledged && host.getParameter("problem_has_been_acknowledged").equals("1") == true)
			return false;

		return true;
	}

	/**
	 * Checks if a service is down and if it is if it should be shown.
	 * That is decided by looking at parameters like state_type and checks enabled.
	 *
	 * @param service		The service to check.
	 * @param always_notify		Also return true when notifications_enabled is set to false (in Nagios).
	 * @param also_acknowledged	Also return true when the problem has been acknowledged in Nagios.
	 * @return 			true/false
	 */
	public boolean shouldIShowService(Service service, boolean always_notify, boolean service_also_acknowledged, boolean service_also_scheduled_downtime, boolean also_soft_state, boolean also_disabled_active_checks, boolean show_flapping)
	{
		if (!also_soft_state && service.getParameter("state_type").equals("1") == false)
			return false;

		if (service.getParameter("current_state").equals("0") == true)
			return false;

		if (!show_flapping && service.getParameter("is_flapping").equals("1") == true)
			return false;

		if (!also_disabled_active_checks && service.getParameter("active_checks_enabled").equals("0") == true && service.getParameter("passive_checks_enabled").equals("0") == true)
			return false;

		if (!service_also_scheduled_downtime && Double.valueOf(service.getParameter("scheduled_downtime_depth")) != 0.0)
			return false;

		if (!always_notify && service.getParameter("notifications_enabled").equals("0") == true)
			return false;

		if (!service_also_acknowledged && service.getParameter("problem_has_been_acknowledged").equals("1") == true)
			return false;

		return true;
	}

	public Double getAvgCheckLatency()
	{
		int n = 0;
		double tot = 0.0;

		for(Host currentHost : hosts)
		{
			String check_latency = currentHost.getParameter("check_latency");
			if (check_latency != null)
			{
				n++;
				tot += Double.valueOf(check_latency);
			}

			for(Service currentService : currentHost.getServices())
			{
				check_latency = currentService.getParameter("check_latency");
				if (check_latency != null)
				{
					n++;
					tot += Double.valueOf(check_latency);
				}
			}
		}

		if (n == 0)
			return null;

		return tot / (double)n;
	}

	public void loadNagiosData(String fileName, NagiosVersion nagiosVersion) throws Exception
	{
		List<String> fileDump = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		String line;

		while((line = in.readLine()) != null)
			fileDump.add(line);

		in.close();

		if (nagiosVersion == NagiosVersion.V1)
		{
			addFromNagios1(fileDump);
		}
		else if (nagiosVersion == NagiosVersion.V2 || nagiosVersion == NagiosVersion.V3)
		{
			addFromNagios2And3(fileDump);
		}
	}

	public void loadNagiosData(String host, int port, NagiosVersion nagiosVersion, boolean compressed) throws Exception
	{
		List<String> fileDump = new ArrayList<String>();
		Socket socket = new Socket(host, port);
		BufferedReader in;
		String line;

		if (compressed)
		{
			in = new BufferedReader(new InputStreamReader(new GZIPInputStream(socket.getInputStream())));
		}
		else
		{
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		}

		while((line = in.readLine()) != null)
			fileDump.add(line);

		in.close();

		socket.close();

		if (nagiosVersion == NagiosVersion.V1)
		{
			addFromNagios1(fileDump);
		}
		else if (nagiosVersion == NagiosVersion.V2 || nagiosVersion == NagiosVersion.V3)
		{
			addFromNagios2And3(fileDump);
		}
	}

	// via livestatus
	public void loadNagiosDataLiveStatus(String host, int port) throws Exception {
		loadNagiosDataLiveStatus_hosts(host, port);

		loadNagiosDataLiveStatus_services(host, port);
	}

	private void loadNagiosDataLiveStatus_hosts(String host, int port) throws Exception {
		Socket socket = new Socket(host, port);
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

		String [][] map = {
			{ "accept_passive_checks", "passive_checks_enabled" },
			{ "acknowledged", "problem_has_been_acknowledged" },
			{ "acknowledgement_type", "acknowledgement_type" },
			{ "active_checks_enabled", "active_checks_enabled" },
			{ "check_command", "check_command" },
			{ "check_interval", "check_interval" },
			{ "check_options", "check_options" },
			{ "check_period", "check_period" },
			{ "check_type", "check_type" },
			{ "current_attempt", "current_attempt" },
			{ "flap_detection_enabled", "flap_detection_enabled" },
			{ "has_been_checked", "has_been_checked" },
			{ "is_flapping", "is_flapping" },
			{ "last_check", "last_check" },
			{ "last_hard_state", "last_hard_state" },
			{ "last_hard_state_change", "last_hard_state_change" },
			{ "last_notification", "last_notification" },
			{ "last_state_change", "last_state_change" },
			{ "long_plugin_output", "long_plugin_output" },
			{ "name", "host_name" },
			{ "next_check", "next_check" },
			{ "next_notification", "next_notification" },
			{ "notification_period", "notification_period" },
			{ "notifications_enabled", "notifications_enabled" },
			{ "obsess_over_host", "obsess_over_host" },
			{ "percent_state_change", "percent_state_change" },
			{ "perf_data", "performance_data" },
			{ "plugin_output", "plugin_output" },
			{ "process_performance_data", "process_performance_data" },
			{ "retry_interval", "retry_interval" },
			{ "scheduled_downtime_depth", "scheduled_downtime_depth" },
			{ "state", "current_state" },
			{ "state_type", "state_type" }
		};

		String request = "GET hosts\nSeparators: 10 124 44 125\nColumns:";
		int nameIndex = -1;
		int nFieldsRequested = 0, index = 0;
		for(String [] curMapEntry : map) {
			if (curMapEntry[0].equals("name")) {
				nameIndex = index;
			}
			request += " " + curMapEntry[0];
			nFieldsRequested++;
			index++;
		}
		if (nameIndex == -1)
			throw new Exception("Cannot parse livestatus stream: 'name' missing");
		request += "\n";
		out.write(request, 0, request.length());
		out.flush();
		socket.shutdownOutput();

		for(;;) {
			String line = in.readLine();
			if (line == null)
				break;

			String [] fieldsCur = line.split("\\|");
System.out.println("hostline: " + line);
			System.out.println("Adding host: " + fieldsCur[nameIndex]);
			Host hostObj = addAndOrFindHost(fieldsCur[nameIndex]);

			int fieldIndex = 0;
			for(String [] mapEntry : map) {
				addHostParameterEntry(hostObj, mapEntry[1], fieldsCur[fieldIndex]);
				fieldIndex++;
			}
		}
	}

	private void loadNagiosDataLiveStatus_services(String host, int port) throws Exception {
		Socket socket = new Socket(host, port);
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

		String [][] map = {
			{ "accept_passive_checks", "passive_checks_enabled" },
			{ "acknowledged", "problem_has_been_acknowledged" },
			{ "acknowledgement_type", "acknowledgement_type" },
			{ "active_checks_enabled", "active_checks_enabled" },
			{ "check_command", "check_command" },
			{ "check_interval", "check_interval" },
			{ "check_options", "check_options" },
			{ "check_period", "check_period" },
			{ "check_type", "check_type" },
			{ "current_attempt", "current_attempt" },
			{ "current_notification_number", "current_notification_number" },
			{ "description", "service_description" },
			{ "event_handler", "event_handler" },
			{ "event_handler_enabled", "event_handler_enabled" },
			{ "execution_time", "check_execution_time" },
			{ "flap_detection_enabled", "flap_detection_enabled" },
			{ "has_been_checked", "has_been_checked" },
			{ "host_name", "host_name" },
			{ "is_flapping", "is_flapping" },
			{ "last_check", "last_check" },
			{ "last_hard_state", "last_hard_state" },
			{ "last_hard_state_change", "last_hard_state_change" },
			{ "last_notification", "last_notification" },
			{ "last_state_change", "last_state_change" },
			{ "latency", "check_latency" },
			// { "long_plugin_output", "long_plugin_output" },
			{ "max_check_attempts", "max_attempts" },
			{ "next_check", "next_check" },
			{ "next_notification", "next_notification" },
			{ "notification_period", "notification_period" },
			{ "notifications_enabled", "notifications_enabled" },
			{ "obsess_over_service", "obsess_over_service" },
			{ "percent_state_change", "percent_state_change" },
			{ "perf_data", "performance_data" },
			{ "plugin_output", "plugin_output" },
			{ "process_performance_data", "process_performance_data" },
			{ "retry_interval", "retry_interval" },
			{ "scheduled_downtime_depth", "scheduled_downtime_depth" },
			{ "state", "current_state" },
			{ "state_type", "state_type" }
		};

		String request = "GET services\nSeparators: 10 124 44 125\nColumns:";
		int hostNameIndex = -1, serviceNameIndex = -1;
		int nFieldsRequested = 0, index = 0;
		for(String [] curMapEntry : map) {
			if (curMapEntry[0].equals("host_name")) {
				hostNameIndex = index;
			}
			else if (curMapEntry[0].equals("description")) {
				serviceNameIndex = index;
			}
			request += " " + curMapEntry[0];
			nFieldsRequested++;
			index++;
		}
		request += "\n";
		out.write(request, 0, request.length());
		out.flush();
		socket.shutdownOutput();

		for(;;) {
			String line = in.readLine();
			if (line == null)
				break;

System.out.println(line);
			String [] fieldsCur = line.split("\\|");
			if (fieldsCur.length != nFieldsRequested)
				throw new Exception("Cannot parse livestatus stream: number of elements mismatch (requested: " + nFieldsRequested + ", got: " + fieldsCur.length + ")");
			System.out.println("Finding host: " + fieldsCur[hostNameIndex]);
			Host hostObj = addAndOrFindHost(fieldsCur[hostNameIndex]);
			System.out.println("Adding service: " + fieldsCur[serviceNameIndex]);
			Service service = hostObj.addAndOrFindService(fieldsCur[serviceNameIndex]);
			int fieldIndex = 0;
			for(String [] mapEntry : map) {
				addServiceEntry(service, mapEntry[1], fieldsCur[fieldIndex]);
				fieldIndex++;
			}
		}

		socket.close();
	}

	public void loadNagiosData(URL url, NagiosVersion nagiosVersion, String username, String password, boolean allowCompression) throws Exception
	{
		List<String> fileDump = new ArrayList<String>();

		HttpURLConnection HTTPConnection = (HttpURLConnection)url.openConnection();
		HTTPConnection.setFollowRedirects(true);
		if (allowCompression)
			HTTPConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
		if (username != null)
			HTTPConnection.setRequestProperty("Authorization", "Basic " + encodeBase64(username + ":" + password));
		//establish connection, get response headers
		HTTPConnection.connect();
		//obtain the encoding returned by the server
		String encoding = HTTPConnection.getContentEncoding();
		InputStream inputStream;
		//create the appropriate stream wrapper based on
		//the encoding type
		if (encoding != null && encoding.equalsIgnoreCase("gzip"))
		{
			System.out.println("GZIPed stream!");
			inputStream = new GZIPInputStream(HTTPConnection.getInputStream());
		}
		else if (encoding != null && encoding.equalsIgnoreCase("deflate"))
		{
			System.out.println("Deflated stream!");
			inputStream = new InflaterInputStream(HTTPConnection.getInputStream(), new Inflater(true));
		}
		else
		{
			inputStream = HTTPConnection.getInputStream();
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
		String line;

		while((line = in.readLine()) != null)
			fileDump.add(line);

		in.close();

		if (nagiosVersion == NagiosVersion.V1)
		{
			addFromNagios1(fileDump);
		}
		else if (nagiosVersion == NagiosVersion.V2 || nagiosVersion == NagiosVersion.V3)
		{
			addFromNagios2And3(fileDump);
		}
	}

	String encode3Chars(String in)
	{
		String result = new String();
		String encodingChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

		int tripple;
		tripple = in.charAt(0);
		tripple <<= 8;
		if (in.length() >= 2)
			tripple += in.charAt(1);
		tripple <<= 8;
		if (in.length() >= 3)
			tripple += in.charAt(2);

		for (int outputIndex=0; outputIndex<4; outputIndex++)
		{   
			int ecIndex = tripple % 64;
			result = encodingChars.substring(ecIndex, ecIndex + 1) + result;
			tripple /= 64;
		}

		return result;
	}


	public String encodeBase64(String input)
	{
		String output = new String();
		int inputLength = input.length(), index = 0;

		while(inputLength > 0)
		{
			output += encode3Chars(input.substring(index, index + Math.min(3, inputLength)));

			index += 3;
			inputLength -= 3;
		}

		return output;
	}

	public long findMostRecentCheckAge()
	{
		long mostRecent = 0;

		for(Host currentHost : hosts)
		{
			String current_state = currentHost.getParameter("current_state");
			if (current_state == null)
				continue;

			String last_check = currentHost.getParameter("last_check");
			mostRecent = Math.max(mostRecent, last_check != null ? Long.valueOf(last_check) : 0);

			String last_update = currentHost.getParameter("last_update");
			mostRecent = Math.max(mostRecent, last_update != null ? Long.valueOf(last_update) : 0);

			for(Service currentService : currentHost.getServices())
			{
				last_check = currentService.getParameter("last_check");
				mostRecent = Math.max(mostRecent, last_check != null ? Long.valueOf(last_check) : 0);

				last_update = currentService.getParameter("last_update");
				mostRecent = Math.max(mostRecent, last_update != null ? Long.valueOf(last_update) : 0);
			}
		}

		return (System.currentTimeMillis() / 1000) - mostRecent;
	}

	public JavNag()
	{
	}

	/**
	 * Loads a Nagios statusfile. See "status_file" in nagios.cfg.
	 *
	 * @param fileName	Path to status-file.
	 * @param nagiosVersion	Nagios-version this file is from.
	 * @see NagiosVersion
	 */
	public JavNag(String fileName, NagiosVersion nagiosVersion) throws Exception
	{
		loadNagiosData(fileName, nagiosVersion);
	}

	/**
	 * Retrieves a Nagios status from a host/port (TCP).
	 *
	 * @param host		Hostname (must be resolvable).
	 * @param port		Portnumber.
	 * @param nagiosVersion	Nagios-version this file is from.
	 * @see NagiosVersion
	 */
	public JavNag(String host, int port, NagiosVersion nagiosVersion, boolean compressed) throws Exception
	{
		loadNagiosData(host, port, nagiosVersion, compressed);
	}

	/**
	 * Retrieves a Nagios status from an URL
	 *
	 * @param url		URL
	 * @param nagiosVersion	Nagios-version this file is from.
	 * @see NagiosVersion
	 */
	public JavNag(URL url, NagiosVersion nagiosVersion, String username, String password, boolean allowCompression) throws Exception
	{
		loadNagiosData(url, nagiosVersion, username, password, allowCompression);
	}

	/**
	 * Loads the Nagios status via LiveStatus.
	 *
	 * @param host		host
	 * @param port		port
	 */
	public JavNag(String host, int port) throws Exception
	{
		loadNagiosDataLiveStatus(host, port);
	}
}
