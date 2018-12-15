package hoge.util;

import java.io.IOException;

import org.eclipse.jdt.core.IType;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;

public class JavassistUtil {

	// クラス内全メソッドをこじ開ける
	// TODO: interfaceからimplした場合の動作が不明
	public static void openAllMethods(CtClass ctClass) {
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

	/**
	 * @param binPath
	 * @param iType
	 * @return CtClass corresponding to IType or null
	 */
	public static CtClass getCtClass(String binPath, IType iType) {
		ClassPool classPool = new ClassPool();
		CtClass ctClass;
		try {
			classPool.insertClassPath(binPath);
			ctClass = classPool.getCtClass(iType.getFullyQualifiedName());
		} catch (NotFoundException e) {
			ctClass = null;
		}

		return ctClass;
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
