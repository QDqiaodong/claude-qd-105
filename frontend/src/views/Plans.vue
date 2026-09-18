<template>
  <div>
    <div class="top">
      <span class="ttl">发车时刻表</span>
      <span class="hint">按发车日期排，一行一趟车</span>
      <span class="grow" />
      <span class="add" @click="openCreate">＋ 开装车单</span>
    </div>

    <div class="tt">
      <div class="tt-head">
        <span class="c1">发车日</span>
        <span class="c2">目的地</span>
        <span class="c3">车 / 货</span>
        <span class="c4">状态</span>
        <span class="c5">操作</span>
      </div>

      <div v-for="p in rows" :key="p.id" class="tt-row" :class="{ sent: p.status === '已发车' }">
        <div class="c1">
          <b>{{ day(p.loadDate) }}</b>
          <i>{{ mon(p.loadDate) }}</i>
        </div>
        <div class="c2">
          <div class="dest">{{ p.destination }}</div>
          <div class="code">{{ p.code }}</div>
        </div>
        <div class="c3">
          <div class="plate">{{ p.plateNo }}</div>
          <div class="load">{{ p.quantity }} 件 · {{ batchCode(p.batchId) }} · {{ p.operator }}</div>
          <div class="bags">中转袋 {{ bagSummary(p.id) }}</div>
        </div>
        <div class="c4">
          <span class="pill" :class="p.status === '已发车' ? 'done' : 'wait'">{{ p.status }}</span>
        </div>
        <div class="c5">
          <button v-if="p.status === '待装车'" class="depart" @click="depart(p)">发　车</button>
          <span v-else class="sentmark">已发车</span>
        </div>
      </div>

      <div v-if="!rows.length" class="none">还没有装车单</div>
    </div>

    <el-dialog v-model="visible" title="开装车单" width="460px">
      <el-form label-width="106px">
        <el-form-item label="单号">
          <el-input v-model="form.code" placeholder="如 LP-0903" />
        </el-form-item>
        <el-form-item label="装哪个批次">
          <el-select v-model="form.batchId" style="width:100%">
            <el-option
              v-for="b in batches"
              :key="b.id"
              :label="`${b.code}（${b.status}，${b.quantity} 件）`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="车牌">
          <el-input v-model="form.plateNo" placeholder="如 沪C11111" />
        </el-form-item>
        <el-form-item label="目的地">
          <el-input v-model="form.destination" placeholder="如 北京中转场" />
        </el-form-item>
        <el-form-item label="件数">
          <el-input-number v-model="form.quantity" :min="1" :step="10" />
        </el-form-item>
        <el-form-item label="装车日期">
          <el-date-picker v-model="form.loadDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="经办人">
          <el-input v-model="form.operator" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { planApi, batchApi, bagApi } from '../api'

const rows = ref([])
const batches = ref([])
const bags = ref([])
const visible = ref(false)
const form = ref({})

function day(d) {
  return d ? d.slice(8) : '--'
}
function mon(d) {
  return d ? d.slice(5, 7) + ' 月' : ''
}
function batchCode(id) {
  const hit = batches.value.find((b) => b.id === id)
  return hit ? hit.code : id
}
function bagSummary(planId) {
  const onPlan = bags.value.filter((t) => t.loadPlanId === planId)
  const active = onPlan.filter((t) => t.status !== '已拆除')
  const pieces = active.reduce((s, t) => s + t.quantity, 0)
  return `${active.length} 个 / ${pieces} 件`
}

async function load() {
  try {
    const [list, bagList] = await Promise.all([planApi.list({}), bagApi.list({})])
    rows.value = [...list].sort((a, b) => String(b.loadDate).localeCompare(String(a.loadDate)))
    bags.value = bagList
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadBatches() {
  try {
    batches.value = await batchApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openCreate() {
  form.value = {}
  visible.value = true
}

async function save() {
  try {
    await planApi.create(form.value)
    ElMessage.success('已开单')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function depart(p) {
  try {
    await planApi.depart(p.id)
    ElMessage.success('已发车')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  await loadBatches()
  await load()
})
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
.c1 {
  width: 86px;
  flex: none;
}
.c2 {
  width: 230px;
  flex: none;
}
.c3 {
  flex: 1;
  min-width: 200px;
}
.c4 {
  width: 110px;
  flex: none;
}
.c5 {
  width: 120px;
  flex: none;
  text-align: right;
}
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
  padding: 14px 20px;
  border-bottom: 1px solid #f5f7fa;
}
.tt-row:last-child {
  border-bottom: none;
}
.tt-row:hover {
  background: #fafcff;
}
.tt-row.sent {
  opacity: 0.72;
}
.tt-row .c1 {
  display: flex;
  align-items: baseline;
  gap: 5px;
}
.c1 b {
  font-size: 25px;
  line-height: 1;
  font-family: monospace;
  color: #303133;
}
.c1 i {
  font-style: normal;
  font-size: 11px;
  color: #909399;
}
.dest {
  font-size: 14px;
  font-weight: 600;
}
.code {
  font-size: 11px;
  color: #a8abb2;
  font-family: monospace;
  margin-top: 3px;
}
.plate {
  font-family: monospace;
  font-size: 13px;
  color: #606266;
}
.load {
  font-size: 11px;
  color: #a8abb2;
  margin-top: 3px;
}
.bags {
  font-size: 11px;
  color: var(--el-color-primary);
  margin-top: 3px;
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
.depart {
  border: none;
  background: var(--el-color-primary);
  color: #fff;
  border-radius: 4px;
  padding: 7px 20px;
  font-size: 13px;
  letter-spacing: 2px;
  cursor: pointer;
}
.depart:hover {
  filter: brightness(1.08);
}
.sentmark {
  font-size: 12px;
  color: #67c23a;
}
.none {
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
  padding: 50px 0;
}
</style>
