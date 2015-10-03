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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.util.Pair;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.projectswg.launchpad.ProjectSWG;
import com.projectswg.launchpad.model.Resource;

public class PswgScanService extends Service<Pair<Double, ArrayList<Resource>>>
{
	private static final String SWG_CLIENT = "SwgClient_r.exe";
	private static final String SWG_CLIENT_SETUP = "SwgClientSetup_r.exe";
	
	public static final int SCAN_INIT = -1;
	public static final int SCAN_INTERRUPT = -2;

	public static final int RESOURCE_COUNT_LINE = 0;
	public static final int TIMESTAMP_LINE = 1;
	public static final int BEGIN_LINE = 2;
	
	private int scanType;
	private int scanStrictness;
	
	private final Manager manager;
	private File file;
	
	
	public PswgScanService(Manager manager)
	{
		this.manager = manager;
		file = null;

		Security.addProvider(new BouncyCastleProvider());
	}
	
	public void startScan(int scanType, int scanStrictness)
	{
		if (isRunning())
			return;
		if (manager.getPswgFolder().getValue().equals(""))
			return;
		
		this.scanType = scanType;
		this.scanStrictness = scanStrictness;
		
		reset();
		start();
	}
	
	@Override
	protected Task<Pair<Double, ArrayList<Resource>>> createTask()
	{
		return new Task<Pair<Double, ArrayList<Resource>>>() {

			@Override
			protected Pair<Double, ArrayList<Resource>> call() throws Exception
			{
				updateProgress(0, 1);

				ArrayList<String> resourceList = null;
				ArrayList<Resource> resources = null;
				
				String resourceListPath = manager.getPswgFolder().getValue() + "/launcherS.dl.dat";
				ProjectSWG.log("Reading encrypted resource list from local: " + resourceListPath);
				resourceList = readEncryptedResourceListFromLocal(resourceListPath);
				
				if (resourceList == null) {
					ProjectSWG.log("Failed to read resource list from local");
					updateMessage("Fetching Resource List");
					
					resourceList = getResourceListFromRemote();
					if (!writeEncryptedResourceList(resourceList)) {
						ProjectSWG.log("Error writing resource");
						return null;
					}
				}
				
				resources = parseResourceList(resourceList);
				if (resources == null) {
					ProjectSWG.log("Error parsing resource list");
					return null;
				}
				double total = scanResources(resources);
				
				return new Pair<>(total, resources);
			}
			
			private double scanResources(ArrayList<Resource> resources)
			{
				ProjectSWG.log("Scanning resources");
				
				Resource resource;
				String resourceName;
				double total = 0;
				
				for (int i = 0; i < resources.size(); i++) {
					
					if (isCancelled()) {
						updateMessage("PSWG Scan Cancelled");
						return -1;
					}
					
					updateProgress(i, resources.size() * 100);

					resource = resources.get(i);
					resourceName = resource.getName();
					
					ProjectSWG.log(String.format("Scanning Resource %s of %s : %s, %s",
							i + 1,
							resources.size(),
							resourceName,
							resource.getSize()));
					
					updateMessage(String.format("Scanning Resource %s of %s", i + 1, resources.size()));

					boolean scanResult = false;
					if (resource.getStrictness() == Resource.DONT_SCAN) {
						scanResult = true;
						continue;
					}
					
					file = new File(manager.getPswgFolder().getValue() + "/" + resourceName);
					if (!file.isFile()) {
						ProjectSWG.log("File not found: " + file.getAbsolutePath());
					} else {
						switch (scanType) {
						case Manager.CHECK_EXIST_PSWG:
							break;
			
						case Manager.CHECK_SIZE_PSWG:
							if (resourceName.equals(SWG_CLIENT) || resourceName.equals(SWG_CLIENT_SETUP))
								scanResult = checkResourceHash(file, resource);
							else
								scanResult = resource.getSize() == file.length();
							break;
			
						case Manager.CHECK_HASH_PSWG:
							scanResult = checkResourceHash(file, resource);
						}
						ProjectSWG.log(scanResult ? "OK" : "Fail : " + file.length());
					}
					resource.setDlFlag(!scanResult);
					if (!scanResult)
						total += resource.getSize();
				}
				return total;
			}
			
			/*
			 * update server files first
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
			*/
			
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
					
					String fullText = decrypt(avail, manager.getUpdateServerEncryptionKey().getValue());
					
					for (String l : fullText.split("\r\n"))
						list.add(l);

					return list;
					
				} catch (IOException e1) {
					ProjectSWG.log(e1.toString());
					return null;
				}
			}
			
			private ArrayList<String> getResourceListFromRemote()
			{
				ProjectSWG.log("Fetching resource list from remote...");
				ArrayList<String> copy = new ArrayList<String>();
				try {
					URL url = new URL(manager.getUpdateServerUrl().getValue() + manager.getUpdateServerFileList().getValue());
					URLConnection urlConnection = url.openConnection();
					if (!manager.getUpdateServerUser().getValue().equals("")) {
						String auth = manager.getUpdateServerUser() + ":" + manager.getUpdateServerPassword();
						String basicAuth = "Basic " + DatatypeConverter.printBase64Binary(auth.getBytes());
						urlConnection.setRequestProperty("Authorization", basicAuth);
					}
					BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
					String line;
					for (int i = 0; (line = in.readLine()) != null; i++) {
						ProjectSWG.log(String.format("(Resource List) %s: %s", i, line));
						copy.add(line);
					}
					in.close();
					return copy;
				} catch (IOException e1) {
					ProjectSWG.log(e1.toString());
					return null;
				}
			}

			private ArrayList<Resource> parseResourceList(ArrayList<String> resourceList)
			{
				updateMessage("Parsing Resource List");
				
				ArrayList<Resource> resources = new ArrayList<Resource>();
				Pattern pattern = Pattern.compile("^([0-9]+)\\s+([0-9a-fA-F]{32})\\s+([0-9]+)\\s+(\\S+)$");
				Matcher matcher;
				
				int size = resourceList.size();
				if (size == 0) {
					ProjectSWG.log("Error: size -> 0");
					return null;
				}
				
				if (!Pattern.matches("^[0-9]+$", resourceList.get(RESOURCE_COUNT_LINE))) {
					ProjectSWG.log("Error: linecount -> " + resourceList.get(RESOURCE_COUNT_LINE));
					return null;
				}
				
				if (!Pattern.matches("^[0-9]{10,}$", resourceList.get(TIMESTAMP_LINE))) {
					ProjectSWG.log("Error: timestamp -> " + resourceList.get(TIMESTAMP_LINE));
					return null;
				}
				
				if (!resourceList.get(BEGIN_LINE).equals("BEGIN")) {
					ProjectSWG.log("Error: begin line -> " + resourceList.get(BEGIN_LINE));
					return null;
				}
				
				if (!resourceList.get(size - 1).equals("END")) {
					ProjectSWG.log("Error: end -> " + resourceList.get(size - 2).equals("END"));
					return null;
				}
			
				//int timestamp = Integer.parseInt(resourceList.get(TIMESTAMP_LINE));
				
				String line;
				for (int i = BEGIN_LINE + 1; i < resourceList.size() - 1; i++) {
					line = resourceList.get(i);
					matcher = pattern.matcher(line);
					if (!matcher.find()) {
						ProjectSWG.log(String.format("Error reading resource list: %s, %s", i, line));
						return null;
					}
					
					Resource res = new Resource(
							matcher.group(4), 						// name
							Integer.parseInt(matcher.group(3)), 	// size
							matcher.group(2), 						// checksum
							Integer.parseInt(matcher.group(1))); 	// strictness
					
					resources.add(res);
				}
				int resourceCount = Integer.parseInt(resourceList.get(RESOURCE_COUNT_LINE));
				
				if (resources.size() != resourceCount) {
					ProjectSWG.log(String.format("Resource count mismatch: %s <> %s", resources.size(), resourceCount));
					return null;
				}
				
				return resources;
			}
			
			private byte[] encrypt(String text, String key)
			{
				try {
					Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
					SecretKeySpec sks = new SecretKeySpec(manager.getUpdateServerEncryptionKey().getValue().getBytes(), "AES");
					cipher.init(Cipher.ENCRYPT_MODE, sks, new IvParameterSpec(manager.getUpdateServerEncryptionKey().getValue().getBytes()));
					return cipher.doFinal(text.getBytes());
				} catch (NoSuchAlgorithmException | 
						 NoSuchPaddingException |
						 InvalidKeyException |
						 InvalidAlgorithmParameterException |
						 IllegalBlockSizeException |
						 BadPaddingException |
						 NoSuchProviderException e1) {
					ProjectSWG.log(e1.toString());
					return null;
				}
			}
			
			private String decrypt(byte[] data, String key)
			{
				try {
					Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
					SecretKeySpec sks = new SecretKeySpec(key.getBytes(), "AES");
					cipher.init(Cipher.DECRYPT_MODE, sks, new IvParameterSpec(key.getBytes()));
					return new String(cipher.doFinal(data));
				} catch (NoSuchAlgorithmException | 
						 NoSuchPaddingException |
						 InvalidKeyException |
						 InvalidAlgorithmParameterException |
						 IllegalBlockSizeException|
						 BadPaddingException |
						 NoSuchProviderException e1) {
					ProjectSWG.log(e1.toString());
					return null;
				}
			}
			
			/*
			 * update server files first
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
			*/
			
			private boolean writeEncryptedResourceList(ArrayList<String> resourceList)
			{
				ProjectSWG.log("Writing encoded resource list");
				file = new File(manager.getPswgFolder().getValue() + "/launcherS.dl.dat");
				file.getParentFile().mkdirs();
				StringBuilder fullText = new StringBuilder();
				for (String l : resourceList)
					fullText.append(l + "\r\n");
				
				try {
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(encrypt(fullText.toString(), manager.getUpdateServerEncryptionKey().getValue()));
					fos.close();
				} catch (IOException e1) {
					ProjectSWG.log(e1.toString());
					return false;
				}
				return true;
			}
			
			private boolean checkResourceHash(File file, Resource resource)
			{
				if (resource.getSize() != file.length())
					return false;
				
				String checksum = Manager.getFileChecksum(file);
				if (checksum == null)
					return false;
				
				return checksum.equals(resource.getChecksum());
			}
		};
	}
}
