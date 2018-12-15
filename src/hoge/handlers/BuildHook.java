package hoge.handlers;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import hoge.core.Tadikarao;

/**
 * Hook all build process, then handles plug-in status unless batch compiling.
 *
 */
public class BuildHook extends CompilationParticipant {
	public BuildHook() {
		super();
	}

	@Override
	public int aboutToBuild(IJavaProject project) {
		return READY_FOR_BUILD;
	}

	@Override
	public boolean isActive(IJavaProject project) {
		return true;
	}
		
	/* (非 Javadoc)
	 * Switching activate-status to false when eclipse batch-compile is running.
	 * @see org.eclipse.jdt.core.compiler.CompilationParticipant#buildStarting(org.eclipse.jdt.core.compiler.BuildContext[], boolean)
	 */
	@Override
	public void buildStarting(BuildContext[] files, boolean isBatch) {
		super.buildStarting(files, isBatch);

		// TODO: watch opening project switching, booting eclipse
		Tadikarao tadikarao = Tadikarao.getInstace();
		if (tadikarao.isRunning()) {
			return;
		}
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (CommandHandler.getCommandActiveStatus(workbench) == true) {
			CommandHandler.setCommandActiveStatus(workbench, false);
			System.out.println("[AutoDisable] build detected");
		}
	}
}
