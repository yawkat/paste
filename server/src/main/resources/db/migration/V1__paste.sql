create table paste
(
    id   text primary key            not null,
    time timestamp without time zone null,
    data bytea                       not null,

    -- at large sizes, bytea breaks down. Most importantly, pg_dump may fail at ~500MB.
    check (octet_length(data) < 100 * 1024 * 1024)
)