CREATE TABLE IF NOT EXISTS tracks (
    id BIGSERIAL PRIMARY KEY,
    event_time TIMESTAMP NOT NULL,
    time_sec BIGINT NOT NULL,
    time_nanosec BIGINT NOT NULL,

    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,

    vx REAL NOT NULL,
    vy REAL NOT NULL,
    vz REAL NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tracks_event UNIQUE (
      time_sec, time_nanosec, x, y, z, vx, vy, vz
    )
);

CREATE INDEX IF NOT EXISTS idx_tracks_event_time ON tracks(event_time);
CREATE INDEX IF NOT EXISTS idx_tracks_time_sec_nanosec ON tracks(time_sec, time_nanosec);



CREATE TABLE IF NOT EXISTS rich_hits (
    id BIGSERIAL PRIMARY KEY,
    event_time TIMESTAMP NOT NULL,
    time_sec BIGINT NOT NULL,
    time_nanosec BIGINT NOT NULL,

    ix INT NOT NULL,
    iy INT NOT NULL,
    is_signal BOOLEAN NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_rich_hits_event UNIQUE (
     time_sec, time_nanosec, ix, iy
    )
);

CREATE INDEX IF NOT EXISTS idx_rich_hits_event_time ON rich_hits(event_time);
CREATE INDEX IF NOT EXISTS idx_rich_hits_signal ON rich_hits(is_signal);
CREATE INDEX IF NOT EXISTS idx_rich_hits_ix_iy ON rich_hits(ix, iy);



CREATE TABLE IF NOT EXISTS track_stats (
    id BIGSERIAL PRIMARY KEY,
    calculated_at TIMESTAMP NOT NULL,
    track_count INT NOT NULL,
    avg_hits_per_track DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_track_stats_calculated_at ON track_stats(calculated_at);