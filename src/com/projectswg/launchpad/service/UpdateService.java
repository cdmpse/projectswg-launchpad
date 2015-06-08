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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import javax.xml.bind.DatatypeConverter;

import com.projectswg.launchpad.PSWG;
import com.projectswg.launchpad.model.Resource;

public class UpdateService extends Service<Boolean>
{
	private final Manager manager;
	
	public UpdateService(Manager manager)
	{
    	this.manager = manager;
	}

	@Override
	protected Task<Boolean> createTask()
	{
		return new Task<Boolean>() {

			@Override
			protected Boolean call() throws Exception
			{
				PSWG.log("UpdateService: start");
				
		    	ArrayList<Resource> resources = manager.getResources();
				ArrayList<Resource> downloadList = new ArrayList<Resource>();
				
				for (Resource resource : resources)
					if (resource.getDlFlag())
						downloadList.add(resource);
				
				for (int i = 0; i < downloadList.size(); i++) {
					updateProgress(-1, 0);
					updateMessage(String.format("Downloading resources: %s / %s", i + 1, downloadList.size()));
					if (!downloadResource(downloadList.get(i))) {
						PSWG.log(downloadList.get(i).getName() + " did not download successfully");
						return false;
					}
				}

				PSWG.log("UpdateService: end");
				return true;
			}
			
			private boolean downloadResource(Resource resource)
			{
				// set resource in prefs for resume
				String name = resource.getName();
				String path = manager.getPswgFolder().getValue() + "/" + name;
				File file = Manager.getLocalResource(path);
				if (file == null)
					return false;
				
				InputStream is = null;
				FileOutputStream fos = null;
				URL url;

				try {
					int total = resource.getSize();

					url = new URL(Manager.PATCH_SERVER_FILES + name);
					URLConnection urlConnection = url.openConnection();
					String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(Manager.HTTP_AUTH.getBytes());
					urlConnection.setRequestProperty("Authorization", basicAuth);
					
					is = urlConnection.getInputStream();
					fos = new FileOutputStream(file);

					byte[] buffer = new byte[Manager.MAX_BUFFER_SIZE];
					int bytesRead = 0, bytesBuffered = 0;
					int runningTotal = 0;
					
					while ((bytesRead = is.read(buffer)) > -1) {
						
						if (isCancelled()) {
							updateProgress(-1, 0);
							fos.close();
							return false;
						}
						
						fos.write(buffer, 0, bytesRead);
						bytesBuffered += bytesRead;
						runningTotal += bytesRead;
						updateProgress(runningTotal, total);
						
						if (bytesBuffered > 1024 * 1024) {
							bytesBuffered = 0;
							fos.flush();
						}
					}
					
					fos.close();
					resource.setDlFlag(false);
					return true;
				} catch (MalformedURLException e) {
					e.printStackTrace();
					return false;
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}
		};
	}
}
