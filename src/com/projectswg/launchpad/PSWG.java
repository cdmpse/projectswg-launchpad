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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import com.projectswg.launchpad.controller.FxmlController;
import com.projectswg.launchpad.controller.GameController;
import com.projectswg.launchpad.controller.MainController;
import com.projectswg.launchpad.model.SWG;
import com.projectswg.launchpad.service.GameService;
import com.projectswg.launchpad.service.Manager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;

public class PSWG extends Application
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
	public static final String FXML_GAME = "Game.fxml";
	public static final String CSS_NAME = "style.css";
	public static final String CSS_DEFAULT = "/resources/style.css";
	public static final String THEMES_FOLDER = "themes";
	public static final Preferences PREFS = Preferences.userNodeForPackage(PSWG.class);
	
	private HashMap<String, FxmlController> controllers;
	private ArrayList<GameController> gameControllers;

	private Stage primaryStage;
	private ArrayList<Stage> gameStages;
	
	private Manager manager;
	
	
	@Override
	public void start(Stage primaryStage)
	{
		this.primaryStage = primaryStage;
		gameStages = new ArrayList<>();

		manager = new Manager();
		controllers = new HashMap<>();
		gameControllers = new ArrayList<>();
		
		loadFxmls();
		
		primaryStage.setTitle("ProjectSWG");
		Image icon = new Image("file:assets/pswg_icon.png");
		if (icon.isError())
			PSWG.log("Error loading application icon");
		else
			primaryStage.getIcons().add(icon);
		
		primaryStage.setResizable(false);
		primaryStage.show();
	}
	
	public void addGameStage(String title)
	{
		Stage stage = new Stage();
		
		stage.setTitle(title);
		
		Image icon = new Image("file:assets/pswg_icon.png");
		if (icon.isError())
			PSWG.log("Error loading application icon");
		else
			stage.getIcons().add(icon);
	
		String theme = PREFS.get("theme", "");
		GameController gameController = (GameController)PSWG.loadFxml(theme, PSWG.FXML_SCREENS.get("game"));
		Scene scene = new Scene(gameController.getRoot());
		stage.setScene(scene);
		
		stage.setResizable(true);
		gameStages.add(stage);
	}
	
	public void loadFxmls()
	{
		controllers.clear();
		 
		for (Map.Entry<String, String> entry : FXML_SCREENS.entrySet())
			controllers.put(entry.getKey(), loadFxml(PREFS.get("theme", ""), entry.getValue()));
		
		Scene scene = new Scene(controllers.get("main").getRoot());
		primaryStage.setScene(scene);
		((MainController)controllers.get("main")).init(this);
		
		// games
		gameControllers.clear();
		for (SWG swg : manager.getInstances()) {
			GameController gameController = (GameController)loadFxml(PREFS.get("theme", ""), FXML_GAME);
			swg.setGameController(gameController);
			gameControllers.add(gameController);
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
		if (true)
			System.out.println("[ PSWGLog ] " + text);
	}

	public static FxmlController loadFxml(String theme, String fxml)
	{
		String codeSource = "";
		try {
			codeSource = new File(PSWG.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
			return null;
		}
		
		FXMLLoader fxmlLoader = null;
		if (!theme.equals("")) {
			File file = new File(codeSource + "/" + THEMES_FOLDER + "/" + theme + "/" + fxml);
			if (file.isFile()) {
				fxmlLoader = new FXMLLoader();
				try {
					PSWG.log("loading: " + codeSource + "/" + THEMES_FOLDER + "/" + theme + "/" + fxml);
					fxmlLoader.load(new FileInputStream(codeSource + "/" + THEMES_FOLDER + "/" + theme + "/" + fxml));
				} catch (IOException e) {
					e.printStackTrace();
					fxmlLoader = null;
				}
			}
		}
	
		if (fxmlLoader == null) {
			fxmlLoader = new FXMLLoader(PSWG.class.getResource("view/" + fxml));
			try { 
				fxmlLoader.load();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		return fxmlLoader.getController();
	}

	public void loadTheme(String theme)
	{
		String cssPath = CSS_DEFAULT;
		
		if (!theme.equals("Default") && !theme.equals("")) {
			try {
			String codeSource = new File(PSWG.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
			File file = new File(codeSource + "/" + THEMES_FOLDER + "/" + theme + "/style.css");
			if (!file.isFile()) {
				PSWG.log("Theme css file not found: " + theme);
				return;
			}
			cssPath = file.toURI().toURL().toExternalForm();
			} catch (MalformedURLException e) {
				e.printStackTrace();
				PSWG.log("Error setting theme: MalformedURLException");
				return;
			} catch (URISyntaxException e) {
				e.printStackTrace();
				PSWG.log("Error setting theme: URISyntaxException");
				return;
			}
		}
		
		primaryStage.getScene().getStylesheets().clear();
		primaryStage.getScene().getStylesheets().add(cssPath);
		
		for (SWG swg : manager.getInstances()) {
			swg.getGameController().getStage().getScene().getStylesheets().clear();
			swg.getGameController().getStage().getScene().getStylesheets().add(cssPath);
		}
	}
	
	public static boolean isWindows()
	{
		if (System.getProperty("os.name").startsWith("Windows"))
			return true;
		else
			return false;
	}
}
