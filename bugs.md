there are bugs in the schema generator
* postgresql
  * only output the create or replace function generate_uuid()
  * we have
    * other top sql for pgsql 1;
    * other top sql for pgsql 2;
  * char char(1) default default 'A' has double default default
  * we have this also
    * test custom function sql for pgsql 1;
    * custom function sql for pgsql 1;
    * custom function sql for pgsql 2;
  * we have this also
    * create or replace view public.TestView2 as
      * select * from pgsql;
      * test custom procedure sql for pgsql 1;
      * custom procedure sql for pgsql 1;
      * custom procedure sql for pgsql 2;
      * custom procedure sql for mssql 2;
      * other bottom sql for pgsql 1;
      * other bottom sql for pgsql 2;
