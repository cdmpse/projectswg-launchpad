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

public class SwgScanService extends Service<String>
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
	private Manager manager;
	private String swgPath;
	private File file;
	
	
	public SwgScanService(Manager manager)
	{
    	this.manager = manager;
    	mainOut = new SimpleStringProperty("");
    	swgPath = null;
		file = null;
	}
	
	public void startScan(String swgPath)
	{
		this.swgPath = swgPath;
		mainOut.set("");
	
		if (isRunning()) {
			PSWG.log("SWG scan already running");
			return;
		}
		
		reset();
		start();
	}
	
	@Override
	protected Task<String> createTask()
	{
		return new Task<String>() {

			@Override
			protected String call() throws Exception
			{
				updateProgress(0, 1);

				for (int i = 0; i < SWG.FILES.length; i++) {
					if (isCancelled())
						return null;
					String fileName = SWG.FILES[i];
					
					updateProgress(i, SWG.FILES.length * 100);
					
					//String.format("Scanning %s [ %s / %s ]", fileName, i + 1, Game.FILES.length);
					
					file = new File(swgPath + "/" + fileName);
					if (!file.isFile()) {
						return "";
					}
				}
				Platform.runLater(() -> {
					manager.scanSwgFinished(swgPath, true);
				});
				return swgPath;
			}
		};
	}
	
	public SimpleStringProperty getMainOut()
	{
		return mainOut;
	}
}
