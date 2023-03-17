# wildfly-vertx-extension

[![CI Tests](https://github.com/gaol/wildfly-vertx-extension/actions/workflows/ci.yml/badge.svg)](https://github.com/gaol/wildfly-vertx-extension/actions/workflows/ci.yml)

[![Doc Site Generator](https://github.com/gaol/wildfly-vertx-extension/actions/workflows/docs-ci.yml/badge.svg)](https://github.com/gaol/wildfly-vertx-extension/actions/workflows/docs-ci.yml)

This is the Vertx extension for [WildFly Application Server](https://www.wildfly.org/).

[Eclipse Vert.x](https://vertx.io/) is a toolkit to build reactive applications on the JVM, integrating it adds more reactive power to WildFly.

It allows you to define Vertx instances using WildFly management model, and they can be accessed using JNDI lookup or CDI injection in your enterprise or web applications.

You can access Vertx core APIs and some component APIs in your applications.

You can package the verticle classes with the application, the extension will deploy the verticles to the associated Vertx instances managed by the WildFly server if there is a `META-INF/vertx.json` or `WEB-INF/vertx.json` file in the application archive.

In case of clustering vertx instance, this extension uses [vertx-infinispan](https://github.com/vert-x3/vertx-infinispan/) as the cluster manager to be able to talk with remote Vertx instances. You can specify the necessary JGroups settings for Vertx using standard WildFly configuration.

## Roadmap

Please see https://github.com/gaol/wildfly-vertx-extension/wiki/Roadmap for the Roadmap 

## To Build the extension

It requires:
* Java 11+
* Maven 3.2.5+

to build the extension.

Run the following command to build:

> mvn clean install

After that, there will be 2 servers and 1 Galleon feature pack produced in the separated subdirectories:

* `build/target/wildfly-vertx-build-${version}/`
* `dist/target/wildfly-vertx-dist-${version}/`
* `galleon-feature-pack/wildfly-feature-pack/target/wildfly-vertx-feature-pack-${version}.zip`

## Start the server

The generated servers can be started the same way as how WildFly server is started:
> $SERVER_HOME/bin/standalone.sh


## Installation to existing server

The produced Galleon feature pack can be installed to an existing WildFly server using Galleon CLI.

> NOTE: To have `galleon.sh` work, please check the [Galleon Provisioning Guide](https://docs.wildfly.org/21/Galleon_Guide.html#download-and-installation-of-the-galleon-command-line-tool) on how to download and install the Gallon CLI tool.

## Documentation

For more information about this extension, please visit the documentation: https://gaol.github.io/wildfly-vertx-extension/

