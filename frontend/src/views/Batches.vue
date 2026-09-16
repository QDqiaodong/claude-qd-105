<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:14px;flex-wrap:wrap">
        <el-select v-model="filters.chuteId" placeholder="全部格口" clearable style="width:160px">
          <el-option v-for="c in chutes" :key="c.id" :label="`${c.code}（${c.area}）`" :value="c.id" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:140px">
          <el-option label="待分拣" value="待分拣" />
          <el-option label="分拣中" value="分拣中" />
          <el-option label="已完成" value="已完成" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">开批次</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="批次号" width="120" />
        <el-table-column label="格口" width="150">
          <template #default="{ row }">{{ chuteLabel(row.chuteId) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="件数" width="90" />
        <el-table-column prop="arriveDate" label="到港日期" width="120" />
        <el-table-column prop="operator" label="操作人" width="110" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '已完成' ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button v-if="row.status === '待分拣'" link type="primary" @click="advance(row, 'start')">
              开工
            </el-button>
            <el-button v-if="row.status === '分拣中'" link type="success" @click="advance(row, 'done')">
              收工
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" title="开分拣批次" width="470px">
      <el-form label-width="106px">
        <el-form-item label="批次号">
          <el-input v-model="form.code" placeholder="如 SB-0905" />
        </el-form-item>
        <el-form-item label="分到格口">
          <el-select v-model="form.chuteId" style="width:100%">
            <el-option v-for="c in chutes" :key="c.id" :label="`${c.code}（${c.area} ${c.status}）`" :value="c.id" />
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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { batchApi, chuteApi } from '../api'

const rows = ref([])
const chutes = ref([])
const filters = ref({ chuteId: null, status: '' })
const visible = ref(false)
const form = ref({})

function chuteLabel(id) {
  const hit = chutes.value.find((c) => c.id === id)
  return hit ? `${hit.code}（${hit.area}）` : id
}

async function load() {
  try {
    rows.value = await batchApi.list({ ...filters.value })
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

async function advance(row, action) {
  try {
    await batchApi.advance(row.id, action)
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
