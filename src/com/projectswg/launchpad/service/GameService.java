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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.prefs.Preferences;

import com.projectswg.launchpad.PSWG;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class GameService extends Service<Void>
{	
	private SimpleStringProperty mainOut;
	private Process process;
	private final Manager manager;
	
	
	public GameService(Manager manager)
	{
		this.manager = manager;
		mainOut = new SimpleStringProperty();
	}
	
	public Process getProcess()
	{
		return process;
	}
	
	public SimpleStringProperty getMainOut()
	{
		return mainOut;
	}

	@Override
	protected Task<Void> createTask()
	{
		return new Task<Void>() {
			
			@Override
			protected void cancelled()
			{
				process.destroy();
			}
			
			@Override
			protected Void call() throws Exception
			{
				String pswgFolder = manager.getPswgFolder().getValue();
				String host = manager.getLoginServerHost().getValue();
				String port = manager.getLoginServerPlayPort().getValue();
				
				String[] processString = {
					pswgFolder + "\\SwgClient_r.exe",
					"--",
					"-s",
					"Station",
					"subscriptionFeatures=1",
					"gameFeatures=34374193",
					"-s",
					"ClientGame",
					"loginServerPort0=" + port,
					"loginServerAddress0=" + (PSWG.PREFS.getBoolean("localhost", false) ? Manager.LOCALHOST : host)
				};
			
				for (String s : processString)
					PSWG.log("Starting SWG: " + s);

				try {
					final ProcessBuilder processBuilder = new ProcessBuilder(processString);
				
					File dir = new File(pswgFolder);
					if (!dir.isDirectory()) {
						PSWG.log("Directory doesn't exist: " + pswgFolder);
						return null;
					}
					
					processBuilder.directory(dir);
					process = processBuilder.start();

					InputStreamReader isr = new InputStreamReader(process.getInputStream());
					BufferedReader stdInput = new BufferedReader(isr);
					
					Date now = new Date();
					String sinp = null;
					while ((sinp = stdInput.readLine()) != null)
						mainOut.set(now.toString() + ": " + sinp);
				
				} catch(IOException e) {
					e.printStackTrace();
				}
				return null;
			}
		};
	}
}
