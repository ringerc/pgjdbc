---
layout: default_docs
title: Autosave
header: Chapter 9. PostgreSQL™ Extensions to the JDBC API
resource: media
previoustitle: Arrays
previous: arrays.html
nexttitle: Chapter 10. Using the Driver in a Multithreaded or a Servlet Environment
next: thread.html
---

PgJDBC provides control over PostgreSQL's rollback behaviour.

By default PostgreSQL automatically marks a transaction as aborted if any
statement in that transaction fails. Subsequent statements fail with

    ERROR: current transaction is aborted, commands ignored until end of transaction block


until an explicit `ROLLBACK` or `COMMIT` is issued.

Many other RDBMSes instead implicitly roll back statements that fail, leaving
the transaction state as if the statement had never been executed. PgJDBC supports
emulating this behaviour on PostgreSQL with savepoints auto-managed by the driver.

## Autosave modes

Rollback behaviour is managed with the `autosave` JDBC parameter, which may have the
following values:

  * `never`: Any error aborts the whole transaction and subsequent queries fail
    with `ERROR: current transaction is aborted, commands ignored until end of
    transaction block` until an explicit rollback or commit. Commit is implicity
    transformed into rollback when the transaction is in the aborted state.

  * `always`: The JDBC driver sets a `SAVEPOINT` before each
    query, and rolls back to that savepoint in case of any error reported by
    the server while executing the statement.

    This mode disables batch round-trip optimisation, forcing every statement
    in a batch to be executed with a full client/server round-trip. It also
    adds an extra round-trip for savepoint management to normal queries.

  * `conservative`: A savepoint is set for each query, however
    automatic rollback is done only for rare cases like 'cached statement
    cannot change return type' or 'statement XXX is not valid'. These failures
    are detected and auto-retried.

    This mode has the same performance impact as `autosave=server` mode but
    preserves normal PostgreSQL transaction abort semantics except for some
    protection for errors in prepared statement optimisations. You don't
    want this unless your app keeps hitting errors like the above and you
    can't work around them other ways.

  * `server`: PgJDBC relies on the server side parameter
    `transaction_rollback_scope` to perform implicit server-side savepoint
    management. If any statement ERRORs, it is automatically rolled back by the
    server and the transaction state remains valid.

    The semantic effect is the same as `autosave=always`, but it performs
    nearly as well as `autosave=never`. Batch round trip optimisations remain
    enabled and no extra round trips are added for unbatched queries. If this
    parameter is not supported on server side, PgJDBC rejects the connection.

    At time of writing this option is only supported by 2ndQPostgres 11 and
    newer.
  
The default is `never`.

## Changing autosave mode at runtime

The autosave mode can also be set after the connection is established by
unwrapping the `PGConnection` object (see
["Extensions to the JDBC API"](ext.html#extensions) and calling the
`setAutosave(...)` method. `getAutosave` queries the current value.

Valid arguments/results are the `org.postgresql.jdbc.AutoSave` enum's values:
`AutoSave.NEVER`, `AutoSave.ALWAYS`, `AutoSave.CONSERVATIVE` or
`AutoSave.SERVER`.

Attempting to set the autosave mode during an open transaction will throw
PSQLException and leave the autosave mode unchanged.

If the new or old autosave mode is `SERVER`, the JDBC driver changes the mode
on the server-side immediately. To do this it must do a full client/server
round trip. This may block on network I/O or fail with any error a normal query
can fail with. Attempting to set the autosave mode to `SERVER` on a server that
doesn't support it will also throw PSQLException and leave the autosave mode
unchanged.
