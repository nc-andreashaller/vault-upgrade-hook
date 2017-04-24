/*
 * (C) Copyright 2016 Netcentric AG.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package biz.netcentric.vlt.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.InstallHook;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;

import biz.netcentric.vlt.upgrade.util.OsgiUtil;
import biz.netcentric.vlt.upgrade.util.PackageInstallLogger;

public class UpgradeProcessor implements InstallHook {

    private static final PackageInstallLogger LOG = PackageInstallLogger.create(UpgradeProcessor.class);

    private static final String STATUS_PATH = "/var/upgrade";
    private static final String UPGRADER_PATH_IN_PACKAGE = ".zip/jcr:content/vlt:definition/upgrader";

    // fields are package private for unit tests
    OsgiUtil osgi = new OsgiUtil();
    boolean failed = false;
    UpgradeStatus status;
    List<UpgradePackage> packages;

    // ----< InstallHook interface >--------------------------------------------

    @Override
    public void execute(InstallContext ctx) throws PackageException {
	LOG.info(ctx, "starting [{}{}]", ctx.getPhase(), failed ? ": FAILED" : "");

	try {
	    if (!failed) {
		switch (ctx.getPhase()) {
		case PREPARE:
		    ResourceResolver resourceResolver = getResourceResolver(ctx);
		    loadStatus(ctx, resourceResolver);
		    loadRelevantPackages(ctx, resourceResolver);
		    executePackages(ctx);
		    break;
		case INSTALLED:
		    executePackages(ctx);
		    break;
		case END:
		    executePackages(ctx);
		    status.update(ctx);
		    updateActions(ctx);
		    ctx.getSession().save();
		    break;
		case PREPARE_FAILED:
		case INSTALL_FAILED:
		default:
		    failed = true;
		    break;
		}
	    }
	} catch (Exception e) {
	    failed = true;
	    LOG.error(ctx, "Error during content upgrade", e);
	    throw new PackageException(e);
	} finally {
	    LOG.debug(ctx, "finished [{}{}]", ctx.getPhase(), failed ? ": FAILED" : "");
	}
    }

    private void updateActions(InstallContext ctx) throws RepositoryException {
	LOG.debug(ctx, "updating actions [{}]", packages);
	for (UpgradePackage pckg : packages) {
	    status.updateActions(ctx, pckg);
	}
    }

    private void executePackages(InstallContext ctx) throws RepositoryException {
	LOG.debug(ctx, "starting package execution [{}]", packages);
	for (UpgradePackage pckg : packages) {
	    pckg.execute(ctx);
	}
    }

    /**
     * Load and return all upgrade infos in the package.
     * 
     * @param ctx
     *            The install context.
     * @param resourceResolver
     * @return A list of upgrade infos.
     * @throws RepositoryException
     * @throws LoginException
     */
    protected void loadRelevantPackages(InstallContext ctx, ResourceResolver resourceResolver)
	    throws RepositoryException {

	String upgradeInfoPath = ctx.getPackage().getId().getInstallationPath() + UPGRADER_PATH_IN_PACKAGE;
	Resource upgradeInfoResource = resourceResolver.getResource(upgradeInfoPath);
	LOG.debug(ctx, "loading packages [{}]: [{}]", upgradeInfoPath, upgradeInfoResource);

	packages = new ArrayList<>();

	if (upgradeInfoResource != null) {

	    for (Resource resource : upgradeInfoResource.getChildren()) {
		final UpgradePackage pckg = new UpgradePackage(ctx, status, resource);
		if (pckg.isRelevant()) {
		    packages.add(pckg);
		} else {
		    LOG.debug(ctx, "package not relevant: [{}]", resource);
		}
	    }

	    // sort upgrade infos according to their version and priority
	    Collections.sort(packages);
	} else {
	    LOG.warn(ctx, "Could not load upgrade info [{}]", upgradeInfoPath);
	}
    }

    /**
     * Loads information about the last upgrade execution and populates
     * {@link #status} with it.
     * 
     * @param ctx
     *            The install context.
     * @param resourceResolver
     * @throws RepositoryException
     */
    protected void loadStatus(InstallContext ctx, ResourceResolver resourceResolver) throws RepositoryException {
	String statusPath = getStatusPath(ctx.getPackage().getId());
	status = new UpgradeStatus(ctx, resourceResolver, statusPath);
    }

    /**
     * Return the absolute JCR path to the version status information.
     * 
     * @param packageId
     *            The package ID to build the path from.
     * @return The status path.
     */
    protected String getStatusPath(PackageId packageId) {
	return STATUS_PATH + "/" + packageId.getGroup() + "/" + packageId.getName();
    }

    private ResourceResolver getResourceResolver(InstallContext ctx) throws LoginException {
	ResourceResolverFactory resourceResolverFactory = osgi.getService(ResourceResolverFactory.class);
	return resourceResolverFactory
		.getResourceResolver(Collections.<String, Object>singletonMap("user.jcr.session", ctx.getSession()));
    }

}
