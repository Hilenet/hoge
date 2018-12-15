package hoge.core;

import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import hoge.util.EclipseUtil;
import hoge.util.JavassistUtil;
import javassist.CtClass;

/**
 * Actual entity of expand scope of IJavaProject. An instance corresponds to a
 * IJavaProject on execution.
 */
public class Expander {
	IJavaProject iJavaProject;
	String binPath;

	public Expander(IJavaProject iJavaProject) {
		this.iJavaProject = iJavaProject;
		binPath = EclipseUtil.getOutPath(iJavaProject).toString();
	}

	public boolean expand(boolean touchFlag) {
		try {
			// 対象package収集して
			ArrayList<IPackageFragment> packageList = new ArrayList<IPackageFragment>();
			for (IPackageFragmentRoot r : iJavaProject.getPackageFragmentRoots()) {
				digPackage(packageList, r);
			}

			// package内のclass分open
			packageList.forEach((iPackageFragment) -> {
				try {
					openInPackage(iPackageFragment, touchFlag);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			});
		} catch (JavaModelException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

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

	/**
	 * Expand all methods of iPackageFragment in CLASSFILE. If touchFlag is set,
	 * touch iCompilationUnit which access to private method.
	 * 
	 * @param iPackageFragment
	 * @param touchFlag
	 * @throws CoreException
	 */
	private void openInPackage(IPackageFragment iPackageFragment, boolean touchFlag) throws CoreException {
		for (ICompilationUnit iCompilationUnit : iPackageFragment.getCompilationUnits()) {
			// touch
			if (touchFlag && Expander.hasPrivateAccess(iCompilationUnit)) {
				IResource iResource = iCompilationUnit.getResource();
				iResource.touch(null);
				iCompilationUnit.save(null, true);
				continue;
			}

			// expand
			for (IJavaElement iJavaElement : iCompilationUnit.getChildren()) {
				if (IJavaElement.TYPE != iJavaElement.getElementType()) {
					continue;
				}
				CtClass ctClass = JavassistUtil.getCtClass(binPath, (IType) iJavaElement);
				JavassistUtil.openAllMethods(ctClass);
				JavassistUtil.updateCtClass(binPath, ctClass);
			}
		}

	}

	// privateアクセスのErrorマーカーを確認
	public static boolean hasPrivateAccess(ICompilationUnit cu) throws CoreException {
		IResource javaSourceFile = cu.getUnderlyingResource();
		IMarker[] markers = javaSourceFile.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true,
				IResource.DEPTH_INFINITE);
		for (IMarker marker : markers) {
			if (50 == (Integer) marker.getAttribute("categoryId")) {
				return true;
			}
		}

		return false;
	}
}
