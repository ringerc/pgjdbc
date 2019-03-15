# PostgreSQL backend protocol: wanted features

Current protocol is documented here: http://www.postgresql.org/docs/9.4/static/protocol.html

It turns out it lacks certain features, thus it makes clients more complex, slower, etc.

Here's the list of features that often appear in discussions. It supplements the list kept
on the
[postgres TODO for the v4 protocol](https://wiki.postgresql.org/wiki/Todo#Wire_Protocol_Changes_.2F_v4_Protocol).


## Features

### Binary transfer vs exact data type

Current protocol supports text and binary transfer.
It turns out in text mode backend does not need to know the exact data type. In most cases it can
easily deduce the data type. Binary mode is typically faster, however when consuming binary,
backend assumes the data type is exact and it does not consider casts.

It would be nice to have an ability to pass a value in binary form (for efficiency) yet
make backend deduce proper data type for it.


Kevin Wooten: my biggest request is always to treat binary types as if the client “just knows” how
to handle them. There are numerous cases with the text format where the server will coerce columns
to the most correct type and it will not do this for binary requests; it just spits out a complaint
that you’ve got the wrong type.

That, and being able to switch to “prefer binary” mode in the protocol. So when I make an non-bound
request I can get the results back in binary. Currently you can only get them in text format.
This has a couple of implications. First, speed, you always have to bind before querying to get
binary results. Second is multiple SQL statements in a single request, which you
cannot do in bound requests.

### Non-trivial semantics of numerics in text mode

In text mode, numerics and money types are transferred with unknown decimal separator.
This makes it hard to decode the value as it is locale-dependent.

See: https://github.com/pgjdbc/pgjdbc/pull/439

### Lack of `prepared statement invalidated` messages from backend

Server-prepared statement might become invalid due to table structure change, column type change,
etc.
It results in "your prepared statement is no longer valid". This is no fun.

See: https://github.com/pgjdbc/pgjdbc/pull/451

## Brain dumps

### Álvaro Hernández

- Uniform headers (type byte). Some messages (startup, ssl, etc) don't have the
  type byte marker.  This is annoying.
- Byte type re-use. The type byte is re-used across senders and receivers. It
  is unambiguous, but it is still very weird.
- Cluster support. Real production environments are clusters (sets of
  PostgreSQL servers using any kind of replication). Protocol should be
  concerned with this, specially if some HA mechanisms are built into the
  protocol.
- Server metadata without authentication. Some messages should be able to be
  exchanged to get information about servers (like version, read/write state
  and probably others) without having to authenticate and involve in lengthy
  processes.
- No out-of-band messages (like query cancellation)
- Simplified design (current is too complex and with too many messages)
- Specified in a formal syntax (I mean not English, a formal language for
  specifying it) and with a TCK (Test Compatibility Kit)
- Allow some controlled form of protocol extensibility
- A back-pressure method for query results
- All the "usual suspects": partial query results, large objects and so on

### Vladimir Sitnikov

- compressed streams over network
- "query response status" messages being sent in plain text (`insert 1`,
  `select 10`, etc.).  Having some binary there would make things easier to
  parse.
- unable to use prepared statements for `set "application_name"=...`, etc

### Craig Ringer

- Formalized, consistent command tags
- Protocol version negotiation handshake for mutually lowest supported protocol
- protocol STARTTLS message instead of trying SSL connection and falling back
  with reconnect. Evaluate in terms of JSSE.
- Protocol level `SET LOCAL` that binds at statement level (and protocol-level
  `SET` in general)
- Modern authentication handshake - client lists accepted methods,
  server lists accepted methods, client picks one, client can retry auth without
  breaking session. e.g. try peer, fall back to md5. Evaluate in terms of Java
  socket and SSL APIs.
- Allow mid-session re-authentication
- Protocol level `SET SESSION AUTHORIZATION` and `RESET SESSION AUTHORIZATION`
  equivalents with a reset cookie. Client cannot reset authorization at SQL
  level. Important for poolers including JDBC.
- Send numeric version to clients in fixed header (make `server_version_num`
  `GUC_REPORT`)
- Ensure the client can determine the encoding of messages sent early in the
  handshake
- Add decoded type length/precision (i.e. typmod information) so driver doesn't
  have to special case each datatype and knows length limits
- Mark result columns as known-not-null when possible in Describe responses
- Preserve typmod information through more of the query path and report it in
  Describe so drivers can better predict max result sizes for more kinds of
  queries.
- Permit lazy fetches of large values, at least out-of-line TOASTED values
  (bytea, xml, etc). Send a (tid,attnum)
  handle in the initial resultset instead of the value. Let the client set delayed
  fetch per-resultset-column (on, off, byte size threshold). Let the driver fetch
  batches of them asynchronously while the statement snapshot is valid. When
  isolation level > `REPEATABLE_READ` that's always safe and in `READ_COMMITED`
  we need to be able to hold the statement snapshot open. Values fetched
  packetized via `COPY` stream or similar to allow client to interrupt sending
  the stream (need to allow client-cancel of COPY), client can also fetch a
  range or limit. Client can request `TOAST`ed value be sent.
- Permit client to request TOASTed values to be sent compressed, as-is, if it
  understands how to deTOAST values. Protocol must include an identifier for
  TOAST format so client doesn't misinterpret future formats like we saw with
  `bytea_format`.
- Send client the xid when it is allocated (for distributed, XA, etc)
- Report xlog lsn (WAL) position in commit message (for distributed, XA, etc)
- Separate transaction delineation from protocol error recovery
  (in v3 both are managed via the same Sync message)
- Solve buffering and deadlock issues per github
  https://github.com/pgjdbc/pgjdbc/issues/194 (may be solveable using only driver)
