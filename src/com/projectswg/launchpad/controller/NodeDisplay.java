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

import java.util.ArrayList;
import com.projectswg.launchpad.PSWG;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

public class NodeDisplay
{
	// add delay option
	// add format option
	
	private static final int SLIDE_DURATION = 100;
	private static final int FADE_DURATION = 100;

	private volatile ArrayList<Parent> queue;
	private volatile boolean busy;
	
	private Pane root;
	
	
	public NodeDisplay(Pane root)
	{
		this.root = root;
		busy = false;
		queue = new ArrayList<>();
	}
	
	public Pane getRoot()
	{
		return root;
	}
	
	public void queueNode(Parent node)
	{
		queue.add(node);
		if (!busy)
			processNextFromQueue();
	}

	public void queueString(String s)
	{
		if (s == null)
			return;

		TextFlow textFlow = new TextFlow();
		Text text = new Text(s);
		
		PSWG.log("string queued: " + s);
		
		textFlow.getChildren().add(text);
		queueNode(textFlow);
	}
	
	private void processNextFromQueue()
	{
		if (root == null) {
			PSWG.log("NodeDisplay root was null");
			return;
		}
		
		if (queue.size() == 0) {
			busy = false;
			return;
		} else if (queue.size() > 2)
			queue.subList(0, queue.size() - 2).clear();

		final Parent prevNode = (root.getChildren().size() > 0) ? (Parent)root.getChildren().get(0) : null;
		final Parent queuedNode = queue.remove(0);
		
		if (queuedNode == null) {
			PSWG.log("NodeDisplay::displayNextFromQueue : queuedNode = null");
			return;
		}
		
		if (queuedNode == prevNode) {
			PSWG.log("NodeDisplay::displayNextFromQueue : queuedNode = prevNode");
			return;
		}
		
		busy = true;
		queuedNode.setOpacity(0);
		
		Platform.runLater(() -> {
			root.getChildren().add(queuedNode);
		});

		Platform.runLater(() -> {
			queuedNode.setLayoutX(root.getBoundsInParent().getWidth() / 2 - queuedNode.boundsInParentProperty().get().getWidth() / 2);
			queuedNode.setLayoutY(root.getBoundsInParent().getHeight() / 2 - queuedNode.boundsInParentProperty().get().getHeight() / 2);
		});
		
		//PSWG.log("x mid: " + (root.getBoundsInParent().getWidth() / 2 - queuedNode.boundsInParentProperty().get().getWidth() / 2));
		//PSWG.log("y mid: " + (root.getBoundsInParent().getHeight() / 2 - queuedNode.boundsInParentProperty().get().getHeight() / 2));
		
		Platform.runLater(() -> {
			displayNode(prevNode, queuedNode);
		});
	}
	
	public void displayNode(Parent prevNode, Parent queuedNode)
	{
		switch (PSWG.PREFS.getInt("animation", 2)) {
		
		case MainController.ANIMATION_NONE:
			if (prevNode != null)
				root.getChildren().remove(0);
			queuedNode.setOpacity(1);
			processNextFromQueue();
			break;
		
		case MainController.ANIMATION_LOW:
			final Timeline fade = new Timeline();
			if (prevNode != null) {
				final KeyValue fadeOutKV = new KeyValue(prevNode.opacityProperty(), 0, Interpolator.EASE_BOTH);
				final KeyFrame fadeOutKF = new KeyFrame(Duration.millis(FADE_DURATION), fadeOutKV);
				fade.getKeyFrames().add(fadeOutKF);
			}
			final KeyValue fadeInKV = new KeyValue(queuedNode.opacityProperty(), 1, Interpolator.EASE_BOTH);
			final KeyFrame fadeInKF = new KeyFrame(Duration.millis(FADE_DURATION), fadeInKV);
			fade.getKeyFrames().add(fadeInKF);
			
			fade.setOnFinished((e) -> {
				if (prevNode != null)
					root.getChildren().remove(0);
				processNextFromQueue();
			});
			fade.play();
			break;
			
		case MainController.ANIMATION_HIGH:
			ParallelTransition parallelTransition = new ParallelTransition();
			final Timeline slideAndFadeIn = new Timeline();
			
			if (prevNode != null) {
				final Timeline delayedFadeOut = new Timeline();
				
				final KeyValue delayedFadeOutKV = new KeyValue(prevNode.opacityProperty(), 0, Interpolator.EASE_BOTH);
				final KeyFrame delayedFadeOutKF = new KeyFrame(Duration.millis(FADE_DURATION), delayedFadeOutKV);
				delayedFadeOut.getKeyFrames().add(delayedFadeOutKF);
				delayedFadeOut.setDelay(Duration.millis(SLIDE_DURATION - FADE_DURATION));
				parallelTransition.getChildren().add(delayedFadeOut);
				
				final KeyValue oldSlideUpKV = new KeyValue(prevNode.translateYProperty(), -root.boundsInParentProperty().get().getHeight(), Interpolator.EASE_OUT);
				final KeyFrame oldSlideUpKF = new KeyFrame(Duration.millis(SLIDE_DURATION), oldSlideUpKV);
				slideAndFadeIn.getKeyFrames().add(oldSlideUpKF);
			}
			
			final KeyValue shortFadeInKV = new KeyValue(queuedNode.opacityProperty(), 1, Interpolator.EASE_BOTH);
			final KeyFrame shortFadeInKF = new KeyFrame(Duration.millis(FADE_DURATION), shortFadeInKV);
			slideAndFadeIn.getKeyFrames().add(shortFadeInKF);
			queuedNode.setTranslateY(root.boundsInParentProperty().get().getHeight());
			final KeyValue newSlideUpKV = new KeyValue(queuedNode.translateYProperty(), 0, Interpolator.EASE_OUT);
			final KeyFrame newSlideUpKF = new KeyFrame(Duration.millis(SLIDE_DURATION), newSlideUpKV);
			slideAndFadeIn.getKeyFrames().add(newSlideUpKF);
			parallelTransition.setOnFinished((e) -> {
				if (prevNode != null)
					root.getChildren().remove(0);
				processNextFromQueue();
			});
			parallelTransition.getChildren().addAll(slideAndFadeIn);
			parallelTransition.play();
			break;
		}
	}
}
