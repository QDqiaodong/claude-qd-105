<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;margin-bottom:14px;flex-wrap:wrap">
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:130px">
          <el-option label="启用" value="启用" />
          <el-option label="停用" value="停用" />
          <el-option label="维修" value="维修" />
        </el-select>
        <el-input v-model="filters.area" placeholder="片区" clearable style="width:140px" />
        <el-input v-model="filters.keyword" placeholder="格口编号" clearable style="width:150px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">新增格口</el-button>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="格口" width="100" />
        <el-table-column prop="area" label="片区" width="120" />
        <el-table-column prop="capacity" label="容量(件)" width="110" />
        <el-table-column label="未完成批次" width="130">
          <template #default="{ row }">{{ running(row.id) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '启用' ? 'success' : row.status === '维修' ? 'danger' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '编辑格口' : '新增格口'" width="450px">
      <el-form label-width="106px">
        <el-form-item label="格口编号">
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 C-06" />
        </el-form-item>
        <el-form-item label="片区">
          <el-input v-model="form.area" placeholder="如 华北" />
        </el-form-item>
        <el-form-item label="容量(件)">
          <el-input-number v-model="form.capacity" :min="1" :step="50" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="启用" value="启用" />
            <el-option label="停用" value="停用" />
            <el-option label="维修" value="维修" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { chuteApi, batchApi } from '../api'

const rows = ref([])
const allBatches = ref([])
const filters = ref({ status: '', area: '', keyword: '' })
const visible = ref(false)
const form = ref({})

function running(chuteId) {
  return allBatches.value.filter((b) => b.chuteId === chuteId && b.status !== '已完成').length
}

async function load() {
  try {
    rows.value = await chuteApi.list({ ...filters.value })
    allBatches.value = await batchApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openCreate() {
  form.value = { capacity: 300, status: '启用' }
  visible.value = true
}

function openEdit(row) {
  form.value = { ...row }
  visible.value = true
}

async function save() {
  try {
    if (form.value.id) {
      await chuteApi.update(form.value.id, form.value)
    } else {
      await chuteApi.create(form.value)
    }
    ElMessage.success('已保存')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>
