# Vault Upgrade Hook

AEM/Jackrabbit content packages allow to place Jars to a package in `META-INF/vault/hooks` that will be executed during installation. These hooks have to implement the interface `InstallHook`. 

InstallHooks are executed for each install phase: `PREPARE`, `INSTALLED` and `END`. If an error occurs `PREPARE_FAILED` or `INSTALL_FAILED`is called. `END` is called if installation was successful.

The *Vault-Upgrade-Hook* is an easy way to add additional logic to the installation of content packages instead of implementing and creating new Jars for every job. For example it can be used to upgrade existing user generated content.

The Hook runs so called `UpgradeAction`s embedded in `UpgradePackage`s. Out-of-the-box Groovy script and Sling Pipe actions are supported.

## Feature Overview

- control upgrade execution depending on the installed version
- control if actions are executed once or with every install of an package
- incremental upgrades which execute only new actions
- extendable API for custom action functionality
- convention over configuration, but still many options
- minimum dependencies

## Requirements
 
`AEM6.0 SP3` and above, if you are using Sling without AEM see the detailed dependencies in `vault-upgrade-hook/pom.xml`

## Usage

Two general ways of how to use it: 

1. ad-hoc admin style:
- take a copy of one of the prepared projects under `samples/...`, 
- update scripts under `src/main/upgrader` (e.g. test-groovy - replace all scripts with yours),
- build and install: `mvn -Pinstall`, target server and credentials can be set via `-Dcrx....`

2. dev-style:
- copy the JAR to your content package:
```<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <executions>
        <execution>
            <id>copy</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>copy</goal>
            </goals>
            <configuration>
                <artifactItems>
                    <artifactItem>
                        <groupId>biz.netcentric.vlt.upgrade</groupId>
                        <artifactId>vault-upgrade-hook</artifactId>
                        <version>${vault.upgrade.hook.version}</version>
                        <overWrite>true</overWrite>
                        <outputDirectory>${project.build.directory}/vault-work/META-INF/vault/hooks</outputDirectory>
                    </artifactItem>
                </artifactItems>
            </configuration>
        </execution>
    </executions>
</plugin>```
- create an upgrade package directory (like `samples/groovy-package/src/main/upgrader/test-groovy`) and place it in your package under `META-INF/vault/definition/upgrader`

## More information

### Upgrade Process

On installation of the content package `biz.netcentric.vlt.upgrade.UpgradeProcessor.execute(InstallContext)` will be called for each of the phases ([https://jackrabbit.apache.org/filevault/apidocs/org/apache/jackrabbit/vault/packaging/InstallContext.Phase.html]). On `PREPARED` the processor will read the status of previous executions from `/var/upgrade` and loads the `biz.netcentric.vlt.upgrade.UpgradePackage` child nodes from the current content package under `<package-path>/jcr:content/vlt:definition/upgrader`. On `END` will save the new status and the list of all actions to `/var/upgrade` and save the session.

An `UpgradePackage` loads a `biz.netcentric.vlt.upgrade.handler.UpgradeActionFactory` implementation to create `biz.netcentric.vlt.upgrade.handler.UpgradeAction`s which are executed during the upgrade.

Whether an `UpgradePackage` and an `UpgradeAction` is executed depends on some attributes:

- a package is relevant if
```
package was not executed before
  AND
current package version is higher or equals to the last execution
```
- an action is executed if `it was not executed before`

This behaviour can be changed by configuration options 
- `runMode="always"` - package and actions will always be executed disregarding of previous upgrades
- `skipOnInitial="false"` - package will also be executed if it is the first upgrade

`UpgradeAction`s are bound to a specific execution phase. The default Phase is `INSTALLED`. Means an arbitrary action is executed after the content got installed. This can be overridden by starting the groovy script name with the name of another phase e.g. "prepare_failed-myscript.groovy".

### Groovy

Groovy scripts are executed using the Groovy Console, see [https://github.com/Citytechinc/cq-groovy-console] for a complete list of features.

### Sling Pipes

For details about Sling Pipes please have a look at [https://sling.apache.org/documentation/bundles/sling-pipes.html]

### Configuration

For a full list of configuration options and their descriptions please see the JavaDocs of biz.netcentric.vlt.upgrade.UpgradePackage.
