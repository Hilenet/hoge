package hoge.util;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

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

	/**
	 * Get active(opening in editor) project. Show workbench and editor inner.
	 * 
	 * @return workbench active project
	 */
	static public IProject getActiveIProject() {
		IWorkbench iworkbench = PlatformUI.getWorkbench();
		if (iworkbench == null) {
			return null;
		}
		IWorkbenchWindow iworkbenchwindow = iworkbench.getActiveWorkbenchWindow();
		if (iworkbenchwindow == null) {
			return null;
		}
		IWorkbenchPage iworkbenchpage = iworkbenchwindow.getActivePage();
		if (iworkbenchpage == null) {
			return null;
		}
		IEditorPart editor = iworkbenchpage.getActiveEditor();
		if (editor == null) {
			return null;
		}
		IFileEditorInput fileEditorInput = (IFileEditorInput) (editor.getEditorInput());

		IEditorInput input = editor.getEditorInput();
		if (!(input instanceof IFileEditorInput)) {
			return null;
		}
		IResource iResource = fileEditorInput.getFile();
		if (iResource == null) {
			return null;
		}
		IProject iProject = iResource.getProject();
		if (iProject == null) {
			return null;
		}

		return iProject;
	}

	/**
	 * Get IJavaProject from IProject.
	 * 
	 * @param iProject target IProject
	 * @return corresponding IJavaProject
	 */
	static public IJavaProject getIJavaProject(IProject iProject) {
		if (iProject == null) {
			return null;
		}
		IJavaProject iJavaProject = iProject.getAdapter(IJavaProject.class);
		if (iJavaProject == null) {
			iJavaProject = JavaCore.create(iProject);
		}
		return iJavaProject;
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
		try {
			// 対象package収集して
			ArrayList<IPackageFragment> packageList = new ArrayList<IPackageFragment>();
			for (IPackageFragmentRoot r : javaProject.getPackageFragmentRoots()) {
				digPackage(packageList, r);
			}

			// package内のclass分open
			String outPath = getOutPath().toString();
			JavassistUtil jUtil = new JavassistUtil(outPath);
			packageList.forEach((iPackageFragment) -> {
				jUtil.openAll(iPackageFragment);
			});
		} catch (JavaModelException e) {
			e.printStackTrace();
			return false;
		}

		// touchしておいたtestFileをビルド
		build(IncrementalProjectBuilder.INCREMENTAL_BUILD);

		return true;
		// TODO: エディタのマーカー処理; IResourceからマーカ消しても無駄，Ruler hoverをリフレッシュとか？
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

	/**
	 * @param iJavaProject
	 * @return java output files (class files) path
	 * @throws JavaModelException
	 */
	private IPath getOutPath() throws JavaModelException {
		IProject iProject = javaProject.getProject();
		return iProject.getLocation().removeLastSegments(1).append(javaProject.getOutputLocation());
	}

	// 再帰でサブパッケージ掘り返してまとめる
	// TODO: コールスタックとArrayList周りのメモリ調査
	/**
	 * Recursive!! Recursively dig packages from IPackageFragmentRoot and store in
	 * ArrayList.
	 * 
	 * @param packageList store found IPackageFragment target IPackageFragmentRoot
	 * @throws JavaModelException
	 */
	private void digPackage(ArrayList<IPackageFragment> packageList, IPackageFragmentRoot iPackageFragmentRoot)
			throws JavaModelException {
		// binパッケージ(jarとか)除く
		if (iPackageFragmentRoot.getKind() != IPackageFragmentRoot.K_SOURCE) {
			return;
		}
		// 直下階層のパッケージを走査
		for (IJavaElement e : iPackageFragmentRoot.getChildren()) {
			IPackageFragment fragment = (IPackageFragment) e;
			if (!fragment.hasChildren()) {
				continue;
			}
			// 子がサブ持ってれば再帰
			if (IJavaElement.PACKAGE_FRAGMENT == fragment.getElementType()) {
				packageList.add(fragment);
			} else if (IJavaElement.PACKAGE_FRAGMENT_ROOT == fragment.getElementType()) {
				digPackage(packageList, (IPackageFragmentRoot) fragment);
			}
		}
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

		// privateなTypeはこれで見れる
		// System.out.println("REF:
		// "+iJavaProject.getOption(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, false));
	}
}
