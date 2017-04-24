package biz.netcentric.vlt.upgrade.handler;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.jackrabbit.vault.packaging.InstallContext;

import biz.netcentric.vlt.upgrade.UpgradePackage;

public abstract class UpgradeAction implements Comparable<UpgradeAction> {

    private final String name;

    public UpgradeAction(String name) {
	this.name = name;
    }

    /**
     * @return the identifying name of this action. This name is unique within a
     *         {@link UpgradePackage}.
     */
    public String getName() {
	return name;
    }

    /**
     * Runs this action.
     * 
     * @param ctx
     *            the current context.
     */
    public abstract void execute(InstallContext ctx);

    @Override
    public int compareTo(UpgradeAction o) {
	return getName().compareTo(o.getName());
    }

    @Override
    public int hashCode() {
	return new HashCodeBuilder().append(getName()).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
	if(getClass().isInstance(obj)) {
	    return getName().equals(((UpgradeAction) obj).getName());
	} else {
	    return false;
    	}
    }

}
