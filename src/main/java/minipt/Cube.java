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
public class Cube extends Geom {
  private final double x0;
  private final double x1;
  private final double y0;
  private final double y1;
  private final double z0;
  private final double z1;

  public Cube(Vector3 pos, Vector3 col, float size) {
    super(pos, col);

    double w = size * .5;
    x0 = pos.x - w;
    x1 = pos.x + w;
    y0 = pos.y - w;
    y1 = pos.y + w;
    z0 = pos.z - w;
    z1 = pos.z + w;
  }

  @Override boolean intersect(Ray ray, Intersection isect) {
    double t1, t2;
    double tNear = Double.NEGATIVE_INFINITY;
    double tFar = Double.POSITIVE_INFINITY;
    Vector3 d = ray.d;
    Vector3 o = ray.o;
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

  @Override void calcNorm(Intersection isect) {
    // Intersection normal was already calculated by the intersection routine.
  }
}
