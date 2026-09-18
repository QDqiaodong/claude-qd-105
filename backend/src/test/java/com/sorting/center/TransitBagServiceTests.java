package com.sorting.center;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sorting.center.dto.BizException;
import com.sorting.center.entity.Chute;
import com.sorting.center.entity.ExceptionItem;
import com.sorting.center.entity.LoadPlan;
import com.sorting.center.entity.SortBatch;
import com.sorting.center.entity.TransitBag;
import com.sorting.center.repository.ChuteRepository;
import com.sorting.center.repository.ExceptionItemRepository;
import com.sorting.center.repository.LoadPlanRepository;
import com.sorting.center.repository.SortBatchRepository;
import com.sorting.center.repository.TransitBagRepository;
import com.sorting.center.service.TransitBagService;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TransitBagServiceTests {

    @Autowired
    TransitBagService bags;
    @Autowired
    ChuteRepository chuteRepo;
    @Autowired
    SortBatchRepository batchRepo;
    @Autowired
    LoadPlanRepository planRepo;
    @Autowired
    ExceptionItemRepository exceptionRepo;
    @Autowired
    TransitBagRepository bagRepo;
    @Autowired
    com.sorting.center.service.LoadPlanService loadPlans;

    Chute activeChute;
    SortBatch doneBatch;
    LoadPlan waitingPlan;

    @BeforeEach
    void seed() {
        bagRepo.deleteAll();
        exceptionRepo.deleteAll();
        planRepo.deleteAll();
        batchRepo.deleteAll();
        chuteRepo.deleteAll();

        activeChute = chuteRepo.save(chute("C-T1", "启用"));
        doneBatch = batchRepo.save(batch(activeChute, "SB-T1", 100, "已完成"));
        waitingPlan = planRepo.save(plan(doneBatch, "LP-T1", "待装车"));
    }

    private Chute chute(String code, String status) {
        Chute c = new Chute();
        c.code = code;
        c.area = "华东";
        c.capacity = 500;
        c.status = status;
        return c;
    }

    private SortBatch batch(Chute chute, String code, int qty, String status) {
        SortBatch b = new SortBatch();
        b.code = code;
        b.chuteId = chute.id;
        b.quantity = qty;
        b.arriveDate = LocalDate.now();
        b.status = status;
        b.operator = "测试";
        return b;
    }

    private LoadPlan plan(SortBatch batch, String code, String status) {
        LoadPlan p = new LoadPlan();
        p.code = code;
        p.batchId = batch.id;
        p.plateNo = "沪A00000";
        p.destination = "测试中转场";
        p.quantity = batch.quantity;
        p.loadDate = LocalDate.now();
        p.status = status;
        p.operator = "测试";
        return p;
    }

    private TransitBag input(String code, int qty, Chute chute, SortBatch batch, LoadPlan plan) {
        TransitBag b = new TransitBag();
        b.code = code;
        b.chuteId = chute.id;
        b.batchId = batch.id;
        b.planId = plan.id;
        b.quantity = qty;
        b.bagDate = LocalDate.now();
        b.operator = "测试";
        return b;
    }

    private long activeQuantity() {
        return bagRepo.sumActiveQuantityByBatch(doneBatch.id);
    }

    private void exception(String code, String status) {
        ExceptionItem e = new ExceptionItem();
        e.code = code;
        e.batchId = doneBatch.id;
        e.kind = "破损";
        e.description = "测试用异常";
        e.foundDate = LocalDate.now();
        e.handler = "测试";
        e.status = status;
        exceptionRepo.save(e);
    }

    @Test
    void openBagHappyPath() {
        TransitBag saved = bags.openBag(input("TB-H1", 30, activeChute, doneBatch, waitingPlan));
        assertNotNull(saved.id);
        assertEquals("在袋", saved.status);
        assertEquals(30, activeQuantity());
    }

    @Test
    void refuseWithoutPlan() {
        TransitBag b = input("TB-X1", 10, activeChute, doneBatch, waitingPlan);
        b.planId = null;
        BizException ex = assertThrows(BizException.class, () -> bags.openBag(b));
        assertTrue(ex.getMessage().contains("没有单不能开袋"), ex.getMessage());
    }

    @Test
    void refuseWhenBatchNotFinished() {
        SortBatch running = batchRepo.save(batch(activeChute, "SB-T2", 50, "分拣中"));
        LoadPlan plan = planRepo.save(plan(running, "LP-T2", "待装车"));
        BizException ex = assertThrows(BizException.class,
                () -> bags.openBag(input("TB-X2", 10, activeChute, running, plan)));
        assertTrue(ex.getMessage().contains("分拣完了才能抽件打袋"), ex.getMessage());
    }

    @Test
    void refuseWhenChuteDisabled() {
        for (String s : new String[]{"停用", "维修"}) {
            Chute c = chuteRepo.save(chute("C-T" + s, s));
            SortBatch b = batchRepo.save(batch(c, "SB-" + s, 50, "已完成"));
            LoadPlan p = planRepo.save(plan(b, "LP-" + s, "待装车"));
            BizException ex = assertThrows(BizException.class,
                    () -> bags.openBag(input("TB-" + s, 10, c, b, p)));
            assertTrue(ex.getMessage().contains("不能再开新袋"), ex.getMessage());
        }
    }

    @Test
    void refuseWhenPlanDeparted() {
        LoadPlan departed = planRepo.save(plan(doneBatch, "LP-T9", "已发车"));
        BizException ex = assertThrows(BizException.class,
                () -> bags.openBag(input("TB-X9", 10, activeChute, doneBatch, departed)));
        assertTrue(ex.getMessage().contains("已经发车，新袋挂不上去"), ex.getMessage());
    }

    @Test
    void pendingExceptionReducesRemaining() {
        exception("EX-P1", "待处理");
        // 100 件登记 - 1 件待处理异常 = 只剩 99，打 100 必须被拒
        BizException ex = assertThrows(BizException.class,
                () -> bags.openBag(input("TB-E1", 100, activeChute, doneBatch, waitingPlan)));
        assertTrue(ex.getMessage().contains("待处理异常件 1 件"), ex.getMessage());
        assertTrue(ex.getMessage().contains("此刻只剩 99 件"), ex.getMessage());
        assertEquals(99L, bags.remaining(doneBatch.id).get("remaining"));

        // 打 99 件可以
        bags.openBag(input("TB-E2", 99, activeChute, doneBatch, waitingPlan));
        assertEquals(99, activeQuantity());

        // 已处理异常不扣减余量
        exception("EX-P2", "已处理");
        assertEquals(0L, bags.remaining(doneBatch.id).get("remaining"));
    }

    @Test
    void overCapacityReportsWhy() {
        bags.openBag(input("TB-O1", 60, activeChute, doneBatch, waitingPlan));
        exception("EX-O1", "待处理");
        // 100 - 60(袋) - 1(异常) = 39
        BizException ex = assertThrows(BizException.class,
                () -> bags.openBag(input("TB-O2", 40, activeChute, doneBatch, waitingPlan)));
        assertTrue(ex.getMessage().contains("其他未拆的袋已占 60 件"), ex.getMessage());
        assertTrue(ex.getMessage().contains("此刻只剩 39 件"), ex.getMessage());
        assertEquals(60, activeQuantity());
    }

    @Test
    void unpackBeforeDepartReleasesCapacity() {
        TransitBag first = bags.openBag(input("TB-U1", 90, activeChute, doneBatch, waitingPlan));
        assertEquals(10L, bags.remaining(doneBatch.id).get("remaining"));

        bags.unpack(first.id);
        assertEquals("已拆袋", bagRepo.findById(first.id).orElseThrow().status);
        assertEquals(0, activeQuantity());
        // 拆掉后余量全部回来，可以重打
        bags.openBag(input("TB-U2", 100, activeChute, doneBatch, waitingPlan));
        assertEquals(100, activeQuantity());
    }

    @Test
    void frozenAfterDepartCannotUnpackOrRebag() {
        TransitBag first = bags.openBag(input("TB-F1", 60, activeChute, doneBatch, waitingPlan));
        planRepo.findById(waitingPlan.id).ifPresent(p -> {
            p.status = "已发车";
            planRepo.save(p);
        });

        BizException unpackEx = assertThrows(BizException.class, () -> bags.unpack(first.id));
        assertTrue(unpackEx.getMessage().contains("已发车，袋已冻结，不能拆"), unpackEx.getMessage());

        // 件数不能改：没有改件数的口子，只能拆了重打——而拆也被冻结
        assertEquals(60, bagRepo.findById(first.id).orElseThrow().quantity);

        LoadPlan sameBatchDeparted = planRepo.save(plan(doneBatch, "LP-TF", "已发车"));
        BizException newBagEx = assertThrows(BizException.class,
                () -> bags.openBag(input("TB-F2", 10, activeChute, doneBatch, sameBatchDeparted)));
        assertTrue(newBagEx.getMessage().contains("已经发车"), newBagEx.getMessage());
    }

    @Test
    void refuseWhenThreeSidesDoNotLineUp() {
        SortBatch otherBatch = batchRepo.save(batch(activeChute, "SB-T3", 100, "已完成"));
        LoadPlan otherPlan = planRepo.save(plan(otherBatch, "LP-T3", "待装车"));

        // 袋挂的格口是这口，但单装的是别的批次
        BizException mismatch = assertThrows(BizException.class,
                () -> bags.openBag(input("TB-M1", 10, activeChute, doneBatch, otherPlan)));
        assertTrue(mismatch.getMessage().contains("三边挂不齐"), mismatch.getMessage());
    }

    /**
     * 两个人对着同一批次几乎同时开袋，余量只够一笔：
     * 后到的一笔必须被拒绝并说清此刻还剩几件，不能两笔都成功。
     */
    @Test
    void concurrentOpenBagOnlyOneSucceeds() throws Exception {
        // 余量 60：各打 50，只够一笔
        bags.openBag(input("TB-C0", 40, activeChute, doneBatch, waitingPlan));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> loserError = new AtomicReference<>();
        AtomicReference<String> results = new AtomicReference<>("");

        Runnable worker = (Runnable) () -> {
            ready.countDown();
            try {
                start.await();
                TransitBag b = input(
                        Thread.currentThread().getName().contains("1") ? "TB-C1" : "TB-C2",
                        50, activeChute, doneBatch, waitingPlan);
                bags.openBag(b);
                results.updateAndGet(x -> x + "OK ");
            } catch (Throwable t) {
                if (t instanceof BizException) {
                    loserError.compareAndSet(null, t);
                } else {
                    loserError.compareAndSet(null, t);
                }
                results.updateAndGet(x -> x + "FAIL ");
            }
        };

        Thread t1 = new Thread(worker, "worker-1");
        Thread t2 = new Thread(worker, "worker-2");
        t1.start();
        t2.start();
        ready.await();
        start.countDown();
        t1.join(15000);
        t2.join(15000);

        // 40 已占，只剩 60；两笔各要 50 -> 恰好一笔成功；
        // 后到那笔是在第一笔提交后才拿到锁的，看到的是最新余量 10 件
        assertEquals(90, activeQuantity(), "只能打成一笔，不能打超: " + results.get());
        assertNotNull(loserError.get(), "必须有一笔被拒绝: " + results.get());
        String msg = loserError.get().getMessage();
        assertTrue(msg.contains("其他未拆的袋已占 90 件"), "失败消息要说清别人已占多少: " + msg);
        assertTrue(msg.contains("此刻只剩 10 件"), "失败消息要说清此刻还剩几件: " + msg);
        assertTrue(msg.contains("打不了 50 件"), "失败消息要说清为什么没开成: " + msg);
    }

    /** 两笔合起来恰好打满余量时串行执行，两笔都应成功，不能错杀后到那笔。 */
    @Test
    void concurrentOpenBagFitsExactlyBothSucceed() throws Exception {
        // 40 已占，剩 60；两笔各 30，加起来恰好打满
        bags.openBag(input("TB-D0", 40, activeChute, doneBatch, waitingPlan));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Runnable worker = (Runnable) () -> {
            ready.countDown();
            try {
                start.await();
                bags.openBag(input(
                        Thread.currentThread().getName().contains("1") ? "TB-D1" : "TB-D2",
                        30, activeChute, doneBatch, waitingPlan));
            } catch (Throwable t) {
                error.compareAndSet(null, t);
            }
        };

        Thread t1 = new Thread(worker, "worker-1");
        Thread t2 = new Thread(worker, "worker-2");
        t1.start();
        t2.start();
        ready.await();
        start.countDown();
        t1.join(15000);
        t2.join(15000);

        assertEquals(null, error.get(), "两笔加起来刚好 60，不该拒绝: "
                + (error.get() == null ? "" : error.get().getMessage()));
        assertEquals(100, activeQuantity());
        assertEquals(0L, bags.remaining(doneBatch.id).get("remaining"));
    }

    /** 发车与开袋抢同一张单：要么挂在发车前成功，要么看到已发车被拒，绝不允许发车后还挂上去。 */
    @Test
    void departAndOpenBagAreMutuallyExclusive() throws Exception {
        final com.sorting.center.service.LoadPlanService planService = loadPlans;
        for (int round = 0; round < 5; round++) {
            bagRepo.deleteAll();
            planRepo.deleteAll();
            final LoadPlan freshPlan = planRepo.save(plan(doneBatch, "LP-R" + round, "待装车"));
            final String bagCode = "TB-R" + round;

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<Boolean> bagOk = new AtomicReference<>(null);

            Thread opene = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    bags.openBag(input(bagCode, 10, activeChute, doneBatch, freshPlan));
                    bagOk.set(true);
                } catch (BizException e) {
                    bagOk.set(false);
                } catch (InterruptedException ignored) {
                }
            });
            Thread dep = new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    planService.depart(freshPlan.id);
                } catch (Exception ignored) {
                }
            });
            opene.start();
            dep.start();
            ready.await();
            start.countDown();
            opene.join(15000);
            dep.join(15000);

            assertNotNull(bagOk.get(), "开袋线程必须有确定结果");
            if (bagOk.get()) {
                // 开袋先拿到锁：袋在袋里，发车在其后提交也合法
                assertEquals("在袋", bagRepo.findAll().get(0).status);
            } else {
                // 发车先拿到锁：不允许再挂袋
                assertEquals(0, bagRepo.count());
            }
            assertEquals("已发车", planRepo.findById(freshPlan.id).orElseThrow().status);
        }
    }
}
