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

/**
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class Sphere extends Geom {

  private final double rSquare;

  Sphere(Vector3 pos, Vector3 col, double radius) {
    super(pos, col);

    this.rSquare = radius * radius;
  }

  @Override boolean intersect(Ray ray, Intersection isect) {
    double dx = ray.o.x - pos.x;
    double dy = ray.o.y - pos.y;
    double dz = ray.o.z - pos.z;
    double b = 2 * (dx * ray.d.x + dy * ray.d.y + dz * ray.d.z);
    double c = ((dx * dx + dy * dy + dz * dz) - rSquare);
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
    double tNear = (t0 < t1) ? t0 : t1;
    if (tNear < isect.t) {
      isect.t = tNear;
      isect.geom = this;
      return true;
    } else {
      return false;
    }
  }

  @Override void calcNorm(Intersection isect) {
    // Compute sphere normal at intersection.
    isect.n.sub(isect.x, pos);
    isect.n.normalize();
  }
}
