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

import se.llbit.math.Matrix3;
import se.llbit.math.QuickMath;
import se.llbit.math.Vector3;

/**
 * Encapsulates camera state and perspective view transform methods.
 */
public class Camera {

  Vector3 pos = new Vector3(-5.4, -2.2, 7.6);

  /**
   * Scratch vector.
   * NB: protected by synchronized methods (no concurrent modification)
   */
  private final Vector3 u = new Vector3(0, 0, 0);

  /**
   * Yaw angle. Down = 0, forward = -PI/2, up = -PI.
   */
  private double yaw = -2.08;

  /**
   * Pitch angle. Pitch = 0 corresponds to the camera pointing along the z axis,
   * pitch = PI/2 corresponds to the negative x axis, etc.
   */
  private double pitch = -1.64;

  /**
   * Camera roll.
   */
  private double roll = 0;

  /**
   * Transform to rotate from camera space to world space (not including
   * translation).
   */
  private final Matrix3 transform = new Matrix3();

  private final Matrix3 tmpTransform = new Matrix3();

  private double fov = 90;

  /**
   * Maximum diagonal width of the world. Recalculated when world is loaded.
   */
  private double worldWidth = 100;

  private double subjectDistance = 2;

  public String name = "camera 1";

  /**
   * Create a new camera
   */
  public Camera() {
    transform.setIdentity();
    initProjector();
    updateTransform();
  }

  void initProjector() {

  }

  /**
   * Copy camera configuration from another camera
   *
   * @param other the camera to copy configuration from
   */
  public void set(Camera other) {
    pos.set(other.pos);
    yaw = other.yaw;
    pitch = other.pitch;
    roll = other.roll;
    fov = other.fov;
    subjectDistance = other.subjectDistance;
    worldWidth = other.worldWidth;
    initProjector();
    updateTransform();
  }

  /**
   * Set the camera position
   *
   * @param v
   */
  public void setPosition(Vector3 v) {
    pos.set(v);
  }

  /**
   * @return Current field of view
   */
  public double getFov() {
    return fov;
  }

  /**
   * Move camera forward
   *
   * @param v
   */
  public synchronized void moveForward(double v) {
    u.set(0, 0, 1);
    transform.transform(u);
    pos.scaleAdd(v, u);
  }

  /**
   * Move camera backward
   *
   * @param v
   */
  public synchronized void moveBackward(double v) {
    u.set(0, 0, 1);
    transform.transform(u);
    pos.scaleAdd(-v, u);
  }

  /**
   * Strafe camera left
   *
   * @param v
   */
  public synchronized void strafeLeft(double v) {
    u.set(1, 0, 0);
    transform.transform(u);
    pos.scaleAdd(-v, u);
  }

  /**
   * Strafe camera right
   *
   * @param v
   */
  public synchronized void strafeRight(double v) {
    u.set(1, 0, 0);
    transform.transform(u);
    pos.scaleAdd(v, u);
  }

  /**
   * Rotate the camera
   *
   * @param yaw
   * @param pitch
   */
  public synchronized void rotateView(double yaw, double pitch) {
    double fovRad = QuickMath.degToRad(fov / 2);
    this.yaw += yaw * fovRad;
    this.pitch += pitch * fovRad;

    this.pitch = QuickMath.min(0, this.pitch);
    this.pitch = QuickMath.max(-Math.PI, this.pitch);

    if (this.yaw > QuickMath.TAU) {
      this.yaw -= QuickMath.TAU;
    } else if (this.yaw < -QuickMath.TAU) {
      this.yaw += QuickMath.TAU;
    }

    updateTransform();
  }

  /**
   * Update the camera transformation matrix.
   */
  synchronized void updateTransform() {
    transform.setIdentity();

    // Yaw (y axis rotation).
    tmpTransform.rotY(QuickMath.HALF_PI + yaw);
    transform.mul(tmpTransform);

    // Pitch (x axis rotation).
    tmpTransform.rotX(QuickMath.HALF_PI - pitch);
    transform.mul(tmpTransform);

    // Roll (z axis rotation).
    tmpTransform.rotZ(roll);
    transform.mul(tmpTransform);
  }

  /**
   * Calculate a ray shooting out of the camera based on normalized
   * image coordinates.
   *
   * @param ray    result ray
   * @param x      normalized image coordinate [-0.5, 0.5]
   * @param y      normalized image coordinate [-0.5, 0.5]
   */
  public void calcViewRay(Ray ray, double x, double y) {
    double fovTan = 2 * Math.tan(QuickMath.degToRad(fov / 2));
    ray.o.set(0, 0, 0);
    ray.d.set(fovTan * x, fovTan * y, 1);

    ray.d.normalize();

    // From camera space to world space.
    transform.transform(ray.d);
    ray.o.add(pos);
  }
}
