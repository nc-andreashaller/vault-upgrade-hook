/*
 * (C) Copyright 2016 Netcentric AG.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package biz.netcentric.vlt.upgrade.handler.slingpipes;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biz.netcentric.vlt.upgrade.handler.UpgradeAction;
import biz.netcentric.vlt.upgrade.handler.UpgradeHandler;
import biz.netcentric.vlt.upgrade.util.OsgiUtil;

public class SlingPipesHandler implements UpgradeHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SlingPipesHandler.class);

    OsgiUtil osgi = new OsgiUtil();
    private Plumber service;

    @Override
    public boolean isAvailable() {
	return getService() != null;
    }

    /**
     * Builds the Map of Phases with a List of ScriptPaths for each Phase.
     * @return Map of ScriptPaths per Phase
     */
    @Override
    public Iterable<UpgradeAction> loadActions(Resource resource) {
	Collection<UpgradeAction> pipes = new ArrayList<>();
        for (Resource child : resource.getChildren()) {
            // sling pipes
            if (StringUtils.startsWith(child.getResourceType(), "slingPipes/")) {
		pipes.add(new SlingPipeUpgrade(getService(), child));
            }
        }
	return pipes;
    }

    private Plumber getService() {
	if (service == null) {
	    try {
		service = osgi.getService(Plumber.class);
	    } catch (NoClassDefFoundError e) {
		LOG.warn("Could not load Plumber.", e);
	    }
	}
	return service;
    }

}
