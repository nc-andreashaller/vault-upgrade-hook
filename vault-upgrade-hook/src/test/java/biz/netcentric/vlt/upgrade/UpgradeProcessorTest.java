package biz.netcentric.vlt.upgrade;

import java.util.Arrays;

import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.InstallContext;
import org.apache.jackrabbit.vault.packaging.InstallContext.Phase;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import biz.netcentric.vlt.upgrade.handler.UpgradeAction;
import biz.netcentric.vlt.upgrade.handler.UpgradeHandler;
import biz.netcentric.vlt.upgrade.util.OsgiUtil;

@RunWith(MockitoJUnitRunner.class)
public class UpgradeProcessorTest {

    @Rule
    public final SlingContext sling = new SlingContext();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private InstallContext ctx;

    @Mock
    private OsgiUtil osgi;

    @Mock
    private VaultPackage vaultPackage;

    @Mock
    private UpgradePackage upgradePackage;

    @Mock
    private static UpgradeAction action;

    @Mock
    private static UpgradeStatus status;

    private UpgradeProcessor processor;

    @Before
    public void setup() {
	Mockito.reset(action);
	processor = new UpgradeProcessor();
	processor.osgi = osgi;
	Mockito.when(ctx.getOptions()).thenReturn(Mockito.mock(ImportOptions.class));
    }

    @Test
    public void testExecutePrepare() throws Exception {
	Mockito.when(ctx.getPhase()).thenReturn(Phase.PREPARE);
	Mockito.when(osgi.getService(ResourceResolverFactory.class))
		.thenReturn(sling.getService(ResourceResolverFactory.class));
	Mockito.when(ctx.getPackage().getId().getName()).thenReturn("testVaultPackage");
	Mockito.when(ctx.getPackage().getId().getGroup()).thenReturn("testVaultGroup");
	Mockito.when(ctx.getPackage().getId().getInstallationPath()).thenReturn("/test/installation/path");
	Mockito.when(ctx.getPackage().getId().getVersionString()).thenReturn("1.0.1-SNAPSHOT");

	sling.load().json("/biz/netcentric/vlt/upgrade/testStatus.json",
		"/var/upgrade/testVaultGroup/testVaultPackage");
	sling.load().json("/biz/netcentric/vlt/upgrade/testUpgrade.json",
		"/test/installation/path.zip/jcr:content/vlt:definition/upgrader");

	Mockito.when(action.getName()).thenReturn("PREPARE-testAction");

	processor.execute(ctx);

	Assert.assertFalse(processor.failed);
	Assert.assertEquals("1.0.0", processor.status.getLastExecution().toString());
	Assert.assertEquals(1, processor.packages.size());
	Assert.assertEquals("1.0.1-SNAPSHOT", processor.packages.get(0).getTargetVersion().toString());
	Assert.assertTrue(processor.packages.get(0).isRelevant());
	Mockito.verify(action).execute(Mockito.any(InstallContext.class));
    }

    @Test
    public void testExecuteInstalled() throws Exception {
	processor.packages = Arrays.asList(upgradePackage);

	Mockito.when(ctx.getPhase()).thenReturn(Phase.INSTALLED);

	processor.execute(ctx);

	Mockito.verify(upgradePackage).execute(ctx);
    }

    @Test
    public void testExecuteEnd() throws Exception {
	processor.status = status;
	processor.packages = Arrays.asList(upgradePackage);

	Mockito.when(ctx.getPhase()).thenReturn(Phase.END);

	processor.execute(ctx);

	Mockito.verify(upgradePackage).execute(ctx);
	Mockito.verify(ctx.getSession()).save();
	Mockito.verify(status).update(ctx);
	Mockito.verify(status).updateActions(ctx, upgradePackage);
    }

    @Test
    public void testExecuteFailed() throws Exception {
	processor.failed = false;
	Mockito.when(ctx.getPhase()).thenReturn(Phase.PREPARE_FAILED);
	processor.execute(ctx);
	Assert.assertTrue(processor.failed);

	processor.failed = false;
	Mockito.when(ctx.getPhase()).thenReturn(Phase.INSTALL_FAILED);
	processor.execute(ctx);
	Assert.assertTrue(processor.failed);
    }

    @Test(expected = PackageException.class)
    public void testExecuteException() throws Exception {
	processor.packages = Arrays.asList(upgradePackage);

	Mockito.when(ctx.getPhase()).thenReturn(Phase.INSTALLED);
	Mockito.doThrow(new IllegalArgumentException("testException")).when(upgradePackage).execute(ctx);

	try {
	    processor.execute(ctx);
	} finally {
	    Assert.assertTrue(processor.failed);
	}
    }

    public static class TestHandler implements UpgradeHandler {

	@Override
	public boolean isAvailable() {
	    return true;
	}

	@Override
	public Iterable<UpgradeAction> loadActions(Resource configResource) {
	    return Arrays.asList(action);
	}

    }

}
