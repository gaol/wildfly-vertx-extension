# wildfly-vertx-extension

This is the Vertx extension for [WildFly Application Server](https://www.wildfly.org/).

[Eclipse Vert.x](https://vertx.io/) is a toolkit to build reactive applications on the JVM, integrating it adds more reactive power to WildFly.

It allows you to define Vertx instances using WildFly configuration, and they can be accessed using JNDI lookup by your enterprise or web applications.

You can access Vertx core APIs and some component APIs in your applications.

You can package the verticle classes with the application, the extension will deploy the verticles to the associated Vertx instances managed by the WildFly server if there is a `META-INF/vertx-deployment.json` or `WEB-INF/vertx-deployment.json` file in the application archive.

In case of clustering vertx instance, this extension uses [vertx-infinispan](https://github.com/vert-x3/vertx-infinispan/) as the cluster manager to be able to talk with remote Vertx instances. You can specify the necessary JGroups settings for Vertx using standard WildFly configuration.

## Roadmap

Please see https://github.com/gaol/wildfly-vertx-extension/wiki/Roadmap for the Roadmap 

## Requirements to build

* Java 11+
* Maven 3.2.5+

## Build the extension

Run the following command to build the extension

> mvn clean install

After that, you will see the distribution at `dist/target/wildfly-vertx-dist-${version}/`, which is a provisioned WildFly server with this extension integrated.

It only has standalone mode produced, you can see 4 pre-defined configurations in the `standalone/configuration` directory:
* standalone-vertx-mini.xml
   * This is the minimal set up to have vertx subsystem and `jaxrs-server` layer added.
* standalone-vertx.xml
   * This is the set up with vertx subsystem added compared to standard standalone.xml in WildFly server
* standalone-vertx-ha.xml
   * This is the set up with vertx subsystem added compared to standard standalone-ha.xml in WildFly server
   * The default vertx in this set up is clustered vertx
* standalone-vertx-full-ha.xml
   * This is the set up with vertx subsystem added compared to standard standalone-full-ha.xml in WildFly server
   * The default vertx in this set up is clustered vertx

All standalone configurations have a pre-defined Vertx instance added: `default`, it can be accessed using JNDI name: `java:/vertx/default`.

## Start the server

Start the server with the following command, change to different configuration based on your needs:
> $SERVER_HOME/bin/standalone.sh -c standalone-vertx-mini.xml

## Configuration of the Vertx subsystem

* Add a Vertx instance
```
[standalone@localhost:9990 /] /subsystem=vertx/vertx=vertxA:add(clustered=false)
{"outcome" => "success"}
```

* Read the Vertx resource
```
[standalone@localhost:9990 /] /subsystem=vertx/vertx=vertxA:read-resource()
{
    "outcome" => "success",
    "result" => {
        "clustered" => false,
        "forked-channel" => false,
        "jgroups-channel" => undefined,
        "jndi-name" => "java:/vertx/vertxA",
        "vertx-options-file" => undefined
    }
}
```

* Update configuration of the Vertx instance
```
[standalone@localhost:9990 /] /subsystem=vertx/vertx=vertxA:write-attribute(name=jndi-name,value=java:/vertx/vertxB)
{
    "outcome" => "success",
    "response-headers" => {
        "operation-requires-reload" => true,
        "process-state" => "reload-required"
    }
}
```
Note: any update will require reload of the server to take effect.

* Remove the Vertx instance
```
[standalone@localhost:9990 /] /subsystem=vertx/vertx=vertxA:remove()
{
    "outcome" => "success",
    "response-headers" => {
        "operation-requires-reload" => true,
        "process-state" => "reload-required"
    }
}
```
Note: removal of the Vertx instance requires server reload.

## Installation to existing server

* This extension can be installed to an existing WildFly server which was provisioned by [Galleon](https://docs.wildfly.org/galleon/).

After you build the extension, you can install it to a server using:

```shell
galleon.sh install --dir=local_server "wildfly-ee@maven(org.jboss.universe:community-universe):current#26.0.1.Final"
galleon.sh install --dir=local_server org.wildfly.extension.vertx:wildfly-vertx-feature-pack:1.0.0-SNAPSHOT
```

after installation, the extra layer of `vertx` is installed and the required bits are downloaded.

* A single layer can be installed to an existing WildFly server which was provisioned by [Galleon](https://docs.wildfly.org/galleon/).

This extension produces 2 layer:
* vertx
* vertx-ha

You can specify one of them to install to an existing server, similar as above, run:

```shell
galleon.sh install --dir=local_server "wildfly-ee@maven(org.jboss.universe:community-universe):current#26.0.1.Final"
galleon.sh install --dir=local_server org.wildfly.extension.vertx:wildfly-vertx-feature-pack:1.0.0-SNAPSHOT --layers=vertx
```
this will update the default configuration `standalone.xml` instead of producing new configurations, and download necessary bits.

in case of clustered Vertx, you need layer of `vertx-ha`, you need to specify the ha configuration:

```shell
galleon.sh install --dir=local_server "wildfly-ee@maven(org.jboss.universe:community-universe):current#26.0.1.Final"
galleon.sh install --dir=local_server org.wildfly.extension.vertx:wildfly-vertx-feature-pack:1.0.0-SNAPSHOT --layers=vertx-ha --config=standalone-ha.xml
```

> NOTE: To have `galleon.sh` work, please check the [Galleon Provisioning Guide](https://docs.wildfly.org/21/Galleon_Guide.html#download-and-installation-of-the-galleon-command-line-tool) on how to download and install the Gallon CLI tool.  