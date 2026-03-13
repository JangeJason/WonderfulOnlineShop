<template>
  <a-card>
    <template #title>
      <span style="font-size: 15px">报价演示</span>
    </template>

    <a-form layout="inline" style="margin-bottom: 16px; flex-wrap: wrap; gap: 8px 0">
      <a-form-item label="产品">
        <a-select v-model:value="form.productId" style="width: 220px" :options="productOptions" @change="onProductChange" />
      </a-form-item>

      <a-form-item label="数量">
        <a-input-number v-model:value="form.quantity" :min="1" />
      </a-form-item>

      <!-- 动态渲染参数 -->
      <template v-for="param in params" :key="param.id">
        <a-form-item :label="param.paramName" v-if="param.paramType === 'INPUT'">
          <a-input-number v-model:value="paramValues[param.id]" :min="0" placeholder="请输入" />
        </a-form-item>
        <a-form-item :label="param.paramName" v-else-if="param.paramType === 'SELECT'">
          <a-select
            v-model:value="paramSelections[param.id]"
            mode="multiple"
            style="min-width: 150px"
            placeholder="请选择"
            :options="(optionsMap[param.id] || []).map(o => ({ label: o.optionName, value: o.id }))"
          />
        </a-form-item>
      </template>

      <a-button type="primary" :loading="loading" @click="onQuote">获取报价</a-button>
    </a-form>

    <!-- 当前公式展示 -->
    <div v-if="currentFormula" class="formula-display">
      📐 当前公式：<code>{{ currentFormula }}</code>
    </div>

    <div v-if="result" class="quote-result">
      <div class="result-grid">
        <div class="result-item">
          <div class="result-label">单价</div>
          <div class="result-value price-gold">¥{{ result.unitPrice }}</div>
        </div>
        <div class="result-item">
          <div class="result-label">总价</div>
          <div class="result-value price-gold" style="font-size: 28px">¥{{ result.totalPrice }}</div>
        </div>
      </div>
      <div v-if="result.breakdown" class="result-breakdown">
        <div class="breakdown-label">价格明细</div>
        <pre class="breakdown-text">{{ result.breakdown }}</pre>
      </div>
    </div>

    <div v-else class="quote-empty">请填写参数后点击「获取报价」</div>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listPublicProductsPaged, type Product } from '../api/product'
import { quote, type QuoteRequest, type QuoteResponse } from '../api/quote'
import { listParameters, listOptions, type ProductParameter, type ParameterOption } from '../api/parameter'

const loading = ref(false)
const products = ref<Product[]>([])
const result = ref<QuoteResponse | null>(null)
const params = ref<ProductParameter[]>([])
const paramValues = reactive<Record<number, number>>({})
const paramSelections = reactive<Record<number, number[]>>({})
const optionsMap = ref<Record<number, ParameterOption[]>>({})
const currentFormula = ref('')

const form = reactive({
  productId: null as number | null,
  quantity: 1
})

const productOptions = computed(() =>
  products.value.map((p) => ({ label: `${p.id} - ${p.name}`, value: p.id }))
)

async function onProductChange(productId: number) {
  result.value = null
  Object.keys(paramValues).forEach(k => delete paramValues[Number(k)])
  Object.keys(paramSelections).forEach(k => delete paramSelections[Number(k)])
  optionsMap.value = {}

  try {
    params.value = await listParameters(productId)
    
    for (const param of params.value) {
      if (param.paramType === 'INPUT') {
        paramValues[param.id] = 0
      } else if (param.paramType === 'SELECT') {
        paramSelections[param.id] = []
        const opts = await listOptions(param.id)
        optionsMap.value[param.id] = opts
      }
    }

    const p = products.value.find(x => x.id === productId)
    currentFormula.value = p?.pricingFormula || ''
  } catch (e: any) {
    message.error(e?.message || '加载参数失败')
  }
}

async function onQuote() {
  if (!form.productId) {
    message.warning('请先选择产品')
    return
  }
  loading.value = true
  try {
    const items = params.value.map(param => {
      if (param.paramType === 'INPUT') {
        return {
          parameterId: param.id,
          paramCode: param.paramName,
          valueNumber: paramValues[param.id] || 0,
          selectedOptionIds: []
        }
      } else {
        return {
          parameterId: param.id,
          paramCode: param.paramName,
          selectedOptionIds: paramSelections[param.id] || []
        }
      }
    })

    const req: QuoteRequest = {
      productId: form.productId,
      quantity: form.quantity,
      items
    }

    result.value = await quote(req)
  } catch (e: any) {
    message.error(e?.message || '报价失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const pageResult = await listPublicProductsPaged({ size: 100 })
    products.value = pageResult.records
    if (products.value.length > 0) {
      form.productId = products.value[0].id
      await onProductChange(form.productId)
    }
  } catch (e: any) {
    message.error(e?.message || '加载产品失败')
  }
})
</script>

<style scoped>
.formula-display {
  margin-bottom: 16px;
  padding: 10px 16px;
  background: var(--c-primary-surface);
  border-radius: var(--radius-sm);
  font-size: 13px;
  border-left: 3px solid var(--c-primary);
}
.formula-display code {
  color: var(--c-primary);
  font-weight: 500;
}

.quote-result {
  background: var(--c-accent-surface);
  border: 1px solid rgba(200, 150, 62, 0.2);
  border-radius: var(--radius-md);
  padding: 24px;
}
.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 16px;
}
.result-item {
  text-align: center;
}
.result-label {
  font-size: 13px;
  color: var(--c-text-secondary);
  margin-bottom: 4px;
}
.result-value {
  font-size: 22px;
}
.result-breakdown {
  border-top: 1px solid rgba(200, 150, 62, 0.15);
  padding-top: 16px;
}
.breakdown-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text-secondary);
  margin-bottom: 8px;
}
.breakdown-text {
  margin: 0;
  font-size: 13px;
  color: var(--c-text-secondary);
  white-space: pre-wrap;
}

.quote-empty {
  color: var(--c-text-muted);
  padding: 24px;
  text-align: center;
}
</style>
