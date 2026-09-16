<template>
  <div>
    <div class="top">
      <span class="ttl">格口分布图</span>
      <input v-model="keyword" class="s" placeholder="搜格口 / 片区" />
      <span class="grow" />
      <span class="legend">
        <i class="lg-启用"></i>启用
        <i class="lg-停用"></i>停用
        <i class="lg-维修"></i>维修
      </span>
      <span class="add" @click="openCreate">＋ 新增格口</span>
    </div>

    <div class="map">
      <div
        v-for="c in shown"
        :key="c.id"
        class="cell"
        :class="'s-' + c.status"
        @click="openEdit(c)"
      >
        <div class="ccode">{{ c.code }}</div>
        <div class="carea">{{ c.area }}</div>
        <div class="ccap">{{ c.capacity }} 件</div>
        <div v-if="running(c.id)" class="crun">{{ running(c.id) }} 批未完</div>
      </div>
    </div>

    <div class="stat">
      共 {{ shown.length }} 个格口
      <span class="sep">|</span>
      启用 {{ count('启用') }}
      <span class="sep">|</span>
      停用 {{ count('停用') }}
      <span class="sep">|</span>
      维修 {{ count('维修') }}
      <span class="grow" />
      <span class="nodata">点任意一个格口可以改它的片区、容量和状态</span>
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑格口' : '新增格口'" width="440px">
      <el-form label-width="96px">
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { chuteApi, batchApi } from '../api'

const rows = ref([])
const batches = ref([])
const keyword = ref('')
const visible = ref(false)
const form = ref({})

const shown = computed(() => {
  const k = keyword.value.trim()
  return rows.value.filter(
    (c) => !k || (c.code || '').includes(k) || (c.area || '').includes(k)
  )
})

function running(chuteId) {
  return batches.value.filter((b) => b.chuteId === chuteId && b.status !== '已完成').length
}

function count(status) {
  return shown.value.filter((c) => c.status === status).length
}

async function load() {
  try {
    rows.value = await chuteApi.list({})
    batches.value = await batchApi.list({})
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

<style scoped>
.top {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.ttl {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}
.s {
  width: 200px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  padding: 7px 11px;
  font-size: 13px;
  outline: none;
}
.s:focus {
  border-color: var(--el-color-primary);
}
.grow {
  flex: 1;
}
.legend {
  font-size: 12px;
  color: #909399;
}
.legend i {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  margin: 0 5px 0 12px;
  vertical-align: middle;
}
.lg-启用 {
  background: var(--el-color-primary);
}
.lg-停用 {
  background: #c0c4cc;
}
.lg-维修 {
  background: #f56c6c;
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
.map {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}
.cell {
  aspect-ratio: 1 / 0.86;
  border-radius: 8px;
  padding: 13px 14px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  border: 1px solid transparent;
  transition: transform 0.15s, box-shadow 0.15s;
  background: #fff;
  border-color: #ebeef5;
}
.cell:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 16px rgba(0, 0, 0, 0.08);
}
.cell.s-启用 {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-7);
}
.cell.s-停用 {
  background: #fafafa;
  opacity: 0.6;
}
.cell.s-维修 {
  background: #fef0f0;
  border-color: #fbc4c4;
}
.ccode {
  font-family: monospace;
  font-size: 15px;
  font-weight: 700;
  color: #303133;
}
.carea {
  font-size: 13px;
  margin: 5px 0 auto;
  color: #606266;
}
.ccap {
  font-size: 11px;
  color: #909399;
  font-family: monospace;
}
.crun {
  margin-top: 6px;
  font-size: 11px;
  color: #e6a23c;
}
.stat {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  font-size: 12px;
  color: #909399;
}
.stat .sep {
  color: #dcdfe6;
}
.stat .grow {
  flex: 1;
}
.nodata {
  color: #c0c4cc;
}
</style>
