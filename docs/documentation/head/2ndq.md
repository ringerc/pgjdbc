---
layout: default_docs
title: 2ndQuadrant changes
header: 2ndQuadrant changes
resource: media
---

Changes in this driver vs the community PostgreSQL driver of the same major version are:

* Support the new `server` mode for the `autosave` parameter, and corresponding
  new `AutoSave.SERVER` enum value. See ["autosave" in "Extensions to the JDBC API"](autosave.html)
  for details.

  Added in `42.2.3.1` (tag `REL2Q.42.2.4.1`)

* Switches to new Maven groupId and artifactId `com.2ndQuadrant:2ndQPostgres-jdbc` to satisfy
  OSGi and Maven versioning rules while allowing us to maintain a sub-minor version.
  (Note, not published to Maven Central)

* Adds runtime switching of autosave mode via PGConnection.setAutosave(AutoSave mode) which:
  * Is now documented
  * Has changed interface to throw SQLException, which is not considered much of concern for a previously undocumented API
  * Refuses to switch into or out of AutoSave.SERVER mode when a transaction is in progress
