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
import java.util.prefs.Preferences;

import com.projectswg.launchpad.PSWG;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.projectswg.launchpad.model.Instance;
import com.projectswg.launchpad.service.GameService;

public class GameDisplay
{
	private static final int SLIDE_DURATION = 500;
	
	private Pane root;
	private ArrayList<Instance> display;
	private MainController mainController;

	
	public GameDisplay(MainController mainController)
	{
		this.mainController = mainController;
		this.root = mainController.getGameProcessPane();
		display = new ArrayList<>();
		
		mainController.getManager().getInstances().addListener((ListChangeListener.Change<? extends Instance> e) -> {
			while (e.next()) {
				if (e.wasAdded()) {
					if (PSWG.PREFS.getBoolean("close_after_launch", false))
						Platform.exit();
					for (Instance swg : e.getAddedSubList()) {
						addGame(swg);
					}
				} else if (e.wasRemoved()) {
					for (Instance swg : e.getRemoved()) {
						removeGame(swg);
					}
				}
			};
		});
	}
	
	public Pane getRoot()
	{
		return root;
	}
	
	public void removeGame(Instance swg)
	{
		if (display.contains(swg)) {
			root.getChildren().remove(display.indexOf(swg));
			display.remove(swg);
		}
	}
	
	public void addGame(Instance swg)
	{
		String title = "ProjectSWG: " + (display.size() + 1);

		Stage stage = new Stage();
		Image icon = new Image("/resources/pswg_icon.png");
		if (icon.isError())
			PSWG.log("Error loading application icon");
		else
			stage.getIcons().add(icon);
		
		stage.setTitle(title);
		GameController gameController = (GameController)PSWG.loadFxml(PSWG.FXML_GAME);
		Scene scene = new Scene(gameController.getRoot());
		stage.setScene(scene);
		stage.setResizable(true);
		gameController.init();
		gameController.setStage(stage);
		swg.setStage(stage);
		Button gameButton = new Button(MainController.WHITE_CIRCLE);
		swg.setGameButton(gameButton);
		swg.setGameController(gameController);
		GameService gameService = swg.getGameService();
		display.add(swg);
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
		gameButton.setMaxSize(50, 100);
		gameButton.setBackground(Background.EMPTY);
		gameButton.setContextMenu(contextMenu);
		gameButton.setOnAction((e) -> {
			Platform.runLater(() -> {
				contextMenu.show(gameButton, Side.TOP, 0, 0);
			});
		});
		
		gameService.runningProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue) {
				gameButton.setEffect(new DropShadow(5, Color.GRAY));
				gameButton.setText(MainController.BLACK_CIRCLE);
				cm2Stop.setDisable(true);
			}
		});
		
		Tooltip tt = new Tooltip("Game process: " + display.size());
		gameButton.setTooltip(tt);
		gameButton.setEffect(new DropShadow(5, Color.BLUE));
		gameButton.setOpacity(0);
		root.getChildren().add(gameButton);
		
		Platform.runLater(() -> {
			displayGames();
		});
		
		if (PSWG.PREFS.getBoolean("open_on_launch", false))
			gameController.show();
	}
	
	public void displayGames()
	{
		PSWG.log("Displaying games");
		
		Preferences prefs = Preferences.userNodeForPackage(PSWG.class);
		double rootHeight = root.boundsInParentProperty().get().getHeight();
		double rootWidth = root.boundsInParentProperty().get().getWidth();
		PSWG.log("display.size: " + display.size());
		double notch = rootWidth / (1 + display.size());

		for (int i = 0; i < display.size(); i++) {
			
			Instance swg = display.get(i);
			Button gameButton = swg.getGameButton();
			double buttonHeight = gameButton.boundsInParentProperty().get().getHeight();
			double buttonWidth = gameButton.boundsInParentProperty().get().getWidth();
			gameButton.setLayoutY(rootHeight / 2 - buttonHeight /2);
			gameButton.setLayoutX(notch * (i + 1) - buttonWidth / 2);
			
			switch (prefs.getInt("animation", 2)) {
			case MainController.ANIMATION_NONE:
				gameButton.setOpacity(1);
				break;
				
			case MainController.ANIMATION_LOW:
				gameButton.setOpacity(1);
				break;
				
			case MainController.ANIMATION_HIGH:
				gameButton.setTranslateX(rootWidth);
				gameButton.setOpacity(1);
				ParallelTransition parallelTransition = new ParallelTransition();
				final Timeline slideLeft = new Timeline();
				final KeyValue newMsgKV = new KeyValue(gameButton.translateXProperty(), 0, Interpolator.EASE_OUT);
				final KeyFrame newMsgKF = new KeyFrame(Duration.millis(SLIDE_DURATION), newMsgKV);
				slideLeft.getKeyFrames().add(newMsgKF);
				slideLeft.setOnFinished((e) -> {
					PSWG.log("after anim x: " + gameButton.getLayoutX());
				});
				
				parallelTransition.getChildren().addAll(slideLeft);
				parallelTransition.play();
				break;
			}
		}
	}
}
