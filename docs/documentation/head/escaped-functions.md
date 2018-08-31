---
layout: default_docs
title: Escaped scalar functions
header: Chapter 8. JDBC escapes
resource: media
previoustitle: Date-time escapes
previous: escapes-datetime.html
nexttitle: Chapter 9. PostgreSQL™ Extensions to the JDBC API
next: ext.html
---

The JDBC specification defines functions with an escape call syntax : `{fn function_name(arguments)}`.
The following tables show which functions are supported by the PostgreSQL™ driver. 
The driver supports the nesting and the mixing of escaped functions and escaped
values. The appendix C of the JDBC specification describes the functions.

Some functions in the following tables are translated but not reported as supported
because they are duplicating or changing their order of the arguments. While this
is harmless for literal values or columns, it will cause problems when using
prepared statements. For example "`{fn right(?,?)}`" will be translated to "`substring(? from (length(?)+1-?))`".
As you can see the translated SQL requires more parameters than before the
translation but the driver will not automatically handle this.

<a name="escape-numeric-functions-table"></a>
**Table 8.1. Supported escaped numeric functions**

  --------------------------------------------------------------------------------
  **Function**          **Reported  **Translation**       **Comments**
                        as
                        supported**
  --------------------- ----------- --------------------- ------------------------
  abs(arg1)             yes         abs(arg1)

  acos(arg1)            yes         acos(arg1)

  asin(arg1)            yes         asin(arg1)

  atan(arg1)            yes         atan(arg1)

  atan2(arg1,arg2)      yes         atan2(arg1,arg2)

  ceiling(arg1)         yes         ceil(arg1)

  cos(arg1)             yes         cos(arg1)

  cot(arg1)             yes         cot(arg1)

  degrees(arg1)         yes         degrees(arg1)

  exp(arg1)             yes         exp(arg1)

  floor(arg1)           yes         floor(arg1)

  log(arg1)             yes         ln(arg1)

  log10(arg1)           yes         log(arg1)

  mod(arg1,arg2)        yes         mod(arg1,arg2)

  pi(arg1)              yes         pi(arg1)

  power(arg1,arg2)      yes         pow(arg1,arg2)

  radians(arg1)         yes         radians(arg1)

  rand()                yes         random()

  rand(arg1)            yes         setseed(arg1) * 0           The seed is initialized with
                                    + random()                  the given argument and a new
                                                                randow value is returned.

  round(arg1,arg2)      yes         round(arg1,arg2)

  sign(arg1)            yes         sign(arg1)

  sin(arg1)             yes         sin(arg1)

  sqrt(arg1)            yes         sqrt(arg1)

  tan(arg1)             yes         tan(arg1)

  truncate(arg1,arg2)   yes         trunc(arg1,arg2)
  --------------------------------------------------------------------------------

<a name="escape-string-functions-table"></a>
**Table 8.2. Supported escaped string functions**

  -------------------------------------------------------------------------------------------------------------
  **Function**                **Reported  **Translation**       **Comments**
                              as
                              supported**
  -------------               ----------- ----------------      --------------------
  ascii(arg1)                 yes         ascii(arg1)

  char(arg1)                  yes         chr(arg1)

  concat(arg1,arg2...)        yes         (arg1||arg2...)       The JDBC specification only require the
                                                                two arguments version, but supporting
                                                                more arguments was so easy...

  insert(arg1,arg2,arg3,arg4) no          overlay(arg1          This function is not reported as supported
                                          placing arg4          since it changes the order of the arguments
                                          from arg2 for arg3)   which can be a problem (for prepared
                                                                statements by example).

  lcase(arg1)                 yes         lower(arg1)

  left(arg1,arg2)             yes         substring(arg1
                                          for arg2)

  length(arg1)                yes         length(trim(
                                          trailing from arg1))

  locate(arg1,arg2)           no          position(arg1
                                          in arg2)

  locate(arg1,                no          (arg2*sign(           Not reported as supported
  arg2,arg3)                              position(arg1 in      since the three arguments version
                                          substring(arg2        duplicate and change the order
                                          from arg3) +          of the arguments.
                                          position(arg1 in
                                          substring(arg2
                                          from arg3))

  ltrim(arg1)                 yes         trim(leading
                                          from arg1)

  repeat(arg1,arg2)           yes         repeat(arg1,arg2)

  replace(arg1,arg2,arg3)     yes         replace(arg1,         Only reported as supported
                                          arg2,arg3)            by 7.3 and above servers.

  right(arg1,arg2)            no          substring(arg1 from(  Not reported as supported
                                          length(arg1)+1-arg2)) since arg2 is duplicated.

  rtrim(arg1)                 yes         trim(trailing
                                          from arg1)

  space(arg1)                 yes         repeat(' ',arg1)

  substring(arg1,arg2)        yes         substr(arg1,arg2)

  substring(arg1,arg2,arg3)   yes         substr(arg1,arg2,
                                          arg3)

  ucase(arg1)                 yes         upper(arg1)

  soundex(arg1)               no          soundex(arg1)         Not reported as supported
                                                                since it requires the
                                                                fuzzystrmatch contrib module.

  difference(arg1,arg2)       no          difference(arg1,arg2) Not reported as supported
                                                                since it requires the
                                                                fuzzystrmatch contrib module.
  -------------------------------------------------------------------------------------------------------------


<a name="escape-datetime-functions-table"></a>
**Table 8.3. Supported escaped date/time functions**

  -------------------------------------------------------------------------------------------------------------
  **Function**                **Reported  **Translation**       **Comments**
                              as
                              supported**
  -------------               ----------- ----------------      --------------------
  curdate()                   yes         current_date

  curtime()                   yes         current_time

  dayname(arg1)               yes         to_char(arg1,'Day')

  dayofmonth(arg1)            yes         extract(day
                                          from arg1)

  dayofweek(arg1)             yes         extract(dow           We must add 1 to be in
                                          from arg1)+1          the expected 1-7 range.

  dayofyear(arg1)             yes         extract(doy
                                          from arg1)

  hour(arg1)                  yes         extract(hour
                                          from arg1)

  minute(arg1)                yes         extract(minute
                                          from arg1)

  month(arg1)                 yes         extract(month
                                          from arg1)

  monthname(arg1)             yes         to_char(arg1,'Month')

  now()                       yes         now()

  quarter(arg1)               yes         extract(quarter
                                          from arg1)

  second(arg1)                yes         extract(second
                                          from arg1)

  week(arg1)                  yes         extract(week
                                          from arg1)

  year(arg1)                  yes         extract(year
                                          from arg1)

  timestampadd(               yes         ('(interval           an argIntervalType value of
  argIntervalType,                        according to          SQL_TSI_FRAC_SECOND is not
  argCount,argTimeStamp)                  argIntervalType       implemented since backend
                                          and argCount)'        does not support it
                                          +argTimeStamp)

  timestampdiff(              no          extract((interval     only an argIntervalType value of
  argIntervalType,                        according to          SQL_TSI_FRAC_SECOND,
  argTimeStamp1,                          argIntervalType)      SQL_TSI_FRAC_MINUTE,
  argTimeStamp2)                          from argTimeStamp2    SQL_TSI_FRAC_HOUR or
                                          - argTimeStamp1)      SQL_TSI_FRAC_DAY is supported
  -------------------------------------------------------------------------------------------------------------

<a name="escape-misc-functions-table"></a>
**Table 8.4. Supported escaped misc functions**


  -------------------------------------------------------------------------------------------------------------
  **Function**                **Reported  **Translation**       **Comments**
                              as
                              supported**
  -------------               ----------- ----------------      --------------------
  database()                  yes         current_database()    Only reported as supported
                                                                by 7.3 and above servers.

  ifnull(arg1,arg2)           yes         coalesce(arg1,arg2)

  user()                      yes         user
  -------------------------------------------------------------------------------------------------------------
