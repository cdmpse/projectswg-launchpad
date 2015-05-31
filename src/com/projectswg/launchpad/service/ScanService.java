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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.projectswg.launchpad.PSWG;
import com.projectswg.launchpad.model.SWG;
import com.projectswg.launchpad.model.Resource;

public class ScanService extends Service<Void>
{
	private static final String SWG_CLIENT = "SwgClient_r.exe";
	private static final String SWG_CLIENT_SETUP = "SwgClientSetup_r.exe";
	private static final String RESOURCE_LIST = "launcherS.dl.dat";
	
	public static final int SCAN_INIT = -1;
	public static final int SCAN_INTERRUPT = -2;

	public static final int RESOURCE_COUNT_LINE = 0;
	public static final int TIMESTAMP_LINE = 1;
	public static final int BEGIN_LINE = 2;
	
	private int scanType;
	private int scanStrictness;
	
	private SimpleStringProperty mainOut;
	
	private volatile boolean running;
	private volatile boolean stop;
	
    private Thread thread;
	private Manager manager;
	private String swgPath;
	private File file;
	
	
	public ScanService(Manager manager)
	{
    	this.manager = manager;
    	mainOut = new SimpleStringProperty("Initializing");
    	swgPath = null;
		file = null;

		Security.addProvider(new BouncyCastleProvider());
	}
	
	public void startSwgScan(int scanType, String swgPath)
	{
		this.swgPath = swgPath;
		this.scanType = scanType;
		this.scanStrictness = Manager.NORMAL_SCAN;

		mainOut.set("");
		
		reset();
		start();
	}
	
	public void startPswgScan(int scanType, int scanStrictness)
	{
		assert scanType == 0 || scanType == 1;
		assert scanStrictness == -1 || scanStrictness == 0;
		
		if (manager.getPswgFolder().getValue().equals(""))
			return;
		
		this.scanType = scanType;
		this.scanStrictness = scanStrictness;
		
		mainOut.set("");
		
		reset();
		start();
	}
	
	@Override
	protected Task<Void> createTask()
	{
		return new Task<Void>() {

			@Override
			protected Void call() throws Exception
			{
				updateProgress(0, 1);
				if (scanType == Manager.CHECK_SWG)
					swgScan();
				else
					pswgScan();
				return null;
			}
			
			private void swgScan()
			{
				for (int i = 0; i < SWG.FILES.length; i++) {
					if (isCancelled())
						return;
					String fileName = SWG.FILES[i];
					
					updateProgress(i, SWG.FILES.length * 100);
					//displayMessage.set(String.format("Scanning %s [ %s / %s ]", fileName, i + 1, Game.FILES.length));
					file = new File(swgPath + "\\" + fileName);
					if (!file.isFile()) {
						Platform.runLater(() -> {
							manager.scanSwgFinished(swgPath, false);
						});
						return;
					}
				}
				Platform.runLater(() -> {
					manager.scanSwgFinished(swgPath, true);
				});
			}
			
			private void pswgScan()
			{
				int downloadSizeRequired = 0;
				ArrayList<String> resourceList = null;
				ArrayList<Resource> resources = null;
				
				String resourceListPath = manager.getPswgFolder().getValue() + "/launcherS.dl.dat";
				
				PSWG.log("Reading encrypted resource list from local: " + resourceListPath);
				
				resourceList = readEncryptedResourceListFromLocal(resourceListPath);
				
				if (resourceList == null) {
					PSWG.log("Failed to read resource list from local");
					PSWG.log("Fetching resource list from remote");
					
					resourceList = getResourceListFromRemote();
					if (!writeEncryptedResourceList(resourceList)) {
						PSWG.log("Error writing resource list from remote");
						mainOut.set("An error occurred while updating");
						manager.scanPswgFinished(null, 0);
						running = false;
						return;
					}
				}
				
				resources = parseResourceList(resourceList);
				if (resources == null) {
					PSWG.log("Error parsing resource list");
					mainOut.set("An error occurred while updating");
					manager.scanPswgFinished(null, 0);
					running = false;
					return;
				}
				downloadSizeRequired = scanResources(resources);

				if (downloadSizeRequired > 0)
					mainOut.set(String.format("Download required: %s B", downloadSizeRequired));
				else if (downloadSizeRequired == -1)
					mainOut.set("Scan interrupted");
				else
					mainOut.set("All scans passed");

				manager.scanPswgFinished(resources, downloadSizeRequired);
			}
			
			private int scanResources(ArrayList<Resource> resources)
			{
				PSWG.log("Scanning resources");
				
				int total = 0;
				Resource resource;
				String resourceName;
				
				for (int i = 0; i < resources.size(); i++) {
					if (stop)
						return -1;
					
					updateProgress(i, resources.size() * 100);

					resource = resources.get(i);
					resourceName = resource.getName();
					
					PSWG.log(String.format("Scanning %s [ %s / %s ]", resourceName, i + 1, resources.size()));
					if (scanType == Manager.CHECK_HASH_PSWG)
						mainOut.set(String.format("Scanning %s [ %s / %s ]", resourceName, i + 1, resources.size()));

					boolean scanResult = false;
					if (resource.getStrictness() == Resource.DONT_SCAN) {
						scanResult = true;
						continue;
					}
					
					file = new File(manager.getPswgFolder().getValue() + "/" + resourceName);
					if (!file.isFile()) {
						PSWG.log("File not found: " + file.getAbsolutePath());
					} else {
						switch (scanType) {
						case Manager.CHECK_SWG:
						case Manager.CHECK_EXIST_PSWG:
							break;
			
						case Manager.CHECK_SIZE_PSWG:
							if (resourceName.equals(SWG_CLIENT) || resourceName.equals(SWG_CLIENT_SETUP))
								scanResult = checkResourceHash(file, resource);
							else
								scanResult = checkResourceSize(file, resource);
							break;
			
						case Manager.CHECK_HASH_PSWG:
							scanResult = checkResourceHash(file, resource);
						}
					}
					
					PSWG.log("Scan result: " + scanResult);
					if (scanResult)
						resource.setDlFlag(false);
					else
						total += resource.getSize();
				}
				return total;
			}
		};
	}
	
	private ArrayList<String> readPlainTextResourceListFromLocal(String filePath)
	{
		ArrayList<String> list = new ArrayList<String>();
		
		file = new File(filePath);
		if (file.length() == 0)
			return null;
		try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
			String line = null;
			while ((line = reader.readLine()) != null)
				list.add(line);
		} catch (IOException e) {
			return null;
		}
		return list;
	}
	
	private ArrayList<String> readEncryptedResourceListFromLocal(String filePath)
	{
		ArrayList<String> list = new ArrayList<String>();
		
		file = new File(filePath);
		if (!file.isFile())
			return null;
		if (file.length() == 0)
			return null;
		try {
			FileInputStream fis = new FileInputStream(file);
			byte[] avail = new byte[fis.available()];
			fis.read(avail);
			fis.close();
			
			String fullText = decrypt(avail, Manager.AES_SESSION_KEY);
			
			for (String l : fullText.split("\r\n")) {
				list.add(l);
				PSWG.log(String.format("Read encrypted text: %s", l));
			}
		} catch (IOException e) {
			PSWG.log(e.toString());
			return null;
		}
		return list;
	}
	
	private ArrayList<String> getResourceListFromRemote()
	{
		PSWG.log("Attempting to retrieve resource list from remote");
		mainOut.set("Fetching resource list");
		ArrayList<String> copy = new ArrayList<String>();
		try {
			URL url = new URL(Manager.PATCH_SERVER_FILES + RESOURCE_LIST);
			URLConnection urlConnection = url.openConnection();
			String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(Manager.HTTP_AUTH.getBytes());
			urlConnection.setRequestProperty("Authorization", basicAuth);
			//urlConnection.setRequestProperty("Accept-Charset",  "UTF-8");
			//urlConnection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
			BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				PSWG.log(String.format("RESOURCE LIST LINE: %s", line));
				copy.add(line);
			}
			in.close();
			
		} catch (MalformedURLException e) {
			PSWG.log(e.toString());
			return null;
		} catch (IOException e) {
			PSWG.log(e.toString());
			return null;
		}

		return copy;
	}

	private ArrayList<Resource> parseResourceList(ArrayList<String> resourceList)
	{
		PSWG.log("Parsing resource list");
		mainOut.set("Parsing resource list");
		
		ArrayList<Resource> resources = new ArrayList<Resource>();
		Pattern pattern = Pattern.compile("^([0-9]+)\\s+([0-9a-fA-F]{32})\\s+([0-9]+)\\s+(\\S+)$");
		Matcher matcher;
		
		int size = resourceList.size();
		if (size == 0) {
			PSWG.log("Parsed resource list was empty.");
			return null;
		}
		
		if (!Pattern.matches("^[0-9]+$", resourceList.get(RESOURCE_COUNT_LINE))) {
			PSWG.log("Error reading resource list: line count");
			return null;
		}
		
		if (!Pattern.matches("^[0-9]{10,}$", resourceList.get(TIMESTAMP_LINE))) {
			PSWG.log("Error reading resource list: timestamp line");
			return null;
		}
		
		if (!resourceList.get(BEGIN_LINE).equals("BEGIN")) {
			PSWG.log("Error reading resource list: begin line");
			return null;
		}
		
		if (!resourceList.get(size - 2).equals("END")) {
			PSWG.log("Error reading resource list: end line");
			return null;
		}
	
		int timestamp = Integer.parseInt(resourceList.get(TIMESTAMP_LINE));
		// check if already have this and check that isnt newer
		
		String line;
		for (int i = BEGIN_LINE + 1; i < resourceList.size() - 2; i++) {
			line = resourceList.get(i);
			matcher = pattern.matcher(line);
			if (!matcher.find()) {
				PSWG.log(String.format("Error reading resource list: %s, %s", i, line));
				return null;
			}
			
			Resource res = new Resource(
					matcher.group(4), // name
					Integer.parseInt(matcher.group(3)), // size
					matcher.group(2), // checksum
					Integer.parseInt(matcher.group(1))); // strictness
			
			resources.add(res);
		}
		int resourceCount = Integer.parseInt(resourceList.get(RESOURCE_COUNT_LINE));
		
		if (resources.size() != resourceCount) {
			PSWG.log(String.format("Resource count mismatch: %s, %s", resources.size(), resourceCount));
			return null;
		}
		
		return resources;
	}
	
	private byte[] encrypt(String text, String key)
	{
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding", "BC");
			SecretKeySpec sks = new SecretKeySpec(Manager.AES_SESSION_KEY.getBytes(), "AES");
			cipher.init(Cipher.ENCRYPT_MODE, sks, new IvParameterSpec(Manager.AES_SESSION_KEY.getBytes()));
			return cipher.doFinal(text.getBytes());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e1) {
			e1.printStackTrace();;
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private String decrypt(byte[] data, String key)
	{
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES/CBC/NoPadding", "BC");
			SecretKeySpec sks = new SecretKeySpec(key.getBytes(), "AES");
			cipher.init(Cipher.DECRYPT_MODE, sks, new IvParameterSpec(key.getBytes()));
			return new String(cipher.doFinal(data));
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e2) {
			e2.printStackTrace();
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e1) {
			e1.printStackTrace();
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private boolean writePlainTextResourceList(ArrayList<String> resourceList)
	{
		PSWG.log("Writting resource list as plain text");
		
		file = new File(manager.getPswgFolder().getValue() + "/launcherS.dl.dat");
		file.getParentFile().mkdirs();

		try {
			FileWriter writer = new FileWriter(file);
			for (String l : resourceList) {
				writer.write(l + "\n");
			}
			writer.close();
		} catch (IOException e) {
			PSWG.log(e.toString());
			return false;
		}
		return true;
	}
	
	private boolean writeEncryptedResourceList(ArrayList<String> resourceList)
	{
		PSWG.log("Writting encoded resource list");
		file = new File(manager.getPswgFolder().getValue() + "/launcherS.dl.dat");
		file.getParentFile().mkdirs();
		StringBuilder fullText = new StringBuilder();
		for (String l : resourceList) {
			fullText.append(l + "\n");
		}
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(encrypt(fullText.toString(), Manager.AES_SESSION_KEY));
			fos.close();
		} catch (IOException e) {
			PSWG.log(e.toString());
			return false;
		}
		return true;
	}
	
	private boolean checkResourceSize(File file, Resource resource)
	{
		if (resource.getSize() != file.length())
			return false;
		else
			return true;
	}
	
	private boolean checkResourceHash(File file, Resource resource)
	{
		if (resource.getSize() != file.length())
			return false;
		
		PSWG.log("Getting file checksum: " + file.getPath());
		mainOut.set("Checking: " + file.getName());
		
		String checksum = Manager.getFileChecksum(file);
		if (checksum == null) {
			PSWG.log(String.format("Error getting file checksum: %s", file.getName()));
			return false;
		}
		
		if (checksum.equals(resource.getChecksum()))
			return true;
		else
			return false;
	}
	
	public SimpleStringProperty getMainOut()
	{
		return mainOut;
	}
}
