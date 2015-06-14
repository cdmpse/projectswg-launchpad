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

package com.projectswg.launchpad.service;

import java.io.File;

import com.projectswg.launchpad.ProjectSWG;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class SwgScanService extends Service<Boolean>
{
	public static final String[] FILES = {

		//"SwgClientSetup_r.exe",
		"BugTool.exe",

		"bottom.tre",
		"data_animation_00.tre",
		"data_music_00.tre",
		"data_other_00.tre",
		"data_sample_00.tre",
		"data_sample_01.tre",
		"data_sample_02.tre",
		"data_sample_03.tre",
		"data_sample_04.tre",
		"data_skeletal_mesh_00.tre",
		"data_skeletal_mesh_01.tre",
		"data_static_mesh_00.tre",
		"data_static_mesh_01.tre",
		"data_texture_00.tre",
		"data_texture_01.tre",
		"data_texture_02.tre",
		"data_texture_03.tre",
		"data_texture_04.tre",
		"data_texture_05.tre",
		"data_texture_06.tre",
		"data_texture_07.tre",
		//"default_patch.tre",

		"dbghelp.dll",
		//"dpvs.dll",
		//"Mss32.dll",
		"qt-mt305.dll"
	};
	
	private final Manager manager;
	private File file;
	
	
	public SwgScanService(Manager manager)
	{
    	this.manager = manager;
		file = null;
	}
	
	@Override
	protected Task<Boolean> createTask()
	{
		return new Task<Boolean>() {

			@Override
			protected Boolean call() throws Exception
			{
				updateProgress(0, 1);

				for (int i = 0; i < FILES.length; i++) {
					if (isCancelled())
						return false;
					String fileName = FILES[i];
					
					updateProgress(i, FILES.length * 100);
					updateMessage("Scanning: " + fileName);
					
					file = new File(manager.getSwgFolder().getValue() + "/" + fileName);
					if (!file.isFile()) {
						ProjectSWG.log("SWG file not found: " + manager.getSwgFolder().getValue() + "/" + fileName);
						updateMessage("SWG scan failed");
						return false;
					}
				}
				updateMessage("SWG scan passed");
				return true;
			}
		};
	}
}
