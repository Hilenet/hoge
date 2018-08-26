package hoge.handlers;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {
	public Activator() {
		// TODO: log4jの設定
		// Logger gLogger = Logger.getGlobal();
		// gLogger.setLevel(Level.FINE);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}
}
