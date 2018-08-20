#!/bin/bash

v="$(xpath -q -e "/project/version/text()" ../../pom.xml)"

cat <<EOF
---
classification: internal
title: The PostgreSQL JDBC Interface
author:
- PostgreSQL Development Group
- (patched by 2ndQuadrant)
version: ${v}
copyright-years: 2018
date: FIXME(date)
toc: yes
---

EOF
