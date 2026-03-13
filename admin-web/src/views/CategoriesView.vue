<template>
  <div>
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ categories.length }}</div>
        <div class="stat-label">分类总数</div>
      </div>
    </div>

    <a-card>
      <template #title>
        <span style="font-size: 15px">分类管理</span>
      </template>
      <template #extra>
        <a-button type="primary" @click="openCreate">+ 新增分类</a-button>
      </template>

      <div style="margin-bottom: 16px;">
        <span style="margin-right: 8px; font-weight: bold;">大类筛选:</span>
        <a-checkable-tag
          :checked="selectedMainCategories.length === 0"
          @change="(checked: boolean) => handleTagChange(null, checked)"
        >
          全部
        </a-checkable-tag>
        <a-checkable-tag
          v-for="cat in mainCategories"
          :key="cat.id"
          :checked="selectedMainCategories.includes(cat.id)"
          @change="(checked: boolean) => handleTagChange(cat.id, checked)"
        >
          {{ cat.name }}
        </a-checkable-tag>
      </div>

      <a-table 
        rowKey="id" 
        :dataSource="filteredCategories" 
        :columns="columns" 
        :loading="loading"
        :pagination="{ pageSize: 20 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <span style="font-weight: 600">{{ record.name }}</span>
            <a-tag v-if="!record.parentId" color="blue" style="margin-left: 8px">一级大类</a-tag>
          </template>
          <template v-else-if="column.key === 'parent'">
            <span>{{ getParentName(record.parentId) }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button size="small" type="link" @click="openEdit(record)">编辑</a-button>
              <a-button size="small" type="link" danger @click="confirmDelete(record)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <a-modal v-model:open="modalOpen" :title="editingId ? '编辑分类' : '新增分类'" @ok="onOk" :confirmLoading="saving">
        <a-form layout="vertical" style="margin-top: 16px">
          <a-form-item label="分类名称">
            <a-input v-model:value="form.name" placeholder="请输入分类名称" />
          </a-form-item>
          <a-form-item label="父级分类（留空代表新增一级大类）">
            <a-select v-model:value="form.parentId" placeholder="选择父级大类（可选）" allowClear>
              <!-- 只能选择当前为大类（parentId为空）的分类 -->
              <a-select-option 
                v-for="c in categories.filter(x => !x.parentId && x.id !== editingId)" 
                :key="c.id" 
                :value="c.id"
              >
                {{ c.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="排序（数值越小越靠前）">
            <a-input-number v-model:value="form.sortOrder" :min="0" style="width: 100%" />
          </a-form-item>
        </a-form>
      </a-modal>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { listCategories, createCategory, updateCategory, deleteCategory, type Category } from '../api/category'

const loading = ref(false)
const saving = ref(false)
const categories = ref<Category[]>([])
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ name: '', sortOrder: 0, parentId: null as number | null })

// State for filtering
const selectedMainCategories = ref<number[]>([])

// Computed properties for filtering
const mainCategories = computed(() => {
  return categories.value.filter(c => !c.parentId)
})

const filteredCategories = computed(() => {
  if (selectedMainCategories.value.length === 0) {
    // If no main category is selected, show ALL subcategories
    return categories.value.filter(c => c.parentId)
  }
  
  // If specific main categories are selected, show ONLY their subcategories
  return categories.value.filter(c => c.parentId && selectedMainCategories.value.includes(c.parentId))
})

function handleTagChange(id: number | null, checked: boolean) {
  if (id === null) {
    if (checked) {
      selectedMainCategories.value = []
    }
  } else {
    // Multi-select logic
    const idx = selectedMainCategories.value.indexOf(id)
    if (checked && idx === -1) {
       selectedMainCategories.value.push(id)
    } else if (!checked && idx !== -1) {
       selectedMainCategories.value.splice(idx, 1)
    }
  }
}

function getParentName(parentId: number | null | undefined): string {
  if (!parentId) return '-'
  return categories.value.find(c => c.id === parentId)?.name || '-'
}

const columns = [
  { title: 'ID', dataIndex: 'id', width: 70 },
  { title: '分类名称', key: 'name', width: 200 },
  { title: '所属大类', key: 'parent', width: 150 },
  { title: '排序', dataIndex: 'sortOrder', width: 100 },
  { title: '操作', key: 'actions', width: 200 }
]

function openCreate() {
  editingId.value = null
  form.name = ''
  form.sortOrder = 0
  form.parentId = null
  modalOpen.value = true
}

function openEdit(c: Category) {
  editingId.value = c.id
  form.name = c.name
  form.sortOrder = c.sortOrder ?? 0
  form.parentId = c.parentId || null
  modalOpen.value = true
}

function confirmDelete(c: Category) {
  Modal.confirm({
    title: '确认删除？',
    content: `即将删除分类「${c.name}」`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deleteCategory(c.id)
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
    message.error('请输入分类名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, { name: form.name, sortOrder: form.sortOrder, parentId: form.parentId })
      message.success('更新成功')
    } else {
      await createCategory({ name: form.name, sortOrder: form.sortOrder, parentId: form.parentId })
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
    categories.value = await listCategories()
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.stats-row {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
</style>
