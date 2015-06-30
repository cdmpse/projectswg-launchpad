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

import java.util.ArrayList;
import com.projectswg.launchpad.ProjectSWG;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.projectswg.launchpad.model.Instance;
import com.projectswg.launchpad.service.GameService;

public class GameDisplay
{
	private static final int SLIDE_DURATION = 500;
	
	private Pane root;
	private volatile ArrayList<Instance> display;
	private MainController mainController;

	
	public GameDisplay(MainController mainController)
	{
		this.mainController = mainController;
		this.root = mainController.getGameProcessPane();
		display = new ArrayList<>();
		
		mainController.getManager().getInstances().addListener((ListChangeListener.Change<? extends Instance> e) -> {
			while (e.next())
				if (e.wasAdded()) {
					if (ProjectSWG.PREFS.getBoolean("close_after_launch", false))
						Platform.exit();
					for (Instance swg : e.getAddedSubList())
						addGame(swg);
				} else if (e.wasRemoved())
					for (Instance swg : e.getRemoved())
						removeGame(swg);
		});
	}
	
	public Pane getRoot()
	{
		return root;
	}
	
	public void removeGame(Instance swg)
	{
		if (display.contains(swg)) {
			Group gameButtonGroup = swg.getGameButtonGroup();
			ParallelTransition removeGameAnimationHigh = new ParallelTransition();
			final Timeline slideLeft = new Timeline();
			final KeyValue removeGameAnimationHighKV = new KeyValue(gameButtonGroup.layoutYProperty(), root.getHeight(), Interpolator.EASE_OUT);
			final KeyFrame removeGameAnimationHighKF = new KeyFrame(Duration.millis(SLIDE_DURATION), removeGameAnimationHighKV);
			slideLeft.getKeyFrames().add(removeGameAnimationHighKF);
			removeGameAnimationHigh.getChildren().addAll(slideLeft);
			removeGameAnimationHigh.setOnFinished((e) -> {
				root.getChildren().remove(display.indexOf(swg));
				display.remove(swg);
			});
			removeGameAnimationHigh.play();
		}
	}
	
	public void addGame(Instance swg)
	{
		String title = "ProjectSWG: " + (display.size() + 1);

		Stage stage = new Stage();
		Image icon = new Image("/resources/pswg_icon.png");
		if (icon.isError())
			ProjectSWG.log("Error loading application icon");
		else
			stage.getIcons().add(icon);
		
		stage.setTitle(title);
		GameController gameController = (GameController)ProjectSWG.loadFxml(ProjectSWG.PREFS.get("theme", "Default"), ProjectSWG.FXML_GAME);
		Scene scene = new Scene(gameController.getRoot());
		stage.setScene(scene);
		stage.setResizable(true);
		swg.setStage(stage);
		
		Button gameButton = new Button(ProjectSWG.WHITE_CIRCLE);
		gameButton.setTooltip(new Tooltip("Game process: " + display.size()));
		gameButton.setOpacity(0);
		Group gameButtonGroup = new Group(gameButton);
		
		swg.setGameButtonGroup(gameButtonGroup);
		swg.setGameController(gameController);
		display.add(swg);
		GameService gameService = swg.getGameService();
		gameController.init(gameService, stage);
		
		// menu
		ContextMenu contextMenu = new ContextMenu();
		
		MenuItem cm1Remove = new MenuItem("Remove");
		cm1Remove.setOnAction((e) -> {
			if (gameService.isRunning())
				gameService.cancel();
			mainController.getManager().getInstances().remove(swg);
		});
		
		MenuItem cm2Stop = new MenuItem("Stop");
		cm2Stop.setOnAction((e) -> {
			gameService.cancel();
		});
		
		MenuItem cm3Show = new MenuItem("Show");
		cm3Show.setOnAction((e) -> {
			gameController.show();
		});
		
		contextMenu.getItems().addAll(cm1Remove, cm2Stop, cm3Show);
		gameButton.setBackground(Background.EMPTY);
		gameButton.setContextMenu(contextMenu);
		gameButton.setOnAction((e) -> {
			Platform.runLater(() -> {
				contextMenu.show(gameButton, Side.TOP, 0, 0);
			});
		});
		
		gameService.runningProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue) {
				gameButton.setText(ProjectSWG.BLACK_CIRCLE);
				cm2Stop.setDisable(true);
			}
		});
		
		root.getChildren().add(gameButtonGroup);
		
		Platform.runLater(() -> {
			displayGames();
		});
		
		if (ProjectSWG.PREFS.getBoolean("open_on_launch", false))
			gameController.show();
	}
	
	public void displayGames()
	{
		double notch = root.getWidth() / (1 + display.size());

		for (int i = 0; i < display.size(); i++) {
			
			Instance swg = display.get(i);
			Group gameButtonGroup = swg.getGameButtonGroup();
			Button gameButton = (Button)gameButtonGroup.getChildren().get(0);
		
			if (gameButton.getLayoutX() == 0) {
				gameButton.setLayoutX(root.getWidth());
				gameButton.setLayoutY(root.getHeight() / 2 - gameButtonGroup.layoutBoundsProperty().getValue().getHeight() / 2);
			}
			
			double destX = notch * (i + 1) - gameButtonGroup.layoutBoundsProperty().getValue().getWidth() / 2;

			Platform.runLater(() -> {
				switch (ProjectSWG.PREFS.getInt("animation", ProjectSWG.ANIMATION_HIGH)) {
				case ProjectSWG.ANIMATION_NONE:
					gameButton.setOpacity(1);
					break;
					
				case ProjectSWG.ANIMATION_LOW:
					gameButton.setOpacity(1);
					break;
					
				case ProjectSWG.ANIMATION_HIGH:
					gameButton.setOpacity(1);
					ParallelTransition showGameAnimationHigh = new ParallelTransition();
					final Timeline slideLeft = new Timeline();
					final KeyValue showGameAnimationHighKV = new KeyValue(gameButton.layoutXProperty(), destX, Interpolator.EASE_OUT);
					final KeyFrame showGameAnimationHighKF = new KeyFrame(Duration.millis(SLIDE_DURATION), showGameAnimationHighKV);
					slideLeft.getKeyFrames().add(showGameAnimationHighKF);
					showGameAnimationHigh.getChildren().addAll(slideLeft);
					showGameAnimationHigh.play();
					break;
				}
			});
		}
	}
}
