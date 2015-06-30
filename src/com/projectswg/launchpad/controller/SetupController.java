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
import java.net.URL;
import java.util.ResourceBundle;

import com.projectswg.launchpad.ProjectSWG;
import com.projectswg.launchpad.service.Manager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

public class SetupController implements ModalComponent
{
	@FXML
	private VBox setupRoot;
	
	@FXML
	private Button pswgFolderButton, swgFolderButton;
	
	@FXML
	private TextField swgFolderTextField, pswgFolderTextField;	
	
	@FXML
	private Pane setupDisplayPane;
	
	public static final String LABEL = "Setup";
	
	private Label swgLabel, pswgLabel;
	private MainController mainController;
	private Blend redGlow, greenGlow;
	private NodeDisplay setupDisplay;
	private ProgressIndicator progressIndicator;
	
	
	public SetupController()
	{
		redGlow = new Blend(BlendMode.MULTIPLY);
		redGlow.setBottomInput(new InnerShadow(5, Color.RED));
		redGlow.setTopInput(new InnerShadow(5, Color.RED));

		greenGlow = new Blend(BlendMode.MULTIPLY);
		greenGlow.setBottomInput(new DropShadow(5, Color.GREEN));
		greenGlow.setTopInput(new InnerShadow(5, Color.GREEN));
		
		swgLabel = new Label(ProjectSWG.UP_ARROW);
		pswgLabel = new Label(ProjectSWG.DOWN_ARROW);
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1)
	{
	}
	
	@Override
	public void init(MainController mainController)
	{
		this.mainController = mainController;
		Manager manager = mainController.getManager();
		
		setupDisplay = new NodeDisplay(setupDisplayPane);
		
		swgFolderTextField.textProperty().bind(manager.getSwgFolder());
		pswgFolderTextField.textProperty().bind(manager.getPswgFolder());
		
		swgFolderButton.setOnAction((e) -> {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select SWG folder");
			File file = directoryChooser.showDialog(mainController.getStage());
			if (file == null || !file.isDirectory())
				return;
			
			String swgPath =  file.getAbsolutePath();
			manager.getSwgFolder().set(null);
			manager.getSwgFolder().set(swgPath);
		});
		
		pswgFolderButton.setOnAction((e) -> {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select ProjectSWG folder");
			File file = directoryChooser.showDialog(mainController.getStage());
			if (file == null || !file.isDirectory())
				return;

			String pswgPath = file.getAbsolutePath();
			manager.getPswgFolder().set(pswgPath);
		});
		
		progressIndicator = new ProgressIndicator();
		progressIndicator.setManaged(false);
		progressIndicator.resize(15, 15);
		
		manager.getState().addListener((observable, oldValue, newValue) -> {
			switch (newValue.intValue()) {
			case Manager.STATE_INIT:
				swgFolderTextField.setEffect(redGlow);
				pswgFolderTextField.setEffect(redGlow);
				setupDisplay.queueNode(swgLabel);
				break;
				
			case Manager.STATE_SWG_SETUP_REQUIRED:
				setupDisplay.queueNode(swgLabel);
				swgFolderTextField.setEffect(redGlow);
				if (pswgFolderTextField.getText().equals(""))
					pswgFolderTextField.setEffect(redGlow);
				else
					pswgFolderTextField.setEffect(greenGlow);
				pswgFolderButton.setDisable(true);
				break;
				
			case Manager.STATE_SWG_SCANNING:
				swgFolderTextField.setEffect(redGlow);
				if (pswgFolderTextField.getText().equals(""))
					pswgFolderTextField.setEffect(redGlow);
				else
					pswgFolderTextField.setEffect(greenGlow);
				setupDisplay.queueNode(progressIndicator);
				break;
				
			case Manager.STATE_PSWG_SETUP_REQUIRED:
				setupDisplay.queueNode(pswgLabel);
				swgFolderTextField.setEffect(greenGlow);
				pswgFolderTextField.setEffect(redGlow);
				pswgFolderButton.setDisable(false);
				break;
				
			default:
				if (mainController.getModalController().getModalComponent() != null)
					if (mainController.getModalController().getModalComponent().getLabel().equals("Setup"))
						mainController.getModalController().hide();
			}
		});
	}
	
	@Override
	public String getLabel()
	{
		return LABEL;
	}
	
	@Override
	public Parent getRoot()
	{
		return setupRoot;
	}
}
