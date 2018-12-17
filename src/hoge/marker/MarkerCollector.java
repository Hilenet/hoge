package hoge.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IPackageFragment;

/**
 * Collects compilation problems as markerInfo.
 */
public class MarkerCollector {

	/**
	 * Collect error marker from given IPackageFragment[].
	 * 
	 * 
	 * @param packageFragments
	 * @param touchFlag
	 * @return collected markers
	 * @throws CoreException
	 */
	public static MarkerRoot collect(IPackageFragment[] packageFragments, boolean touchFlag) throws CoreException {
		MarkerRoot markerRoot = new MarkerRoot();
		for (IPackageFragment packageFragment : packageFragments) {
			for (ICompilationUnit compilationUnit : packageFragment.getCompilationUnits()) {
				IResource soureFile = compilationUnit.getResource();
				IMarker[] markers = soureFile.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true,
						IResource.DEPTH_INFINITE);

				boolean isChanged = false;
				for (IMarker marker : markers) {
					if (50 == (Integer) marker.getAttribute("categoryId")) {
						/* 3:junit_test_exdample.Klass#private_add#int, int */
						String targetAttr = (String) marker.getAttribute("arguments");
						if ('3' != targetAttr.charAt(0)) {
							continue;
						}
						MarkerInfo markerInfo = MarkerInfo.convertFromRaw(targetAttr);
						markerRoot.addMarkerInfo(markerInfo);
						isChanged = true;
					}
				}

				if (isChanged) {
					soureFile.touch(null);
				}
			}
		}
		return markerRoot;
	}

}
