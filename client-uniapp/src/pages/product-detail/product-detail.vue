<template>
  <view class="detail-page">
    <OfficialHeader />
    <view class="page-container">
    <!-- 商品头部 -->
    <view class="product-header">
      <!-- #ifdef H5 -->
      <view class="header-meta-line">
        <view class="header-back-btn" @click="onBackTap">
          <image class="header-back-icon" src="/static/icons/right.svg" mode="aspectFit" />
        </view>
        <view class="header-favorite-btn" @click="toggleProductFavorite">
          <image class="header-favorite-icon" :src="favoriteIconSrc" mode="aspectFit" />
        </view>
        <text class="header-meta-item header-meta-name">{{ product?.name || "-" }}</text>
        <text class="header-meta-dot">·</text>
        <text class="header-meta-item">基础价: ¥{{ product?.basePrice }}</text>
        <template v-if="product?.description">
          <text class="header-meta-dot">·</text>
          <text class="header-meta-item header-meta-desc">{{ product?.description }}</text>
        </template>
      </view>
      <scroll-view class="header-gallery" scroll-x enable-flex>
        <view class="header-gallery-list">
          <image
            v-for="(img, idx) in productImages"
            :key="`img-${idx}`"
            class="header-gallery-img"
            :src="resolveAsset(img)"
            mode="aspectFill"
            @click="previewProductImage(idx)"
          />
        </view>
      </scroll-view>
      <!-- #endif -->
      <!-- #ifdef MP-WEIXIN -->
      <view class="mp-hero-wrap">
        <swiper
          v-if="productImages.length > 0"
          class="mp-hero-swiper"
          :current="currentHeroIndex"
          :indicator-dots="productImages.length > 1"
          indicator-color="rgba(255,255,255,0.45)"
          indicator-active-color="#FFFFFF"
          circular
          @change="onHeroSwiperChange"
        >
          <swiper-item v-for="(img, idx) in productImages" :key="`hero-${idx}`">
            <image
              class="mp-hero-img"
              :src="resolveAsset(img)"
              mode="aspectFill"
              @click="previewProductImage(idx)"
            />
          </swiper-item>
        </swiper>
        <view v-else class="mp-hero-empty"></view>
        <view class="mp-hero-count" v-if="productImages.length > 1">
          {{ currentHeroIndex + 1 }}/{{ productImages.length }}
        </view>
        <view class="mp-hero-left-actions">
          <view class="mp-hero-back" @click="onBackTap">
            <image class="mp-hero-back-icon" src="/static/icons/right.svg" mode="aspectFit" />
          </view>
          <view class="mp-hero-favorite" @click="toggleProductFavorite">
            <image class="mp-hero-favorite-icon" :src="favoriteIconSrc" mode="aspectFit" />
          </view>
        </view>
      </view>
      <view class="mp-product-meta">
        <view class="mp-meta-row">
          <view class="mp-meta-left">
            <view class="mp-meta-main">
              <text class="mp-meta-name">{{ product?.name || "-" }}</text>
              <text class="mp-meta-dot">·</text>
              <text class="mp-price-label">基础价</text>
              <text class="mp-meta-price">¥{{ product?.basePrice }}</text>
            </view>
            <text class="mp-meta-desc">{{ product?.description || "-" }}</text>
          </view>
          <view class="mp-meta-right">
            <button class="mp-share-btn" open-type="share">
              <image class="mp-share-icon" src="/static/icons/share.svg" mode="aspectFit" />
              <text class="mp-share-text">分享</text>
            </button>
          </view>
        </view>
      </view>
      <!-- #endif -->
    </view>

    <!-- 参数配置 -->
    <view class="section" v-if="visibleParams.length > 0">
      <view class="section-head">
        <view class="section-head-left">
          <view class="section-bar"></view>
          <text class="section-title">参数配置</text>
        </view>
        <view class="section-code-btn" @click="onApplyConfigCode">输入配置码</view>
      </view>
      <view v-for="param in visibleParams" :key="param.id" class="param-group">
        <view class="param-header">
          <text class="param-name">
            {{ param.paramName }}
            <text v-if="isParamRequired(param)" class="required-asterisk">*</text>
            <text v-if="isMultiSelectParam(param)" class="multi-hint">(可多选)</text>
          </text>
          <text v-if="param.paramType === 'INPUT' && getRange(param)" class="param-range">
            {{ getRange(param)!.min }} ~ {{ getRange(param)!.max }}
          </text>
        </view>

        <!-- INPUT -->
        <view v-if="param.paramType === 'INPUT'" class="input-wrap">
          <view class="input-container">
            <view class="input-box" :class="{ 'has-error': validationErrors[param.paramName] }">
              <input
                type="digit"
                :placeholder="getRange(param) ? `${getRange(param)!.min} ~ ${getRange(param)!.max}` : '请输入'"
                :value="formValues[param.paramName]"
                :adjust-position="true"
                @input="(e: any) => onInputChange(param, e.detail.value)"
              />
            </view>
            <text class="param-unit" v-if="param.unit">{{ param.unit }}</text>
          </view>
          <text v-if="validationErrors[param.paramName]" class="error-msg">
            {{ validationErrors[param.paramName] }}
          </text>
        </view>

        <view v-else class="options-row">
          <view
            v-for="opt in getVisibleOptions(param)"
            :key="opt.id"
            :class="['option-chip', { active: isOptionSelected(param, opt.id), 'has-img': opt.imageUrl }]"
            @click="onOptionSelect(param, opt)"
          >
            <image v-if="opt.imageUrl" :src="resolveAsset(opt.imageUrl)" mode="aspectFill" class="chip-img" @click.stop="previewOptionImage(opt.imageUrl)" />
            <view class="chip-info">
              <text class="chip-text">{{ opt.optionName }}</text>
              <text v-if="opt.priceAdjustment" class="chip-price">+¥{{ opt.priceAdjustment }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 数量 -->
    <view class="section">
      <view class="section-head">
        <view class="section-head-left">
          <view class="section-bar"></view>
          <text class="section-title">购买数量</text>
        </view>
      </view>
      <view class="qty-control">
        <view class="qty-btn" :class="{ disabled: quantity <= 1 }" @click="changeQty(-1)">
          <text class="qty-btn-text">−</text>
        </view>
        <input class="qty-num-input" type="number" v-model.number="quantity" @blur="onQtyBlur" />
        <view class="qty-btn" @click="changeQty(1)">
          <text class="qty-btn-text">＋</text>
        </view>
        <view class="calc-btn" :class="{ disabled: !quantity }" @click="onQtyBlur">
          <text>计算价格</text>
        </view>
      </view>
    </view>

    <!-- 设计文件上传 -->
    <view class="section">
      <view class="section-head">
        <view class="section-head-left">
          <view class="section-bar"></view>
          <text class="section-title">设计文件上传（可选）</text>
        </view>
      </view>
      <view class="upload-item">
        <text class="upload-label">印品文件（可选）</text>
        <view class="upload-container" @click="choosePrintFile">
          <view class="upload-box" v-if="!printFileUrl">
            <text class="upload-icon">+</text>
            <text class="upload-text">点击上传印品文件</text>
            <text class="upload-subtext">支持 .pdf .cdr .jpg .ai</text>
          </view>
          <view class="file-info-box" v-else>
            <view class="file-info-row">
              <text class="file-icon">📄</text>
              <view class="file-detail">
                <text class="file-name">{{ printFileName }}</text>
                <text class="file-status">✅ 已上传</text>
              </view>
              <view class="file-delete" @click.stop="removePrintFile">
                <text>×</text>
              </view>
            </view>
            <view class="reupload-link" @click.stop="choosePrintFile">重新选择</view>
          </view>
        </view>
      </view>

      <view class="upload-item">
        <text class="upload-label">工艺对稿图（可选）</text>
        <view class="upload-container" @click="chooseProofFile">
          <view class="upload-box" v-if="!proofFileUrl">
            <text class="upload-icon">+</text>
            <text class="upload-text">点击上传工艺对稿图</text>
            <text class="upload-subtext">支持 .pdf .cdr .jpg .ai</text>
          </view>
          <view class="file-info-box" v-else>
            <view class="file-info-row">
              <text class="file-icon">📄</text>
              <view class="file-detail">
                <text class="file-name">{{ proofFileName }}</text>
                <text class="file-status">✅ 已上传</text>
              </view>
              <view class="file-delete" @click.stop="removeProofFile">
                <text>×</text>
              </view>
            </view>
            <view class="reupload-link" @click.stop="chooseProofFile">重新选择</view>
          </view>
        </view>
      </view>
      <text class="design-upload-tip">无印品文件时，客服会与您联系进行产品设计，请留意电话</text>
    </view>

    <!-- 版权问题声明 -->
    <view class="section">
      <view class="section-head">
        <view class="section-head-left">
          <view class="section-bar"></view>
          <text class="section-title">版权相关</text>
        </view>
      </view>
      <view class="copyright-row">
        <text class="copyright-label">是否存在版权问题？</text>
        <switch :checked="hasCopyright" color="#3B82F6" @change="onCopyrightChange" />
      </view>
      <text class="copyright-hint" v-if="hasCopyright">
        由于涉及版权问题，请提供授权书。您可以先下单并在后续补传授权书，但未提供授权书将无法进入生产环节。
      </text>
      <view v-if="hasCopyright" class="copyright-actions">
        <view class="cert-select-btn" @click="openCertPopup">
          <text>{{ selectedCertificate ? "更换证书" : "选择证书" }}</text>
        </view>
        <view v-if="selectedCertificate" class="selected-cert">
          <text class="selected-cert-name">{{ selectedCertificate.certificateType }} | {{ selectedCertificate.trademarkContent }}</text>
          <text class="selected-cert-meta">{{ selectedCertificate.principal }} · 截止 {{ selectedCertificate.endDate }}</text>
        </view>
      </view>
    </view>

    <view class="cert-popup-mask" v-if="showCertPopup" @click="closeCertPopup">
      <view class="cert-popup" :class="{ desktop: isDesktopH5 }" @click.stop>
        <view class="cert-popup-head">
          <view class="cert-popup-left">
            <text v-if="certPopupMode === 'add'" class="cert-popup-back" @click="backToCertList">←</text>
            <text class="cert-popup-title">{{ certPopupMode === "list" ? "证书选择" : (editingCertificateId ? "查看/编辑证书" : "新增证书") }}</text>
          </view>
          <text class="cert-popup-close" @click="closeCertPopup">×</text>
        </view>

        <view v-if="certPopupMode === 'list'" class="cert-popup-content">
          <view v-if="commonCertificates.length === 0" class="empty-cert">无常用证书</view>
          <view v-else class="cert-list">
            <view
              class="cert-item"
              :class="{ active: selectedCertificate?.id === cert.id }"
              v-for="cert in commonCertificates"
              :key="cert.id"
              @click="selectCertificate(cert)"
            >
              <view class="cert-item-main">
                <text class="cert-item-title">{{ cert.certificateType }} · {{ cert.trademarkContent }}</text>
                <text class="cert-item-sub">{{ cert.principal }} · 截止 {{ cert.endDate }}</text>
              </view>
              <view class="cert-item-edit-btn" @click.stop="editCertificate(cert)">查看/编辑</view>
            </view>
          </view>
          <view class="cert-add-btn" @click="goAddCertificate">新增证书</view>
        </view>

        <view v-else class="cert-form">
          <view class="cert-form-item">
            <text class="cert-form-label">证书类型 *</text>
            <picker mode="selector" :range="certificateTypes" :value="certificateTypeIndex" @change="onCertificateTypeChange">
              <view class="cert-picker-box cert-picker-select">
                <text class="cert-picker-text">{{ certForm.certificateType || "请选择证书类型" }}</text>
                <text class="cert-picker-arrow">▼</text>
              </view>
            </picker>
          </view>
          <view class="cert-form-item">
            <text class="cert-form-label">商标类型</text>
            <picker mode="selector" :range="trademarkTypes" :value="trademarkTypeIndex" @change="onTrademarkTypeChange">
              <view class="cert-picker-box cert-picker-select">
                <text class="cert-picker-text">{{ certForm.trademarkType || "未知" }}</text>
                <text class="cert-picker-arrow">▼</text>
              </view>
            </picker>
          </view>
          <view class="cert-form-item">
            <text class="cert-form-label">商标/授权内容 *</text>
            <input class="cert-input-box" v-model="certForm.trademarkContent" placeholder="请输入商标或授权内容" />
          </view>
          <view class="cert-form-item">
            <text class="cert-form-label">委托方 *</text>
            <input class="cert-input-box" v-model="certForm.principal" placeholder="请输入委托方（盖章单位）名称" />
          </view>
          <view class="cert-form-item">
            <text class="cert-form-label">结束日期 *</text>
            <picker mode="date" :value="certForm.endDate" @change="onCertEndDateChange">
              <view class="cert-picker-box">{{ certForm.endDate || "请选择结束日期" }}</view>
            </picker>
          </view>
          <view class="cert-form-item">
            <text class="cert-form-label">上传证书 *</text>
            <view class="cert-upload-btn" @click="chooseCertificateFiles">
              <text>选择文件（{{ certFileList.length }}/5）</text>
            </view>
            <text class="cert-upload-hint">证书信息仅供审核使用，最多上传5张图片，每张大小不超过10M，图片格式:jpg/png/gif/pdf</text>
            <view class="cert-file-list" v-if="certFileList.length > 0">
              <view class="cert-file-row" v-for="(f, idx) in certFileList" :key="idx">
                <text class="cert-file-name">{{ f.name }}</text>
                <text class="cert-remove-btn" @click="removeCertificateFile(idx)">删除</text>
              </view>
            </view>
          </view>
          <view class="cert-submit-btn" :class="{ disabled: certSubmitting }" @click="submitCertificate">
            <text>{{ certSubmitting ? "提交中..." : "提交并保存" }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 报价结果 -->
    <view class="quote-section" v-if="quoteResult">
      <view class="quote-row">
        <text class="quote-label">单价</text>
        <text class="quote-val">¥{{ quoteResult.unitPrice }}</text>
      </view>
      
      <!-- 参数明细 -->
      <view class="quote-breakdown" v-if="quoteResult.breakdown">
        <view class="breakdown-item" v-for="row in quoteBreakdownRows" :key="row.key">
          <text class="bd-label">- {{ row.label }}</text>
          <text class="bd-val" :class="{ negative: row.value < 0 }">{{ formatSignedPrice(row.value) }}</text>
        </view>
      </view>

      <view class="quote-row" v-if="quoteResult.breakdown?.setupFee > 0">
        <text class="quote-label" style="color: #F59E0B; font-size: 13px;">含起售价（数量达到 {{ product?.freeSetupQuantity }} 免起售价）</text>
        <text class="quote-val" style="color: #F59E0B">+¥{{ quoteResult.breakdown.setupFee }}</text>
      </view>
      <view class="quote-line"></view>
      <view class="quote-row">
        <text class="quote-label">总价</text>
        <text class="quote-val total">¥{{ quoteResult.totalPrice }}</text>
      </view>
    </view>

    <!-- 底部栏 -->
    <view class="bottom-bar">
      <view class="bar-left">
        <text class="bar-hint" v-if="!quoteResult">请配置参数</text>
        <view v-else>
          <text class="bar-small">合计</text>
          <text class="bar-total">¥{{ quoteResult.totalPrice }}</text>
        </view>
      </view>
      <view class="bar-actions">
        <view class="bar-config-code-btn" @click="onGenerateConfigCode">
          <text class="bar-config-code-text">生成配置码</text>
        </view>
        <view class="bar-action" :class="{ disabled: adding }" @click="onAddToCart">
          <text class="action-text">{{ adding ? '提交中...' : (isEditingExistingItem ? '保存修改' : '加入购物车') }}</text>
        </view>
      </view>
    </view>

    <view class="add-cart-popup-mask" v-if="showAddCartPopup">
      <view class="add-cart-popup">
        <image class="add-cart-popup-icon" src="/static/icons/cart.svg" mode="aspectFit" />
        <text class="add-cart-popup-title">加入购物车成功</text>
        <view class="add-cart-popup-actions">
          <view class="popup-btn popup-btn-secondary" @click="goHomeAfterAddCart">
            <text>继续选产品</text>
          </view>
          <view class="popup-btn popup-btn-primary" @click="goCartAfterAddCart">
            <text>前往购物车</text>
          </view>
        </view>
      </view>
    </view>
    </view>
    
    <!-- #ifdef H5 -->
    <OfficialFooter />
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from "vue";
import { onLoad, onShow, onShareAppMessage, onShareTimeline } from "@dcloudio/uni-app";
import {
  getPublicProduct,
  getProductParameters,
  getParameterOptions,
  getMaterialConfig,
  type Product,
  type ProductParameter,
  type ParameterOption,
} from "../../api/product";
import { quote, type QuoteResponse } from "../../api/quote";
import { addCartItem, listCartItems, removeCartItem } from "../../api/cart";
import { getOrderDetail, updateRejectedOrderItem } from "../../api/order";
import { createCommonCertificate, listCommonCertificates, updateCommonCertificate, type UserCertificate } from "../../api/certificate";
import { createProductConfigCode, resolveProductConfigCode } from "../../api/config-code";
import { favoriteConfigCode, favoriteProduct, getProductFavoriteStatus, unfavoriteProduct } from "../../api/favorite";
import { getMe } from "../../api/auth";
import { getToken } from "../../utils/storage";
import { getApiBaseUrl, toAbsoluteAssetUrl } from "../../utils/url";

const product = ref<Product | null>(null);
const params = ref<ProductParameter[]>([]);
const paramOptions = reactive<Record<number, ParameterOption[]>>({});
const formValues = reactive<Record<string, any>>({});
const validationErrors = reactive<Record<string, string>>({});
const quantity = ref(1);
const quoteResult = ref<QuoteResponse | null>(null);
const adding = ref(false);
const printFileUrl = ref<string>('');
const printFileName = ref<string>('');
const proofFileUrl = ref<string>('');
const proofFileName = ref<string>('');
const hasCopyright = ref(false);
const copyrightFileUrl = ref<string>('');
const DESIGN_FILE_EXTENSIONS = ['pdf', 'cdr', 'jpg', 'ai'];
const CERT_ALLOWED_EXT = ["jpg", "png", "gif", "pdf"];
const showCertPopup = ref(false);
const certPopupMode = ref<"list" | "add">("list");
const commonCertificates = ref<UserCertificate[]>([]);
const selectedCertificate = ref<UserCertificate | null>(null);
const isDesktopH5 = ref(false);
const certificateTypes = ["商标注册证", "营业执照", "合同/授权证明", "印刷委托书"];
const trademarkTypes = ["未知", "普通商标", "知名商标", "著名商标", "驰名商标", "地理标志商标", "无商标"];
const certificateTypeIndex = ref(0);
const trademarkTypeIndex = ref(0);
const certSubmitting = ref(false);
type LocalCertFile = { name: string; size?: number; path?: string; url?: string };
const certFileList = ref<LocalCertFile[]>([]);
const editingCertificateId = ref<number | null>(null);
const certForm = reactive({
  certificateType: "",
  trademarkType: "未知",
  trademarkContent: "",
  principal: "",
  endDate: "",
});
const editingCartItemId = ref<number | null>(null);
const editingOrderId = ref<number | null>(null);
const editingOrderItemId = ref<number | null>(null);
const entrySource = ref<"home" | "cart" | "favorites" | "order" | "unknown">("unknown");
const shareUserName = ref("用户");
const showAddCartPopup = ref(false);
const isProductFavorited = ref(false);
const favoriteLoading = ref(false);
const favoriteConfigCodeCount = ref(0);
const materialParameterId = ref<number | null>(null);
const enabledDynamicParamIds = ref<number[]>([]);
const enabledOptionIds = ref<Record<number, number[]>>({});
const currentHeroIndex = ref(0);
const pendingConfigCode = ref<string>("");
const shouldAutoApplyConfigCode = ref(false);
const CONFIG_EXPIRE_OPTIONS = [
  { label: "1 天", days: 1 },
  { label: "7 天", days: 7 },
  { label: "14 天", days: 14 },
  { label: "30 天", days: 30 },
  { label: "90 天", days: 90 },
  { label: "永久", days: 0 },
];
const productImages = computed<string[]>(() => {
  const current = product.value;
  if (!current) return [];
  if (Array.isArray(current.imageUrls) && current.imageUrls.length > 0) {
    return current.imageUrls;
  }
  return current.imageUrl ? [current.imageUrl] : [];
});
const favoriteIconSrc = computed(() =>
  isProductFavorited.value ? "/static/icons/favourite-filled.svg" : "/static/icons/favourite.svg"
);
const isEditingExistingItem = computed(() => !!editingCartItemId.value || !!editingOrderItemId.value);

const visibleParams = computed<ProductParameter[]>(() => {
  return params.value.filter((p) => {
    if (materialParameterId.value != null && p.id === materialParameterId.value) {
      return true;
    }
    if (!p.isDynamicByMaterial) {
      return true;
    }
    return enabledDynamicParamIds.value.includes(p.id);
  });
});

const quoteBreakdownRows = computed<Array<{ key: string; label: string; value: number }>>(() => {
  const rows: Array<{ key: string; label: string; value: number }> = [];
  const breakdown = quoteResult.value?.breakdown || {};

  const basePrice = Number((breakdown as any)["基础价"] || 0);
  rows.push({ key: "base", label: "基础价", value: Number.isFinite(basePrice) ? basePrice : 0 });

  Object.entries(breakdown).forEach(([key, raw]) => {
    if (key === "基础价" || key === "setupFee") return;
    if (key.includes("计价公式")) {
      const n = Number(raw || 0);
      rows.push({ key: `formula:${key}`, label: key, value: Number.isFinite(n) ? n : 0 });
    }
  });

  for (const p of getActiveParamsForQuote()) {
    if (p.paramType !== "SELECT") continue;
    const selectedIds = getSelectedOptionIdsForParam(p);
    if (selectedIds.length === 0) continue;
    const options = paramOptions[p.id] || [];
    const selectedOptions = options.filter((opt) => selectedIds.includes(opt.id));
    const selectedNames = selectedOptions.map((opt) => opt.optionName).filter(Boolean);
    const label = selectedNames.length > 0
      ? `${p.paramName}（${selectedNames.join("，")}）`
      : p.paramName;
    const value = selectedOptions.reduce((sum, opt) => sum + Number(opt.priceAdjustment || 0), 0);
    rows.push({ key: `param:${p.id}`, label, value: Number.isFinite(value) ? value : 0 });
  }

  return rows;
});

async function choosePrintFile() {
  await chooseDesignFileAndUpload("print");
}

async function chooseProofFile() {
  await chooseDesignFileAndUpload("proof");
}

async function chooseDesignFileAndUpload(kind: "print" | "proof") {
  try {
    const selected = await pickDesignFile();
    if (!selected || !selected.path) {
      return;
    }
    const ext = getFileExtension(selected.name || selected.path);
    if (!DESIGN_FILE_EXTENSIONS.includes(ext)) {
      uni.showToast({ title: "仅支持 .pdf .cdr .jpg .ai", icon: "none" });
      return;
    }

    uni.showLoading({ title: "上传中..." });
    const baseUrl = getApiBaseUrl();
    const uploadRes: any = await new Promise((resolve, reject) => {
      uni.uploadFile({
        url: baseUrl + "/api/upload",
        filePath: selected.path,
        name: "file",
        formData: { scene: "design" },
        header: {
          Authorization: getToken(),
        },
        success: resolve,
        fail: reject,
      });
    });
    uni.hideLoading();
    if (uploadRes.statusCode === 200) {
      let data = uploadRes.data;
      if (typeof data === "string") data = JSON.parse(data);
      if (data.success) {
        if (kind === "print") {
          printFileUrl.value = data.data.url;
          printFileName.value = selected.name || getFilenameFromPath(selected.path);
        } else {
          proofFileUrl.value = data.data.url;
          proofFileName.value = selected.name || getFilenameFromPath(selected.path);
        }
        uni.showToast({ title: "上传成功", icon: "success" });
      } else {
        uni.showToast({ title: data.message || "上传失败", icon: "none" });
      }
    } else {
      uni.showToast({ title: "上传失败", icon: "none" });
    }
  } catch (e) {
    uni.hideLoading();
    console.error(e);
  }
}

function removePrintFile() {
  printFileUrl.value = "";
  printFileName.value = "";
}

function removeProofFile() {
  proofFileUrl.value = "";
  proofFileName.value = "";
}

function getFileExtension(filename: string): string {
  if (!filename || !filename.includes(".")) return "";
  return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
}

function getFilenameFromPath(path: string): string {
  return path.split("/").pop() || path.split("\\").pop() || "";
}

function getSnapshotFileName(snapshot: string | null | undefined, kind: "print" | "proof"): string {
  if (!snapshot) return "";
  try {
    const obj = JSON.parse(snapshot);
    const key = kind === "print" ? "printFileName" : "proofFileName";
    const raw = obj?.[key];
    return typeof raw === "string" ? raw : "";
  } catch {
    return "";
  }
}

function getDisplayCertificateFileName(fileName: string | undefined, index: number, url: string): string {
  if (fileName && fileName.trim()) {
    return fileName;
  }
  const raw = getFilenameFromPath(url);
  const dotIdx = raw.lastIndexOf(".");
  const ext = dotIdx >= 0 ? raw.substring(dotIdx) : "";
  const noExt = dotIdx >= 0 ? raw.substring(0, dotIdx) : raw;
  const looksLikeUuid = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$/.test(noExt);
  if (looksLikeUuid) {
    return `证书文件${index + 1}${ext}`;
  }
  return raw;
}

function getSnapshotValueForParam(snapshotParam: any, param: ProductParameter): any {
  const raw = snapshotParam?.value;
  if (param.paramType === "INPUT") {
    const n = Number(raw);
    return Number.isFinite(n) ? n : undefined;
  }
  const opts = paramOptions[param.id] || [];
  const isMulti = isMultiSelectParam(param);
  const resolveSingle = (v: any): number | undefined => {
    if (typeof v === "number") {
      return opts.some((opt) => opt.id === v) ? v : undefined;
    }
    if (typeof v === "string") {
      if (/^\d+$/.test(v)) {
        const n = Number(v);
        return opts.some((opt) => opt.id === n) ? n : undefined;
      }
      const byName = opts.find((opt) => opt.optionName === v);
      return byName?.id;
    }
    return undefined;
  };

  if (Array.isArray(raw)) {
    const ids = raw
      .map((v) => resolveSingle(v))
      .filter((v): v is number => typeof v === "number");
    if (isMulti) {
      return ids;
    }
    return ids[0];
  }

  if (isMulti && typeof raw === "string" && (raw.includes("，") || raw.includes(","))) {
    const parts = raw.split(/[，,]/).map((s) => s.trim()).filter(Boolean);
    const ids = parts
      .map((v) => resolveSingle(v))
      .filter((v): v is number => typeof v === "number");
    return ids;
  }

  const single = resolveSingle(raw);
  if (isMulti) {
    return single != null ? [single] : [];
  }
  return single;
}

async function pickDesignFile(): Promise<{ path: string; name: string } | null> {
  // #ifdef MP-WEIXIN
  const res: any = await new Promise((resolve, reject) => {
    uni.chooseMessageFile({
      count: 1,
      type: "file",
      extension: DESIGN_FILE_EXTENSIONS,
      success: resolve,
      fail: reject,
    });
  });
  const file = res?.tempFiles?.[0];
  return file ? { path: file.path || file.tempFilePath, name: file.name || "" } : null;
  // #endif

  // #ifdef H5
  const res: any = await new Promise((resolve, reject) => {
    uni.chooseFile({
      count: 1,
      extension: DESIGN_FILE_EXTENSIONS,
      success: resolve,
      fail: reject,
    });
  });
  const file = res?.tempFiles?.[0];
  return file ? { path: file.path || file.tempFilePath, name: file.name || "" } : null;
  // #endif

  // #ifndef MP-WEIXIN
  return null;
  // #endif
}

async function loadCommonCertificates() {
  try {
    commonCertificates.value = await listCommonCertificates();
    if (selectedCertificate.value) {
      const latest = commonCertificates.value.find((c) => c.id === selectedCertificate.value?.id);
      if (latest) {
        selectedCertificate.value = latest;
      }
    }
  } catch {
    commonCertificates.value = [];
  }
}

function openCertPopup() {
  certPopupMode.value = "list";
  showCertPopup.value = true;
  loadCommonCertificates();
}

function closeCertPopup() {
  showCertPopup.value = false;
  certPopupMode.value = "list";
  editingCertificateId.value = null;
}

function selectCertificate(cert: UserCertificate) {
  selectedCertificate.value = cert;
  copyrightFileUrl.value = cert.fileUrls?.[0] || "";
  showCertPopup.value = false;
}

function goAddCertificate() {
  if (isDesktopH5.value) {
    resetCertificateForm();
    certPopupMode.value = "add";
    return;
  }
  showCertPopup.value = false;
  uni.navigateTo({ url: "/pages/certificate-add/certificate-add" });
}

function backToCertList() {
  editingCertificateId.value = null;
  certPopupMode.value = "list";
}

function resetCertificateForm() {
  editingCertificateId.value = null;
  certificateTypeIndex.value = 0;
  trademarkTypeIndex.value = 0;
  certFileList.value = [];
  certForm.certificateType = "";
  certForm.trademarkType = "未知";
  certForm.trademarkContent = "";
  certForm.principal = "";
  certForm.endDate = "";
}

function editCertificate(cert: UserCertificate) {
  editingCertificateId.value = cert.id;
  certPopupMode.value = "add";
  certForm.certificateType = cert.certificateType || "";
  certForm.trademarkType = cert.trademarkType || "未知";
  certForm.trademarkContent = cert.trademarkContent || "";
  certForm.principal = cert.principal || "";
  certForm.endDate = cert.endDate || "";
  certificateTypeIndex.value = Math.max(0, certificateTypes.indexOf(certForm.certificateType));
  trademarkTypeIndex.value = Math.max(0, trademarkTypes.indexOf(certForm.trademarkType));
  certFileList.value = (cert.fileUrls || []).map((url, idx) => ({
    url,
    name: getDisplayCertificateFileName(cert.fileNames?.[idx], idx, url),
  }));
}

function onCopyrightChange(e: any) {
  hasCopyright.value = e.detail.value;
  if (!hasCopyright.value) {
    selectedCertificate.value = null;
    copyrightFileUrl.value = '';
    showCertPopup.value = false;
  } else {
    loadCommonCertificates();
  }
}

function getRange(param: ProductParameter): { min: number; max: number } | null {
  const rules = param.validationRules;
  if (!rules || rules.min == null || rules.max == null) return null;
  return { min: Number(rules.min), max: Number(rules.max) };
}

function validateParam(param: ProductParameter, value: number | undefined): string {
  if (value == null) return '';
  const range = getRange(param);
  if (!range) return '';
  if (value < range.min) return `不能小于 ${range.min}`;
  if (value > range.max) return `不能大于 ${range.max}`;
  return '';
}

function isParamRequired(param: ProductParameter): boolean {
  return param.isRequired !== false;
}

function getActiveParamsForQuote(): ProductParameter[] {
  return visibleParams.value;
}

function getMaterialParam(): ProductParameter | undefined {
  if (materialParameterId.value != null) {
    return params.value.find((p) => p.id === materialParameterId.value);
  }
  return params.value.find((p) => p.paramType === "SELECT" && p.paramName === "材质");
}

function getCurrentMaterialOptionId(): number | null {
  const materialParam = getMaterialParam();
  if (!materialParam) return null;
  const val = formValues[materialParam.paramName];
  if (Array.isArray(val)) {
    const first = Number(val[0]);
    return Number.isFinite(first) ? first : null;
  }
  const n = Number(val);
  return Number.isFinite(n) && n > 0 ? n : null;
}

function getVisibleOptions(param: ProductParameter): ParameterOption[] {
  const options = paramOptions[param.id] || [];
  if (materialParameterId.value == null || param.id === materialParameterId.value) {
    return options;
  }
  if (!param.isDynamicByMaterial) {
    return options;
  }
  const allowed = enabledOptionIds.value[param.id] || [];
  if (allowed.length === 0) {
    return options;
  }
  return options.filter((opt) => allowed.includes(opt.id));
}

function getSelectedOptionIdsForParam(param: ProductParameter): number[] {
  const val = formValues[param.paramName];
  if (isMultiSelectParam(param)) {
    return Array.isArray(val)
      ? val.map((v: any) => Number(v)).filter((v: number) => Number.isFinite(v))
      : [];
  }
  const n = Number(val);
  return Number.isFinite(n) ? [n] : [];
}

function formatSignedPrice(value: number): string {
  const n = Number.isFinite(value) ? value : 0;
  if (n > 0) return `+¥${n.toFixed(2)}`;
  if (n < 0) return `-¥${Math.abs(n).toFixed(2)}`;
  return "¥0.00";
}

function isMultiSelectParam(param: ProductParameter): boolean {
  return param.paramType === "SELECT" && param.isMultiple === true;
}

function isOptionSelected(param: ProductParameter, optionId: number): boolean {
  const val = formValues[param.paramName];
  if (isMultiSelectParam(param)) {
    return Array.isArray(val) && val.includes(optionId);
  }
  return val === optionId;
}

function isParamFilled(param: ProductParameter): boolean {
  const val = formValues[param.paramName];
  if (param.paramType === "INPUT") {
    return val !== undefined && val !== null && val !== "" && Number.isFinite(Number(val));
  }
  if (isMultiSelectParam(param)) {
    return Array.isArray(val) && val.length > 0;
  }
  return val !== undefined && val !== null && val !== "";
}

function getMissingRequiredParamNames(): string[] {
  return getActiveParamsForQuote()
    .filter((p) => isParamRequired(p) && !isParamFilled(p))
    .map((p) => p.paramName);
}

let productId = 0;

onLoad((query: any) => {
  // #ifdef H5
  isDesktopH5.value = window.innerWidth >= 992;
  // #endif
  productId = Number(query.id);
  const cartItemId = Number(query.cartItemId || 0);
  editingCartItemId.value = Number.isFinite(cartItemId) && cartItemId > 0 ? cartItemId : null;
  const orderIdFromQuery = Number(query.orderId || 0);
  const orderItemIdFromQuery = Number(query.orderItemId || 0);
  editingOrderId.value = Number.isFinite(orderIdFromQuery) && orderIdFromQuery > 0 ? orderIdFromQuery : null;
  editingOrderItemId.value = Number.isFinite(orderItemIdFromQuery) && orderItemIdFromQuery > 0 ? orderItemIdFromQuery : null;
  if (query.source === "home" || query.source === "cart" || query.source === "favorites" || query.source === "order") {
    entrySource.value = query.source;
  } else if (editingOrderItemId.value && editingOrderId.value) {
    entrySource.value = "order";
  } else if (editingCartItemId.value) {
    entrySource.value = "cart";
  }
  if (typeof query.configCode === "string" && query.configCode.trim()) {
    pendingConfigCode.value = query.configCode.trim();
  }
  if (String(query.autoApply || "") === "1") {
    shouldAutoApplyConfigCode.value = true;
  }
  loadShareUserName();
  loadData();
});

onShow(() => {
  if (hasCopyright.value) {
    loadCommonCertificates();
  }
});

async function loadData() {
  try {
    product.value = await getPublicProduct(productId);
    currentHeroIndex.value = 0;
    params.value = await getProductParameters(productId);
    const materialParam = params.value.find((p) => p.paramType === "SELECT" && p.paramName === "材质");
    materialParameterId.value = materialParam?.id || null;
    for (const p of params.value) {
      if (p.paramType === "SELECT") {
        paramOptions[p.id] = await getParameterOptions(p.id);
      }
    }
    if (editingOrderId.value && editingOrderItemId.value) {
      await preloadFromOrderItem(editingOrderId.value, editingOrderItemId.value);
    } else if (editingCartItemId.value) {
      await preloadFromCartItem(editingCartItemId.value);
    }
    await refreshMaterialConfig(getCurrentMaterialOptionId());
    sanitizeSelectionsByMaterialConfig();
    doQuote();
    await loadProductFavoriteStatus();
    if (pendingConfigCode.value) {
      await handleResolvedConfigCode(pendingConfigCode.value, shouldAutoApplyConfigCode.value);
      pendingConfigCode.value = "";
      shouldAutoApplyConfigCode.value = false;
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || "加载失败", icon: "none" });
  }
}

async function loadProductFavoriteStatus() {
  if (!productId) return;
  try {
    const res = await getProductFavoriteStatus(productId);
    isProductFavorited.value = !!res.favorited;
    favoriteConfigCodeCount.value = Number(res.favoriteConfigCodeCount || 0);
  } catch {
    isProductFavorited.value = false;
    favoriteConfigCodeCount.value = 0;
  }
}

async function loadShareUserName() {
  try {
    const me = await getMe();
    shareUserName.value = (me.nickname || me.username || "用户").trim() || "用户";
  } catch {
    shareUserName.value = "用户";
  }
}

async function preloadFromCartItem(cartItemId: number) {
  try {
    const cartItems = await listCartItems();
    const current = cartItems.find((item) => item.id === cartItemId);
    if (!current) {
      uni.showToast({ title: "购物车条目不存在", icon: "none" });
      return;
    }
    quantity.value = Math.max(1, Number(current.quantity || 1));
    printFileUrl.value = current.printFileUrl || "";
    printFileName.value = "";
    proofFileUrl.value = current.proofFileUrl || "";
    proofFileName.value = "";
    hasCopyright.value = !!current.hasCopyright;
    copyrightFileUrl.value = current.copyrightFileUrl || "";
    if (current.paramsSnapshot) {
      const snapshot = JSON.parse(current.paramsSnapshot);
      printFileName.value = getSnapshotFileName(current.paramsSnapshot, "print")
        || (current.printFileUrl ? getFilenameFromPath(current.printFileUrl) : "");
      proofFileName.value = getSnapshotFileName(current.paramsSnapshot, "proof")
        || (current.proofFileUrl ? getFilenameFromPath(current.proofFileUrl) : "");
      const snapshotParams = Array.isArray(snapshot?.params) ? snapshot.params : [];
      for (const p of params.value) {
        const matched = snapshotParams.find((sp: any) =>
          Number(sp?.parameterId || 0) === p.id || sp?.name === p.paramName || sp?.code === p.paramName
        );
        if (!matched) continue;
        const resolved = getSnapshotValueForParam(matched, p);
        if (resolved !== undefined) {
          formValues[p.paramName] = resolved;
        }
      }
      if (snapshot?.certificate) {
        selectedCertificate.value = snapshot.certificate as UserCertificate;
      }
    } else {
      printFileName.value = current.printFileUrl ? getFilenameFromPath(current.printFileUrl) : "";
      proofFileName.value = current.proofFileUrl ? getFilenameFromPath(current.proofFileUrl) : "";
    }
    await refreshMaterialConfig(getCurrentMaterialOptionId());
    sanitizeSelectionsByMaterialConfig();
    doQuote();
  } catch (e: any) {
    uni.showToast({ title: e?.message || "加载购物车配置失败", icon: "none" });
  }
}

async function preloadFromOrderItem(targetOrderId: number, targetOrderItemId: number) {
  try {
    const detail = await getOrderDetail(targetOrderId);
    if (!detail?.order || detail.order.status !== "REJECTED") {
      uni.showToast({ title: "仅待修改订单可编辑", icon: "none" });
      return;
    }
    const current = (detail.items || []).find((item) => Number(item.id) === Number(targetOrderItemId));
    if (!current) {
      uni.showToast({ title: "订单商品不存在", icon: "none" });
      return;
    }
    quantity.value = Math.max(1, Number(current.quantity || 1));
    printFileUrl.value = current.printFileUrl || "";
    printFileName.value = "";
    proofFileUrl.value = current.proofFileUrl || "";
    proofFileName.value = "";
    hasCopyright.value = !!current.hasCopyright;
    copyrightFileUrl.value = current.copyrightFileUrl || "";
    if (current.paramsSnapshot) {
      const snapshot = JSON.parse(current.paramsSnapshot);
      printFileName.value = getSnapshotFileName(current.paramsSnapshot, "print")
        || (current.printFileUrl ? getFilenameFromPath(current.printFileUrl) : "");
      proofFileName.value = getSnapshotFileName(current.paramsSnapshot, "proof")
        || (current.proofFileUrl ? getFilenameFromPath(current.proofFileUrl) : "");
      const snapshotParams = Array.isArray(snapshot?.params) ? snapshot.params : [];
      for (const p of params.value) {
        const matched = snapshotParams.find((sp: any) =>
          Number(sp?.parameterId || 0) === p.id || sp?.name === p.paramName || sp?.code === p.paramName
        );
        if (!matched) continue;
        const resolved = getSnapshotValueForParam(matched, p);
        if (resolved !== undefined) {
          formValues[p.paramName] = resolved;
        }
      }
      if (snapshot?.certificate) {
        selectedCertificate.value = snapshot.certificate as UserCertificate;
      }
    } else {
      printFileName.value = current.printFileUrl ? getFilenameFromPath(current.printFileUrl) : "";
      proofFileName.value = current.proofFileUrl ? getFilenameFromPath(current.proofFileUrl) : "";
    }
    await refreshMaterialConfig(getCurrentMaterialOptionId());
    sanitizeSelectionsByMaterialConfig();
    doQuote();
  } catch (e: any) {
    uni.showToast({ title: e?.message || "加载订单配置失败", icon: "none" });
  }
}

async function refreshMaterialConfig(materialOptionId?: number | null) {
  try {
    const data = await getMaterialConfig(productId, materialOptionId ?? undefined);
    materialParameterId.value = data.materialParameterId || materialParameterId.value;
    enabledDynamicParamIds.value = Array.isArray(data.enabledDynamicParamIds) ? data.enabledDynamicParamIds : [];
    const mapped: Record<number, number[]> = {};
    const raw = data.enabledOptionIds || {};
    Object.keys(raw).forEach((k) => {
      const key = Number(k);
      if (!Number.isFinite(key)) return;
      const ids = Array.isArray((raw as any)[k]) ? (raw as any)[k].map((v: any) => Number(v)).filter((v: number) => Number.isFinite(v)) : [];
      mapped[key] = ids;
    });
    enabledOptionIds.value = mapped;
  } catch {
    enabledDynamicParamIds.value = [];
    enabledOptionIds.value = {};
  }
}

function sanitizeSelectionsByMaterialConfig(showNotice = false) {
  let changed = false;
  for (const p of params.value) {
    if (!visibleParams.value.find((v) => v.id === p.id)) {
      if (formValues[p.paramName] !== undefined) {
        changed = true;
      }
      delete formValues[p.paramName];
      delete validationErrors[p.paramName];
      continue;
    }
    if (p.paramType !== "SELECT") continue;
    const visibleOptionIds = new Set(getVisibleOptions(p).map((o) => o.id));
    const current = formValues[p.paramName];
    if (isMultiSelectParam(p)) {
      const next = Array.isArray(current) ? current.filter((v: any) => visibleOptionIds.has(Number(v))) : [];
      if (Array.isArray(current) && current.length !== next.length) {
        changed = true;
      }
      formValues[p.paramName] = next;
    } else if (current != null && !visibleOptionIds.has(Number(current))) {
      changed = true;
      delete formValues[p.paramName];
    }
  }
  if (showNotice && changed) {
    uni.showToast({ title: "部分选项不适用于当前材质，已自动重置", icon: "none" });
  }
}

function onInputChange(param: ProductParameter, value: string) {
  const numVal = value ? Number(value) : undefined;
  formValues[param.paramName] = numVal;
  validationErrors[param.paramName] = validateParam(param, numVal);
  doQuote();
}

async function onOptionSelect(param: ProductParameter, opt: ParameterOption) {
  const key = param.paramName;
  if (isMultiSelectParam(param)) {
    const current = Array.isArray(formValues[key]) ? [...formValues[key]] : [];
    const idx = current.indexOf(opt.id);
    if (idx >= 0) {
      current.splice(idx, 1);
    } else {
      current.push(opt.id);
    }
    formValues[key] = current;
    if (materialParameterId.value != null && param.id === materialParameterId.value) {
      await refreshMaterialConfig(getCurrentMaterialOptionId());
      sanitizeSelectionsByMaterialConfig(true);
    }
    doQuote();
    return;
  }
  // Single select: tap again to cancel selection
  formValues[key] = formValues[key] === opt.id ? undefined : opt.id;
  if (materialParameterId.value != null && param.id === materialParameterId.value) {
    await refreshMaterialConfig(getCurrentMaterialOptionId());
    sanitizeSelectionsByMaterialConfig(true);
  }
  doQuote();
}

function previewOptionImage(url?: string) {
  if (!url) return;
  uni.previewImage({ urls: [resolveAsset(url)] });
}

function previewProductImage(currentIndex = 0) {
  if (productImages.value.length === 0) return;
  const urls = productImages.value.map((img) => resolveAsset(img));
  uni.previewImage({
    urls,
    current: urls[Math.max(0, currentIndex)] || urls[0],
  });
}

function onHeroSwiperChange(e: any) {
  const idx = Number(e?.detail?.current ?? 0);
  currentHeroIndex.value = Number.isFinite(idx) && idx >= 0 ? idx : 0;
}

function resolveAsset(url?: string) {
  return toAbsoluteAssetUrl(url || "");
}

function changeQty(delta: number) {
  quantity.value = Math.max(1, quantity.value + delta);
  doQuote();
}

function onQtyBlur() {
  if (!quantity.value || quantity.value < 1) {
    quantity.value = 1;
  } else {
    quantity.value = Math.floor(quantity.value);
  }
  doQuote();
}

let quoteTimer: ReturnType<typeof setTimeout> | null = null;

function doQuote() {
  if (quoteTimer) clearTimeout(quoteTimer);
  quoteTimer = setTimeout(async () => {
    try {
      const activeParams = getActiveParamsForQuote();
      const items = activeParams.map((p) => {
        const val = formValues[p.paramName];
        const selectedOptionIds = p.paramType === "SELECT"
          ? (isMultiSelectParam(p)
            ? (Array.isArray(val) ? val : [])
            : (val != null && val !== "" ? [Number(val)] : []))
          : [];
        return {
          parameterId: p.id,
          paramCode: p.paramName,
          valueNumber: p.paramType === "INPUT" ? (val as number) : undefined,
          selectedOptionIds: p.paramType === "SELECT" && selectedOptionIds.length > 0 ? selectedOptionIds : undefined,
        };
      });
      const allRequiredFilled = activeParams.every((p) => !isParamRequired(p) || isParamFilled(p));
      if (!allRequiredFilled || hasValidationErrors()) {
        quoteResult.value = null;
        return;
      }
      quoteResult.value = await quote({ productId, quantity: quantity.value, items });
    } catch {}
  }, 400);
}

function hasValidationErrors(): boolean {
  return Object.values(validationErrors).some((e) => e !== '');
}

function sanitizeConfigCode(raw: string): string {
  return String(raw || "").replace(/\D/g, "").slice(0, 12);
}

async function promptConfigCodeInput(): Promise<string | null> {
  // #ifdef MP-WEIXIN
  return await new Promise((resolve) => {
    uni.showModal({
      title: "输入产品配置码",
      editable: true,
      placeholderText: "请输入8位数字配置码",
      confirmText: "解析",
      cancelText: "取消",
      success: (res) => {
        if (!res.confirm) {
          resolve(null);
          return;
        }
        resolve(sanitizeConfigCode((res as any).content || ""));
      },
      fail: () => resolve(null),
    });
  });
  // #endif
  // #ifdef H5
  const value = window.prompt("请输入8位数字配置码", "");
  if (!value) return null;
  return sanitizeConfigCode(value);
  // #endif
  // #ifndef H5
  return null;
  // #endif
}

async function pickConfigCodeExpireDays(): Promise<number | null> {
  const labels = CONFIG_EXPIRE_OPTIONS.map((o) => o.label);
  return await new Promise((resolve) => {
    uni.showActionSheet({
      itemList: labels,
      success: (res) => resolve(CONFIG_EXPIRE_OPTIONS[res.tapIndex]?.days ?? null),
      fail: () => resolve(null),
    });
  });
}

function buildConfigShareSnapshot() {
  const activeParams = getActiveParamsForQuote();
  return {
    productName: product.value?.name,
    params: activeParams.map((p) => {
      const raw = formValues[p.paramName];
      const value = raw === undefined || raw === "" ? null : raw;
      return {
        parameterId: p.id,
        name: p.paramName,
        code: p.paramName,
        value,
      };
    }),
  };
}

function buildConfigShareSummaryContent(resolved: {
  creatorName: string;
  productName: string;
  code: string;
  expireAt?: string | null;
}): string {
  const lines = [
    `分享人：${resolved.creatorName}`,
    `产品：${resolved.productName}`,
    `配置码：${resolved.code}`,
  ];
  if (resolved.expireAt) {
    lines.push(`有效期至：${String(resolved.expireAt).replace("T", " ").slice(0, 16)}`);
  } else {
    lines.push("有效期：永久");
  }
  return lines.join("\n");
}

async function applyConfigSnapshot(paramsSnapshot: string, creatorName?: string) {
  let parsed: any;
  try {
    parsed = JSON.parse(paramsSnapshot);
  } catch {
    throw new Error("配置快照格式无效");
  }
  const snapshotParams: any[] = Array.isArray(parsed?.params) ? parsed.params : [];
  const findSnapshot = (param: ProductParameter) =>
    snapshotParams.find((sp: any) =>
      Number(sp?.parameterId || 0) === param.id || sp?.name === param.paramName || sp?.code === param.paramName
    );

  const materialParam = getMaterialParam();
  if (materialParam) {
    const materialSnapshot = findSnapshot(materialParam);
    if (materialSnapshot) {
      const materialValue = getSnapshotValueForParam(materialSnapshot, materialParam);
      if (materialValue === undefined || materialValue === null || materialValue === "") {
        delete formValues[materialParam.paramName];
      } else {
        formValues[materialParam.paramName] = materialValue;
      }
    } else {
      delete formValues[materialParam.paramName];
    }
  }

  await refreshMaterialConfig(getCurrentMaterialOptionId());

  for (const p of params.value) {
    if (materialParam && p.id === materialParam.id) continue;
    const snapshotParam = findSnapshot(p);
    if (!snapshotParam) {
      delete formValues[p.paramName];
      continue;
    }
    const resolved = getSnapshotValueForParam(snapshotParam, p);
    if (resolved === undefined || resolved === null || resolved === "") {
      delete formValues[p.paramName];
    } else {
      formValues[p.paramName] = resolved;
    }
  }

  sanitizeSelectionsByMaterialConfig(true);
  doQuote();
  uni.showToast({ title: creatorName ? `已应用 ${creatorName} 的配置` : "已应用配置", icon: "success" });
}

async function handleResolvedConfigCode(code: string, autoApply = false) {
  if (!/^\d{8}$/.test(code)) {
    uni.showToast({ title: "请输入8位数字配置码", icon: "none" });
    return;
  }
  const resolved = await resolveProductConfigCode(code);

  if (resolved.productId !== productId) {
    if (autoApply) {
      uni.redirectTo({
        url: `/pages/product-detail/product-detail?id=${resolved.productId}&source=home&configCode=${resolved.code}&autoApply=1`,
      });
      return;
    }
    uni.showModal({
      title: "配置码对应其他商品",
      content: `${resolved.creatorName} 分享的是「${resolved.productName}」，是否跳转并应用？`,
      confirmText: "跳转应用",
      cancelText: "取消",
      success: (res) => {
        if (!res.confirm) return;
        uni.redirectTo({
          url: `/pages/product-detail/product-detail?id=${resolved.productId}&source=home&configCode=${resolved.code}&autoApply=1`,
        });
      },
    });
    return;
  }

  if (!autoApply) {
    uni.showModal({
      title: "配置码解析成功",
      content: buildConfigShareSummaryContent(resolved),
      confirmText: "应用",
      cancelText: "取消",
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await applyConfigSnapshot(resolved.paramsSnapshot, resolved.creatorName);
        } catch (e: any) {
          uni.showToast({ title: e?.message || "应用配置失败", icon: "none" });
        }
      },
    });
    return;
  }

  await applyConfigSnapshot(resolved.paramsSnapshot, resolved.creatorName);
}

async function onApplyConfigCode() {
  const code = await promptConfigCodeInput();
  if (!code) return;
  if (!/^\d{8}$/.test(code)) {
    uni.showToast({ title: "请输入8位数字配置码", icon: "none" });
    return;
  }
  uni.navigateTo({ url: `/pages/config-code-preview/config-code-preview?code=${code}` });
}

async function onGenerateConfigCode() {
  if (!product.value) {
    uni.showToast({ title: "商品信息未加载完成", icon: "none" });
    return;
  }
  const snapshot = buildConfigShareSnapshot();
  const hasAnySelected = (snapshot.params || []).some((p: any) => {
    if (Array.isArray(p.value)) return p.value.length > 0;
    return p.value !== null && p.value !== undefined && p.value !== "";
  });
  if (!hasAnySelected) {
    uni.showToast({ title: "请先选择至少一个参数", icon: "none" });
    return;
  }
  const expireDays = await pickConfigCodeExpireDays();
  if (expireDays == null) return;
  try {
    const result = await createProductConfigCode(productId, JSON.stringify(snapshot), expireDays);
    const expireText = result.expireAt
      ? String(result.expireAt).replace("T", " ").slice(0, 16)
      : "永久";
    uni.showModal({
      title: "配置码生成成功",
      content: `配置码：${result.code}\n有效期：${expireText}`,
      confirmText: "复制",
      cancelText: "收藏",
      success: async (res) => {
        if (res.confirm) {
          uni.setClipboardData({
            data: result.code,
            success: () => uni.showToast({ title: "已复制配置码", icon: "success" }),
          });
          return;
        }
        if (!res.cancel) return;
        try {
          await favoriteConfigCode(result.code);
          isProductFavorited.value = true;
          favoriteConfigCodeCount.value += 1;
          uni.showToast({ title: "已收藏配置码", icon: "success" });
        } catch (e: any) {
          uni.showToast({ title: e?.message || "收藏失败", icon: "none" });
        }
      },
    });
  } catch (e: any) {
    uni.showToast({ title: e?.message || "生成配置码失败", icon: "none" });
  }
}

async function toggleProductFavorite() {
  if (!productId || favoriteLoading.value) return;
  favoriteLoading.value = true;
  try {
    if (isProductFavorited.value) {
      if (favoriteConfigCodeCount.value > 0) {
        const shouldDeleteAll = await new Promise<boolean>((resolve) => {
          uni.showModal({
            title: "取消收藏",
            content: `此商品收藏有 ${favoriteConfigCodeCount.value} 个配置码，是否删除收藏？删除会同步删除配置码。`,
            confirmText: "不删除了",
            cancelText: "全部删除",
            success: (res) => resolve(!!res.cancel),
            fail: () => resolve(false),
          });
        });
        if (!shouldDeleteAll) {
          return;
        }
        await unfavoriteProduct(productId, true);
        if (shouldDeleteAll) {
          isProductFavorited.value = false;
          favoriteConfigCodeCount.value = 0;
          uni.showToast({ title: "已删除收藏", icon: "none" });
        }
      } else {
        await unfavoriteProduct(productId);
        isProductFavorited.value = false;
        favoriteConfigCodeCount.value = 0;
        uni.showToast({ title: "已取消收藏", icon: "none" });
      }
    } else {
      await favoriteProduct(productId);
      isProductFavorited.value = true;
      await loadProductFavoriteStatus();
      uni.showToast({ title: "已收藏商品", icon: "success" });
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || "操作失败", icon: "none" });
  } finally {
    favoriteLoading.value = false;
  }
}

function onCertificateTypeChange(e: any) {
  const idx = Number(e.detail.value || 0);
  certificateTypeIndex.value = idx;
  certForm.certificateType = certificateTypes[idx] || "";
}

function onTrademarkTypeChange(e: any) {
  const idx = Number(e.detail.value || 0);
  trademarkTypeIndex.value = idx;
  certForm.trademarkType = trademarkTypes[idx] || "未知";
}

function onCertEndDateChange(e: any) {
  certForm.endDate = e.detail.value;
}

function appendCertificateFile(file: LocalCertFile) {
  const ext = getFileExtension(file.name || file.path || file.url || "");
  if (!CERT_ALLOWED_EXT.includes(ext)) {
    uni.showToast({ title: "仅支持 jpg/png/gif/pdf", icon: "none" });
    return;
  }
  if ((file.size || 0) > 10 * 1024 * 1024) {
    uni.showToast({ title: "单个文件不能超过10M", icon: "none" });
    return;
  }
  if (certFileList.value.length >= 5) {
    return;
  }
  certFileList.value.push(file);
}

async function chooseCertificateFiles() {
  const remain = 5 - certFileList.value.length;
  if (remain <= 0) {
    uni.showToast({ title: "最多上传5个文件", icon: "none" });
    return;
  }
  try {
    // #ifdef MP-WEIXIN
    const mpRes: any = await new Promise((resolve, reject) => {
      uni.chooseMessageFile({
        count: remain,
        type: "file",
        extension: CERT_ALLOWED_EXT,
        success: resolve,
        fail: reject,
      });
    });
    const mpFiles = (mpRes?.tempFiles || []) as any[];
    for (const f of mpFiles) {
      appendCertificateFile({ path: f.path || f.tempFilePath, name: f.name || "", size: Number(f.size || 0) });
    }
    // #endif

    // #ifdef H5
    const h5Res: any = await new Promise((resolve, reject) => {
      uni.chooseFile({
        count: remain,
        extension: CERT_ALLOWED_EXT,
        success: resolve,
        fail: reject,
      });
    });
    const h5Files = (h5Res?.tempFiles || []) as any[];
    for (const f of h5Files) {
      appendCertificateFile({ path: f.path || f.tempFilePath, name: f.name || "", size: Number(f.size || 0) });
    }
    // #endif
  } catch {}
}

function removeCertificateFile(idx: number) {
  certFileList.value.splice(idx, 1);
}

function validateCertificateForm() {
  if (!certForm.certificateType) {
    uni.showToast({ title: "请选择证书类型", icon: "none" });
    return false;
  }
  if (!certForm.trademarkContent.trim()) {
    uni.showToast({ title: "请输入商标/授权内容", icon: "none" });
    return false;
  }
  if (!certForm.principal.trim()) {
    uni.showToast({ title: "请输入委托方", icon: "none" });
    return false;
  }
  if (!certForm.endDate) {
    uni.showToast({ title: "请选择结束日期", icon: "none" });
    return false;
  }
  if (certFileList.value.length === 0) {
    uni.showToast({ title: "请上传证书文件", icon: "none" });
    return false;
  }
  return true;
}

async function uploadCertificateSingle(path: string): Promise<string> {
  const baseUrl = getApiBaseUrl();
  const token = getToken();
  const uploadRes: any = await new Promise((resolve, reject) => {
    uni.uploadFile({
      url: baseUrl + "/api/upload",
      filePath: path,
      name: "file",
      formData: { scene: "certificate" },
      header: { Authorization: token },
      success: resolve,
      fail: reject,
    });
  });
  if (uploadRes.statusCode !== 200) {
    throw new Error("上传失败");
  }
  let data = uploadRes.data;
  if (typeof data === "string") data = JSON.parse(data);
  if (!data.success) {
    throw new Error(data.message || "上传失败");
  }
  return data.data.url;
}

async function submitCertificate() {
  if (!validateCertificateForm()) return;
  certSubmitting.value = true;
  uni.showLoading({ title: "提交中..." });
  try {
    const fileUrls: string[] = [];
    const fileNames: string[] = [];
    for (const f of certFileList.value) {
      if (f.url) {
        fileUrls.push(f.url);
        fileNames.push(f.name || getFilenameFromPath(f.url));
      } else if (f.path) {
        fileUrls.push(await uploadCertificateSingle(f.path));
        fileNames.push(f.name || getFilenameFromPath(f.path));
      }
    }
    if (fileUrls.length === 0) {
      throw new Error("请上传证书文件");
    }
    const reqPayload = {
      certificateType: certForm.certificateType,
      trademarkType: certForm.trademarkType || "未知",
      trademarkContent: certForm.trademarkContent.trim(),
      principal: certForm.principal.trim(),
      endDate: certForm.endDate,
      fileUrls,
      fileNames,
    };
    const saved = editingCertificateId.value
      ? await updateCommonCertificate(editingCertificateId.value, reqPayload)
      : await createCommonCertificate(reqPayload);
    selectedCertificate.value = saved;
    copyrightFileUrl.value = saved.fileUrls?.[0] || "";
    await loadCommonCertificates();
    certPopupMode.value = "list";
    editingCertificateId.value = null;
    uni.hideLoading();
    uni.showToast({ title: "证书已保存并选用", icon: "success" });
    showCertPopup.value = false;
  } catch (e: any) {
    uni.hideLoading();
    uni.showToast({ title: e?.message || "提交失败", icon: "none" });
  } finally {
    certSubmitting.value = false;
  }
}

async function onAddToCart() {
  const missingRequired = getMissingRequiredParamNames();
  if (missingRequired.length > 0) {
    uni.showToast({ title: `请先填写必填参数：${missingRequired[0]}`, icon: "none" });
    return;
  }
  const activeParams = getActiveParamsForQuote();
  for (const p of activeParams) {
    if (p.paramType === 'INPUT') {
      validationErrors[p.paramName] = validateParam(p, formValues[p.paramName]);
    }
  }
  if (hasValidationErrors()) {
    uni.showToast({ title: "请修正参数范围", icon: "none" });
    return;
  }
  if (!quoteResult.value) {
    uni.showToast({ title: "请先配置参数获取报价", icon: "none" });
    return;
  }
  adding.value = true;
  try {
    const snapshot = {
      productName: product.value?.name,
      params: activeParams.map((p) => {
        const val = formValues[p.paramName];
        if (p.paramType !== "SELECT") {
          return {
            parameterId: p.id,
            name: p.paramName,
            code: p.paramName,
            value: val,
          };
        }
        if (isMultiSelectParam(p)) {
          const selectedIds = Array.isArray(val) ? val : [];
          return {
            parameterId: p.id,
            name: p.paramName,
            code: p.paramName,
            value: selectedIds,
          };
        }
        return {
          parameterId: p.id,
          name: p.paramName,
          code: p.paramName,
          value: val,
        };
      }),
      setupFee: Number(quoteResult.value.breakdown?.setupFee || 0),
      freeSetupQuantity: Number(product.value?.freeSetupQuantity || 0),
      breakdown: quoteResult.value.breakdown || {},
      certificate: selectedCertificate.value
        ? {
            id: selectedCertificate.value.id,
            certificateType: selectedCertificate.value.certificateType,
            trademarkContent: selectedCertificate.value.trademarkContent,
            principal: selectedCertificate.value.principal,
            endDate: selectedCertificate.value.endDate,
          }
        : undefined,
      printFileName: printFileName.value || undefined,
      proofFileName: proofFileName.value || undefined,
    };
    const payload = {
      productId,
      quantity: quantity.value,
      paramsSnapshot: JSON.stringify(snapshot),
      unitPrice: quoteResult.value.unitPrice,
      printFileUrl: printFileUrl.value || undefined,
      proofFileUrl: proofFileUrl.value || undefined,
      hasCopyright: hasCopyright.value,
      copyrightFileUrl: copyrightFileUrl.value || undefined,
    };
    if (editingOrderId.value && editingOrderItemId.value) {
      await updateRejectedOrderItem(editingOrderId.value, editingOrderItemId.value, {
        quantity: payload.quantity,
        paramsSnapshot: payload.paramsSnapshot,
        unitPrice: Number(payload.unitPrice),
        printFileUrl: payload.printFileUrl,
        proofFileUrl: payload.proofFileUrl,
        hasCopyright: payload.hasCopyright,
        copyrightFileUrl: payload.copyrightFileUrl,
      });
      uni.showToast({ title: "订单修改已保存", icon: "success" });
      setTimeout(() => onBackTap(), 400);
      return;
    }
    await addCartItem(payload);
    if (editingCartItemId.value) {
      try {
        await removeCartItem(editingCartItemId.value);
      } catch {
        // Keep new item even if old item deletion fails
      }
      uni.showToast({ title: "已更新购物车", icon: "success" });
      setTimeout(() => onBackTap(), 400);
    } else {
      showAddCartPopup.value = true;
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || "操作失败", icon: "none" });
  } finally {
    adding.value = false;
  }
}

function goHomeAfterAddCart() {
  showAddCartPopup.value = false;
  uni.switchTab({ url: "/pages/index/index" });
}

function goCartAfterAddCart() {
  showAddCartPopup.value = false;
  uni.switchTab({ url: "/pages/cart/cart" });
}

function onBackTap() {
  if (entrySource.value === "order" || editingOrderItemId.value) {
    if (editingOrderId.value) {
      uni.redirectTo({ url: `/pages/order-detail/order-detail?id=${editingOrderId.value}` });
      return;
    }
    uni.navigateBack({ delta: 1 });
    return;
  }
  if (entrySource.value === "cart" || editingCartItemId.value) {
    uni.switchTab({ url: "/pages/cart/cart" });
    return;
  }
  if (entrySource.value === "favorites") {
    uni.navigateBack({
      fail: () => uni.navigateTo({ url: "/pages/profile-favorites/profile-favorites" }),
    });
    return;
  }
  if (entrySource.value === "home") {
    uni.switchTab({ url: "/pages/index/index" });
    return;
  }
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack({ delta: 1 });
    return;
  }
  uni.switchTab({ url: "/pages/index/index" });
}

function buildSharePayload() {
  const productName = (product.value?.name || "商品").trim();
  const rawDesc = (product.value?.description || "").trim();
  const shortDesc = rawDesc.length > 30 ? `${rawDesc.slice(0, 30)}...` : rawDesc;
  const title = `${shareUserName.value}推荐[环地福印刷商城]${productName}${shortDesc ? ` ${shortDesc}` : ""}`;
  const imageUrl = productImages.value.length > 0 ? resolveAsset(productImages.value[0]) : "";
  const path = `/pages/product-detail/product-detail?id=${productId}&source=home`;
  return { title, imageUrl, path };
}

onShareAppMessage(() => {
  const payload = buildSharePayload();
  return {
    title: payload.title,
    path: payload.path,
    imageUrl: payload.imageUrl || undefined,
  };
});

onShareTimeline(() => {
  const payload = buildSharePayload();
  return {
    title: payload.title,
    query: `id=${productId}&source=home`,
    imageUrl: payload.imageUrl || undefined,
  };
});
</script>

<style scoped>
.upload-container {
  padding: 10rpx 0;
}
.upload-item + .upload-item {
  margin-top: 24rpx;
}
.design-upload-tip {
  margin-top: 12rpx;
  display: block;
  font-size: 22rpx;
  color: #64748B;
  line-height: 1.5;
}
.upload-label {
  display: block;
  font-size: 26rpx;
  color: #334155;
  margin-bottom: 8rpx;
}
.upload-box {
  width: 100%;
  height: 200rpx;
  background-color: #f8f9fa;
  border: 2rpx dashed #dcdfe6;
  border-radius: 12rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.upload-icon {
  font-size: 60rpx;
  color: #909399;
}
.upload-text {
  font-size: 28rpx;
  color: #909399;
  margin-top: 10rpx;
}
.upload-subtext {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #94A3B8;
}
.preview-box {
  width: 100%;
  position: relative;
  border: 2rpx solid #ebeef5;
  border-radius: 12rpx;
  overflow: hidden;
}
.preview-img {
  width: 100%;
  height: 300rpx;
  background-color: #f8f9fa;
}
.reupload-btn {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  text-align: center;
  padding: 10rpx 0;
  font-size: 26rpx;
}
.file-info-box {
  width: 100%;
  box-sizing: border-box;
  border: 2rpx solid #E2E8F0;
  border-radius: 12rpx;
  padding: 24rpx;
  background: #F8FAFC;
}
.file-info-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.file-icon {
  font-size: 48rpx;
}
.file-detail {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}
.file-name {
  font-size: 28rpx;
  color: #1E293B;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-status {
  font-size: 24rpx;
  color: #10B981;
  margin-top: 4rpx;
}
.file-delete {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FEE2E2;
  border-radius: 24rpx;
  margin-left: 16rpx;
}
.file-delete text {
  color: #EF4444;
  font-size: 32rpx;
  font-weight: 600;
  line-height: 1;
  margin-top: -4rpx;
}
.reupload-link {
  margin-top: 16rpx;
  font-size: 26rpx;
  color: #3B82F6;
  text-align: right;
}
.detail-page {
  min-height: 100vh;
  background: #F8FAFC;
}

/* #ifdef H5 */
.header-meta-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
  overflow-x: auto;
  white-space: nowrap;
  padding-bottom: 12rpx;
}
.header-meta-line::-webkit-scrollbar {
  display: none;
}
.header-back-btn {
  width: 54rpx;
  height: 54rpx;
  border-radius: 50%;
  border: 1rpx solid #E2E8F0;
  background: #FFFFFF;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.header-favorite-btn {
  width: 54rpx;
  height: 54rpx;
  border-radius: 50%;
  border: 1rpx solid #E2E8F0;
  background: #FFFFFF;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.header-back-icon {
  width: 28rpx;
  height: 28rpx;
  transform: rotate(180deg);
}
.header-favorite-icon {
  width: 30rpx;
  height: 30rpx;
  opacity: 1;
}
.header-meta-item {
  font-size: 26rpx;
  color: #334155;
  flex: 0 0 auto;
}
.header-meta-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #0F172A;
}
.header-meta-desc {
  color: #64748B;
}
.header-meta-dot {
  color: #94A3B8;
  font-size: 28rpx;
  line-height: 1;
  flex: 0 0 auto;
}
.header-gallery {
  width: 100%;
  white-space: nowrap;
}
.header-gallery-list {
  display: inline-flex;
  gap: 16rpx;
}
.header-gallery-img {
  width: 320rpx;
  height: 320rpx;
  border-radius: 12rpx;
  border: 1rpx solid #E2E8F0;
  background: #FFFFFF;
  flex: 0 0 auto;
}
/* #endif */

/* 头部 */
.product-header {
  background: linear-gradient(135deg, #F1F5F9, #FFFFFF);
  border: 1rpx solid #E2E8F0;
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
}

/* #ifdef MP-WEIXIN */
.product-header {
  background: transparent;
  border: 0;
  border-radius: 0;
  padding: 0;
}
.mp-hero-wrap {
  position: relative;
  width: 100%;
  height: 750rpx;
  overflow: hidden;
  background: #E2E8F0;
}
.mp-hero-swiper {
  width: 100%;
  height: 750rpx;
}
.mp-hero-img,
.mp-hero-empty {
  width: 100%;
  height: 750rpx;
}
.mp-hero-back {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}
.mp-hero-left-actions {
  position: absolute;
  left: 24rpx;
  top: 24rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
}
.mp-hero-favorite {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}
.mp-hero-count {
  position: absolute;
  right: 20rpx;
  bottom: 20rpx;
  min-width: 82rpx;
  height: 44rpx;
  padding: 0 12rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.88);
  color: #111827;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 600;
}
.mp-hero-back-icon {
  width: 50rpx;
  height: 50rpx;
  transform: rotate(180deg);
  filter: brightness(0) invert(1);
}
.mp-hero-favorite-icon {
  width: 42rpx;
  height: 42rpx;
  opacity: 1;
}
.mp-product-meta {
  background: #FFFFFF;
  padding: 24rpx;
  margin-bottom: 16rpx;
}
.mp-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
}
.mp-meta-left {
  flex: 1;
  min-width: 0;
}
.mp-meta-main {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8rpx;
}
.mp-meta-right {
  width: 108rpx;
  min-width: 108rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mp-meta-name {
  font-size: 36rpx;
  font-weight: 700;
  color: #0F172A;
  line-height: 1.3;
}
.mp-meta-dot {
  color: #94A3B8;
  font-size: 30rpx;
  line-height: 1;
}
.mp-price-label {
  font-size: 22rpx;
  color: #0F172A;
}
.mp-meta-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #F59E0B;
}
.mp-meta-desc {
  display: block;
  margin-top: 12rpx;
  font-size: 28rpx;
  color: #64748B;
  line-height: 1.5;
}
.mp-share-btn {
  width: 108rpx;
  min-width: 108rpx;
  padding: 0;
  margin: 0;
  border: 0;
  background: transparent;
  line-height: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
}
.mp-share-btn::after {
  border: 0;
}
.mp-share-icon {
  width: 52rpx;
  height: 52rpx;
}
.mp-share-text {
  font-size: 22rpx;
  color: #64748B;
}
/* #endif */

.header-content {
  display: flex;
  align-items: center; /* Keep centers aligned perfectly */
  justify-content: flex-start;
  gap: 24rpx;
}
.header-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.header-img {
  width: 160rpx;      /* Mathematically locked to text height */
  height: 160rpx;     /* Match the 3-line text height */
  border-radius: 12rpx;
  border: 1rpx solid #E2E8F0;
  flex-shrink: 0;
  background-color: #F8FAFC;
}
.p-name {
  display: block;
  font-size: 36rpx;
  line-height: 48rpx; /* Exact height */
  font-weight: 700;
  color: #0F172A;
  margin-bottom: 12rpx;
}
.price-row {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
  line-height: 48rpx; /* Exact height */
  margin-bottom: 12rpx;
}
.price-label {
  font-size: 24rpx;
  line-height: 40rpx;
  color: #64748B;
}
.price-val {
  font-size: 36rpx;
  font-weight: 700;
  line-height: 48rpx;
  color: #F59E0B;
}
.p-desc {
  font-size: 28rpx;
  line-height: 40rpx; /* Exact height */
  color: #64748B;
  display: block;
}

/* 分区 */
.section {
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}
/* #ifdef MP-WEIXIN */
.section {
  border: 0;
  border-radius: 0;
  margin-bottom: 8rpx;
}
/* #endif */
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}
.section-head-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.section-bar {
  width: 6rpx;
  height: 28rpx;
  background: linear-gradient(180deg, #3B82F6, #8B5CF6);
  border-radius: 3rpx;
}
.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1E293B;
}
.section-code-btn {
  height: 52rpx;
  padding: 0 18rpx;
  border: 1rpx solid #0F4C81;
  color: #0F4C81;
  border-radius: 10rpx;
  font-size: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 参数 */
.param-group { margin-bottom: 24rpx; }
.param-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}
.param-name {
  font-size: 26rpx;
  color: #475569;
  font-weight: 500;
}
.required-asterisk {
  color: #EF4444;
  margin-left: 4rpx;
  font-weight: 700;
}
.multi-hint {
  color: #64748B;
  margin-left: 6rpx;
  font-size: 22rpx;
  font-weight: 400;
}
.param-range {
  font-size: 22rpx;
  color: #3B82F6;
  background: rgba(59, 130, 246, 0.1);
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}
.input-wrap { 
  display: flex; 
  flex-direction: column; 
  max-width: 100%;
}
.input-container {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.input-box {
  flex: 1;
  background: #F8FAFC;
  border: 2rpx solid #E2E8F0;
  border-radius: 12rpx;
  padding: 0 20rpx;
  height: 84rpx;
  display: flex;
  align-items: center;
}
.param-unit {
  font-size: 28rpx;
  color: #475569;
  font-weight: 500;
}
.input-box input {
  width: 100%;
  height: 84rpx;
  font-size: 28rpx;
  color: #0F172A;
}
.input-box.has-error {
  border-color: #EF4444;
}
.error-msg {
  font-size: 22rpx;
  color: #EF4444;
  margin-top: 6rpx;
}

/* 选项 */
.options-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}
.option-chip {
  background: #F8FAFC;
  border: 2rpx solid #E2E8F0;
  border-radius: 12rpx;
  padding: 18rpx 32rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  transition: all 0.2s ease;
}
.option-chip.has-img {
  padding: 14rpx 24rpx 14rpx 14rpx; /* Less left padding when image is present */
}
.chip-img {
  width: 90rpx;
  height: 90rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
  border: 1rpx solid #E2E8F0;
  background: #FFFFFF;
}
.chip-info {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.option-chip.active {
  border-color: #3B82F6;
  background: rgba(59, 130, 246, 0.08);
}
.chip-text {
  font-size: 28rpx;
  color: #475569;
}
.option-chip.active .chip-text {
  color: #3B82F6;
  font-weight: 600;
}
.chip-price {
  font-size: 22rpx;
  color: #F59E0B;
}

/* 数量 */
.qty-control {
  display: flex;
  align-items: center;
  gap: 28rpx;
}
.qty-btn {
  width: 68rpx;
  height: 68rpx;
  background: #3B82F6;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.qty-btn.disabled {
  background: #E2E8F0;
}
.qty-btn:active { opacity: 0.85; }
.qty-btn-text {
  font-size: 32rpx;
  color: #fff;
  font-weight: 600;
}
.calc-btn {
  background: #3B82F6;
  color: #fff;
  font-size: 28rpx;
  padding: 0 32rpx;
  height: 68rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
  font-weight: 500;
  margin-left: 20rpx;
}
.calc-btn.disabled {
  background: #E2E8F0;
  color: #94A3B8;
  pointer-events: none; /* Prevents clicking when disabled */
}
.calc-btn:active:not(.disabled) {
  opacity: 0.85;
}
.qty-num-input {
  font-size: 36rpx;
  font-weight: 700;
  color: #0F172A;
  min-width: 80rpx;
  max-width: 120rpx;
  text-align: center;
  border-bottom: 2rpx solid #E2E8F0;
  padding: 4rpx;
}

/* 版权问题说明 */
.copyright-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.copyright-label {
  font-size: 28rpx;
  color: #0F172A;
  font-weight: 500;
}
.copyright-hint {
  display: block;
  font-size: 24rpx;
  color: #F59E0B;
  background-color: #FEF3C7;
  padding: 16rpx;
  border-radius: 8rpx;
  margin-bottom: 16rpx;
  line-height: 1.5;
}
.copyright-actions {
  margin-top: 10rpx;
}
.cert-select-btn {
  height: 84rpx;
  border-radius: 10rpx;
  background: #0F4C81;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 500;
}
.selected-cert {
  margin-top: 14rpx;
  background: #F8FAFC;
  border: 1rpx solid #E2E8F0;
  border-radius: 10rpx;
  padding: 14rpx 16rpx;
}
.selected-cert-name {
  display: block;
  font-size: 24rpx;
  color: #1E293B;
  font-weight: 600;
}
.selected-cert-meta {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #64748B;
}
.cert-popup-mask {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: flex-end;
  z-index: 99;
}
.cert-popup {
  width: 100%;
  background: #fff;
  border-radius: 20rpx 20rpx 0 0;
  padding: 24rpx;
  max-height: 70vh;
}
.cert-popup-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.cert-popup-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.cert-popup-back {
  font-size: 36rpx;
  color: #334155;
  line-height: 1;
}
.cert-popup-title {
  font-size: 30rpx;
  font-weight: 600;
}
.cert-popup-close {
  font-size: 44rpx;
  color: #64748B;
}
.empty-cert {
  padding: 36rpx 0;
  text-align: center;
  color: #94A3B8;
}
.cert-list {
  max-height: 46vh;
  overflow: auto;
  margin-top: 14rpx;
}
.cert-item {
  border: 1rpx solid #E2E8F0;
  border-radius: 10rpx;
  padding: 14rpx;
  margin-bottom: 12rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}
.cert-item-main {
  flex: 1;
  min-width: 0;
}
.cert-item.active {
  border-color: #3B82F6;
  background: rgba(59, 130, 246, 0.08);
}
.cert-item-title {
  display: block;
  font-size: 26rpx;
  color: #1E293B;
}
.cert-item-sub {
  display: block;
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #64748B;
}
.cert-item-edit-btn {
  flex-shrink: 0;
  min-width: 120rpx;
  height: 56rpx;
  border: 1rpx solid #0F4C81;
  color: #0F4C81;
  border-radius: 8rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
}
.cert-add-btn {
  margin-top: 18rpx;
  height: 80rpx;
  border: 1rpx solid #0F4C81;
  color: #0F4C81;
  border-radius: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cert-form {
  margin-top: 10rpx;
  max-height: 58vh;
  overflow: auto;
}
.cert-form-item {
  margin-bottom: 18rpx;
}
.cert-form-label {
  display: block;
  margin-bottom: 8rpx;
  font-size: 24rpx;
  color: #334155;
}
.cert-picker-box,
.cert-input-box {
  border: 1rpx solid #CBD5E1;
  border-radius: 10rpx;
  padding: 0 16rpx;
  min-height: 72rpx;
  display: flex;
  align-items: center;
  color: #1E293B;
  font-size: 24rpx;
  background: #fff;
}
.cert-input-box {
  height: 72rpx;
}
.cert-picker-select {
  justify-content: space-between;
}
.cert-picker-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cert-picker-arrow {
  font-size: 20rpx;
  color: #64748B;
  margin-left: 12rpx;
}
.cert-upload-btn {
  height: 72rpx;
  border-radius: 10rpx;
  background: #0F4C81;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
}
.cert-upload-hint {
  margin-top: 8rpx;
  display: block;
  color: #64748B;
  font-size: 21rpx;
  line-height: 1.5;
}
.cert-file-list {
  margin-top: 8rpx;
}
.cert-file-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1rpx solid #E2E8F0;
  border-radius: 8rpx;
  padding: 10rpx 12rpx;
  margin-bottom: 8rpx;
}
.cert-file-name {
  flex: 1;
  font-size: 22rpx;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 16rpx;
}
.cert-remove-btn {
  font-size: 22rpx;
  color: #EF4444;
}
.cert-submit-btn {
  height: 76rpx;
  border-radius: 10rpx;
  background: #10B981;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 600;
  margin-top: 8rpx;
}
.cert-submit-btn.disabled {
  opacity: 0.6;
}

/* 报价 */
.quote-section {
  background: rgba(245, 158, 11, 0.08);
  border: 1rpx solid rgba(245, 158, 11, 0.2);
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}
.quote-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}
.quote-breakdown {
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12rpx;
  padding: 16rpx;
  margin-bottom: 16rpx;
}
.breakdown-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}
.breakdown-item:last-child {
  margin-bottom: 0;
}
.bd-label {
  font-size: 24rpx;
  color: #64748B;
}
.bd-val {
  font-size: 24rpx;
  color: #F59E0B;
  font-weight: 500;
}
.bd-val.negative {
  color: #10B981;
}
.quote-label { font-size: 26rpx; color: #475569; }
.quote-val { font-size: 32rpx; font-weight: 700; color: #F59E0B; }
.quote-val.total { font-size: 40rpx; }
.quote-line {
  height: 1rpx;
  background: rgba(245, 158, 11, 0.15);
  margin: 8rpx 0;
}

/* 底部 */
.bottom-bar {
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  margin-top: 16rpx;
  margin-bottom: 40rpx;
}
/* #ifdef MP-WEIXIN */
.page-container {
  padding-bottom: calc(150rpx + env(safe-area-inset-bottom));
}
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 60;
  border-radius: 0;
  border-left: 0;
  border-right: 0;
  border-bottom: 0;
  margin: 0;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom));
}
/* #endif */
.bar-left { flex: 1; }
.bar-hint { font-size: 26rpx; color: #475569; }
.bar-small { display: block; font-size: 22rpx; color: #64748B; }
.bar-total { display: block; font-size: 36rpx; font-weight: 700; color: #F59E0B; }
.bar-actions {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.bar-config-code-btn {
  height: 82rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;
  border: 1rpx solid #0F4C81;
  color: #0F4C81;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FFFFFF;
}
.bar-config-code-text {
  font-size: 24rpx;
  font-weight: 600;
}
.bar-action {
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  padding: 20rpx 40rpx;
  border-radius: 14rpx;
  box-shadow: 0 4rpx 16rpx rgba(59, 130, 246, 0.3);
}
.bar-action:active { opacity: 0.85; }
.bar-action.disabled { opacity: 0.5; }
.action-text {
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  letter-spacing: 2rpx;
}

.add-cart-popup-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}
.add-cart-popup {
  width: 560rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 32rpx 28rpx 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.add-cart-popup-icon {
  width: 84rpx;
  height: 84rpx;
}
.add-cart-popup-title {
  margin-top: 14rpx;
  font-size: 32rpx;
  color: #0F172A;
  font-weight: 600;
}
.add-cart-popup-actions {
  width: 100%;
  margin-top: 26rpx;
  display: flex;
  gap: 16rpx;
}
.popup-btn {
  flex: 1;
  height: 78rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 500;
}
.popup-btn-secondary {
  color: #334155;
  background: #F1F5F9;
}
.popup-btn-primary {
  color: #FFFFFF;
  background: #0F4C81;
}

@media screen and (min-width: 768px) {
  .bottom-bar {
    max-width: 1200px;
    margin: 0 auto;
    left: 0;
    right: 0;
  }
  .page-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 24rpx;
  }
  .input-wrap {
    max-width: 400px;
  }
}

@media screen and (min-width: 992px) {
  .cert-popup-mask {
    align-items: stretch;
    justify-content: flex-end;
  }
  .cert-popup.desktop {
    width: 25vw;
    min-width: 380px;
    max-width: 520px;
    height: 100vh;
    max-height: none;
    border-radius: 0;
    padding: 28rpx 24rpx;
    display: flex;
    flex-direction: column;
  }
  .cert-popup-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }
  .cert-list {
    flex: 1;
    max-height: none;
  }
  .cert-form {
    flex: 1;
    max-height: none;
  }
}
</style>
