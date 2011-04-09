/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.scenario.scenegraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.awt.print.PrinterGraphics;
import java.awt.print.PrinterJob;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * Graphics2D to make transformed swing painting more appealing.   
 * 
 * @author Igor Kushnirskiy
 */

class SGComponentGraphics extends Graphics2D implements
        PrinterGraphics {
    // TODO: installing a custom Stroke causes Apple's JDK
    // (specifically the Quartz pipeline) to choke, so for now
    // only do this on non-Mac platforms
    static final boolean useCustomStroke =
        !"Mac OS X".equals(System.getProperty("os.name"));
    
    static class SGComponentStroke implements Stroke {
        private static final AffineTransform transform = 
            AffineTransform.getTranslateInstance(.5d, .5d); 
        private final BasicStroke delegate;
        SGComponentStroke(BasicStroke stroke) {
            delegate = stroke;
        }
        
        @Override
		public Shape createStrokedShape(Shape s) {
            Shape shape = delegate.createStrokedShape(s);
            return transform.createTransformedShape(shape);
        }

        public float[] getDashArray() {
            return delegate.getDashArray();
        }

        public float getDashPhase() {
            return delegate.getDashPhase();
        }

        public int getEndCap() {
            return delegate.getEndCap();
        }

        public int getLineJoin() {
            return delegate.getLineJoin();
        }

        public float getLineWidth() {
            return delegate.getLineWidth();
        }

        public float getMiterLimit() {
            return delegate.getMiterLimit();
        }

        @Override
		public String toString() {
            return delegate.toString();
        }
    }
    
    private final Graphics2D delegate;
    SGComponentGraphics(Graphics2D graphics) {
        delegate = graphics;
        Stroke stroke = delegate.getStroke();
        if (useCustomStroke && (stroke instanceof BasicStroke)) {
            delegate.setStroke(new SGComponentStroke((BasicStroke) stroke));
        }
    }
    
    @Override
	public void addRenderingHints(Map< ? , ? > hints) {
        delegate.addRenderingHints(hints);
    }

    @Override
	public void clearRect(int x, int y, int width, int height) {
        delegate.clearRect(x, y, width, height);
    }

    @Override
	public void clip(Shape s) {
        delegate.clip(s);
    }

    @Override
	public void clipRect(int x, int y, int width, int height) {
        delegate.clipRect(x, y, width, height);
    }

    @Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        delegate.copyArea(x, y, width, height, dx, dy);
    }

    @Override
	public Graphics create() {
        return delegate.create();
    }

    @Override
	public Graphics create(int x, int y, int width, int height) {
        return delegate.create(x, y, width, height);
    }

    @Override
	public void dispose() {
        delegate.dispose();
    }

    @Override
	public void draw(Shape s) {
        delegate.draw(s);
    }

    @Override
	public void draw3DRect(int x, int y, int width, int height,
            boolean raised) {
        delegate.draw3DRect(x, y, width, height, raised);
    }

    @Override
	public void drawArc(int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        delegate.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
	public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        delegate.drawBytes(data, offset, length, x, y);
    }

    @Override
	public void drawChars(char[] data, int offset, int length, int x, int y) {
        delegate.drawChars(data, offset, length, x, y);
    }

    @Override
	public void drawGlyphVector(GlyphVector g, float x, float y) {
        delegate.drawGlyphVector(g, x, y);
    }

    @Override
	public void drawImage(BufferedImage img, BufferedImageOp op, int x,
            int y) {
        delegate.drawImage(img, op, x, y);
    }

    @Override
	public boolean drawImage(Image img, AffineTransform xform,
            ImageObserver obs) {
        return delegate.drawImage(img, xform, obs);
    }

    @Override
	public boolean drawImage(Image img, int x, int y, Color bgcolor,
            ImageObserver observer) {
        return delegate.drawImage(img, x, y, bgcolor, observer);
    }

    @Override
	public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        return delegate.drawImage(img, x, y, observer);
    }

    @Override
	public boolean drawImage(Image img, int x, int y, int width,
            int height, Color bgcolor, ImageObserver observer) {
        return delegate.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    @Override
	public boolean drawImage(Image img, int x, int y, int width,
            int height, ImageObserver observer) {
        return delegate.drawImage(img, x, y, width, height, observer);
    }

    @Override
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
            int sx1, int sy1, int sx2, int sy2, Color bgcolor,
            ImageObserver observer) {
        return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2,
            bgcolor, observer);
    }

    @Override
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
            int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2,
            observer);
    }

    @Override
	public void drawLine(int x1, int y1, int x2, int y2) {
        delegate.drawLine(x1, y1, x2, y2);
    }

    @Override
	public void drawOval(int x, int y, int width, int height) {
        delegate.drawOval(x, y, width, height);
    }

    @Override
	public void drawPolygon(int[] points, int[] points2, int points3) {
        delegate.drawPolygon(points, points2, points3);
    }

    @Override
	public void drawPolygon(Polygon p) {
        delegate.drawPolygon(p);
    }

    @Override
	public void drawPolyline(int[] points, int[] points2, int points3) {
        delegate.drawPolyline(points, points2, points3);
    }

    @Override
	public void drawRect(int x, int y, int width, int height) {
        delegate.drawRect(x, y, width, height);
    }

    @Override
	public void drawRenderableImage(RenderableImage img,
            AffineTransform xform) {
        delegate.drawRenderableImage(img, xform);
    }

    @Override
	public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        delegate.drawRenderedImage(img, xform);
    }

    @Override
	public void drawRoundRect(int x, int y, int width, int height,
            int arcWidth, int arcHeight) {
        delegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
	public void drawString(AttributedCharacterIterator iterator, float x,
            float y) {
        delegate.drawString(iterator, x, y);
    }

    @Override
	public void drawString(AttributedCharacterIterator iterator, int x,
            int y) {
        delegate.drawString(iterator, x, y);
    }

    @Override
	public void drawString(String str, float x, float y) {
        delegate.drawString(str, x, y);
    }

    @Override
	public void drawString(String str, int x, int y) {
        delegate.drawString(str, x, y);
    }
    
    @Override
	public void fill(Shape s) {
        delegate.fill(s);
    }

    @Override
	public void fill3DRect(int x, int y, int width, int height,
            boolean raised) {
        delegate.fill3DRect(x, y, width, height, raised);
    }

    @Override
	public void fillArc(int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        delegate.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
	public void fillOval(int x, int y, int width, int height) {
        delegate.fillOval(x, y, width, height);
    }

    @Override
	public void fillPolygon(int[] points, int[] points2, int points3) {
        delegate.fillPolygon(points, points2, points3);
    }

    @Override
	public void fillPolygon(Polygon p) {
        delegate.fillPolygon(p);
    }

    @Override
	public void fillRect(int x, int y, int width, int height) {
        delegate.fillRect(x, y, width, height);
    }

    @Override
	public void fillRoundRect(int x, int y, int width, int height,
            int arcWidth, int arcHeight) {
        delegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
	public void finalize() {
        delegate.finalize();
    }

    @Override
	public Color getBackground() {
        return delegate.getBackground();
    }

    @Override
	public Shape getClip() {
        return delegate.getClip();
    }

    @Override
	public Rectangle getClipBounds() {
        return delegate.getClipBounds();
    }

    @Override
	public Rectangle getClipBounds(Rectangle r) {
        return delegate.getClipBounds(r);
    }

    @Override
	@SuppressWarnings("deprecation")
    public Rectangle getClipRect() {
        return delegate.getClipBounds();
    }

    @Override
	public Color getColor() {
        return delegate.getColor();
    }

    @Override
	public Composite getComposite() {
        return delegate.getComposite();
    }

    @Override
	public GraphicsConfiguration getDeviceConfiguration() {
        return delegate.getDeviceConfiguration();
    }

    @Override
	public Font getFont() {
        return delegate.getFont();
    }

    @Override
	public FontMetrics getFontMetrics() {
        return delegate.getFontMetrics();
    }

    @Override
	public FontMetrics getFontMetrics(Font f) {
        return delegate.getFontMetrics(f);
    }

    @Override
	public FontRenderContext getFontRenderContext() {
        return delegate.getFontRenderContext();
    }

    @Override
	public Paint getPaint() {
        return delegate.getPaint();
    }

    @Override
	public Object getRenderingHint(Key hintKey) {
        return delegate.getRenderingHint(hintKey);
    }

    @Override
	public RenderingHints getRenderingHints() {
        return delegate.getRenderingHints();
    }

    @Override
	public Stroke getStroke() {
        return delegate.getStroke();
    }

    @Override
	public AffineTransform getTransform() {
        return delegate.getTransform();
    }

    @Override
	public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return delegate.hit(rect, s, onStroke);
    }

    @Override
	public boolean hitClip(int x, int y, int width, int height) {
        return delegate.hitClip(x, y, width, height);
    }

    @Override
	public void rotate(double theta, double x, double y) {
        delegate.rotate(theta, x, y);
    }

    @Override
	public void rotate(double theta) {
        delegate.rotate(theta);
    }

    @Override
	public void scale(double sx, double sy) {
        delegate.scale(sx, sy);
    }

    @Override
	public void setBackground(Color color) {
        delegate.setBackground(color);
    }

    @Override
	public void setClip(int x, int y, int width, int height) {
        delegate.setClip(x, y, width, height);
    }

    @Override
	public void setClip(Shape clip) {
        delegate.setClip(clip);
    }

    @Override
	public void setColor(Color c) {
        delegate.setColor(c);
    }

    @Override
	public void setComposite(Composite comp) {
        delegate.setComposite(comp);
    }

    @Override
	public void setFont(Font font) {
        delegate.setFont(font);
    }

    @Override
	public void setPaint(Paint paint) {
        delegate.setPaint(paint);
    }

    @Override
	public void setPaintMode() {
        delegate.setPaintMode();
    }

    @Override
	public void setRenderingHint(Key hintKey, Object hintValue) {
        delegate.setRenderingHint(hintKey, hintValue);
    }

    @Override
	public void setRenderingHints(Map< ? , ? > hints) {
        delegate.setRenderingHints(hints);
    }

    @Override
	public void setStroke(Stroke s) {
        delegate.setStroke(s);
    }

    @Override
	public void setTransform(AffineTransform Tx) {
        delegate.setTransform(Tx);
    }

    @Override
	public void setXORMode(Color c1) {
        delegate.setXORMode(c1);
    }

    @Override
	public void shear(double shx, double shy) {
        delegate.shear(shx, shy);
    }

    @Override
	public String toString() {
        return delegate.toString();
    }

    @Override
	public void transform(AffineTransform Tx) {
        delegate.transform(Tx);
    }

    @Override
	public void translate(double tx, double ty) {
        delegate.translate(tx, ty);
    }

    @Override
	public void translate(int x, int y) {
        delegate.translate(x, y);
    }

    @Override
	public PrinterJob getPrinterJob() {
        //we do not need to return anything here.
        return null;
    }
}
