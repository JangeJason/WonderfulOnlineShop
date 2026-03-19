<template>
  <div>
    <a-card>
      <template #title>
        <span style="font-size: 15px">售后处理</span>
      </template>
      <template #extra>
        <a-button @click="() => load(1)" :loading="loading">刷新</a-button>
      </template>

      <a-form layout="inline" style="margin-bottom: 16px; row-gap: 10px">
        <a-form-item label="售后状态">
          <a-select v-model:value="filters.status" style="width: 140px" allow-clear placeholder="全部状态">
            <a-select-option value="PENDING">待处理</a-select-option>
            <a-select-option value="PROCESSING">处理中</a-select-option>
            <a-select-option value="RESOLVED">已解决</a-select-option>
            <a-select-option value="REJECTED">已驳回</a-select-option>
            <a-select-option value="CLOSED">已关闭</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="订单号">
          <a-input v-model:value="filters.orderIdText" placeholder="精确订单号" style="width: 140px" allow-clear />
        </a-form-item>
        <a-form-item label="关键词">
          <a-input v-model:value="filters.keyword" placeholder="售后单/订单/用户" style="width: 180px" allow-clear />
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
        :customRow="onTableRow"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'requestType'">
            {{ requestTypeText(record.requestType) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'selectedItems'">
            <a-tag v-for="it in (record.selectedItems || [])" :key="it.id">{{ it.productName }}</a-tag>
          </template>
          <template v-else-if="column.key === 'userInfo'">
            <div style="display: flex; flex-direction: column; line-height: 1.3">
              <span style="font-weight: 600; color: #1E293B">{{ record.userNickname || "用户" }}</span>
              <span v-if="record.userCompanyName" style="font-size: 12px; color: #64748B">{{ record.userCompanyName }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'contactInfo'">
            <div style="display: flex; flex-direction: column; line-height: 1.3">
              <span style="font-size: 12px; color: #64748B">手机：{{ record.userPhone || "—" }}</span>
              <span style="font-size: 12px; color: #64748B">邮箱：{{ record.userEmail || "—" }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button size="small" type="link" @click.stop="openDetail(record.id)">详情</a-button>
              <a-dropdown>
                <a-button size="small" @click.stop>处理状态 ▾</a-button>
                <template #overlay>
                  <a-menu @click="(info: any) => quickSetStatus(record.id, info.key)">
                    <a-menu-item key="PENDING">待处理</a-menu-item>
                    <a-menu-item key="PROCESSING">处理中</a-menu-item>
                    <a-menu-item key="RESOLVED">已解决</a-menu-item>
                    <a-menu-item key="REJECTED">已驳回</a-menu-item>
                    <a-menu-item key="CLOSED">已关闭</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="drawerOpen" title="售后详情" width="980">
      <template v-if="detail">
        <a-descriptions bordered :column="2" size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="售后单号">#{{ detail.request.id }}</a-descriptions-item>
          <a-descriptions-item label="售后状态">
            <a-tag :color="statusColor(detail.request.status)">{{ statusText(detail.request.status) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="订单号">#{{ detail.request.orderId }}</a-descriptions-item>
          <a-descriptions-item label="售后类型">{{ requestTypeText(detail.request.requestType) }}</a-descriptions-item>
          <a-descriptions-item label="用户">{{ detail.request.userNickname || "—" }}</a-descriptions-item>
          <a-descriptions-item label="公司名称">{{ detail.request.userCompanyName || "—" }}</a-descriptions-item>
          <a-descriptions-item label="联系方式">手机：{{ detail.request.userPhone || "—" }} / 邮箱：{{ detail.request.userEmail || "—" }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatTime(detail.request.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatTime(detail.request.updatedAt) }}</a-descriptions-item>
          <a-descriptions-item label="问题描述" :span="2">{{ detail.request.detailText || "—" }}</a-descriptions-item>
          <a-descriptions-item label="处理备注" :span="2">{{ detail.request.adminRemark || "—" }}</a-descriptions-item>
        </a-descriptions>

        <a-card size="small" title="售后图片" style="margin-bottom: 16px">
          <a-space wrap>
            <a-button v-for="(url, idx) in detail.imageUrls || []" :key="idx" @click="openFile(url)">图片{{ idx + 1 }}</a-button>
            <span v-if="!detail.imageUrls || detail.imageUrls.length === 0" style="color:#94A3B8">—</span>
          </a-space>
        </a-card>

        <a-card size="small" title="申请售后的商品" style="margin-bottom: 16px">
          <a-space wrap>
            <a-tag v-for="it in selectedOrderItems" :key="it.id">{{ it.productName }} x{{ it.quantity }}</a-tag>
            <span v-if="selectedOrderItems.length === 0" style="color:#94A3B8">—</span>
          </a-space>
        </a-card>

        <h4 style="margin-bottom: 8px; color: var(--c-primary); font-weight: 600">📦 原订单商品明细</h4>
        <a-table :dataSource="detail.orderItems" :columns="itemColumns" rowKey="id" size="small" :pagination="false" :scroll="{ x: 1200 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'unitPrice'">
              <span class="price-gold">¥{{ record.unitPrice }}</span>
            </template>
            <template v-else-if="column.key === 'subtotal'">
              <span class="price-gold">¥{{ (Number(record.unitPrice) * Number(record.quantity)).toFixed(2) }}</span>
            </template>
            <template v-else-if="column.key === 'files'">
              <a-space>
                <a-button v-if="record.printFileUrl" size="small" @click="openFile(record.printFileUrl)">印品文件</a-button>
                <a-button v-if="record.proofFileUrl" size="small" @click="openFile(record.proofFileUrl)">工艺对稿图</a-button>
                <a-button v-if="record.copyrightFileUrl" size="small" @click="openFile(record.copyrightFileUrl)">版权书</a-button>
              </a-space>
              <span v-if="!record.printFileUrl && !record.proofFileUrl && !record.copyrightFileUrl" style="color:#94A3B8">—</span>
            </template>
          </template>
        </a-table>

        <a-card size="small" title="处理售后" style="margin-top: 16px">
          <a-space direction="vertical" style="width: 100%">
            <a-select v-model:value="editStatus" style="width: 180px">
              <a-select-option value="PENDING">待处理</a-select-option>
              <a-select-option value="PROCESSING">处理中</a-select-option>
              <a-select-option value="RESOLVED">已解决</a-select-option>
              <a-select-option value="REJECTED">已驳回</a-select-option>
              <a-select-option value="CLOSED">已关闭</a-select-option>
            </a-select>
            <a-textarea v-model:value="editRemark" :rows="3" placeholder="处理备注（可选）" />
            <a-button type="primary" @click="saveStatus">保存处理结果</a-button>
          </a-space>
        </a-card>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { message } from "ant-design-vue";
import { getAdminAfterSaleDetail, listAdminAfterSales, updateAdminAfterSaleStatus, type AfterSaleDetail, type AfterSaleRecord } from "../api/after-sale";

const loading = ref(false);
const records = ref<AfterSaleRecord[]>([]);
const pagination = ref({ current: 1, pageSize: 15, total: 0 });
const drawerOpen = ref(false);
const detail = ref<AfterSaleDetail | null>(null);
const editStatus = ref("PENDING");
const editRemark = ref("");

const filters = ref({
  status: "",
  orderIdText: "",
  keyword: "",
});

const columns = [
  { title: "售后单号", dataIndex: "id", key: "id", width: 90 },
  { title: "订单号", dataIndex: "orderId", key: "orderId", width: 90 },
  { title: "用户", dataIndex: "userNickname", key: "userInfo", width: 180 },
  { title: "联系方式", key: "contactInfo", width: 200 },
  { title: "售后类型", key: "requestType", width: 120 },
  { title: "售后商品", key: "selectedItems", width: 240 },
  { title: "状态", key: "status", width: 100 },
  { title: "提交时间", key: "createdAt", width: 170 },
  { title: "操作", key: "actions", width: 200 },
];

const itemColumns = [
  { title: "商品", dataIndex: "productName", width: 160 },
  { title: "数量", dataIndex: "quantity", width: 70 },
  { title: "单价", key: "unitPrice", width: 100 },
  { title: "小计", key: "subtotal", width: 100 },
  { title: "文件", key: "files", width: 300 },
];

const selectedOrderItems = computed(() => {
  if (!detail.value) return [];
  const ids = new Set(detail.value.selectedItemIds || []);
  return (detail.value.orderItems || []).filter((it: any) => ids.has(it.id));
});

function requestTypeText(v: string) {
  const map: Record<string, string> = {
    QUANTITY_SHORTAGE: "数量不足",
    WRONG_PRODUCT: "产品有误",
    PRINT_ISSUE: "印刷问题",
    PROCESS_ISSUE: "工艺问题",
    LOGISTICS_ISSUE: "物流问题",
    RETURN_REFUND: "退货退款",
  };
  return map[v] || v;
}

function statusText(v: string) {
  const map: Record<string, string> = {
    PENDING: "待处理",
    PROCESSING: "处理中",
    RESOLVED: "已解决",
    REJECTED: "已驳回",
    CLOSED: "已关闭",
  };
  return map[v] || v;
}

function statusColor(v: string) {
  if (v === "PENDING") return "gold";
  if (v === "PROCESSING") return "blue";
  if (v === "RESOLVED") return "green";
  if (v === "REJECTED") return "red";
  return "default";
}

function formatTime(v: string) {
  if (!v) return "—";
  return v.replace("T", " ").substring(0, 19);
}

function buildPayload() {
  const payload: Record<string, any> = {};
  if (filters.value.status) payload.status = filters.value.status;
  const orderId = Number(filters.value.orderIdText || 0);
  if (Number.isFinite(orderId) && orderId > 0) payload.orderId = orderId;
  if (filters.value.keyword.trim()) payload.keyword = filters.value.keyword.trim();
  return payload;
}

async function load(page = pagination.value.current) {
  loading.value = true;
  try {
    const res = await listAdminAfterSales(page, pagination.value.pageSize, buildPayload());
    records.value = res.records || [];
    pagination.value.current = res.current;
    pagination.value.total = res.total;
  } catch (e: any) {
    message.error(e.message || "加载售后记录失败");
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  load(1);
}

function onReset() {
  filters.value.status = "";
  filters.value.orderIdText = "";
  filters.value.keyword = "";
  load(1);
}

function handleTableChange(pag: any) {
  load(pag.current);
}

function onTableRow(record: AfterSaleRecord) {
  return {
    style: { cursor: "pointer" },
    onClick: () => openDetail(record.id),
  };
}

async function openDetail(id: number) {
  try {
    detail.value = await getAdminAfterSaleDetail(id);
    editStatus.value = detail.value.request.status || "PENDING";
    editRemark.value = detail.value.request.adminRemark || "";
    drawerOpen.value = true;
  } catch (e: any) {
    message.error(e.message || "加载详情失败");
  }
}

async function quickSetStatus(id: number, status: string) {
  try {
    await updateAdminAfterSaleStatus(id, status, "");
    message.success("状态已更新");
    await load();
    if (detail.value && detail.value.request.id === id) {
      detail.value = await getAdminAfterSaleDetail(id);
      editStatus.value = detail.value.request.status;
      editRemark.value = detail.value.request.adminRemark || "";
    }
  } catch (e: any) {
    message.error(e.message || "更新失败");
  }
}

async function saveStatus() {
  if (!detail.value) return;
  try {
    await updateAdminAfterSaleStatus(detail.value.request.id, editStatus.value, editRemark.value);
    message.success("处理结果已保存");
    await load();
    detail.value = await getAdminAfterSaleDetail(detail.value.request.id);
  } catch (e: any) {
    message.error(e.message || "保存失败");
  }
}

function openFile(url: string) {
  if (!url) return;
  const full = url.startsWith("http") ? url : `http://localhost:8080${url}`;
  window.open(full, "_blank");
}

load(1);
</script>
