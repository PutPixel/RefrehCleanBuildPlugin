package com.github.putpixel.refrehcleanbuild;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.ui.progress.IProgressConstants2;

import refrehcleanbuild.Activator;

public class RefreshCleanBuildCommand extends AbstractHandler {

    private static final int MAX_ATTEMPTS = 5;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Job job = new Job("Refresh Clean Build") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                int i = 0;
                while (i < MAX_ATTEMPTS) {
                    refreshAll(monitor);
                    cleanAll(monitor);

                    List<IMarker> errorMarkers = getErrorMarkers();
                    if (errorMarkers.isEmpty()) {
                        buildAll(monitor);
                        return Status.OK_STATUS;
                    }

                    boolean hasOtherProblems = wsHasProblemsThatCantBeSolvedByCleanRefresh(errorMarkers);
                    if (hasOtherProblems) {
                        return new Status(IStatus.WARNING, Activator.PLUGIN_ID,
                                "Workspace has problems that can't be solved by clean-refresh", null);
                    }

                    monitor.worked(1);
                    i++;
                }
                return new Status(IStatus.WARNING, Activator.PLUGIN_ID,
                        "Workspace problems were not resolved within " + MAX_ATTEMPTS + " clean-refresh attempts", null);
            }

        };
        job.setUser(true);
        job.setProperty(IProgressConstants2.SHOW_IN_TASKBAR_ICON_PROPERTY, Boolean.TRUE);
        job.schedule();
        return "Done";
    }

    private void refreshAll(IProgressMonitor monitor) {
        try {
            ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, monitor);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    protected void buildAll(IProgressMonitor monitor) {
        try {
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    private void cleanAll(IProgressMonitor monitor) {
        try {
            ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean wsHasProblemsThatCantBeSolvedByCleanRefresh(List<IMarker> errorMarkers) {
        return errorMarkers.stream()
                .map(m -> toMessage(m))
                .anyMatch(message -> !isDeletionProblemMessage(message));
    }

    private List<IMarker> getErrorMarkers() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        try {
            return Arrays.asList(workspace.getRoot().findMarkers(
                    IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE)).stream()
                    .filter(m -> isErrorMarker(m))
                    .collect(Collectors.toList());
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isDeletionProblemMessage(String message) {
        return message.contains("Could not delete") && message.contains("try refreshing");
    }

    private String toMessage(IMarker m) {
        return m.getAttribute(IMarker.MESSAGE, "Should fail");
    }

    private boolean isErrorMarker(IMarker marker) {
        Integer severityType;
        try {
            severityType = (Integer) marker.getAttribute(IMarker.SEVERITY);
            return severityType.intValue() == IMarker.SEVERITY_ERROR;
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
}
