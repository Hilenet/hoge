package hoge.handlers;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * Be used after project starts and loads this plug-in.
 *
 */
public class Activator extends Plugin {
	public Activator() {
		// TODO: log4jの設定?
		Logger gLogger = Logger.getGlobal();
		gLogger.setLevel(Level.FINE);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}
}
