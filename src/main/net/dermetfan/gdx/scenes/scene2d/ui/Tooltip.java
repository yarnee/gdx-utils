/** Copyright 2014 Robin Stumm (serverkorken@gmail.com, http://dermetfan.net)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. */

package net.dermetfan.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import net.dermetfan.gdx.scenes.scene2d.Scene2DUtils;

import static com.badlogic.gdx.scenes.scene2d.InputEvent.Type.enter;
import static com.badlogic.gdx.scenes.scene2d.InputEvent.Type.exit;
import static com.badlogic.gdx.scenes.scene2d.InputEvent.Type.mouseMoved;
import static com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchDown;
import static com.badlogic.gdx.scenes.scene2d.InputEvent.Type.touchUp;

/** Shows a tooltip under the pointer or at the event position after {@link #showDelay} and hides it on certain events.
 *  @author dermetfan */
public class Tooltip<T extends Actor> extends PositionedPopup<T> {

	/** Classically moves the position down by {@link #popup popup's} height, so the upper left corner of the popup lines up with the upper right corner of the actor this listener is attached to.
	 *  @author dermetfan
	 *  @since 0.4.0 */
	public class TooltipPosition implements Position {

		/** the position */
		private Position position;

		/** @param position the {@link #position} */
		public TooltipPosition(Position position) {
			this.position = position;
		}

		@Override
		public Vector2 apply(Event event) {
			return position.apply(event).sub(0, getPopup().getHeight());
		}

		// getters and setters

		/** @return the {@link #position} */
		public Position getPosition() {
			return position;
		}

		/** @param position the {@link #position} to set */
		public void setPosition(Position position) {
			this.position = position;
		}

	}

	/** calls {@link #show(Event)}
	 *  @author dermetfan */
	private class ShowTask extends Task {
		private final InputEvent event = new InputEvent();

		@Override
		public void run() {
			show(event);
		}
	}

	/** calls {@link #hide(Event)}
	 *  @author dermetfan */
	private class HideTask extends Task {
		private final InputEvent event = new InputEvent();

		@Override
		public void run() {
			hide(event);
		}
	}

	/** @see #show(Event)  */
	private final ShowTask showTask = new ShowTask();

	/** @see #hide(Event) */
	private final HideTask hideTask = new HideTask();

	/** the mask bits */
	public static final byte mask = 1;

	/** the flags that define when to hide, show or cancel the tooltip */
	private int showFlags = mask << enter.ordinal(), hideFlags = mask << touchDown.ordinal() | mask << touchUp.ordinal() | mask << exit.ordinal(), cancelFlags = mask << touchDown.ordinal() | mask << exit.ordinal();

	/** if the tooltip's position should be updated on {@link Type#mouseMoved mouseMoved} events */
	private boolean positionOnMouseMoved;

	/** the offset from the pointer position */
	private float offsetX, offsetY;

	/** the delay before {@link #show(Event)} */
	private float showDelay = .75f;

	/** the delay before {@link #hide(Event)} */
	private float hideDelay;

	/** the time the {@link #popup} will fade */
	private float fadeInDuration = .4f, fadeOutDuration = fadeInDuration;

	/** if not null, {@link #show(Event)} will set the touchability of the {@link #popup} to this */
	private Touchable showTouchable = Touchable.childrenOnly;

	/** if not null, {@link #hide(Event)} will set the touchability of the {@link #popup} to this */
	private Touchable hideTouchable = Touchable.disabled;

	/** @see Popup#Popup(Actor) */
	public Tooltip(T popup) {
		super(popup, null);
		setPosition(new TooltipPosition(new PointerPosition()));
	}

	/** @see PositionedPopup#PositionedPopup(Actor, Position) */
	public Tooltip(T popup, Position position) {
		super(popup, position);
	}

	@Override
	public boolean handle(Event e) {
		if(!(e instanceof InputEvent))
			return false;
		InputEvent event = (InputEvent) e;

		if(event.getRelatedActor() == getPopup())
			return false;

		Type type = event.getType();
		int flag = mask << type.ordinal();

		if(positionOnMouseMoved && type == mouseMoved) {
			Vector2 pos = getPosition().apply(event);
			if(getPopup().hasParent())
				Scene2DUtils.stageToLocalCoordinates(pos, getPopup().getParent());
			getPopup().setPosition(pos.x, pos.y);
		}

		if((cancelFlags & flag) == flag)
			showTask.cancel();

		if((hideFlags & flag) == flag) {
			Scene2DUtils.copy(hideTask.event, event);
			if(hideDelay > 0) {
				if(!hideTask.isScheduled())
					Timer.schedule(hideTask, hideDelay);
			} else
				hideTask.run();
		}

		if((showFlags & flag) == flag) {
			Scene2DUtils.copy(showTask.event, event);
			if(showDelay > 0) {
				if(!showTask.isScheduled())
					Timer.schedule(showTask, showDelay);
			} else
				showTask.run();
		}
		return false;
	}

	/** Brings the {@link #popup} {@link Actor#toFront() to front} and {@link Actions#fadeIn(float) fades} it in for {@link #fadeInDuration} seconds.
	 *  @param event The {@link ShowTask#event event} of {@link #showTask} was {@link Scene2DUtils#copy(InputEvent, InputEvent) copied} from the original event, so cancelling this has no effect. */
	@Override
	public boolean show(Event event) {
		super.show(event);
		SequenceAction sequence = Actions.sequence();
		if(showTouchable != null)
			sequence.addAction(Actions.touchable(showTouchable));
		sequence.addAction(Actions.fadeIn(fadeInDuration));
		getPopup().addAction(sequence);
		return false;
	}

	/** {@link Actions#fadeOut(float) Fades} the tooltip out for 0.4 seconds ({@code Dialog#fadeDuration when it still existed}).
	 *  @param event {@link Scene2DUtils#copy(InputEvent, InputEvent) copied} {@link HideTask#event} from {@link #hideTask}, so cancelling has no effect */
	@Override
	public boolean hide(Event event) {
		SequenceAction sequence = Actions.sequence();
		if(hideTouchable != null)
			sequence.addAction(Actions.touchable(hideTouchable));
		sequence.addAction(Actions.fadeOut(fadeOutDuration));
		sequence.addAction(Actions.hide());
		getPopup().addAction(sequence);
		return false;
	}

	/** @param flag the {@link Type} on which to show the tooltip */
	public void showOn(Type flag) {
		showFlags |= mask << flag.ordinal();
	}

	/** @param flag the {@link Type} on which not to show the tooltip */
	public void showNotOn(Type flag) {
		showFlags &= ~(mask << flag.ordinal());
	}

	/** @param flag the {@link Type} on which to hide the tooltip */
	public void hideOn(Type flag) {
		hideFlags |= mask << flag.ordinal();
	}

	/** @param flag the {@link Type} on which not to hide the tooltip */
	public void hideNotOn(Type flag) {
		hideFlags &= ~(mask << flag.ordinal());
	}

	/** @param flag the {@link Type} on which to cancel showing the tooltip */
	public void cancelOn(Type flag) {
		cancelFlags |= mask << flag.ordinal();
	}

	/** @param flag the {@link Type} on which to not cancel showing the tooltip */
	public void cancelNotOn(Type flag) {
		cancelFlags &= ~(mask << flag.ordinal());
	}

	/** never show the tooltip */
	public void showNever() {
		showFlags = 0;
	}

	/** never hide the tooltip */
	public void hideNever() {
		hideFlags = 0;
	}

	/** never cancel showing the tooltip */
	public void cancelNever() {
		cancelFlags = 0;
	}

	/** show the tooltip on any event */
	public void showAlways() {
		showFlags = ~0;
	}

	/** hide the tooltip on any event */
	public void hideAlways() {
		hideFlags = ~0;
	}

	/** cancel showing the tooltip on any event */
	public void cancelAlways() {
		cancelFlags = ~0;
	}

	// getters and setters

	/** @param delay the {@link #showDelay} and {@link #hideDelay} */
	public void setDelay(float delay) {
		showDelay = hideDelay = delay;
	}

	/** @return the {@link #positionOnMouseMoved} */
	public boolean isPositionOnMouseMoved() {
		return positionOnMouseMoved;
	}

	/** @param positionOnMouseMoved the {@link #positionOnMouseMoved} to set */
	public void setPositionOnMouseMoved(boolean positionOnMouseMoved) {
		this.positionOnMouseMoved = positionOnMouseMoved;
	}

	/** @return the {@link #offsetX} */
	public float getOffsetX() {
		return offsetX;
	}

	/** @param offsetX the {@link #offsetX} to set */
	public void setOffsetX(float offsetX) {
		this.offsetX = offsetX;
	}

	/** @return the {@link #offsetY} */
	public float getOffsetY() {
		return offsetY;
	}

	/** @param offsetY the {@link #offsetY} to set */
	public void setOffsetY(float offsetY) {
		this.offsetY = offsetY;
	}

	/** @return the {@link #showDelay} */
	public float getShowDelay() {
		return showDelay;
	}

	/** @param showDelay the {@link #showDelay} to set */
	public void setShowDelay(float showDelay) {
		this.showDelay = showDelay;
	}

	/** @return the {@link #hideDelay} */
	public float getHideDelay() {
		return hideDelay;
	}

	/** @param hideDelay the {@link #hideDelay} to set */
	public void setHideDelay(float hideDelay) {
		this.hideDelay = hideDelay;
	}

	/** @return the {@link #showTouchable} */
	public Touchable getShowTouchable() {
		return showTouchable;
	}

	/** @param showTouchable the {@link #showTouchable} to set */
	public void setShowTouchable(Touchable showTouchable) {
		this.showTouchable = showTouchable;
	}

	/** @return the {@link #hideTouchable} */
	public Touchable getHideTouchable() {
		return hideTouchable;
	}

	/** @param hideTouchable the {@link #hideTouchable} to set */
	public void setHideTouchable(Touchable hideTouchable) {
		this.hideTouchable = hideTouchable;
	}

	/** @return the {@link #showFlags} */
	public int getShowFlags() {
		return showFlags;
	}

	/** @param showFlags the {@link #showFlags} to set */
	public void setShowFlags(int showFlags) {
		this.showFlags = showFlags;
	}

	/** @return the {@link #hideFlags} */
	public int getHideFlags() {
		return hideFlags;
	}

	/** @param hideFlags the {@link #hideFlags} to set */
	public void setHideFlags(int hideFlags) {
		this.hideFlags = hideFlags;
	}

	/** @return the {@link #cancelFlags} */
	public int getCancelFlags() {
		return cancelFlags;
	}

	/** @param cancelFlags the {@link #cancelFlags} to set */
	public void setCancelFlags(int cancelFlags) {
		this.cancelFlags = cancelFlags;
	}

}