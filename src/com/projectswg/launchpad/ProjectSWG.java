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

package com.projectswg.launchpad;
	
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import com.projectswg.launchpad.controller.FxmlController;
import com.projectswg.launchpad.controller.GameController;
import com.projectswg.launchpad.controller.MainController;
import com.projectswg.launchpad.model.Instance;
import com.projectswg.launchpad.service.Manager;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.fxml.FXMLLoader;

public class ProjectSWG extends Application
{	
	public static final String PSWG_URL = "http://www.projectswg.com";
	public static final Map<String, String> FXML_SCREENS;
	static {
		FXML_SCREENS = new HashMap<>();
		FXML_SCREENS.put("main", "Main.fxml");
		FXML_SCREENS.put("setup", "Setup.fxml");
		FXML_SCREENS.put("settings", "Settings.fxml");
		FXML_SCREENS.put("modal", "Modal.fxml");
		FXML_SCREENS.put("extras", "Extras.fxml");
	}
	public static final Preferences PREFS = Preferences.userNodeForPackage(ProjectSWG.class);
	
	public static final String FXML_GAME = "Game.fxml";
	public static final String CSS_NAME = "style.css";
	public static final String CSS_DEFAULT = "/resources/style.css";
	public static final String THEMES_FOLDER = "themes";

	public static final String CHECKMARK = "\u2713";
	public static final String XMARK = "\u2717";
	public static final String PENCIL_ICON = "\u270e";
	public static final String MAGNIFYING_GLASS = "\ud83d\udd0d";
	public static final String UP_ARROW = "\u21e1";
	public static final String DOWN_ARROW = "\u21e3";
	public static final String WHITE_CIRCLE = "\u25cb";
	public static final String BLACK_CIRCLE = "\u25cf";
	public static final String SPEAKER = "\ud83d\udd0a";
	public static final String SPEAKER_MUTE = "\ud83d\udd07";
	public static final String LOCK = "\ud83d\udd12";
	public static final String OPEN_LOCK = "\ud83d\udd13";
	
	public static final int ANIMATION_NONE = 0;
	public static final int ANIMATION_LOW = 1;
	public static final int ANIMATION_HIGH = 2;
	
	public static final double SLIDE_DURATION = 300;
	public static final double FADE_DURATION = 250;
	
	private HashMap<String, FxmlController> controllers;
	private Stage primaryStage;
	private Manager manager;

	
	@Override
	public void start(Stage primaryStage)
	{
		this.primaryStage = primaryStage;

		manager = new Manager();
		
		controllers = new HashMap<>();
		loadTheme(PREFS.get("theme", "Default"));
		
		primaryStage.setResizable(false);
		primaryStage.show();
	}
	
	public void loadTheme(String theme)
	{
		PREFS.put("theme", theme);
		
		loadFxmls(theme);
		loadCss(theme);
		
		int state = manager.getState().getValue();
		manager.getState().set(Manager.STATE_INIT);
		manager.getState().set(state);
	}
	
	public void loadFxmls(String theme)
	{
		controllers.clear();
		 
		for (Map.Entry<String, String> entry : FXML_SCREENS.entrySet())
			controllers.put(entry.getKey(), loadFxml(theme, entry.getValue()));
		
		Scene scene = new Scene(controllers.get("main").getRoot());
		primaryStage.setScene(scene);
		primaryStage.setTitle("ProjectSWG");
		Image icon = new Image("/resources/pswg_icon.png");
		if (icon.isError())
			ProjectSWG.log("Error loading application icon");
		else
			primaryStage.getIcons().add(icon);
		
		((MainController)controllers.get("main")).init(this);
		
		// games
		for (Instance swg : manager.getInstances()) {
			GameController gameController = (GameController)loadFxml(theme, FXML_GAME);
			swg.setGameController(gameController);
		}
	}
	
	public void loadCss(String theme)
	{
		String cssPath = CSS_DEFAULT;
		
		if (!theme.equals("Default")) {
			try {
				String codeSource = new File(ProjectSWG.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
				File file = new File(codeSource + "/" + THEMES_FOLDER + "/" + theme + "/style.css");
				if (!file.isFile()) {
					ProjectSWG.log("Theme css file not found: " + theme);
					return;
				}
				cssPath = file.toURI().toURL().toExternalForm();
			} catch (MalformedURLException e) {
				e.printStackTrace();
				ProjectSWG.log("Error setting theme: MalformedURLException");
				return;
			} catch (URISyntaxException e) {
				e.printStackTrace();
				ProjectSWG.log("Error setting theme: URISyntaxException");
				return;
			}
		}
		
		primaryStage.getScene().getStylesheets().clear();
		primaryStage.getScene().getStylesheets().add(cssPath);
		
		for (Instance swg : manager.getInstances()) {
			swg.getGameController().getStage().getScene().getStylesheets().clear();
			swg.getGameController().getStage().getScene().getStylesheets().add(cssPath);
		}
	}
	
	public HashMap<String, FxmlController> getControllers()
	{
		return controllers;
	}
	
	public static void main(String[] args)
	{
		launch(args);
	}
	
	public Manager getManager()
	{
		return manager;
	}
	
	public Stage getStage()
	{
		return primaryStage;
	}
	
	public static void log(String text)
	{
		System.out.println("[ PSWGLog ] " + text);
	}

	public static FxmlController loadFxml(String theme, String fxml)
	{
		ProjectSWG.log("loadFxml: " + fxml);
		
		String codeSource = "";
		try {
			codeSource = new File(ProjectSWG.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
			return null;
		}
		
		FXMLLoader fxmlLoader = null;
		
		if (!theme.equals("Default")) {
			File file = new File(codeSource + "/" + THEMES_FOLDER + "/" + theme + "/" + fxml);
			if (file.isFile()) {
				fxmlLoader = new FXMLLoader();
				try {
					ProjectSWG.log("loading: " + codeSource + "/" + THEMES_FOLDER + "/" + theme + "/" + fxml);
					fxmlLoader.load(new FileInputStream(codeSource + "/" + THEMES_FOLDER + "/" + theme + "/" + fxml));
				} catch (IOException e) {
					e.printStackTrace();
					fxmlLoader = null;
				}
			}
		}
		
		if (fxmlLoader == null) {
			// theme wasnt loaded, load default
			fxmlLoader = new FXMLLoader(ProjectSWG.class.getResource("view/" + fxml));
			try { 
				fxmlLoader.load();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		return fxmlLoader.getController();
	}
	
	public static boolean isWindows()
	{
		if (System.getProperty("os.name").startsWith("Windows"))
			return true;
		else
			return false;
	}
}
