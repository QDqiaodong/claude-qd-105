<template>
  <div>
    <div class="top">
      <span class="ttl">中转袋台账</span>
      <span class="hint">先有待装车单才能开袋；发车后袋子冻结</span>
      <select v-model="statusFilter" class="m">
        <option value="">全部状态</option>
        <option value="待发车">待发车</option>
        <option value="已发车">已发车</option>
        <option value="已拆除">已拆除</option>
      </select>
      <span class="grow" />
      <span class="add" @click="openCreate">＋ 开袋</span>
    </div>

    <div class="tt">
      <div class="tt-head">
        <span class="c1">袋号</span>
        <span class="c2">格口 / 批次</span>
        <span class="c3">装车单</span>
        <span class="c4">件数</span>
        <span class="c5">状态</span>
        <span class="c6">操作</span>
      </div>

      <div v-for="t in shown" :key="t.id" class="tt-row" :class="rowClass(t.status)">
        <div class="c1">
          <b>{{ t.code }}</b>
          <i>{{ t.bagDate }} · {{ t.operator }}</i>
        </div>
        <div class="c2">
          <div class="line">{{ chuteCode(t.chuteId) }}</div>
          <div class="sub">{{ batchCode(t.batchId) }}</div>
        </div>
        <div class="c3">
          <div class="line">{{ planCode(t.loadPlanId) }}</div>
          <div class="sub">{{ planDest(t.loadPlanId) }}</div>
        </div>
        <div class="c4">
          <span class="qty">{{ t.quantity }}</span> 件
        </div>
        <div class="c5">
          <span class="pill" :class="pillClass(t.status)">{{ t.status }}</span>
        </div>
        <div class="c6">
          <template v-if="t.status === '待发车'">
            <button class="edit" @click="openEdit(t)">改件数</button>
            <button class="tear" @click="tearDown(t)">拆掉重打</button>
          </template>
          <span v-else-if="t.status === '已发车'" class="frozen">已冻结</span>
          <span v-else class="tornmark">已归还余量</span>
        </div>
      </div>

      <div v-if="!shown.length" class="none">还没有中转袋</div>
    </div>

    <!-- 开袋 -->
    <el-dialog v-model="visible" title="开中转袋" width="480px">
      <el-form label-width="104px">
        <el-form-item label="袋号">
          <el-input v-model="form.code" placeholder="如 TB-0904" />
        </el-form-item>
        <el-form-item label="挂哪张装车单">
          <el-select v-model="form.loadPlanId" style="width:100%" placeholder="先选待装车的装车单">
            <el-option
              v-for="p in openablePlans"
              :key="p.id"
              :label="`${p.code}（${planBatchCode(p)} · ${p.destination} · ${p.status}）`"
              :value="p.id"
            />
          </el-select>
          <div v-if="!openablePlans.length" class="warn">
            没有待装车的装车单：调度定了月台上不许有无家可归的袋，先去装车发运开单
          </div>
        </el-form-item>
        <el-form-item v-if="selectedBatch" label="三边挂齐">
          <div class="tri">
            <span>{{ selectedChute ? selectedChute.code : '-' }}（{{ selectedChute ? selectedChute.status : '' }}）</span>
            <span>{{ selectedBatch.code }}（{{ selectedBatch.status }}，登记 {{ selectedBatch.quantity }} 件）</span>
            <span>{{ selectedPlan.code }}（{{ selectedPlan.status }}）</span>
          </div>
        </el-form-item>
        <el-form-item v-if="selectedBatch" label="本袋还能装">
          <div class="cap">
            <span :class="{ short: remaining <= 0 }">
              余量 {{ remaining }} 件
              （登记 {{ selectedBatch.quantity }} − 待处理异常 {{ pendingOf(selectedBatch.id) }}
              − 未拆袋 {{ usedOf(selectedBatch.id) }}）
            </span>
          </div>
        </el-form-item>
        <el-form-item label="袋装件数">
          <el-input-number v-model="form.quantity" :min="1" :step="10" />
        </el-form-item>
        <el-form-item label="打袋日期">
          <el-date-picker v-model="form.bagDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="经办人">
          <el-input v-model="form.operator" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">开袋</el-button>
      </template>
    </el-dialog>

    <!-- 改件数 -->
    <el-dialog v-model="editVisible" title="改袋内件数" width="380px">
      <el-form label-width="92px">
        <el-form-item label="袋号">
          <span>{{ editing && editing.code }}</span>
        </el-form-item>
        <el-form-item v-if="editing" label="此刻可填">
          <span>最多 {{ editRemaining }} 件（已扣待处理异常与其它未拆袋）</span>
        </el-form-item>
        <el-form-item label="新件数">
          <el-input-number v-model="editQuantity" :min="1" :step="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuantity">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bagApi, planApi, batchApi, chuteApi, exceptionApi } from '../api'

const rows = ref([])
const plans = ref([])
const batches = ref([])
const chutes = ref([])
const pendingEx = ref([])
const statusFilter = ref('')

const visible = ref(false)
const form = ref({})
const editVisible = ref(false)
const editing = ref(null)
const editQuantity = ref(1)

const shown = computed(() =>
  rows.value.filter((t) => !statusFilter.value || t.status === statusFilter.value)
)

const openablePlans = computed(() => plans.value.filter((p) => p.status === '待装车'))

const selectedPlan = computed(
  () => plans.value.find((p) => p.id === form.value.loadPlanId) || null
)
const selectedBatch = computed(() => {
  const p = selectedPlan.value
  if (!p) return null
  return batches.value.find((b) => b.id === p.batchId) || null
})
const selectedChute = computed(() => {
  const b = selectedBatch.value
  if (!b) return null
  return chutes.value.find((c) => c.id === b.chuteId) || null
})

const remaining = computed(() => {
  const b = selectedBatch.value
  if (!b) return 0
  return b.quantity - pendingOf(b.id) - usedOf(b.id)
})

const editRemaining = computed(() => {
  const t = editing.value
  if (!t) return 0
  const b = batches.value.find((x) => x.id === t.batchId)
  if (!b) return 0
  const usedByOthers = usedOf(b.id) - t.quantity
  return b.quantity - pendingOf(b.id) - usedByOthers
})

function pendingOf(batchId) {
  return pendingEx.value.filter((e) => e.batchId === batchId).length
}
function usedOf(batchId) {
  return rows.value
    .filter((t) => t.batchId === batchId && (t.status === '待发车' || t.status === '已发车'))
    .reduce((s, t) => s + t.quantity, 0)
}
function batchCode(id) {
  const hit = batches.value.find((b) => b.id === id)
  return hit ? hit.code : id
}
function planBatchCode(p) {
  return batchCode(p.batchId)
}
function chuteCode(id) {
  const hit = chutes.value.find((c) => c.id === id)
  return hit ? `${hit.code} ${hit.area}` : id
}
function planCode(id) {
  const hit = plans.value.find((p) => p.id === id)
  return hit ? hit.code : id
}
function planDest(id) {
  const hit = plans.value.find((p) => p.id === id)
  return hit ? `${hit.plateNo} · ${hit.destination}` : ''
}
function pillClass(s) {
  return s === '已发车' ? 'done' : s === '已拆除' ? 'torn' : 'wait'
}
function rowClass(s) {
  return s === '已发车' ? 'frozenrow' : s === '已拆除' ? 'tornrow' : ''
}

async function loadAll() {
  try {
    const [b, p, ba, c, ex] = await Promise.all([
      bagApi.list({}),
      planApi.list({}),
      batchApi.list({}),
      chuteApi.list({}),
      exceptionApi.list({ status: '待处理' })
    ])
    rows.value = b
    plans.value = p
    batches.value = ba
    chutes.value = c
    pendingEx.value = ex
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openCreate() {
  form.value = { quantity: 1 }
  visible.value = true
}

async function save() {
  if (!form.value.loadPlanId) {
    ElMessage.error('先选一张待装车的装车单')
    return
  }
  try {
    await bagApi.open(form.value)
    ElMessage.success('已开袋')
    visible.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openEdit(t) {
  editing.value = t
  editQuantity.value = t.quantity
  editVisible.value = true
}

async function saveQuantity() {
  try {
    await bagApi.updateQuantity(editing.value.id, editQuantity.value)
    ElMessage.success('件数已改')
    editVisible.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function tearDown(t) {
  try {
    await ElMessageBox.confirm(
      `确认把 ${t.code} 拆掉重打？拆掉后 ${t.quantity} 件余量归还批次。`,
      '发车前可拆',
      { confirmButtonText: '拆掉', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await bagApi.tearDown(t.id)
    ElMessage.success('袋已拆除，余量已归还')
    await loadAll()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(loadAll)
</script>

<style scoped>
.top {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 16px;
}
.ttl {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}
.hint {
  font-size: 12px;
  color: #909399;
}
.m {
  height: 30px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 0 8px;
  background: #fff;
}
.grow {
  flex: 1;
}
.add {
  font-size: 13px;
  color: var(--el-color-primary);
  cursor: pointer;
  user-select: none;
}
.add:hover {
  text-decoration: underline;
}
.tt {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}
.c1 { width: 170px; flex: none; }
.c2 { width: 150px; flex: none; }
.c3 { width: 200px; flex: none; }
.c4 { width: 90px; flex: none; }
.c5 { width: 92px; flex: none; }
.c6 { flex: 1; min-width: 170px; text-align: right; }
.tt-head {
  display: flex;
  gap: 20px;
  padding: 10px 20px;
  background: #fafbfc;
  border-bottom: 1px solid #ebeef5;
  font-size: 12px;
  color: #909399;
}
.tt-row {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 13px 20px;
  border-bottom: 1px solid #f5f7fa;
}
.tt-row:last-child {
  border-bottom: none;
}
.tt-row:hover {
  background: #fafcff;
}
.frozenrow {
  opacity: 0.72;
}
.tornrow {
  opacity: 0.6;
}
.c1 b {
  display: block;
  font-size: 14px;
  font-family: monospace;
  color: #303133;
}
.c1 i {
  font-style: normal;
  font-size: 11px;
  color: #a8abb2;
}
.line {
  font-size: 13px;
  font-weight: 600;
}
.sub {
  font-size: 11px;
  color: #a8abb2;
  font-family: monospace;
  margin-top: 2px;
}
.qty {
  font-size: 16px;
  font-weight: 700;
  color: var(--el-color-primary);
  font-family: monospace;
}
.pill {
  font-size: 12px;
  border-radius: 9px;
  padding: 2px 10px;
}
.pill.wait {
  background: #fdf6ec;
  color: #b88230;
}
.pill.done {
  background: #f0f9eb;
  color: #529b2e;
}
.pill.torn {
  background: #f4f4f5;
  color: #909399;
}
button.edit,
button.tear {
  border: 1px solid var(--el-color-primary);
  background: #fff;
  color: var(--el-color-primary);
  border-radius: 4px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  margin-left: 8px;
}
button.tear {
  border-color: #f56c6c;
  color: #f56c6c;
}
button.edit:hover {
  background: var(--el-color-primary);
  color: #fff;
}
button.tear:hover {
  background: #f56c6c;
  color: #fff;
}
.frozen {
  font-size: 12px;
  color: #67c23a;
}
.tornmark {
  font-size: 12px;
  color: #909399;
}
.none {
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
  padding: 50px 0;
}
.warn {
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.6;
}
.tri {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #606266;
}
.cap {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
}
.cap .short {
  color: #f56c6c;
  font-weight: 600;
}
</style>
