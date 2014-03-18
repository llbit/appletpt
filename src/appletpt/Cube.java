/**
 * I consider this code to be so general that it should be public domain.
 * As far as I am concerned anyone can use this for anything they want,
 * but credits are appreciated if you do use my code =)
 * 2013 Jesper Öqvist <jesper@llbit.se>
 */
package appletpt;

/**
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class Cube extends Geom {
	private final double x0;
	private final double x1;
	private final double y0;
	private final double y1;
	private final double z0;
	private final double z1;

	public Cube(Vec pos, Vec col, float size) {
		super(pos, col);

		double w = size * .5;
		x0 = pos.x - w;
		x1 = pos.x + w;
		y0 = pos.y - w;
		y1 = pos.y + w;
		z0 = pos.z - w;
		z1 = pos.z + w;
	}

	@Override
	boolean intersect(Ray ray, Intersection isect) {
		double t1, t2;
		double tNear = Double.NEGATIVE_INFINITY;
		double tFar = Double.POSITIVE_INFINITY;
		Vec d = ray.d;
		Vec o = ray.o;
		double nx = 0;
		double ny = 0;
		double nz = 0;

		if (d.x != 0) {
			double rx = 1 / d.x;
			t1 = (x0 - o.x) * rx;
			t2 = (x1 - o.x) * rx;

			int sign = -1;

			if (t1 > t2) {
				double t = t1;
				t1 = t2;
				t2 = t;
				sign = 1;
			}

			if (t1 > tNear) {
				tNear = t1;
				nx = sign;
				ny = nz = 0;
			}
			if (t2 < tFar) {
				tFar = t2;
			}
		}

		if (d.y != 0) {
			double ry = 1 / d.y;
			t1 = (y0 - o.y) * ry;
			t2 = (y1 - o.y) * ry;

			int sign = -1;

			if (t1 > t2) {
				double t = t1;
				t1 = t2;
				t2 = t;
				sign = 1;
			}

			if (t1 > tNear) {
				tNear = t1;
				ny = sign;
				nx = nz = 0;
			}
			if (t2 < tFar) {
				tFar = t2;
			}
		}

		if (d.z != 0) {
			double rz = 1 / d.z;
			t1 = (z0 - o.z) * rz;
			t2 = (z1 - o.z) * rz;

			int sign = -1;

			if (t1 > t2) {
				double t = t1;
				t1 = t2;
				t2 = t;
				sign = 1;
			}

			if (t1 > tNear) {
				tNear = t1;
				nz = sign;
				nx = ny = 0;
			}
			if (t2 < tFar) {
				tFar = t2;
			}
		}

		if (tNear < tFar && tNear >= 0 && tNear < isect.t) {
			isect.t = tNear;
			isect.n.set(nx, ny, nz);
			isect.geom = this;
			return true;
		} else {
			return false;
		}
	}

	@Override
	void calcNorm(Intersection isect) {
	}
}
