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

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;






import com.projectswg.launchpad.PSWG;





import com.projectswg.launchpad.service.GameService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class GameController implements FxmlController
{
	@FXML
	private VBox gameRoot;
	
	@FXML
	private Button clearButton, saveAsButton, findButton, stopButton;
	
	@FXML
	private TextArea outputTextArea;
	
	@FXML
	private TextField findTextField;
	
	@FXML
	private Pane gameStatusPane;
	
	private int lastFind;
	private NodeDisplay gameStatusDisplay;
	private ContextMenu contextMenu;
	private GameService gameService;
	private Stage stage;
	private Button gameButton;
	private boolean debug;
	private Blend redGlow;
	
	
	public GameController()
	{
		lastFind = 0;
		debug = false;
		
		redGlow = new Blend(BlendMode.MULTIPLY);
		redGlow.setBottomInput(new DropShadow(5, Color.RED));
		redGlow.setTopInput(new InnerShadow(8, Color.WHITE));
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1)
	{
	}

	public void show()
	{
		this.stage.show();

		TextFlow tf = new TextFlow(new Text(MainController.WHITE_CIRCLE));
		Platform.runLater(() -> {
			if (gameService.isRunning())
				tf.setEffect(new DropShadow(5, Color.BLUE));
			else
				tf.setEffect(new DropShadow(5, Color.GRAY));
			gameStatusDisplay.queueNode(tf);
		});
	}
	
	public void init()
	{
		gameStatusDisplay = new NodeDisplay(gameStatusPane);
		
		clearButton.setOnAction((e) -> {
			outputTextArea.clear();
		});
		
		saveAsButton.setOnAction((e) -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save output...");
			fileChooser.setInitialFileName("log.txt");
			File file = fileChooser.showSaveDialog(stage);
			if (file == null)
				return;
			
			String savePath =  file.getAbsolutePath();
			try {
				PrintWriter fout = new PrintWriter(savePath);
				fout.write(outputTextArea.getText());
				fout.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		
		findTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			lastFind = 0;
			findTextField.setEffect(null);
		});
		
		findButton.setOnAction((e) -> {
			findFunc(findTextField.getText());
		});
		
		stopButton.setOnAction((e) -> {
			if (gameService.isRunning())
				gameService.cancel();
		});
	}
	
	public void setGameService(GameService gameService)
	{
		this.gameService = gameService;
		if (Preferences.userNodeForPackage(PSWG.class).getBoolean("debug", false)) {
			gameService.getMainOut().addListener((observable, oldValue, newValue) -> {
				outputTextArea.appendText(newValue + "\n");
			});
		} else {
			outputTextArea.setText("debug not set");
		}
		
		gameService.runningProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue)
				return;
			stopButton.setDisable(true);
			
			TextFlow tf = new TextFlow(new Text(MainController.BLACK_CIRCLE));
			tf.setEffect(new DropShadow(5, Color.GRAY));
			Platform.runLater(() -> {
				gameStatusDisplay.queueNode(tf);
			});
		});
	}
	
	public void findFunc(String s)
	{
		if (s.equals(""))
			return;
		lastFind = outputTextArea.getText().indexOf(s, lastFind);
		if (lastFind == -1) {
			lastFind = 0;

			findTextField.setEffect(redGlow);
			return;
		}
		
		Platform.runLater(() -> {
			outputTextArea.selectRange(lastFind, lastFind + s.length());
			lastFind += s.length();
		});
	}
	
	@Override
	public Parent getRoot()
	{
		return gameRoot;
	}
	
	public void setStage(Stage stage)
	{
		this.stage = stage;
	}
	
	public Stage getStage()
	{
		return stage;
	}
	
	public void setGameButton(Button gameButton)
	{
		this.gameButton = gameButton;
	}
	
	public Button getStopButton()
	{
		return stopButton;
	}
	
	public GameService getGameService()
	{
		return gameService;
	}
	
	public Button getGameButton()
	{
		return gameButton;
	}
}
