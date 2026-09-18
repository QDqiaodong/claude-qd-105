<template>
  <el-container style="min-height: 100vh">
    <el-aside width="216px" class="app-aside">
      <div class="app-brand">快递分拣中心 · 格口与分拣批次管理</div>
      <el-menu :default-active="route.path" router class="app-menu">
        <el-menu-item index="/chutes">格口台账</el-menu-item>
        <el-menu-item index="/batches">分拣批次</el-menu-item>
        <el-menu-item index="/plans">装车发运</el-menu-item>
        <el-menu-item index="/bags">中转袋台账</el-menu-item>
        <el-menu-item index="/exceptions">异常件</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <span class="app-crumb">{{ route.meta.title }}</span>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<style>
.app-aside {
  display: flex;
  flex-direction: column;
  background: var(--el-color-primary);
}
.app-brand {
  padding: 20px 18px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
  color: #fff;
}
.app-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  --el-menu-text-color: rgba(255, 255, 255, 0.85);
  --el-menu-hover-text-color: #ffffff;
  --el-menu-active-color: #ffffff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.14);
}
.app-menu .el-menu-item.is-active {
  font-weight: 600;
  background: rgba(255, 255, 255, 0.22);
}
.app-header {
  display: flex;
  align-items: center;
  background: #ffffff;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.app-crumb {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-color-primary-dark-2);
}
</style>
