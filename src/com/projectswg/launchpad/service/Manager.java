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
	public static final String PATCH_SERVER = "patch1.projectswg.com";
	public static final String PATCH_SERVER_FILES = "http://" + PATCH_SERVER + "/files/";
	public static final String PATCH_SERVER_LAUNCHER = "http://" + PATCH_SERVER + "/launcher/";
	public static final String HTTP_AUTH = "pswglaunch:wvQAxc5mGgF0";

	public static final String AES_SESSION_KEY = "eKgeg75J3pTBURgh";
	public static final String LOCALHOST = "127.0.0.1";
	
	public static final String PSWG_LOGIN_SERVER_NAME = "ProjectSWG";
	// server string format: [hostname],[port],[statusPort]
	public static final String PSWG_LOGIN_SERVER_STRING = "login1.projectswg.com,44453,44462";
	
	public static final String GAME_FEATURES = "34374193";
	
	public static final int LOGIN_SERVER_HOSTNAME = 0;
	public static final int LOGIN_SERVER_PORT = 1;
	public static final int LOGIN_SERVER_STATUSPORT = 2;

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
	private SimpleStringProperty mainOut;
	
	// install locations
	private SimpleStringProperty swgFolder;
	private SimpleStringProperty pswgFolder;
	
	// state
	private SimpleIntegerProperty state;
	
	// current login server
	private SimpleStringProperty loginServerHost;
	private SimpleStringProperty loginServerPlayPort;
	private SimpleStringProperty loginServerPingPort;
	
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
		
		loginServerHost = new SimpleStringProperty();
		loginServerPlayPort = new SimpleStringProperty();
		loginServerPingPort = new SimpleStringProperty();
		
		swgFolder = new SimpleStringProperty();
		pswgFolder = new SimpleStringProperty();
		
		state = new SimpleIntegerProperty(initialState);
		
		binary = new SimpleStringProperty();
		gameFeatures = new SimpleStringProperty();
		
		wineBinary = new SimpleStringProperty("");
		wineArguments = new SimpleStringProperty("");
		wineEnvironmentVariables = new SimpleStringProperty("");
		
		swgScanService = new SwgScanService(this);
		pswgScanService = new PswgScanService(this);
		updateService = new UpdateService(this);
		pingService = new PingService(this);
		
		addSwgScanServiceListeners();
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
		
		// pswg
		addPswgScanServiceListeners();
		pswgFolder.addListener((observable, oldValue, newValue) -> {
			if (swgFolder.getValue().equals(""))
				return;
			ProjectSWG.log(String.format("pswgFolder changed: %s -> %s", oldValue, newValue));
			ProjectSWG.PREFS.put("pswg_folder", newValue);
			if (newValue.equals(""))
				Platform.runLater(() -> {
					state.set(STATE_PSWG_SETUP_REQUIRED);
				});
			else
				quickScan();
		});
		
		// update service
		addUpdateServiceListeners();
		
		// launch configuration
		binary.set(ProjectSWG.PREFS.get("binary", ""));
		binary.addListener((observable, oldValue, newValue) -> {
			ProjectSWG.PREFS.put("binary", newValue);
		});
		
		gameFeatures.set(ProjectSWG.PREFS.get("game_features", ""));
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
		
		// Login server
		Preferences loginServersNode = ProjectSWG.PREFS.node("login_servers");
		if (loginServersNode.get(PSWG_LOGIN_SERVER_NAME, "").equals(""))
			loginServersNode.put(PSWG_LOGIN_SERVER_NAME, PSWG_LOGIN_SERVER_STRING);
		
		if (ProjectSWG.PREFS.get("login_server", "").equals(""))
			ProjectSWG.PREFS.put("login_server", PSWG_LOGIN_SERVER_NAME);
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
						binary.set(pswgFolder.getValue() + "/SwgClient_r.exe");
					if (gameFeatures.getValue().equals(""))
						gameFeatures.set(Manager.GAME_FEATURES);
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
		String pswgFolder = ProjectSWG.PREFS.get("pswg_folder", "");
		if (pswgFolder.equals(""))
			return;
		
		ProjectSWG.log(String.format("Launching game... Folder: %s, Host: %s, Port: %s",
				pswgFolder,
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
		String pswgFolder = ProjectSWG.PREFS.get("pswg_folder", "");
		if (pswgFolder.equals(""))
			return;
		File dir = new File(pswgFolder);
		if (!dir.exists())
			return;
		
		String[] processString = null;
		if (ProjectSWG.isWindows())
			processString = new String[] { pswgFolder + "/SwgClientSetup_r.exe" };
		else {
			if (wineBinary.getValue().equals(""))
				return;
			processString = new String[] {
					wineBinary.getValue(),
					pswgFolder + "/SwgClientSetup_r.exe"
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
			while ((read = is.read(buffer)) > 0) {
				md.update(buffer, 0, read);
			}
			is.close();
		} catch (NoSuchAlgorithmException e) {
			ProjectSWG.log(e.toString());
			return null;
		} catch (IOException e) {
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
			SecretKeySpec sks = new SecretKeySpec(Manager.AES_SESSION_KEY.getBytes(), "AES");
			cipher.init(Cipher.ENCRYPT_MODE, sks, new IvParameterSpec(Manager.AES_SESSION_KEY.getBytes()));
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

	public String[] getLoginServerValues(String loginServerName)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("login_servers");
		String serverString = loginServersNode.get(loginServerName, "");
		
		if (serverString.equals("")) {
			ProjectSWG.log("Login server doesn't exist: " + loginServerName);
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
			matcher = pattern.matcher(PSWG_LOGIN_SERVER_STRING);
			if (!matcher.find()) {
				ProjectSWG.log("Critical error: PSWG_LOGIN_SERVER_STRING -> " + PSWG_LOGIN_SERVER_STRING);
				return null;
			}
		}
	
		return new String[] {
			matcher.group(LOGIN_SERVER_HOSTNAME + 1),
			matcher.group(LOGIN_SERVER_PORT + 1),
			matcher.group(LOGIN_SERVER_STATUSPORT + 1)
		};
	}
	
	public void setLoginServerByName(String loginServerName)
	{
		ProjectSWG.PREFS.put("login_server", loginServerName);
		
		String[] loginServerValues = getLoginServerValues(loginServerName);
		
		ProjectSWG.log("Setting login server hostname: " + loginServerValues[LOGIN_SERVER_HOSTNAME]);
		ProjectSWG.log("Setting login server playport: " + loginServerValues[LOGIN_SERVER_PORT]);
		ProjectSWG.log("Setting login server pingport: " + loginServerValues[LOGIN_SERVER_STATUSPORT]);
		
		setLoginServerHostname(ProjectSWG.PREFS.get("login_server", ""), loginServerValues[LOGIN_SERVER_HOSTNAME]);
		setLoginServerPort(ProjectSWG.PREFS.get("login_server", ""), loginServerValues[LOGIN_SERVER_PORT]);
		setLoginServerStatusPort(ProjectSWG.PREFS.get("login_server", ""), loginServerValues[LOGIN_SERVER_STATUSPORT]);

		loginServerHost.set(loginServerValues[LOGIN_SERVER_HOSTNAME]);
		loginServerPlayPort.set(loginServerValues[LOGIN_SERVER_PORT]);
		loginServerPingPort.set(loginServerValues[LOGIN_SERVER_STATUSPORT]);
	}
	
	public void setLoginServerHostname(String loginServerName, String value)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("login_servers");
		String[] loginServerValues = getLoginServerValues(loginServerName);
		loginServersNode.put(loginServerName, String.format("%s,%s,%s",
			value,
			loginServerValues[LOGIN_SERVER_PORT],
			loginServerValues[LOGIN_SERVER_STATUSPORT]
		));
		loginServerHost.set(value);
	}
	
	public void setLoginServerPort(String loginServerName, String value)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("login_servers");
		String[] loginServerValues = getLoginServerValues(loginServerName);
		loginServersNode.put(loginServerName, String.format("%s,%s,%s",
			loginServerValues[LOGIN_SERVER_HOSTNAME],
			value,
			loginServerValues[LOGIN_SERVER_STATUSPORT]
		));
		loginServerPlayPort.set(value);
	}
	
	public void setLoginServerStatusPort(String loginServerName, String value)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(ProjectSWG.class).node("login_servers");
		String[] loginServerValues = getLoginServerValues(loginServerName);
		loginServersNode.put(loginServerName, String.format("%s,%s,%s",
			loginServerValues[LOGIN_SERVER_HOSTNAME],
			loginServerValues[LOGIN_SERVER_PORT],
			value
		));
		loginServerPingPort.set(value);
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
	
	public SimpleStringProperty getLoginServerHost()
	{
		return loginServerHost;
	}

	public SimpleStringProperty getLoginServerPlayPort()
	{
		return loginServerPlayPort;
	}

	public SimpleStringProperty getLoginServerPingPort()
	{
		return loginServerPingPort;
	}
	
	public SimpleStringProperty getPswgFolder()
	{
		return pswgFolder;
	}
	
	public SimpleStringProperty getSwgFolder()
	{
		return swgFolder;
	}
	
	public PingService getPingService()
	{
		return pingService;
	}
	
	public UpdateService getUpdateService()
	{
		return updateService;
	}
	
	public SimpleStringProperty getMainOut()
	{
		return mainOut;
	}

	public SimpleStringProperty getWineBinary()
	{
		return wineBinary;
	}

	public SimpleIntegerProperty getState()
	{
		return state;
	}

	public SwgScanService getSwgScanService()
	{
		return swgScanService;
	}

	public PswgScanService getPswgScanService()
	{
		return pswgScanService;
	}
	
	public ArrayList<Resource> getResources()
	{
		return resources;
	}
	
	public SimpleStringProperty getWineArguments()
	{
		return wineArguments;
	}
	
	public SimpleStringProperty getWineEnvironmentVariables()
	{
		return wineEnvironmentVariables;
	}

	public SimpleStringProperty getBinary() {
		return binary;
	}
	
	public SimpleStringProperty getGameFeatures() {
		return gameFeatures;
	}
}
