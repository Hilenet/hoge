package hoge.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.RegistryToggleState;

import hoge.util.Tadikarao;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SampleHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IJavaProject iJavaProject = Tadikarao.getIJavaProject(Tadikarao.getActiveIProject());
		if (iJavaProject == null) {
			System.out.println("[ERR] no open project");
			return false;
		}
		IWorkbench workbench = HandlerUtil.getActiveWorkbenchWindowChecked(event).getWorkbench();
		Tadikarao tadikarao = Tadikarao.getInstace();
		Boolean isActive = getCommandActiveStatus(workbench);
		Boolean success = false;
		if (isActive) {
			// リセットしてoffに
			success = tadikarao.reset(iJavaProject);
			setCommandActiveStatus(workbench, false);
		} else {
			// expandしてonに
			success = tadikarao.expand(iJavaProject);
			setCommandActiveStatus(workbench, true);
		}
		System.out.println("[" + (isActive ? "DeActivation" : "Activation") + "] proc " + success);
		// MessageDialog.openInformation(window.getShell(), "Hoge", "become: " +
		// getCommandActiveStatus(workbench));
		
		return null;
	}

	/**
	 * Get command active status.
	 * 
	 * @param workbench target workbench
	 * @return toggle activation status
	 */
	public static Boolean getCommandActiveStatus(IWorkbench workbench) {
		ICommandService commandService = (ICommandService) workbench.getService(ICommandService.class);
		Command command = commandService.getCommand(Tadikarao.COMMAND_ID);
		return (Boolean) command.getState(RegistryToggleState.STATE_ID).getValue();
	}

	/**
	 * Switch command active status(true/false).
	 * 
	 * @param workbench target workbench
	 * @param status    setting status
	 */
	public static void setCommandActiveStatus(IWorkbench workbench, Boolean status) {
		ICommandService commandService = (ICommandService) workbench.getService(ICommandService.class);
		Command command = commandService.getCommand(Tadikarao.COMMAND_ID);
		command.getState(RegistryToggleState.STATE_ID).setValue(status);
	}
}
