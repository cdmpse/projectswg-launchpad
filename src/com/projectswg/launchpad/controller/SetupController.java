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

import com.projectswg.launchpad.service.Manager;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
	private Button setupCompleteButton;
	private MainController mainController;
	private Blend redGlow, greenGlow;
	private NodeDisplay setupDisplay;
	
	
	public SetupController()
	{
		redGlow = new Blend(BlendMode.MULTIPLY);
		redGlow.setBottomInput(new DropShadow(5, Color.RED));
		redGlow.setTopInput(new InnerShadow(8, Color.RED));

		greenGlow = new Blend(BlendMode.MULTIPLY);
		greenGlow.setBottomInput(new DropShadow(5, Color.GREEN));
		greenGlow.setTopInput(new InnerShadow(8, Color.GREEN));
		
		swgLabel = new Label(MainController.UP_ARROW);
		pswgLabel = new Label(MainController.DOWN_ARROW);
		
		setupCompleteButton = new Button("Setup Complete!");
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
		
		manager.getSwgReady().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				pswgFolderButton.setDisable(false);
				swgFolderTextField.setEffect(greenGlow);
				if (manager.getPswgFolder().getValue().equals("")) {
					setupDisplay.queueNode(pswgLabel);
					pswgFolderTextField.setEffect(redGlow);
				} else {
					setupDisplay.queueNode(setupCompleteButton);
				}
			} else {
				swgFolderTextField.setEffect(redGlow);
				pswgFolderTextField.setEffect(null);
				setupDisplay.queueNode(swgLabel);
				pswgFolderButton.setDisable(true);
			}
		});
		
		manager.getPswgFolder().addListener((observable, oldValue, newValue) -> {
			//if (newValue == null || manager.getSwgFolder().getValue() == null)
			//	return;
			if (!newValue.equals("")) {
				pswgFolderTextField.setEffect(greenGlow);
				if (!manager.getSwgFolder().getValue().equals("")) {
					setupDisplay.queueNode(setupCompleteButton);
				}
			} else {
				pswgFolderTextField.setEffect(redGlow);
			}
		});

		setupCompleteButton.setOnAction((e) -> {
			mainController.getModalController().hide();
		});
		
		// initial setup
		
		if (manager.getSwgReady().getValue()) {
			swgFolderTextField.setEffect(greenGlow);
		} else {
			setupDisplay.queueNode(swgLabel);
			swgFolderTextField.setEffect(redGlow);
			pswgFolderButton.setDisable(true);
		}
		if (!manager.getPswgFolder().getValue().equals(""))
			pswgFolderTextField.setEffect(greenGlow);
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
