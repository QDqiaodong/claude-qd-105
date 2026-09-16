<template>
  <div>
    <div class="top">
      <span class="ttl">异常件</span>
      <div class="tabs">
        <span :class="{ on: kind === '' }" @click="kind = ''">全部 {{ rows.length }}</span>
        <span
          v-for="k in kinds"
          :key="k"
          :class="{ on: kind === k }"
          @click="kind = k"
        >
          {{ k }} {{ count(k) }}
        </span>
      </div>
      <span class="grow" />
      <span class="add" @click="openCreate">＋ 登记一件</span>
    </div>

    <div class="stream">
      <div v-for="e in shown" :key="e.id" class="msg" :class="'k-' + e.kind">
        <div class="stripe" />
        <div class="body">
          <div class="head">
            <span class="badge">{{ e.kind }}</span>
            <span class="code">{{ e.code }}</span>
            <span class="grow" />
            <span class="date">{{ e.foundDate }}</span>
          </div>
          <div class="desc">{{ e.description }}</div>
          <div class="foot">
            <span class="meta">
              {{ batchCode(e.batchId) }} · {{ e.handler }}
            </span>
            <span class="grow" />
            <span v-if="e.status === '待处理'" class="act" @click="resolve(e)">标记已处理</span>
            <span v-else class="done">已处理</span>
          </div>
        </div>
      </div>
      <div v-if="!shown.length" class="none">这一类没有异常件</div>
    </div>

    <el-dialog v-model="visible" title="登记异常件" width="460px">
      <el-form label-width="106px">
        <el-form-item label="单号">
          <el-input v-model="form.code" placeholder="如 EX-0903" />
        </el-form-item>
        <el-form-item label="所属批次">
          <el-select v-model="form.batchId" style="width:100%">
            <el-option v-for="b in batches" :key="b.id" :label="`${b.code}（${b.status}）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="异常类型">
          <el-select v-model="form.kind" style="width:100%">
            <el-option v-for="k in kinds" :key="k" :label="k" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="form.description" type="textarea" placeholder="要说清是什么异常" />
        </el-form-item>
        <el-form-item label="发现日期">
          <el-date-picker v-model="form.foundDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="处理人">
          <el-input v-model="form.handler" />
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
import { exceptionApi, batchApi } from '../api'

const kinds = ['破损', '错分', '无面单']
const rows = ref([])
const batches = ref([])
const kind = ref('')
const visible = ref(false)
const form = ref({})

const shown = computed(() =>
  rows.value.filter((e) => !kind.value || e.kind === kind.value)
)

function count(k) {
  return rows.value.filter((e) => e.kind === k).length
}

function batchCode(id) {
  const hit = batches.value.find((b) => b.id === id)
  return hit ? hit.code : id
}

async function load() {
  try {
    const list = await exceptionApi.list({})
    rows.value = [...list].sort((a, b) => String(b.foundDate).localeCompare(String(a.foundDate)))
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
  form.value = { kind: '破损' }
  visible.value = true
}

async function save() {
  try {
    await exceptionApi.create(form.value)
    ElMessage.success('已登记')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function resolve(row) {
  try {
    await exceptionApi.resolve(row.id)
    ElMessage.success('已处理')
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
  align-items: center;
  gap: 18px;
  margin-bottom: 18px;
  flex-wrap: wrap;
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
  padding: 5px 13px;
  border-radius: 14px;
  color: #606266;
  cursor: pointer;
  user-select: none;
  background: #f4f4f5;
}
.tabs span:hover {
  color: var(--el-color-primary);
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
.stream {
  max-width: 860px;
}
.msg {
  display: flex;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
  transition: box-shadow 0.15s;
}
.msg:hover {
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.06);
}
.stripe {
  width: 4px;
  flex: none;
}
.k-破损 .stripe {
  background: #f56c6c;
}
.k-错分 .stripe {
  background: #e6a23c;
}
.k-无面单 .stripe {
  background: #909399;
}
.body {
  flex: 1;
  padding: 13px 17px;
}
.head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.badge {
  font-size: 11px;
  border-radius: 3px;
  padding: 1px 8px;
  color: #fff;
}
.k-破损 .badge {
  background: #f56c6c;
}
.k-错分 .badge {
  background: #e6a23c;
}
.k-无面单 .badge {
  background: #909399;
}
.code {
  font-family: monospace;
  font-size: 13px;
  color: #606266;
}
.date {
  font-size: 12px;
  color: #a8abb2;
  font-family: monospace;
}
.desc {
  font-size: 13px;
  color: #303133;
  margin: 9px 0;
  line-height: 1.6;
}
.foot {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #a8abb2;
}
.act {
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 12px;
}
.act:hover {
  text-decoration: underline;
}
.done {
  color: #67c23a;
  font-size: 12px;
}
.none {
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
  padding: 50px 0;
}
</style>
