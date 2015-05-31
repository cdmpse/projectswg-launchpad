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
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javax.xml.bind.DatatypeConverter;
import com.projectswg.launchpad.PSWG;
import com.projectswg.launchpad.model.Resource;

public class UpdateService extends Service<Void>
{
	private float subTaskProgress;
	
	private SimpleStringProperty mainOut;
	
	private final Manager manager;
	private ArrayList<Resource> resources;
	
	public UpdateService(Manager manager)
	{
    	this.manager = manager;
    	this.subTaskProgress = 0f;
    	
    	mainOut = new SimpleStringProperty("");
	}
	
	public void start(ArrayList<Resource> resources)
	{
		this.resources = resources;
		start();
	}

	@Override
	protected Task<Void> createTask()
	{
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception
			{
				downloadResources(resources);
				return null;
			}
		};
	}
	
	private void downloadResources(ArrayList<Resource> resources)
	{
		ArrayList<Resource> downloadList = new ArrayList<Resource>();
		
		for (Resource resource : resources)
			if (resource.getDlFlag())
				downloadList.add(resource);
		
		for (int i = 0; i < downloadList.size(); i++) {
			subTaskProgress = (float)i / downloadList.size() * 100f;
			downloadResource(downloadList.get(i));
		}
		subTaskProgress = -1f;

		for (Resource resource : downloadList) {
			if (!resource.getDlFlag())
				PSWG.log(resource.getName() + " did not download successfully");
		}
	}
	
	private void downloadResource(Resource resource)
	{
		// set resource in prefs for resume
		
		String name = resource.getName();
		String path = manager.getPswgFolder().getValue() + name;
		File file = Manager.getLocalResource(path);
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
				fos.write(buffer, 0, bytesRead);
				bytesBuffered += bytesRead;
				runningTotal += bytesRead;
				if (bytesBuffered > 1024 * 1024) {
					bytesBuffered = 0;
					fos.flush();
				}
				
				subTaskProgress = (float)runningTotal / total;
			}
			fos.close();
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public SimpleStringProperty getMainOut() {
		return mainOut;
	}
}
