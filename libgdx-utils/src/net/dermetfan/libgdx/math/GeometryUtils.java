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

package net.dermetfan.libgdx.math;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

/** contains some useful methods for geometric calculations */
public abstract class GeometryUtils {

	/** @return a Vector2 representing the size of a rectangle containing all given vertices */
	public static Vector2 size(Vector2[] vertices) {
		return new Vector2(amplitude(filterX(vertices)), amplitude(filterY(vertices)));
	}

	/** @return the peak-to-peak amplitude of the given array */
	public static float amplitude(float[] f) {
		return Math.abs(max(f) - min(f));
	}

	/** @return the largest element of the given array */
	public static float max(float[] floats) {
		float max = Float.NEGATIVE_INFINITY;
		for(float f : floats)
			max = f > max ? f : max;
		return max;
	}

	/** @return the smallest element of the given array */
	public static float min(float[] floats) {
		float min = Float.POSITIVE_INFINITY;
		for(float f : floats)
			min = f < min ? f : min;
		return min;
	}

	/** @return the x values of the given vertices */
	public static float[] filterX(Vector2[] vertices) {
		float[] x = new float[vertices.length];
		for(int i = 0; i < x.length; i++)
			x[i] = vertices[i].x;
		return x;
	}

	/** @return the y values of the given vertices */
	public static float[] filterY(Vector2[] vertices) {
		float[] y = new float[vertices.length];
		for(int i = 0; i < y.length; i++)
			y[i] = vertices[i].y;
		return y;
	}

	/**
	 * @param vector2s the Vector2[] to convert to a float[]
	 * @return the float[] converted from the given Vector2[]
	 */
	public static float[] toFloatArray(Vector2[] vector2s) {
		float[] floats = new float[vector2s.length * 2];

		int vi = -1;
		for(int i = 0; i < floats.length; i++)
			if(i % 2 == 0)
				floats[i] = vector2s[++vi].x;
			else
				floats[i] = vector2s[vi].y;

		return floats;
	}

	/**
	 * @param floats the float[] to convert to a Vector2[]
	 * @return the Vector2[] converted from the given float[]
	 */
	public static Vector2[] toVector2Array(float[] floats) {
		if(floats.length % 2 != 0)
			throw new IllegalArgumentException("the float array's length is not dividable by two, so it won't make up a Vector2 array: " + floats.length);

		Vector2[] vector2s = new Vector2[floats.length / 2];

		int fi = -1;
		for(int i = 0; i < vector2s.length; i++)
			vector2s[i] = new Vector2(floats[++fi], floats[++fi]);

		return vector2s;
	}

	/**
	 * @param vertexCount the number of vertices for each {@link Polygon}
	 * @see #toPolygonArray(Vector2[], int[])
	 */
	public static Polygon[] toPolygonArray(Vector2[] vertices, int vertexCount) {
		int[] vertexCounts = new int[vertices.length / vertexCount];
		for(int i = 0; i < vertexCounts.length; i++)
			vertexCounts[i] = vertexCount;
		return toPolygonArray(vertices, vertexCounts);
	}

	/**
	 * @param vertices the vertices which should be split into a {@link Polygon} array
	 * @param vertexCounts the number of vertices of each {@link Polygon}
	 * @return the {@link Polygon} array extracted from the vertices
	 */
	public static Polygon[] toPolygonArray(Vector2[] vertices, int[] vertexCounts) {
		Polygon[] polygons = new Polygon[vertexCounts.length];

		int vertice = -1;
		for(int i = 0; i < polygons.length; i++) {
			Vector2[] verts = new Vector2[vertexCounts[i]];
			for(int i2 = 0; i2 < verts.length; i2++)
				verts[i2] = vertices[++vertice];
			polygons[i] = new Polygon(toFloatArray(verts));
		}

		return polygons;
	}

	/** @see Polygon#area() */
	public static float area(float[] vertices) {
		// from com.badlogic.gdx.math.Polygon#area()
		float area = 0;

		int x1, y1, x2, y2;
		for(int i = 0; i < vertices.length; i += 2) {
			x1 = i;
			y1 = i + 1;
			x2 = (i + 2) % vertices.length;
			y2 = (i + 3) % vertices.length;

			area += vertices[x1] * vertices[y2];
			area -= vertices[x2] * vertices[y1];
		}

		return area /= 2f;
	}

	/**
	 * @param polygon the polygon, assumed to be simple
	 * @return if the vertices are in clockwise order 
	 */
	public static boolean areVerticesClockwise(Polygon polygon) {
		return polygon.area() < 0;
	}

	/** @see #areVerticesClockwise(Polygon) */
	public static boolean areVerticesClockwise(float[] vertices) {
		return area(vertices) < 0;
	}

	/** @see #isConvex(Vector2[]) */
	public static boolean isConvex(float[] vertices) {
		return isConvex(toVector2Array(vertices));
	}

	/** @see #isConvex(Vector2[]) */
	public static boolean isConvex(Polygon polygon) {
		return isConvex(polygon.getVertices());
	}

	/**
	 * @param vertices the vertices of the polygon to examine for convexity
	 * @return if the polygon is convex
	 */
	public static boolean isConvex(Vector2[] vertices) {
		// http://www.sunshine2k.de/coding/java/Polygon/Convex/polygon.htm

		Vector2 p, v, u, tmp;
		float res = 0;
		for(int i = 0; i < vertices.length; i++) {
			p = vertices[i];
			tmp = vertices[(i + 1) % vertices.length];
			v = new Vector2();
			v.x = tmp.x - p.x;
			v.y = tmp.y - p.y;
			u = vertices[(i + 2) % vertices.length];

			if(i == 0) // in first loop direction is unknown, so save it in res
				res = u.x * v.y - u.y * v.x + v.x * p.y - v.y * p.x;
			else {
				float newres = u.x * v.y - u.y * v.x + v.x * p.y - v.y * p.x;
				if(newres > 0 && res < 0 || newres < 0 && res > 0)
					return false;
			}
		}

		return true;
	}

}
