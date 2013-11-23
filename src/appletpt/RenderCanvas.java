/**
 * I consider this code to be so general that it should be public domain.
 * As far as I am concerned anyone can use this for anything they want,
 * but credits are appreciated if you do use my code =)
 * 2013 Jesper Öqvist <jesper@llbit.se>
 */
package appletpt;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

/**
 * @author Jesper Öqvist <jesper@llbit.se>
 */
@SuppressWarnings("serial")
public class RenderCanvas extends Canvas {

	private BufferStrategy bufferStrategy = null;
	private final BufferedImage buffer;
	private final int width;
	private final int height;

	public RenderCanvas(int width, int height) {
		this.width = width;
		this.height = height;
		setPreferredSize(new Dimension(width, height));

		buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}

	public synchronized void update(double[] samples) {
		int yoffset = 0;
		for (int y = 0; y < height; ++y) {
			int offset = yoffset;
			for (int x = 0; x < width; ++x) {
				int cr = (int) (samples[offset+0] * 255);
				int cg = (int) (samples[offset+1] * 255);
				int cb = (int) (samples[offset+2] * 255);
				cr = (cr>255)?255:cr;
				cg = (cg>255)?255:cg;
				cb = (cb>255)?255:cb;
				buffer.setRGB(x, y, (0xFF<<24) | (cr<<16) | (cg<<8) | (cb));
				offset += 3;
			}
			yoffset += width*3;
		}

		if (bufferStrategy == null) {
			createBufferStrategy(2);
			bufferStrategy = getBufferStrategy();
		}
		do {
			do {
				Graphics g = bufferStrategy.getDrawGraphics();
				g.drawImage(buffer, 0, 0, null);
				g.dispose();
			} while (bufferStrategy.contentsRestored());
			bufferStrategy.show();
		} while (bufferStrategy.contentsLost());
	}
}
