package hoge.util;

import java.io.IOException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IPackageFragment;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;

public class JavassistUtil {
	String outPath;
	ClassPool classPool;

	public JavassistUtil(String outPath) {
		this.outPath = outPath;
		classPool = new ClassPool();
		try {
			classPool.insertClassPath(outPath);
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
	}

	// パッケージ内全クラスの全メソッドをこじ開ける
	// TODO: できれば並列化
	public void openAll(IPackageFragment iPackageFragment) {
		// GCされないっぽいので逐一生成
		String packageName = iPackageFragment.getElementName();
		classPool.importPackage(packageName);

		try {
			for (ICompilationUnit cu : iPackageFragment.getCompilationUnits()) {
				if (hasPrivateAccess(cu)) {
					IResource resource = cu.getResource();

					// touchして後のincrementalBuildで拾わせる
					resource.touch(null);

					// TODO: エディタのマーカを消したい！
					// for (IMarker marker : resource.findMarkers(null, true,
					// IResource.DEPTH_INFINITE)) {
					// System.out.println(cu.getElementName() + " : " + marker);
					// marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_LOW);
					// marker.setAttribute(IMarker.LINE_NUMBER, (int)
					// marker.getAttribute(IMarker.LINE_NUMBER) + 2);
					// marker.delete();
					// }
					// cu.getResource().deleteMarkers(null, true, IResource.DEPTH_INFINITE);

					// cu.commitWorkingCopy(true, null);
					cu.save(null, true);

					continue;
				}

				// クラス洗ってこじ開ける
				for (IJavaElement e : cu.getChildren()) {
					if (IJavaElement.TYPE != e.getElementType()) {
						continue;
					}

					// TODO: classへのアクセス権限はいじってない
					CtClass ctClass = classPool.getCtClass(packageName + "." + e.getElementName());
					openAllMethods(ctClass);
					if (ctClass.isModified()) {
						ctClass.writeFile(outPath);
					}
				}
			}
		} catch (NotFoundException | CannotCompileException | IOException | CoreException e) {
			e.printStackTrace();
		}
	}

	// クラス内全メソッドをこじ開ける
	// TODO: interfaceからimplした場合の動作が不明
	private void openAllMethods(CtClass ctClass) {
		for (MethodInfo mi : ctClass.getClassFile().getMethods()) {
			// コンパイラがゴニョるのはこれくらいか
			// 他に識別方法が提供されてないか
			if (mi.getName().equals("<clinit>") || mi.getName().equals("<init>")) {
				continue;
			}

			if (!AccessFlag.isPublic(mi.getAccessFlags())) {
				mi.setAccessFlags(AccessFlag.PUBLIC);
				System.out.println("[BecomePublic] " + ctClass.getName() + "#" + mi.getName());
			}
		}
	}

	// privateアクセスのErrorマーカーを確認
	private boolean hasPrivateAccess(ICompilationUnit cu) throws CoreException {
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
