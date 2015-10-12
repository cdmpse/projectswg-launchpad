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
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import javax.xml.bind.DatatypeConverter;

import com.projectswg.launchpad.ProjectSWG;
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

			private String swgFolder = manager.getSwgFolder().getValue();
			private String pswgFolder = manager.getPswgFolder().getValue();
			
			@Override
			protected Boolean call() throws Exception
			{
		    	ArrayList<Resource> resources = manager.getResources();
				ArrayList<Resource> downloadList = new ArrayList<Resource>();
				
				for (Resource resource : resources)
					if (resource.getDlFlag())
						downloadList.add(resource);
				
				String resourceName;
				File copyFrom, copyTo;
				for (int i = 0; i < downloadList.size(); i++) {
					resourceName = downloadList.get(i).getName();
					// check if swg file
					if (Arrays.asList(SwgScanService.SWG_FILES.keySet()).contains(resourceName)) {
						updateMessage(String.format("Copying Resource %s of %s", i + 1, downloadList.size()));
						if (manager.getSwgFolder().getValue().equals(manager.getPswgFolder().getValue()))
							continue;
						copyFrom = new File(swgFolder + "/" + resourceName);
						copyTo = new File(pswgFolder + "/" + resourceName);
						if (copyTo.isFile())
							Files.delete(copyTo.toPath());
						Files.copy(copyFrom.toPath(), copyTo.toPath());
						ProjectSWG.log("Copied file: " + resourceName);
					} else {
						updateMessage(String.format("Downloading Resource %s of %s", i + 1, downloadList.size()));
						if (!downloadResource(downloadList.get(i))) {
							ProjectSWG.log(resourceName + " did not download successfully");
							return false;
						}
					}
				}
				ProjectSWG.log("UpdateService: end");
				return true;
			}
			
			private boolean downloadResource(Resource resource)
			{
				String name = resource.getName();
				String path = manager.getPswgFolder().getValue() + "/" + name;
				File file = Manager.getLocalResource(path);
				if (file == null)
					return false;
				
				if (file.length() >= resource.getSize())
					file.delete();
				
				long downloaded = file.length();
				
				try {
					long total = resource.getSize();
					
					URL url = new URL(manager.getUpdateServerUrl().getValue() + name);
					URLConnection urlConnection = url.openConnection();
					if (!manager.getUpdateServerUsername().getValue().equals("")) {
						String auth = manager.getUpdateServerUsername().getValue() + ":" + manager.getUpdateServerPassword().getValue();
						String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(auth.getBytes());
						urlConnection.setRequestProperty("Authorization", basicAuth);
					}
					String resumeDownlaoad = ProjectSWG.PREFS.get("resume_download", "");
					String[] resumeDownlaoadArray = resumeDownlaoad.split("::");

					if (resumeDownlaoadArray.length == 2)
						if (resumeDownlaoadArray[0].equals(name)) {
							ProjectSWG.log("Resuming download: " + name);
							String lastModified = resumeDownlaoadArray[1];
							urlConnection.setRequestProperty("If-Range", lastModified);
							urlConnection.setRequestProperty("Range", "bytes=" + downloaded + "-");
						};
					
					// resume
					String lastModified = urlConnection.getHeaderField("Last-Modified");
					ProjectSWG.PREFS.put("resume_download", name + "::" + lastModified);
					
					InputStream is = urlConnection.getInputStream();
					FileOutputStream fos = new FileOutputStream(file, true);
					
					byte[] buffer = new byte[Manager.MAX_BUFFER_SIZE];
					int bytesRead = 0, bytesBuffered = 0;
	
					ProjectSWG.log(String.format("Resuming file from: %s -> %s", name, downloaded));
					while ((bytesRead = is.read(buffer)) > -1) {
						if (isCancelled()) {
							updateProgress(-1, 0);
							fos.close();
							return false;
						}
						fos.write(buffer, 0, bytesRead);
						bytesBuffered += bytesRead;
						downloaded += bytesRead;
						updateProgress(downloaded, total);
						if (bytesBuffered > 1024 * 1024) {
							bytesBuffered = 0;
							fos.flush();
						}
					}
					
					fos.close();
					is.close();
					resource.setDlFlag(false);
					return true;
					
				} catch (IOException e1) {
					ProjectSWG.log("Update Error: " + e1.toString());
					return false;
					
				} finally {
					updateProgress(-1, 0);
				}
			}
		};
	}
}
