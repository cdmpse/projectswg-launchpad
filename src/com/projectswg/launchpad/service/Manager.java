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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.projectswg.launchpad.ProjectSWG;
import com.projectswg.launchpad.model.Resource;
import com.projectswg.launchpad.model.Instance;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.util.Pair;

public class Manager
{

	
	// update server string format: [files_url],[auth_user],[auth_pass],[filelist],[enc_key],[folder]
	// default pswg
	public static final String US_PSWG_K = "ProjectSWG";
	public static final String US_PSWG_V = "http://patch1.projectswg.com/files/" +
										   ",pswglaunch" +
										   ",wvQAxc5mGgF0" +
										   ",launcherS.dl.dat" +
										   ",eKgeg75J3pTBURgh" +
										   ",";
	
	// login server string format: [hostname],[playPort],[pingPort]
	// default pswg
	public static final String LS_PSWG_K = "ProjectSWG";
	public static final String LS_PSWG_V = "login1.projectswg.com" +
										   ",44453" +
										   ",44462";
	// default localhost
	public static final String LS_LOCALHOST_K = "localhost";
	public static final String LS_LOCALHOST_V = "127.0.0.1,44453,44462";
	
	public static final String GAME_FEATURES = "34374193";
	public static final String BINARY_DEFAULT = "SwgClient_r.exe";
	
	public static final int MAX_BUFFER_SIZE = 2048;
	public static final int RESOURCE_LIST_HASH = 0;
	
	public static final int CHECK_SWG = 0;
	public static final int CHECK_EXIST_PSWG = 1;
	public static final int CHECK_SIZE_PSWG = 2;
	public static final int CHECK_HASH_PSWG = 3;
	
	public static final int STRICT_SCAN = -1;
	public static final int NORMAL_SCAN = 0;
	
	public static final int MAX_INSTANCES = 5;
	
	public static final int STATE_INIT = 0;
	public static final int STATE_SWG_SETUP_REQUIRED = 1;
	public static final int STATE_SWG_SCANNING = 2;
	public static final int STATE_PSWG_SETUP_REQUIRED = 3;
	public static final int STATE_PSWG_SCAN_REQUIRED = 4;
	public static final int STATE_PSWG_SCANNING = 5;
	public static final int STATE_UPDATE_REQUIRED = 6;
	public static final int STATE_UPDATING = 7;
	public static final int STATE_WINE_REQUIRED = 8;
	public static final int STATE_PSWG_READY = 9;
	
	private ArrayList<Resource> resources;

	private SimpleIntegerProperty state;
	
	private SimpleStringProperty mainOut;
	
	private SimpleStringProperty swgFolder;
	// profile
	private SimpleStringProperty loginServer;
	private SimpleStringProperty loginServerHost;
	private SimpleStringProperty loginServerPlayPort;
	private SimpleStringProperty loginServerPingPort;
	
	private SimpleStringProperty updateServer;
	private SimpleStringProperty updateServerFileList;
	private SimpleStringProperty updateServerUrl;
	private SimpleStringProperty updateServerUsername;
	private SimpleStringProperty updateServerPassword;
	private SimpleStringProperty updateServerEncryptionKey;
	private SimpleStringProperty pswgFolder;
	
	// wine
	private SimpleStringProperty wineBinary;
	private SimpleStringProperty wineArguments;
	private SimpleStringProperty wineEnvironmentVariables;
	
	private SimpleStringProperty binary;
	private SimpleStringProperty gameFeatures;
	
	private SwgScanService swgScanService;
	private PswgScanService pswgScanService;
	private UpdateService updateService;
	private PingService pingService;
	private ProjectSWG pswg;
	
	
	public Manager(ProjectSWG pswg)
	{
		this(pswg, STATE_INIT);
	}
	
	public Manager(ProjectSWG pswg, int initialState)
	{
		this.pswg = pswg;
		resources = null;
		mainOut = new SimpleStringProperty();
		
		swgFolder = new SimpleStringProperty();
		swgFolder.addListener((observable, oldValue, newValue) -> {
			if (newValue == null)
				return;
			ProjectSWG.log(String.format("swgFolder changed: %s -> %s", oldValue, newValue));
			ProjectSWG.PREFS.put("swg_folder", newValue);
			
			if (newValue.equals(""))
				Platform.runLater(() -> {
					state.set(STATE_SWG_SETUP_REQUIRED);
				});
			else
				if (!swgScanService.isRunning()) {
					Platform.runLater(() -> {
						state.set(STATE_SWG_SCANNING);
					});
					swgScanService.reset();
					swgScanService.start();
				} else
					ProjectSWG.log("swg scan already running");
		});
		
		loginServer = new SimpleStringProperty();
		loginServerHost = new SimpleStringProperty();
		loginServerPlayPort = new SimpleStringProperty();
		loginServerPingPort = new SimpleStringProperty();
		
		updateServer = new SimpleStringProperty("");
		updateServerUrl = new SimpleStringProperty();
		updateServerUsername = new SimpleStringProperty();
		updateServerPassword = new SimpleStringProperty();
		updateServerFileList = new SimpleStringProperty();
		updateServerEncryptionKey = new SimpleStringProperty();
		
		pswgFolder = new SimpleStringProperty();
		pswgFolder.addListener((observable, oldValue, newValue) -> {
			if (swgFolder.getValue() == null || swgFolder.getValue().equals(""))
				return;
			ProjectSWG.log(String.format("pswgFolder changed: %s -> %s", oldValue, newValue));
			
			if (newValue.equals(""))
				Platform.runLater(() -> {
					state.set(STATE_PSWG_SETUP_REQUIRED);
					mainOut.set("PSWG Setup Required");
				});
			else
				quickScan();
		});
		
		state = new SimpleIntegerProperty(initialState);
		
		binary = new SimpleStringProperty(ProjectSWG.PREFS.get("binary", BINARY_DEFAULT));
		gameFeatures = new SimpleStringProperty(ProjectSWG.PREFS.get("game_features", GAME_FEATURES));
		
		wineBinary = new SimpleStringProperty("");
		wineArguments = new SimpleStringProperty("");
		wineEnvironmentVariables = new SimpleStringProperty("");
		
		swgScanService = new SwgScanService(this);
		pswgScanService = new PswgScanService(this);
		updateService = new UpdateService(this);
		pingService = new PingService(this);
		
		// scan service
		addSwgScanServiceListeners();
		addPswgScanServiceListeners();
		
		// update service
		addUpdateServiceListeners();
		
		// launch configuration
		binary.addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.put("binary", newValue);
		});
		// temp
		if (binary.getValue().contains("/") || binary.getValue().contains("\\"))
			binary.set(BINARY_DEFAULT);
		
		gameFeatures.addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.put("game_features", newValue);
		});
		
		// wine
		wineBinary.addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.put("wine_binary", newValue);
			if (!newValue.equals(""))
				if (state.intValue() == STATE_WINE_REQUIRED)
					Platform.runLater(() -> {
						state.set(STATE_PSWG_READY);
						mainOut.set("Ready");
					});
		});
		
		wineArguments.addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.put("wine_arguments", newValue);
		});
		
		wineEnvironmentVariables.addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.put("wine_environment_variables", newValue);
		});
		
		// update server
		Preferences updateServersNode = ProjectSWG.PREFS.node("update_servers");
		// add pswg update server if doesnt exist
		if (updateServersNode.get(US_PSWG_K, "").equals(""))
			updateServersNode.put(US_PSWG_K, US_PSWG_V);
		
		updateServer.addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(""))
				return;
			
			String[] updateServerValues = getUpdateServerValues(newValue);
			updateServerUrl.set(updateServerValues[0]);
			updateServerUsername.set(updateServerValues[1]);
			updateServerPassword.set(updateServerValues[2]);
			updateServerFileList.set(updateServerValues[3]);
			updateServerEncryptionKey.set(updateServerValues[4]);
			pswgFolder.set(updateServerValues[5]);
			
			ProjectSWG.PREFS.put("update_server", newValue);
		});
		
		if (ProjectSWG.PREFS.get("update_server", "").equals(""))
			ProjectSWG.PREFS.put("update_server", US_PSWG_K);
		
		// login server
		Preferences loginServersNode = ProjectSWG.PREFS.node("login_servers");
		// add pswg login server if doesnt exist
		if (loginServersNode.get(LS_PSWG_K, "").equals(""))
			loginServersNode.put(LS_PSWG_K, LS_PSWG_V);
		// add localhost if doesnt exist
		if (loginServersNode.get(LS_LOCALHOST_K, "").equals(""))
			loginServersNode.put(LS_LOCALHOST_K, LS_LOCALHOST_V);
		
		if (ProjectSWG.PREFS.get("login_server", "").equals(""))
			ProjectSWG.PREFS.put("login_server", LS_PSWG_K);
		
		loginServer.addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.put("login_server", newValue);
			
			String[] loginServerValues = getLoginServerValues(newValue);
			
			ProjectSWG.log("Setting login server hostname: " + loginServerValues[0]);
			ProjectSWG.log("Setting login server playport: " + loginServerValues[1]);
			ProjectSWG.log("Setting login server pingport: " + loginServerValues[2]);
			
			//setLoginServerHostname(ProjectSWG.PREFS.get("login_server", ""), loginServerValues[0]);
			//setLoginServerPort(ProjectSWG.PREFS.get("login_server", ""), loginServerValues[1]);
			//setLoginServerPingPort(ProjectSWG.PREFS.get("login_server", ""), loginServerValues[2]);

			loginServerHost.set(loginServerValues[0]);
			loginServerPlayPort.set(loginServerValues[1]);
			loginServerPingPort.set(loginServerValues[2]);
		});
		
		state.addListener((observable, oldValue, newValue) -> {
			ProjectSWG.log(String.format("state change: %s -> %s", oldValue, newValue));
		});
	}

	public void addSwgScanServiceListeners()
	{
		swgScanService.setOnRunning((e) -> {
			ProjectSWG.log("swgScanService: started");
			mainOut.bind(swgScanService.messageProperty());
			Platform.runLater(() -> {
				state.set(STATE_SWG_SCANNING);
			});
		});
		
		swgScanService.setOnCancelled((e) -> {
			mainOut.unbind();
			ProjectSWG.log("swgScanService: cancelled");
			Platform.runLater(() -> {
				state.set(STATE_SWG_SETUP_REQUIRED);
			});
		});
		
		swgScanService.setOnFailed((e) -> {
			mainOut.unbind();
			ProjectSWG.log("swgScanService: failed");
			Platform.runLater(() -> {
				state.set(STATE_SWG_SETUP_REQUIRED);
			});
		});
		
		swgScanService.setOnSucceeded((e) -> {
			mainOut.unbind();
			ProjectSWG.log("swgScanService succeeded: " + swgScanService.getValue());
			Platform.runLater(() -> {
				if (swgScanService.getValue())
					if (pswgFolder.getValue().equals(""))
						state.set(STATE_PSWG_SETUP_REQUIRED);
					else
						quickScan();
				else
					state.set(STATE_SWG_SETUP_REQUIRED);
			});
		});
	}
	
	public void addPswgScanServiceListeners()
	{
		pswgScanService.setOnRunning((e) -> {
			Platform.runLater(() -> {
				state.set(STATE_PSWG_SCANNING);
			});
			mainOut.bind(pswgScanService.messageProperty());
		});
		
		pswgScanService.setOnCancelled((e) -> {
			mainOut.unbind();
			Platform.runLater(() -> {
				mainOut.set("PSWG Scan Cancelled");
				state.set(STATE_PSWG_SCAN_REQUIRED);
			});
			ProjectSWG.log("PSWG Scan Cancelled");
		});
		
		pswgScanService.setOnFailed((e) -> {
			mainOut.unbind();
			Platform.runLater(() -> {
				mainOut.set("PSWG Scan Failed");
				state.set(STATE_PSWG_SCAN_REQUIRED);
			});
		});
		
		pswgScanService.setOnSucceeded((e) -> {
			mainOut.unbind();
			Pair<Double, ArrayList<Resource>> result = pswgScanService.getValue();
			
			if (result == null) {
				Platform.runLater(() -> {
					mainOut.set("PSWG Scan Error");
					state.set(STATE_PSWG_SCAN_REQUIRED);
				});
				return;
			}
			
			final double dlTotal = result.getKey();
			resources = result.getValue();
			
			Platform.runLater(() -> {
				if (dlTotal > 0) {
					state.set(STATE_UPDATE_REQUIRED);
					mainOut.set(String.format("%.2f MB Required", dlTotal / 1024 / 1024));
				} else {
					if (!ProjectSWG.isWindows())
						if (wineBinary.getValue().equals("")) {
							state.set(STATE_WINE_REQUIRED);
							mainOut.set("Wine Setup Required");
							return;
						}
					if (binary.getValue().equals(""))
						binary.set(BINARY_DEFAULT);
					if (gameFeatures.getValue().equals(""))
						gameFeatures.set(GAME_FEATURES);
					state.set(STATE_PSWG_READY);
					mainOut.set("Ready");
				}
			});
		});
	}
	
	public void addUpdateServiceListeners()
	{
		updateService.setOnRunning((e) -> {
			mainOut.bind(updateService.messageProperty());
			Platform.runLater(() -> {
				state.set(STATE_UPDATING);
			});
		});
		
		updateService.setOnCancelled((e) -> {
			mainOut.unbind();
			Platform.runLater(() -> {
				state.set(STATE_PSWG_SCAN_REQUIRED);
				mainOut.set("Update Cancelled");
			});
		});
		
		updateService.setOnFailed((e) -> {
			mainOut.unbind();
			ProjectSWG.log("Update failed: " + updateService.getException());
			Platform.runLater(() -> {
				state.set(STATE_PSWG_SCAN_REQUIRED);
				mainOut.set("Update Failed");
			});
		});
		
		updateService.setOnSucceeded((e) -> {
			mainOut.unbind();
			Platform.runLater(() -> {
				if (updateService.getValue()) {
					if (!ProjectSWG.isWindows())
						if (wineBinary.getValue().equals("")) {
							state.set(STATE_WINE_REQUIRED);
							mainOut.set("Wine Binary Not Set");
							return;
						}
					fullScan();
				} else {
					state.set(STATE_PSWG_SCAN_REQUIRED);
					mainOut.set("Update Failed");
				}
			});
		});
	}
	
	public void quickScan()
	{
		if (state.get() < STATE_SWG_SCANNING)
			return;
		if (pswgScanService.isRunning())
			return;
		pswgScanService.startScan(CHECK_SIZE_PSWG, NORMAL_SCAN);
	}
	
	public void fullScan()
	{
		if (state.get() < STATE_SWG_SCANNING)
			return;
		if (pswgScanService.isRunning())
			return;
		pswgScanService.startScan(CHECK_HASH_PSWG, NORMAL_SCAN);
	}
	
	public void updatePswg()
	{
		if (updateService.isRunning())
			return;
		updateService.reset();
		updateService.start();
	}
	
	public void requestStop()
	{
		if (pswgScanService.isRunning())
			pswgScanService.cancel();
		
		if (updateService.isRunning())
			updateService.cancel();
	}
	
	public void pingLoginServer()
	{
		if (!pingService.isRunning()) {
			pingService.reset();
			pingService.start();
		}
	}
	
	public void startSWG()
	{
		ObservableList<Instance> instances = pswg.getInstances();
		if (instances.size() > MAX_INSTANCES)
			return;

		if (pswgFolder.getValue().equals(""))
			return;
		
		ProjectSWG.log(String.format("Launching game... Folder: %s, Host: %s, Port: %s",
				pswgFolder.getValue(),
				loginServerHost.getValue(),
				loginServerPlayPort.getValue()));
		
		GameService gameService = new GameService(this);
		Instance instance = new Instance(gameService);
		instance.setLabel("ProjectSWG: " + pswg.getInstanceNumber());
		instances.add(instance);
		gameService.start();
	}
	
	public void launchGameSettings()
	{
		if (pswgFolder.getValue().equals(""))
			return;
		File dir = new File(pswgFolder.getValue());
		if (!dir.exists())
			return;
		
		String[] processString = null;
		if (ProjectSWG.isWindows())
			processString = new String[] { pswgFolder.getValue() + "/SwgClientSetup_r.exe" };
		else {
			if (wineBinary.getValue().equals(""))
				return;
			processString = new String[] {
					wineBinary.getValue(),
					pswgFolder.getValue() + "/SwgClientSetup_r.exe"
			};
		}
		
		try {
			ProcessBuilder pb = new ProcessBuilder(processString);
			pb.directory(dir);
			pb.start();
		} catch(IOException e1) {
			ProjectSWG.log(e1.toString());
		}
	}
	
	public static File getLocalResource(String path)
	{
		File file = new File(path);
		if (!file.exists())
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e1) {
				ProjectSWG.log("Error getting local resource: " + path);
				ProjectSWG.log(e1.toString());
				return null;
			}
		return file;
	}
	
	public static String getFileChecksum(File file)
	{
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("md5");
			InputStream is = Files.newInputStream(Paths.get(file.getPath()));
			byte[] buffer = new byte[MAX_BUFFER_SIZE];
			int read;
			while ((read = is.read(buffer)) > 0)
				md.update(buffer, 0, read);

			is.close();
		} catch (NoSuchAlgorithmException | IOException e) {
			ProjectSWG.log(e.toString());
			return null;
		}
		
		byte[] hash = md.digest();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hash.length; i++)
	        sb.append(Integer.toHexString((hash[i] & 0xff) | 0x100).substring(1, 3));

		return sb.toString();
	}
	
	public static byte[] encrypt(String text, String key)
	{
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			SecretKeySpec sks = new SecretKeySpec(key.getBytes(), "AES");
			cipher.init(Cipher.ENCRYPT_MODE, sks, new IvParameterSpec(key.getBytes()));
			return cipher.doFinal(text.getBytes());
		} catch (NoSuchAlgorithmException |
				NoSuchPaddingException |
				InvalidKeyException |
				InvalidAlgorithmParameterException |
				IllegalBlockSizeException |
				BadPaddingException e1) {
			ProjectSWG.log(e1.toString());
		}
		return null;
	}
	
	public static String decrypt(byte[] data, String key)
	{
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			SecretKeySpec sks = new SecretKeySpec(key.getBytes(), "AES");
			cipher.init(Cipher.DECRYPT_MODE, sks, new IvParameterSpec(key.getBytes()));
			return new String(cipher.doFinal(data));
		} catch (NoSuchAlgorithmException |
				NoSuchPaddingException |
				InvalidKeyException |
				InvalidAlgorithmParameterException |
				IllegalBlockSizeException |
				BadPaddingException e1) {
			ProjectSWG.log(e1.toString());
		}
		return null;
	}

	public String[] getUpdateServerValues(String updateServer)
	{
		Preferences updateServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("update_servers");
		String serverString = updateServersNode.get(updateServer, "");
		if (serverString.equals("")) {
			ProjectSWG.log("Update server doesn't exist: " + updateServer);
			return null;
		}
		
		Pattern pattern;
		pattern = Pattern.compile("^(.*),(.*),(.*),(.*),(.*),(.*)$");
		Matcher matcher;

		matcher = pattern.matcher(serverString);
		if (!matcher.find()) {
			ProjectSWG.log("updateServerString did not match pattern: " + serverString);
			ProjectSWG.log("Update server string corrupt. Creating new...");
			matcher = pattern.matcher(US_PSWG_V);
			if (!matcher.find()) {
				ProjectSWG.log("Update string error: " + US_PSWG_V);
				return null;
			}
		}
	
		System.out.println("url=" + matcher.group(1));
		System.out.println("username=" + matcher.group(2));
		System.out.println("password=" + matcher.group(3));
		System.out.println("filelist=" + matcher.group(4));
		System.out.println("key=" + matcher.group(5));
		System.out.println("pswg=" + matcher.group(6));
		
		return new String[] {
			matcher.group(1),
			matcher.group(2),
			matcher.group(3),
			matcher.group(4),
			matcher.group(5),
			matcher.group(6),
		};
	}
	
	private void setUpdateServerValues(String updateServerName, String[] values)
	{	
		Preferences updateServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("update_servers");
		updateServersNode.put(updateServerName, String.format("%s,%s,%s,%s,%s,%s",
			values[0],
			values[1],
			values[2],
			values[3],
			values[4],
			values[5]
		));
		
		System.out.println("url=" + values[0]);
		System.out.println("username=" + values[1]);
		System.out.println("password=" + values[2]);
		System.out.println("filelist=" + values[3]);
		System.out.println("key=" + values[4]);
		System.out.println("pswg=" + values[5]);
	}
	
	public void addUpdateServer(String name)
	{
		Preferences updateServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("update_servers");
		
		if (!updateServersNode.get(name, "").equals("")) {
			ProjectSWG.log("update server already exists");
			return;
		}
		updateServersNode.put(name, ",,,,,");
	}
	
	public void setUpdateServerUrl(String url)
	{
		String[] values = getUpdateServerValues(updateServer.getValue());
		values[0] = url;
		setUpdateServerValues(updateServer.getValue(), values);
		updateServerUrl.set(url);
		ProjectSWG.log("Setting update server url -> " + url);
	}
	
	public void setUpdateServerUsername(String username)
	{
		String[] values = getUpdateServerValues(updateServer.getValue());
		values[1] = username;
		setUpdateServerValues(updateServer.getValue(), values);
		updateServerUsername.set(username);
		ProjectSWG.log("Setting update server username -> " + username);
	}
	
	public void setUpdateServerPassword(String password)
	{
		String[] values = getUpdateServerValues(updateServer.getValue());
		values[2] = password;
		setUpdateServerValues(updateServer.getValue(), values);
		updateServerPassword.set(password);
		ProjectSWG.log("Setting update server password -> " + password);
	}
	
	public void setUpdateServerFileList(String fileList)
	{
		String[] values = getUpdateServerValues(updateServer.getValue());
		values[3] = fileList;
		setUpdateServerValues(updateServer.getValue(), values);
		updateServerFileList.set(fileList);
	}
	
	public void setUpdateServerEncryptionKey(String encryptionKey)
	{
		String[] values = getUpdateServerValues(updateServer.getValue());
		values[4] = encryptionKey;
		setUpdateServerValues(updateServer.getValue(), values);
		updateServerEncryptionKey.set(encryptionKey);
	}
	
	public void setUpdateServerInstallationFolder(String folder)
	{
		String[] values = getUpdateServerValues(updateServer.getValue());
		values[5] = folder;
		setUpdateServerValues(updateServer.getValue(), values);
		pswgFolder.set(folder);
	}
	
	public String[] getLoginServerValues(String loginServer)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("login_servers");
		String serverString = loginServersNode.get(loginServer, "");
		if (serverString.equals("")) {
			ProjectSWG.log("Login server doesn't exist: " + loginServer);
			return null;
		}
		
		Pattern pattern;
		// fix
		pattern = Pattern.compile("^([a-zA-Z0-9\\.-]*),([0-9]*),([0-9]*)$");
		Matcher matcher;

		matcher = pattern.matcher(serverString);
		if (!matcher.find()) {
			ProjectSWG.log("loginServerString did not match pattern: " + serverString);
			ProjectSWG.log("Login server string corrupt. Creating new...");
			matcher = pattern.matcher(LS_PSWG_V);
			if (!matcher.find()) {
				ProjectSWG.log("Critical error: PSWG_LOGIN_SERVER_STRING -> " + LS_PSWG_V);
				return null;
			}
		}
	
		return new String[] {
			matcher.group(1),
			matcher.group(2),
			matcher.group(3)
		};
	}
	
	/*
	public void setLoginServer(String loginServerName)
	{
		ProjectSWG.PREFS.put("login_server", loginServerName);
		loginServer.set(loginServerName);
		
		String[] loginServerValues = getLoginServerValues(loginServerName);
		
		ProjectSWG.log("Setting login server hostname: " + loginServerValues[0]);
		ProjectSWG.log("Setting login server playport: " + loginServerValues[1]);
		ProjectSWG.log("Setting login server pingport: " + loginServerValues[2]);
		
		//setLoginServerHostname(ProjectSWG.PREFS.get("login_server", ""), loginServerValues[0]);
		//setLoginServerPort(ProjectSWG.PREFS.get("login_server", ""), loginServerValues[1]);
		//setLoginServerPingPort(ProjectSWG.PREFS.get("login_server", ""), loginServerValues[2]);

		loginServerHost.set(loginServerValues[0]);
		loginServerPlayPort.set(loginServerValues[1]);
		loginServerPingPort.set(loginServerValues[2]);
	}*/
	
	public void setLoginServerHostname(String loginServerName, String value)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("login_servers");
		String[] loginServerValues = getLoginServerValues(loginServerName);
		loginServersNode.put(loginServerName, String.format("%s,%s,%s",
			value,
			loginServerValues[1],
			loginServerValues[2]
		));
	}
	
	public void setLoginServerPort(String loginServerName, String value)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("login_servers");
		String[] loginServerValues = getLoginServerValues(loginServerName);
		loginServersNode.put(loginServerName, String.format("%s,%s,%s",
			loginServerValues[0],
			value,
			loginServerValues[2]
		));
	}
	
	public void setLoginServerPingPort(String loginServerName, String value)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("login_servers");
		String[] loginServerValues = getLoginServerValues(loginServerName);
		loginServersNode.put(loginServerName, String.format("%s,%s,%s",
			loginServerValues[0],
			loginServerValues[1],
			value
		));
	}
	
	public void addLoginServer(String name)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("login_servers");
		
		if (!loginServersNode.get(name, "").equals("")) {
			ProjectSWG.log("Login server already exists");
			return;
		}
		loginServersNode.put(name, ",,");
	}
	
	public ArrayList<String> getLoginServers()
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("login_servers");
		ArrayList<String> serverList = new ArrayList<>();
		try {
			for (String name : loginServersNode.keys())
				serverList.add(name);
		} catch (BackingStoreException e1) {
			ProjectSWG.log(e1.toString());
		}
		return serverList;
	}

	// http://stackoverflow.com/questions/779519/delete-files-recursively-in-java/8685959#8685959
	public static void removeRecursive(Path path) throws IOException
	{
	    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
	        @Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
	        {
	            Files.delete(file);
	            return FileVisitResult.CONTINUE;
	        }

	        @Override
	        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
	        {
	            // try to delete the file anyway, even if its attributes
	            // could not be read, since delete-only access is
	            // theoretically possible
	            Files.delete(file);
	            return FileVisitResult.CONTINUE;
	        }

	        @Override
	        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
	        {
	            if (exc == null) {
	                Files.delete(dir);
	                return FileVisitResult.CONTINUE;
	            } else {
	                // directory iteration failed; propagate exception
	                throw exc;
	            }
	        }
	    });
	}
	
	public ArrayList<Resource> getResources() { return resources; }
	
	public PingService getPingService() { return pingService; }
	public UpdateService getUpdateService() { return updateService; }
	public SwgScanService getSwgScanService() { return swgScanService; }
	public PswgScanService getPswgScanService() { return pswgScanService; }
	
	public SimpleStringProperty getMainOut() { return mainOut; }
	public SimpleStringProperty getWineBinary() { return wineBinary; }
	public SimpleStringProperty getLoginServerHost() { return loginServerHost; }
	public SimpleStringProperty getLoginServerPlayPort() { return loginServerPlayPort; }
	public SimpleStringProperty getLoginServerPingPort() { return loginServerPingPort; }
	public SimpleStringProperty getPswgFolder() { return pswgFolder; }
	public SimpleStringProperty getSwgFolder() { return swgFolder; }
	public SimpleStringProperty getWineArguments() { return wineArguments; }
	public SimpleStringProperty getWineEnvironmentVariables() { return wineEnvironmentVariables; }
	public SimpleStringProperty getBinary() { return binary; }
	public SimpleStringProperty getGameFeatures() { return gameFeatures; }
	public SimpleStringProperty getLoginServer() { return loginServer; }
	public SimpleStringProperty getUpdateServer() { return updateServer; }
	public SimpleStringProperty getUpdateServerEncryptionKey() { return updateServerEncryptionKey; }
	public SimpleStringProperty getUpdateServerPassword() { return updateServerPassword; }
	public SimpleStringProperty getUpdateServerFileList() { return updateServerFileList; }
	public SimpleStringProperty getUpdateServerUrl() { return updateServerUrl; }
	public SimpleStringProperty getUpdateServerUsername() { return updateServerUsername; }
	
	public SimpleIntegerProperty getState() { return state; }
}
