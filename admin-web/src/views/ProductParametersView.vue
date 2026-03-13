<template>
  <div>
    <!-- 计价公式（产品级） -->
    <a-card title="💰 计价公式" style="margin-bottom: 16px">
      <a-form layout="vertical">
        <a-form-item label="拖拽方块拼接公式">
          <FormulaBlockBuilder v-model="pricingFormula" :params="params" />
        </a-form-item>
        <a-button type="primary" :loading="savingFormula" @click="saveFormula">保存公式</a-button>
      </a-form>
    </a-card>

    <!-- 参数列表 -->
    <a-card title="参数配置">
      <template #extra>
        <a-button @click="router.back()" style="margin-right: 8px">返回</a-button>
        <a-button @click="openMaterialRules" style="margin-right: 8px">材质规则</a-button>
        <a-button type="primary" @click="openCreate">新增参数</a-button>
      </template>

      <a-table rowKey="id" :dataSource="params" :columns="columns" :loading="loading" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="record.paramType === 'INPUT' ? 'blue' : 'green'">
              {{ record.paramType === 'INPUT' ? '输入型' : '选择型' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'required'">
            <a-tag :color="record.isRequired ? 'orange' : 'default'">{{ record.isRequired ? '必填' : '可选' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'multiple'">
            <a-tag v-if="record.paramType === 'SELECT'" :color="record.isMultiple ? 'blue' : 'default'">
              {{ record.isMultiple ? '多选' : '单选' }}
            </a-tag>
            <span v-else style="color:#999">-</span>
          </template>
          <template v-else-if="column.key === 'dynamicByMaterial'">
            <a-tag :color="record.isDynamicByMaterial ? 'purple' : 'default'">
              {{ record.isDynamicByMaterial ? '动态' : '固定' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'rules'">
            <div v-if="record.paramType === 'INPUT' && record.validationRules">
               范围：{{ record.validationRules.min }} ~ {{ record.validationRules.max }}
            </div>
            <div v-if="record.paramType === 'SELECT'">
               <span style="color: #999">点击「选项」管理</span>
            </div>
          </template>
          <template v-else-if="column.key === 'sortOrder'">
            <div style="display: flex; align-items: center; gap: 8px">
              <span>{{ record.sortOrder }}</span>
              <div style="display: flex; flex-direction: column; gap: 2px">
                <a-button type="text" size="small" style="padding: 0; height: 16px; line-height: 1" title="上移" @click="changeSort(record, -1)">⬆️</a-button>
                <a-button type="text" size="small" style="padding: 0; height: 16px; line-height: 1" title="下移" @click="changeSort(record, 1)">⬇️</a-button>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a @click="openEdit(record)" style="margin-right: 8px">编辑</a>
            <a v-if="record.paramType === 'SELECT'" @click="openOptions(record)" style="margin-right: 8px">选项</a>
            <a style="color: red" @click="doDelete(record)">删除</a>
          </template>
        </template>
      </a-table>

      <!-- 参数编辑弹窗 -->
      <a-modal v-model:open="modalOpen" :title="editingId ? '编辑参数' : '新增参数'" @ok="onSave" :confirmLoading="saving" okText="保存" cancelText="取消">
        <a-form layout="vertical">
          <a-form-item label="参数名称" required>
            <a-input v-model:value="form.paramName" placeholder="例如：长度、宽度、材质" />
          </a-form-item>
          <a-form-item label="参数类型" required>
            <a-select v-model:value="form.paramType">
              <a-select-option value="INPUT">输入型（数字/文本）</a-select-option>
              <a-select-option value="SELECT">选择型（下拉选项）</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="单位" v-if="form.paramType === 'INPUT'">
            <a-input v-model:value="form.unit" placeholder="例如：m, kg, mm (选填)" />
          </a-form-item>
          <a-form-item label="是否必填">
            <a-switch v-model:checked="form.isRequired" />
          </a-form-item>
          <a-form-item label="是否多选" v-if="form.paramType === 'SELECT'">
            <a-switch v-model:checked="form.isMultiple" />
          </a-form-item>
          <a-form-item label="按材质动态" v-if="form.paramName !== '材质'">
            <a-switch v-model:checked="form.isDynamicByMaterial" />
          </a-form-item>
          <a-form-item label="排序" v-if="editingId">
            <a-input-number v-model:value="form.sortOrder" />
          </a-form-item>
          
          <!-- 输入型校验规则 -->
          <div v-if="form.paramType === 'INPUT'" style="border: 1px dashed #d9d9d9; padding: 12px; border-radius: 6px">
            <p style="font-weight: 500; margin-bottom: 8px">📏 校验规则</p>
            <a-row :gutter="8">
              <a-col :span="12">
                <a-form-item label="最小值">
                  <a-input-number v-model:value="form.validationRules.min" style="width: 100%" placeholder="例如 500" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="最大值">
                  <a-input-number v-model:value="form.validationRules.max" style="width: 100%" placeholder="例如 3000" />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </a-form>
      </a-modal>

      <!-- 选项管理抽屉 -->
      <a-drawer v-model:open="drawerOpen" title="管理选项" width="500">
        <a-button type="primary" size="small" style="margin-bottom: 12px" @click="addOption">新增选项</a-button>
        <a-table rowKey="id" :dataSource="options" :columns="optColumns" size="small" :pagination="false">
           <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'imageUrl'">
                  <a-image v-if="record.imageUrl" :src="record.imageUrl" :width="40" :height="40" style="border-radius: 4px; object-fit: cover; border: 1px solid #d9d9d9;" />
                  <span v-else style="color: #ccc;">-</span>
               </template>
              <template v-else-if="column.key === 'actions'">
                 <a @click="editOption(record)" style="margin-right: 8px">编辑</a>
                 <a style="color: red" @click="delOption(record)">删除</a>
              </template>
           </template>
        </a-table>
        
        <a-modal v-model:open="optModalOpen" :title="optId ? '编辑选项' : '新增选项'" @ok="saveOption" okText="保存" cancelText="取消">
            <a-form layout="vertical">
               <a-form-item label="选项名称"><a-input v-model:value="optForm.optionName" placeholder="例如：橡木、松木"/></a-form-item>
               <a-form-item label="价格调整（+加价 / -减价）"><a-input-number v-model:value="optForm.priceAdjustment" style="width: 100%" placeholder="例如 500"/></a-form-item>
               
               <a-form-item label="选项图片 (可选)">
                 <a-upload
                   name="file"
                   :showUploadList="false"
                   :action="uploadUrl"
                   :headers="uploadHeaders"
                   @change="handleImageUpload"
                 >
                   <div v-if="optForm.imageUrl" class="uploaded-image-preview">
                     <img :src="optForm.imageUrl" alt="Preview" style="width: 80px; height: 80px; object-fit: cover; border-radius: 8px; border: 1px solid #d9d9d9;" />
                     <div style="margin-top: 8px;"><a-button size="small">更换图片</a-button></div>
                   </div>
                   <div v-else>
                     <a-button>
                       <upload-outlined></upload-outlined>
                       上传图片
                     </a-button>
                   </div>
                 </a-upload>
               </a-form-item>
               
               <a-form-item label="排序"><a-input-number v-model:value="optForm.sortOrder" style="width: 100%"/></a-form-item>
            </a-form>
        </a-modal>
      </a-drawer>

      <a-drawer v-model:open="materialRuleDrawerOpen" title="材质动态规则" width="760">
        <a-form layout="vertical">
          <a-form-item label="材质选项">
            <div style="display:flex; gap:8px; align-items:center;">
              <a-select v-model:value="selectedMaterialOptionId" placeholder="请选择材质" style="flex:1;" @change="onMaterialOptionChange">
                <a-select-option v-for="opt in materialOptions" :key="opt.id" :value="opt.id">{{ opt.optionName }}</a-select-option>
              </a-select>
              <a-button @click="openCopyMaterialRules" :disabled="!selectedMaterialOptionId">复制到其他材质</a-button>
            </div>
          </a-form-item>
        </a-form>

        <a-table
          rowKey="id"
          :dataSource="dynamicParams"
          :loading="materialRuleLoading"
          :pagination="false"
          size="small"
          :columns="materialRuleColumns"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'enabled'">
              <a-switch v-model:checked="materialRuleState[record.id].enabled" />
            </template>
            <template v-else-if="column.key === 'options'">
              <a-select
                v-if="record.paramType === 'SELECT'"
                mode="multiple"
                style="width: 100%"
                :disabled="!materialRuleState[record.id].enabled"
                v-model:value="materialRuleState[record.id].optionIds"
                placeholder="不选表示该参数下全部选项可用"
              >
                <a-select-option
                  v-for="opt in (dynamicOptionMap[record.id] || [])"
                  :key="opt.id"
                  :value="opt.id"
                >
                  {{ opt.optionName }}
                </a-select-option>
              </a-select>
              <span v-else style="color:#999">输入型参数无需配置选项</span>
            </template>
          </template>
        </a-table>

        <template #footer>
          <div style="display:flex; justify-content:flex-end; gap:8px">
            <a-button @click="materialRuleDrawerOpen = false">取消</a-button>
            <a-button type="primary" :loading="savingMaterialRules" @click="saveCurrentMaterialRules">保存规则</a-button>
          </div>
        </template>
      </a-drawer>

      <a-modal
        v-model:open="copyRulesModalOpen"
        title="复制规则到其他材质"
        okText="开始复制"
        cancelText="取消"
        :confirmLoading="copyingMaterialRules"
        @ok="confirmCopyMaterialRules"
      >
        <a-form layout="vertical">
          <a-form-item label="目标材质（可多选）">
            <a-select
              mode="multiple"
              v-model:value="copyTargetMaterialOptionIds"
              placeholder="请选择要复制到的材质"
            >
              <a-select-option v-for="opt in copyTargetMaterialOptions" :key="opt.id" :value="opt.id">
                {{ opt.optionName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-modal>

    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import { 
  listParameters, createParameter, updateParameter, deleteParameter,
  listOptions, createOption, updateOption, deleteOption, getMaterialRules, saveMaterialRules,
  type ProductParameter, type ParameterOption
} from '../api/parameter'
import { updateProduct, getAdminProduct, type Product } from '../api/product'
import FormulaBlockBuilder from './FormulaBlockBuilder.vue'

const route = useRoute()
const router = useRouter()
const productId = Number(route.params.id)

const loading = ref(false)
const params = ref<ProductParameter[]>([])

// --- 产品级计价公式 ---
const pricingFormula = ref('')
const savingFormula = ref(false)

async function loadProduct() {
  try {
    const p = await getAdminProduct(productId)
    if (p) {
      pricingFormula.value = p.pricingFormula || ''
    }
  } catch (e: any) {
    message.error(e.message || '加载产品信息失败')
  }
}

async function saveFormula() {
  savingFormula.value = true
  try {
    await updateProduct(productId, { pricingFormula: pricingFormula.value } as any)
    message.success('公式保存成功')
  } catch (e: any) {
    message.error(e.message || '保存公式失败')
  } finally {
    savingFormula.value = false
  }
}

// --- 参数 CRUD ---
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)

const form = reactive({
  paramName: '',
  paramType: 'INPUT' as 'INPUT' | 'SELECT',
  isRequired: true,
  isMultiple: false,
  isDynamicByMaterial: false,
  unit: null as string | null,
  sortOrder: 0,
  validationRules: { min: null, max: null } as Record<string, any>
})

const columns = [
  { title: '参数名称', dataIndex: 'paramName' },
  { title: '类型', key: 'type', width: 100 },
  { title: '必填', key: 'required', width: 80 },
  { title: '选择方式', key: 'multiple', width: 90 },
  { title: '材质联动', key: 'dynamicByMaterial', width: 90 },
  { title: '排序', key: 'sortOrder', width: 100 },
  { title: '校验规则', key: 'rules' },
  { title: '操作', key: 'actions', width: 200 }
]

async function load() {
  loading.value = true
  try {
    params.value = await listParameters(productId)
  } catch(e:any) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    paramName: '', paramType: 'INPUT', isRequired: true, isMultiple: false, isDynamicByMaterial: false, sortOrder: params.value.length, unit: null,
    validationRules: { min: null, max: null }
  })
  modalOpen.value = true
}

function openEdit(p: ProductParameter) {
  editingId.value = p.id
  Object.assign(form, {
    paramName: p.paramName,
    paramType: p.paramType,
    isRequired: p.isRequired,
    isMultiple: !!p.isMultiple,
    isDynamicByMaterial: !!p.isDynamicByMaterial,
    unit: p.unit,
    sortOrder: p.sortOrder,
    validationRules: p.validationRules || { min: null, max: null }
  })
  modalOpen.value = true
}

async function onSave() {
  if (!form.paramName.trim()) {
    message.error('请输入参数名称')
    return
  }
  try {
     saving.value = true
     const payload = {
       ...form,
       isMultiple: form.paramType === 'SELECT' ? !!form.isMultiple : false,
       isDynamicByMaterial: form.paramName === '材质' ? false : !!form.isDynamicByMaterial,
       productId
     }
     if (editingId.value) {
       await updateParameter(editingId.value, payload)
       message.success('更新成功')
     } else {
       await createParameter(payload)
       message.success('创建成功')
     }
     modalOpen.value = false
     load()
  } catch(e:any) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function doDelete(p: ProductParameter) {
  Modal.confirm({
    title: '确认删除？',
    content: '删除参数将同时删除其所有选项，是否继续？',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      await deleteParameter(p.id)
      message.success('删除成功')
      load()
    }
  })
}

async function changeSort(record: ProductParameter, offset: number) {
  // Find the parameter to swap with
  const sortedParams = [...params.value].sort((a, b) => a.sortOrder - b.sortOrder)
  const currentIndex = sortedParams.findIndex(p => p.id === record.id)
  if (currentIndex === -1) return

  const targetIndex = currentIndex + offset
  if (targetIndex < 0 || targetIndex >= sortedParams.length) {
    message.warning(offset < 0 ? '已在最顶部' : '已在最底部')
    return
  }

  const targetRecord = sortedParams[targetIndex]
  const currentSortOrder = record.sortOrder
  
  loading.value = true
  try {
    // Swap sort orders
    await updateParameter(record.id, { ...record, sortOrder: targetRecord.sortOrder } as any)
    await updateParameter(targetRecord.id, { ...targetRecord, sortOrder: currentSortOrder } as any)
    message.success('排序已更新')
    await load()
  } catch (e: any) {
    message.error(e.message || '更新排序失败')
  } finally {
    loading.value = false
  }
}

// --- 选项管理 ---
const drawerOpen = ref(false)
const currentParamId = ref<number | null>(null)
const options = ref<ParameterOption[]>([])
const optModalOpen = ref(false)
const optId = ref<number | null>(null)
const optForm = reactive({ optionName: '', priceAdjustment: 0, sortOrder: 0, imageUrl: '' })

// Upload dependencies
const uploadUrl = '/api/upload'
const uploadHeaders = {
  Authorization: `Bearer ${localStorage.getItem('adminToken')}`
}

function handleImageUpload(info: any) {
  if (info.file.status === 'done') {
    // Force strictly as a string, extracting 'url' from the response data object
    const url = info.file.response?.data?.url || info.file.response?.data || ''
    optForm.imageUrl = String(url)
    message.success('图片上传成功')
  } else if (info.file.status === 'error') {
    message.error(`${info.file.name} 上传失败.`)
  }
}

const optColumns = [
   { title: '选项名称', dataIndex: 'optionName' },
   { 
      title: '选项图片', 
      key: 'imageUrl',
      dataIndex: 'imageUrl'
   },
   { title: '价格调整', dataIndex: 'priceAdjustment' },
   { title: '排序', dataIndex: 'sortOrder' },
   { title: '操作', key: 'actions' }
]

const materialRuleDrawerOpen = ref(false)
const materialRuleLoading = ref(false)
const savingMaterialRules = ref(false)
const materialParameterId = ref<number | null>(null)
const materialOptions = ref<ParameterOption[]>([])
const selectedMaterialOptionId = ref<number | null>(null)
const dynamicOptionMap = reactive<Record<number, ParameterOption[]>>({})
const materialRuleState = reactive<Record<number, { enabled: boolean; optionIds: number[] }>>({})
const copyRulesModalOpen = ref(false)
const copyingMaterialRules = ref(false)
const copyTargetMaterialOptionIds = ref<number[]>([])

const materialRuleColumns = [
  { title: '参数', dataIndex: 'paramName', width: 140 },
  { title: '启用', key: 'enabled', width: 90 },
  { title: '可用选项', key: 'options' }
]

const dynamicParams = computed(() => {
  return params.value.filter((p) => p.paramName !== '材质' && !!p.isDynamicByMaterial)
})

function ensureRuleState(paramId: number) {
  if (!materialRuleState[paramId]) {
    materialRuleState[paramId] = { enabled: false, optionIds: [] }
  }
}

const copyTargetMaterialOptions = computed(() => {
  const current = selectedMaterialOptionId.value
  return materialOptions.value.filter((opt) => opt.id !== current)
})

async function openMaterialRules() {
  const materialParam = params.value.find((p) => p.paramType === 'SELECT' && p.paramName === '材质')
  if (!materialParam) {
    message.warning('请先创建“材质”选择型参数')
    return
  }
  for (const p of dynamicParams.value) {
    ensureRuleState(p.id)
  }
  materialParameterId.value = materialParam.id
  materialRuleDrawerOpen.value = true
  materialRuleLoading.value = true
  try {
    const [matOptions] = await Promise.all([
      listOptions(materialParam.id),
      loadDynamicOptions(),
    ])
    materialOptions.value = matOptions
    if (matOptions.length === 0) {
      message.warning('材质参数还没有选项，请先去添加材质选项')
      return
    }
    selectedMaterialOptionId.value = selectedMaterialOptionId.value || matOptions[0].id
    await loadCurrentMaterialRules()
  } finally {
    materialRuleLoading.value = false
  }
}

async function loadDynamicOptions() {
  const dynamicSelectParams = dynamicParams.value.filter((p) => p.paramType === 'SELECT')
  for (const p of dynamicSelectParams) {
    dynamicOptionMap[p.id] = await listOptions(p.id)
    ensureRuleState(p.id)
  }
  for (const p of dynamicParams.value) {
    ensureRuleState(p.id)
  }
}

async function onMaterialOptionChange() {
  await loadCurrentMaterialRules()
}

function buildMaterialRulePayload() {
  return dynamicParams.value.map((p) => ({
    paramId: p.id,
    enabled: !!materialRuleState[p.id]?.enabled,
    optionIds: p.paramType === 'SELECT' ? (materialRuleState[p.id]?.optionIds || []) : []
  }))
}

async function loadCurrentMaterialRules() {
  if (!selectedMaterialOptionId.value) return
  materialRuleLoading.value = true
  try {
    const data = await getMaterialRules(productId, selectedMaterialOptionId.value)
    for (const p of dynamicParams.value) {
      ensureRuleState(p.id)
      materialRuleState[p.id].enabled = false
      materialRuleState[p.id].optionIds = []
    }
    for (const r of data.rules || []) {
      if (!r || !r.paramId) continue
      ensureRuleState(r.paramId)
      materialRuleState[r.paramId].enabled = !!r.enabled
      materialRuleState[r.paramId].optionIds = Array.isArray(r.optionIds) ? r.optionIds : []
    }
  } catch (e: any) {
    message.error(e.message || '加载材质规则失败')
  } finally {
    materialRuleLoading.value = false
  }
}

async function saveCurrentMaterialRules() {
  if (!selectedMaterialOptionId.value) {
    message.warning('请先选择材质选项')
    return
  }
  savingMaterialRules.value = true
  try {
    const rules = buildMaterialRulePayload()
    await saveMaterialRules(productId, { materialOptionId: selectedMaterialOptionId.value, rules })
    message.success('材质规则已保存')
  } catch (e: any) {
    message.error(e.message || '保存材质规则失败')
  } finally {
    savingMaterialRules.value = false
  }
}

function openCopyMaterialRules() {
  copyTargetMaterialOptionIds.value = []
  copyRulesModalOpen.value = true
}

async function confirmCopyMaterialRules() {
  if (!selectedMaterialOptionId.value) {
    message.warning('请先选择来源材质')
    return
  }
  if (copyTargetMaterialOptionIds.value.length === 0) {
    message.warning('请至少选择一个目标材质')
    return
  }
  copyingMaterialRules.value = true
  try {
    const rules = buildMaterialRulePayload()
    await Promise.all(copyTargetMaterialOptionIds.value.map((targetId) =>
      saveMaterialRules(productId, { materialOptionId: targetId, rules })
    ))
    copyRulesModalOpen.value = false
    message.success(`已复制到 ${copyTargetMaterialOptionIds.value.length} 个材质`)
  } catch (e: any) {
    message.error(e.message || '复制材质规则失败')
  } finally {
    copyingMaterialRules.value = false
  }
}

async function openOptions(p: ProductParameter) {
  currentParamId.value = p.id
  drawerOpen.value = true
  loadOpts()
}

async function loadOpts() {
  if (!currentParamId.value) return
  const data = await listOptions(currentParamId.value)
  options.value = data.sort((a,b) => a.sortOrder - b.sortOrder)
}

function addOption() {
  optId.value = null
  optForm.optionName = ''
  optForm.priceAdjustment = 0
  optForm.imageUrl = ''
  optForm.sortOrder = options.value.length
  optModalOpen.value = true
}

function editOption(o: ParameterOption) {
  optId.value = o.id
  optForm.optionName = o.optionName
  optForm.priceAdjustment = Number(o.priceAdjustment)
  optForm.imageUrl = o.imageUrl || ''
  optForm.sortOrder = o.sortOrder
  optModalOpen.value = true
}

async function delOption(o: ParameterOption) {
   await deleteOption(o.id)
   message.success('删除成功')
   loadOpts()
}

async function saveOption() {
  if (!currentParamId.value) return
  if (!optForm.optionName.trim()) {
    message.error('请输入选项名称')
    return
  }
  try {
    // Explicitly destructure to avoid sending Proxy objects or weird file artifacts
    const payload = { 
      optionName: optForm.optionName,
      priceAdjustment: optForm.priceAdjustment,
      sortOrder: optForm.sortOrder,
      imageUrl: (optForm.imageUrl && typeof optForm.imageUrl === 'string' && optForm.imageUrl.trim() !== '') ? optForm.imageUrl : undefined,
      parameterId: currentParamId.value 
    }
    
    if (optId.value) {
       await updateOption(optId.value, payload)
       message.success('更新成功')
    } else {
       await createOption(payload)
       message.success('创建成功')
    }
    optModalOpen.value = false
    loadOpts()
  } catch(e:any) {
    message.error(e.message || '保存失败')
  }
}

onMounted(() => {
  if (productId) {
    loadProduct()
    load()
  } else {
    message.error('缺少产品ID')
    router.back()
  }
})
</script>
