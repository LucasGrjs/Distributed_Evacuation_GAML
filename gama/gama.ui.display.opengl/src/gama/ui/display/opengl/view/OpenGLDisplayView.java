/*******************************************************************************************************
 *
 * OpenGLDisplayView.java, in gama.ui.display.opengl, is part of the source code of the GAMA modeling and simulation
 * platform (v.2.0.0).
 *
 * (c) 2007-2024 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, TLU, CTU)
 *
 * Visit https://github.com/gama-platform/gama2 for license information and contacts.
 *
 ********************************************************************************************************/
package gama.ui.display.opengl.view;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import gama.core.common.interfaces.IDisposable;
import gama.core.runtime.GAMA;
import gama.core.runtime.PlatformHelper;
import gama.dev.DEBUG;
import gama.ui.experiment.views.displays.LayeredDisplayView;

/**
 * Class OpenGLLayeredDisplayView.
 *
 * @author drogoul
 * @since 25 mars 2015
 *
 */
public class OpenGLDisplayView extends LayeredDisplayView {

	{
		DEBUG.OFF();
	}

	/** The id. */
	public static String ID = "gama.ui.application.view.OpenGLDisplayView";

	@Override
	public SWTOpenGLDisplaySurface getDisplaySurface() { return (SWTOpenGLDisplaySurface) super.getDisplaySurface(); }

	@Override
	protected Composite createSurfaceComposite(final Composite parent) {
		final SWTOpenGLDisplaySurface surface =
				(SWTOpenGLDisplaySurface) GAMA.getGui().createDisplaySurfaceFor(getOutput(), parent);
		surfaceComposite = surface.renderer.getCanvas();
		// synchronizer.setSurface(getDisplaySurface());
		surface.outputReloaded();
		return surfaceComposite;
	}

	/**
	 * Gets the GL canvas.
	 *
	 * @return the GL canvas
	 */
	protected GamaGLCanvas getGLCanvas() { return (GamaGLCanvas) surfaceComposite; }

	/**
	 * Checks if is open GL.
	 *
	 * @return true, if is open GL
	 */
	@Override
	public boolean isOpenGL() { return true; }

	@Override
	public Control[] getZoomableControls() {
		// surfaceComposite is a GamaGLCanvas which contains a sub-canvas : this one should have the keyboard/mouse
		// focus
		return surfaceComposite.getChildren();
	}

	@Override
	public boolean forceOverlayVisibility() {
		final SWTOpenGLDisplaySurface surface = getDisplaySurface();
		return surface != null && surface.getROIDimensions() != null;
	}

	// /**
	// * Gets the multi listener.
	// *
	// * @return the multi listener
	// */
	@Override
	public IDisposable getMultiListener() {
		return new NEWTLayeredDisplayMultiListener(decorator, getDisplaySurface(), getGLCanvas().getNEWTWindow());
	}

	/**
	 * Hide canvas.
	 */
	@Override
	public void hideCanvas() {
		getGLCanvas().setVisible(false);
	}

	/**
	 * Show canvas.
	 */
	@Override
	public void showCanvas() {
		getGLCanvas().setVisible(true);
		// Maybe only necessary on macOS ? Prevents JOGL views to move over Java2D views created before
		if (PlatformHelper.isMac()) { getGLCanvas().reparentWindow(); }
	}

	/**
	 * Show canvas.
	 */
	@Override
	public void focusCanvas() {
		getGLCanvas().setFocus();
	}

	@Override
	public void ownCreatePartControl(final Composite c) {
		super.ownCreatePartControl(c);
		getSurfaceComposite().forceFocus();
	}

	@Override
	public ICameraHelper getCameraHelper() { return getDisplaySurface().renderer.getCameraHelper(); }

	@Override
	public boolean hasCameras() {
		return true;
	}

	@Override
	public boolean is2D() {
		return false;
	}

}
