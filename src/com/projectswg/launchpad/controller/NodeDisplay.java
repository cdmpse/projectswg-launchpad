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

import com.projectswg.launchpad.ProjectSWG;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Group;
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

	private ArrayList<Parent> queue;
	private boolean busy;
	
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
	
	public void queueString(String s)
	{
		if (s == null)
			return;
		queueNode(new TextFlow(new Text(s)));
	}
	
	public void queueNode(Parent node)
	{
		queue.add(node);
		if (!busy)
			processNextFromQueue();
	}

	private void processNextFromQueue()
	{
		busy = true;
		
		if (root == null) {
			busy = false;
			return;
		}
	
		if (queue.size() == 0) {
			busy = false;
			return;
		} else if (queue.size() > 2)
			queue.subList(0, queue.size() - 2).clear();
	
		final Group prevGroup = (root.getChildren().size() > 0) ? (Group)root.getChildren().get(0) : null;
		final Parent queuedNode = queue.remove(0);
		final Group nextGroup = new Group(queuedNode);
		queuedNode.setOpacity(0);
		root.getChildren().add(nextGroup);
		
		Platform.runLater(() -> {
			// otherwise buttonGroup width is sometimes 0
			Platform.runLater(() -> {
				displayGroup(prevGroup, nextGroup);
			});
		});
	}
	
	public void displayGroup(Group prevGroup, Group nextGroup)
	{
		final Parent prevNode = ((prevGroup != null) && prevGroup.getChildren().size() > 0) ? (Parent)prevGroup.getChildren().get(0) : null;
		if (nextGroup.getChildren().size() == 0)
			return;

		final Parent nextNode = (Parent)nextGroup.getChildren().get(0);
		nextNode.setLayoutX(root.getWidth() / 2 - nextGroup.layoutBoundsProperty().getValue().getWidth() / 2);
		if (ProjectSWG.PREFS.getInt("animation", ProjectSWG.ANIMATION_HIGH) == ProjectSWG.ANIMATION_HIGH)
			nextNode.setLayoutY(root.getHeight());
		else
			nextNode.setLayoutY(root.getHeight() / 2 - nextGroup.layoutBoundsProperty().getValue().getHeight() / 2);

		switch (ProjectSWG.PREFS.getInt("animation", ProjectSWG.ANIMATION_HIGH)) {
		case ProjectSWG.ANIMATION_NONE:
			if (root.getChildren().size() > 1)
				root.getChildren().remove(0);
			nextNode.setOpacity(1);
			processNextFromQueue();
			break;
		
		case ProjectSWG.ANIMATION_LOW:
			final Timeline fade = new Timeline();
			if (prevNode != null) {
				final KeyValue fadeOutKV = new KeyValue(prevNode.opacityProperty(), 0, Interpolator.EASE_BOTH);
				final KeyFrame fadeOutKF = new KeyFrame(Duration.millis(FADE_DURATION), fadeOutKV);
				fade.getKeyFrames().add(fadeOutKF);
			}
			final KeyValue fadeInKV = new KeyValue(nextNode.opacityProperty(), 1, Interpolator.EASE_BOTH);
			final KeyFrame fadeInKF = new KeyFrame(Duration.millis(FADE_DURATION), fadeInKV);
			fade.getKeyFrames().add(fadeInKF);
			
			fade.setOnFinished((e) -> {
				if (root.getChildren().size() > 1)
					root.getChildren().remove(0);
				processNextFromQueue();
			});
			fade.play();
			break;
			
		case ProjectSWG.ANIMATION_HIGH:
			ParallelTransition parallelTransition = new ParallelTransition();
			final Timeline slideAndFadeIn = new Timeline();
			
			if (prevNode != null) {
				final Timeline slideAndFadeOut = new Timeline();
				final KeyValue delayedFadeOutKV = new KeyValue(prevNode.opacityProperty(), 0, Interpolator.EASE_BOTH);
				final KeyFrame delayedFadeOutKF = new KeyFrame(Duration.millis(FADE_DURATION), delayedFadeOutKV);
				slideAndFadeOut.getKeyFrames().add(delayedFadeOutKF);
				slideAndFadeOut.setDelay(Duration.millis(SLIDE_DURATION - FADE_DURATION));
				
				final KeyValue oldSlideUpKV = new KeyValue(prevNode.layoutYProperty(), 0, Interpolator.EASE_OUT);
				final KeyFrame oldSlideUpKF = new KeyFrame(Duration.millis(SLIDE_DURATION), oldSlideUpKV);
				slideAndFadeOut.getKeyFrames().add(oldSlideUpKF);
				
				parallelTransition.getChildren().add(slideAndFadeOut);
			}
			
			final KeyValue shortFadeInKV = new KeyValue(nextNode.opacityProperty(), 1, Interpolator.EASE_BOTH);
			final KeyFrame shortFadeInKF = new KeyFrame(Duration.millis(FADE_DURATION), shortFadeInKV);
			slideAndFadeIn.getKeyFrames().add(shortFadeInKF);

			final double midY = root.getHeight() / 2 - nextGroup.layoutBoundsProperty().getValue().getHeight() / 2;
			final KeyValue newSlideUpKV = new KeyValue(nextNode.layoutYProperty(), midY, Interpolator.EASE_OUT);
			final KeyFrame newSlideUpKF = new KeyFrame(Duration.millis(SLIDE_DURATION), newSlideUpKV);
			slideAndFadeIn.getKeyFrames().add(newSlideUpKF);

			parallelTransition.setOnFinished((e) -> {
				if (root.getChildren().size() > 1)
					root.getChildren().remove(0);
				processNextFromQueue();
			});
			
			parallelTransition.getChildren().addAll(slideAndFadeIn);
			parallelTransition.play();
			break;
		}
	}
}
