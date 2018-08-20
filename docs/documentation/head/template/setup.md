---
layout: default_docs
title: Chapter 2. Setting up the JDBC Driver
header: Chapter 2. Setting up the JDBC Driver
resource: media
previoustitle: Chapter 1. Introduction
previous: intro.html
nexttitle: Setting up the Class Path
next: classpath.html
---
		
**Table of Contents**

* [Getting the Driver](setup.html#build)
* [Setting up the Class Path](classpath.html)
* [Preparing the Database Server for JDBC](prepare.html)
* [Creating a Database](your-database.html)

This section describes the steps you need to take before you can write or run
programs that use the JDBC interface.

<a name="build"></a>
# Getting the Driver

The 2ndQPostgres JDBC driver is obtained from 2ndQuadrant by request.

It is not published to Maven Central.

The 2ndQPostgres driver uses different artifact co-ordinates and accordingly a
different file name convention to upstream PgJDBC, despite having only small
changes to the driver code. This is  necessitated by Maven and OSGi version
policies.

## Jar file

The jar file name follows the pattern:

    2ndQPostgres-jdbc-${project.version}.jar

not PgJDBC upstream's:

    postgresql-{version}.jar

You will need to account for this in your `build.xml` if using Ant.

## Apache Maven usage

Similarly, the Maven coordinates have changed from PgJDBC's

    org.postgresql:postgresql:{version}

to

    com.2ndQuadrant:2ndQPostgres-jdbc:${project.version}

so a suitable `pom.xml` dependency declaration would be:

    <dependency>
      <groupId>${project.groupId}</groupId>
      <artifactId>${project.artifactId}</artifactId>
      <version>${project.version}</version>
    </dependency>

As it is not in Central, you will need to *install the jar to your maven local
repository yourself*, or publish it to your corporate repository. For local
repository installation use `mvn install:install-file`, e.g.:

    mvn install:install-file -Dfile=2ndQPostgres-jdbc-${project.version}.jar

You do not need to specify the groupId, artifactId or version as they are read
by the plugin from the `pom.xml` embedded in the jar.
