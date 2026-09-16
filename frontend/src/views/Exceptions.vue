<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:14px;flex-wrap:wrap">
        <el-select v-model="filters.batchId" placeholder="全部批次" clearable style="width:180px">
          <el-option v-for="b in batches" :key="b.id" :label="b.code" :value="b.id" />
        </el-select>
        <el-select v-model="filters.kind" placeholder="全部类型" clearable style="width:130px">
          <el-option label="破损" value="破损" />
          <el-option label="错分" value="错分" />
          <el-option label="无面单" value="无面单" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:130px">
          <el-option label="待处理" value="待处理" />
          <el-option label="已处理" value="已处理" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">登记异常件</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="单号" width="120" />
        <el-table-column label="批次" width="120">
          <template #default="{ row }">{{ batchCode(row.batchId) }}</template>
        </el-table-column>
        <el-table-column prop="kind" label="类型" width="100">
          <template #default="{ row }">
            <el-tag type="danger">{{ row.kind }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="问题描述" min-width="200" />
        <el-table-column prop="foundDate" label="发现日期" width="120" />
        <el-table-column prop="handler" label="处理人" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '已处理' ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status === '待处理'" link type="primary" @click="resolve(row)">
              标记处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="登记异常件" width="480px">
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
            <el-option label="破损" value="破损" />
            <el-option label="错分" value="错分" />
            <el-option label="无面单" value="无面单" />
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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { exceptionApi, batchApi } from '../api'

const rows = ref([])
const batches = ref([])
const filters = ref({ batchId: null, kind: '', status: '' })
const visible = ref(false)
const form = ref({})

function batchCode(id) {
  const hit = batches.value.find((b) => b.id === id)
  return hit ? hit.code : id
}

async function load() {
  try {
    rows.value = await exceptionApi.list({ ...filters.value })
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
