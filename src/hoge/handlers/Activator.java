package hoge.handlers;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {
	public static Bundle bundle;

	public Activator() {
		// Logger gLogger = Logger.getGlobal();
		// gLogger.setLevel(Level.FINE);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		// register main bundle
		bundle = context.getBundle();

		// TODO: 要チェック for ビルドhook
//		new CompilationParticipant() {
//		};
		
		// TODO: ビルド時のhook，これ
		// IResourceChangeListener listener = (e) -> {
		// System.out.println(e);
		// System.out.println("all");
		// Logger.getGlobal().log(null, "hoge");
		// };
		//
		// ResourcesPlugin.getWorkspace().addResourceChangeListener(listener,
		// IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE |
		// IResourceChangeEvent.PRE_BUILD
		// | IResourceChangeEvent.POST_BUILD | IResourceChangeEvent.POST_CHANGE);

	}
}
