<template>
  <div class="formula-builder">
    <!-- 公式拼接区域 -->
    <div
      class="formula-area"
      @dragover.prevent="onDragOver"
      @dragleave="onDragLeave"
      @drop="onDrop"
      :class="{ 'drag-over': isDragOver }"
    >
      <div v-if="blocks.length === 0" class="formula-placeholder">
        将下方方块拖到此处拼接公式
      </div>
      <div
        v-for="(block, index) in blocks"
        :key="block.id"
        class="formula-block"
        :class="[`block-${block.type}`]"
        :style="{ backgroundColor: block.color }"
        draggable="true"
        @dragstart="onBlockDragStart(index, $event)"
        @dragover.prevent.stop="onBlockDragOver(index, $event)"
        @drop.stop="onBlockDrop(index, $event)"
      >
        <span v-if="block.type !== 'number'" class="block-label">{{ block.display }}</span>
        <input
          v-else
          class="number-input"
          type="number"
          v-model="block.value"
          @input="emitFormula"
          placeholder="0"
          step="any"
        />
        <span class="block-remove" @click="removeBlock(index)">×</span>
      </div>
    </div>

    <!-- 方块面板 -->
    <div class="block-palette">
      <!-- 参数方块 -->
      <div class="palette-section">
        <div class="palette-label">📐 参数</div>
        <div class="palette-blocks">
          <div
            v-for="p in params"
            :key="'param-' + p.id"
            class="palette-block block-param"
            draggable="true"
            @dragstart="onPaletteDragStart('param', '#' + p.paramName, '#' + p.paramName, $event)"
          >
            #{{ p.paramName }}
          </div>
          <div v-if="params.length === 0" class="palette-empty">暂无参数，请先添加</div>
        </div>
      </div>

      <!-- 运算符方块 -->
      <div class="palette-section">
        <div class="palette-label">➕ 运算符</div>
        <div class="palette-blocks">
          <div
            v-for="op in operators"
            :key="'op-' + op"
            class="palette-block block-operator"
            draggable="true"
            @dragstart="onPaletteDragStart('operator', op, op, $event)"
          >
            {{ op }}
          </div>
        </div>
      </div>

      <!-- 数字方块 -->
      <div class="palette-section">
        <div class="palette-label">🔢 数字</div>
        <div class="palette-blocks">
          <div
            class="palette-block block-number"
            draggable="true"
            @dragstart="onPaletteDragStart('number', '', '数字', $event)"
          >
            输入数字
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { ProductParameter } from '../api/parameter'

interface FormulaBlock {
  id: string
  type: 'param' | 'operator' | 'number'
  value: string
  display: string
  color: string
}

const props = defineProps<{
  modelValue: string
  params: ProductParameter[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const operators = ['+', '-', '*', '/', '(', ')']

const colorMap: Record<string, string> = {
  param: '#004178',
  operator: '#6B7A8D',
  number: '#C8963E'
}

const blocks = ref<FormulaBlock[]>([])
const isDragOver = ref(false)

// Drag state for reordering
let dragSourceIndex: number | null = null
let dragFromPalette = false

function makeId(): string {
  return Math.random().toString(36).substring(2, 10)
}

function createBlock(type: 'param' | 'operator' | 'number', value: string, display: string): FormulaBlock {
  return {
    id: makeId(),
    type,
    value,
    display,
    color: colorMap[type]
  }
}

// --- Palette drag ---
function onPaletteDragStart(type: 'param' | 'operator' | 'number', value: string, display: string, e: DragEvent) {
  dragFromPalette = true
  dragSourceIndex = null
  e.dataTransfer!.effectAllowed = 'copy'
  e.dataTransfer!.setData('application/json', JSON.stringify({ type, value, display }))
}

// --- Formula area drag ---
function onBlockDragStart(index: number, e: DragEvent) {
  dragFromPalette = false
  dragSourceIndex = index
  e.dataTransfer!.effectAllowed = 'move'
  e.dataTransfer!.setData('text/plain', String(index))
}

function onDragOver(e: DragEvent) {
  isDragOver.value = true
  e.dataTransfer!.dropEffect = dragFromPalette ? 'copy' : 'move'
}

function onDragLeave() {
  isDragOver.value = false
}

function onDrop(e: DragEvent) {
  isDragOver.value = false

  if (dragFromPalette) {
    try {
      const data = JSON.parse(e.dataTransfer!.getData('application/json'))
      blocks.value.push(createBlock(data.type, data.value, data.display))
      emitFormula()
    } catch { /* ignore */ }
  }
  // If reordering and dropped on empty area, do nothing special
  dragSourceIndex = null
}

function onBlockDragOver(index: number, e: DragEvent) {
  e.dataTransfer!.dropEffect = dragFromPalette ? 'copy' : 'move'
}

function onBlockDrop(index: number, e: DragEvent) {
  isDragOver.value = false

  if (dragFromPalette) {
    // Insert from palette at position
    try {
      const data = JSON.parse(e.dataTransfer!.getData('application/json'))
      blocks.value.splice(index, 0, createBlock(data.type, data.value, data.display))
      emitFormula()
    } catch { /* ignore */ }
  } else if (dragSourceIndex !== null && dragSourceIndex !== index) {
    // Reorder
    const [moved] = blocks.value.splice(dragSourceIndex, 1)
    blocks.value.splice(index > dragSourceIndex ? index - 1 : index, 0, moved)
    emitFormula()
  }
  dragSourceIndex = null
}

function removeBlock(index: number) {
  blocks.value.splice(index, 1)
  emitFormula()
}

// --- Formula <-> blocks conversion ---
let internalUpdate = false

function emitFormula() {
  internalUpdate = true
  const parts = blocks.value.map(b => {
    if (b.type === 'number') return b.value || '0'
    return b.value
  })
  emit('update:modelValue', parts.join(' '))
}

function parseFormula(formula: string) {
  if (!formula || !formula.trim()) {
    blocks.value = []
    return
  }

  const result: FormulaBlock[] = []
  // Tokenize: match #param, operators, or numbers
  const tokenRegex = /#[\u4e00-\u9fa5\w]+|[+\-*/()]+|\d+(?:\.\d+)?/g
  const tokens = formula.match(tokenRegex)

  if (!tokens) {
    blocks.value = []
    return
  }

  for (const token of tokens) {
    if (token.startsWith('#')) {
      result.push(createBlock('param', token, token))
    } else if (['+', '-', '*', '/', '(', ')'].includes(token)) {
      result.push(createBlock('operator', token, token))
    } else if (/^\d+(?:\.\d+)?$/.test(token)) {
      result.push(createBlock('number', token, '数字'))
    } else {
      // Handle multi-char operator sequences like "(("
      for (const char of token) {
        if (['+', '-', '*', '/', '(', ')'].includes(char)) {
          result.push(createBlock('operator', char, char))
        }
      }
    }
  }

  blocks.value = result
}

// Initial parse of modelValue
onMounted(() => {
  parseFormula(props.modelValue)
})

// Watch external changes to modelValue (e.g., from server load)
watch(() => props.modelValue, (newVal) => {
  if (internalUpdate) {
    internalUpdate = false
    return
  }
  parseFormula(newVal)
})
</script>

<style scoped>
.formula-builder {
  width: 100%;
}

/* ---- Formula area ---- */
.formula-area {
  min-height: 60px;
  border: 2px dashed var(--c-border);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  background: var(--c-surface);
  transition: all 0.3s ease;
  margin-bottom: 16px;
}

.formula-area.drag-over {
  border-color: var(--c-primary);
  background: var(--c-primary-surface);
  box-shadow: inset 0 0 0 2px rgba(0, 65, 120, 0.08);
}

.formula-placeholder {
  color: var(--c-text-muted);
  font-size: 14px;
  user-select: none;
  width: 100%;
  text-align: center;
  padding: 8px 0;
}

/* ---- Blocks in formula area ---- */
.formula-block {
  display: inline-flex;
  align-items: center;
  height: 36px;
  padding: 0 12px;
  border-radius: var(--radius-sm);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: grab;
  user-select: none;
  position: relative;
  gap: 6px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  font-family: 'DM Sans', sans-serif;
}

.formula-block:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
}

.formula-block:active {
  cursor: grabbing;
}

.block-label {
  pointer-events: none;
}

.number-input {
  width: 60px;
  border: none;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
  padding: 2px 4px;
  outline: none;
  font-family: 'DM Sans', sans-serif;
}

.number-input::placeholder {
  color: rgba(255, 255, 255, 0.6);
}

.number-input:focus {
  background: rgba(255, 255, 255, 0.4);
}

/* Remove spinner for number input */
.number-input::-webkit-outer-spin-button,
.number-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.number-input[type="number"] {
  -moz-appearance: textfield;
}

.block-remove {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  line-height: 1;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.2);
  cursor: pointer;
  margin-left: 2px;
  transition: background 0.2s;
}

.block-remove:hover {
  background: rgba(229, 62, 62, 0.7);
}

/* ---- Palette ---- */
.block-palette {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.palette-section {
  flex: 1;
  min-width: 160px;
}

.palette-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text-secondary);
  margin-bottom: 10px;
  letter-spacing: 0.02em;
}

.palette-blocks {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.palette-block {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 14px;
  border-radius: var(--radius-sm);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: grab;
  user-select: none;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  font-family: 'DM Sans', 'Noto Sans SC', sans-serif;
}

.palette-block:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
}

.palette-block:active {
  cursor: grabbing;
  transform: translateY(0);
}

.palette-block.block-param {
  background: var(--c-primary);
}

.palette-block.block-operator {
  background: var(--c-text-secondary);
  min-width: 38px;
}

.palette-block.block-number {
  background: var(--c-accent);
}

.palette-empty {
  color: var(--c-text-muted);
  font-size: 12px;
}
</style>
