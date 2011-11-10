package ch.gleisdrei.eclipse.error.badge;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.TaskBar;
import org.eclipse.swt.widgets.TaskItem;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

public class MarkerMonitor implements IResourceChangeListener, IStartup {

	private Set<Long> markerIds = new HashSet<Long>();

	private int previousNumberOfMarkers;

	public void earlyStartup() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
				IResourceChangeEvent.POST_CHANGE);
		try {
			IMarker[] markers = root.findMarkers(IMarker.PROBLEM, true,
					IResource.DEPTH_INFINITE);
			for (IMarker marker : markers) {
				if (marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) {
					markerIds.add(marker.getId());
				}
			}
			update();
		} catch (CoreException e) {
			// nop
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IMarkerDelta[] deltas = event.findMarkerDeltas(IMarker.PROBLEM, true);
		if (deltas != null) {
			for (IMarkerDelta delta : deltas) {
				switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					if (delta.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) {
						markerIds.add(delta.getId());
					}
					break;
				case IResourceDelta.REMOVED:
					if (delta.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) {
						markerIds.remove(delta.getId());
					}
					break;
				}
			}
			update();
		}
	}

	private void update() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				int numberOfMarkers = markerIds.size();
				if (numberOfMarkers != previousNumberOfMarkers) {
					updateTaskBarItem(numberOfMarkers > 0 ? String
							.valueOf(numberOfMarkers) : StringUtils.EMPTY);
					previousNumberOfMarkers = numberOfMarkers;
				}
			}
		});
	}

	private void updateTaskBarItem(String overlayText) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		TaskBar bar = workbench.getDisplay().getSystemTaskBar();
		if (bar != null) {
			TaskItem item = bar.getItem(workbench.getActiveWorkbenchWindow()
					.getShell());
			if (item == null) {
				item = bar.getItem(null);
			}

			if (item != null) {
				item.setOverlayText(overlayText);
			}
		}
	}
}