/* Released under the GPL2. See license.txt for details. */
import java.util.*;

/**
 * Class Host contains all host- and servicechecks results for one host.
 *
 * @author	Folkert van Heusden
 * @version	%I%, %G%
 * @since	0.1 
 */
public class Host
{
	String hostName;

	List<ParameterEntry> hostEntries = new ArrayList<ParameterEntry>();

	List<Service> services = new ArrayList<Service>();

	/**
	 * @param hostName	Name of the host this object describes.
	 */
	public Host(String hostName)
	{
		this.hostName = hostName;
	}

	/**
	 * @return	The hostname set in the constructor.
	 */
	public String getHostName()
	{
		return hostName;
	}

	/**
	 * Find a service or allocate a new one if it doesn't exist.
	 *
	 * @param serviceName	Name of the service to look for.
	 * @return		Reference to newly created or found object.
	 */
	Service addAndOrFindService(String serviceName)
	{
		for(Service currentService : services)
		{
			if (currentService.getServiceName().equals(serviceName))
				return currentService;
		}

		Service newService = new Service(serviceName);

		services.add(newService);

		return newService;
	}

	/**
	 * @return	Returns a list of all services checked for this host.
	 */
	public List<Service> getServices()
	{
		return services;
	}

	/**
	 * Each host has a couple of parameters as well. These can be obtained using this method.
	 *
	 * @return	A List of 'ParameterEntry'-objects defining each parameter of this host
	 */
	public List<ParameterEntry> getParameters()
	{
		return hostEntries;
	}

	/**
	 * Get the value of a host-parameter.
	 *
	 * @return	A string with the value. Indeed, also values are returned as a string.
	 */
	public String getParameter(String parameter)
	{
		for(ParameterEntry parameterEntry : hostEntries)
		{
			if (parameterEntry.getParameterName().equals(parameter))
				return parameterEntry.getParameterValue();
		}

		return null;
	}

	/**
	 * Add a host-parameter.
	 *
	 * @param hostParameterName	The name of the parameter. See nagios documentation for a list.
	 * @param hostParameterValue	String with the value.
	 * @return 			Parameter value.
	 */
	public ParameterEntry addParameter(String hostParameterName, String hostParameterValue)
	{
		for(ParameterEntry currentHostEntry : hostEntries)
		{
			if (currentHostEntry.getParameterName().equals(hostParameterName))
				return currentHostEntry;
		}

		ParameterEntry currentHostEntry = new ParameterEntry(hostParameterName, hostParameterValue);
		hostEntries.add(currentHostEntry);

		return currentHostEntry;
	}

	/**
	 * Add a parameter/value pair to a service of this host.
	 *
	 * @param serviceName		Service to which this parameter/value pair applies.
	 * @param serviceParameterName	Name of the parameter.
	 * @param serviceParameterValue	Parameter value.
	 * @return			Altered service
	 */
	public Service addServiceEntry(String serviceName, String serviceParameterName, String serviceParameterValue)
	{
		ParameterEntry parameterEntry = new ParameterEntry(serviceParameterName, serviceParameterValue);

		for(Service curService : services)
		{
			if (curService.getServiceName().equals(serviceName))
			{
				curService.addParameter(parameterEntry);
				return curService;
			}
		}

		Service newService = new Service(serviceName);
		newService.addParameter(parameterEntry);
		services.add(newService);

		return newService;
	}
}
