package hoge.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

import hoge.util.Tadikarao;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SampleHandler extends AbstractHandler {
	// クラスファイルに編集かける
	// TODO: 可能ならばビルドでhook
	// TODO: toggleに書き換え
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("[Start] plugin hoge start running");
		Tadikarao tad = new Tadikarao();
		boolean success = tad.expand();
		System.out.println("[Finish] plugin hoge done\n");
		

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(window.getShell(), "Hoge", "Script " + success);

		return null;
	}
}
