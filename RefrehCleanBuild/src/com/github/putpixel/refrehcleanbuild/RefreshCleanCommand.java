package com.github.putpixel.refrehcleanbuild;

import org.eclipse.core.runtime.IProgressMonitor;

public class RefreshCleanCommand extends RefreshCleanBuildCommand {

    protected String getJobName() {
        return "Refresh Clean";
    }

    @Override
    protected void buildAll(IProgressMonitor monitor) {
        // No build
    }

}
