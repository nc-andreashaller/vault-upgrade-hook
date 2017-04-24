package biz.netcentric.vlt.upgrade.handler;

import org.apache.sling.api.resource.Resource;

public interface UpgradeHandler {

    boolean isAvailable();

    Iterable<UpgradeAction> loadActions(Resource configResource);

}
