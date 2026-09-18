package com.sorting.center.service;

import com.sorting.center.dto.BizException;
import com.sorting.center.entity.*;
import com.sorting.center.repository.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TransitBagServiceIntegrationTest {

    @Autowired TransitBagService bagService;
    @Autowired LoadPlanService planService;
    @Autowired ChuteService chuteService;
    @Autowired TransitBagRepository bagRepo;
    @Autowired LoadPlanRepository planRepo;
    @Autowired SortBatchRepository batchRepo;
    @Autowired ChuteRepository chuteRepo;
    @Autowired ExceptionItemRepository exceptionRepo;

    private Long openChute;     // 启用格口
    private Long repairChute;   // 报修格口
    private Long doneBatch;     // 已完成批次（挂在启用格口）
    private Long runningBatch;  // 分拣中批次
    private Long waitingBatch;  // 待分拣批次
    private Long readyPlan;     // 待装车单（挂 doneBatch）
    private LocalDate today = LocalDate.of(2026, 9, 18);

    @BeforeEach
    void setup() {
        bagRepo.deleteAll();
        exceptionRepo.deleteAll();
        planRepo.deleteAll();
        batchRepo.deleteAll();
        chuteRepo.deleteAll();

        openChute = chute("C-T1", "华东", 500, "启用");
        repairChute = chute("C-T2", "华南", 400, "维修");
        Long disabledChute = chute("C-T3", "西北", 200, "停用");

        doneBatch = batch("SB-T1", openChute, 100, "已完成");
        runningBatch = batch("SB-T2", openChute, 100, "分拣中");
        waitingBatch = batch("SB-T3", openChute, 100, "待分拣");
        batch("SB-T4", repairChute, 100, "已完成");
        batch("SB-T5", disabledChute, 100, "已完成");

        readyPlan = plan("LP-T1", doneBatch, 100, "待装车");
        plan("LP-T9", doneBatch, 100, "已发车");
    }

    // ---------- 开袋前置：三边一次挂齐 ----------

    @Test
    void opens_bag_when_chute_enabled_batch_done_plan_waiting() {
        TransitBag bag = openBag("TB-001", readyPlan, 60);
        assertEquals("待发车", bag.status);
        assertEquals(openChute, bag.chuteId);
        assertEquals(doneBatch, bag.batchId);
        assertEquals(readyPlan, bag.loadPlanId);
    }

    @Test
    void rejects_open_without_plan() {
        TransitBag input = new TransitBag();
        input.code = "TB-002";
        input.quantity = 10;
        BizException ex = assertThrows(BizException.class, () -> bagService.open(input));
        assertTrue(ex.getMessage().contains("待装车的装车单"), ex.getMessage());
    }

    @Test
    void rejects_open_when_batch_still_sorting() {
        Long p = plan("LP-T2", runningBatch, 100, "待装车");
        BizException ex = assertThrows(BizException.class, () -> openBag("TB-003", p, 10));
        assertTrue(ex.getMessage().contains("分拣中"), ex.getMessage());
    }

    @Test
    void rejects_open_when_batch_waiting() {
        Long p = plan("LP-T3", waitingBatch, 100, "待装车");
        BizException ex = assertThrows(BizException.class, () -> openBag("TB-004", p, 10));
        assertTrue(ex.getMessage().contains("待分拣"), ex.getMessage());
    }

    @Test
    void rejects_open_when_chute_under_repair() {
        Long b = batchRepo.findAll().stream()
                .filter(x -> "SB-T4".equals(x.code)).findFirst().orElseThrow().id;
        Long p = plan("LP-T4", b, 100, "待装车");
        BizException ex = assertThrows(BizException.class, () -> openBag("TB-005", p, 10));
        assertTrue(ex.getMessage().contains("维修"), ex.getMessage());
    }

    @Test
    void rejects_open_when_chute_disabled() {
        Long b = batchRepo.findAll().stream()
                .filter(x -> "SB-T5".equals(x.code)).findFirst().orElseThrow().id;
        Long p = plan("LP-T5", b, 100, "待装车");
        BizException ex = assertThrows(BizException.class, () -> openBag("TB-006", p, 10));
        assertTrue(ex.getMessage().contains("停用"), ex.getMessage());
    }

    @Test
    void rejects_open_onto_departed_plan() {
        Long departed = planRepo.findAll().stream()
                .filter(x -> "LP-T9".equals(x.code)).findFirst().orElseThrow().id;
        BizException ex = assertThrows(BizException.class, () -> openBag("TB-007", departed, 10));
        assertTrue(ex.getMessage().contains("已经"), ex.getMessage());
        assertTrue(ex.getMessage().contains("新袋挂不上去"), ex.getMessage());
    }

    // ---------- 余量与异常件扣减 ----------

    @Test
    void capacity_excludes_pending_exception_items() {
        // 批次登记 100，2 件待处理异常，袋最多只能装 98
        exception("EX-001", doneBatch, "破损", "待处理");
        exception("EX-002", doneBatch, "无面单", "待处理");

        BizException over = assertThrows(BizException.class,
                () -> openBag("TB-010", readyPlan, 99));
        assertTrue(over.getMessage().contains("只剩 98 件"), over.getMessage());
        assertTrue(over.getMessage().contains("待处理异常件扣掉 2 件"), over.getMessage());

        TransitBag ok = openBag("TB-011", readyPlan, 98);
        assertEquals(98, ok.quantity);
    }

    @Test
    void capacity_excludes_other_live_bags_but_not_torn_ones() {
        openBag("TB-020", readyPlan, 40);
        // 拆一袋归还 30 件
        Long torn = openBag("TB-021", readyPlan, 30).id;
        bagService.tearDown(torn);

        // 已占 40，另开 60 正好，开 61 超
        openBag("TB-022", readyPlan, 60);
        BizException over = assertThrows(BizException.class,
                () -> openBag("TB-023", readyPlan, 1));
        assertTrue(over.getMessage().contains("只剩 0 件"), over.getMessage());
    }

    @Test
    void resolved_exception_no_longer_blocks_capacity() {
        Long exId = exception("EX-003", doneBatch, "错分", "待处理");
        BizException over = assertThrows(BizException.class,
                () -> openBag("TB-030", readyPlan, 100));
        assertTrue(over.getMessage().contains("只剩 99 件"), over.getMessage());

        // 把异常处理掉，余量恢复成 100
        ExceptionItem item = exceptionRepo.findById(exId).orElseThrow();
        item.status = "已处理";
        exceptionRepo.save(item);

        TransitBag bag = openBag("TB-031", readyPlan, 100);
        assertEquals(100, bag.quantity);
    }

    // ---------- 发车冻结 ----------

    @Test
    void departure_freezes_bags_and_blocks_teardown_and_edit() {
        TransitBag bag = openBag("TB-040", readyPlan, 50);
        planService.depart(readyPlan);

        assertEquals("已发车", bagRepo.findById(bag.id).orElseThrow().status);

        BizException tear = assertThrows(BizException.class, () -> bagService.tearDown(bag.id));
        assertTrue(tear.getMessage().contains("冻结") || tear.getMessage().contains("不能拆"),
                tear.getMessage());

        BizException edit = assertThrows(BizException.class,
                () -> bagService.updateQuantity(bag.id, 49));
        assertTrue(edit.getMessage().contains("冻住了不能改"), edit.getMessage());

        BizException open = assertThrows(BizException.class,
                () -> openBag("TB-041", readyPlan, 1));
        assertTrue(open.getMessage().contains("新袋挂不上去"), open.getMessage());
    }

    @Test
    void teardown_allowed_before_departure_and_returns_capacity() {
        TransitBag bag = openBag("TB-050", readyPlan, 100);
        TransitBag torn = bagService.tearDown(bag.id);
        assertEquals("已拆除", torn.status);
        // 余量回来，还能再开一袋 100
        assertEquals(100, openBag("TB-051", readyPlan, 100).quantity);
    }

    @Test
    void update_quantity_rechecks_capacity_before_departure() {
        TransitBag bag = openBag("TB-060", readyPlan, 50);
        openBag("TB-061", readyPlan, 40); // 余量剩 10
        TransitBag changed = bagService.updateQuantity(bag.id, 60); // 60+40=100
        assertEquals(60, changed.quantity);
        BizException over = assertThrows(BizException.class,
                () -> bagService.updateQuantity(bag.id, 61));
        assertTrue(over.getMessage().contains("只剩 60 件") || over.getMessage().contains("装不进"),
                over.getMessage());
    }

    // ---------- 并发开袋抢余量：只允许一笔成功 ----------

    @Test
    void concurrent_open_only_one_succeeds_and_loser_sees_remaining() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        List<Future<?>> futures = new ArrayList<>();
        AtomicInteger success = new AtomicInteger();
        List<String> loserMessages = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    // 每笔都要装 80，而余量只有 100，最多一笔成功
                    openBag(String.format("TB-C%02d", idx), readyPlan, 80);
                    success.incrementAndGet();
                } catch (BizException e) {
                    loserMessages.add(e.getMessage());
                } catch (Exception e) {
                    loserMessages.add("OTHER:" + e.getClass().getSimpleName() + ":" + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        start.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "并发开袋没有在限定时间内全部返回");
        pool.shutdown();

        assertEquals(1, success.get(), "同一批次余量只够一笔，却有 " + success.get() + " 笔开袋成功");
        assertEquals(threads - 1, loserMessages.size());
        for (String msg : loserMessages) {
            assertTrue(msg.startsWith("批次") || msg.contains("袋号"),
                    "后到的一笔要说明余量与原因，实际：" + msg);
            assertTrue(msg.contains("只剩 20 件"),
                    "后到者必须看到此刻还剩 20 件，实际：" + msg);
            assertFalse(msg.startsWith("OTHER:"), "出现非业务异常（可能是死锁/约束报错）：" + msg);
        }

        int total = bagRepo.findByBatchIdAndStatusIn(doneBatch, List.of("待发车", "已发车"))
                .stream().mapToInt(b -> b.quantity).sum();
        assertEquals(80, total, "落库件数绝不能超过可打余量");
    }

    // ---------- helpers ----------

    private Long chute(String code, String area, int capacity, String status) {
        Chute c = new Chute();
        c.code = code;
        c.area = area;
        c.capacity = capacity;
        c.status = status;
        return chuteRepo.save(c).id;
    }

    private Long batch(String code, Long chuteId, int qty, String status) {
        SortBatch b = new SortBatch();
        b.code = code;
        b.chuteId = chuteId;
        b.quantity = qty;
        b.arriveDate = today;
        b.status = status;
        b.operator = "测试员";
        return batchRepo.save(b).id;
    }

    private Long plan(String code, Long batchId, int qty, String status) {
        LoadPlan p = new LoadPlan();
        p.code = code;
        p.batchId = batchId;
        p.plateNo = "沪T" + code.substring(3);
        p.destination = "测试中转场";
        p.quantity = qty;
        p.loadDate = today;
        p.status = status;
        p.operator = "测试员";
        return planRepo.save(p).id;
    }

    private Long exception(String code, Long batchId, String kind, String status) {
        ExceptionItem e = new ExceptionItem();
        e.code = code;
        e.batchId = batchId;
        e.kind = kind;
        e.description = "测试异常";
        e.foundDate = today;
        e.handler = "测试员";
        e.status = status;
        return exceptionRepo.save(e).id;
    }

    private TransitBag openBag(String code, Long planId, int qty) {
        TransitBag input = new TransitBag();
        input.code = code;
        input.loadPlanId = planId;
        input.quantity = qty;
        input.bagDate = today;
        input.operator = "测试员";
        return bagService.open(input);
    }
}
