package hoge.util;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import hoge.marker.MarkerInfo;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;

/**
 * Handles methods that relying with Javassist.
 */
public class JavassistUtil {
	/**
	 * Expand given methods access scope in classfile.
	 * 
	 * @param binPath
	 * @param fqcn
	 * @param markers
	 * @return success status
	 */
	public static boolean openMethodsOfClass(String binPath, String fqcn, MarkerInfo[] markers) {
		ClassPool classPool = new ClassPool();
		List<String> markerInfos = Stream.of(markers).map(m -> m.toSignatureString()).collect(Collectors.toList());

		try {
			classPool.insertClassPath(binPath);
			CtClass targetClass = classPool.getCtClass(fqcn);
			List<MethodInfo> methodInfos = targetClass.getClassFile().getMethods();
			for (MethodInfo methodInfo : methodInfos) {
				String signature = methodInfo.toString();
				if (markerInfos.contains(signature.substring(0, signature.indexOf(')') + 1))) {
					methodInfo.setAccessFlags(AccessFlag.setPublic(methodInfo.getAccessFlags()));
					System.out.println("[BecomePublic] " + fqcn + "." + methodInfo.toString());
				}
			}
			updateCtClass(binPath, targetClass);
		} catch (NotFoundException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Write CtClass to file if it has been modified.
	 * 
	 * @param binPath
	 * @param ctClass
	 * @return success status
	 */
	public static boolean updateCtClass(String binPath, CtClass ctClass) {
		if (ctClass.isModified()) {
			try {
				ctClass.writeFile(binPath);
			} catch (CannotCompileException | IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
}
