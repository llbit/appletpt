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
public class Sphere extends Geom {

	private final double rSquare;

	Sphere(Vec pos, Vec col, double radius) {
		super(pos, col);

		this.rSquare = radius * radius;
	}

	@Override
	boolean intersect(Ray ray, Intersection isect) {
		double dx = ray.o.x - pos.x;
		double dy = ray.o.y - pos.y;
		double dz = ray.o.z - pos.z;
		double b = 2 * (dx*ray.d.x + dy*ray.d.y + dz*ray.d.z);
		double c = ((dx*dx+dy*dy+dz*dz) - rSquare);
		double ta = -b * .5;
		double tb = .25 * (b * b) - c;
		if (tb <= 0) {
			return false;
		}
		tb = Math.sqrt(tb);
		double t0 = ta + tb;
		double t1 = ta - tb;
		if (t0 <= 0) {
			t0 = Double.POSITIVE_INFINITY;
		}
		if (t1 <= 0) {
			t1 = Double.POSITIVE_INFINITY;
		}
		double tNear = (t0<t1)?t0:t1;
		if (tNear < isect.t) {
			isect.t = tNear;
			isect.geom = this;
			return true;
		} else {
			return false;
		}
	}

	@Override
	void calcNorm(Intersection isect) {
		// compute sphere normal at intersection
		isect.n.sub(isect.x, pos);
		isect.n.normalize();
	}
}
