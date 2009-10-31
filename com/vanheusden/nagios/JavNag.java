/* Released under the GPL2. See license.txt for details. */
import java.util.*;
import java.net.Socket;
import java.net.URL;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;

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
			String [] elements = currentLine.split(";");
			if (elements.length != 21 && elements.length != 32)
				throw new Exception("Expecting either 21 or 32 elements per line, got " + elements.length);
			int space = elements[0].indexOf(" ");
			if (space == -1)
				throw new Exception("Invalid line: first field should contain space");
			String timeStamp = elements[0].substring(0, space);
			String type = elements[0].substring(space + 1);
			String hostName = elements[1];

			if (type.equals("HOST"))
			{
				Host host = addAndOrFindHost(hostName);
				int current_state = 255;
				if (elements[2].equals("UP"))
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
			}
			else if (type.equals("SERVICE"))
			{
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
				addServiceEntry(service, "state_type", elements[5]);
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
				addServiceEntry(service, "plugin_output", elements[31]);
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
	 * Loads a Nagios statusfile. See "status_file" in nagios.cfg.
	 *
	 * @param fileName	Path to status-file.
	 * @param nagiosVersion	Nagios-version this file is from.
	 * @see NagiosVersion
	 */
	public JavNag(String fileName, NagiosVersion nagiosVersion) throws Exception
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

	/**
	 * Retrieves a Nagios status from a host/port (TCP).
	 *
	 * @param host		Hostname (must be resolvable).
	 * @param port		Portnumber.
	 * @param nagiosVersion	Nagios-version this file is from.
	 * @see NagiosVersion
	 */
	public JavNag(String host, int port, NagiosVersion nagiosVersion) throws Exception
	{
		List<String> fileDump = new ArrayList<String>();
		Socket socket = new Socket(host, port);
		InputStreamReader inputStream = new InputStreamReader(socket.getInputStream());
		BufferedReader in = new BufferedReader(inputStream);
		String line;

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

	/**
	 * Retrieves a Nagios status from an URL
	 *
	 * @param url		URL
	 * @param nagiosVersion	Nagios-version this file is from.
	 * @see NagiosVersion
	 */
	public JavNag(URL url, NagiosVersion nagiosVersion) throws Exception
	{
		List<String> fileDump = new ArrayList<String>();
		InputStreamReader inputStream = new InputStreamReader(url.openStream());
		BufferedReader in = new BufferedReader(inputStream);
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

		for(Host currentHost : hosts)
		{
			String current_state = currentHost.getParameter("current_state");

			if (current_state.equals("0"))
				nUp++;
			else if (current_state.equals("1"))
				nDown++;
			else if (current_state.equals("2"))
				nUnreachable++;
			else if (current_state.equals("3"))
				nPending++;

			for(Service currentService : currentHost.getServices())
			{
				current_state = currentService.getParameter("current_state");

				if (current_state.equals("0"))
					nOk++;
				else if (current_state.equals("1"))
					nWarning++;
				else if (current_state.equals("2"))
					nCritical++;
			}
		}

		return new Totals(nCritical, nWarning, nOk, nUp, nDown, nUnreachable, nPending);
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
	public boolean shouldIShowHost(Host host, boolean always_notify, boolean also_acknowledged)
	{
		if (host.getParameter("state_type").equals("1") == false)
			return false;

		if (host.getParameter("current_state").equals("0") == true)
			return false;

		if (host.getParameter("active_checks_enabled").equals("0") == true && host.getParameter("passive_checks_enabled").equals("0") == true)
			return false;

		if (host.getParameter("scheduled_downtime_depth").equals("0") == false)
			return false;

		if (!always_notify && host.getParameter("notifications_enabled").equals("0") == true)
			return false;

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
	public boolean shouldIShowService(Service service, boolean always_notify, boolean also_acknowledged)
	{
		if (service.getParameter("state_type").equals("1") == false)
			return false;

		if (service.getParameter("current_state").equals("0") == true)
			return false;

		if (service.getParameter("active_checks_enabled").equals("0") == true && service.getParameter("passive_checks_enabled").equals("0") == true)
			return false;

		if (service.getParameter("scheduled_downtime_depth").equals("0") == false)
			return false;

		if (!always_notify && service.getParameter("notifications_enabled").equals("0") == true)
			return false;

		if (!also_acknowledged && service.getParameter("problem_has_been_acknowledged").equals("1") == true)
			return false;

		return true;
	}
}
