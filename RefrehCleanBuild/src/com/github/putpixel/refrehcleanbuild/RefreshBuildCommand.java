package com.github.putpixel.refrehcleanbuild;

import org.eclipse.core.runtime.IProgressMonitor;

public class RefreshBuildCommand extends RefreshCleanBuildCommand {

    @Override
    protected String getJobName() {
        return "Refresh Build";
    }

    @Override
    protected void cleanAll(IProgressMonitor monitor) {
        // No clean
    }

}
