/**
 * I consider this code to be so general that it should be public domain.
 * As far as I am concerned anyone can use this for anything they want,
 * but credits are appreciated if you do use my code =)
 * 2013 Jesper Öqvist <jesper@llbit.se>
 */
package appletpt;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;

/**
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class Worker extends Thread {
	private static final double AMBIENT = 0.4;
	private static final double DIRECT_LIGHT = 0.6;

	private final PT pt;
	private final Ray ray = new Ray();
	private final Ray shadowRay = new Ray();
	private final Intersection isect = new Intersection();
	private final Intersection shadowIsect = new Intersection();
	private final Random rand;

	public Worker(PT pt, long seed) {
		this.pt = pt;
		rand = new Random(seed);
	}

	@Override
	public void run() {
		try {
			while (!isInterrupted()) {
				int next = pt.nextJob.getAndIncrement();
				if (next >= PT.IMAGE_SIZE) {
					pt.barrier.await();
					synchronized (pt.renderLock) {
						while (pt.nextJob.get() >= PT.IMAGE_SIZE) {
							pt.renderLock.wait();
						}
					}
				} else {
					trace(next);
				}
			}
		} catch (InterruptedException e) {
		} catch (BrokenBarrierException e) {
		}
	}

	public void trace(int y) {
		int spp = pt.spp;
		double buffer[] = pt.buffer;

		int offset = y*PT.IMAGE_SIZE*3;
		for (int x = 0; x < PT.IMAGE_SIZE; ++x) {
			double ox = rand.nextFloat() - .5;
			double oy = rand.nextFloat() - .5;
			pt.cameraTransform(ray, x+ox, y+oy);

			double cr = 0;
			double cg = 0;
			double cb = 0;
			double ar = 1;
			double ag = 1;
			double ab = 1;
			int bounce = 0;
			while (pt.closestIntersection(ray, isect)) {

				bounce += 1;

				ray.o.scaleAdd(PT.EPSILON, isect.n, isect.x);

				shadowRay.o.set(ray.o);
				pt.getSunDirection(shadowRay.d, rand);
				double direct = 0;
				if (pt.sunEnabled && !pt.anyIntersection(shadowRay, shadowIsect)) {
					direct = shadowRay.d.dot(isect.n) * DIRECT_LIGHT;
				}
				double light = direct + isect.geom.emittance * pt.emittersMod;
				Vec color = isect.geom.color;
				cr += ar * light * color.x;
				cg += ag * light * color.y;
				cb += ab * light * color.z;

				pt.diffuseReflection(ray, isect.n, rand);
				double dot = ray.d.dot(isect.n);
				ar *= dot * color.x;
				ag *= dot * color.y;
				ab *= dot * color.z;

				// russian roulette
				if (bounce >= 4 && rand.nextBoolean()) {
					break;
				}
			}

			if (bounce > 0) {
				cr += ar * AMBIENT;
				cg += ag * AMBIENT;
				cb += ab * AMBIENT;
			} else {
				cr = AMBIENT;
				cg = AMBIENT;
				cb = AMBIENT;
			}

			double sinv = 1.0 / (spp + 1);
			buffer[offset+0] = (buffer[offset+0] * spp + cr) * sinv;
			buffer[offset+1] = (buffer[offset+1] * spp + cg) * sinv;
			buffer[offset+2] = (buffer[offset+2] * spp + cb) * sinv;

			offset += 3;
		}
	}

}
