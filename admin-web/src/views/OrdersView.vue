<template>
  <div>
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ pagination.total }}</div>
        <div class="stat-label">全部订单记录</div>
      </div>
      <div class="stat-card" style="grid-column: span 5; display: flex; align-items: center; justify-content: center">
        <div class="stat-label" style="text-align: center; color: #64748B; font-size: 14px">
          此处显示当前页订单的粗略分类<br/>
          (实际统计需后端聚合接口，或全量拉取。此处为简单展示当前页的状态分布。)
        </div>
      </div>
    </div>

    <a-card>
      <template #title>
        <span style="font-size: 15px">订单列表</span>
      </template>
      <template #extra>
        <a-button @click="() => load(1)" :loading="loading">刷新</a-button>
      </template>

      <a-table
        rowKey="id"
        :dataSource="orders"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'userInfo'">
            <div style="display: flex; flex-direction: column;">
              <span style="font-weight: 600; color: #1E293B">{{ record.userNickname || '用户' + record.userId }}</span>
              <span v-if="record.userCompanyName" style="font-size: 12px; color: #64748B">{{ record.userCompanyName }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            <span class="price-gold">¥{{ record.totalAmount }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button size="small" type="link" @click="openDetail(record)">详情</a-button>
              <a-button v-if="record.status === 'PAID'" size="small" type="primary" @click="onShip(record.id)">发货</a-button>
              <a-dropdown v-if="record.status !== 'COMPLETED' && record.status !== 'CANCELLED'">
                <a-button size="small" type="link">更新状态 ▾</a-button>
                <template #overlay>
                  <a-menu @click="(info: any) => onStatusChange(record.id, info.key)">
                    <a-menu-item v-if="record.status === 'PENDING'" key="PAID">标记已支付</a-menu-item>
                    <a-menu-item v-if="record.status === 'PAID'" key="SHIPPED">标记已发货</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
            <div v-if="record.hasCopyrightWarning" style="margin-top: 8px; color: #faad14; font-size: 12px;">
              <span title="订单中含有版权商品但未上传授权书">⚠️ 缺版权授权书</span>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 订单详情抽屉 -->
    <a-drawer v-model:open="drawerOpen" title="订单详情" width="600">
      <template v-if="detail">
        <a-descriptions bordered :column="1" size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="订单号">#{{ detail.order.id }}</a-descriptions-item>
          <a-descriptions-item label="客户名称">
            {{ detail.order.userNickname || '用户' + detail.order.userId }} 
            <span v-if="detail.order.userCompanyName" style="color: #64748B">({{ detail.order.userCompanyName }})</span>
          </a-descriptions-item>
          <a-descriptions-item label="总金额">
            <span class="price-gold" style="font-size: 18px">¥{{ detail.order.totalAmount }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColor(detail.order.status)">{{ statusText(detail.order.status) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="备注">{{ detail.order.remark || '—' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatTime(detail.order.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatTime(detail.order.updatedAt) }}</a-descriptions-item>
        </a-descriptions>

        <h4 style="margin-bottom: 8px; color: var(--c-primary); font-weight: 600">📦 订单条目</h4>
        <a-table
          rowKey="id"
          :dataSource="detail.items"
          :columns="itemColumns"
          size="small"
          :pagination="false"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'unitPrice'">
              <span class="price-gold">¥{{ record.unitPrice }}</span>
            </template>
            <template v-else-if="column.key === 'subtotal'">
              <span class="price-gold">¥{{ (Number(record.unitPrice) * record.quantity).toFixed(2) }}</span>
            </template>
            <template v-else-if="column.key === 'params'">
              <a-popover v-if="record.paramsSnapshot" title="参数快照">
                <template #content>
                  <pre style="max-width: 300px; white-space: pre-wrap; font-size: 12px">{{ formatParams(record.paramsSnapshot) }}</pre>
                </template>
                <a-button size="small" type="link">查看参数</a-button>
              </a-popover>
              <span v-else style="color: #ccc">—</span>
            </template>
            <template v-else-if="column.key === 'designFile'">
              <a-button v-if="record.designFileUrl" size="small" type="primary" ghost @click="previewImage(record.designFileUrl)">
                查看图稿
              </a-button>
              <span v-else style="color: #ccc">—</span>
            </template>
            <template v-else-if="column.key === 'copyright'">
              <span v-if="!record.hasCopyright" style="color: #ccc">无</span>
              <div v-else>
                <a-tag v-if="!record.copyrightFileUrl" color="warning">缺失</a-tag>
                <a-button v-else size="small" type="dashed" @click="previewImage(record.copyrightFileUrl)">
                  查看授权书
                </a-button>
              </div>
            </template>
          </template>
        </a-table>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  listAdminOrders,
  getAdminOrderDetail,
  updateOrderStatus,
  shipOrder,
  type Order,
  type OrderDetail
} from '../api/order'

const loading = ref(false)
const orders = ref<Order[]>([])
const drawerOpen = ref(false)
const detail = ref<OrderDetail | null>(null)
const pagination = ref({ current: 1, pageSize: 15, total: 0 })

const columns = [
  { title: '订单号', dataIndex: 'id', key: 'id', width: 80 },
  { title: '客户信息', dataIndex: 'userId', key: 'userInfo', width: 200 },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 220 }
];

const itemColumns = [
  { title: '商品', dataIndex: 'productName' },
  { title: '设计文件', key: 'designFile', width: 100 },
  { title: '数量', dataIndex: 'quantity', width: 60 },
  { title: '单价', key: 'unitPrice', width: 100 },
  { title: '小计', key: 'subtotal', width: 100 },
  { title: '参数', key: 'params', width: 100 }
]

function statusColor(s: string) {
  if (s === 'PENDING') return 'orange'
  if (s === 'PAID') return 'green'
  if (s === 'SHIPPED') return 'blue'
  if (s === 'COMPLETED') return 'purple'
  if (s === 'CANCELLED') return 'default'
  return 'blue'
}

function statusText(s: string) {
  const map: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消'
  }
  return map[s] || s
}

function formatTime(t: string) {
  if (!t) return '—'
  return t.replace('T', ' ').substring(0, 19)
}

function formatParams(snapshot: string) {
  try {
    return JSON.stringify(JSON.parse(snapshot), null, 2)
  } catch {
    return snapshot
  }
}

function previewImage(url: string) {
  if (url) {
    const fullUrl = url.startsWith('http') ? url : 'http://localhost:8080' + url;
    window.open(fullUrl, '_blank');
  }
}

async function load(page = pagination.value.current) {
  loading.value = true
  try {
    const res = await listAdminOrders(page, pagination.value.pageSize)
    // We don't have items in the list endpoint, so the warning can only reliably show in the detail view unless we add it to OrderDTO.
    // For now, we will just show the warning if any item has copyright issues but no file.
    orders.value = res.records.map((o: any) => {
      // If the backend adds a boolean hasCopyrightWarning later, we use it.
      // Otherwise, the detailed items in the drawer will show it individually.
      return { ...o, hasCopyrightWarning: o.hasCopyrightWarning || false }
    })
    pagination.value.current = res.current
    pagination.value.total = res.total
  } catch (e: any) {
    message.error(e.message || '加载订单失败')
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: any) {
  load(pag.current)
}

async function openDetail(order: Order) {
  try {
    detail.value = await getAdminOrderDetail(order.id)
    drawerOpen.value = true
  } catch (e: any) {
    message.error(e.message || '加载详情失败')
  }
}

async function onStatusChange(orderId: number, status: string) {
  try {
    await updateOrderStatus(orderId, status)
    message.success('状态更新成功')
    load()
    if (detail.value && detail.value.order.id === orderId) {
      detail.value = await getAdminOrderDetail(orderId)
    }
  } catch (e: any) {
    message.error(e.message || '状态更新失败')
  }
}

async function onShip(orderId: number) {
  try {
    await shipOrder(orderId)
    message.success('已发货')
    load()
    if (detail.value && detail.value.order.id === orderId) {
      detail.value = await getAdminOrderDetail(orderId)
    }
  } catch (e: any) {
    message.error(e.message || '发货失败')
  }
}

onMounted(() => load())
</script>

<style scoped>
.stats-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
</style>
