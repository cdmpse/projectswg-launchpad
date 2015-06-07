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

import com.projectswg.launchpad.PSWG;
import com.projectswg.launchpad.model.Resource;
import com.projectswg.launchpad.model.SWG;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;

public class Manager
{
	//public static final int STATE_INIT = 0;
	//public static final int STATE_SETUP = 1;
	//public static final int STATE_SCAN_REQUIRED = 2;
	//public static final int STATE_SCANNING = 3;
	//public static final int STATE_UPDATE_REQUIRED = 4;
	//public static final int STATE_UPDATING = 5;
	//public static final int STATE_PLAY = 6;

	public static final String PATCH_SERVER = "patch1.projectswg.com";
	public static final String PATCH_SERVER_FILES = "http://" + PATCH_SERVER + "/files/";
	public static final String PATCH_SERVER_LAUNCHER = "http://" + PATCH_SERVER + "/launcher/";
	public static final String HTTP_AUTH = "pswglaunch:wvQAxc5mGgF0";

	public static final String AES_SESSION_KEY = "eKgeg75J3pTBURgh";
	public static final String LOCALHOST = "127.0.0.1";
	
	public static final String PSWG_LOGIN_SERVER_NAME = "ProjectSWG";
	// server string format: [hostname],[port],[statusPort]
	public static final String PSWG_LOGIN_SERVER_STRING = "login1.projectswg.com,44453,44462";
	
	public static final int LOGIN_SERVER_HOSTNAME = 0;
	public static final int LOGIN_SERVER_PORT = 1;
	public static final int LOGIN_SERVER_STATUSPORT = 2;

	//private static final int MAX_BUFFER_SIZE = 8192;
	public static final int MAX_BUFFER_SIZE = 2048;
	public static final int RESOURCE_LIST_HASH = 0;
	
	public static final int CHECK_SWG = 0;
	public static final int CHECK_EXIST_PSWG = 1;
	public static final int CHECK_SIZE_PSWG = 2;
	public static final int CHECK_HASH_PSWG = 3;
	
	public static final int STRICT_SCAN = -1;
	public static final int NORMAL_SCAN = 0;
	
	public static final int MAX_INSTANCES = 5;
	
	//private long busySince;
	
	private SimpleDoubleProperty dlSizeRequired;
	
	//private boolean busy;
	private boolean showWine;
	
	private volatile ArrayList<Resource> resources;
	private volatile ObservableList<SWG> instances;
	
	// launcher state
	//private volatile SimpleIntegerProperty state;
	private volatile SimpleStringProperty mainOut;
	
	// install locations
	private SimpleStringProperty swgFolder;
	private SimpleStringProperty pswgFolder;
	
	// scan results
	private SimpleBooleanProperty swgReady;
	private SimpleBooleanProperty pswgReady;
	
	// current login server
	private SimpleStringProperty loginServerHost;
	private SimpleStringProperty loginServerPlayPort;
	private SimpleStringProperty loginServerPingPort;
	
	// wine
	private SimpleStringProperty wineBinary;
	private SimpleStringProperty wineArguments;
	private SimpleStringProperty wineEnvironmentVariables;
	
	private SwgScanService swgScanService;
	private PswgScanService pswgScanService;
	private UpdateService updateService;
	private PingService pingService;
	
	private ChangeListener<String> serviceListener;

	
	public Manager()
	{
		resources = null;
		//busy = false;
		dlSizeRequired = new SimpleDoubleProperty();
		//busySince = 0;
		
		//state = new SimpleIntegerProperty(STATE_INIT);
		mainOut = new SimpleStringProperty();
		
		loginServerHost = new SimpleStringProperty();
		loginServerPlayPort = new SimpleStringProperty();
		loginServerPingPort = new SimpleStringProperty();
		
		swgFolder = new SimpleStringProperty("");
		pswgFolder = new SimpleStringProperty("");
		
		swgReady = new SimpleBooleanProperty(false);
		pswgReady = new SimpleBooleanProperty(false);
		
		wineBinary = new SimpleStringProperty("");
		wineArguments = new SimpleStringProperty("");
		wineEnvironmentVariables = new SimpleStringProperty("");
		
		swgScanService = new SwgScanService(this);
		pswgScanService = new PswgScanService(this);
		updateService = new UpdateService(this);
		pingService = new PingService(this);
		
		// track output
		serviceListener = new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				mainOut.set(newValue);
			}
		};

		// game instances
		instances = FXCollections.observableArrayList();
		
		// Wine
		if (PSWG.isWindows())
			showWine = false;
		else
			showWine = true;
		
		// installation folders
		swgFolder.addListener((observable, oldValue, newValue) -> {
			PSWG.log(String.format("swgFolder changed: %s -> %s", oldValue, newValue));
			PSWG.PREFS.put("swg_folder", newValue);
			if (newValue.equals("")) {
				//state.set(STATE_SETUP);
				swgReady.set(false);
			} else {
				swgScanService.getMainOut().addListener(serviceListener);
				//state.set(STATE_SCANNING);
				swgScanService.startScan(newValue);

				if (pswgFolder.getValue().equals("")) {
					//state.set(STATE_SETUP);
				} else {
					scanPswg(true);
				}
			}
		});
		
		pswgFolder.addListener((observable, oldValue, newValue) -> {
			PSWG.log(String.format("pswgFolder changed: %s -> %s", oldValue, newValue));
			if (swgFolder.getValue().equals(""))
				return;
			PSWG.PREFS.put("pswg_folder", newValue);
			if (newValue.equals("")) {
				//state.set(STATE_SETUP);
				Platform.runLater(() -> {
					pswgReady.set(false);
				});
				
			} else {
				scanPswg(true);
			}
		});
		
		swgScanService.setOnRunning((e) -> {
			swgReady.set(false);
		});
		
		swgScanService.runningProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue)
				return;
			swgScanService.getMainOut().removeListener(serviceListener);
			Platform.runLater(() -> {
				swgFolder.set(swgScanService.getValue());
			});
			//busy = false;
		});
		
		pswgScanService.setOnRunning((e) -> {
			pswgReady.set(false);
		});
		
		pswgScanService.runningProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue)
				return;
	
			pswgScanService.getMainOut().removeListener(serviceListener);
			resources = pswgScanService.getValue();
			
			if (resources == null) {
				PSWG.log("Scan failed");
				Platform.runLater(() -> {
					dlSizeRequired.set(-1);
					mainOut.set("Scan failed");
				});
				//busy = false;
				return;
			}
			
			int total = 0;
			for (Resource resource : resources) {
				if (!resource.getDlFlag())
					total += resource.getSize();
			}
			
			final int dlTotal = total;
			if (dlTotal > 0) {
				Platform.runLater(() -> {
					dlSizeRequired.set(dlTotal);
					mainOut.set(String.format("Required download size: %s B", dlTotal));
				});
			} else
				pswgReady.set(true);

			PSWG.log("PSWG scan finished: " + dlTotal);
			//busy = false;
		});
		
		// wine
		wineBinary.addListener((observable, oldValue, newValue) -> {
			PSWG.PREFS.put("wine_binary", newValue);
		});
		
		wineArguments.addListener((observable, oldValue, newValue) -> {
			PSWG.PREFS.put("wine_arguments", newValue);
		});
		
		wineEnvironmentVariables.addListener((observable, oldValue, newValue) -> {
			PSWG.PREFS.put("wine_environment_variables", newValue);
		});
		
		// Login server	
		Preferences loginServersNode = PSWG.PREFS.node("login_servers");
		if (loginServersNode.get(PSWG_LOGIN_SERVER_NAME, "").equals(""))
			loginServersNode.put(PSWG_LOGIN_SERVER_NAME, PSWG_LOGIN_SERVER_STRING);
		
		String loginServer = PSWG.PREFS.get("login_server", "");
		if (loginServer.equals(""))
			PSWG.PREFS.put("login_server", PSWG_LOGIN_SERVER_NAME);
	}
	
	public SimpleDoubleProperty getDlSizeRequired() {
		return dlSizeRequired;
	}

	public void loadPrefs()
	{
		// paths
		pswgFolder.set(PSWG.PREFS.get("pswg_folder", ""));
		swgFolder.set(PSWG.PREFS.get("swg_folder", ""));
		
		// wine
		wineBinary.set(PSWG.PREFS.get("wine_binary",  ""));
		wineArguments.set(PSWG.PREFS.get("wine_arguments", ""));
		wineEnvironmentVariables.set(PSWG.PREFS.get("wine_environment_variables", ""));
		
		// animation
	}
	
	//public SimpleIntegerProperty getState()
	//{
	//	return state;
	//}
	
	public void scanSwg(String swgPath)
	{
		//if (!busy) {
			pswgScanService.getMainOut().addListener(serviceListener);
			PSWG.log("Scanning Star Wars Galaxies installation");
			swgScanService.startScan(swgPath);
			//busySince = System.currentTimeMillis();
		//	busy = true;
			
		//} else {
		//	long now = System.currentTimeMillis();
		//	long diff = now - busySince;
		//	PSWG.log("manager is busy for :" + diff);
		//}
	}
	
	public void scanSwgFinished(String swgPath, boolean result)
	{
		pswgScanService.getMainOut().removeListener(serviceListener);
		if (result) {
			PSWG.log("Star Wars Galaxies installation verified");
			Platform.runLater(() -> {
				swgFolder.set(swgPath);
			});
		} else {
			PSWG.log("Star Wars Galaxies scan failed");
			Platform.runLater(() -> {
				swgFolder.set("");
			});
		}
		//busy = false;
	}
	
	public void scanPswg(boolean quick)
	{
		// should be checked before this
		if (pswgFolder.getValue().equals("")) {
			//state.set(STATE_SETUP);
			return;
		}
		
		//if (!busy) {
			pswgScanService.getMainOut().addListener(serviceListener);
			//state.set(STATE_SCANNING);
			if (quick)
				pswgScanService.startScan(CHECK_SIZE_PSWG, NORMAL_SCAN);
			else
				pswgScanService.startScan(CHECK_HASH_PSWG, NORMAL_SCAN);
			
		//	busySince = System.currentTimeMillis();
		//	busy = true;
		//} else {
		//	long now = System.currentTimeMillis();
		//	long diff = now - busySince;
		//	PSWG.log("Manager busy: " + diff);
			//scanPswgFinished(null, 0);
		//}
	}
	
	public void scanPswgFinished(ArrayList<Resource> resources, long scanResult)
	{
		pswgScanService.getMainOut().removeListener(serviceListener);
		
		this.resources = resources;
		dlSizeRequired.set(scanResult);
		
		if (resources == null) {
			PSWG.log("Scan returned null resources");
			//Platform.runLater(() -> {
			//	state.set(STATE_SCAN_REQUIRED);
			//});
			//busy = false;
			return;
		}

		if (scanResult > 0)
			Platform.runLater(() -> {
				//state.set(STATE_UPDATE_REQUIRED);
				mainOut.set(String.format("Required download size: %s B", scanResult));
			});
		else if (scanResult == -1)
			Platform.runLater(() -> {
				//state.set(STATE_SCAN_REQUIRED);
				mainOut.set("Scan interrupted");
			});
		else
			Platform.runLater(() -> {
				//state.set(STATE_PLAY);
				mainOut.set("All scans passed");
			});

		PSWG.log("PSWG scan finished: " + dlSizeRequired);
		//busy = false;
	}
	
	public void gameFinished(int index)
	{
		
	}
	
	public void installPswg()
	{
		
	}
	
	public void updatePswg()
	{
		//if (!busy) {
			updateService.getMainOut().addListener(serviceListener);
			updateService.startUpdate(resources);
			//busySince = System.currentTimeMillis();
			//busy = true;
	
		//} else {
		//	long now = System.currentTimeMillis();
		//	long diff = now - busySince;
		//	System.out.println("manager is busy for: " + diff);
		//}
	}
	
	public void updatePswgFinished()
	{
		updateService.getMainOut().removeListener(serviceListener);
	}
	
	public void requestStop()
	{
		if (pswgScanService.isRunning())
			pswgScanService.cancel();
		
		if (updateService.isRunning())
			updateService.cancel();
	}
	
	public ArrayList<Resource> getResources()
	{
		return resources;
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
		if (instances.size() > MAX_INSTANCES) {
			PSWG.log("Too many games running");
			return;
		}
		String pswgFolder = PSWG.PREFS.get("pswg_folder", "");
		if (pswgFolder.equals("")) {
			PSWG.log("pswgFolder was empty");
			return;
		}
		
		PSWG.log(String.format("Launching game: Folder: %s, Host: %s, Port: %s",
				pswgFolder,
				loginServerHost.getValue(),
				loginServerPlayPort.getValue()));
		
		GameService gameService = new GameService(this);
		SWG swg = new SWG(gameService);
		instances.add(swg);
		
		gameService.start();
	}
	
	public void launchGameSettings()
	{
		PSWG.log("Launching game settings...");
		
		String pswgFolder = PSWG.PREFS.get("pswg_folder", "");
		if (pswgFolder.equals("")) {
			PSWG.log("pswg_folder not set");
			return;
		}
		
		File dir = new File(pswgFolder);
		if (!dir.exists()) {
			PSWG.log("pswg_folder not found");
			return;
		}
		
		try {
			ProcessBuilder pb = new ProcessBuilder(pswgFolder + "/SwgClientSetup_r.exe");
			pb.directory(dir);
			Process p = pb.start();
		} catch(IOException e) {
			e.printStackTrace();
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
				PSWG.log("Error getting local resource: " + path);
				return null;
			}
		
		return file;
	}
	
	//public boolean isBusy()
	//{
	//	return busy;
	//}
	
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
			PSWG.log(e.toString());
			return null;
		} catch (IOException e) {
			PSWG.log(e.toString());
			return null;
		}
		
		byte[] hash = md.digest();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hash.length; i++) {
			//sb.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1));
	        sb.append(Integer.toHexString((hash[i] & 0xff) | 0x100).substring(1, 3));
	    }

		return sb.toString();
	}
	
	public static byte[] encrypt(String text, String key)
	{
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			//Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
			//SecretKeySpec sks = new SecretKeySpec(Manager.AES_SESSION_KEY.getBytes("UTF-8"), "AES");
			SecretKeySpec sks = new SecretKeySpec(Manager.AES_SESSION_KEY.getBytes(), "AES");
			//cipher.init(Cipher.ENCRYPT_MODE, sks, new IvParameterSpec(Manager.AES_SESSION_KEY.getBytes("UTF-8")));
			cipher.init(Cipher.ENCRYPT_MODE, sks, new IvParameterSpec(Manager.AES_SESSION_KEY.getBytes()));
			//return cipher.doFinal(text.getBytes("UTF-8"));
			return cipher.doFinal(text.getBytes());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e1) {
			e1.printStackTrace();
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String decrypt(byte[] data, String key)
	{
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			//cipher = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
			SecretKeySpec sks = new SecretKeySpec(key.getBytes(), "AES");
			cipher.init(Cipher.DECRYPT_MODE, sks, new IvParameterSpec(key.getBytes()));
			return new String(cipher.doFinal(data));
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e2) {
			e2.printStackTrace();
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e1) {
			e1.printStackTrace();
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void removeLoginServer(String loginServerName)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(PSWG.class).node("login_servers");
		loginServersNode.remove(loginServerName);
	}
	
	public String[] getLoginServerValues(String loginServerName)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(PSWG.class).node("login_servers");
		String serverString = loginServersNode.get(loginServerName, "");
		
		if (serverString.equals("")) {
			PSWG.log("Login server doesn't exist: " + loginServerName);
			if (loginServerName.equals(PSWG_LOGIN_SERVER_NAME)) {

			} else {
				return null;
			}
		}
		//("^([0-9]+)\\s+([0-9a-fA-F]{32})\\s+([0-9]+)\\s+(\\S+)$");
		
		Pattern pattern;
		pattern = Pattern.compile("^([a-zA-Z0-9\\.-]*),([0-9]*),([0-9]*)$");
		Matcher matcher;

		matcher = pattern.matcher(serverString);
		if (!matcher.find()) {
			PSWG.log("loginServerString did not match pattern: " + serverString);
			PSWG.log("Login server string corrupt. Creating new...");
			matcher = pattern.matcher(PSWG_LOGIN_SERVER_STRING);
			if (!matcher.find()) {
				PSWG.log("Critical error: PSWG_LOGIN_SERVER_STRING -> " + PSWG_LOGIN_SERVER_STRING);
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
		PSWG.PREFS.put("login_server", loginServerName);
		
		String[] loginServerValues = getLoginServerValues(loginServerName);
		
		PSWG.log("Setting login server hostname: " + loginServerValues[LOGIN_SERVER_HOSTNAME]);
		PSWG.log("Setting login server playport: " + loginServerValues[LOGIN_SERVER_PORT]);
		PSWG.log("Setting login server pingport: " + loginServerValues[LOGIN_SERVER_STATUSPORT]);
		
		setLoginServerHostname(PSWG.PREFS.get("login_server", ""), loginServerValues[LOGIN_SERVER_HOSTNAME]);
		setLoginServerPort(PSWG.PREFS.get("login_server", ""), loginServerValues[LOGIN_SERVER_PORT]);
		setLoginServerStatusPort(PSWG.PREFS.get("login_server", ""), loginServerValues[LOGIN_SERVER_STATUSPORT]);

		loginServerHost.set(loginServerValues[LOGIN_SERVER_HOSTNAME]);
		loginServerPlayPort.set(loginServerValues[LOGIN_SERVER_PORT]);
		loginServerPingPort.set(loginServerValues[LOGIN_SERVER_STATUSPORT]);
	}
	
	public void setLoginServerHostname(String loginServerName, String value)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(PSWG.class).node("login_servers");
		String[] loginServerValues = getLoginServerValues(loginServerName);
		loginServersNode.put(loginServerName, String.format("%s,%s,%s",
			value,
			loginServerValues[LOGIN_SERVER_PORT],
			loginServerValues[LOGIN_SERVER_STATUSPORT]
		));
	}
	
	public void setLoginServerPort(String loginServerName, String value)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(PSWG.class).node("login_servers");
		String[] loginServerValues = getLoginServerValues(loginServerName);
		loginServersNode.put(loginServerName, String.format("%s,%s,%s",
			loginServerValues[LOGIN_SERVER_HOSTNAME],
			value,
			loginServerValues[LOGIN_SERVER_STATUSPORT]
		));
	}
	
	public void setLoginServerStatusPort(String loginServerName, String value)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(PSWG.class).node("login_servers");
		String[] loginServerValues = getLoginServerValues(loginServerName);
		loginServersNode.put(loginServerName, String.format("%s,%s,%s",
			loginServerValues[LOGIN_SERVER_HOSTNAME],
			loginServerValues[LOGIN_SERVER_PORT],
			value
		));
	}
	
	public void addLoginServer(String name)
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(PSWG.class).node("login_servers");
		
		if (!loginServersNode.get(name, "").equals("")) {
			PSWG.log("Login server already exists");
			return;
		}
		loginServersNode.put(name, ",,");
	}
	
	public ArrayList<String> getLoginServers()
	{
		Preferences loginServersNode = Preferences.userNodeForPackage(PSWG.class).node("login_servers");
		ArrayList<String> serverList = new ArrayList<>();
		try {
			for (String name : loginServersNode.keys()) {
				serverList.add(name);
			}
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		return serverList;
	}

	// http://stackoverflow.com/questions/779519/delete-files-recursively-in-java/8685959#8685959
	public static void removeRecursive(Path path) throws IOException
	{
	    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
	        @Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	            Files.delete(file);
	            return FileVisitResult.CONTINUE;
	        }

	        @Override
	        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
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
	
	public SimpleStringProperty getLoginServerHost() {
		return loginServerHost;
	}

	public SimpleStringProperty getLoginServerPlayPort() {
		return loginServerPlayPort;
	}

	public SimpleStringProperty getLoginServerPingPort() {
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
	
	public ObservableList<SWG> getInstances()
	{
		return instances;
	}
	
	public boolean getShowWine()
	{
		return showWine;
	}

	public SimpleStringProperty getWineBinary() {
		return wineBinary;
	}

	public void setWineBinary(SimpleStringProperty wineBinary) {
		this.wineBinary = wineBinary;
	}
	
	public SimpleBooleanProperty getSwgReady()
	{
		return swgReady;
	}
	
	public SimpleBooleanProperty getPswgReady()
	{
		return pswgReady;
	}
}
