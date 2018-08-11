package hoge.util;

import java.awt.geom.CubicCurve2D;
import java.util.ArrayList;

import javax.swing.plaf.basic.BasicBorders.MarginBorder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.views.markers.MarkerSupportConstants;

import javassist.tools.framedump;

public class Tadikarao {
	// entry point
	// return Success status
	public boolean expand() {
		IProject iProject = getActiveIProject();
		if (iProject == null) {
			return false;
		}
		IJavaProject iJavaProject = getIJavaProject(iProject);

		// prebuild (build hookが望ましい)
		// build(iJavaProject, IncrementalProjectBuilder.FULL_BUILD);

		try {
			// 対象package収集して
			ArrayList<IPackageFragment> packageList = new ArrayList<IPackageFragment>();
			for (IPackageFragmentRoot r : iJavaProject.getPackageFragmentRoots()) {
				digPackage(packageList, r);
			}

			// package内のclass分open
			String outPath = getOutPath(iJavaProject).toString();
			JavassistUtil jUtil = new JavassistUtil(outPath);
			packageList.forEach((iPackageFragment) -> {
				jUtil.openAll(iPackageFragment);
			});
		} catch (JavaModelException e) {
			e.printStackTrace();

			return false;
		}

		// touchしておいたtestFileをビルド
		build(iJavaProject, IncrementalProjectBuilder.INCREMENTAL_BUILD);
		
		// TODO: エディタのマーカー処理
	}

	// fullBuild(before?)
	private boolean build(IJavaProject iJavaProject, int state) {
		setProjectOption(iJavaProject);
		IProject iProject = iJavaProject.getProject();
		try {
			iProject.build(state, null);
			iProject.open(null);
		} catch (CoreException e1) {
			e1.printStackTrace();
			return false;
		}
		return true;
	}

	// 選択されているプロジェクト検出してIProject返す
	// maybe null
	private IProject getActiveIProject() {
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

	private IJavaProject getIJavaProject(IProject iProject) {
		IJavaProject iJavaProject = iProject.getAdapter(IJavaProject.class);
		if (iJavaProject == null) {
			iJavaProject = JavaCore.create(iProject);
		}

		return iJavaProject;
	}

	private IPath getOutPath(IJavaProject iJavaProject) throws JavaModelException {
		IProject iProject = iJavaProject.getProject();
		return iProject.getLocation().removeLastSegments(1).append(iJavaProject.getOutputLocation());
	}

	// 再帰でサブパッケージ掘り返してまとめる
	// TODO: コールスタックとArrayList周りのメモリ調査
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
