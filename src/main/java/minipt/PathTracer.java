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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.stage.Stage;
import se.llbit.math.Vector3;

import java.net.URL;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class PathTracer extends Application implements Initializable {

  @FXML Canvas canvas;

  @FXML Label sppLabel;

  @FXML ToggleButton pauseBtn;

  @FXML ToggleButton sunlightBtn;

  protected final StringProperty sppText = new SimpleStringProperty("SPP: 0");

  protected Camera camera = new Camera();

  protected Camera tempCamera = new Camera();

  final WritableImage image;

  private static final int NUM_WORKERS = Runtime.getRuntime().availableProcessors();
  protected static final int IMAGE_SIZE = 400;

  // Sample buffer:
  final double[] buffer = new double[IMAGE_SIZE * IMAGE_SIZE * 3];

  private final Collection<Geom> objects = new ArrayList<>();
  private final Thread renderThread;
  private final Thread sppThread;
  private boolean stopped = true;
  protected final Object renderLock = new Object();
  protected AtomicInteger nextJob = new AtomicInteger(IMAGE_SIZE);
  protected CyclicBarrier barrier = new CyclicBarrier(NUM_WORKERS + 1);
  protected int lastMouseX;
  protected int lastMouseY;
  protected int spp = 0;
  protected boolean refresh = true;
  protected int nextSpp = 0;

  protected boolean sunEnabled = true;
  protected double emittersMod = 1.0;
  protected boolean sunNext = sunEnabled;
  protected double emittersNext = emittersMod;
  protected double totalTime = 0;
  protected Thread[] workers = new Thread[NUM_WORKERS];

  // Frame index - used by workers to detect next frame start.
  protected int frameId = 0;

  public static void main(String[] args) {
    launch();
  }

  public PathTracer() {
    image = new WritableImage(IMAGE_SIZE, IMAGE_SIZE);

    buildScene();
    initSun(2.4, -2.8);

    renderThread = new Thread() {
      @Override public void run() {
        try {
          long time;
          barrier.await();
          while (!isInterrupted()) {
            synchronized (renderLock) {
              while (stopped) {
                renderLock.wait();
              }
              if (refresh) {
                nextSpp = 0;
                spp = nextSpp;
                camera.set(tempCamera);
                sunEnabled = sunNext;
                emittersMod = emittersNext;
                totalTime = 0;
                for (Geom geom : objects) {
                  geom.emittance = geom.nextEmittance;
                }
                refresh = false;
              }
            }
            time = System.nanoTime();
            nextJob.set(0);
            synchronized (renderLock) {
              frameId += 1;
              renderLock.notifyAll();
            }
            barrier.await();
            time = System.nanoTime() - time;
            updateCanvas(buffer);
            double ms = time / 1000000.0;
            synchronized (renderLock) {
              spp += 1;
              totalTime += ms;
            }
            //System.out.println(String.format("frame took %.3fms", ms));
          }
        } catch (InterruptedException | BrokenBarrierException ignored) {
          // Ignored.
        }
      }
    };
    sppThread = new Thread() {
      @Override public void run() {
        try {
          while (!isInterrupted()) {
            sleep(1000);
            synchronized (renderLock) {
              if (spp == 0) {
                Platform.runLater(() -> sppText.setValue("SPP: 0"));
              } else {
                double sps = (1000 * spp) / totalTime;
                Platform.runLater(
                    () -> sppText.setValue(String.format("SPP: %d (%.1f/s)", spp, sps)));
              }
            }
          }
        } catch (InterruptedException ignored) {
          // Ignored.
        }
      }
    };
  }

  private void buildScene() {
    objects.add(new Cube(new Vector3(0, 40, 0), new Vector3(1, 1, 1), 80));
    objects.add(new Cube(new Vector3(0, -2, 0), new Vector3(1, 1, .2), 4));
    objects.add(new Sphere(new Vector3(1.4, -1, 4), new Vector3(.3, .8, .4), 1));
    objects.add(new Sphere(new Vector3(-4, -1.2, 0), new Vector3(.9, .65, .0), 1.2));
    objects.add(new Sphere(new Vector3(-3, -5, -8), new Vector3(1, .3, .3), 5));
    objects.add(new Sphere(new Vector3(0, -5, 1), new Vector3(.4, .4, 1), 1));
  }

  private static final WritablePixelFormat<IntBuffer> PIXEL_FORMAT =
      PixelFormat.getIntArgbInstance();

  private void updateCanvas(double[] samples) {
    int height = IMAGE_SIZE;
    int width = IMAGE_SIZE;
    int[] pixels = new int[width * height];

    int yoffset = 0;
    for (int y = 0; y < height; ++y) {
      int offset = yoffset;
      for (int x = 0; x < width; ++x) {
        int cr = (int) (samples[offset + 0] * 255);
        int cg = (int) (samples[offset + 1] * 255);
        int cb = (int) (samples[offset + 2] * 255);
        cr = (cr > 255) ? 255 : cr;
        cg = (cg > 255) ? 255 : cg;
        cb = (cb > 255) ? 255 : cb;
        pixels[y * width + x] = (0xFF << 24) | (cr << 16) | (cg << 8) | (cb);
        offset += 3;
      }
      yoffset += width * 3;
    }

    image.getPixelWriter().setPixels(0, 0, width, height, PIXEL_FORMAT, pixels, 0, width);
    canvas.getGraphicsContext2D().drawImage(image, 0, 0);
  }

  @Override public void init() {
  }

  @Override public void start(Stage stage) throws Exception {
    stage.setTitle("Path Tracer");

    FXMLLoader loader = new FXMLLoader(getClass().getResource("Canvas.fxml"));
    loader.setController(this);
    Parent root = loader.load();

    stage.setOnHiding(event -> {
      synchronized (renderLock) {
        stopped = true;
      }
      for (Thread worker : workers) {
        worker.interrupt();
      }
      renderThread.interrupt();
      sppThread.interrupt();
    });
    Scene scene = new Scene(root);
    stage.setScene(scene);
    stage.show();

    scene.setOnKeyPressed(event -> {
      switch (event.getCode()) {
        case W:
          tempCamera.moveForward(1);
          refresh = true;
          break;
        case A:
          tempCamera.strafeLeft(1);
          refresh = true;
          break;
        case S:
          tempCamera.moveBackward(1);
          refresh = true;
          break;
        case D:
          tempCamera.strafeRight(1);
          refresh = true;
          break;
      }
    });
  }

  protected void toggleLight(double x, double y) {
    Ray ray = new Ray();
    cameraTransform(ray, -0.5 + x / IMAGE_SIZE, 0.5 - y / IMAGE_SIZE);
    Intersection isect = new Intersection();
    if (closestIntersection(ray, isect)) {
      Geom geom = isect.geom;
      synchronized (renderLock) {
        geom.setEmittance((geom.emittance != 0) ? 0 : 1);
        if (emittersMod != 0) {
          refresh = true;
        }
      }
    }
  }

  private final Vector3 su = new Vector3(0, 0, 0);
  private final Vector3 sv = new Vector3(0, 0, 0);
  private final Vector3 sw = new Vector3(0, 0, 0);

  private void initSun(double azimuth, double altitude) {
    double theta = azimuth;
    double phi = altitude;

    sw.set(Math.cos(theta), Math.sin(phi), Math.sin(theta));

    double r = Math.sqrt(sw.x * sw.x + sw.z * sw.z);
    r = Math.abs(Math.cos(phi) / r);

    sw.x *= r;
    sw.z *= r;

    if (Math.abs(sw.x) > .1) {
      su.set(0, 1, 0);
    } else {
      su.set(1, 0, 0);
    }
    sv.cross(sw, su);
    sv.normalize();
    su.cross(sv, sw);
  }

  private static final double RADIUS = .03;
  private static final double RADIUS_COS = Math.cos(RADIUS);
  public static final double EPSILON = 0.0005;

  public void getSunDirection(Vector3 d, Random rand) {
    double x1 = rand.nextFloat();
    double x2 = rand.nextFloat();
    double cos_a = 1 - x1 + x1 * RADIUS_COS;
    double sin_a = Math.sqrt(1 - cos_a * cos_a);
    double phi = 2 * Math.PI * x2;

    double cos_sin_a = Math.cos(phi) * sin_a;
    double sin_sin_a = Math.sin(phi) * sin_a;
    double ux = su.x * cos_sin_a;
    double uy = su.y * cos_sin_a;
    double uz = su.z * cos_sin_a;
    double vx = sv.x * sin_sin_a;
    double vy = sv.y * sin_sin_a;
    double vz = sv.z * sin_sin_a;
    double wx = sw.x * cos_a;
    double wy = sw.y * cos_a;
    double wz = sw.z * cos_a;

    d.x = ux + vx + wx;
    d.y = uy + vy + wy;
    d.z = uz + vz + wz;
    d.normalize();
  }

  /**
   * @param ray  ray to modify
   * @param n    intersection normal
   * @param rand random number generator
   */
  public static void diffuseReflection(Ray ray, Vector3 n, Random rand) {
    double x1 = rand.nextDouble();
    double x2 = rand.nextDouble();
    double r = Math.sqrt(x1);
    double theta = 2 * Math.PI * x2;

    // project to point on hemisphere in tangent space
    double tx = r * Math.cos(theta);
    double ty = r * Math.sin(theta);
    double tz = Math.sqrt(1 - x1);

    // transform from tangent space to world space
    double xx, xy, xz;
    double ux, uy, uz;
    double vx, vy, vz;

    if (n.x > .1 || n.x < -.1) {
      xx = 0;
      xy = 1;
      xz = 0;
    } else {
      xx = 1;
      xy = 0;
      xz = 0;
    }

    ux = xy * n.z - xz * n.y;
    uy = xz * n.x - xx * n.z;
    uz = xx * n.y - xy * n.x;

    r = 1 / Math.sqrt(ux * ux + uy * uy + uz * uz);

    ux *= r;
    uy *= r;
    uz *= r;

    vx = uy * n.z - uz * n.y;
    vy = uz * n.x - ux * n.z;
    vz = ux * n.y - uy * n.x;

    ray.d.x = ux * tx + vx * ty + n.x * tz;
    ray.d.y = uy * tx + vy * ty + n.y * tz;
    ray.d.z = uz * tx + vz * ty + n.z * tz;
    ray.o.scaleAdd(EPSILON, ray.d);
  }

  public boolean closestIntersection(Ray ray, Intersection isect) {
    boolean hit = false;
    isect.t = Double.POSITIVE_INFINITY;
    for (Geom s : objects) {
      if (s.intersect(ray, isect)) {
        hit = true;
      }
    }
    if (hit) {
      isect.x.scaleAdd(isect.t, ray.d, ray.o);
      isect.geom.calcNorm(isect);
    }
    return hit;
  }

  public boolean anyIntersection(Ray ray, Intersection isect) {
    isect.t = Double.POSITIVE_INFINITY;
    for (Geom s : objects) {
      if (s.intersect(ray, isect)) {
        return true;
      }
    }
    return false;
  }

  @Override public void initialize(URL location, ResourceBundle resources) {
    sppLabel.textProperty().bind(sppText);

    pauseBtn.setOnAction(event -> {
      synchronized (renderLock) {
        stopped = pauseBtn.isSelected();
        renderLock.notifyAll();
      }
    });

    sunlightBtn.setOnAction(event -> {
      synchronized (renderLock) {
        sunNext = sunlightBtn.isSelected();
        refresh = true;
      }
    });

    renderThread.start();
    sppThread.start();

    long seed = System.nanoTime();
    for (int i = 0; i < NUM_WORKERS; ++i) {
      workers[i] = new Worker(this, seed + i);
      workers[i].start();
    }

    synchronized (renderLock) {
      stopped = false;
      renderLock.notifyAll();
    }

    canvas.setOnMousePressed(event -> {
      lastMouseX = (int) event.getX();
      lastMouseY = (int) event.getY();
    });

    canvas.setOnMouseMoved(event -> {
      lastMouseX = (int) event.getX();
      lastMouseY = (int) event.getY();
    });

    canvas.setOnMouseDragged(event -> {
      int dx = (int) (event.getX() - lastMouseX);
      int dy = (int) (event.getY() - lastMouseY);
      lastMouseX = (int) event.getX();
      lastMouseY = (int) event.getY();
      tempCamera.rotateView(-dx * 0.005, -dy * 0.005);
      synchronized (renderLock) {
        refresh = true;
      }
    });

    canvas.setOnMouseClicked(event -> {
      if (event.isStillSincePress()) {
        toggleLight(event.getX(), event.getY());
      }
    });
  }

  public void cameraTransform(Ray ray, double x, double y) {
    camera.calcViewRay(ray, x, y);
  }
}
