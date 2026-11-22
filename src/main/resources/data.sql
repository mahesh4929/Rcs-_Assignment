-- Demo data. Runs on every start; ON CONFLICT / NOT EXISTS make it safe to re-run.

-- Four handlers. CH003 is on leave, CH004 is inactive.
INSERT INTO claim_handler (handler_code, handler_name, lob, min_amount, max_amount, active)
VALUES
  ('CH001', 'Rohit Deshmukh', 'MOTOR',      1.00,  500000.00, TRUE),
  ('CH002', 'Priya Nair',     'MOTOR',      1.00,  500000.00, TRUE),
  ('CH003', 'Imran Shaikh',   'MOTOR',      1.00,  500000.00, TRUE),
  ('CH004', 'Anita Kulkarni', 'MOTOR', 500001.00, 2500000.00, FALSE)
ON CONFLICT (handler_code) DO NOTHING;

-- Locations: one handler can have many.
INSERT INTO handler_mapping (handler_id, mapping_type, mapping_value)
SELECT h.id, 'LOCATION', v.loc
  FROM claim_handler h
  JOIN (VALUES ('CH001', 'PUNE'), ('CH001', 'MUMBAI'),
               ('CH002', 'PUNE'), ('CH002', 'NASHIK'),
               ('CH003', 'PUNE'),
               ('CH004', 'PUNE'), ('CH004', 'MUMBAI')) AS v(code, loc)
    ON v.code = h.handler_code
ON CONFLICT DO NOTHING;

-- Policy numbers: CH004 only handles these two policies.
-- Handlers with no policy numbers mapped take any policy.
INSERT INTO handler_mapping (handler_id, mapping_type, mapping_value)
SELECT h.id, 'POLICY_NUMBER', v.pol
  FROM claim_handler h
  JOIN (VALUES ('CH004', 'POL-5001'), ('CH004', 'POL-5002')) AS v(code, pol)
    ON v.code = h.handler_code
ON CONFLICT DO NOTHING;

-- CH003 on leave
INSERT INTO handler_leave (handler_id, leave_from, leave_to, half_day)
SELECT id, TIMESTAMP '2026-11-15 00:00:00', TIMESTAMP '2026-12-31 23:59:59', FALSE
  FROM claim_handler h
 WHERE handler_code = 'CH003'
   AND NOT EXISTS (SELECT 1 FROM handler_leave l WHERE l.handler_id = h.id);

-- Two claims already in CH001's tray, so the workloads differ.
INSERT INTO claim (claim_number, claim_location, lob, policy_number, insured_name, loss_reserve_amount, status, handler_id)
SELECT 'CLM-100', 'PUNE', 'MOTOR', 'POL-9001', 'RAHUL MEHTA', 11000.00, 'OPEN', id FROM claim_handler WHERE handler_code = 'CH001'
ON CONFLICT (claim_number) DO NOTHING;

INSERT INTO claim (claim_number, claim_location, lob, policy_number, insured_name, loss_reserve_amount, status, handler_id)
SELECT 'CLM-101', 'PUNE', 'MOTOR', 'POL-9002', 'NEHA SHAH', 12000.00, 'OPEN', id FROM claim_handler WHERE handler_code = 'CH001'
ON CONFLICT (claim_number) DO NOTHING;

-- A new PUNE claim  -> CH002 (0 open claims vs CH001's 2).
-- A new MUMBAI claim -> CH001 (only active handler with MUMBAI).
