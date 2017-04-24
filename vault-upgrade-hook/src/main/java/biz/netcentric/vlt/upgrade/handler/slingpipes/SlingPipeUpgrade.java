package biz.netcentric.vlt.upgrade.handler.slingpipes;

import java.util.Iterator;

import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.Pipe;
import org.apache.sling.pipes.Plumber;

import biz.netcentric.vlt.upgrade.handler.UpgradeAction;
import biz.netcentric.vlt.upgrade.util.PackageInstallLogger;

public class SlingPipeUpgrade extends UpgradeAction {

    private static final PackageInstallLogger LOG = PackageInstallLogger.create(SlingPipeUpgrade.class);

    private final Resource resource;
    private final Plumber service;

    public SlingPipeUpgrade(Plumber service, Resource resource) {
	super(resource.getName());
	this.service = service;
	this.resource = resource;
    }

    /**
     * Executes the sling pipe
     * 
     * @param pipePath
     *            the path a package definition to execute
     */
    @Override
    public void execute(InstallContext ctx) {
	Pipe pipe = service.getPipe(resource);
	if (pipe == null) {
	    throw new IllegalArgumentException("No valid pipe at " + resource);
	}
	LOG.debug(ctx, "Executing [{}]: [{}]", resource.getName(), pipe);

	final Iterator<Resource> output = pipe.getOutput();

	while (output.hasNext()) {
	    Resource r = output.next();
	    // output affected resource path for information
	    LOG.info(ctx, r.getPath());
	}
    }

}
