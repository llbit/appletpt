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
public class Vec {
	double x, y, z;

	Vec(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	Vec(Vec b) {
		x = b.x;
		y = b.y;
		z = b.z;
	}

	double dot(Vec b) {
		return x * b.x + y * b.y + z * b.z;
	}

	void normalize() {
		double r = 1 / Math.sqrt(x * x + y * y + z * z);
		x *= r;
		y *= r;
		z *= r;
	}

	public void set(double f, double g, double h) {
		x = f;
		y = g;
		z = h;
	}

	public void cross(Vec a, Vec b) {
		x = a.y * b.z - a.z * b.y;
		y = a.z * b.x - a.x * b.z;
		z = a.x * b.y - a.y * b.x;
	}

	public void scaleAdd(double s, Vec d, Vec o) {
		x = s * d.x + o.x;
		y = s * d.y + o.y;
		z = s * d.z + o.z;
	}

	public void scaleAdd(double s, Vec d) {
		x += s * d.x;
		y += s * d.y;
		z += s * d.z;
	}

	public void set(Vec o) {
		x = o.x;
		y = o.y;
		z = o.z;
	}

	public void add(Vec u, Vec v) {
		x = u.x + v.x;
		y = u.y + v.y;
		z = u.z + v.z;
	}

	public void sub(Vec u, Vec v) {
		x = u.x - v.x;
		y = u.y - v.y;
		z = u.z - v.z;
	}
}
