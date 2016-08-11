/* Copyright (c) 2012-2016 Jesper Öqvist <jesper@llbit.se>
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
 * A three by three matrix of doubles.
 *
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class Matrix3 {

  @SuppressWarnings("javadoc") public double m11, m12, m13;
  @SuppressWarnings("javadoc") public double m21, m22, m23;
  @SuppressWarnings("javadoc") public double m31, m32, m33;

  /**
   * Set the matrix to be a rotation matrix for rotation
   * around the X axis
   *
   * @param theta
   */
  public final void rotX(double theta) {
    double cost = Math.cos(theta);
    double sint = Math.sin(theta);
    m11 = 1;
    m12 = 0;
    m13 = 0;
    m21 = 0;
    m22 = cost;
    m23 = -sint;
    m31 = 0;
    m32 = sint;
    m33 = cost;
  }

  /**
   * Set the matrix to be a rotation matrix for rotation
   * around the Y axis
   *
   * @param theta
   */
  public final void rotY(double theta) {
    double cost = Math.cos(theta);
    double sint = Math.sin(theta);
    m11 = cost;
    m12 = 0;
    m13 = sint;
    m21 = 0;
    m22 = 1;
    m23 = 0;
    m31 = -sint;
    m32 = 0;
    m33 = cost;
  }

  /**
   * Set the matrix to be a rotation matrix for rotation
   * around the X axis
   *
   * @param theta
   */
  public final void rotZ(double theta) {
    double cost = Math.cos(theta);
    double sint = Math.sin(theta);
    m11 = cost;
    m12 = -sint;
    m13 = 0;
    m21 = sint;
    m22 = cost;
    m23 = 0;
    m31 = 0;
    m32 = 0;
    m33 = 1;
  }

  /**
   * Transform a vector using this matrix
   *
   * @param o
   */
  public void transform(Vector3 o) {
    o.set(m11 * o.x + m12 * o.y + m13 * o.z, m21 * o.x + m22 * o.y + m23 * o.z,
        m31 * o.x + m32 * o.y + m33 * o.z);
  }

  /**
   * Set to the identity matrix
   */
  public final void setIdentity() {
    m11 = m22 = m33 = 1;
    m12 = m13 = m21 = m23 = m31 = m32 = 0;
  }

  /**
   * Multiply with other matrix
   *
   * @param o
   */
  public final void mul(Matrix3 o) {
    double t11 = m11 * o.m11 + m12 * o.m21 + m13 * o.m31;
    double t12 = m11 * o.m12 + m12 * o.m22 + m13 * o.m32;
    double t13 = m11 * o.m13 + m12 * o.m23 + m13 * o.m33;
    double t21 = m21 * o.m11 + m22 * o.m21 + m23 * o.m31;
    double t22 = m21 * o.m12 + m22 * o.m22 + m23 * o.m32;
    double t23 = m21 * o.m13 + m22 * o.m23 + m23 * o.m33;
    double t31 = m31 * o.m11 + m32 * o.m21 + m33 * o.m31;
    double t32 = m31 * o.m12 + m32 * o.m22 + m33 * o.m32;
    double t33 = m31 * o.m13 + m32 * o.m23 + m33 * o.m33;
    m11 = t11;
    m12 = t12;
    m13 = t13;
    m21 = t21;
    m22 = t22;
    m23 = t23;
    m31 = t31;
    m32 = t32;
    m33 = t33;
  }
}
