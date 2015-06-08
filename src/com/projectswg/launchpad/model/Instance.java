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

package com.projectswg.launchpad.model;

import javafx.scene.control.Button;
import javafx.stage.Stage;

import com.projectswg.launchpad.controller.GameController;
import com.projectswg.launchpad.service.GameService;


public class Instance
{
	private GameService gameService;
	private GameController gameController;
	private Button gameButton;
	private Stage stage;
	
	public Instance(GameService gameService)
	{
		this.gameService = gameService;
	}
	
	public void setGameButton(Button gameButton)
	{
		this.gameButton = gameButton;
	}
	
	public Button getGameButton()
	{
		return gameButton;
	}
	
	public void setGameController(GameController gameController)
	{
		this.gameController = gameController;
		gameController.setGameService(gameService);
	}
	
	public GameController getGameController()
	{
		return gameController;
	}
	
	public GameService getGameService()
	{
		return gameService;
	}
	
	public Stage getStage()
	{
		return stage;
	}
	
	public void setStage(Stage stage)
	{
		this.stage = stage;
	}
}
