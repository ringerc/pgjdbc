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

  Added in `42.2.3-2ndq-r1_1`
