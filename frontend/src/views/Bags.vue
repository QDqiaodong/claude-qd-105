<template>
  <div>
    <div class="top">
      <span class="ttl">中转袋台账</span>
      <div class="tabs">
        <span :class="{ on: status === '' }" @click="status = ''">全部 {{ rows.length }}</span>
        <span :class="{ on: status === '在袋' }" @click="status = '在袋'">
          在袋 {{ countBy('在袋') }}
        </span>
        <span :class="{ on: status === '已拆袋' }" @click="status = '已拆袋'">
          已拆袋 {{ countBy('已拆袋') }}
        </span>
      </div>
      <span class="grow" />
      <span class="add" @click="openCreate">＋ 开袋</span>
    </div>

    <div class="tt">
      <div class="tt-head">
        <span class="c1">袋号 / 格口</span>
        <span class="c2">批次</span>
        <span class="c3">挂的装车单</span>
        <span class="c4">件数</span>
        <span class="c5">状态</span>
        <span class="c6">操作</span>
      </div>

      <div v-for="b in shown" :key="b.id" class="tt-row" :class="{ gone: b.status === '已拆袋' }">
        <div class="c1">
          <div class="code">{{ b.code }}</div>
          <div class="sub">{{ chuteCode(b.chuteId) }}</div>
        </div>
        <div class="c2">
          <div>{{ batchCode(b.batchId) }}</div>
          <div class="sub">{{ batchStatus(b.batchId) }}</div>
        </div>
        <div class="c3">
          <div class="code">{{ planCode(b.planId) }}</div>
          <div class="sub">{{ planStatus(b.planId) }} · {{ planPlate(b.planId) }}</div>
        </div>
        <div class="c4">
          <b>{{ b.quantity }}</b> 件
          <div class="sub">{{ b.bagDate }} · {{ b.operator }}</div>
        </div>
        <div class="c5">
          <span class="pill" :class="b.status === '在袋' ? 'live' : 'gonepill'">{{ b.status }}</span>
        </div>
        <div class="c6">
          <button
            v-if="canUnpack(b)"
            class="unpack"
            @click="unpack(b)"
          >
            拆袋重打
          </button>
          <span v-else-if="b.status === '在袋'" class="frozen">已发车冻结</span>
          <span v-else class="gone-mark">—</span>
        </div>
      </div>

      <div v-if="!shown.length" class="none">还没有中转袋</div>
    </div>

    <el-dialog v-model="visible" title="开袋" width="480px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="先挂待装车的装车单，格口和批次自动带出；月台上不允许有无单的袋"
        style="margin-bottom: 16px"
      />
      <el-form label-width="106px">
        <el-form-item label="袋号">
          <el-input v-model="form.code" placeholder="如 TB-0904" />
        </el-form-item>
        <el-form-item label="挂哪张装车单">
          <el-select v-model="form.planId" style="width:100%" placeholder="只列待装车的单" @change="onPlanChange">
            <el-option
              v-for="p in pendingPlans"
              :key="p.id"
              :label="`${p.code}（${planPlate2(p)} · ${batchCode(p.batchId)}）`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="批次">
          <el-input :model-value="batchCode(form.batchId)" disabled />
        </el-form-item>
        <el-form-item label="格口">
          <el-input :model-value="chuteCode(form.chuteId)" disabled />
        </el-form-item>
        <el-form-item label="件数">
          <el-input-number v-model="form.quantity" :min="1" :step="10" />
        </el-form-item>
        <el-form-item v-if="remain" label="本批余量">
          <div class="remain">
            <el-tag :type="remain.remaining > 0 ? 'success' : 'danger'" effect="plain">
              登记 {{ remain.totalQuantity }} · 已打袋 {{ remain.activeQuantity }}
              · 待处理异常 {{ remain.pendingExceptionQuantity }} ·
              <b>还能打 {{ remain.remaining }} 件</b>
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="开袋日期">
          <el-date-picker v-model="form.bagDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="经办人">
          <el-input v-model="form.operator" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">提交开袋</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bagApi, planApi, batchApi, chuteApi } from '../api'

const rows = ref([])
const plans = ref([])
const batches = ref([])
const chutes = ref([])
const status = ref('')
const visible = ref(false)
const form = ref({})
const remain = ref(null)

const pendingPlans = computed(() => plans.value.filter((p) => p.status === '待装车'))

const shown = computed(() =>
  [...rows.value]
    .filter((b) => !status.value || b.status === status.value)
    .sort((a, b) => String(b.bagDate).localeCompare(String(a.bagDate)) || b.id - a.id)
)

function countBy(s) {
  return rows.value.filter((b) => b.status === s).length
}
function batch(id) {
  return batches.value.find((b) => b.id === id)
}
function batchCode(id) {
  const hit = batch(id)
  return hit ? hit.code : id ?? ''
}
function batchStatus(id) {
  const hit = batch(id)
  return hit ? hit.status : ''
}
function plan(id) {
  return plans.value.find((p) => p.id === id)
}
function planCode(id) {
  const hit = plan(id)
  return hit ? hit.code : id
}
function planStatus(id) {
  const hit = plan(id)
  return hit ? hit.status : ''
}
function planPlate(id) {
  const hit = plan(id)
  return hit ? hit.plateNo : ''
}
function planPlate2(p) {
  return p.plateNo
}
function chuteCode(id) {
  const hit = chutes.value.find((c) => c.id === id)
  return hit ? `${hit.code}（${hit.area} · ${hit.status}）` : ''
}
function canUnpack(b) {
  if (b.status !== '在袋') return false
  const p = plan(b.planId)
  return p && p.status === '待装车'
}

async function loadAll() {
  try {
    const [bagList, planList, batchList, chuteList] = await Promise.all([
      bagApi.list({}),
      planApi.list({}),
      batchApi.list({}),
      chuteApi.list({})
    ])
    rows.value = bagList
    plans.value = planList
    batches.value = batchList
    chutes.value = chuteList
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openCreate() {
  form.value = { quantity: 1 }
  remain.value = null
  visible.value = true
}

async function onPlanChange(planId) {
  remain.value = null
  const p = plans.value.find((x) => x.id === planId)
  if (!p) return
  form.value.batchId = p.batchId
  const b = batch(p.batchId)
  form.value.chuteId = b ? b.chuteId : null
  try {
    remain.value = await bagApi.remaining(p.batchId)
    if (remain.value.remaining > 0) {
      form.value.quantity = Math.min(remain.value.remaining, form.value.quantity || 1)
    }
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function save() {
  try {
    await bagApi.open(form.value)
    ElMessage.success('已开袋')
    visible.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function unpack(b) {
  try {
    await ElMessageBox.confirm(
      `确定拆掉 ${b.code}（${b.quantity} 件）吗？拆掉后释放余量，可以重打，台账保留。`,
      '拆袋重打',
      { type: 'warning', confirmButtonText: '拆掉', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await bagApi.unpack(b.id)
    ElMessage.success('已拆袋，余量已释放')
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
.tabs {
  display: flex;
  gap: 6px;
}
.tabs span {
  font-size: 12px;
  color: #606266;
  background: #f4f4f5;
  border-radius: 12px;
  padding: 3px 12px;
  cursor: pointer;
  user-select: none;
}
.tabs span.on {
  background: var(--el-color-primary);
  color: #fff;
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
.c1 { width: 150px; flex: none; }
.c2 { width: 140px; flex: none; }
.c3 { width: 210px; flex: none; }
.c4 { width: 170px; flex: none; }
.c5 { width: 92px; flex: none; }
.c6 { flex: 1; text-align: right; }
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
.tt-row.gone {
  opacity: 0.62;
}
.code {
  font-family: monospace;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.sub {
  font-size: 11px;
  color: #a8abb2;
  margin-top: 3px;
}
.c4 b {
  font-size: 15px;
  font-family: monospace;
  color: #303133;
}
.pill {
  font-size: 12px;
  border-radius: 9px;
  padding: 2px 10px;
}
.pill.live {
  background: #ecf5ff;
  color: #409eff;
}
.pill.gonepill {
  background: #f4f4f5;
  color: #909399;
}
.unpack {
  border: 1px solid #e6a23c;
  background: #fdf6ec;
  color: #b88230;
  border-radius: 4px;
  padding: 6px 14px;
  font-size: 12px;
  cursor: pointer;
}
.unpack:hover {
  background: #faecd8;
}
.frozen {
  font-size: 12px;
  color: #f56c6c;
}
.gone-mark {
  color: #c0c4cc;
}
.none {
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
  padding: 50px 0;
}
.remain {
  line-height: 1.9;
}
</style>
