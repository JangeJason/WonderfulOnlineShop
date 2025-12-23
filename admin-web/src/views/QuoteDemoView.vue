<template>
  <a-card title="Quote Demo">
    <a-form layout="inline" style="margin-bottom: 12px" @finish="onQuote">
      <a-form-item label="Product">
        <a-select v-model:value="form.productId" style="width: 220px" :options="productOptions" />
      </a-form-item>

      <a-form-item label="Qty">
        <a-input-number v-model:value="form.quantity" :min="1" />
      </a-form-item>

      <a-form-item label="L">
        <a-input-number v-model:value="form.L" :min="0" />
      </a-form-item>

      <a-form-item label="W">
        <a-input-number v-model:value="form.W" :min="0" />
      </a-form-item>

      <a-form-item label="Option Count">
        <a-input-number v-model:value="form.optionCount" :min="0" />
      </a-form-item>

      <a-button type="primary" html-type="submit" :loading="loading">Quote</a-button>
    </a-form>

    <a-descriptions v-if="result" bordered size="small" :column="1">
      <a-descriptions-item label="Unit Price">{{ result.unitPrice }}</a-descriptions-item>
      <a-descriptions-item label="Total Price">{{ result.totalPrice }}</a-descriptions-item>
      <a-descriptions-item label="Breakdown">
        <pre style="margin: 0">{{ result.breakdown }}</pre>
      </a-descriptions-item>
    </a-descriptions>

    <div v-else style="color: rgba(0,0,0,.45)">Fill the form and click Quote.</div>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listPublicProducts, type Product } from '../api/product'
import { quote, type QuoteRequest, type QuoteResponse } from '../api/quote'

const loading = ref(false)
const products = ref<Product[]>([])
const result = ref<QuoteResponse | null>(null)

const form = reactive({
  productId: 1,
  quantity: 1,
  L: 1000,
  W: 800,
  optionCount: 0
})

const productOptions = computed(() =>
  products.value.map((p) => ({ label: `${p.id} - ${p.name}`, value: p.id }))
)

function buildOptionIds(n: number) {
  const ids: number[] = []
  for (let i = 1; i <= n; i++) ids.push(i)
  return ids
}

async function onQuote() {
  loading.value = true
  try {
    const req: QuoteRequest = {
      productId: form.productId,
      quantity: form.quantity,
      items: [
        { parameterId: 1, paramCode: 'L', valueNumber: form.L, selectedOptionIds: [] },
        { parameterId: 2, paramCode: 'W', valueNumber: form.W, selectedOptionIds: [] },
        { parameterId: 3, paramCode: 'OPT', selectedOptionIds: buildOptionIds(form.optionCount) }
      ]
    }

    result.value = await quote(req)
  } catch (e: any) {
    message.error(e?.message || 'Quote failed')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    products.value = await listPublicProducts()
    if (products.value.length > 0) {
      form.productId = products.value[0].id
    }
  } catch (e: any) {
    message.error(e?.message || 'Load products failed')
  }
})
</script>
