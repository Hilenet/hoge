package hoge.core;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * Plug-in Core, Singleton class Having ExpandThread instance, and supervise
 * expand process.
 */
public class Tadikarao {
	public static String COMMAND_ID = "hoge.commands.sampleCommand";

	static Tadikarao instance = null; /* as singleton */
	ExecutorService service; /* not concurrent... */
	Future<Boolean> future; /* hold using Callable */

	/**
	 * Singleton support.
	 * 
	 * @return usable instance
	 */
	public static synchronized Tadikarao getInstace() {
		if (instance == null) {
			instance = new Tadikarao();
		}
		return instance;
	}

	/**
	 * Not access from other class.
	 */
	private Tadikarao() {
		service = Executors.newSingleThreadExecutor();
		future = null;
	}

	/**
	 * Return running thread, and refresh thread variable.
	 * 
	 * @return living thread or null
	 */
	public Future<Boolean> getRunningThread() {
//		if (future != null && (future.isCancelled() || future.isDone())) {
//			future = null;
//		}
		return future;
	}

	/**
	 * Return expand process is running or not.
	 * 
	 * @return expand thread is running
	 */
	public boolean isRunning() {
		return (getRunningThread() != null);
	}

	/**
	 * Run expand process. Instantiate expand thread, and run expand process. Fail
	 * when thread already running.
	 * 
	 * @param javaProject target project
	 * @return success status
	 */
	public boolean expand(IJavaProject javaProject) {
		if (isRunning()) {
			return false;
		}
		future = service.submit(new ExpandThread(javaProject));
		boolean success = false;
		try {
			success = future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		future = null;

		return success;
	}

	/**
	 * Reset project classfiles.
	 * 
	 * @param javaProject
	 * @return success status
	 */
	public boolean reset(IJavaProject javaProject) {
		if (isRunning()) {
			return false;
		}
		future = service.submit(new ResetThread(javaProject));
		boolean success = false;
		System.out.println("[rb] build start");
		try {
			success = future.get(10, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
		}
		System.out.println("[rb] build fin");

		future = null;

		return success;
	}
}

/**
 * Entity of reset process.
 */
class ResetThread implements Callable<Boolean> {
	IJavaProject javaProject;

	/**
	 * @param javaProject target project
	 */
	public ResetThread(IJavaProject javaProject) {
		this.javaProject = javaProject;
	}

	@Override
	public Boolean call() throws Exception {
		return innerReset();
	}

	/**
	 * Build project with IProject build settings.
	 * 
	 * @param state IncrementalProjectBuilder build option
	 * @return succeed status
	 */
	private boolean innerReset() {
		try {
			javaProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		} catch (CoreException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}

/**
 * Entity of Expand process.
 */
class ExpandThread implements Callable<Boolean> {
	IJavaProject javaProject;

	/**
	 * @param javaProject target project.
	 */
	public ExpandThread(IJavaProject javaProject) {
		this.javaProject = javaProject;
	}

	@Override
	public Boolean call() throws Exception {
		return innerExpand();
	}

	/**
	 * Main process. Expand all unpublic methods of public classes of packages of
	 * project. Open with Javassist and compile classes(compilation unit) which
	 * having private access error marker. Note class path depends on project
	 * (default) out path.
	 * 
	 * @return success status
	 */
	private boolean innerExpand() {
		Expander expander = new Expander(javaProject);
		expander.expand(true);

		// touchしておいたtestFileをビルド
		build(IncrementalProjectBuilder.INCREMENTAL_BUILD);

		// TODO: エディタのマーカー処理; IResourceからマーカ消しても無駄，Ruler hoverをリフレッシュとか？

		return true;
	}

	/**
	 * Build project with IProject build settings.
	 * 
	 * @param state IncrementalProjectBuilder build option
	 * @return succeed status
	 */
	private boolean build(int state) {
		setProjectOption(javaProject);
		IProject iProject = javaProject.getProject();
		try {
			iProject.build(state, null);
			iProject.open(null);
		} catch (CoreException e1) {
			e1.printStackTrace();
			return false;
		}
		return true;
	}

	// いただいたコードから
	// TODO: 動いたらOptionの解読とゴミとり
	private void setProjectOption(IJavaProject iJavaProject) {
		if (iJavaProject.getOption(JavaCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER, false) == null) {
			iJavaProject.setOption(JavaCore.CORE_JAVA_BUILD_CLEAN_OUTPUT_FOLDER, JavaCore.IGNORE);
		}
		if (iJavaProject.getOption(JavaCore.COMPILER_LOCAL_VARIABLE_ATTR, false) == null) {
			iJavaProject.setOption(JavaCore.COMPILER_LOCAL_VARIABLE_ATTR, JavaCore.GENERATE);
		}
		if (iJavaProject.getOption(JavaCore.COMPILER_LINE_NUMBER_ATTR, false) == null) {
			iJavaProject.setOption(JavaCore.COMPILER_LINE_NUMBER_ATTR, JavaCore.GENERATE);
		}
		if (iJavaProject.getOption(JavaCore.COMPILER_CODEGEN_UNUSED_LOCAL, false) == null) {
			iJavaProject.setOption(JavaCore.COMPILER_CODEGEN_UNUSED_LOCAL, JavaCore.PRESERVE);
		}
	}
}
