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
import com.projectswg.launchpad.extras.ExtraModule;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.TilePane;

public class ExtrasController implements ModalComponent
{
	@FXML
	private TilePane extrasRoot;
	
	public static final String LABEL = "Extras";
	
	private MainController mainController;
	private EventHandler<MouseEvent> buttonHover;
	private EventHandler<MouseEvent> buttonPress;
	
	public ExtrasController() {}
	
	public void addExtra(ExtraModule module)
	{
		Button button = module.createButton();
		// css?
		button.setOnMouseEntered(buttonHover);
		button.setOnMouseClicked(buttonPress);
		button.setPrefHeight(90);
		button.setPrefWidth(90);
		extrasRoot.getChildren().add(button);
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {}

	@Override
	public Parent getRoot() { return extrasRoot; }

	@Override
	public void init(MainController mainController)
	{
		this.mainController = mainController;
		
		extrasRoot.getChildren().clear();
		
		buttonHover = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				ProjectSWG.playSound("button_hover");
			}
		};
		
		buttonPress = new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				ProjectSWG.playSound("button_press");
			}
		};
	}
	
	@Override
	public String getLabel() { return LABEL; }
}
