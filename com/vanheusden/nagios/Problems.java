/* Released under the GPL2. See license.txt for details. */
package com.vanheusden.nagios;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Problems
{
	static void addProblem(List<Pattern> prioPatterns, List<Problem> problems, List<Problem> lessImportant, Host host, Service service, String state)
	{
		boolean important = false;

		if (prioPatterns != null)
		{
			String msg = host.getHostName() + ": " + (service != null ? service.getServiceName() : "");

			for(Pattern currentPattern : prioPatterns)
			{
				if (currentPattern.matcher(msg).matches())
				{
					important = true;
					System.out.println("important: " + msg);
					break;
				}
			}
		}

		if (important)
			problems.add(new Problem(host, service, state));
		else
			lessImportant.add(new Problem(host, service, state));
	}

	public static void collectProblems(JavNag javNag, List<Pattern> prioPatterns, List<Problem> problems, boolean always_notify, boolean also_acknowledged)
	{
		List<Problem> lessImportant = new ArrayList<Problem>();

		for(Host currentHost: javNag.getListOfHosts())
		{
			assert currentHost != null;

			if (javNag.shouldIShowHost(currentHost, always_notify, also_acknowledged))
			{
				String useState = null;
				String state = currentHost.getParameter("current_state");

				if (state.equals("0")) /* UP = OK */
					useState = "0";
				else if (state.equals("1") || state.equals("2")) /* DOWN & UNREACHABLE = CRITICAL */
					useState = "2";
				else /* all other states (including 'pending' ("3")) are WARNING */
					useState = "1";

				addProblem(prioPatterns, problems, lessImportant, currentHost, null, useState);
			}
			else
			{
				for(Service currentService : currentHost.getServices())
				{
					assert currentService != null;
					if (javNag.shouldIShowService(currentService, always_notify, also_acknowledged))
					{
						String state = currentService.getParameter("current_state");

						addProblem(prioPatterns, problems, lessImportant, currentHost, currentService, state);
					}
				}
			}
		}

		for(Problem currentLessImportant : lessImportant)
		{
			problems.add(currentLessImportant);
		}
	}
}
