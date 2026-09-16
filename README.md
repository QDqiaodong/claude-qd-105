# 快递分拣中心 · 格口与分拣批次管理系统

分拣中心的日常台账：**格口台账**、**分拣批次**、**装车发运**、**异常件**。

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

### 3. 装车发运（`load_plan`）

装车单号 `LP-xxxx` 全库唯一，记车牌、目的地、装车件数与装车日期，状态 `待装车 → 已发车`。
写入时校验：**只有分拣完成的批次才能装车**；**装车件数不能超过批次总件数**。

- 页面：装车发运（`/plans`）
- 接口：`GET /api/plans`、`POST /api/plans`、`POST /api/plans/{id}/depart`

### 4. 异常件（`exception_item`）

破损、错分、无面单的包裹单独登记跟进：单号 `EX-xxxx` 全库唯一，记类型、问题描述与发现日期。
**问题描述必填**（报了异常就得说清是什么）。状态 `待处理 → 已处理`，处理过一次就不能再处理。

- 页面：异常件（`/exceptions`）
- 接口：`GET /api/exceptions`、`POST /api/exceptions`、`POST /api/exceptions/{id}/resolve`

## 目录

```
backend/src/main/java/com/sorting/center/
├── config/       CORS 配置
├── controller/   REST 入口
├── dto/          BizException + 统一错误响应
├── entity/       4 张业务表
├── repository/   Spring Data JPA
└── service/      业务规则（容量校验、状态机、前置状态、异常必填）
backend/src/main/resources/schema.sql   建表 + 种子数据（挂进 MySQL initdb）
frontend/src/views/                     4 个业务页面
```
