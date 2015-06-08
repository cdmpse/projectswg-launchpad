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






















import com.projectswg.launchpad.PSWG;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ModalController implements FxmlController
{
	private static final double MODAL_OPACITY = 0.98;
	
	MainController mainController;
	
	@FXML
	private Label modalLabel;
	
	@FXML
	private VBox modalRoot;
	
	@FXML
	private Button closeButton;
	
	@FXML
	private StackPane modalStackPane;
	
	@FXML
	public SimpleStyleableDoubleProperty opacity;
	
	private ModalComponent modalComponent;
	
	
	public ModalController()
	{
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1)
	{
	}
	
	@Override
	public Parent getRoot()
	{
		return modalRoot;
	}

	public MainController getMain()
	{
		return mainController;
	}
	
	public void init(MainController mainController)
	{
		this.mainController = mainController;
		closeButton.setOnAction((e) -> {
			hide();
		});
		modalRoot.setVisible(false);
	}
	
	public void addComponent(ModalComponent component)
	{
		modalStackPane.getChildren().add(component.getRoot());
		component.getRoot().setVisible(false);
	}
	
	public void loadComponent(ModalComponent modalComponent)
	{
		if (modalComponent == null)
			return;
		
		this.modalComponent = modalComponent;
		modalComponent.getRoot().setVisible(true);
		modalLabel.setText(modalComponent.getLabel());

		modalRoot.setOpacity(0);
		modalRoot.setOpacity(MODAL_OPACITY);
		modalRoot.setVisible(true);
	}
	
	public void showWithComponent(ModalComponent modalComponent)
	{
		if (modalComponent == null)
			return;
		
		this.modalComponent = modalComponent;
		modalComponent.getRoot().setVisible(true);
		modalLabel.setText(modalComponent.getLabel());

		modalRoot.setOpacity(0);
		switch (mainController.getAnimationLevel().getValue()) {
		case MainController.ANIMATION_NONE:
			modalRoot.setOpacity(MODAL_OPACITY);
			//modalRoot.setOpacity(initialOpacity);
			modalRoot.setVisible(true);
			break;
			
		case MainController.ANIMATION_LOW:
			modalRoot.setVisible(true);
			final Timeline longFadeIn = new Timeline();
			final KeyValue longFadeInKV = new KeyValue(modalRoot.opacityProperty(), MODAL_OPACITY, Interpolator.EASE_BOTH);
			//final KeyValue longFadeInKV = new KeyValue(modalRoot.opacityProperty(), initialOpacity, Interpolator.EASE_BOTH);
			final KeyFrame longFadeInKF = new KeyFrame(Duration.millis(MainController.FADE_DURATION), longFadeInKV);
			longFadeIn.getKeyFrames().add(longFadeInKF);
			longFadeIn.play();
			break;
			
		case MainController.ANIMATION_HIGH:
			modalRoot.setVisible(true);
			//scale
			final ScaleTransition scaleUp = new ScaleTransition(Duration.millis(MainController.SLIDE_DURATION), modalRoot);
			scaleUp.setFromX(0);
			scaleUp.setFromY(0);
			scaleUp.setToX(1);
			scaleUp.setToY(1);
			// fade
			final Timeline shortFadeIn = new Timeline();
			final KeyValue shortFadeInKV = new KeyValue(modalRoot.opacityProperty(), MODAL_OPACITY, Interpolator.EASE_BOTH);
			//final KeyValue shortFadeInKV = new KeyValue(modalRoot.opacityProperty(), initialOpacity, Interpolator.EASE_BOTH);
			final KeyFrame shortFadeInKF = new KeyFrame(Duration.millis(MainController.FADE_DURATION), shortFadeInKV);
			shortFadeIn.getKeyFrames().add(shortFadeInKF);
			// combine and play
			ParallelTransition parallelTransition = new ParallelTransition();
			parallelTransition.getChildren().addAll(scaleUp, shortFadeIn);
			parallelTransition.play();
			break;
		}
	}
	
	public void hide()
	{
		if (modalComponent == null) {
			PSWG.log("modalScreen::hide : null component");
			return;
		}

		//mainRoot.setDisable(false);
		double initialOpacity = modalRoot.getOpacity();
		
		switch (mainController.getAnimationLevel().getValue()) {
		case MainController.ANIMATION_NONE:
			modalRoot.setVisible(false);
			modalComponent.getRoot().setVisible(false);
			break;
		
		case MainController.ANIMATION_LOW:
			final Timeline longFadeOut = new Timeline();
			final KeyValue longFadeOutKV = new KeyValue(modalRoot.opacityProperty(), 0, Interpolator.EASE_BOTH);
			final KeyFrame longFadeOutKF = new KeyFrame(Duration.millis(MainController.SLIDE_DURATION), longFadeOutKV);
			longFadeOut.getKeyFrames().add(longFadeOutKF);
			longFadeOut.setOnFinished((e) -> {
				modalRoot.setVisible(false);
				modalRoot.setOpacity(initialOpacity);
				modalComponent.getRoot().setVisible(false);
			});
			longFadeOut.play();
			break;
			
		case MainController.ANIMATION_HIGH:
			// scale
			final ScaleTransition scaleDown = new ScaleTransition(Duration.millis(MainController.SLIDE_DURATION), modalRoot);
			scaleDown.setFromX(1);
			scaleDown.setFromY(1);
			scaleDown.setToX(0);
			scaleDown.setToY(0);
			// fade
			final Timeline fadeOut = new Timeline();
			final KeyValue kv = new KeyValue(modalRoot.opacityProperty(), 0, Interpolator.EASE_BOTH);
			final KeyFrame kf = new KeyFrame(Duration.millis(MainController.SLIDE_DURATION), kv);
			fadeOut.getKeyFrames().add(kf);
			double delay = ((delay = MainController.SLIDE_DURATION - MainController.FADE_DURATION) < 0 ? 0 : delay);
			fadeOut.setDelay(Duration.millis(delay));
			// combine and play
			ParallelTransition parallelTransition = new ParallelTransition();
			parallelTransition.getChildren().addAll(scaleDown, fadeOut);
			parallelTransition.setOnFinished((e) -> {
				modalRoot.setVisible(false);
				modalRoot.setOpacity(initialOpacity);
				modalComponent.getRoot().setVisible(false);
			});
			parallelTransition.play();
			break;
		}
	}
}
