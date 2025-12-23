<template>
  <a-card title="Products">
    <template #extra>
      <a-button type="primary" @click="openCreate">New</a-button>
    </template>

    <a-table rowKey="id" :dataSource="rows" :columns="columns" :loading="loading">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'actions'">
          <a style="margin-right: 12px" @click="openEdit(record)">Edit</a>
          <a style="color: #ff4d4f" @click="confirmDelete(record)">Delete</a>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalOpen" :title="editingId ? 'Edit Product' : 'Create Product'" @ok="onOk" :confirmLoading="saving">
      <a-form layout="vertical">
        <a-form-item label="Name">
          <a-input v-model:value="form.name" />
        </a-form-item>
        <a-form-item label="Base Price">
          <a-input-number v-model:value="form.basePrice" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="Status (1=active,0=off)">
          <a-input-number v-model:value="form.status" :min="0" :max="1" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { createProduct, deleteProduct, listAdminProducts, updateProduct, type Product } from '../api/product'

const loading = ref(false)
const saving = ref(false)
const rows = ref<Product[]>([])

const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  name: '',
  basePrice: 0,
  status: 1
})

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: 'Name', dataIndex: 'name' },
  { title: 'Base Price', dataIndex: 'basePrice' },
  { title: 'Status', dataIndex: 'status', width: 100 },
  {
    title: 'Actions',
    key: 'actions',
    width: 180
  }
]

function openCreate() {
  editingId.value = null
  form.name = ''
  form.basePrice = 800
  form.status = 1
  modalOpen.value = true
}

function openEdit(p: Product) {
  editingId.value = p.id
  form.name = p.name
  form.basePrice = Number(p.basePrice)
  form.status = p.status
  modalOpen.value = true
}

function confirmDelete(p: Product) {
  Modal.confirm({
    title: 'Delete product?',
    content: `ID=${p.id} ${p.name}`,
    async onOk() {
      try {
        await deleteProduct(p.id)
        message.success('Deleted')
        await load()
      } catch (e: any) {
        message.error(e?.message || 'Delete failed')
      }
    }
  })
}

async function onOk() {
  if (!form.name.trim()) {
    message.error('Name required')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateProduct(editingId.value, { name: form.name, basePrice: form.basePrice, status: form.status })
      message.success('Updated')
    } else {
      await createProduct({ name: form.name, basePrice: form.basePrice, status: form.status })
      message.success('Created')
    }
    modalOpen.value = false
    await load()
  } catch (e: any) {
    message.error(e?.message || 'Save failed')
  } finally {
    saving.value = false
  }
}

async function load() {
  loading.value = true
  try {
    rows.value = await listAdminProducts()
  } catch (e: any) {
    message.error(e?.message || 'Load failed')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await load()
  if (rows.value.length === 0) {
    openCreate()
  }
})
</script>
