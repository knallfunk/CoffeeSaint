/* Released under the GPL2. See license.txt for details. */
package com.vanheusden.nagios;

/**
 * Class Totals is only used to return counts.
 *
 * @author	Folkert van Heusden
 * @version	%I%, %G%
 * @since	0.1 
 */
public class Totals
{
	int nCritical, nWarning, nOk;
	int nUp, nDown, nUnreachable, nPending;

	public Totals(int nCritical, int nWarning, int nOk, int nUp, int nDown, int nUnreachable, int nPending)
	{
		this.nCritical = nCritical;
		this.nWarning = nWarning;
		this.nOk = nOk;
		this.nUp = nUp;
		this.nDown = nDown;
		this.nUnreachable = nUnreachable;
		this.nPending = nPending;
	}

	public int getNCritical()
	{
		return nCritical;
	}

	public int getNWarning()
	{
		return nWarning;
	}

	public int getNOk()
	{
		return nOk;
	}

	public int getNUp()
	{
		return nUp;
	}

	public int getNDown()
	{
		return nDown;
	}

	public int getNUnreachable()
	{
		return nUnreachable;
	}

	public int getNPending()
	{
		return nPending;
	}
}
