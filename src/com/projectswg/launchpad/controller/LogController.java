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

import com.projectswg.launchpad.ProjectSWG;









import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class LogController implements FxmlController
{
	@FXML
	private VBox logRoot;
	
	@FXML
	private Button clearButton, saveAsButton, findButton;
	
	@FXML
	private TextArea outputTextArea;
	
	@FXML
	private TextField findTextField;
	
	private int lastFind;
	private Stage stage;
	private Blend redGlow;
	private DebugListener debugListener;
	
	
	public LogController()
	{
		debugListener = new DebugListener();
		lastFind = 0;

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
	}
	
	public void init(Stage stage)
	{
		this.stage = stage;
		
		clearButton.setOnAction((e) -> {
			outputTextArea.clear();
		});
		
		saveAsButton.setOnAction((e) -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save debug...");
			fileChooser.setInitialFileName("debug.txt");
			File file = fileChooser.showSaveDialog(stage);
			if (file == null)
				return;
			
			String savePath =  file.getAbsolutePath();
			try {
				PrintWriter fout = new PrintWriter(savePath);
				fout.write(outputTextArea.getText());
				fout.close();
			} catch (Exception e1) {
				ProjectSWG.log(e1.toString());
			}
		});
		
		findTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			lastFind = 0;
			findTextField.setEffect(null);
		});
		
		findButton.setOnAction((e) -> {
			findFunc(findTextField.getText());
		});
		
		if (ProjectSWG.PREFS.getBoolean("debug", false))
			addDebugListener();
	}
	
	public void removeDebugListener()
	{
		ProjectSWG.DEBUG.removeListener(debugListener);
	}
	
	public void addDebugListener()
	{
		ProjectSWG.DEBUG.addListener(debugListener);
	}
	
	public void findFunc(String searchText)
	{
		if (searchText.equals(""))
			return;
		lastFind = outputTextArea.getText().toLowerCase().indexOf(searchText.toLowerCase(), lastFind);
		if (lastFind == -1) {
			lastFind = 0;
			findTextField.setEffect(redGlow);
			return;
		} else
			findTextField.setEffect(null);
		
		Platform.runLater(() -> {
			outputTextArea.selectRange(lastFind, lastFind + searchText.length());
			lastFind += searchText.length();
		});
	}
	
	@Override
	public Parent getRoot()
	{
		return logRoot;
	}
	
	public Stage getStage()
	{
		return stage;
	}
	
	private class DebugListener implements ChangeListener<String>
	{
		@Override
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			outputTextArea.appendText(newValue + "\n");
		}
	}
}
