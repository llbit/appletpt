/**
 * I consider this code to be so general that it should be public domain.
 * As far as I am concerned anyone can use this for anything they want,
 * but credits are appreciated if you do use my code =)
 * 2013 Jesper Öqvist <jesper@llbit.se>
 */
package appletpt;

public abstract class Geom {
	protected final Vec pos;
	protected Vec color;
	protected double emittance = 0;
	protected double nextEmittance = 0;

	public Geom(Vec pos, Vec col) {
		this.pos = pos;
		this.color = col;
	}

	void setEmittance(double value) {
		nextEmittance = value;
	}

	/**
	 * Intersect the ray with this geometry.
	 * @param ray
	 * @param isect intersection object to store the intersection in
	 * @return {@code true} if there was an intersection
	 */
	abstract boolean intersect(Ray ray, Intersection isect);

	/**
	 * Update normal of intersection after running intersection test.
	 * @param isect
	 */
	abstract void calcNorm(Intersection isect);
}
