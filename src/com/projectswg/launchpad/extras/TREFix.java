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

package com.projectswg.launchpad.extras;

import java.io.File;
import java.io.IOException;

import javafx.scene.control.Button;

import com.projectswg.launchpad.ProjectSWG;
import com.projectswg.launchpad.controller.MainController;

public class TREFix implements ExtraModule
{
	public static final String LABEL = "TREFix";
	public static final String TREFIX_LOCATION = "/TREFix.exe";

	private MainController main;
	
	
	public TREFix(MainController main)
	{
		this.main = main;
	}
	
	public void launch()
	{
		String pswgFolder = main.getManager().getPswgFolder().getValue();
		
		if (pswgFolder == null || pswgFolder.equals("")) {
			ProjectSWG.log("pswgFolder not set");
			return;
		}
		
		File dir = new File(pswgFolder);
		if (!dir.exists()) {
			ProjectSWG.log("pswgFolder not found");
			return;
		}
		
		try {
			ProcessBuilder pb = new ProcessBuilder(pswgFolder + TREFIX_LOCATION);
			pb.directory(dir);
			pb.start();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Button createButton() {
		Button extraButton = new Button(LABEL);
		extraButton.setOnAction((e) -> {
			launch();
		});
		return extraButton;
	}
}
