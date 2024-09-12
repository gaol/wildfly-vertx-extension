# wildfly-vertx-extension

[![CI Tests](https://github.com/gaol/wildfly-vertx-extension/actions/workflows/ci.yml/badge.svg)](https://github.com/gaol/wildfly-vertx-extension/actions/workflows/ci.yml)

This is the Vertx extension for [WildFly Application Server](https://www.wildfly.org/).

WildFly subsystem and Galleon feature pack for integrating the [Eclipse Vertx.](https://vertx.io/) into a WildFly installation.

It allows you to define Vertx instance using WildFly management model, and they can be accessed using CDI injection in your enterprise or web applications.

## Project Structure
This project provides the following modules:

* **subsystem** -- A WildFly `Extension` implementation that provides an `vertx` subsystem for integrating Eclipse Vert.x into a WildFly deployment. This module's artifact can be used with the feature pack produced from this repository, or it can be incorporated in other feature packs (e.g. WildFly's `wildfly-preview` feature pack).
* **galleon-feature-pack/galleon-content** -- Provides source material for inclusion in a Galleon feature pack. This includes the definition of an `vertx` Galleon layer. This module's content can be used with the feature pack produced from this repository.
* **galleon-feature-pack/galleon-local** -- Provides source material for inclusion in a Galleon feature pack. This module's content is only meant to be used with the feature pack produced from this repository.
* **galleon-feature-pack/wildfly-feature-pack** -- Produces a feature pack that can be used to integrate Eclipse Vert.x into standard WildFly.
* **testsuite/shared/** -- Some shared test utils can be used in the test cases.
* **testsuite/integration/basic/** -- Some testcases to test manipulate Eclipse VertxOptions against a WildFly installation that includes the subsystem.


