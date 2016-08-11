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
package se.llbit.math;

/**
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class Vector3 {
  public double x, y, z;

  public Vector3(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public double dot(Vector3 b) {
    return x * b.x + y * b.y + z * b.z;
  }

  public void normalize() {
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

  public void cross(Vector3 a, Vector3 b) {
    x = a.y * b.z - a.z * b.y;
    y = a.z * b.x - a.x * b.z;
    z = a.x * b.y - a.y * b.x;
  }

  public void scaleAdd(double s, Vector3 d, Vector3 o) {
    x = s * d.x + o.x;
    y = s * d.y + o.y;
    z = s * d.z + o.z;
  }

  public void scaleAdd(double s, Vector3 d) {
    x += s * d.x;
    y += s * d.y;
    z += s * d.z;
  }

  public void set(Vector3 o) {
    x = o.x;
    y = o.y;
    z = o.z;
  }

  public void add(Vector3 u) {
    x += u.x;
    y += u.y;
    z += u.z;
  }

  public void sub(Vector3 u, Vector3 v) {
    x = u.x - v.x;
    y = u.y - v.y;
    z = u.z - v.z;
  }
}
