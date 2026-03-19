<template>
  <div>
    <a-card>
      <template #title>
        <span style="font-size: 15px">发货管理</span>
      </template>
      <template #extra>
        <a-button @click="() => load(1)" :loading="loading">刷新</a-button>
      </template>

      <a-space style="margin-bottom: 16px" wrap>
        <a-button
          :type="filters.status === 'WAIT_SHIPMENT' ? 'primary' : 'default'"
          @click="switchStatus('WAIT_SHIPMENT')"
        >待发货</a-button>
        <a-button
          :type="filters.status === 'SHIPPED' ? 'primary' : 'default'"
          @click="switchStatus('SHIPPED')"
        >已发货</a-button>
      </a-space>

      <a-form layout="inline" style="margin-bottom: 16px; row-gap: 10px">
        <a-form-item label="订单号">
          <a-input v-model:value="filters.orderId" placeholder="精确订单号" style="width: 140px" allow-clear />
        </a-form-item>
        <a-form-item label="客户">
          <a-input v-model:value="filters.customerKeyword" placeholder="昵称/手机号/收件人" style="width: 180px" allow-clear />
        </a-form-item>
        <a-form-item v-if="filters.status === 'SHIPPED'" label="物流状态">
          <a-select v-model:value="filters.shippingStatus" style="width: 160px" allow-clear placeholder="全部物流状态">
            <a-select-option value="IN_TRANSIT">运输中</a-select-option>
            <a-select-option value="OUT_FOR_DELIVERY">派送中</a-select-option>
            <a-select-option value="SIGNED">已签收</a-select-option>
          </a-select>
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
        :dataSource="orders"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        :customRow="onTableRow"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'userInfo'">
            <div style="display: flex; flex-direction: column;">
              <span style="font-weight: 600; color: #1E293B">{{ record.userNickname || '用户' + record.userId }}</span>
              <span v-if="record.userCompanyName" style="font-size: 12px; color: #64748B">{{ record.userCompanyName }}</span>
              <span v-if="record.userPhone" style="font-size: 12px; color: #64748B">手机：{{ record.userPhone }}</span>
              <span v-if="record.userEmail" style="font-size: 12px; color: #64748B">邮箱：{{ record.userEmail }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'products'">
            <div class="products-snapshot">
              <div class="products-snapshot-tags">
                <a-tag v-for="name in (record.previewProductNames || [])" :key="name" color="default">{{ name }}</a-tag>
                <span v-if="!record.previewProductNames || record.previewProductNames.length === 0" style="color: #94A3B8">—</span>
              </div>
              <div class="products-snapshot-count">共 {{ record.previewProductCount || 0 }} 个产品</div>
            </div>
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            <span class="price-gold">¥{{ record.totalAmount }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ displayStatusText(record) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button size="small" type="link" @click.stop="openDetail(record)">详情</a-button>
              <a-button
                v-if="record.status === 'WAIT_SHIPMENT'"
                size="small"
                type="primary"
                @click.stop="onShip(record.id)"
              >发货</a-button>
              <a-dropdown v-if="record.status === 'SHIPPED'">
                <a-button size="small" @click.stop>物流状态 ▾</a-button>
                <template #overlay>
                  <a-menu @click="(info: any) => onShippingStatusChange(record.id, info.key)">
                    <a-menu-item key="IN_TRANSIT">运输中</a-menu-item>
                    <a-menu-item key="OUT_FOR_DELIVERY">派送中</a-menu-item>
                    <a-menu-item key="SIGNED">已签收</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
            <div v-if="record.hasCopyrightWarning" style="margin-top: 8px; color: #faad14; font-size: 12px;">
              <span>⚠️ 缺版权授权书</span>
            </div>
            <div v-else-if="record.hasCopyrightIssue" style="margin-top: 8px; color: #16a34a; font-size: 12px;">
              <span>✅ 已上传版权授权书</span>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="drawerOpen" title="订单详情" width="720">
      <template v-if="detail">
        <a-descriptions bordered :column="2" size="small">
          <a-descriptions-item label="订单号">#{{ detail.order.id }}</a-descriptions-item>
          <a-descriptions-item label="订单状态">
            <a-tag :color="statusColor(detail.order.status)">{{ displayStatusText(detail.order) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="客户">{{ detail.order.userNickname || ('用户' + detail.order.userId) }}</a-descriptions-item>
          <a-descriptions-item label="手机号">{{ detail.order.userPhone || '—' }}</a-descriptions-item>
          <a-descriptions-item label="收件人">{{ detail.order.receiverName || '—' }}</a-descriptions-item>
          <a-descriptions-item label="收件手机号">{{ detail.order.receiverPhone || '—' }}</a-descriptions-item>
          <a-descriptions-item label="收货地址" :span="2">{{ detail.order.receiverAddress || '—' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatTime(detail.order.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="总金额">¥{{ detail.order.totalAmount }}</a-descriptions-item>
        </a-descriptions>
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
  shipOrder,
  updateShippingStatus,
  type Order,
  type OrderDetail
} from '../api/order'

const loading = ref(false)
const orders = ref<Order[]>([])
const drawerOpen = ref(false)
const detail = ref<OrderDetail | null>(null)
const pagination = ref({ current: 1, pageSize: 15, total: 0 })
const filters = ref({
  status: 'WAIT_SHIPMENT',
  orderId: '',
  customerKeyword: '',
  shippingStatus: ''
})

const columns = [
  { title: '订单号', dataIndex: 'id', key: 'id', width: 80 },
  { title: '客户信息', dataIndex: 'userId', key: 'userInfo', width: 180 },
  { title: '产品快照', key: 'products', width: 260 },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 150 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 250 }
]

function statusColor(s: string) {
  if (s === 'WAIT_SHIPMENT') return 'blue'
  if (s === 'SHIPPED') return 'blue'
  if (s === 'COMPLETED') return 'purple'
  if (s === 'AFTER_SALE') return 'magenta'
  if (s === 'CANCELLED') return 'default'
  return 'orange'
}

function statusText(s: string) {
  const map: Record<string, string> = {
    WAIT_SHIPMENT: '待发货',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    AFTER_SALE: '售后中',
    CANCELLED: '已取消'
  }
  return map[s] || s
}

function shippingStatusText(s?: string | null) {
  const map: Record<string, string> = {
    IN_TRANSIT: '运输中',
    OUT_FOR_DELIVERY: '派送中',
    SIGNED: '已签收'
  }
  if (!s) return ''
  return map[s] || s
}

function displayStatusText(order: any) {
  const main = statusText(order?.status || '')
  if (order?.status === 'SHIPPED' && order?.shippingStatus) {
    return `${main} > ${shippingStatusText(order.shippingStatus)}`
  }
  return main
}

function formatTime(t: string) {
  if (!t) return '—'
  return t.replace('T', ' ').substring(0, 19)
}

function switchStatus(status: string) {
  filters.value.status = status
  filters.value.shippingStatus = ''
  load(1)
}

function buildFiltersPayload() {
  const payload: Record<string, any> = { status: filters.value.status }
  const orderIdText = String(filters.value.orderId || '').trim()
  if (orderIdText) {
    const n = Number(orderIdText)
    if (Number.isFinite(n) && n > 0) payload.orderId = n
  }
  if (filters.value.customerKeyword.trim()) payload.customerKeyword = filters.value.customerKeyword.trim()
  return payload
}

async function load(page = pagination.value.current) {
  loading.value = true
  try {
    const res = await listAdminOrders(page, pagination.value.pageSize, buildFiltersPayload())
    const records = (res.records || []) as Order[]
    orders.value =
      filters.value.status === 'SHIPPED' && filters.value.shippingStatus
        ? records.filter((o: any) => o.shippingStatus === filters.value.shippingStatus)
        : records
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

function onTableRow(record: Order) {
  return {
    style: { cursor: 'pointer' },
    onClick: () => openDetail(record)
  }
}

function onSearch() {
  load(1)
}

function onReset() {
  filters.value.orderId = ''
  filters.value.customerKeyword = ''
  filters.value.shippingStatus = ''
  load(1)
}

async function openDetail(order: Order) {
  try {
    detail.value = await getAdminOrderDetail(order.id)
    drawerOpen.value = true
  } catch (e: any) {
    message.error(e.message || '加载详情失败')
  }
}

async function onShip(orderId: number) {
  try {
    await shipOrder(orderId)
    message.success('已发货')
    load()
  } catch (e: any) {
    message.error(e.message || '发货失败')
  }
}

async function onShippingStatusChange(orderId: number, shippingStatus: string) {
  try {
    await updateShippingStatus(orderId, shippingStatus)
    message.success('物流状态更新成功')
    load()
  } catch (e: any) {
    message.error(e.message || '物流状态更新失败')
  }
}

onMounted(() => load())
</script>

<style scoped>
.products-snapshot {
  min-height: 84px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.products-snapshot-tags {
  min-height: 60px;
  max-height: 60px;
  overflow: hidden;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-content: flex-start;
}
.products-snapshot-count {
  margin-top: 6px;
  font-size: 12px;
  color: #64748B;
}
</style>
