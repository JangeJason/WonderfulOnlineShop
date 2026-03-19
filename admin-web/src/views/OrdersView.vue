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

      <a-form layout="inline" style="margin-bottom: 16px; row-gap: 10px">
        <a-form-item label="订单号">
          <a-input v-model:value="filters.orderId" placeholder="精确订单号" style="width: 140px" allow-clear />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="filters.status" style="width: 130px" allow-clear placeholder="全部状态">
            <a-select-option value="PENDING">待支付</a-select-option>
            <a-select-option value="WAIT_PRODUCTION">待生产</a-select-option>
            <a-select-option value="IN_PRODUCTION">生产中</a-select-option>
            <a-select-option value="WAIT_SHIPMENT">待发货</a-select-option>
            <a-select-option value="SHIPPED">已发货</a-select-option>
            <a-select-option value="COMPLETED">已完成</a-select-option>
            <a-select-option value="AFTER_SALE">售后中</a-select-option>
            <a-select-option value="REJECTED">已驳回</a-select-option>
            <a-select-option value="CANCELLED">已取消</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="客户">
          <a-input v-model:value="filters.customerKeyword" placeholder="昵称/手机号/收件人" style="width: 180px" allow-clear />
        </a-form-item>
        <a-form-item label="地区">
          <a-input v-model:value="filters.regionKeyword" placeholder="收货地区关键词" style="width: 180px" allow-clear />
        </a-form-item>
        <a-form-item label="版权文件">
          <a-select v-model:value="filters.hasCopyrightFile" style="width: 120px">
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="true">有</a-select-option>
            <a-select-option value="false">无</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="加急">
          <a-select v-model:value="filters.urgent" style="width: 120px">
            <a-select-option value="">全部</a-select-option>
            <a-select-option value="true">是</a-select-option>
            <a-select-option value="false">否</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="下单时间">
          <a-range-picker
            v-model:value="filters.createdRange"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
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
            <a-tag v-if="record.urgent" color="red" style="margin-left: 6px">加急</a-tag>
          </template>
          <template v-else-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button size="small" type="link" @click.stop="openDetail(record)">详情</a-button>
              <a-dropdown v-if="record.status !== 'COMPLETED' && record.status !== 'CANCELLED' && record.status !== 'AFTER_SALE'">
                <a-button size="small" type="link" @click.stop>更新状态 ▾</a-button>
                <template #overlay>
                  <a-menu @click="(info: any) => onStatusChange(record.id, info.key)">
                    <a-menu-item v-if="record.status === 'SHIPPED'" key="COMPLETED">标记已完成</a-menu-item>
                    <a-menu-item v-if="record.status === 'COMPLETED'" key="AFTER_SALE">标记售后中</a-menu-item>
                    <a-menu-item v-if="record.status === 'REJECTED'" key="WAIT_PRODUCTION">改回待生产</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
            <div v-if="record.hasCopyrightWarning" style="margin-top: 8px; color: #faad14; font-size: 12px;">
              <span title="订单中含有版权商品但未上传授权书">⚠️ 缺版权授权书</span>
            </div>
            <div v-else-if="record.hasCopyrightIssue" style="margin-top: 8px; color: #16a34a; font-size: 12px;">
              <span>✅ 已上传版权授权书</span>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 订单详情抽屉 -->
    <a-drawer v-model:open="drawerOpen" title="订单详情" width="860">
      <template v-if="detail">
        <a-descriptions bordered :column="2" size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="订单号">#{{ detail.order.id }}</a-descriptions-item>
          <a-descriptions-item label="订单状态">
            <a-tag :color="statusColor(detail.order.status)">{{ displayStatusText(detail.order) }}</a-tag>
            <a-tag v-if="detail.order.urgent" color="red" style="margin-left: 6px">加急</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="总金额">
            <span class="price-gold" style="font-size: 18px">
              ¥{{ detail.order.totalAmount }}
              <span v-if="getOrderSetupFeeTotal(detail.items) > 0" style="font-size: 13px; color: #64748B; margin-left: 6px;">
                （含起售价¥{{ getOrderSetupFeeTotal(detail.items).toFixed(2) }}）
              </span>
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="备注">{{ detail.order.remark || '—' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatTime(detail.order.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ formatTime(detail.order.updatedAt) }}</a-descriptions-item>
          <a-descriptions-item label="审核状态">{{ reviewStatusText(detail.order.reviewStatus) }}</a-descriptions-item>
          <a-descriptions-item label="驳回原因">{{ detail.order.reviewReason || '—' }}</a-descriptions-item>
        </a-descriptions>

        <a-descriptions bordered :column="2" size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="客户昵称">{{ detail.order.userNickname || ('用户' + detail.order.userId) }}</a-descriptions-item>
          <a-descriptions-item label="公司名称">{{ detail.order.userCompanyName || '—' }}</a-descriptions-item>
          <a-descriptions-item label="客户手机号">{{ detail.order.userPhone || '—' }}</a-descriptions-item>
          <a-descriptions-item label="客户邮箱">{{ detail.order.userEmail || '—' }}</a-descriptions-item>
        </a-descriptions>

        <a-descriptions bordered :column="1" size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="收件人">{{ detail.order.receiverName || '—' }}</a-descriptions-item>
          <a-descriptions-item label="收件手机号">{{ detail.order.receiverPhone || '—' }}</a-descriptions-item>
          <a-descriptions-item label="收货地址">{{ detail.order.receiverAddress || '—' }}</a-descriptions-item>
        </a-descriptions>

        <h4 style="margin-bottom: 8px; color: var(--c-primary); font-weight: 600">📦 商品明细</h4>
        <a-table
          rowKey="id"
          :dataSource="detail.items"
          :columns="itemColumns"
          size="small"
          :pagination="false"
          :scroll="{ x: 1240 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'unitPrice'">
              <span class="price-gold">¥{{ record.unitPrice }}</span>
            </template>
            <template v-else-if="column.key === 'subtotal'">
              <span class="price-gold">¥{{ (Number(record.unitPrice) * record.quantity).toFixed(2) }}</span>
            </template>
            <template v-else-if="column.key === 'params'">
              <div v-if="extractSnapshotParams(record.paramsSnapshot, record.productId).length > 0" class="params-cell">
                <div v-for="(p, idx) in extractSnapshotParams(record.paramsSnapshot, record.productId)" :key="`${record.id}-${idx}`" class="param-line">
                  <span class="param-k">{{ p.name }}：</span>
                  <span class="param-v">{{ p.value }}</span>
                </div>
              </div>
              <span v-else style="color: #94A3B8">—</span>
            </template>
            <template v-else-if="column.key === 'printFile'">
              <a-tooltip v-if="record.printFileUrl" :title="getDisplayFileName(record, 'print')">
                <a-button size="small" type="primary" ghost @click="openFile(record.printFileUrl)">
                  印品文件：{{ getShortDisplayFileName(record, 'print') }}
                </a-button>
              </a-tooltip>
              <span v-else style="color: #94A3B8">—</span>
            </template>
            <template v-else-if="column.key === 'proofFile'">
              <a-tooltip v-if="record.proofFileUrl" :title="getDisplayFileName(record, 'proof')">
                <a-button size="small" type="default" @click="openFile(record.proofFileUrl)">
                  工艺对稿图：{{ getShortDisplayFileName(record, 'proof') }}
                </a-button>
              </a-tooltip>
              <span v-else style="color: #94A3B8">—</span>
            </template>
            <template v-else-if="column.key === 'copyright'">
              <span v-if="!record.hasCopyright" style="color: #94A3B8">不涉及</span>
              <div v-else>
                <a-tag v-if="!record.copyrightFileUrl" color="warning">缺失</a-tag>
                <a-button v-else size="small" type="dashed" @click="openFile(record.copyrightFileUrl)">
                  查看版权书
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
  type Order,
  type OrderDetail
} from '../api/order'
import { listOptions, listParameters } from '../api/parameter'

const loading = ref(false)
const orders = ref<Order[]>([])
const drawerOpen = ref(false)
const detail = ref<OrderDetail | null>(null)
const pagination = ref({ current: 1, pageSize: 15, total: 0 })
const filters = ref({
  orderId: '',
  status: '',
  customerKeyword: '',
  regionKeyword: '',
  hasCopyrightFile: '',
  urgent: '',
  createdRange: [] as string[]
})

const columns = [
  { title: '订单号', dataIndex: 'id', key: 'id', width: 80 },
  { title: '客户信息', dataIndex: 'userId', key: 'userInfo', width: 200 },
  { title: '产品快照', key: 'products', width: 260 },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '操作', key: 'actions', width: 320 }
];

const itemColumns = [
  { title: '商品', dataIndex: 'productName', width: 130 },
  { title: '数量', dataIndex: 'quantity', width: 70 },
  { title: '单价', key: 'unitPrice', width: 100 },
  { title: '小计', key: 'subtotal', width: 100 },
  { title: '参数信息', key: 'params', width: 380 },
  { title: '印品文件', key: 'printFile', width: 180 },
  { title: '工艺对稿图', key: 'proofFile', width: 180 },
  { title: '版权书', key: 'copyright', width: 120 }
]
const paramNameMap = ref<Record<string, string>>({})
const optionNameMap = ref<Record<string, string>>({})

function statusColor(s: string) {
  if (s === 'PENDING') return 'orange'
  if (s === 'WAIT_PRODUCTION') return 'gold'
  if (s === 'IN_PRODUCTION') return 'cyan'
  if (s === 'WAIT_SHIPMENT') return 'blue'
  if (s === 'SHIPPED') return 'blue'
  if (s === 'COMPLETED') return 'purple'
  if (s === 'AFTER_SALE') return 'magenta'
  if (s === 'REJECTED') return 'red'
  if (s === 'CANCELLED') return 'default'
  return 'blue'
}

function statusText(s: string) {
  const map: Record<string, string> = {
    PENDING: '待支付',
    WAIT_PRODUCTION: '待生产',
    IN_PRODUCTION: '生产中',
    WAIT_SHIPMENT: '待发货',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    AFTER_SALE: '售后中',
    REJECTED: '已驳回',
    CANCELLED: '已取消'
  }
  return map[s] || s
}

function productionStatusText(s?: string | null) {
  const map: Record<string, string> = {
    PREPRESS_CHECK: '印前检查',
    PLATE_MAKING: '制版',
    PRINTING: '印刷中',
    POST_PROCESS: '印后加工',
    QC_PACKING: '质检打包'
  }
  if (!s) return '—'
  return map[s] || s
}

function shippingStatusText(s?: string | null) {
  const map: Record<string, string> = {
    IN_TRANSIT: '运输中',
    OUT_FOR_DELIVERY: '派送中',
    SIGNED: '已签收'
  }
  if (!s) return '—'
  return map[s] || s
}

function displayStatusText(order: any) {
  const main = statusText(order?.status || '')
  if (order?.status === 'IN_PRODUCTION' && order?.productionStatus) {
    return `${main} > ${productionStatusText(order.productionStatus)}`
  }
  if (order?.status === 'SHIPPED' && order?.shippingStatus) {
    return `${main} > ${shippingStatusText(order.shippingStatus)}`
  }
  return main
}

function reviewStatusText(s?: string | null) {
  const map: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回'
  }
  if (!s) return '待审核'
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

function openFile(url: string) {
  if (url) {
    const fullUrl = url.startsWith('http') ? url : 'http://localhost:8080' + url;
    window.open(fullUrl, '_blank');
  }
}

function getFilename(url: string): string {
  if (!url) return ""
  const clean = url.split('?')[0]
  const part = clean.split('/').pop() || clean
  try {
    return decodeURIComponent(part)
  } catch {
    return part
  }
}

function getSnapshotFileName(snapshot: string | null | undefined, kind: 'print' | 'proof'): string {
  if (!snapshot) return ''
  try {
    const obj = JSON.parse(snapshot)
    const key = kind === 'print' ? 'printFileName' : 'proofFileName'
    const value = obj?.[key]
    return typeof value === 'string' ? value : ''
  } catch {
    return ''
  }
}

function getDisplayFileName(item: any, kind: 'print' | 'proof'): string {
  const fromSnapshot = getSnapshotFileName(item?.paramsSnapshot, kind)
  if (fromSnapshot) return fromSnapshot
  const raw = kind === 'print' ? item?.printFileUrl : item?.proofFileUrl
  return raw ? getFilename(raw) : ''
}

function shortenFileName(name: string, max = 16): string {
  if (!name) return ''
  if (name.length <= max) return name
  const dot = name.lastIndexOf('.')
  if (dot > 0 && dot < name.length - 1) {
    const ext = name.slice(dot)
    const base = name.slice(0, dot)
    const keep = Math.max(4, max - ext.length - 1)
    return `${base.slice(0, keep)}…${ext}`
  }
  return `${name.slice(0, Math.max(4, max - 1))}…`
}

function getShortDisplayFileName(item: any, kind: 'print' | 'proof'): string {
  return shortenFileName(getDisplayFileName(item, kind), 16)
}

function normalizeOptionValue(raw: any, optionMap: Record<string, string>): string {
  if (raw == null || raw === '') return '未填写'
  const values = Array.isArray(raw) ? raw : [raw]
  const mapped = values.map((v: any) => {
    const key = String(v)
    return optionMap[key] || String(v)
  })
  return mapped.join('、')
}

function extractSnapshotParams(snapshot: string | null | undefined, productId?: number): Array<{ name: string; value: string }> {
  if (!snapshot) return []
  try {
    const obj = JSON.parse(snapshot)
    const list = Array.isArray(obj?.params) ? obj.params : []
    return list.map((p: any) => {
      const parameterId = Number(p?.parameterId || 0)
      const paramKey = productId && parameterId ? `${productId}:${parameterId}` : ''
      const name = paramNameMap.value[paramKey] || String(p?.name || p?.code || '参数')
      const optionKeyPrefix = productId && parameterId ? `${productId}:${parameterId}:` : ''
      const optionMap: Record<string, string> = {}
      if (optionKeyPrefix) {
        Object.keys(optionNameMap.value).forEach((k) => {
          if (k.startsWith(optionKeyPrefix)) {
            optionMap[k.replace(optionKeyPrefix, '')] = optionNameMap.value[k]
          }
        })
      }
      const value = normalizeOptionValue(p?.value, optionMap)
      return { name, value }
    })
  } catch {
    return []
  }
}

function parseNumberLike(raw: any): number {
  if (raw == null) return 0
  const n = Number(raw)
  return Number.isFinite(n) ? n : 0
}

function getSetupFeeFromSnapshot(snapshot: string | null | undefined): number {
  if (!snapshot) return 0
  try {
    const obj = JSON.parse(snapshot)
    return parseNumberLike(obj?.setupFee)
  } catch {
    return 0
  }
}

function getFreeSetupQuantityFromSnapshot(snapshot: string | null | undefined): number {
  if (!snapshot) return 0
  try {
    const obj = JSON.parse(snapshot)
    return Math.max(0, Math.floor(parseNumberLike(obj?.freeSetupQuantity)))
  } catch {
    return 0
  }
}

function getAppliedSetupFee(item: any): number {
  const setupFee = getSetupFeeFromSnapshot(item?.paramsSnapshot)
  if (setupFee <= 0) return 0
  const freeSetupQuantity = getFreeSetupQuantityFromSnapshot(item?.paramsSnapshot)
  if (freeSetupQuantity <= 0) return setupFee
  const qty = Math.max(0, Math.floor(parseNumberLike(item?.quantity)))
  return qty < freeSetupQuantity ? setupFee : 0
}

function getOrderSetupFeeTotal(items: any[]): number {
  if (!Array.isArray(items) || items.length === 0) return 0
  return items.reduce((sum, item) => sum + getAppliedSetupFee(item), 0)
}

function collectSnapshotParameterIds(snapshot: string | null | undefined): number[] {
  if (!snapshot) return []
  try {
    const obj = JSON.parse(snapshot)
    const list = Array.isArray(obj?.params) ? obj.params : []
    return list
      .map((p: any) => Number(p?.parameterId || 0))
      .filter((id: number) => Number.isFinite(id) && id > 0)
  } catch {
    return []
  }
}

async function preloadSnapshotMeta(orderDetail: OrderDetail) {
  const productMap = new Map<number, Set<number>>()
  for (const item of orderDetail.items || []) {
    const productId = Number(item.productId || 0)
    if (!productId) continue
    const parameterIds = collectSnapshotParameterIds(item.paramsSnapshot)
    if (parameterIds.length === 0) continue
    if (!productMap.has(productId)) productMap.set(productId, new Set<number>())
    const set = productMap.get(productId)!
    parameterIds.forEach((id) => set.add(id))
  }

  for (const [productId, paramIdSet] of productMap.entries()) {
    try {
      const params = await listParameters(productId)
      for (const param of params) {
        if (!paramIdSet.has(param.id)) continue
        paramNameMap.value[`${productId}:${param.id}`] = param.paramName
        try {
          const options = await listOptions(param.id)
          for (const opt of options) {
            optionNameMap.value[`${productId}:${param.id}:${opt.id}`] = opt.optionName
          }
        } catch {
          // ignore option meta loading failure, fallback to raw value
        }
      }
    } catch {
      // ignore parameter meta loading failure, fallback to snapshot fields
    }
  }
}

function buildFiltersPayload() {
  const payload: Record<string, any> = {}
  const orderIdText = String(filters.value.orderId || '').trim()
  if (orderIdText) {
    const n = Number(orderIdText)
    if (Number.isFinite(n) && n > 0) payload.orderId = n
  }
  if (filters.value.status) payload.status = filters.value.status
  if (filters.value.customerKeyword.trim()) payload.customerKeyword = filters.value.customerKeyword.trim()
  if (filters.value.regionKeyword.trim()) payload.regionKeyword = filters.value.regionKeyword.trim()
  if (filters.value.hasCopyrightFile === 'true') payload.hasCopyrightFile = true
  if (filters.value.hasCopyrightFile === 'false') payload.hasCopyrightFile = false
  if (filters.value.urgent === 'true') payload.urgent = true
  if (filters.value.urgent === 'false') payload.urgent = false
  if (Array.isArray(filters.value.createdRange) && filters.value.createdRange.length === 2) {
    payload.createdFrom = filters.value.createdRange[0]
    payload.createdTo = filters.value.createdRange[1]
  }
  return payload
}

async function load(page = pagination.value.current) {
  loading.value = true
  try {
    const res = await listAdminOrders(page, pagination.value.pageSize, buildFiltersPayload())
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
  filters.value = {
    orderId: '',
    status: '',
    customerKeyword: '',
    regionKeyword: '',
    hasCopyrightFile: '',
    urgent: '',
    createdRange: []
  }
  load(1)
}

async function openDetail(order: Order) {
  try {
    const orderDetail = await getAdminOrderDetail(order.id)
    detail.value = orderDetail
    await preloadSnapshotMeta(orderDetail)
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

onMounted(() => load())
</script>

<style scoped>
.stats-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
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
.params-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.param-line {
  line-height: 1.4;
  font-size: 12px;
}
.param-k {
  color: #64748B;
}
.param-v {
  color: #1E293B;
}
</style>
