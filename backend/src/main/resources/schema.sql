SET NAMES utf8mb4;

DROP TABLE IF EXISTS transit_bag;
DROP TABLE IF EXISTS exception_item;
DROP TABLE IF EXISTS load_plan;
DROP TABLE IF EXISTS sort_batch;
DROP TABLE IF EXISTS chute;

CREATE TABLE chute (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  area VARCHAR(32) NOT NULL,
  capacity INT NOT NULL,
  status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_chute_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sort_batch (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  chute_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  arrive_date DATE NOT NULL,
  status VARCHAR(16) NOT NULL,
  operator VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_batch_code (code),
  KEY idx_batch_chute (chute_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE load_plan (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  batch_id BIGINT NOT NULL,
  plate_no VARCHAR(16) NOT NULL,
  destination VARCHAR(32) NOT NULL,
  quantity INT NOT NULL,
  load_date DATE NOT NULL,
  status VARCHAR(16) NOT NULL,
  operator VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plan_code (code),
  KEY idx_plan_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exception_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  batch_id BIGINT NOT NULL,
  kind VARCHAR(16) NOT NULL,
  description VARCHAR(255) NULL,
  found_date DATE NOT NULL,
  handler VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_exception_code (code),
  KEY idx_exception_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE transit_bag (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  chute_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  bag_date DATE NOT NULL,
  operator VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bag_code (code),
  KEY idx_bag_batch (batch_id, status),
  KEY idx_bag_plan (plan_id),
  KEY idx_bag_chute (chute_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO chute (code, area, capacity, status) VALUES
('C-01', '华东', 500, '启用'),
('C-02', '华中', 400, '启用'),
('C-03', '同城', 300, '启用'),
('C-04', '华南', 400, '维修'),
('C-05', '西北', 200, '启用');

INSERT INTO sort_batch (code, chute_id, quantity, arrive_date, status, operator) VALUES
('SB-0901', 1, 320, '2026-09-16', '待分拣', '王师傅'),
('SB-0902', 2, 260, '2026-09-16', '分拣中', '李师傅'),
('SB-0903', 3, 180, '2026-09-15', '已完成', '王师傅'),
('SB-0904', 5, 150, '2026-09-15', '已完成', '赵师傅');

INSERT INTO load_plan (code, batch_id, plate_no, destination, quantity, load_date, status, operator) VALUES
('LP-0901', 3, '沪A12345', '武汉中转场', 180, '2026-09-15', '已发车', '赵师傅'),
('LP-0902', 4, '沪B67890', '西安中转场', 150, '2026-09-16', '待装车', '赵师傅');

INSERT INTO exception_item (code, batch_id, kind, description, found_date, handler, status) VALUES
('EX-0901', 2, '破损', '外箱压破，内件完好', '2026-09-16', '李师傅', '待处理'),
('EX-0902', 1, '错分', '面单地址与片区不符', '2026-09-16', '王师傅', '已处理'),
('EX-0903', 4, '无面单', '面单脱落，待核件后再打袋', '2026-09-17', '赵师傅', '待处理');

INSERT INTO transit_bag (code, chute_id, batch_id, plan_id, quantity, bag_date, operator, status) VALUES
('TB-0901', 5, 4, 2, 60, '2026-09-17', '赵师傅', '在袋'),
('TB-0902', 5, 4, 2, 40, '2026-09-17', '赵师傅', '在袋'),
('TB-0903', 3, 3, 1, 180, '2026-09-15', '赵师傅', '在袋');
