use event_management_db;

show tables;

select * from event;
select * from rsvp;
select * from users;

SHOW TABLES LIKE 'QRTZ_%';
SELECT * FROM qrtz_calendars;