/**
 * Copyright 2013 Robin Stumm (serverkorken@googlemail.com, http://dermetfan.bplaced.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dermetfan.util.libgdx.box2d;

import net.dermetfan.util.Accessor;
import net.dermetfan.util.math.MathUtils;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Array;

/** Breaks bodies or fixtures if they get hit too hard. Put in fixture's or body's user data and set {@link Manager} as {@link ContactListener}.<br/>
 *  Don't forget to call {@link Manager#destroy()} after every world time step.<br/>
 *  You can manually destroy fixtures or bodies using the {@link Manager#destroy(Fixture, boolean, boolean)} and {@link Manager#destroy(Body)} methods.
 *  @author dermetfan */
public class Breakable {

	/** Manages the {@link Breakable Breakables} of the Contacts it receives. <strong>Do not forget to set as ContactListener and to call {@link #destroy()} after every world step.</strong>
	 *  @author dermetfan */
	public static class Manager implements ContactListener {

		/** the fixtures that broke in {@link #strain(Contact, ContactImpulse)} */
		public final Array<Fixture> brokenFixtures = new Array<Fixture>(1);

		/** the bodies that broke in {@link #strain(Contact, ContactImpulse)} */
		public final Array<Body> brokenBodies = new Array<Body>(1);

		/** the {@link #userDataAccessor} used by default */
		public static final Accessor defaultUserDataAccessor = new Accessor() {

			@SuppressWarnings("unchecked")
			@Override
			public Breakable access(Object userData) {
				return userData instanceof Breakable ? (Breakable) userData : null;
			}

		};

		/** the {@link Accessor} used to access a Breakable in user data ({@link Accessor#access(Object) access} must return a Breakable) */
		private Accessor userDataAccessor = defaultUserDataAccessor;

		/** instantiates a new {@link Manager} */
		public Manager() {
		}

		/** instantiates a new {@link Manager} with the given {@link #userDataAccessor} */
		public Manager(Accessor userDataAccessor) {
			setUserDataAccessor(userDataAccessor);
		}

		/** actually destroys all bodies in {@link #brokenBodies} and fixtures in {@link #brokenFixtures} */
		public void destroy() {
			for(Fixture fixture : brokenFixtures) {
				brokenFixtures.removeValue(fixture, true);
				fixture.getBody().destroyFixture(fixture);
			}
			for(Body body : brokenBodies) {
				brokenBodies.removeValue(body, true);
				body.getWorld().destroyBody(body);
			}
		}

		/** {@link #destroy(Fixture, boolean, boolean) destroys}/{@link #destroy(Body) destroys} all fixtures/bodies involved in the given Contact if they could not bear the given impulse */
		public void strain(Contact contact, ContactImpulse impulse) {
			float normalImpulse = MathUtils.sum(impulse.getNormalImpulses()), tangentImpulse = Math.abs(MathUtils.sum(impulse.getTangentImpulses()));

			Fixture fixtureA = contact.getFixtureA(), fixtureB = contact.getFixtureB();
			Breakable breakable = userDataAccessor.access(fixtureA.getUserData());
			if(breakable != null) {
				if(breakable.callback != null)
					breakable.callback.strained(fixtureA, breakable, contact, impulse, normalImpulse, tangentImpulse);
				if(normalImpulse > breakable.normalResistance || tangentImpulse > breakable.tangentResistance)
					destroy(fixtureA, breakable.breakBodyWithoutFixtures, breakable.breakBody);
			}

			breakable = userDataAccessor.access(fixtureB.getUserData());
			if(breakable != null) {
				if(breakable.callback != null)
					breakable.callback.strained(fixtureB, breakable, contact, impulse, normalImpulse, tangentImpulse);
				if(normalImpulse > breakable.normalResistance || tangentImpulse > breakable.tangentResistance)
					destroy(fixtureB, breakable.breakBodyWithoutFixtures, breakable.breakBody);
			}

			Body bodyA = fixtureA.getBody(), bodyB = fixtureB.getBody();
			breakable = userDataAccessor.access(bodyA.getUserData());
			if(breakable != null && (normalImpulse > breakable.normalResistance || tangentImpulse > breakable.tangentResistance))
				destroy(bodyA);

			breakable = userDataAccessor.access(bodyB.getUserData());
			if(breakable != null && (normalImpulse > breakable.normalResistance || tangentImpulse > breakable.tangentResistance))
				destroy(bodyB);
		}

		/** destroys the given fixture (and its body depending on {@link #breakBodyWithoutFixtures} and {@link #breakBody})
		 *  @param fixture the {@link Fixture} to destroy
		 *  @param breakBodyWithoutFixtures {@link #breakBodyWithoutFixtures}
		 *  @param breakBody {@link #breakBody} */
		public void destroy(Fixture fixture, boolean breakBodyWithoutFixtures, boolean breakBody) {
			if(brokenFixtures.contains(fixture, true))
				return;

			brokenFixtures.add(fixture);
			Breakable breakable = userDataAccessor.access(fixture.getUserData());
			if(breakable != null && breakable.callback != null)
				breakable.callback.destroyedFixture(fixture, breakable);

			Body body = fixture.getBody();
			if(brokenBodies.contains(body, true))
				return;

			if(breakBody) {
				destroy(body);
				return;
			}

			if(breakBodyWithoutFixtures) {
				for(Fixture bodyFixture : body.getFixtureList())
					if(!brokenFixtures.contains(bodyFixture, true))
						return;
				destroy(body);
			}
		}

		/** @param body the {@link Body} to destroy */
		public void destroy(Body body) {
			if(brokenBodies.contains(body, true))
				return;

			brokenBodies.add(body);

			Breakable breakable = userDataAccessor.access(body.getUserData());
			if(breakable != null && breakable.callback != null)
				breakable.callback.destroyedBody(body, breakable);
		}

		/** does nothing */
		@Override
		public void beginContact(Contact contact) {
		}

		/** does nothing */
		@Override
		public void preSolve(Contact contact, Manifold oldManifold) {
		}

		/** calls {@link #strain(Contact, ContactImpulse)} */
		@Override
		public void postSolve(Contact contact, ContactImpulse impulse) {
			strain(contact, impulse);
		}

		/** does nothing */
		@Override
		public void endContact(Contact contact) {
		}

		/** @return the {@link #brokenFixtures} */
		public Array<Fixture> getBrokenFixtures() {
			return brokenFixtures;
		}

		/** @return the {@link #brokenBodies} */
		public Array<Body> getBrokenBodies() {
			return brokenBodies;
		}

		/** @return the {@link #userDataAccessor} */
		public Accessor getUserDataAccessor() {
			return userDataAccessor;
		}

		/** @param userDataAccessor the {@link #userDataAccessor} to set */
		public void setUserDataAccessor(Accessor userDataAccessor) {
			if(userDataAccessor == null)
				throw new IllegalArgumentException("userDataAccessor must not be null");
			this.userDataAccessor = userDataAccessor;
		}

	}

	/** a callback for a {@link Breakable} if its container (body or fixture) was destroyed (for example to play a sound)
	 *  @author dermetfan */
	public static interface Callback {

		/** called by {@link Manager#strain(Contact, ContactImpulse)}
		 *  @param fixture the strained fixture
		 *  @param breakable the Breakable instance that called this Callback
		 *  @param contact the straining contact
		 *  @param impulse the straining ContactImpulse
		 *  @param normalImpulse the sum of the normal impulses of impulse
		 *  @param tangentImpulse the sum of the tangent impulses of impulse */
		public void strained(Fixture fixture, Breakable breakable, Contact contact, ContactImpulse impulse, float normalImpulse, float tangentImpulse);

		/** called by {@link Manager#destroy(Body)} */
		public void destroyedBody(Body body, Breakable breakable);

		/** called by {@link Manager#destroy(Fixture, boolean, boolean)} */
		public void destroyedFixture(Fixture fixture, Breakable breakable);

	}

	/** how much force the Breakable can bear */
	private float normalResistance;

	/** how much friction the Breakable can bear */
	private float tangentResistance = 100;

	/** if the fixture's body (in case the Breakable is used for a fixture) should be destroyed if the fixture is destroyed (false by default) */
	private boolean breakBody;

	/** if the fixture's body (in case the Breakable is used for a fixture) should be destroyed if the fixture is destroyed and it was the bodie's last one (true by default) */
	private boolean breakBodyWithoutFixtures = true;

	/** the {@link Callback} called when the {@link Breakable}'s container is destroyed */
	private Callback callback;

	/** @see #Breakable(float, float, boolean) */
	public Breakable(float normalResistance, float tangentResistance) {
		this(normalResistance, tangentResistance, false);
	}

	/** @see #Breakable(float, float, boolean, boolean) */
	public Breakable(float normalResistance, float tangentResistance, boolean breakBody) {
		this(normalResistance, tangentResistance, breakBody, true);
	}

	/** @see #Breakable(float, float, boolean, boolean, Callback) */
	public Breakable(float normalResistance, float tangentResistance, boolean breakBody, boolean breakBodyWithoutFixtures) {
		this(normalResistance, tangentResistance, breakBody, breakBodyWithoutFixtures, null);
	}

	/** @see #Breakable(float, float, boolean, Callback) */
	public Breakable(float normalResistance, float tangentResistance, Callback callback) {
		this(normalResistance, tangentResistance, false, callback);
	}

	/** @see #Breakable(float, float, boolean, boolean, Callback) */
	public Breakable(float normalResistance, float tangentResistance, boolean breakBody, Callback callback) {
		this(normalResistance, tangentResistance, breakBody, true, callback);
	}

	/** @param normalResistance the {@link #normalResistance}
	 * 	@param tangentResistance the {@link #tangentResistance}
	 *  @param breakBody the {@link #breakBody}
	 *  @param breakBodyWithoutFixtures the {@link #breakBodyWithoutFixtures}
	 *  @param callback the {@link #callback} */
	public Breakable(float normalResistance, float tangentResistance, boolean breakBody, boolean breakBodyWithoutFixtures, Callback callback) {
		this.normalResistance = normalResistance;
		this.tangentResistance = tangentResistance;
		this.breakBody = breakBody;
		this.breakBodyWithoutFixtures = breakBodyWithoutFixtures;
		this.callback = callback;
	}

	/** constructs a new Breakable exactly like the given other one */
	public Breakable(Breakable other) {
		this(other.normalResistance, other.tangentResistance, other.breakBody, other.breakBodyWithoutFixtures, other.callback);
	}

	/** @return the {@link #normalResistance} */
	public float getNormalResistance() {
		return normalResistance;
	}

	/** @param normalResistance the {@link #normalResistance} to set */
	public void setNormalResistance(float normalResistance) {
		this.normalResistance = normalResistance;
	}

	/** @return the {@link #tangentResistance} */
	public float getTangentResistance() {
		return tangentResistance;
	}

	/** @param tangentResistance the {@link #tangentResistance} to set */
	public void setTangentResistance(float tangentResistance) {
		this.tangentResistance = tangentResistance;
	}

	/** @return the {@link #breakBody} */
	public boolean isBreakBody() {
		return breakBody;
	}

	/** @param breakBody the {@link #breakBody} to set */
	public void setBreakBody(boolean breakBody) {
		this.breakBody = breakBody;
	}

	/** @return the {@link #breakBodyWithoutFixtures} */
	public boolean isBreakBodyWithoutFixtures() {
		return breakBodyWithoutFixtures;
	}

	/** @param breakBodyWithoutFixtures the {@link #breakBodyWithoutFixtures} to set */
	public void setBreakBodyWithoutFixtures(boolean breakBodyWithoutFixtures) {
		this.breakBodyWithoutFixtures = breakBodyWithoutFixtures;
	}

	/** @return the {@link #callback} */
	public Callback getCallback() {
		return callback;
	}

	/** @param callback the {@link #callback} to set */
	public void setCallback(Callback callback) {
		this.callback = callback;
	}

}