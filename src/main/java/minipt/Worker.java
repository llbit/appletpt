/* Copyright (c) 2013-2016 Jesper Öqvist <jesper@llbit.se>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 *   2. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *
 *   3. The name of the author may not be used to endorse or promote
 *      products derived from this software without specific prior
 *      written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package minipt;

import se.llbit.math.Vector3;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;

/**
 * Most of the path tracing work is done here.
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class Worker extends Thread {
  private static final double AMBIENT = 0.45;
  private static final double DIRECT_LIGHT = 0.75;

  private final PathTracer pt;
  private final Ray ray = new Ray();
  private final Ray shadowRay = new Ray();
  private final Intersection isect = new Intersection();
  private final Intersection shadowIsect = new Intersection();
  private final Random rand;

  public Worker(PathTracer pt, long seed) {
    this.pt = pt;
    rand = new Random(seed);
  }

  @Override public void run() {
    try {
      while (!isInterrupted()) {
        int next;
        int gen = pt.frameId;
        while (true) {
          next = pt.nextJob.getAndIncrement();
          if (next < PathTracer.IMAGE_SIZE) {
            break;
          } else {
            pt.barrier.await();
            synchronized (pt.renderLock) {
              while (gen == pt.frameId) {
                pt.renderLock.wait();
              }
              gen = pt.frameId;
            }
          }
        }
        trace(next);
      }
    } catch (InterruptedException e) {
    } catch (BrokenBarrierException e) {
    }
  }

  public void trace(int y) {
    int spp = pt.spp;
    double buffer[] = pt.buffer;

    int offset = y * PathTracer.IMAGE_SIZE * 3;
    for (int x = 0; x < PathTracer.IMAGE_SIZE; ++x) {
      double ox = rand.nextFloat() - .5;
      double oy = rand.nextFloat() - .5;
      pt.cameraTransform(ray, -0.5 + (x + ox) / PathTracer.IMAGE_SIZE,
          0.5 - (y + oy) / PathTracer.IMAGE_SIZE);

      double cr = 0;
      double cg = 0;
      double cb = 0;
      double ar = 1;
      double ag = 1;
      double ab = 1;
      int bounce = 0;
      while (pt.closestIntersection(ray, isect)) {
        bounce += 1;

        ray.o.scaleAdd(PathTracer.EPSILON, isect.n, isect.x);

        shadowRay.o.set(ray.o);
        pt.getSunDirection(shadowRay.d, rand);
        double direct = 0;
        if (pt.sunEnabled && !pt.anyIntersection(shadowRay, shadowIsect)) {
          direct = shadowRay.d.dot(isect.n) * DIRECT_LIGHT;
        }
        double light = direct + isect.geom.emittance * pt.emittersMod;
        Vector3 color = isect.geom.color;
        cr += ar * light * color.x;
        cg += ag * light * color.y;
        cb += ab * light * color.z;

        pt.diffuseReflection(ray, isect.n, rand);
        double dot = ray.d.dot(isect.n);
        ar *= dot * color.x;
        ag *= dot * color.y;
        ab *= dot * color.z;

        // Russian roulette:
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
      buffer[offset + 0] = (buffer[offset + 0] * spp + cr) * sinv;
      buffer[offset + 1] = (buffer[offset + 1] * spp + cg) * sinv;
      buffer[offset + 2] = (buffer[offset + 2] * spp + cb) * sinv;

      offset += 3;
    }
  }

}
