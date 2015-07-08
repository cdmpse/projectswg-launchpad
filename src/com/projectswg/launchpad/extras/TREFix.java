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

	private MainController mainController;
	
	
	public TREFix(MainController mainController)
	{
		this.mainController = mainController;
	}
	
	public void launch()
	{
		String pswgFolder = mainController.getManager().getPswgFolder().getValue();
		
		if (pswgFolder == null || pswgFolder.equals("")) {
			ProjectSWG.log("pswgFolder not set");
			return;
		}
		
		File dir = new File(pswgFolder);
		if (!dir.exists()) {
			ProjectSWG.log("pswgFolder not found");
			return;
		}
		
		String[] processString = null;
		if (ProjectSWG.isWindows()) {
			processString = new String[] {
					pswgFolder + TREFIX_LOCATION
			};
		} else {
			if (mainController.getManager().getWineBinary().getValue().equals("")) {
				ProjectSWG.log("Wine binary path not set");
				return;
			}
			processString = new String[] {
					mainController.getManager().getWineBinary().getValue(),
					pswgFolder + TREFIX_LOCATION
			};
		}
		
		try {
			ProcessBuilder pb = new ProcessBuilder(processString);
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
