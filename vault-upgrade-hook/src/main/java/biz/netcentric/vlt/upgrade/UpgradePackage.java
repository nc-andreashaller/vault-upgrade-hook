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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.InstallContext.Phase;
import org.apache.sling.api.resource.Resource;

import biz.netcentric.vlt.upgrade.handler.HandlerFactory;
import biz.netcentric.vlt.upgrade.handler.UpgradeAction;
import biz.netcentric.vlt.upgrade.handler.UpgradeHandler;
import biz.netcentric.vlt.upgrade.util.ComparableVersion;
import biz.netcentric.vlt.upgrade.util.PackageInstallLogger;

public class UpgradePackage implements Comparable<UpgradePackage> {

    private static final PackageInstallLogger LOG = PackageInstallLogger.create(UpgradePackage.class);

    private static final String PN_VERSION = "version";
    private static final String PN_SAVE_THRESHOLD = "saveThreshold";
    private static final String PN_PRIORITY = "priority";
    private static final String PN_HANDLER = "handler";
    private static final String PN_DEFAULT_PHASE = "defaultPhase";
    private static final String PN_RUN_MODE = "run";
    private static final String PN_INCREMENTAL = "incremental";

    private final String name;
    private final UpgradeStatus status;
    private final long priority;
    private final long saveThreshold;
    private final ComparableVersion version;
    private final RunMode runMode;
    private final boolean incremental;
    private final UpgradeHandler handler;
    private final Map<Phase, List<UpgradeAction>> actions;
    private final List<String> executedActions = new ArrayList<>();
    private long counter = 0;

    /**
     * Create upgrade info.
     * 
     * @param ctx
     *            The install context.
     * @param status
     * @param configResource
     *            The config resource.
     * 
     * @throws RepositoryException
     */
    public UpgradePackage(InstallContext ctx, UpgradeStatus status, Resource configResource)
	    throws RepositoryException {
	name = configResource.getName();
	this.status = status;
	priority = configResource.getValueMap().get(PN_PRIORITY, Long.MAX_VALUE);
	saveThreshold = configResource.getValueMap().get(PN_SAVE_THRESHOLD, 1000l);
	version = new ComparableVersion(
		configResource.getValueMap().get(PN_VERSION, ctx.getPackage().getId().getVersionString()));
	runMode = RunMode.valueOf(configResource.getValueMap().get(PN_RUN_MODE, RunMode.ONCE.toString()).toUpperCase());
	incremental = configResource.getValueMap().get(PN_INCREMENTAL, true);
	handler = HandlerFactory.create(configResource.getValueMap().get(PN_HANDLER, HandlerFactory.GROOVY.toString()));
	actions = loadActions(configResource, handler);
	LOG.debug(ctx, "package [{}]", this);
    }

    private static Map<Phase, List<UpgradeAction>> loadActions(Resource configResource, UpgradeHandler handler) {
	Phase defaultPhase = Phase
		.valueOf(configResource.getValueMap().get(PN_DEFAULT_PHASE, Phase.INSTALLED.toString()).toUpperCase());
	Map<Phase, List<UpgradeAction>> actions = new HashMap<>();
	for (Phase phase : Phase.values()) {
	    actions.put(phase, new ArrayList<UpgradeAction>());
	}
	for (UpgradeAction action : handler.loadActions(configResource)) {
	    actions.get(getPhaseFromPrefix(defaultPhase, action.getName())).add(action);
	}
	for (Phase phase : Phase.values()) {
	    Collections.sort(actions.get(phase)); // make sure the scripts are
						  // correctly sorted
	}
	return actions;
    }

    /**
     * returns the correct Phase for a script name by its prefix. Important to
     * handle PREPARE_FAILED and PREPARE correctly
     * 
     * @param defaultPhase
     * @param name
     *            the script name
     * @return related phase. defaults to INSTALLED
     */
    private static Phase getPhaseFromPrefix(Phase defaultPhase, String name) {
	for (Phase phase : Phase.values()) {
	    if (StringUtils.startsWithIgnoreCase(name, phase.name())) {
		return phase;
	    }
	}
	return defaultPhase;
    }

    public void execute(InstallContext ctx) throws RepositoryException {
	LOG.debug(ctx, "executing [{}]: [{}]", this, actions.get(ctx.getPhase()));
	boolean reinstall = false;
	for (UpgradeAction action : actions.get(ctx.getPhase())) {
	    if (reinstall || (incremental && status.notExecuted(ctx, this, action))) {
		reinstall = true; // if the one action was regarded relevant all
				  // following actions are also executed no
				  // matter what their status is
		action.execute(ctx);
		executedActions.add(action.getName());
		saveOnThreshold(ctx, ++counter);
	    }
	}
    }

    /**
     * Save the JCR session, if the specified count of changes exceeds our
     * saving threshold.
     * 
     * @param ctx
     * @param count
     *            The count of changes.
     * @throws RepositoryException
     */
    private void saveOnThreshold(InstallContext ctx, long count) throws RepositoryException {
	if (saveThreshold > 0 && count % saveThreshold == 0) {
	    LOG.info(ctx, "saving [{}]", count);
	    ctx.getSession().save();
	}
    }

    /**
     * Check, if an upgrade info should be included, depending on its version
     * and on the specified runType.
     * 
     * <pre>
     * target | status ONCE | ALWAYS v | - -> skip | run 1-SNAPSHOT | 1-SNAPSHOT
     * -> run* | run 1 | 1-SNAPSHOT -> run* | run 2-SNAPSHOT | 1 -> run | run 2
     * | 1 -> run | run 1 | 2 -> skip | run *only new scripts have to be
     * executed
     * 
     * <pre>
     * 
     * @param status
     *            The current status of upgrades already run on this instance.
     * @param pckg
     *            The upgrade info to check.
     * @return true, if the upgrade info should be included; false otherwise.
     */
    public boolean isRelevant() {
	if (runMode == UpgradePackage.RunMode.ALWAYS) {
	    return true;
	}
	if (status.isInitial()) {
	    return false; // don't spool all upgrades on a new installation
	}
	return status.getLastExecution().compareTo(getTargetVersion()) <= 0;
    }

    public enum RunMode {
	/**
	 * Run only once, i.e. if source version < version <= target version
	 * (default).
	 */
	ONCE,

	/**
	 * run always, completely disregarding versions
	 */
	ALWAYS;
    }

    public ComparableVersion getTargetVersion() {
	return version;
    }

    @Override
    public int compareTo(UpgradePackage other) {
	// first sorting criterion: version
	int versionCompare = version.compareTo(other.version);
	if (versionCompare == 0) {
	    // second sorting criterion: priority
	    return (priority < other.priority) ? -1 : ((priority == other.priority) ? 0 : 1);
	} else {
	    return versionCompare;
	}
    }

    @Override
    public int hashCode() {
	return new HashCodeBuilder().append(priority).append(version).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (obj == this) {
	    return true;
	}
	if (obj.getClass() != getClass()) {
	    return false;
	}
	UpgradePackage rhs = (UpgradePackage) obj;
	return new EqualsBuilder().append(priority, rhs.priority).append(version, rhs.version)
		.appendSuper(super.equals(obj)).isEquals();
    }

    @Override
    public String toString() {
	return new ToStringBuilder(this).append("status", status).append("priority", priority)
		.append("saveThreshold", saveThreshold).append("version", version).append("runMode", runMode)
		.append("incremental", incremental).append("handler", handler).toString();
    }

    public String getName() {
	return name;
    }

    public List<String> getExecutedActions() {
	return executedActions;
    }
}
