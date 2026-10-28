INSERT INTO establishment (id, name)
VALUES (1, 'Camping Les Pins'),
       (2, 'Camping La Plage');

INSERT INTO room (id, number, type, establishment_id)
VALUES (1, 'A01', 'MOBIL_HOME', 1),
       (2, 'A02', 'MOBIL_HOME', 1),
       (3, 'A03', 'MOBIL_HOME', 1),
       (4, 'A04', 'MOBIL_HOME', 1),
       (5, 'A05', 'CHALET', 1),
       (6, 'A06', 'CHALET', 1),
       (7, 'A07', 'CHALET', 1),
       (8, 'A08', 'TENT', 1),
       (9, 'A09', 'TENT', 1),
       (10, 'A10', 'TENT', 1),
       (11, 'B01', 'MOBIL_HOME', 2),
       (12, 'B02', 'MOBIL_HOME', 2),
       (13, 'B03', 'MOBIL_HOME', 2),
       (14, 'B04', 'MOBIL_HOME', 2),
       (15, 'B05', 'MOBIL_HOME', 2),
       (16, 'B06', 'CHALET', 2),
       (17, 'B07', 'CHALET', 2),
       (18, 'B08', 'CHALET', 2),
       (19, 'B09', 'TENT', 2),
       (20, 'B10', 'TENT', 2);

INSERT INTO staff (id, username, first_name, role, establishment_id)
VALUES (1, 'alice', 'Alice', 'STAFF', 1),
       (2, 'bob', 'Bob', 'STAFF', 1),
       (3, 'marc', 'Marc', 'MANAGER', 1),
       (4, 'claire', 'Claire', 'STAFF', 2),
       (5, 'nina', 'Nina', 'MANAGER', 2);

-- Explicit ids above: move the identity sequences past them
SELECT setval(pg_get_serial_sequence('establishment', 'id'), (SELECT max(id) FROM establishment));
SELECT setval(pg_get_serial_sequence('room', 'id'), (SELECT max(id) FROM room));
SELECT setval(pg_get_serial_sequence('staff', 'id'), (SELECT max(id) FROM staff));
