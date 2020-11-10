drop table if exists users cascade;
drop table if exists walks cascade;

create table users (
    _id serial primary key,
    key_hash text not null
);

create table walks (
   _id serial primary key,
   user_id int not null references users(_id),
   walked_at timestamp not null default NOW()
);
