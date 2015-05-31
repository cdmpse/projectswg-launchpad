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

package com.projectswg.launchpad.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import com.projectswg.launchpad.PSWG;
import com.projectswg.launchpad.extras.TREFix;
import com.projectswg.launchpad.service.Manager;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainController implements FxmlController
{
	@FXML
	private Button launcherSettingsButton, extrasButton, gameSettingsButton;
	
	@FXML
	private Button updateButton, setupButton, cancelButton, playButton, scanButton;
	
	@FXML
	private ProgressIndicator progressIndicator;
	
	@FXML
	private ProgressBar progressBar;
	
	@FXML
	private StackPane root;
	
	@FXML
	private VBox mainRoot;
	
	@FXML
	private Pane mainDisplayPane, gameProcessPane;
	
	@FXML
	private ImageView mainGraphic;
	
	public static final double SLIDE_DURATION = 300;
	public static final double FADE_DURATION = 250;
	
	public static final int ANIMATION_NONE = 0;
	public static final int ANIMATION_LOW = 1;
	public static final int ANIMATION_HIGH = 2;
	
	public static final String CHECKMARK = "\u2713";
	public static final String XMARK = "\u2717";
	public static final String PENCIL_ICON = "\u270e";
	public static final String MAGNIFYING_GLASS = "\ud83d\udd0d";
	public static final String UP_ARROW = "\u21e1";
	public static final String DOWN_ARROW = "\u21e3";
	public static final String WHITE_CIRCLE = "\u25cb";
	public static final String BLACK_CIRCLE = "\u25cf";
	public static final String SPEAKER = "\ud83d\udd0a";
	
	// observables
	private SimpleIntegerProperty animationLevel;
	private ObservableList<GameController> games;
	
	private List<String> screens;
	
	// animation
	private GaussianBlur blur;
	
	private PSWG pswg;
	private Stage stage;
	private Manager manager;
	private ModalController modal;
	private SettingsController settingsComponent;
	private ExtrasController extrasComponent;
	private SetupController setupComponent;
	private NodeDisplay mainDisplay;
	private GameDisplay gameDisplay;
	
	
	public MainController()
	{
		animationLevel = new SimpleIntegerProperty();
		blur = new GaussianBlur();
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1)
	{
	}
	
	public void init(PSWG pswg)
	{
		this.pswg = pswg;
		
		manager = pswg.getManager();
		
		progressIndicator.setMaxSize(36, 36);
		
		animationLevel.set(PSWG.PREFS.getInt("animation", 2));
		animationLevel.addListener((observable, oldValue, newValue) -> {
			PSWG.PREFS.putInt("animation", newValue.intValue());
		});
		
		// Game process display
		gameDisplay = new GameDisplay(this);
		
		// main text display
		mainDisplay = new NodeDisplay(mainDisplayPane);
		manager.getMainOut().addListener((observable, oldValue, newValue) -> {
			Platform.runLater(() -> {
				mainDisplay.queueString(newValue);
			});
		});

		addButtonListeners();
		
		manager.getState().addListener((observable, oldValue, newValue) -> {
			PSWG.log("Main::init .. Manager state changed: " + oldValue + " -> " + newValue);
			setControlsState(newValue.intValue());
		});

		PSWG.log("Main::init .. Manager.state = " + manager.getState().getValue());
		
		modal = (ModalController)pswg.getControllers().get("modal");
		settingsComponent = (SettingsController)pswg.getControllers().get("settings");
		setupComponent = (SetupController)pswg.getControllers().get("setup");
		extrasComponent = (ExtrasController)pswg.getControllers().get("extras");
		
		modal.getRoot().opacityProperty().addListener((observable, oldValue, newValue) -> {
			blur.setRadius(newValue.doubleValue() * 10);
		});
		
		modal.init(this);
		modal.addComponent(settingsComponent);
		modal.addComponent(setupComponent);
		modal.addComponent(extrasComponent);
		
		root.getChildren().add(modal.getRoot());

		settingsComponent.init(modal);
		setupComponent.init(modal);
		extrasComponent.init(modal);
		
		// theme
		pswg.loadTheme(PSWG.PREFS.get("theme", "Default"));
		
		/*
		 * Extras
		 */
		TREFix trefix = new TREFix(this);
		extrasComponent.addExtra(trefix);

		// trigger
		manager.loadPrefs();
	}
	
	public void onShow()
	{
	}
	
	public void setControlsState(int state)
	{
		switch (state) {
		case Manager.STATE_SETUP:
			launcherSettingsButton.setDisable(false);
			gameSettingsButton.setDisable(true);
			extrasButton.setDisable(true);
			
			setupButton.setVisible(true);
			playButton.setVisible(false);
			scanButton.setVisible(false);
			updateButton.setVisible(false);
			progressIndicator.setVisible(false);
			
			mainDisplay.queueString("Setup required");
			break;
			
		case Manager.STATE_SCAN_REQUIRED:
			launcherSettingsButton.setDisable(false);
			gameSettingsButton.setDisable(true);
			extrasButton.setDisable(true);
			
			playButton.setVisible(false);
			scanButton.setVisible(true);
			updateButton.setVisible(false);
			setupButton.setVisible(false);
			progressIndicator.setVisible(false);
			
			mainDisplay.queueString("Scan required");
			break;
			
		case Manager.STATE_SCANNING:
			launcherSettingsButton.setDisable(false);
			gameSettingsButton.setDisable(true);
			extrasButton.setDisable(true);
			playButton.setDisable(true);
			
			playButton.setVisible(true);
			scanButton.setVisible(false);
			updateButton.setVisible(false);
			setupButton.setVisible(false);
			progressIndicator.setVisible(true);
			break;
			
		case Manager.STATE_UPDATE_REQUIRED:
			updateButton.setDisable(false);
			playButton.setDisable(true);
			launcherSettingsButton.setDisable(false);
			gameSettingsButton.setDisable(true);
			extrasButton.setDisable(true);
			
			playButton.setVisible(true);
			scanButton.setVisible(false);
			updateButton.setVisible(true);
			setupButton.setVisible(false);
			progressIndicator.setVisible(false);
			
			mainDisplay.queueString("Update required");
			break;
			
		case Manager.STATE_PLAY:
			playButton.setDisable(false);
			launcherSettingsButton.setDisable(false);
			gameSettingsButton.setDisable(false);
			extrasButton.setDisable(false);
			scanButton.setDisable(false);
			
			playButton.setVisible(true);
			scanButton.setVisible(true);
			updateButton.setVisible(false);
			setupButton.setVisible(false);
			progressIndicator.setVisible(false);
			
			mainDisplay.queueString("Ready to play");
			
			Platform.runLater(() -> {
				playButton.requestFocus();
			});
			break;
		}
	}
	
	public void addButtonListeners()
	{
		playButton.setOnAction((e) -> {
			manager.startSWG();
		});
	
		setupButton.setOnAction((e) -> {
			modal.showWithComponent(setupComponent);
		});
		
		cancelButton.setOnAction((e) -> {
			if (manager.isBusy())
				manager.requestStop();
		});
		
		scanButton.setOnAction((e) -> {
			manager.scanPswg(false);
		});
		
		launcherSettingsButton.setOnAction((e) -> {
			modal.showWithComponent(settingsComponent);
		});
		
		gameSettingsButton.setOnAction((e) -> {
			manager.launchGameSettings();
		});
		
		extrasButton.setOnAction((e) -> {
			modal.showWithComponent(extrasComponent);
		});
		
		updateButton.setOnAction((e) -> {
			manager.startDownload();
		});
	}
	
	public void setStage(Stage stage)
	{
		this.stage = stage;
	}
	
	public Stage getStage()
	{
		return stage;
	}
	
	public void refreshLoginServerStatus()
	{
		manager.pingLoginServer();
	}
	
	public ModalController getModal()
	{
		return modal;
	}
	
	public void showProgress(float p)
	{
		switch (PSWG.PREFS.getInt("animation", 2)) {
		case ANIMATION_NONE:
			progressBar.setVisible(true);
			break;
			
		case ANIMATION_LOW:
			progressBar.setOpacity(0);
			progressBar.setVisible(true);
			// fade
			final Timeline longFadeIn = new Timeline();
			final KeyValue longFadeInKV = new KeyValue(progressBar.opacityProperty(), 1, Interpolator.EASE_BOTH);
			final KeyFrame longFadeInKF = new KeyFrame(Duration.millis(FADE_DURATION), longFadeInKV);
			longFadeIn.getKeyFrames().add(longFadeInKF);
			longFadeIn.play();
			break;
			
		case ANIMATION_HIGH:
			progressBar.setOpacity(0);
			progressBar.setVisible(true);
			// scale
			final ScaleTransition scaleProgressUp = new ScaleTransition(Duration.millis(SLIDE_DURATION), progressBar);
			scaleProgressUp.setFromX(0);
			scaleProgressUp.setFromY(0);
			scaleProgressUp.setToX(1);
			scaleProgressUp.setToY(1);
			// fade
			final Timeline shortFadeIn = new Timeline();
			final KeyValue shortFadeInKV = new KeyValue(progressBar.opacityProperty(), 1, Interpolator.EASE_BOTH);
			final KeyFrame shortFadeInKF = new KeyFrame(Duration.millis(FADE_DURATION), shortFadeInKV);
			shortFadeIn.getKeyFrames().add(shortFadeInKF);
			// combine and play
			ParallelTransition parallelTransition = new ParallelTransition();
			parallelTransition.getChildren().addAll(scaleProgressUp, shortFadeIn);
			parallelTransition.play();
			break;
		}
	}
	
	public void hideProgress()
	{
		switch (PSWG.PREFS.getInt("animation", 2)) {
		case ANIMATION_NONE:
			progressBar.setVisible(false);;
			break;
			
		case ANIMATION_LOW:
			// long fade
			final Timeline longFadeOut = new Timeline();
			final KeyValue longFadeOutKV = new KeyValue(progressBar.opacityProperty(), 0, Interpolator.EASE_BOTH);
			final KeyFrame longFadeOutKF = new KeyFrame(Duration.millis(SLIDE_DURATION), longFadeOutKV);
			longFadeOut.getKeyFrames().add(longFadeOutKF);
			longFadeOut.setOnFinished((event) -> {
				progressBar.setOpacity(1);
				progressBar.setVisible(false);;
			});
			longFadeOut.play();
			break;
			
		case ANIMATION_HIGH:
			// scale
			final ScaleTransition scaleProgressUp = new ScaleTransition(Duration.millis(SLIDE_DURATION), progressBar);
			scaleProgressUp.setFromX(1);
			scaleProgressUp.setFromY(1);
			scaleProgressUp.setToX(0);
			scaleProgressUp.setToY(0);
			// short fade
			final Timeline shortFadeOut = new Timeline();
			final KeyValue shortFadeOutKV = new KeyValue(progressBar.opacityProperty(), 0, Interpolator.EASE_BOTH);
			final KeyFrame shortFadeOutKF = new KeyFrame(Duration.millis(FADE_DURATION), shortFadeOutKV);
			shortFadeOut.getKeyFrames().add(shortFadeOutKF);
			// combine and play
			ParallelTransition parallelTransition = new ParallelTransition();
			parallelTransition.getChildren().addAll(scaleProgressUp, shortFadeOut);
			parallelTransition.setOnFinished((event) -> {
				progressBar.setOpacity(1);
				progressBar.setVisible(false);;
			});
			parallelTransition.play();
			break;
		}
	}
	
	@Override
	public Parent getRoot()
	{
		return root;
	}
	
	public PSWG getMain()
	{
		return pswg;
	}
	
	public Manager getManager()
	{
		return manager;
	}
	
	public GaussianBlur getBlur()
	{
		return blur;
	}
	
	public SimpleIntegerProperty getAnimationLevel()
	{
		return animationLevel;
	}

	public ObservableList<GameController> getGames()
	{
		return games;
	}
	
	public Pane getGameProcessPane()
	{
		return gameProcessPane;
	}
}
