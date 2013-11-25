/**
 * I consider this code to be so general that it should be public domain.
 * As far as I am concerned anyone can use this for anything they want,
 * but credits are appreciated if you do use my code =)
 * 2013 Jesper Öqvist <jesper@llbit.se>
 */
package appletpt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JApplet;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;

/**
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class PT extends JApplet {

	private static final int NUM_WORKERS = Runtime.getRuntime().availableProcessors();
	protected static final double CAMERA_DISTANCE = 10;
	protected static final int IMAGE_SIZE = 350;
	protected static final double INV_IMG_SIZE = 1.0/IMAGE_SIZE;
	protected static final double FOV = Math.PI*.5;
	protected static final double TAN_FOV = 2*Math.tan(FOV*.5);

	final double[] buffer = new double[IMAGE_SIZE*IMAGE_SIZE*3];
	private final RenderCanvas canvas = new RenderCanvas(IMAGE_SIZE, IMAGE_SIZE);
	private final Collection<Geom> objects = new ArrayList<Geom>();
	private final Thread renderThread;
	private final Thread sppThread;
	private boolean stopped = true;
	protected final Object renderLock = new Object();
	protected AtomicInteger nextJob = new AtomicInteger(IMAGE_SIZE);
	protected CyclicBarrier barrier = new CyclicBarrier(NUM_WORKERS+1);
	protected int mx;
	protected int my;
	protected int spp = 0;
	protected double theta;
	protected boolean refresh = true;
	protected int nextSpp = 0;
	protected double nextTheta = -.1;
	protected boolean sunEnabled = true;
	protected double emittersMod = 1.0;
	protected boolean sunNext = sunEnabled;
	protected double emittersNext = emittersMod;
	protected double totalTime = 0;
	protected double totalRenderTime = 0;
	protected JLabel sppLabel = new JLabel("SPP: 0");
	protected Thread[] workers = new Thread[NUM_WORKERS];

	protected double cos_t;
	protected double sin_t;

	public static void main(String[] args) {
		final PT pt = new PT();
		JFrame frame = new JFrame("Path Tracing Applet");
		frame.add(pt);

		frame.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {
				pt.init();
				pt.start();
			}
			@Override
			public void windowIconified(WindowEvent e) {
			}
			@Override
			public void windowDeiconified(WindowEvent e) {
			}
			@Override
			public void windowDeactivated(WindowEvent e) {
			}
			@Override
			public void windowClosing(WindowEvent e) {
				pt.stop();
				pt.destroy();
			}
			@Override
			public void windowClosed(WindowEvent e) {
			}
			@Override
			public void windowActivated(WindowEvent e) {
			}
		});
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
	}

	public PT() {
		JCheckBox sunCB = new JCheckBox("Enable Sun");
		sunCB.setSelected(sunEnabled);
		sunCB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBox source = (JCheckBox) e.getSource();
				synchronized (renderLock) {
					sunNext = source.isSelected();
					refresh = true;
				}
			}
		});

		JCheckBox emitterCB = new JCheckBox("Enable Emitters");
		emitterCB.setSelected(emittersMod != 0);
		emitterCB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBox source = (JCheckBox) e.getSource();
				synchronized (renderLock) {
					emittersNext = source.isSelected() ? 1.0 : 0.0;
					refresh = true;
				}
			}
		});

		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		layout.setHorizontalGroup(layout.createParallelGroup()
			.addComponent(canvas)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(sunCB)
				.addPreferredGap(ComponentPlacement.UNRELATED)
				.addComponent(emitterCB)
				.addPreferredGap(ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addComponent(sppLabel)
				.addContainerGap()
			)
		);
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(canvas)
			.addGroup(layout.createParallelGroup(Alignment.BASELINE)
				.addComponent(sunCB)
				.addComponent(emitterCB)
				.addComponent(sppLabel)
			)
		);
		panel.setLayout(layout);

		add(panel);


		int text[][] = {
				{ 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1 },
				{ 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 1 },
				{ 1, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0 },
				{ 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 2 },
		};
		double x0 = 5;
		for (int i = 0; i < text.length; ++i) {
			for (int j = 0; j < text[0].length; ++j) {
				if (text[i][j] == 1) {
					objects.add(new Cube(
							new Vec(j-x0, i, 1.5),
							new Vec(0,1-i*.2,1),
							1));
				} else if (text[i][j] == 2) {
					Sphere light = new Sphere(
							new Vec(j-x0, i, 1.5),
							new Vec(1,1,.2),
							.7);
					light.setEmittance(6.f);
					objects.add(light);
				}
			}
		}
		objects.add(new Sphere(
				new Vec(1.5, -8, 1.5),
				new Vec(.4,  .4,   1),
				1.5f));
		objects.add(new Sphere(
				new Vec(4.6, -3, .5),
				new Vec(1,   .5, .5),
				2));
		objects.add(new Sphere(
				new Vec(6.6, -6.5, .5),
				new Vec(.5,     1, .5),
				4));
		objects.add(new Sphere(
				new Vec(-5, -6, -1.5),
				new Vec(1,   1,   .5),
				6));
		objects.add(new Sphere(
				new Vec(-2, 15, -1.5),
				new Vec(.7,  1,    1),
				12));
		objects.add(new Sphere(
				new Vec(-5, 10, -60),
				new Vec(1,   1,   1),
				62.25));

		initSun(2.4, -2.8);

		renderThread = new Thread() {
			@Override
			public void run() {
				try {
					long time;
					barrier.await();
					while (!isInterrupted()) {
						synchronized (renderLock) {
							while (stopped) {
								renderLock.wait();
							}
							if (refresh) {
								spp = nextSpp;
								theta = nextTheta;
								cos_t = Math.cos(theta);
								sin_t = Math.sin(theta);
								sunEnabled = sunNext;
								emittersMod = emittersNext;
								totalTime = 0;
								for (Geom geom: objects) {
									geom.emittance = geom.nextEmittance;
								}
								refresh = false;
							}
						}
						time = System.nanoTime();
						nextJob.set(0);
						synchronized (renderLock) {
							renderLock.notifyAll();
						}
						barrier.await();
						time = System.nanoTime()-time;
						canvas.update(buffer);
						double ms = time/1000000.0;
						synchronized (renderLock) {
							spp += 1;
							totalTime += ms;
						}
						//System.out.println(String.format("frame took %.3fms", ms));
					}
				} catch (InterruptedException e) {
				} catch (BrokenBarrierException e) {
				}
			}
		};
		sppThread = new Thread() {
			@Override
			public void run() {
				try {
					while (!isInterrupted()) {
						sleep(1000);
						synchronized (renderLock) {
							if (spp == 0) {
								sppLabel.setText("SPP: 0");
							} else {
								double sps = (1000*spp) / totalTime;
								sppLabel.setText(String.format("SPP: %d (%.1f/s)", spp, sps));
							}
						}
					}
				} catch (InterruptedException e) {
				}
			};
		};
	}

	@Override
	public void init() {
		canvas.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			@Override
			public void mousePressed(MouseEvent e) {
				mx = e.getX();
				my = e.getY();
			}
			@Override
			public void mouseExited(MouseEvent e) {
			}
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				toggleLight(e.getX(), e.getY());
			}
		});

		canvas.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
			}
			@Override
			public void mouseDragged(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				int dx = x - mx;
				int dy = y - my;
				mx = x;
				my = y;
				synchronized (renderLock) {
					nextTheta -= dx*0.01;
					nextTheta = Math.max(-Math.PI*.45, Math.min(Math.PI*.5, nextTheta));
					nextSpp = 0;
					refresh = true;
				}
			}
		});
		renderThread.start();
		sppThread.start();

		long seed = System.nanoTime();
		for (int i = 0; i < NUM_WORKERS; ++i) {
			workers[i] = new Worker(this, seed+i);
			workers[i].start();
		}
	}

	protected void toggleLight(int x, int y) {
		Ray ray = new Ray();
		cameraTransform(ray, x, y);
		Intersection isect = new Intersection();
		if (closestIntersection(ray, isect)) {
			Geom geom = isect.geom;
			synchronized (renderLock) {
				geom.setEmittance((geom.emittance != 0)?0:1);
				if (emittersMod != 0) {
					refresh = true;
				}
			}
		}
	}

	@Override
	public void start() {
		synchronized (renderLock) {
			stopped = false;
			renderLock.notifyAll();
		}
	}

	@Override
	public void stop() {
		synchronized (renderLock) {
			stopped = true;
		}
	}

	@Override
	public void destroy() {
		for (int i = 0; i < NUM_WORKERS; ++i) {
			workers[i].interrupt();
		}
		sppThread.interrupt();
		renderThread.interrupt();
		try {
			renderThread.join();
		} catch (InterruptedException e) {
		}
	}

	private final Vec su = new Vec(0,0,0);
	private final Vec sv = new Vec(0,0,0);
	private final Vec sw = new Vec(0,0,0);

	private void initSun(double azimuth, double altitude) {
		double theta = azimuth;
		double phi = altitude;

		sw.set(Math.cos(theta),
			Math.sin(phi),
			Math.sin(theta));

		double r = Math.sqrt(sw.x*sw.x + sw.z*sw.z);
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

	public void getSunDirection(Vec d, Random rand) {
		double x1 = rand.nextFloat();
		double x2 = rand.nextFloat();
		double cos_a = 1-x1 + x1*RADIUS_COS;
		double sin_a = Math.sqrt(1 - cos_a*cos_a);
		double phi = 2 * Math.PI * x2;

		double cos_sin_a = Math.cos(phi)*sin_a;
		double sin_sin_a = Math.sin(phi)*sin_a;
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
	 * @param ray ray to modify
	 * @param n intersection normal
	 * @param rand
	 */
	public static void diffuseReflection(Ray ray, Vec n, Random rand) {
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

		r = 1/Math.sqrt(ux*ux + uy*uy + uz*uz);

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

	/**
	 * @param ray
	 * @param x screen-space x
	 * @param y screen-space y
	 */
	public void cameraTransform(Ray ray, double x, double y) {
		ray.o.x = sin_t * CAMERA_DISTANCE;
		ray.o.y = 0;
		ray.o.z = cos_t * CAMERA_DISTANCE;

		double dx = (TAN_FOV*-.5) + x * (TAN_FOV*INV_IMG_SIZE);
		double dy = (TAN_FOV*-.5) + y * (TAN_FOV*INV_IMG_SIZE);
		double dz = -1;

		ray.d.x = cos_t * dx + sin_t * dz;
		ray.d.y = dy;
		ray.d.z = -sin_t * dx + cos_t * dz;
		ray.d.normalize();
	}
}
