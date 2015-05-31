/*
 * 
 * This file is part of ProjectSWG Launchpad.
 *
 * ProjectSWG Launchpad is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * ProjectSWG Launchpad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with ProjectSWG Launchpad.  If not, see <http://www.gnu.org/licenses/>.      
 *
 */

package com.projectswg.launchpad.model;

public class Resource
{
	// strictness
	public static final int DELETE_FILE = -1; // nothing implemented
	public static final int DONT_SCAN = 0;
	public static final int SCAN_IF_REQUIRED = 1;
	public static final int ALWAYS_SCAN = 2;
	
	private String name;
	private String checksum;
	private int strictness;
	private int size;
	private boolean dlFlag;
	
	public Resource(String name, int size, String checksum, int strictness)
	{
		this.name = name;
		this.size = size;
		this.checksum = checksum;
		this.strictness = strictness;
		this.dlFlag = true;
	}

	@Override
	public String toString()
	{
		return strictness + " " + checksum + " " + size + " " + name;
	}

	public String getName()
	{
		return name;
	}

	public String getChecksum()
	{
		return checksum;
	}
	
	public int getSize()
	{
		return size;
	}
	
	public int getStrictness()
	{
		return strictness;
	}

	public boolean getDlFlag()
	{
		return dlFlag;
	}

	public void setDlFlag(boolean dlFlag)
	{
		this.dlFlag = dlFlag;
	}
}
