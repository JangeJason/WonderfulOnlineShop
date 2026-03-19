<template>
  <a-card>
    <template #title>
      <span style="font-size: 15px">操作日志</span>
    </template>
    <template #extra>
      <a-button :loading="loading" @click="() => load(1)">刷新</a-button>
    </template>

    <a-form layout="inline" style="margin-bottom: 16px; row-gap: 10px">
      <a-form-item label="模块">
        <a-select v-model:value="filters.moduleName" style="width: 140px" allow-clear placeholder="全部">
          <a-select-option value="orders">订单</a-select-option>
          <a-select-option value="products">产品</a-select-option>
          <a-select-option value="categories">分类</a-select-option>
          <a-select-option value="after-sales">售后</a-select-option>
          <a-select-option value="shipping">发货</a-select-option>
          <a-select-option value="production">生产</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="动作">
        <a-select v-model:value="filters.actionName" style="width: 130px" allow-clear placeholder="全部">
          <a-select-option value="CREATE">新增</a-select-option>
          <a-select-option value="UPDATE">更新</a-select-option>
          <a-select-option value="DELETE">删除</a-select-option>
          <a-select-option value="QUERY">查询</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="关键词">
        <a-input v-model:value="filters.keyword" style="width: 220px" placeholder="用户/公司/路径/摘要" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" @click="onSearch">查询</a-button>
          <a-button @click="onReset">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>

    <a-table
      rowKey="id"
      :dataSource="records"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      :scroll="{ x: 1350 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'operator'">
          <div style="display: flex; flex-direction: column; line-height: 1.3">
            <span style="font-weight: 600; color: #1E293B">{{ record.adminNickname || ("管理员#" + record.adminUserId) }}</span>
            <span style="font-size: 12px; color: #64748B">{{ record.adminCompanyName || "—" }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'moduleName'">
          <a-tag>{{ moduleText(record.moduleName) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'actionName'">
          <a-tag :color="actionColor(record.actionName)">{{ actionText(record.actionName) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'requestPath'">
          <span style="font-family: Menlo, monospace; font-size: 12px">{{ record.requestPath }}</span>
        </template>
        <template v-else-if="column.key === 'statusCode'">
          <a-tag :color="statusColor(record.statusCode)">{{ record.statusCode ?? "—" }}</a-tag>
        </template>
        <template v-else-if="column.key === 'createdAt'">
          {{ formatTime(record.createdAt) }}
        </template>
        <template v-else-if="column.key === 'summary'">
          <a-typography-text :ellipsis="{ tooltip: record.summary || '' }">{{ record.summary || "—" }}</a-typography-text>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { message } from "ant-design-vue";
import { listOperationLogs, type OperationLogRecord } from "../api/operation-log";

const loading = ref(false);
const records = ref<OperationLogRecord[]>([]);
const pagination = ref({ current: 1, pageSize: 20, total: 0 });
const filters = ref({
  moduleName: "",
  actionName: "",
  keyword: "",
});

const columns = [
  { title: "ID", dataIndex: "id", key: "id", width: 80 },
  { title: "操作人", key: "operator", width: 180 },
  { title: "模块", dataIndex: "moduleName", key: "moduleName", width: 120 },
  { title: "动作", dataIndex: "actionName", key: "actionName", width: 100 },
  { title: "路径", dataIndex: "requestPath", key: "requestPath", width: 260 },
  { title: "目标ID", dataIndex: "targetId", key: "targetId", width: 100 },
  { title: "摘要", key: "summary", width: 260 },
  { title: "IP", dataIndex: "ipAddress", key: "ipAddress", width: 140 },
  { title: "状态码", key: "statusCode", width: 100 },
  { title: "时间", key: "createdAt", width: 170 },
];

function moduleText(v: string) {
  const map: Record<string, string> = {
    orders: "订单",
    products: "产品",
    categories: "分类",
    "after-sales": "售后",
    shipping: "发货",
    production: "生产",
  };
  return map[v] || v;
}

function actionText(v: string) {
  const map: Record<string, string> = {
    CREATE: "新增",
    UPDATE: "更新",
    DELETE: "删除",
    QUERY: "查询",
  };
  return map[v] || v;
}

function actionColor(v: string) {
  if (v === "CREATE") return "green";
  if (v === "UPDATE") return "blue";
  if (v === "DELETE") return "red";
  if (v === "QUERY") return "default";
  return "default";
}

function statusColor(code?: number) {
  if (!code) return "default";
  if (code >= 200 && code < 300) return "green";
  if (code >= 400 && code < 500) return "gold";
  if (code >= 500) return "red";
  return "default";
}

function formatTime(v: string) {
  if (!v) return "—";
  return v.replace("T", " ").substring(0, 19);
}

function buildPayload() {
  const payload: Record<string, any> = {};
  if (filters.value.moduleName) payload.moduleName = filters.value.moduleName;
  if (filters.value.actionName) payload.actionName = filters.value.actionName;
  if (filters.value.keyword.trim()) payload.keyword = filters.value.keyword.trim();
  return payload;
}

async function load(page = pagination.value.current) {
  loading.value = true;
  try {
    const res = await listOperationLogs(page, pagination.value.pageSize, buildPayload());
    records.value = res.records || [];
    pagination.value.current = res.current;
    pagination.value.total = res.total;
  } catch (e: any) {
    message.error(e.message || "加载操作日志失败");
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  load(1);
}

function onReset() {
  filters.value.moduleName = "";
  filters.value.actionName = "";
  filters.value.keyword = "";
  load(1);
}

function handleTableChange(pag: any) {
  load(pag.current);
}

load(1);
</script>
