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

import javafx.scene.Group;
import javafx.stage.Stage;

import com.projectswg.launchpad.controller.GameController;
import com.projectswg.launchpad.service.GameService;

public class Instance
{
	private GameService gameService;
	private GameController gameController;
	private Group gameButtonGroup;
	private Stage stage;
	private String label;
	
	
	public Instance(GameService gameService)
	{
		this.gameService = gameService;
	}
	
	public void setGameButtonGroup(Group gameButtonGroup)
	{
		this.gameButtonGroup = gameButtonGroup;
	}
	
	public Group getGameButtonGroup()
	{
		return gameButtonGroup;
	}
	
	public void setGameController(GameController gameController)
	{
		this.gameController = gameController;
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
	
	public void setLabel(String label)
	{
		this.label = label;
	}
	
	public String getLabel()
	{
		return label;
	}
}
