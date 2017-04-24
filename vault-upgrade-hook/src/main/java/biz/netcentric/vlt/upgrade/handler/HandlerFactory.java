package biz.netcentric.vlt.upgrade.handler;

import biz.netcentric.vlt.upgrade.handler.groovy.GroovyConsoleHandler;
import biz.netcentric.vlt.upgrade.handler.slingpipes.SlingPipesHandler;

public enum HandlerFactory {
    GROOVY(GroovyConsoleHandler.class), SLINGPIPES(SlingPipesHandler.class);

    private final Class<? extends UpgradeHandler> clazz;

    private HandlerFactory(Class<? extends UpgradeHandler> clazz) {
	this.clazz = clazz;
    }

    public static UpgradeHandler create(String key) {
	UpgradeHandler handler = null;
	for (HandlerFactory type : values()) {
	    if (type.name().equalsIgnoreCase(key)) {
		handler = create(type.clazz);
	    }
	}
	if (handler == null) {
	    try {
		handler = create(Class.forName(key));
	    } catch (ClassNotFoundException e) {
		throw new IllegalArgumentException("Cannot find custom handler: " + key, e);
	    }
	}
	if (!handler.isAvailable()) {
	    throw new IllegalArgumentException("Handler not available: " + handler);
	}
	return handler;
    }

    private static UpgradeHandler create(Class<?> clazz) {
	try {
	    return (UpgradeHandler) clazz.newInstance();
	} catch (InstantiationException | IllegalAccessException | ClassCastException e) {
	    throw new IllegalArgumentException("Cannot instantiate class: " + clazz, e);
	}
    }

}