<template>
  <div>
    <div class="top">
      <span class="ttl">分拣流水</span>
      <select v-model="statusFilter" class="m">
        <option value="">全部状态</option>
        <option value="待分拣">待分拣</option>
        <option value="分拣中">分拣中</option>
        <option value="已完成">已完成</option>
      </select>
      <span class="grow" />
      <span class="add" @click="openCreate">＋ 开个批次</span>
    </div>

    <div class="rows">
      <div v-for="b in shown" :key="b.id" class="fr">
        <div class="fr-left">
          <div class="fr-code">{{ b.code }}</div>
          <div class="fr-sub">{{ chuteLabel(b.chuteId) }}</div>
        </div>

        <div class="fr-stage">
          <div class="stages">
            <div
              v-for="(s, i) in STAGES"
              :key="s"
              class="stage"
              :class="i < idx(b.status) ? 'done' : i === idx(b.status) ? 'now' : 'todo'"
            >
              <i />
              <span>{{ s }}</span>
            </div>
          </div>
        </div>

        <div class="fr-mid">
          <div class="fr-qty">{{ b.quantity }}<i>件</i></div>
          <div class="fr-date">{{ b.arriveDate }}</div>
        </div>

        <div class="fr-right">
          <span class="fr-op">{{ b.operator }}</span>
          <button v-if="next(b.status)" class="act" @click="advance(b)">
            {{ next(b.status).label }}
          </button>
          <span v-else class="fin">已收工</span>
        </div>
      </div>
      <div v-if="!shown.length" class="none">没有符合条件的批次</div>
    </div>

    <el-dialog v-model="visible" title="开个分拣批次" width="460px">
      <el-form label-width="106px">
        <el-form-item label="批次号">
          <el-input v-model="form.code" placeholder="如 SB-0905" />
        </el-form-item>
        <el-form-item label="分到格口">
          <el-select v-model="form.chuteId" style="width:100%">
            <el-option
              v-for="c in chutes"
              :key="c.id"
              :label="`${c.code}（${c.area} ${c.status}）`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="件数">
          <el-input-number v-model="form.quantity" :min="1" :step="10" />
        </el-form-item>
        <el-form-item label="到港日期">
          <el-date-picker v-model="form.arriveDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="操作人">
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { batchApi, chuteApi } from '../api'

const STAGES = ['待分拣', '分拣中', '已完成']
const NEXT = {
  待分拣: { label: '开工', action: 'start' },
  分拣中: { label: '收工', action: 'done' }
}

const rows = ref([])
const chutes = ref([])
const statusFilter = ref('')
const visible = ref(false)
const form = ref({})

const shown = computed(() =>
  rows.value.filter((b) => !statusFilter.value || b.status === statusFilter.value)
)

function idx(status) {
  const i = STAGES.indexOf(status)
  return i < 0 ? 0 : i
}

function next(status) {
  return NEXT[status] || null
}

function chuteLabel(id) {
  const hit = chutes.value.find((c) => c.id === id)
  return hit ? `${hit.code} ${hit.area}` : id
}

async function load() {
  try {
    rows.value = await batchApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadChutes() {
  try {
    chutes.value = await chuteApi.list({})
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
    await batchApi.create(form.value)
    ElMessage.success('已开批次')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function advance(b) {
  const step = next(b.status)
  if (!step) return
  try {
    await batchApi.advance(b.id, step.action)
    ElMessage.success('已推进')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  await loadChutes()
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
.m {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 6px 10px;
  font-size: 12px;
  outline: none;
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
.rows {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}
.fr {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f7fa;
}
.fr:last-child {
  border-bottom: none;
}
.fr:hover {
  background: #fafcff;
}
.fr-left {
  width: 150px;
  flex: none;
}
.fr-code {
  font-family: monospace;
  font-size: 14px;
  font-weight: 700;
}
.fr-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 3px;
}
.fr-stage {
  flex: 1;
  min-width: 240px;
}
.stages {
  display: flex;
}
.stage {
  flex: 1;
  position: relative;
  text-align: center;
}
.stage::before {
  content: '';
  position: absolute;
  left: 0;
  right: 50%;
  top: 7px;
  height: 2px;
  background: #e9ecf0;
}
.stage:first-child::before {
  display: none;
}
.stage.done::before {
  background: var(--el-color-primary);
}
.stage i {
  position: relative;
  z-index: 1;
  display: inline-block;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid #dcdfe6;
}
.stage.done i {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
}
.stage.now i {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 4px var(--el-color-primary-light-8);
}
.stage.now i::after {
  content: '';
  position: absolute;
  inset: 4px;
  border-radius: 50%;
  background: var(--el-color-primary);
}
.stage span {
  display: block;
  font-size: 11px;
  color: #909399;
  margin-top: 6px;
}
.stage.done span,
.stage.now span {
  color: var(--el-color-primary);
  font-weight: 600;
}
.fr-mid {
  width: 110px;
  text-align: right;
  flex: none;
}
.fr-qty {
  font-family: monospace;
  font-size: 17px;
  color: #303133;
  line-height: 1.1;
}
.fr-qty i {
  font-style: normal;
  font-size: 11px;
  color: #909399;
  margin-left: 2px;
}
.fr-date {
  font-size: 11px;
  color: #a8abb2;
  font-family: monospace;
  margin-top: 4px;
}
.fr-right {
  width: 130px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex: none;
}
.fr-op {
  font-size: 12px;
  color: #a8abb2;
}
.act {
  border: 1px solid var(--el-color-primary);
  background: #fff;
  color: var(--el-color-primary);
  border-radius: 4px;
  padding: 5px 15px;
  font-size: 12px;
  cursor: pointer;
}
.act:hover {
  background: var(--el-color-primary-light-9);
}
.fin {
  font-size: 12px;
  color: #67c23a;
}
.none {
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
  padding: 46px 0;
}
</style>
