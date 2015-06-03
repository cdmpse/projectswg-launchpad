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

import com.projectswg.launchpad.PSWG;
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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
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
	
	//private TextFlow textFlow;
	//private Text upArrow, downArrow;
	private Label swgLabel, pswgLabel;
	
	private Button setupCompleteButton;
	private ModalController modalController;
	private MainController mainController;
	private DropShadow redGlow, greenGlow;
	private Blend blend;
	private NodeDisplay setupDisplay;
	
	
	public SetupController()
	{
		blend = new Blend(BlendMode.MULTIPLY);
		blend.setBottomInput(new DropShadow(5, Color.RED));
		blend.setTopInput(new InnerShadow(8, Color.RED));

		swgLabel = new Label(MainController.UP_ARROW);
		pswgLabel = new Label(MainController.DOWN_ARROW);
		
		/*
		textFlow = new TextFlow();
		
		upArrow = new Text(MainController.UP_ARROW);
		upArrow.setStyle("-fx-font-size: 36;");
		
		downArrow = new Text(MainController.DOWN_ARROW);
		downArrow.setStyle("-fx-font-size: 36;");
		*/
		
		setupCompleteButton = new Button("Setup Complete!");
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1)
	{
	}
	
	@Override
	public void init(ModalController modalController)
	{
		this.modalController = modalController;
		mainController = modalController.getMain();
		Manager manager = mainController.getManager();
		
		setupDisplay = new NodeDisplay(setupDisplayPane);
		
		swgFolderButton.setOnAction((e) -> {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select SWG folder");
			File file = directoryChooser.showDialog(mainController.getStage());
			if (file == null || !file.isDirectory())
				return;
			
			String swgPath =  file.getAbsolutePath();
			swgFolderTextField.setText(swgPath);
			manager.scanSwg(swgPath);
		});
		
		pswgFolderButton.setOnAction((e) -> {
			DirectoryChooser directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select ProjectSWG folder");
			File file = directoryChooser.showDialog(mainController.getStage());
			if (file == null || !file.isDirectory())
				return;

			String pswgPath = file.getAbsolutePath();
			pswgFolderTextField.setText(pswgPath);
			manager.getPswgFolder().set(pswgPath);
		});
		

		manager.getSwgFolder().addListener((observable, oldValue, newValue) -> {
			if (newValue.equals(""))
				swgFolderNotSet();
			else
				if (mainController.getManager().getPswgFolder().getValue().equals(""))
					pswgFolderNotSet();
				else
					updateRequired();
		});
		
		manager.getPswgFolder().addListener((observable, oldValue, newValue) -> {
			if (manager.getSwgFolder().getValue().equals(""))
				return;
			if (newValue.equals(""))
				pswgFolderNotSet();
			else
				updateRequired();
		});

		setupCompleteButton.setOnAction((e) -> {
			modalController.hide();
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
	
	public void swgFolderNotSet()
	{
		PSWG.log("SetupComponent::swgFolderNotSet");
		
		swgFolderTextField.setEffect(blend);
		swgFolderButton.setDisable(false);
	
		pswgFolderButton.setDisable(true);
		pswgFolderTextField.setEffect(null);
		
		//textFlow.getChildren().clear();
		//textFlow.getChildren().add(upArrow);
		
		//setupDisplay.queueNode(textFlow);
	}
	
	public void pswgFolderNotSet()
	{
		PSWG.log("SetupComponent::pswgFolderNotSet");
	
		swgFolderTextField.setEffect(greenGlow);
		swgFolderButton.setDisable(false);
		
		pswgFolderButton.setDisable(false);
		pswgFolderTextField.setEffect(redGlow);
		
		//textFlow.getChildren().clear();
		//textFlow.getChildren().add(downArrow);
		
		//setupDisplay.queueNode(textFlow);
	}
	
	public void updateRequired()
	{
		PSWG.log("SetupComponent::updateRequired");

		swgFolderTextField.setEffect(greenGlow);
		swgFolderButton.setDisable(false);
		
		pswgFolderButton.setDisable(false);
		pswgFolderTextField.setEffect(greenGlow);
		setupDisplay.queueNode(setupCompleteButton);
	}
	
	@Override
	public void onShow()
	{
		Manager manager = mainController.getManager();
		
		if (manager.getSwgFolder().getValue().equals(""))
			swgFolderNotSet();
		else if (manager.getSwgFolder().getValue().equals(""))
			pswgFolderNotSet();
	
		swgFolderTextField.setText(manager.getSwgFolder().getValue());
		pswgFolderTextField.setText(manager.getPswgFolder().getValue());
	}
}
