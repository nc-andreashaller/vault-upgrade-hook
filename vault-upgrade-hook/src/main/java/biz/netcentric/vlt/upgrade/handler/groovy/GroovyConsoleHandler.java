/*
 * (C) Copyright 2016 Netcentric AG.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package biz.netcentric.vlt.upgrade.handler.groovy;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.citytechinc.aem.groovy.console.GroovyConsoleService;

import biz.netcentric.vlt.upgrade.handler.UpgradeAction;
import biz.netcentric.vlt.upgrade.handler.UpgradeHandler;
import biz.netcentric.vlt.upgrade.util.OsgiUtil;

public class GroovyConsoleHandler implements UpgradeHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GroovyConsoleHandler.class);

    OsgiUtil osgi = new OsgiUtil();
    private GroovyConsoleService service;

    @Override
    public boolean isAvailable() {
	return getService() != null;
    }

    /**
     * Builds the Map of Phases with a List of ScriptPaths for each Phase.
     * @return Map of ScriptPaths per Phase
     */
    @Override
    public List<UpgradeAction> loadActions(Resource configResource) {
	List<UpgradeAction> scripts = new ArrayList<>();

	for (Resource child : configResource.getChildren()) {
            if (StringUtils.endsWith(child.getName(), ".groovy") && child.isResourceType("nt:file")) {
                // it's a script, assign the script to a phase
		scripts.add(new GroovyScriptUpgrade(getService(), child));
            }
        }

        return scripts;
    }

    private GroovyConsoleService getService() {
	if (service == null) {
	    try {
		service = osgi.getService(GroovyConsoleService.class);
	    } catch (NoClassDefFoundError e) {
		LOG.warn("Could not load GroovyConsoleService.", e);
	    }
	}
	return service;
    }

}
