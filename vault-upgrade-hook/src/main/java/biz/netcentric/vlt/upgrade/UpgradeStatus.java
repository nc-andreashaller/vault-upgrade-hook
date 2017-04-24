package biz.netcentric.vlt.upgrade;

import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;

import biz.netcentric.vlt.upgrade.handler.UpgradeAction;
import biz.netcentric.vlt.upgrade.util.ComparableVersion;
import biz.netcentric.vlt.upgrade.util.PackageInstallLogger;

public class UpgradeStatus {

    private static final PackageInstallLogger LOG = PackageInstallLogger.create(UpgradeStatus.class);

    private static final String PN_UPGRADE_TIME = "time";
    private static final String PN_VERSION = "version";
    private static final String PN_ACTION = "actions";

    private final Resource resource;
    private final ComparableVersion version;

    public UpgradeStatus(InstallContext ctx, ResourceResolver resourceResolver, String path)
	    throws RepositoryException {
	LOG.debug(ctx, "loading status [{}]", path);
	resource = getOrCreateResource(ctx.getSession(), resourceResolver, path);
	version = createVersion(resource);
	LOG.info(ctx, "loaded status [{}]", this);
    }

    private static Resource getOrCreateResource(Session session, ResourceResolver resourceResolver, String path)
	    throws RepositoryException {
	JcrUtils.getOrCreateByPath(path, JcrResourceConstants.NT_SLING_FOLDER, session);
	return resourceResolver.getResource(path);
    }

    private static ComparableVersion createVersion(Resource resource) {
	String version = resource.getValueMap().get(PN_VERSION, String.class);
	if (version != null) {
	    return new ComparableVersion(version);
	} else {
	    return null;
	}
    }

    public final boolean isInitial() {
	return version == null;
    }

    public boolean notExecuted(InstallContext ctx, UpgradePackage pckg, UpgradeAction action)
	    throws RepositoryException {
	Resource packageStatus = getPackageStatusResource(ctx, pckg);
	if (packageStatus != null) {
	    for (String executedAction : packageStatus.getValueMap().get(PN_ACTION, new String[0])) {
		if (StringUtils.equals(executedAction, action.getName())) {
		    return false;
		}
	    }
	}
	return true;
    }

    private Resource getPackageStatusResource(InstallContext ctx, UpgradePackage pckg) throws RepositoryException {
	String packagePath = resource.getPath() + "/" + pckg.getName();
	JcrUtils.getOrCreateByPath(packagePath, JcrResourceConstants.NT_SLING_FOLDER, ctx.getSession());
	return resource.getResourceResolver().getResource(packagePath);
    }

    /**
     * Store the upgrade version and timestamp into the repository.
     * 
     * @param ctx
     *            The install context.
     * @throws RepositoryException
     */
    public void update(InstallContext ctx) throws RepositoryException {
	ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
	properties.put(PN_UPGRADE_TIME, Calendar.getInstance());
	String versionString = ctx.getPackage().getId().getVersionString();
	properties.put(PN_VERSION, versionString);
	LOG.info(ctx, "stored new status [{}]: [{}]", resource, versionString);
    }

    public void updateActions(InstallContext ctx, UpgradePackage pckg) throws RepositoryException {
	ModifiableValueMap properties = getPackageStatusResource(ctx, pckg).adaptTo(ModifiableValueMap.class);
	properties.put(PN_ACTION, pckg.getExecutedActions().toArray(new String[pckg.getExecutedActions().size()]));
    }

    public ComparableVersion getLastExecution() {
	checkStatus();
	return version;
    }

    /**
     * Throws an {@link IllegalStateException} if
     * {@link #isInitial()}=={@code true}.
     */
    protected void checkStatus() {
	if (isInitial()) {
	    throw new IllegalStateException("Cannot check values of an initial status.");
	}
    }

    @Override
    public String toString() {
	return new ToStringBuilder(this).append("resource", resource).append("version", version).toString();
    }

}
