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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.projectswg.launchpad.controller.FxmlController;
import com.projectswg.launchpad.controller.GameController;
import com.projectswg.launchpad.controller.LogController;
import com.projectswg.launchpad.controller.MainController;
import com.projectswg.launchpad.model.Instance;
import com.projectswg.launchpad.service.Manager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
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
		FXML_SCREENS.put("log", "Log.fxml");
	}
	public static final Preferences PREFS = Preferences.userNodeForPackage(ProjectSWG.class);
	public static final SimpleStringProperty DEBUG = new SimpleStringProperty();
	
	public static final String FXML_GAME = "Game.fxml";
	public static final String CSS_NAME = "style.css";
	public static final String CSS_DEFAULT = "/resources/style.css";
	public static final String CSS_BASIC = "/resources/basic.css";
	public static final String ICON = "/resources/pswg_icon.png";
	public static final String THEMES_FOLDER = "themes";
	public static final String THEME_DEFAULT = "Default";
	public static final String CHECKMARK = "\u2713";
	public static final String XMARK = "\u2717";
	public static final String CIRCLE = "\u25cb";
	public static final String DOT = "\u25cf";
	
	public static final int ANIMATION_NONE = 0;
	public static final int ANIMATION_LOW = 1;
	public static final int ANIMATION_HIGH = 2;
	public static final int ANIMATION_WARS = 3;
	
	// slide > fade
	public static final double SLIDE_DURATION = 300;
	public static final double FADE_DURATION = 250;
	
	private HashMap<String, FxmlController> controllers;
	private Stage primaryStage, debugStage;
	private Manager manager;
	private ObservableList<Instance> instances;
	private int instanceCount = 0;
	
	
	@Override
	public void start(Stage primaryStage)
	{
		this.primaryStage = primaryStage;
		primaryStage.setTitle("ProjectSWG");
		primaryStage.setResizable(false);
		// otherwise loads slightly different from theme load
		primaryStage.setOpacity(0);
		primaryStage.show();
		
		debugStage = new Stage();
		debugStage.setResizable(true);
		Image debugIcon = new Image("/resources/pswg_icon.png");
		if (debugIcon.isError())
			log("Error loading application icon");
		else
			debugStage.getIcons().add(debugIcon);
		debugStage.setTitle("Debug");
		
		instances = FXCollections.observableArrayList();
		controllers = new HashMap<>();
		Font.loadFont(ProjectSWG.class.getResource("/resources/galbasic.ttf").toExternalForm(), 10);
		loadTheme(PREFS.get("theme", THEME_DEFAULT));
		
		primaryStage.centerOnScreen();
		primaryStage.setOpacity(1);
		playSound("launcher_start");
	}
	
	public int getInstanceNumber()
	{
		return ++instanceCount;
	}
	
	public void loadTheme(String theme)
	{
		PREFS.put("theme", theme);
		
		manager = new Manager(this);
		
		loadFxmls(theme);
		loadCss(theme);

		Platform.runLater(() -> {
			// wine
			manager.getWineBinary().set(ProjectSWG.PREFS.get("wine_binary",  ""));
			manager.getWineArguments().set(ProjectSWG.PREFS.get("wine_arguments", ""));
			manager.getWineEnvironmentVariables().set(ProjectSWG.PREFS.get("wine_environment_variables", ""));
			
			// folders
			manager.getSwgFolder().set(ProjectSWG.PREFS.get("swg_folder", ""));
			manager.getPswgFolder().set(ProjectSWG.PREFS.get("pswg_folder", ""));
		});
	}
	
	public void loadFxmls(String theme)
	{
		controllers.clear();
		 
		for (Map.Entry<String, String> entry : FXML_SCREENS.entrySet())
			controllers.put(entry.getKey(), loadFxml(theme, entry.getValue()));
		
		primaryStage.setScene(new Scene(controllers.get("main").getRoot()));

		// add to theme
		Image icon = new Image("/resources/pswg_icon.png");
		if (icon.isError())
			log("Error loading application icon");
		else
			primaryStage.getIcons().add(icon);
		
		((MainController)controllers.get("main")).init(this);
		
		LogController logController = (LogController)controllers.get("log");
		logController.init(debugStage);
		debugStage.setScene(new Scene(logController.getRoot()));
		
		// games
		for (Instance instance : instances) {
			GameController gameController = (GameController)loadFxml(theme, FXML_GAME);
			instance.setGameController(gameController);
			gameController.init(instance.getGameService(), instance.getStage());
		}
	}
	
	public void loadCss(String theme)
	{
		String cssPath = CSS_DEFAULT;
		
		if (!theme.equals(THEME_DEFAULT)) {
			try {
				String codeSource = new File(ProjectSWG.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
				final File file = new File(codeSource + "/" + THEMES_FOLDER + "/" + theme + "/style.css");
				if (!file.isFile()) {
					log("Theme css file not found: " + theme);
					return;
				}
				cssPath = file.toURI().toURL().toExternalForm();
			} catch (MalformedURLException | URISyntaxException e) {
				log("Error setting theme: " + e.toString());
				return;
			}
		}
		
		primaryStage.getScene().getStylesheets().clear();
		primaryStage.getScene().getStylesheets().add(cssPath);

		debugStage.getScene().getStylesheets().clear();
		debugStage.getScene().getStylesheets().add(cssPath);

		for (Instance instance : instances) {
			instance.getGameController().getStage().getScene().getStylesheets().clear();
			instance.getGameController().getStage().getScene().getStylesheets().add(cssPath);
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
		System.out.println(text);
		Platform.runLater(() -> {
			DEBUG.set((new Date()).toString() + ": " + text);
		});
	}
	
	public static FxmlController loadFxml(String theme, String fxml)
	{
		FXMLLoader fxmlLoader = null;
		try {
			if (!theme.equals(THEME_DEFAULT)) {
				final String codeSource = new File(ProjectSWG.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
				log("loading fxml: " + codeSource + "/" + THEMES_FOLDER + "/" + theme + "/" + fxml);
				File file = new File(codeSource + "/" + THEMES_FOLDER + "/" + theme + "/" + fxml);
				if (file.isFile()) {
					fxmlLoader = new FXMLLoader();
					fxmlLoader.load(new FileInputStream(codeSource + "/" + THEMES_FOLDER + "/" + theme + "/" + fxml));
				}
			}
			if (fxmlLoader == null) {
				fxmlLoader = new FXMLLoader(ProjectSWG.class.getResource("view/" + fxml));
				fxmlLoader.load();
			}
		} catch (IOException | URISyntaxException e) {
			log("Error loading fmxl: " + e.toString());
			return null;
		}
		
		return fxmlLoader.getController();
	}
	
	public static void playSound(String sound)
	{
		if (!PREFS.getBoolean("sound", false))
			return;
		
		sound += ".mp3";
		String theme = PREFS.get("theme", THEME_DEFAULT);
		Media media = null;

		if (theme.equals(THEME_DEFAULT)) {
			try {
				media = new Media(ProjectSWG.class.getResource("/resources/" + sound).toString());
			} catch (MediaException | NullPointerException e1) {
				log("Error loading default audio: " + e1.toString());
				return;
			}
		} else {
			try {
				final String codeSource = new File(ProjectSWG.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
				File f = new File(codeSource + "/" + THEMES_FOLDER + "/" + theme + "/" + sound);
				if (!f.isFile())
					return;
				media = new Media(f.toURI().toString());
				if (media.getError() == null)
					media.setOnError(new Runnable() {
						public void run()
						{
							log("Media error");
						}
					});
			} catch (IllegalArgumentException | URISyntaxException e1) {
				log("Error loading theme audio: " + e1.toString());
				return;
			}
		} 

		final MediaPlayer mediaPlayer = new MediaPlayer(media);
		if (mediaPlayer.getError() == null) {
			mediaPlayer.setOnError(new Runnable() {
				public void run()
				{
					log("MediaPlayer error");
				}
			});
			mediaPlayer.play();
		}
	}
	
	public static boolean isWindows()
	{
		return System.getProperty("os.name").startsWith("Windows");
	}
	
	public ObservableList<Instance> getInstances()
	{
		return instances;
	}
}
