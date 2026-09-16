<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:14px;flex-wrap:wrap">
        <el-select v-model="filters.batchId" placeholder="全部批次" clearable style="width:190px">
          <el-option v-for="b in batches" :key="b.id" :label="`${b.code}（${b.status}）`" :value="b.id" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:130px">
          <el-option label="待装车" value="待装车" />
          <el-option label="已发车" value="已发车" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">开装车单</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="单号" width="120" />
        <el-table-column label="批次" width="120">
          <template #default="{ row }">{{ batchCode(row.batchId) }}</template>
        </el-table-column>
        <el-table-column prop="plateNo" label="车牌" width="120" />
        <el-table-column prop="destination" label="目的地" min-width="150" />
        <el-table-column prop="quantity" label="件数" width="90" />
        <el-table-column prop="loadDate" label="装车日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '已发车' ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status === '待装车'" link type="primary" @click="depart(row)">
              发车
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="开装车单" width="470px">
      <el-form label-width="106px">
        <el-form-item label="单号">
          <el-input v-model="form.code" placeholder="如 LP-0903" />
        </el-form-item>
        <el-form-item label="批次">
          <el-select v-model="form.batchId" style="width:100%">
            <el-option v-for="b in batches" :key="b.id" :label="`${b.code}（${b.status}，${b.quantity}件）`" :value="b.id" />
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
import { planApi, batchApi } from '../api'

const rows = ref([])
const batches = ref([])
const filters = ref({ batchId: null, status: '' })
const visible = ref(false)
const form = ref({})

function batchCode(id) {
  const hit = batches.value.find((b) => b.id === id)
  return hit ? hit.code : id
}

async function load() {
  try {
    rows.value = await planApi.list({ ...filters.value })
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

async function depart(row) {
  try {
    await planApi.depart(row.id)
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
