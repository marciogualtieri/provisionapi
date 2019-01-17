# --- !Ups

create table "instance" (
  "id" bigserial primary key,
  "name" varchar not null,
  "plan" varchar not null,
  "state" varchar not null,
  "targetId" varchar,
  "created" timestamp not null,
  "updated" timestamp not null
);

create table "token"(
  "id" bigserial primary key,
  "value" varchar not null
);

insert into "token"("value") values ('bWFyY2lvZ3VhbHRpZXJpOmRkamtsbXJydmN2Y3VpbzQzNA==');

# --- !Downs

drop table if exists "instance";
drop table if exists "token";