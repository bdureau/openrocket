package net.sf.openrocket.gui.figure3d;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import net.sf.openrocket.appearance.Appearance;
import net.sf.openrocket.appearance.Decal;
import net.sf.openrocket.document.DecalRegistry;
import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.logging.LogHelper;
import net.sf.openrocket.rocketcomponent.RocketComponent;
import net.sf.openrocket.startup.Application;
import net.sf.openrocket.util.Color;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

public class RealisticRenderStrategy extends RenderStrategy {

	private final float[] colorBlack = { 0, 0, 0, 1 };
	private final float[] color = new float[4];
	private static final LogHelper log = Application.getLogger();

	private final DecalRegistry decalLoader;
	private boolean needClearCache = false;
	private Map<String, Texture> oldTexCache = new HashMap<String, Texture>();
	private Map<String, Texture> texCache = new HashMap<String, Texture>();

	public RealisticRenderStrategy(OpenRocketDocument document) {
		super(document);
		this.decalLoader = document.getDecalRegistry();
	}

	@Override
	public void updateFigure() {
		needClearCache = true;
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		oldTexCache = new HashMap<String,Texture>();
		texCache = new HashMap<String,Texture>();
		
		GL2 gl = drawable.getGL().getGL2();
		
		gl.glLightModelfv(GL2ES1.GL_LIGHT_MODEL_AMBIENT, 
                new float[] { 0,0,0 }, 0);

		float amb = 0.3f;
		float dif = 1.0f - amb;
		float spc = 1.0f;
		gl.glLightfv(GLLightingFunc.GL_LIGHT1, GLLightingFunc.GL_AMBIENT,
				new float[] { amb, amb, amb, 1 }, 0);
		gl.glLightfv(GLLightingFunc.GL_LIGHT1, GLLightingFunc.GL_DIFFUSE,
				new float[] { dif, dif, dif, 1 }, 0);
		gl.glLightfv(GLLightingFunc.GL_LIGHT1, GLLightingFunc.GL_SPECULAR,
				new float[] { spc, spc, spc, 1 }, 0);

		gl.glEnable(GLLightingFunc.GL_LIGHT1);
		gl.glEnable(GLLightingFunc.GL_LIGHTING);
		gl.glShadeModel(GLLightingFunc.GL_SMOOTH);

		gl.glEnable(GLLightingFunc.GL_NORMALIZE);
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		oldTexCache = null;
		texCache = null;
	}

	@Override
	public boolean isDrawn(RocketComponent c) {
		return true;
	}

	@Override
	public boolean isDrawnTransparent(RocketComponent c) {
		return false;
	}

	@Override
	public void preGeometry(GL2 gl, RocketComponent c, float alpha) {
		if (needClearCache) {
			clearCaches(gl);
			needClearCache = false;
		}

		Appearance a = getAppearance(c);
		gl.glLightModeli(GL2ES1.GL_LIGHT_MODEL_TWO_SIDE, 1);
		gl.glLightModeli(GL2.GL_LIGHT_MODEL_COLOR_CONTROL,GL2.GL_SEPARATE_SPECULAR_COLOR);


		convertColor(a.getDiffuse(), color);
		color[3] = alpha;
		gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_DIFFUSE, color, 0);
		gl.glMaterialfv(GL.GL_BACK, GLLightingFunc.GL_DIFFUSE, color, 0);

		convertColor(a.getAmbient(), color);
		color[3] = alpha;
		gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_AMBIENT, color, 0);
		gl.glMaterialfv(GL.GL_BACK, GLLightingFunc.GL_AMBIENT, color, 0);

		convertColor(a.getSpecular(), color);
		color[3] = alpha;
		gl.glMaterialfv(GL.GL_FRONT, GLLightingFunc.GL_SPECULAR, color, 0);
		gl.glMateriali(GL.GL_FRONT, GLLightingFunc.GL_SHININESS, a.getShininess());

		gl.glMaterialfv(GL.GL_BACK, GLLightingFunc.GL_SPECULAR, colorBlack, 0);
		gl.glMateriali(GL.GL_BACK, GLLightingFunc.GL_SHININESS, 0);

		Decal t = a.getTexture();
		Texture tex = null;
		if (t != null) {
			tex = getTexture(t);
		}
		if (t != null && tex != null) {
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);

			tex.enable(gl);
			tex.bind(gl);
			gl.glMatrixMode(GL.GL_TEXTURE);
			gl.glPushMatrix();

			gl.glTranslated(-t.getCenter().x, -t.getCenter().y, 0);
			gl.glRotated(57.2957795 * t.getRotation(), 0, 0, 1);
			gl.glTranslated(t.getCenter().x, t.getCenter().y, 0);

			gl.glScaled(t.getScale().x, t.getScale().y, 0);
			gl.glTranslated(t.getOffset().x, t.getOffset().y, 0);

			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, toEdgeMode(t.getEdgeMode()));
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, toEdgeMode(t.getEdgeMode()));

			gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		}
	}

	@Override
	public void postGeometry(GL2 gl, RocketComponent c, float alpha) {
		Appearance a = getAppearance(c);
		Decal t = a.getTexture();
		Texture tex = null;
		if (t != null) {
			tex = getTexture(t);
		}
		if (tex != null) {
			gl.glMatrixMode(GL.GL_TEXTURE);
			gl.glPopMatrix();
			gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
			tex.disable(gl);
		}
	}

	private void clearCaches(GL2 gl) {
		log.debug("ClearCaches");
		for (Map.Entry<String, Texture> e : oldTexCache.entrySet()) {
			log.debug("Destroying Texture for " + e.getKey());
			if (e.getValue() != null)
				e.getValue().destroy(gl);
		}
		oldTexCache = texCache;
		texCache = new HashMap<String, Texture>();
	}

	private Texture getTexture(Decal t) {
		String imageName = t.getImage();

		// Return the Cached value if available
		if (texCache.containsKey(imageName))
			return texCache.get(imageName);

		// If the texture is in the Old Cache, save it.
		if (oldTexCache.containsKey(imageName)) {
			texCache.put(imageName, oldTexCache.get(imageName));
			oldTexCache.remove(imageName);
			return texCache.get(imageName);
		}

		// Otherwise load it.
		Texture tex = null;
		try {
			log.debug("Loading texture " + t);
			InputStream is = decalLoader.getDecal(imageName);
			TextureData data = TextureIO.newTextureData(GLProfile.getDefault(), is, true, null);
			tex = TextureIO.newTexture(data);
		} catch (Throwable e) {
			log.error("Error loading Texture", e);
		}
		texCache.put(imageName, tex);

		return tex;

	}

	private Appearance getAppearance(RocketComponent c) {
		Appearance ret = c.getAppearance();
		if (ret == null) {
			ret = Appearance.MISSING;
		}
		return ret;
	}

	private int toEdgeMode(Decal.EdgeMode m) {
		switch (m) {
		case REPEAT:
			return GL.GL_REPEAT;
		case MIRROR:
			return GL.GL_MIRRORED_REPEAT;
		case CLAMP:
			return GL.GL_CLAMP_TO_EDGE;
		default:
			return GL.GL_CLAMP_TO_EDGE;
		}
	}

	protected static void convertColor(Color color, float[] out) {
		if (color == null) {
			out[0] = 1;
			out[1] = 1;
			out[2] = 0;
		} else {
			out[0] = (float) color.getRed() / 255f;
			out[1] = (float) color.getGreen() / 255f;
			out[2] = (float) color.getBlue() / 255f;
		}
	}
}