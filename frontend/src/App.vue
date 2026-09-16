<template>
  <el-container style="min-height: 100vh">
    <el-header style="display:flex;align-items:center;gap:28px;background:#f5f7fa">
      <strong style="font-size:16px">快递分拣中心 · 格口与分拣批次管理</strong>
      <el-menu mode="horizontal" :default-active="route.path" router style="flex:1;border:none;background:transparent">
        <el-menu-item index="/chutes">格口台账</el-menu-item>
        <el-menu-item index="/batches">分拣批次</el-menu-item>
        <el-menu-item index="/plans">装车发运</el-menu-item>
        <el-menu-item index="/exceptions">异常件</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
