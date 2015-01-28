package net.dermetfan.utils.math;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GeometryUtilsTest {

	@Test
	public void between() {
		assertTrue(GeometryUtils.between(.5f, .5f, 0, 0, 1, 1));
		assertTrue(GeometryUtils.between(1, 1, 0, 0, 1, 1, true));
		assertFalse(GeometryUtils.between(1, 1, 0, 0, 1, 1, false));
		assertFalse(GeometryUtils.between(-.5f, .5f, 0, 0, 1, 1));
		assertFalse(GeometryUtils.between(.4f, .5f, 0, 0, 1, 1));
	}

	@Test
	public void width() {
		assertEquals(1, GeometryUtils.width(new float[] {0, 0, 1, 0, 1, 1, 0, 1}), 0);
		assertEquals(1, GeometryUtils.width(new float[] {5, 5, 0, 0, 1, 0, 1, 1, 0, 1, 5, 5}, 2, 8), 0);
	}

	@Test
	public void height() {
		assertEquals(1, GeometryUtils.height(new float[] {0, 0, 1, 0, 1, 1, 0, 1}), 0);
		assertEquals(1, GeometryUtils.height(new float[] {5, 5, 0, 0, 1, 0, 1, 1, 0, 1, 5, 5}, 2, 8), 0);
	}

	@Test
	public void filterX() {
		assertArrayEquals(new float[] {0, 0, 0, 0}, GeometryUtils.filterX(new float[] {0, 1, 0, 1, 0, 1, 0, 1}), 0);
		assertArrayEquals(new float[] {0, 0}, GeometryUtils.filterX(new float[] {0, 1, 0, 1, 0, 1, 0, 1}, 2, 4), 0);
	}

	@Test
	public void filterY() {
		assertArrayEquals(new float[] {1, 1, 1, 1}, GeometryUtils.filterY(new float[] {0, 1, 0, 1, 0, 1, 0, 1}), 0);
		assertArrayEquals(new float[] {1, 1}, GeometryUtils.filterY(new float[] {0, 1, 0, 1, 0, 1, 0, 1}, 2, 4), 0);
	}

	@Test
	public void filterZ() {
		assertArrayEquals(new float[] {0, 0, 0}, GeometryUtils.filterZ(new float[] {1, 1, 0, 1, 1, 0, 1, 1, 0}), 0);
		assertArrayEquals(new float[] {0}, GeometryUtils.filterZ(new float[] {1, 1, 0, 1, 1, 0, 1, 1, 0}, 3, 3), 0);
	}

	@Test
	public void filterW() {
		assertArrayEquals(new float[] {0, 0, 0}, GeometryUtils.filterW(new float[] {1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0}), 0);
		assertArrayEquals(new float[] {0}, GeometryUtils.filterW(new float[] {1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0}, 4, 4), 0);
	}

	@Test
	public void invertAxis() {
		assertEquals(5, GeometryUtils.invertAxis(27, 32), 0);
		assertEquals(27, GeometryUtils.invertAxis(5, 32), 0);
		assertEquals(13, GeometryUtils.invertAxis(19, 32), 0);
	}

}
