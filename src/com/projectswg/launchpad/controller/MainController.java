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
import java.util.ResourceBundle;
import com.projectswg.launchpad.ProjectSWG;
import com.projectswg.launchpad.extras.TREFix;
import com.projectswg.launchpad.model.Instance;
import com.projectswg.launchpad.service.Manager;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainController implements FxmlController
{
	@FXML
	private Button settingsButton, extrasButton, optionsButton;
	
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
	private Label profileLabel;
	
	@FXML
	private Pane mainDisplayPane, gameProcessPane;
	
	private ObservableList<GameController> games;
	private GaussianBlur blur;
	private ProjectSWG pswg;
	private Stage stage;
	private Manager manager;
	private ModalController modalController;
	private SettingsController settingsComponent;
	private ExtrasController extrasComponent;
	private SetupController setupComponent;
	private NodeDisplay mainDisplay;
	private GameDisplay gameDisplay;
	private Timeline showDownloadLow, hideDownloadLow;
	private ParallelTransition showDownloadHigh, hideDownloadHigh;
	private InstanceListener instanceListener;
	private Tooltip playButtonTooltip;
	private EventHandler<MouseEvent> buttonHover;
	

	@Override
	public void initialize(URL arg0, ResourceBundle arg1)
	{
	}
	
	public void init(ProjectSWG pswg)
	{
		this.pswg = pswg;
		
		instanceListener = new InstanceListener();
		manager = pswg.getManager();
		blur = new GaussianBlur();
		
		// Displays
		gameDisplay = new GameDisplay(this);
		mainDisplay = new NodeDisplay(mainDisplayPane);

		progressIndicator.setMaxSize(36, 36);
		playButtonTooltip = new Tooltip();
		playButton.setTooltip(playButtonTooltip);
		
		modalController = (ModalController)pswg.getControllers().get("modal");
		settingsComponent = (SettingsController)pswg.getControllers().get("settings");
		setupComponent = (SetupController)pswg.getControllers().get("setup");
		extrasComponent = (ExtrasController)pswg.getControllers().get("extras");
		
		modalController.init(this);
		modalController.addComponent(settingsComponent);
		modalController.addComponent(setupComponent);
		modalController.addComponent(extrasComponent);
		
		root.getChildren().add(modalController.getRoot());

		settingsComponent.init(this);
		setupComponent.init(this);
		extrasComponent.init(this);
		
		addButtonListeners();

		profileLabel.setText(manager.getProfile().getValue());
		manager.getProfile().addListener((observable, oldValue, newValue) -> {
			profileLabel.setText(newValue);
		});
		
		modalController.getRoot().opacityProperty().addListener((observable, oldValue, newValue) -> {
			blur.setRadius(newValue.doubleValue() * 10);
		});
		
		manager.getMainOut().addListener((observable, oldValue, newValue) -> {
			mainDisplay.queueString(newValue);
		});
		
		manager.getState().addListener((observable, oldValue, newValue) -> {
			switch (newValue.intValue()) {
			case Manager.STATE_SWG_SETUP_REQUIRED:
				scanButton.setVisible(false);
				setupButton.setVisible(true);
				cancelButton.setVisible(false);
				progressIndicator.setVisible(false);
				playButton.setDisable(true);
				settingsButton.setDisable(false);
				optionsButton.setDisable(true);
				extrasButton.setDisable(true);
				break;
			
			case Manager.STATE_SWG_SCANNING:
				setupButton.setVisible(false);
				cancelButton.setVisible(true);
				cancelButton.setDefaultButton(true);
				progressIndicator.setVisible(true);
				settingsButton.setDisable(true);
				optionsButton.setDisable(true);
				extrasButton.setDisable(true);
				break;
			
			case Manager.STATE_PSWG_SETUP_REQUIRED:
				scanButton.setVisible(false);
				setupButton.setVisible(true);
				cancelButton.setVisible(false);
				progressIndicator.setVisible(false);
				playButton.setDisable(true);
				settingsButton.setDisable(false);
				optionsButton.setDisable(true);
				extrasButton.setDisable(true);
				break;
				
			case Manager.STATE_PSWG_SCAN_REQUIRED:
				scanButton.setVisible(true);
				scanButton.setDefaultButton(true);
				setupButton.setVisible(false);
				cancelButton.setVisible(false);
				progressIndicator.setVisible(false);
				playButton.setDisable(true);
				settingsButton.setDisable(false);
				optionsButton.setDisable(true);
				extrasButton.setDisable(true);
				break;
				
			case Manager.STATE_PSWG_SCANNING:
				scanButton.setVisible(false);
				setupButton.setVisible(false);
				cancelButton.setVisible(true);
				progressIndicator.setVisible(true);
				settingsButton.setDisable(true);
				optionsButton.setDisable(true);
				extrasButton.setDisable(true);
				playButton.setDisable(true);
				break;
			
			case Manager.STATE_UPDATE_REQUIRED:
				updateButton.setDefaultButton(true);
				cancelButton.setVisible(false);
				updateButton.setVisible(true);
				setupButton.setVisible(false);
				progressIndicator.setVisible(false);
				settingsButton.setDisable(false);
				optionsButton.setDisable(true);
				extrasButton.setDisable(true);
				break;
				
			case Manager.STATE_UPDATING:
				setupButton.setVisible(false);
				updateButton.setVisible(false);
				cancelButton.setVisible(true);
				progressIndicator.setVisible(true);
				if (manager.getUpdateService().getProgress() != -1)
					showProgressBar();
				optionsButton.setDisable(true);
				extrasButton.setDisable(true);
				break;
			
			case Manager.STATE_WINE_REQUIRED:
				playButton.setVisible(true);
				scanButton.setVisible(true);
				setupButton.setVisible(false);
				cancelButton.setVisible(false);
				progressIndicator.setVisible(false);
				playButton.setDisable(true);
				settingsButton.setDisable(false);
				optionsButton.setDisable(true);
				extrasButton.setDisable(true);
				scanButton.setDefaultButton(false);
				break;
				
			case Manager.STATE_PSWG_READY:
				updateButton.setVisible(false);
				setupButton.setVisible(false);
				playButton.setVisible(true);
				scanButton.setVisible(true);
				cancelButton.setVisible(false);
				progressIndicator.setVisible(false);
				settingsButton.setDisable(false);
				optionsButton.setDisable(false);
				extrasButton.setDisable(false);
				playButton.setDisable(false);
				scanButton.setDefaultButton(false);
				break;
				
			default:
				
			}
		});
		
		manager.getUpdateService().progressProperty().addListener((observable, oldValue, newValue) -> {
			Platform.runLater(() -> {
				if (oldValue.intValue() == -1)
					showProgressBar();
				progressBar.setProgress(newValue.doubleValue());
				if (newValue.intValue() == -1)
					hideProgressBar();
			});
		});

		pswg.getInstances().addListener(instanceListener);
		
		// animation
		// show low anim
		// fade
		showDownloadLow = new Timeline();
		final KeyValue showDownloadLowKV = new KeyValue(progressBar.opacityProperty(), 1, Interpolator.EASE_BOTH);
		final KeyFrame showDownloadLowKF = new KeyFrame(Duration.millis(ProjectSWG.FADE_DURATION), showDownloadLowKV);
		showDownloadLow.getKeyFrames().add(showDownloadLowKF);
		
		// show full anim
		// scale
		final ScaleTransition showDownloadHighScale = new ScaleTransition(Duration.millis(ProjectSWG.SLIDE_DURATION), progressBar);
		showDownloadHighScale.setFromX(0);
		showDownloadHighScale.setFromY(0);
		showDownloadHighScale.setToX(1);
		showDownloadHighScale.setToY(1);
		// fade
		final Timeline showDownloadHighFade = new Timeline();
		final KeyValue showDownloadHighFadeKV = new KeyValue(progressBar.opacityProperty(), 1, Interpolator.EASE_BOTH);
		final KeyFrame showDownloadHighFadeKF = new KeyFrame(Duration.millis(ProjectSWG.FADE_DURATION), showDownloadHighFadeKV);
		showDownloadHighFade.getKeyFrames().add(showDownloadHighFadeKF);
		// combine and play
		showDownloadHigh = new ParallelTransition();
		showDownloadHigh.getChildren().addAll(showDownloadHighScale, showDownloadHighFade);
		
		// hide low anim
		// long fade
		hideDownloadLow = new Timeline();
		final KeyValue hideDownloadLowKV = new KeyValue(progressBar.opacityProperty(), 0, Interpolator.EASE_BOTH);
		final KeyFrame hideDownloadLowKF = new KeyFrame(Duration.millis(ProjectSWG.SLIDE_DURATION), hideDownloadLowKV);
		hideDownloadLow.getKeyFrames().add(hideDownloadLowKF);
		hideDownloadLow.setOnFinished((event) -> {
			progressBar.setOpacity(1);
			progressBar.setVisible(false);;
		});
		
		// hide full anim
		// scale
		final ScaleTransition hideDownloadHighScale = new ScaleTransition(Duration.millis(ProjectSWG.SLIDE_DURATION), progressBar);
		hideDownloadHighScale.setFromX(1);
		hideDownloadHighScale.setFromY(1);
		hideDownloadHighScale.setToX(0);
		hideDownloadHighScale.setToY(0);
		// short fade
		final Timeline highDownloadHighFade = new Timeline();
		final KeyValue highDownloadHighFadeKV = new KeyValue(progressBar.opacityProperty(), 0, Interpolator.EASE_BOTH);
		final KeyFrame highDownloadHighFadeKF = new KeyFrame(Duration.millis(ProjectSWG.FADE_DURATION), highDownloadHighFadeKV);
		highDownloadHighFade.getKeyFrames().add(highDownloadHighFadeKF);
		// combine and play
		hideDownloadHigh = new ParallelTransition();
		hideDownloadHigh.getChildren().addAll(hideDownloadHighScale, highDownloadHighFade);
		hideDownloadHigh.setOnFinished((event) -> {
			progressBar.setOpacity(1);
			progressBar.setVisible(false);;
		});
		
		// game output
		gameDisplay.displayGames();

		// extras
		loadExtras();
	}
	
	/*
	 * add extras here
	 */
	public void loadExtras()
	{
		TREFix trefix = new TREFix(this);
		extrasComponent.addExtra(trefix);
	}
	
	public void addButtonListeners()
	{
		buttonHover = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				ProjectSWG.playSound("button_hover");
			}
		};
		
		playButton.setOnMouseEntered(buttonHover);
		playButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			manager.startSWG();
		});
	
		setupButton.setOnMouseEntered(buttonHover);
		setupButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			modalController.showWithComponent(setupComponent);
		});
		
		cancelButton.setOnMouseEntered(buttonHover);
		cancelButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			manager.requestStop();
		});
		
		scanButton.setOnMouseEntered(buttonHover);
		scanButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			manager.fullScan();
		});
		
		settingsButton.setOnMouseEntered(buttonHover);
		settingsButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			modalController.showWithComponent(settingsComponent);
		});
		
		optionsButton.setOnMouseEntered(buttonHover);
		optionsButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			manager.launchGameSettings();
		});
		
		extrasButton.setOnMouseEntered(buttonHover);
		extrasButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			modalController.showWithComponent(extrasComponent);
		});
		
		updateButton.setOnMouseEntered(buttonHover);
		updateButton.setOnAction((e) -> {
			ProjectSWG.playSound("button_press");
			manager.updatePswg();
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
	
	public ModalController getModalController()
	{
		return modalController;
	}
	
	public void stopDownloadAnimation()
	{
		showDownloadLow.stop();
		showDownloadHigh.stop();
		hideDownloadLow.stop();
		hideDownloadHigh.stop();
	}
	
	public void showProgressBar()
	{
		switch (ProjectSWG.PREFS.getInt("animation", ProjectSWG.ANIMATION_NONE)) {
		default:
		case ProjectSWG.ANIMATION_NONE:
			progressBar.setVisible(true);
			break;
		case ProjectSWG.ANIMATION_LOW:
			progressBar.setOpacity(0);
			progressBar.setVisible(true);
			stopDownloadAnimation();
			showDownloadLow.play();
			break;
		case ProjectSWG.ANIMATION_HIGH:
		case ProjectSWG.ANIMATION_WARS:
			progressBar.setOpacity(0);
			progressBar.setVisible(true);
			stopDownloadAnimation();
			showDownloadHigh.play();
			break;
		}
	}
	
	public void hideProgressBar()
	{
		switch (ProjectSWG.PREFS.getInt("animation", ProjectSWG.ANIMATION_NONE)) {
		default:
		case ProjectSWG.ANIMATION_NONE:
			progressBar.setVisible(false);
			break;
		case ProjectSWG.ANIMATION_LOW:
			stopDownloadAnimation();
			hideDownloadLow.play();
			break;
		case ProjectSWG.ANIMATION_HIGH:
		case ProjectSWG.ANIMATION_WARS:
			stopDownloadAnimation();
			hideDownloadHigh.play();
			break;
		}
	}
	
	@Override
	public Parent getRoot()
	{
		return root;
	}
	
	public ProjectSWG getPswg()
	{
		return pswg;
	}
	
	public Manager getManager()
	{
		return manager;
	}
	
	public ObservableList<GameController> getGames()
	{
		return games;
	}
	
	public Pane getGameProcessPane()
	{
		return gameProcessPane;
	}
	
	public void removeInstanceListener()
	{
		pswg.getInstances().removeListener(instanceListener);
	}
	
	private class InstanceListener implements ListChangeListener<Instance>
	{
		@Override
		public void onChanged(Change<? extends Instance> change) {
			while (change.next())
				if (change.wasAdded())
					if (ProjectSWG.PREFS.getBoolean("close_after_launch", false))
						Platform.exit();
					else
						for (Instance instance : change.getAddedSubList()) {
							ProjectSWG.log("instanceListener: " + instance.getLabel());
							gameDisplay.addGame(instance);
							if (ProjectSWG.PREFS.getBoolean("open_on_launch", false) && ProjectSWG.PREFS.getBoolean("capture", false))
									instance.getGameController().show();
						}
				else if (change.wasRemoved())
					for (Instance instance : change.getRemoved())
						gameDisplay.removeGame(instance);
			}
	}
	
	public Tooltip getPlayButtonTooltip()
	{
		return playButtonTooltip;
	}
	
	public EventHandler<MouseEvent> getButtonHover()
	{
		return buttonHover;
	}
}