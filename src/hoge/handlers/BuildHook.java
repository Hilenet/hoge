package hoge.handlers;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import hoge.util.Tadikarao;

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
	
	@Override
	public void buildStarting(BuildContext[] files, boolean isBatch) {
		super.buildStarting(files, isBatch);

		// TODO: watch opening project switching, booting eclipse
		// TODO: autoDisableじゃなくビルド時にhookしてやりたい (not batchに引っかかると詰む)
		Tadikarao tadikarao = Tadikarao.getInstace();
		if (tadikarao.isRunning()) {
			return;
		}
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (SampleHandler.getCommandActiveStatus(workbench)) {
			// tadikarao.reset(project); /* ここでbuildがデッドロック?  */
			SampleHandler.setCommandActiveStatus(workbench, false);
			System.out.println("[AutoDisable] build detected(not cleaned)");
		}
	}
}
