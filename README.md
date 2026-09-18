# 快递分拣中心 · 格口 / 批次 / 中转袋与装车管理系统

分拣中心的日常台账：**格口台账**、**分拣批次**、**中转袋**、**装车发运**、**异常件**。

## 技术栈

Spring Boot 3.3（Java 17）+ MySQL 8.0 + Redis 7 + Vue 3 + Element Plus + Vite + nginx，全栈 `docker compose` 一键启动。

## 启动

```bash
./start.sh              # 等价于 docker compose up -d --build
```

| 入口 | 地址 |
| --- | --- |
| 前端页面 | http://127.0.0.1:8215/ |
| 后端接口 | http://127.0.0.1:8315/api/ |
| MySQL | 127.0.0.1:3515（库 `sorting_center`） |
| Redis | 127.0.0.1:6515 |

## 停止

```bash
docker compose down       # 保留数据卷
docker compose down -v    # 连数据卷一起删，下次启动重新灌种子数据
```

## 业务模块

### 1. 格口台账（`chute`）

格口编号 `C-xx` 全库唯一，每个格口对应一个目的片区（华东 / 华中 / 同城…），
有一个「一次最多堆多少件」的容量。状态 `启用 / 停用 / 维修`。
列表同时显示这个格口还有几个批次没分完。
**还有未完成批次的格口不许停用或报修**。

- 页面：格口台账（`/chutes`）
- 接口：`GET /api/chutes`、`POST /api/chutes`、`PUT /api/chutes/{id}`

### 2. 分拣批次（`sort_batch`）

批次号 `SB-xxxx` 全库唯一，一个批次 = 一批到港包裹分到某个格口。
状态机 `待分拣 → 分拣中 → 已完成`。
写入时校验：**批次件数不能超过格口容量**；**停用或维修中的格口不能分拣**。

- 页面：分拣批次（`/batches`）
- 接口：`GET /api/batches`、`POST /api/batches`、`POST /api/batches/{id}/advance?action=`

### 3. 中转袋（`transit_bag`）

分完的包裹打成中转袋再装车。袋号 `TB-xxxx` 全库唯一，**开袋时格口、已完成的分拣批次、待装车的装车单三边一次挂齐**；
状态 `在袋 → 已拆袋`（拆袋是状态流转，台账保留，不物理删除）。

- 必须先有待装车单才允许开袋（月台上不许有无家可归的袋），无单直接拒绝；
- 批次还在 `待分拣 / 分拣中` 的货抽不进袋；格口 `停用 / 维修` 不能再开新袋；
- 装车单 `已发车` 后：新袋挂不上去、已挂的袋不能拆（件数也没有直接修改的口子，要改只能发车前拆了重打）；
- 余量公式：**袋内件数 + 该批次其它未拆袋件数 + 该批次待处理异常件件数 ≤ 批次登记总件数**
  ——待处理的破损 / 错分 / 无面单不许混进袋带走；
- 两个人同时对同一批次开袋时，事务对「格口行 → 批次行 → 装车单行」依次加行锁串行化，
  后到那笔看到的是最新余量，余量不够时返回：登记多少、已打袋多少、待处理异常多少、此刻还剩几件、为什么没开成；
- 发车同样锁装车单行，与开袋 / 拆袋互斥，保证冻结窗口里没有漏网的袋。

- 页面：中转袋（`/bags`）
- 接口：`GET /api/bags`、`POST /api/bags`、`POST /api/bags/{id}/unpack`、`GET /api/bags/remaining?batchId=`

### 4. 装车发运（`load_plan`）

装车单号 `LP-xxxx` 全库唯一，记车牌、目的地、装车件数与装车日期，状态 `待装车 → 已发车`。
写入时校验：**只有分拣完成的批次才能装车**；**装车件数不能超过批次总件数**。
发车对装车单行加锁，与中转袋的开袋 / 拆袋互斥，发完车的单上所有袋立即冻结。

- 页面：装车发运（`/plans`）
- 接口：`GET /api/plans`、`POST /api/plans`、`POST /api/plans/{id}/depart`

### 5. 异常件（`exception_item`）

破损、错分、无面单的包裹单独登记跟进：单号 `EX-xxxx` 全库唯一，记类型、问题描述与发现日期。
**问题描述必填**（报了异常就得说清是什么）。状态 `待处理 → 已处理`，处理过一次就不能再处理。
**待处理异常件直接占用所属批次的打袋余量**，处理完才释放。

- 页面：异常件（`/exceptions`）
- 接口：`GET /api/exceptions`、`POST /api/exceptions`、`POST /api/exceptions/{id}/resolve`

## 目录

```
backend/src/main/java/com/sorting/center/
├── config/       CORS 配置
├── controller/   REST 入口
├── dto/          BizException + 统一错误响应
├── entity/       5 张业务表（含 transit_bag 中转袋）
├── repository/   Spring Data JPA（开袋/发车路径带行锁查询）
└── service/      业务规则（容量校验、状态机、前置状态、异常必填、开袋三边挂齐与余量冻结）
backend/src/main/resources/schema.sql   建表 + 种子数据（挂进 MySQL initdb）
frontend/src/views/                     5 个业务页面
```
