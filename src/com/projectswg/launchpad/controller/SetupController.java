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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.projectswg.launchpad.ProjectSWG;
import com.projectswg.launchpad.service.Manager;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

public class SetupController implements ModalComponent
{
	@FXML
	private VBox setupRoot;
	@FXML
	private HBox stepOneHBox, stepTwoHBox;
	@FXML
	private Label stepOneLabel, stepTwoLabel;
	@FXML
	private Button pswgFolderButton, swgFolderButton;
	@FXML
	private TextField swgFolderTextField, pswgFolderTextField;	
	
	public static final String LABEL = "Setup";
	
	private MainController mainController;
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {}
	
	@Override
	public void init(MainController mainController)
	{
		this.mainController = mainController;
		Manager manager = mainController.getManager();
		
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
		
		final Tooltip swgFolderButtonTooltip = new Tooltip("Select the original Star Wars Galaxies installation folder.");
		swgFolderButton.setTooltip(swgFolderButtonTooltip);
		
		pswgFolderButton.setOnAction((e) -> {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select ProjectSWG folder");
			File file = directoryChooser.showDialog(mainController.getStage());
			if (file == null || !file.isDirectory())
				return;

			String pswgPath = file.getAbsolutePath();
			manager.getPswgFolder().set(pswgPath);
		});
		
		final Tooltip pswgFolderButtonTooltip = new Tooltip("Select an existing ProjectSWG folder or create a new one.");
		pswgFolderButton.setTooltip(pswgFolderButtonTooltip);

		manager.getState().addListener((observable, oldValue, newValue) -> {
			switch (newValue.intValue()) {
			case Manager.STATE_INIT:
			case Manager.STATE_SWG_SETUP_REQUIRED:
				if (!swgFolderTextField.getStyleClass().contains("fail"))
					swgFolderTextField.getStyleClass().add("fail");

				stepOneLabel.setOpacity(1);
				stepTwoHBox.setDisable(true);
				stepTwoHBox.setOpacity(0.25);
				stepTwoLabel.setEffect(null);
				break;

			case Manager.STATE_SWG_SCANNING:
				if (!swgFolderTextField.getStyleClass().contains("fail"))
					swgFolderTextField.getStyleClass().add("fail");
				
				stepOneLabel.setOpacity(1);
				stepTwoHBox.setDisable(true);
				stepTwoHBox.setOpacity(0.25);
				stepTwoLabel.setEffect(null);
				break;
				
			case Manager.STATE_PSWG_SETUP_REQUIRED:
				if (swgFolderTextField.getStyleClass().contains("fail"))
					swgFolderTextField.getStyleClass().remove("fail");
				if (!swgFolderTextField.getStyleClass().contains("pass"))
					swgFolderTextField.getStyleClass().add("pass");
				if (!pswgFolderTextField.getStyleClass().contains("fail"))
					pswgFolderTextField.getStyleClass().add("fail");
				
				stepOneLabel.setOpacity(0.5);
				stepTwoHBox.setDisable(false);
				stepTwoHBox.setOpacity(1);
				stepTwoLabel.setEffect(null);
				break;
				
			default:
				if (mainController.getModalController().getModalComponent() != null)
					if (mainController.getModalController().getModalComponent().getLabel().equals("Setup"))
						mainController.getModalController().hide();
			}
		});
	}
	
	@Override
	public String getLabel() { return LABEL; }
	@Override
	public Parent getRoot() { return setupRoot; }
}
