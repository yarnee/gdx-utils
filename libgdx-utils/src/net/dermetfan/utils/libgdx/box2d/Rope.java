/** Copyright 2013 Robin Stumm (serverkorken@googlemail.com, http://dermetfan.net/)
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

package net.dermetfan.utils.libgdx.box2d;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.JointDef;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.SnapshotArray;

/** Holds {@link #segments} and {@link #joints} to simulate a rope. Also provides modification methods that use a {@link Builder}.
 *  @author dermetfan */
public class Rope {

	/** used by a {@link Rope} to modify it
	 *  @author dermetfan */
	public static interface Builder {

		/** creates a segment that is going to be added to {@link Rope#segments}
		 *  @param index the index of the segment to create
		 *  @param previous the previously created segment
		 *  @param length the desired length of the {@link Rope} that is being build
		 *  @return the created segment */
		public Body createSegment(int index, Body previous, int length);

		/** connects two segments with each other using a {@link Joint}
		 *  @param seg1 the first segment
		 *  @param seg1index the index of the first segment
		 *  @param seg2 the second segment
		 *  @param seg2index the index of the second segment
		 *  @return the created {@link Joint} */
		public Joint createJoint(Body seg1, int seg1index, Body seg2, int seg2index);

	}

	/** a {@link Builder} that creates a new {@link Rope} using this {@link Builder}
	 *  @author dermetfan */
	public static abstract class RopeBuilder implements Builder {

		/** the {@link Rope} this {@link RopeBuilder} builds */
		public final Rope rope;

		/** Creates a new {@link RopeBuilder} and sets {@link #rope} to a new {@link Rope} of the given {@code length}. The {@link #rope} will not be {@link Rope#build(int) build} immediantly.
		 *  @param length the desired length of the {@link #rope} */
		public RopeBuilder(int length) {
			rope = new Rope(length, this, false);
		}

	}

	/** a {@link Builder} that builds using a {@link BodyDef}, {@link FixtureDef} and {@link Joint}
	 *  @author dermetfan */
	public static class DefBuilder implements Builder {

		/** the {@link World} in which to create segments and joints */
		protected World world;

		/** the {@link BodyDef} to use in {@link #createSegment(int, Body, int)} */
		protected BodyDef bodyDef;

		/** the {@link FixtureDef} to use in {@link #createSegment(int, Body, int)} */
		protected FixtureDef fixtureDef;

		/** the {@link JointDef} to use in {@link #createJoint(Body, int, Body, int)} */
		protected JointDef jointDef;

		/** @param world the {@link #world}
		 *  @param bodyDef the {@link #bodyDef}
		 *  @param fixtureDef the {@link #fixtureDef}
		 *  @param jointDef the {@link #jointDef} */
		public DefBuilder(World world, BodyDef bodyDef, FixtureDef fixtureDef, JointDef jointDef) {
			this.world = world;
			this.bodyDef = bodyDef;
			this.fixtureDef = fixtureDef;
			this.jointDef = jointDef;
		}

		/** @return a new {@link Body segment} created with{@link #bodyDef} and {@link #fixtureDef} */
		@Override
		public Body createSegment(int index, Body previous, int length) {
			return world.createBody(bodyDef).createFixture(fixtureDef).getBody();
		}

		/** @return a new {@link JointDef} created with {@link #jointDef} */
		@Override
		public Joint createJoint(Body seg1, int seg1index, Body seg2, int seg2index) {
			jointDef.bodyA = seg1;
			jointDef.bodyB = seg2;
			return world.createJoint(jointDef);
		}

	}

	/** A {@link Builder} that builds using a {@link BodyDef}, {@link JointDef} and {@link Shape}. Should be {@link DefShapeBuilder#dispose() dispose} if no longer used.
	 *  @author dermetfan */
	public static class DefShapeBuilder implements Builder, Disposable {

		/** the {@link World} to create things in */
		protected World world;

		/** the {@link BodyDef} to use in {@link #createSegment(int, Body, int)} */
		protected BodyDef bodyDef;

		/** the {@link Shape} to use in {@link #createSegment(int, Body, int)} */
		protected Shape shape;

		/** the density to use in {@link Body#createFixture(Shape, float)} */
		protected float density;

		/** the {@link JointDef} to use in {@link #createJoint(Body, int, Body, int)} */
		protected JointDef jointDef;

		/** @param world the {@link #world}
		 *  @param bodyDef the {@link #bodyDef}
		 *  @param shape the {@link Shape}
		 *  @param density the {@link #density}
		 *  @param jointDef the {@link #jointDef} */
		public DefShapeBuilder(World world, BodyDef bodyDef, Shape shape, float density, JointDef jointDef) {
			this.world = world;
			this.bodyDef = bodyDef;
			this.shape = shape;
			this.density = density;
			this.jointDef = jointDef;
		}

		/** creates a {@link Body segment} using {@link #bodyDef}, {@link #shape} and {@link #density}
		 *  @see Body#createFixture(Shape, float) */
		@Override
		public Body createSegment(int index, Body previous, int length) {
			return world.createBody(bodyDef).createFixture(shape, density).getBody();
		}

		/** @return a new {@link Joint} created with {@link #jointDef} */
		@Override
		public Joint createJoint(Body seg1, int seg1index, Body seg2, int seg2index) {
			jointDef.bodyA = seg1;
			jointDef.bodyB = seg2;
			return world.createJoint(jointDef);
		}

		/** {@link Shape#dispose() disposes} the {@link #shape} */
		@Override
		public void dispose() {
			shape.dispose();
		}

	}

	/** a {@link Builder} that {@link Box2DUtils#copy(Body) copies} a {@link Body} as template in {@link #createSegment(int, Body, int)}
	 *  @author dermetfan */
	public static abstract class CopyBuilder implements Builder {

		/** the {@link Body} to {@link Box2DUtils#copy(Body) copy} in {@link #createSegment(int, Body, int)} */
		protected Body template;

		/** @param template the {@link #template} */
		public CopyBuilder(Body template) {
			this.template = template;
		}

		/** @return a {@link Box2DUtils#copy(Body) copy} of {@link #template} */
		@Override
		public Body createSegment(int index, Body previous, int length) {
			return Box2DUtils.copy(template);
		}

	}

	/** a {@link CopyBuilder} that uses a {@link JointDef} in {@link #createJoint(Body, int, Body, int)}
	 *  @author dermetfan */
	public static class JointDefCopyBuilder extends CopyBuilder {

		/** the {@link JointDef} to use in {@link #createJoint(Body, int, Body, int)} */
		protected JointDef jointDef;

		/** @param template the {@link CopyBuilder#template}
		 *  @param jointDef the {@link #jointDef} */
		public JointDefCopyBuilder(Body template, JointDef jointDef) {
			super(template);
			this.jointDef = jointDef;
		}

		/** @return a new {@link Joint} created with {@link #jointDef} */
		@Override
		public Joint createJoint(Body seg1, int seg1index, Body seg2, int seg2index) {
			jointDef.bodyA = seg1;
			jointDef.bodyB = seg2;
			return template.getWorld().createJoint(jointDef);
		}

	}

	/** the {@link Builder} used for modifications of this Rope */
	private Builder builder;

	/** the {@link Body segments} of this Rope */
	private final SnapshotArray<Body> segments = new SnapshotArray<Body>();

	/** the {@link Joint Joints} of this Rope */
	private final SnapshotArray<Joint> joints = new SnapshotArray<Joint>();

	/** creates a new Rope and {@link #build(int) builds} it to the given {@code length}
	 *  @param length the desired length of this Rope
	 *  @param builder the {@link #builder}
	 *  @see #Rope(int, Builder, boolean) */
	public Rope(int length, Builder builder) {
		this(length, builder, true);
	}

	/** creates a new Rope and {@link #build(int) builds} it to the given {@code length} if {@code build} is true
	 *  @param length The desired length of this Rope. Will be ignored if {@code build} is false.
	 *  @param builder the {@link #builder}
	 *  @param build if this Rope should be {@link #build(int) build} to the given {@code length} */
	public Rope(int length, Builder builder, boolean build) {
		segments.ensureCapacity(length - segments.size);
		joints.ensureCapacity(length - segments.size);
		this.builder = builder;
		if(build)
			build(length);
	}

	/** creates a new Rope with the given {@code segments}
	 *  @param builder the {@link #builder}
	 *  @param segments the {@link #segments} */
	public Rope(Builder builder, Body... segments) {
		this.builder = builder;
		for(Body segment : segments)
			add(segment);
	}

	/** builds this rope to the given {@code length} using the {@link #builder}
	 *  @see #build(int, Builder) */
	public Rope build(int length) {
		return build(length, builder);
	}

	/** builds this rope to the given {@code length} using the given {@link Builder} */
	public Rope build(int length, Builder builder) {
		while(length > 0) {
			extend();
			length--;
		}
		return this;
	}

	/** Creates a {@link Body segment} using the {@link #builder} passing the correct parameters to {@link Builder#createSegment(int, Body, int)} specified by the given {@code index}. Does NOT add it to this Rope.
	 *  @see Builder#createSegment(int, Body, int) */
	public Body createSegment(int index) {
		return builder.createSegment(index, segments.size > 0 ? segments.peek() : null, segments.size + 1);
	}

	/** Creates a {@link Joint joint} using the {@link #builder} passing the correct parameters to {@link Builder#createJoint(Body, int, Body, int)} specified by the given {@code index}. Does NOT add it to this Rope.
	 *  @see Builder#createJoint(Body, int, Body, int) */
	public Joint createJoint(int segmentIndex1, int segmentIndex2) {
		Body seg1 = segments.get(segmentIndex1), seg2 = segments.get(segmentIndex2);
		return builder.createJoint(seg1, segmentIndex1, seg2, segmentIndex2);
	}

	/** {@link #createSegment(int) creates} and {@link #add(Body) adds} a new segment to this Rope
	 *  @return the {@link #createSegment(int) created} and {@link #add(Body) added} segment */
	public Body extend() {
		Body segment = createSegment(segments.size);
		add(segment);
		return segment;
	}

	/** {@link SnapshotArray#add(Object) adds} the given {@code segment} to the end of this Rope
	 *  @param segment the {@link Body segment} to add */
	public void add(Body segment) {
		segments.add(segment);
		if(segments.size > 1)
			joints.add(createJoint(segments.size - 2 < 0 ? 0 : segments.size - 2, segments.size - 1));
	}

	/** {@link #createSegment(int) creates} a new {@link Body segment} and {@link #insert(int, Body) inserts} it into this Rope
	 *  @see #insert(int, Body) */
	public Body insert(int index) {
		Body segment = createSegment(index);
		insert(index, segment);
		return segment;
	}

	/** inserts a {@link Body segment} into this Rope
	 *  @param index the {@link #segments index} at which to insert the given {@code segment}
	 *  @param segment the {@link Body segment} to insert */
	public void insert(int index, Body segment) {
		if(index - 1 >= 0)
			segment.getWorld().destroyJoint(joints.removeIndex(index - 1));
		segments.insert(index, segment);
		if(index - 1 >= 0)
			joints.insert(index - 1, createJoint(index - 1, index));
		if(index + 1 < segments.size)
			joints.insert(index, createJoint(index, index + 1));
	}

	/** @param index the index of the segment to replace
	 *  @param segment the {@link Body segment} to insert
	 *  @return the {@link Body segment} that was at the given {@code index} previously */
	public Body replace(int index, Body segment) {
		Body old = remove(index);
		insert(index, segment);
		return old;
	}

	/** @param segment The {@link Body} to remove. Must be a {@link #segments segment} of this {@link Rope}.
	 *  @return the given {@code body}
	 *  @see #remove(int) */
	public Body remove(Body segment) {
		if(!segments.contains(segment, true))
			throw new IllegalArgumentException("the given body is not a segment of this Rope");
		return remove(segments.indexOf(segment, true));
	}

	/** removes a {@link #segments segment} from this Rope
	 *  @param index the index of the {@link #segments segment} to remove
	 *  @return the removed {@link #segments segment} */
	public Body remove(int index) {
		Body segment = segments.removeIndex(index);
		if(--index >= 0)
			segment.getWorld().destroyJoint(joints.removeIndex(index));
		else
			index++;
		if(index < joints.size)
			segment.getWorld().destroyJoint(joints.removeIndex(index));

		Body prevSegment = segments.get(index), nextSegment = segments.get(MathUtils.clamp(index + 1, 0, segments.size - 1));
		if(prevSegment != nextSegment)
			joints.insert(index, builder.createJoint(prevSegment, index, nextSegment, index + 1));

		return segment;
	}

	/** @param segment the {@link Body segment} to destroy
	 *  @see #destroy(int) */
	public void destroy(Body segment) {
		if(!segments.contains(segment, true))
			throw new IllegalArgumentException("the given body must be a segment of this Rope");
		destroy(segments.indexOf(segment, true));
	}

	/** @param index the index of the {@link #segments segment} to {@link World#destroyBody(Body) destroy}
	 *  @see #remove(int) */
	public void destroy(int index) {
		Body segment = remove(index);
		segment.getWorld().destroyBody(segment);
	}

	/** @param joint the {@link #joints Joint} at which to split this {@link Rope}
	 *  @return the new {@link Rope}
	 *  @see #split(int) */
	public Rope split(Joint joint) {
		if(!joints.contains(joint, true))
			throw new IllegalArgumentException("the joint must be part of this Rope");
		return split(joints.indexOf(joint, true));
	}

	/** splits this Rope at the given index and returns a new Rope consisting of the {@link #segments} up to the given {@code index}
	 *  @param jointIndex the index of the {@link #joints Joint} to destroy
	 *  @return a Rope consisting of the segments before the given index */
	public Rope split(int jointIndex) {
		Body[] segs = new Body[jointIndex + 1];
		for(int i = 0; i <= jointIndex; i++) {
			Joint joint = joints.removeIndex(0);
			joint.getBodyA().getWorld().destroyJoint(joint);
			segs[i] = segments.removeIndex(0);
		}
		return new Rope(builder, segs);
	}

	/** @return the amount of segments in this Rope */
	public int length() {
		return segments.size;
	}

	/** @param index the index of the desired segment
	 *  @return the {@link #segments segment} at the given index */
	public Body getSegment(int index) {
		return segments.get(index);
	}

	/** @param index the index of the desired {@link #joints Joint}
	 *  @return the {@link #joints Joint} at the given index */
	public Joint getJoint(int index) {
		return joints.get(index);
	}

	/** @return the {@link #builder} */
	public Builder getBuilder() {
		return builder;
	}

	/** @param builder the {@link #builder} to set */
	public void setBuilder(Builder builder) {
		if(builder == null)
			throw new IllegalArgumentException("builder must not be null");
		this.builder = builder;
	}

	/** @return the {@link #segments} */
	public Body[] getSegments() {
		segments.end();
		return segments.begin();
	}

	/** @return the {@link #joints} */
	public Joint[] getJoints() {
		joints.end();
		return joints.begin();
	}

}
