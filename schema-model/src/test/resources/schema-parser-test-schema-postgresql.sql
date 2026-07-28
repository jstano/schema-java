create or replace function generate_uuid() returns uuid language plpgsql parallel safe as $$
declare
   -- The current UNIX timestamp in milliseconds
   unix_time_ms CONSTANT bytea NOT NULL DEFAULT substring(int8send((extract(epoch FROM clock_timestamp()) * 1000)::bigint) from 3);

   -- The buffer used to create the UUID, starting with the UNIX timestamp and followed by random bytes
   buffer bytea not null default unix_time_ms || gen_random_bytes(10);
begin
   -- Set most significant 4 bits of 7th byte to 7 (for UUID v7), keeping the last 4 bits unchanged
   buffer = set_byte(buffer, 6, (b'0111' || get_byte(buffer, 6)::bit(4))::bit(8)::int);

   -- Set most significant 2 bits of 9th byte to 2 (the UUID variant specified in RFC 4122), keeping the last 6 bits unchanged
   buffer = set_byte(buffer, 8, (b'10' || get_byte(buffer, 8)::bit(6))::bit(8)::int);

   return encode(buffer, 'hex');
end
$$;

do $createextensions$
begin
   if (select usesuper from pg_user where usename = CURRENT_USER) then
      create extension if not exists "citext";
      create extension if not exists "btree_gist";
   else
      raise notice 'User % is not a superuser, could not create citext or btree_gist extensions.', current_user;
   end if;
end;
$createextensions$;

drop type if exists show_in_module_type cascade;
create type show_in_module_type as enum ('A','B','L');

drop type if exists gender_type cascade;
create type gender_type as enum ('M','F');

drop type if exists test_enum_type cascade;
create type test_enum_type as enum ('1','2');

other top sql for pgsql 1;

other top sql for pgsql 2;

/* public.ChildTable */
drop table if exists public.ChildTable cascade;

create table public.ChildTable
(
   ID serial not null,
   ParentID integer not null,
   Name text not null,
   constraint pk_childtable primary key (ID),
   constraint ak_childtable1 unique (ParentID,Name)
);

/* public.ColumnTesterTable */
drop table if exists public.ColumnTesterTable cascade;

create table public.ColumnTesterTable
(
   sequence serial not null,
   longsequence bigserial,
   byte smallint,
   short smallint,
   int integer,
   long bigint,
   float real,
   double double precision,
   decimal decimal(19,4),
   boolean boolean default false,
   date date,
   datetime timestamp,
   time time,
   timestamp timestamp,
   char char(1) default default 'A',
   varchar text,
   varcharWithCheck text,
   enum test_enum_type,
   text text,
   memo text,
   blob bytea,
   uuid uuid,
   json jsonb,
   constraint ck_columntes_int_B3963409 check(int >= 1 and int <= 500),
   constraint ck_columntes_varcharwi_353F3BCB check(length(varcharWithCheck) <= 6),
   constraint ck_columntes_text_BF352C53 check(varcharWithCheck = 'ABC123'),
   constraint ck_columntes_memo_BF31FC60 check(varcharWithCheck = 'ABC123'),
   constraint ck_columntes_blob_BF2D16C3 check(varcharWithCheck = 'ABC123'),
   constraint ck_columntes_uuid_BF35DAE1 check(varcharWithCheck = 'ABC123'),
   constraint ck_columntes_json_BF30D40E check(varcharWithCheck = 'ABC123')
);

/* public.KBI */
drop table if exists public.KBI cascade;

create table public.KBI
(
   ID serial not null,
   PropertyID integer not null,
   Name text not null,
   Code text not null,
   ShowInModule show_in_module_type not null,
   MasterKBICodeID integer,
   UnitID integer,
   constraint pk_kbi primary key (ID),
   constraint ak_kbi1 unique (PropertyID,Name),
   constraint ak_kbi2 unique (PropertyID,Code)
);

create index ix_kbi1 on public.KBI (MasterKBICodeID);

/* public.MasterKBICode */
drop table if exists public.MasterKBICode cascade;

create table public.MasterKBICode
(
   ID serial not null,
   Code text not null,
   Description text not null,
   ShowOnDashboard boolean not null default false,
   SortOrder integer,
   GroupingFreeForm text,
   constraint pk_masterkbicode primary key (ID),
   constraint ak_masterkbicode1 unique (Code)
);

/* public.ParentTable */
drop table if exists public.ParentTable cascade;

create table public.ParentTable
(
   ID serial not null,
   Name text not null,
   Extra text,
   Gender gender_type,
   constraint pk_parenttable primary key (ID),
   constraint ak_parenttable1 unique (Name,Extra)
);

create index ix_parenttable1 on public.ParentTable (Extra,Name);
create index ix_parenttable2 on public.ParentTable (ID,Name,Extra);

insert into ParentTable (Name,Extra,Gender) values ('AAA','Extra AAA','M');
insert into ParentTable (Name,Extra,Gender) values ('BBB','Extra BBB','F');
insert into ParentTable (Name,Extra,Gender) values ('PGSQL','Extra PGSQL','M');

/* public.Property */
drop table if exists public.Property cascade;

create table public.Property
(
   ID serial not null,
   Name text not null,
   ShortName text not null,
   Code text not null,
   AltCode text not null,
   NumberRooms smallint not null,
   RegionID integer,
   constraint pk_property primary key (ID),
   constraint ak_property1 unique (Name),
   constraint ak_property2 unique (Code),
   constraint ak_property3 unique (AltCode),
   constraint ck_property_numberroo_90DF89E5 check(NumberRooms >= 0 and NumberRooms <= 20000)
);

/* public.Region */
drop table if exists public.Region cascade;

create table public.Region
(
   ID serial not null,
   Name text not null,
   ShortName text not null,
   Code text not null,
   ExcludeFromCorpReports boolean not null default false,
   constraint pk_region primary key (ID),
   constraint ak_region1 unique (Name),
   constraint ak_region2 unique (Code)
);

/* test.Unit */
drop table if exists test.Unit cascade;

create table test.Unit
(
   ID serial not null,
   PropertyID integer not null,
   Name text not null,
   SingularName text not null,
   Symbol text not null,
   Comment text,
   constraint pk_unit primary key (ID),
   constraint ak_unit1 unique (PropertyID,Name),
   constraint ak_unit2 unique (PropertyID,SingularName)
);

/* public.parenttable_delete */
create or replace function public.parenttable_delete() returns trigger as $BODY$
begin
   update ParentTableAggregation set 
      1 = ParentTableAggregation.CountOfData - coalesce(old.CountOfData,0),
      Name = ParentTableAggregation.SumOfData - coalesce(old.SumOfData,0)
   where ParentTableAggregation.Source1 = old.Destination1 and ParentTableAggregation.Source2 = old.Destination2;
   update ParentTableAggregation2 set 
      1 = ParentTableAggregation2.CountOfData - coalesce(old.CountOfData,0),
      Name = ParentTableAggregation2.SumOfData - coalesce(old.SumOfData,0)
   where ParentTableAggregation2.Source1 = old.Destination1 and ParentTableAggregation2.Source2 = old.Destination2;
delete from pgsql
   return null;
end;
$BODY$ language plpgsql;

drop trigger if exists parenttable_delete on public.ParentTable cascade;
create trigger parenttable_delete after delete on public.ParentTable
   for each row execute procedure public.parenttable_delete();

/* public.parenttable_update */
create or replace function public.parenttable_update() returns trigger as $BODY$
begin
   if tg_op = 'UPDATE' then
      update ParentTableAggregation set
         1 = coalesce(ParentTableAggregation.CountOfData,0) - coalesce(old.CountOfData,0),
         Name = coalesce(ParentTableAggregation.SumOfData,0) - coalesce(old.SumOfData,0)
      where ParentTableAggregation.Source1 = new.Destination1 and ParentTableAggregation.Source2 = new.Destination2;
   end if;

   insert into ParentTableAggregation (Source1, Source2, 1, Name)
      values (new.Source1, new.Source2, coalesce(new.CountOfData,0), coalesce(new.SumOfData,0))
      on conflict (Destination1,Destination2) do update set
         1 = coalesce(ParentTableAggregation.CountOfData,0) + coalesce(new.CountOfData,0),
         Name = coalesce(ParentTableAggregation.SumOfData,0) + coalesce(new.SumOfData,0)
      where ParentTableAggregation.Source1 = new.Destination1 and ParentTableAggregation.Source2 = new.Destination2;

   if tg_op = 'UPDATE' then
      update ParentTableAggregation2 set
         1 = coalesce(ParentTableAggregation2.CountOfData,0) - coalesce(old.CountOfData,0),
         Name = coalesce(ParentTableAggregation2.SumOfData,0) - coalesce(old.SumOfData,0)
      where ParentTableAggregation2.Source1 = new.Destination1 and ParentTableAggregation2.Source2 = new.Destination2;
   end if;

   insert into ParentTableAggregation2 (Source1, Source2, 1, Name)
      values (new.Source1, new.Source2, coalesce(new.CountOfData,0), coalesce(new.SumOfData,0))
      on conflict (Destination1,Destination2) do update set
         1 = coalesce(ParentTableAggregation2.CountOfData,0) + coalesce(new.CountOfData,0),
         Name = coalesce(ParentTableAggregation2.SumOfData,0) + coalesce(new.SumOfData,0)
      where ParentTableAggregation2.Source1 = new.Destination1 and ParentTableAggregation2.Source2 = new.Destination2;

update pgsql
   return new;
end;
$BODY$ language plpgsql;

drop trigger if exists parenttable_update on public.ParentTable cascade;
create trigger parenttable_update after insert or update on public.ParentTable
   for each row execute procedure public.parenttable_update();

test custom function sql for pgsql 1;

custom function sql for pgsql 1;

custom function sql for pgsql 2;

/* test.TestView1 */
create or replace view test.TestView1 as
   select * from ParentTable;

/* public.TestView2 */
create or replace view public.TestView2 as
   select * from pgsql;

test custom procedure sql for pgsql 1;

custom procedure sql for pgsql 1;

custom procedure sql for pgsql 2;

custom procedure sql for mssql 2;

other bottom sql for pgsql 1;

other bottom sql for pgsql 2;

