use logs;

drop table if exists tag_types;
create table tag_types (
    type_id int primary key,
    name varchar(255)
);

drop table if exists tags;
create table tags (
    tag_id int primary key,
    type_id int,
    name varchar(255),
    foreign key (type_id) references tag_types(type_id)
);

drop table if exists queries;
create table queries (
#ifdef hsql
    query_id int primary key,
#endif
#ifdef mysql
    query_id int primary key auto_increment,
#endif
    query varchar(255),
    results int,
    ip varchar(15),
    time datetime
);

drop table if exists results;
create table results (
#ifdef hsql
    result_id int primary key,
#endif
#ifdef mysql
    result_id int primary key auto_increment,
#endif
    link varchar(512),
    clicked boolean,
    distance int,
    query_id int,
    foreign key (query_id) references queries(query_id)
);

drop table if exists tag_lists;
create table tag_lists (
    result_id int,
    tag_id int,
    foreign key (result_id) references results(result_id),
    foreign key (tag_id) references tags(tag_id)
);

create index tag_lists_index on tag_lists (result_id);


insert into tag_types values (1,'category');
insert into tag_types values (2,'scope');
insert into tag_types values (3,'subdomain');
insert into tags values (1,1,'cycling');
insert into tags values (2,1,'running');
insert into tags values (3,1,'swimming');
insert into tags values (4,2,'web');
insert into tags values (5,2,'user');
insert into tags values (6,2,'lockit');
insert into tags values (7,3,'slow.activeathletesearch.com');

