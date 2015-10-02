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
import java.util.HashMap;
import java.util.Map;

import com.projectswg.launchpad.ProjectSWG;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class SwgScanService extends Service<Boolean>
{
	public static final HashMap<String, String> SWG_FILES;
	static {
		SWG_FILES = new HashMap<>();
		SWG_FILES.put("bottom.tre", "16205884");
		SWG_FILES.put("data_animation_00.tre", "58966791");
		SWG_FILES.put("data_music_00.tre", "77322894");
		SWG_FILES.put("data_other_00.tre", "46546173");
		SWG_FILES.put("data_sample_00.tre", "104636589");
		SWG_FILES.put("data_sample_01.tre", "104848649");
		SWG_FILES.put("data_sample_02.tre", "104806646");
		SWG_FILES.put("data_sample_03.tre", "104479178");
		SWG_FILES.put("data_sample_04.tre", "33887496");
		SWG_FILES.put("data_skeletal_mesh_00.tre", "104259690");
		SWG_FILES.put("data_skeletal_mesh_01.tre", "42575909");
		SWG_FILES.put("data_static_mesh_00.tre", "104681322");
		SWG_FILES.put("data_static_mesh_01.tre", "94719985");
		SWG_FILES.put("data_texture_00.tre", "104816351");
		SWG_FILES.put("data_texture_01.tre", "104831553");
		SWG_FILES.put("data_texture_02.tre", "104923301");
		SWG_FILES.put("data_texture_03.tre", "104659874");
		SWG_FILES.put("data_texture_04.tre", "104887073");
		SWG_FILES.put("data_texture_05.tre", "104820008");
		SWG_FILES.put("data_texture_06.tre", "104773001");
		SWG_FILES.put("data_texture_07.tre", "66226334");
	}
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

				int counter = 1;
				for (Map.Entry<String, String> e : SWG_FILES.entrySet()) {
					String fileName = e.getKey();
					updateProgress(counter, SWG_FILES.size() * 100);
					updateMessage("Scanning " + fileName);
					file = new File(manager.getSwgFolder().getValue() + "/" + fileName);
					if (!file.isFile()) {
						ProjectSWG.log("SWG file not found: " + manager.getSwgFolder().getValue() + "/" + fileName);
						updateMessage("SWG Not Found");
						return false;
					}
					if (file.length() != Long.parseLong(e.getValue())) {
						ProjectSWG.log("File size does not match: " + e.getKey());
						updateMessage("SWG Not Found");
						return false;
					}
				}

				updateMessage("SWG Scan Passed");
				return true;
			}
		};
	}
}
