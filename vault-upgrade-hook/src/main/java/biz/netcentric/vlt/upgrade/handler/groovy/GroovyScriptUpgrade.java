package biz.netcentric.vlt.upgrade.handler.groovy;

import java.util.HashMap;
import java.util.Map;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import com.citytechinc.aem.groovy.console.GroovyConsoleService;
import com.citytechinc.aem.groovy.console.response.RunScriptResponse;

import biz.netcentric.vlt.upgrade.handler.UpgradeAction;
import biz.netcentric.vlt.upgrade.util.PackageInstallLogger;

public class GroovyScriptUpgrade extends UpgradeAction {

    private static final PackageInstallLogger LOG = PackageInstallLogger.create(GroovyScriptUpgrade.class);

    private final Resource script;
    private final GroovyConsoleService service;

    public GroovyScriptUpgrade(GroovyConsoleService service, Resource script) {
	super(script.getName());
	this.service = service;
	this.script = script;
    }

    /**
     * Executes the script from a given path via GroovyConsole.
     * 
     * @param scriptPath
     *            the path a package definition to execute
     */
    @Override
    public void execute(InstallContext ctx) {
	SlingHttpServletRequest request = getRequestForScript();
	if (request != null) {
	    LOG.debug(ctx, "Executing [{}]", script.getName());
	    RunScriptResponse scriptResponse = service.runScript(request);
	    LOG.info(ctx, "Executed [{}]: [{}]\n{}\n---\n", script.getName(), scriptResponse.getRunningTime(),
		    scriptResponse.getOutput().trim());
	}
    }

    private SlingHttpServletRequest getRequestForScript() {
	Resource contentResource = script.getChild(JcrConstants.JCR_CONTENT);
	if (contentResource != null) {
	    Map<String, Object> parameters = new HashMap<>();
	    parameters.put("script", contentResource.getValueMap().get(JcrConstants.JCR_DATA, String.class));
	    parameters.put("scriptPath", script.getPath());
	    return new FakeRequest(script.getResourceResolver(), "GET", "/bin/groovyconsole/post.json", parameters);
	}
	return null;
    }

}
