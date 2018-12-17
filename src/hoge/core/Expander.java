package hoge.core;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import hoge.marker.MarkerCollector;
import hoge.marker.MarkerInfo;
import hoge.marker.MarkerRoot;
import hoge.util.EclipseUtil;
import hoge.util.JavassistUtil;

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

	/**
	 * Expand target private methods of IJavaProject.
	 * 
	 * @param touchFlag
	 * @return success status
	 */
	public boolean expand(boolean touchFlag) {
		try {
			// 対象package収集して
			ArrayList<IPackageFragment> packageList = new ArrayList<IPackageFragment>();
			for (IPackageFragmentRoot r : iJavaProject.getPackageFragmentRoots()) {
				digPackage(packageList, r);
			}

			// マーカーベースで
			MarkerRoot markers = MarkerCollector.collect(packageList.toArray(new IPackageFragment[packageList.size()]),
					true);
			markers.getMarkers().forEach((fqcn, cMarkers) -> {
				Collection<MarkerInfo> infos = cMarkers.values();
				JavassistUtil.openMethodsOfClass(binPath, fqcn, infos.toArray(new MarkerInfo[infos.size()]));
			});
		} catch (CoreException e) {
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

}
