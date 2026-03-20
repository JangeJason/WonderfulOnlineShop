<template>
  <div>
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-label">产品总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" style="color: #10B981">{{ stats.online }}</div>
        <div class="stat-label">已上架</div>
      </div>
      <div class="stat-card">
        <div class="stat-value" style="color: #9FADBF">{{ stats.offline }}</div>
        <div class="stat-label">已下架</div>
      </div>
    </div>

    <a-card>
      <template #title>
        <div class="title-row">
          <span style="font-size: 15px">产品列表</span>
          <a-input-search
            v-model:value="searchKeywordInput"
            class="product-search"
            placeholder="搜索产品ID/名称"
            allow-clear
            @search="onSearch"
            @clear="onSearchClear"
          />
        </div>
      </template>
      <template #extra>
        <a-button type="primary" @click="openCreate">+ 新增产品</a-button>
      </template>

      <div style="margin-bottom: 16px;">
        <span style="margin-right: 8px; font-weight: bold;">大类筛选:</span>
        <a-checkable-tag
          :checked="selectedMainCategory === null"
          @change="(checked: boolean) => handleTagChange(null, checked)"
        >
          全部
        </a-checkable-tag>
        <a-checkable-tag
          v-for="cat in mainCategories"
          :key="cat.id"
          :checked="selectedMainCategory === cat.id"
          @change="(checked: boolean) => handleTagChange(cat.id, checked)"
        >
          {{ cat.name }}
        </a-checkable-tag>
      </div>

      <a-table rowKey="id" :dataSource="rows" :columns="columns" :loading="loading" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span style="font-weight: 600">{{ record.name }}</span>
          </template>
          <template v-else-if="column.key === 'category'">
            <a-tag v-if="getCategoryName(record.categoryId)" color="blue">
              {{ getCategoryName(record.categoryId) }}
            </a-tag>
            <span v-else style="color: #9FADBF">未分类</span>
          </template>
          <template v-else-if="column.key === 'basePrice'">
            <span class="price-gold">¥{{ record.basePrice }}</span>
          </template>
          <template v-else-if="column.key === 'setupFee'">
            <view v-if="record.setupFee > 0">
              <span class="price-gold" style="font-size: 13px;">¥{{ record.setupFee }}</span>
              <view style="font-size: 12px; color: #9FADBF; margin-top: 2px;">满{{ record.freeSetupQuantity || 0 }}件免</view>
            </view>
            <span v-else style="color: #9FADBF">-</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">
              {{ record.status === 1 ? '上架' : '下架' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button size="small" type="link" @click="openEdit(record)">编辑</a-button>
              <a-button size="small" type="link" @click="openParams(record)">参数配置</a-button>
              <a-button size="small" type="link" danger @click="confirmDelete(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
      <div class="table-load-state">
        <span v-if="loadingMore">正在加载更多...</span>
        <span v-else-if="hasMore">向下滚动自动加载更多</span>
        <span v-else>已加载全部 {{ totalCount }} 条</span>
      </div>

      <a-modal v-model:open="modalOpen" :title="editingId ? '编辑产品' : '新增产品'" @ok="onOk" :confirmLoading="saving">
        <a-form layout="vertical" style="margin-top: 16px">
          <a-form-item label="产品名称">
            <a-input v-model:value="form.name" placeholder="请输入产品名称" />
          </a-form-item>
          <a-form-item label="产品描述">
            <a-textarea v-model:value="form.description" placeholder="请输入产品描述" :rows="3" />
          </a-form-item>
          <a-form-item label="分类">
            <a-cascader
              v-model:value="form.categoryPath"
              :options="categoryTree"
              :fieldNames="{ label: 'name', value: 'id', children: 'children' }"
              placeholder="请选择分类"
              change-on-select
            />
          </a-form-item>
          <a-form-item label="基础价格（元）">
            <a-input-number v-model:value="form.basePrice" :min="0" style="width: 100%" />
          </a-form-item>
          <a-form-item label="起售价（元）">
            <a-input-number v-model:value="form.setupFee" :min="0" style="width: 100%" placeholder="无起售价填 0" />
          </a-form-item>
          <a-form-item label="免起售价件数">
            <a-input-number v-model:value="form.freeSetupQuantity" :min="0" :precision="0" style="width: 100%" placeholder="满多少件免起售" />
          </a-form-item>
          <a-form-item label="商品图片">
            <a-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              list-type="picture-card"
              :file-list="fileList"
              :multiple="true"
              :max-count="9"
              @change="onUploadChange"
            >
              <div v-if="fileList.length < 9">
                <span style="font-size: 24px">+</span>
                <div style="margin-top: 4px; font-size: 12px">上传图片</div>
              </div>
            </a-upload>
            <div style="font-size: 12px; color: #64748B; margin-top: 6px">最多 9 张，第一张将作为主图用于商品卡片展示</div>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="form.status">
              <a-select-option :value="1">上架</a-select-option>
              <a-select-option :value="0">下架</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-modal>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { createProduct, deleteProduct, getAdminProductStats, listAdminProducts, updateProduct, type Product } from '../api/product'
import { listCategories, type Category } from '../api/category'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const rows = ref<Product[]>([])
const categories = ref<Category[]>([])
const stats = reactive({
  total: 0,
  online: 0,
  offline: 0
})

const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  name: '',
  description: '',
  basePrice: 0,
  setupFee: 0,
  freeSetupQuantity: 0,
  status: 1,
  categoryPath: [] as number[],
  imageUrl: '',
  imageUrls: [] as string[]
})

type CategoryNode = Category & { children?: CategoryNode[] };

const categoryTree = computed(() => {
  const map = new Map<number, CategoryNode>();
  const tree: CategoryNode[] = [];
  categories.value.forEach(c => map.set(c.id, { ...c }));
  categories.value.forEach(c => {
    if (c.parentId) {
      const parent = map.get(c.parentId);
      if (parent) {
        if (!parent.children) parent.children = [];
        parent.children.push(map.get(c.id)!);
      }
    } else {
      tree.push(map.get(c.id)!);
    }
  });
  return tree;
})

// State for filtering
const selectedMainCategory = ref<number | null>(null)
const searchKeywordInput = ref('')
const searchKeyword = ref('')
const searchMode = ref(false)
const pageSize = 20
const currentPage = ref(1)
const totalPages = ref(1)
const totalCount = ref(0)
const loadingMore = ref(false)
const hasMore = computed(() => currentPage.value < totalPages.value)

// Computed properties for filtering
const mainCategories = computed(() => {
  return categories.value.filter(c => !c.parentId)
})

function handleTagChange(id: number | null, checked: boolean) {
  if (!checked) return
  searchMode.value = false
  searchKeyword.value = ''
  selectedMainCategory.value = id
  resetAndLoad()
}

function onSearch() {
  searchKeyword.value = searchKeywordInput.value.trim()
  searchMode.value = !!searchKeyword.value
  resetAndLoad()
}

function onSearchClear() {
  searchKeywordInput.value = ''
  searchKeyword.value = ''
  searchMode.value = false
  resetAndLoad()
}

const fileList = ref<any[]>([])

const uploadUrl = '/api/upload'
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('tokenValue')
  return token ? { Authorization: token } : {}
})

function onUploadChange(info: any) {
  fileList.value = info.fileList
  if (info.file.status === 'done') {
    const resp = info.file.response
    if (!resp || !resp.success) {
      message.error(resp?.message || '上传失败')
    }
  } else if (info.file.status === 'error') {
    message.error('图片上传失败')
  }
  const urls = fileList.value
    .map((f: any) => f?.url || f?.response?.data?.url)
    .filter((u: string | undefined) => !!u)
  form.imageUrls = urls
  form.imageUrl = urls[0] || ''
}

function getCategoryName(categoryId: number | null): string | null {
  if (!categoryId) return null
  const cat = categories.value.find(c => c.id === categoryId)
  if (!cat) return null
  
  if (cat.parentId) {
    const parent = categories.value.find(c => c.id === cat.parentId)
    if (parent) {
      return `${parent.name} > ${cat.name}`
    }
  }
  return cat.name
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: '产品名称', key: 'name', dataIndex: 'name' },
  { title: '分类', key: 'category', width: 120 },
  { title: '基础价格', key: 'basePrice', width: 100 },
  { title: '起售价', key: 'setupFee', width: 100 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'actions', width: 280 }
]

function openCreate() {
  editingId.value = null
  form.name = ''
  form.description = ''
  form.basePrice = 800
  form.setupFee = 0
  form.freeSetupQuantity = 0
  form.status = 1
  form.categoryPath = []
  form.imageUrl = ''
  form.imageUrls = []
  fileList.value = []
  modalOpen.value = true
}

function openEdit(p: Product) {
  editingId.value = p.id
  form.name = p.name
  form.description = p.description || ''
  form.basePrice = Number(p.basePrice)
  form.setupFee = p.setupFee ? Number(p.setupFee) : 0
  form.freeSetupQuantity = p.freeSetupQuantity || 0
  form.status = p.status
  
  // Reconstruct path array for the cascader
  const path: number[] = []
  if (p.categoryId) {
    const target = categories.value.find(c => c.id === p.categoryId)
    if (target) {
      if (target.parentId) path.push(target.parentId)
      path.push(target.id)
    }
  }
  form.categoryPath = path
  
  const imageUrls = Array.isArray(p.imageUrls) && p.imageUrls.length > 0
    ? p.imageUrls
    : (p.imageUrl ? [p.imageUrl] : [])
  form.imageUrls = [...imageUrls]
  form.imageUrl = imageUrls[0] || ''
  fileList.value = imageUrls.map((url, idx) => ({ uid: `-${idx + 1}`, name: `image-${idx + 1}`, status: 'done', url }))
  modalOpen.value = true
}

function openParams(p: Product) {
  router.push(`/products/${p.id}/parameters`)
}

function confirmDelete(p: Product) {
  Modal.confirm({
    title: '确认删除？',
    content: `即将删除产品「${p.name}」(ID: ${p.id})`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteProduct(p.id)
        message.success('删除成功')
        await load()
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    }
  })
}

async function onOk() {
  if (!form.name.trim()) {
    message.error('请输入产品名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name,
      description: form.description || undefined,
      basePrice: form.basePrice,
      setupFee: form.setupFee || 0,
      freeSetupQuantity: form.freeSetupQuantity || 0,
      status: form.status,
      // If path array has items, grab the last one as the final assigned category ID
      categoryId: form.categoryPath.length > 0 ? form.categoryPath[form.categoryPath.length - 1] : undefined,
      imageUrl: form.imageUrl || undefined,
      imageUrls: form.imageUrls.length > 0 ? form.imageUrls : undefined
    }
    if (editingId.value) {
      await updateProduct(editingId.value, payload)
      message.success('更新成功')
    } else {
      await createProduct(payload)
      message.success('创建成功')
    }
    modalOpen.value = false
    await load()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function load() {
  loading.value = true
  try {
    currentPage.value = 1
    const [pageResult, cats, statsResp] = await Promise.all([
      listAdminProducts({
        page: currentPage.value,
        size: pageSize,
        keyword: searchMode.value ? searchKeyword.value : undefined,
        mainCategoryId: searchMode.value ? undefined : selectedMainCategory.value || undefined
      }),
      listCategories(),
      getAdminProductStats()
    ])
    rows.value = pageResult.records
    totalCount.value = pageResult.total || 0
    totalPages.value = Math.max(pageResult.pages || 1, 1)
    categories.value = cats
    stats.total = statsResp.total || 0
    stats.online = statsResp.online || 0
    stats.offline = statsResp.offline || 0
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (loading.value || loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const nextPage = currentPage.value + 1
    const pageResult = await listAdminProducts({
      page: nextPage,
      size: pageSize,
      keyword: searchMode.value ? searchKeyword.value : undefined,
      mainCategoryId: searchMode.value ? undefined : selectedMainCategory.value || undefined
    })
    rows.value = [...rows.value, ...pageResult.records]
    currentPage.value = pageResult.current || nextPage
    totalPages.value = Math.max(pageResult.pages || 1, 1)
    totalCount.value = pageResult.total || totalCount.value
  } catch (e: any) {
    message.error(e?.message || '加载更多失败')
  } finally {
    loadingMore.value = false
  }
}

function resetAndLoad() {
  rows.value = []
  totalCount.value = 0
  totalPages.value = 1
  currentPage.value = 1
  load()
}

function onWindowScroll() {
  if (loading.value || loadingMore.value || !hasMore.value) return
  const nearBottom = window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 120
  if (nearBottom) {
    loadMore()
  }
}

onMounted(async () => {
  window.addEventListener('scroll', onWindowScroll, { passive: true })
  await load()
  if (rows.value.length === 0) {
    openCreate()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onWindowScroll)
})
</script>

<style scoped>
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-search {
  width: 320px;
}

.table-load-state {
  margin-top: 12px;
  color: #64748B;
  font-size: 13px;
}
</style>
