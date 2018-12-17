package hoge.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
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
 * Handles methods that relying with Eclipse IProject, IJavaProject.
 */
public class EclipseUtil {

	/**
	 * Get active(opening in editor) IProject. Show workbench and editor inner.
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

	/**
	 * @param iJavaProject
	 * @return java output files (class files) path
	 * @throws JavaModelException
	 */
	public static IPath getOutPath(IJavaProject iJavaProject) {
		IProject iProject = iJavaProject.getProject();
		IPath relativePath;
		try {
			relativePath = iJavaProject.getOutputLocation();
		} catch (JavaModelException e) {
			e.printStackTrace();
			return null;
		}
		return iProject.getLocation().removeLastSegments(1).append(relativePath);
	}

}
