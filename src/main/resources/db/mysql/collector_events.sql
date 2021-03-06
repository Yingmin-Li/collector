
create table feed_events
(
    id varchar(36) PRIMARY KEY NOT NULL,
    created_at bigint,
    channel varchar(128),
    metadata mediumtext,
    event mediumtext,
    subscription_id integer
) ;

create table subscriptions
(
  id INTEGER PRIMARY KEY AUTO_INCREMENT,
  topic varchar(128),
  metadata varchar(512),
  channel varchar(128)
);

CREATE TABLE feeds (
  feed_key VARCHAR(50) NOT NULL,
  feed MEDIUMBLOB NOT NULL,
  PRIMARY KEY (feed_key)
);

create index subscriptions_topic_idx on subscriptions (topic);
create index feed_events_channel_idx on feed_events (channel);
create index subscriptions_metadata_idx on subscriptions (metadata(128));
create index feed_events_created_idx on feed_events (created_at);