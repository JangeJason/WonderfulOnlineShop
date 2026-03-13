<template>
  <view class="address-container">
    <OfficialHeader />
    <view class="content-wrapper">
      <view class="form-container">
        <text class="page-title">{{ isEdit ? '编辑收货地址' : '新增收货地址' }}</text>
        
        <view class="form-content">
          <uni-forms ref="formRef" :modelValue="formData" :rules="rules" label-position="top">
            <uni-forms-item label="收货人姓名" name="receiverName" required>
              <uni-easyinput v-model="formData.receiverName" placeholder="请输入收货人真实姓名" />
            </uni-forms-item>
            
            <uni-forms-item label="手机号码" name="phone" required>
              <uni-easyinput v-model="formData.phone" type="number" placeholder="请输入11位手机号码" />
            </uni-forms-item>
            
            <view class="region-group">
              <uni-forms-item label="省份" name="province" required>
                <uni-easyinput v-model="formData.province" placeholder="如：广东省" />
              </uni-forms-item>
              <uni-forms-item label="城市" name="city" required>
                <uni-easyinput v-model="formData.city" placeholder="如：深圳市" />
              </uni-forms-item>
              <uni-forms-item label="区/县" name="district" required>
                <uni-easyinput v-model="formData.district" placeholder="如：南山区" />
              </uni-forms-item>
            </view>
            
            <uni-forms-item label="详细地址" name="detailAddress" required>
              <uni-easyinput type="textarea" v-model="formData.detailAddress" placeholder="街道、小区楼栋号等详细信息" />
            </uni-forms-item>
            
            <uni-forms-item label="设为默认地址" name="isDefault">
              <switch :checked="formData.isDefault" @change="onDefaultChange" color="#004178" />
            </uni-forms-item>
          </uni-forms>
          
          <view class="btn-group">
            <button class="cancel-btn" @click="goBack">取消</button>
            <button class="save-btn" @click="saveAddress" :loading="saving">保存</button>
          </view>
        </view>
      </view>
    </view>
    <OfficialFooter />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import type { Address } from '@/api/address';
import { addAddress, updateAddress } from '@/api/address';
import OfficialHeader from '@/components/OfficialHeader/OfficialHeader.vue';
import OfficialFooter from '@/components/OfficialFooter/OfficialFooter.vue';

const formRef = ref<any>(null);
const isEdit = ref(false);
const saving = ref(false);

const formData = ref<Address>({
  receiverName: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
});

const rules = {
  receiverName: { rules: [{ required: true, errorMessage: '请输入收货人姓名' }] },
  phone: { 
    rules: [
      { required: true, errorMessage: '请输入手机号码' },
      { pattern: '^1[3-9]\\d{9}$', errorMessage: '请输入正确的手机号码' }
    ] 
  },
  province: { rules: [{ required: true, errorMessage: '请输入省份' }] },
  city: { rules: [{ required: true, errorMessage: '请输入城市' }] },
  district: { rules: [{ required: true, errorMessage: '请输入区/县' }] },
  detailAddress: { rules: [{ required: true, errorMessage: '请输入详细地址' }] },
};

onLoad((options: any) => {
  if (options && options.id && options.data) {
    isEdit.value = true;
    try {
      const data = JSON.parse(decodeURIComponent(options.data));
      formData.value = { ...data };
    } catch (e) {
      console.error('Failed to parse address data', e);
    }
  }
});

const onDefaultChange = (e: any) => {
  formData.value.isDefault = e.detail.value;
};

const goBack = () => {
  uni.navigateBack();
};

const saveAddress = async () => {
  try {
    await formRef.value.validate();
    
    saving.value = true;
    if (isEdit.value) {
      await updateAddress(formData.value.id!, formData.value);
      uni.showToast({ title: '修改成功', icon: 'success' });
    } else {
      await addAddress(formData.value);
      uni.showToast({ title: '添加成功', icon: 'success' });
    }
    
    // Notify previous page to refresh
    uni.$emit('addressUpdated');
    
    setTimeout(() => {
      goBack();
    }, 1500);
  } catch (error: any) {
    if (error && error.length > 0) {
      // Validate error
      return;
    }
    uni.showToast({ title: '保存失败', icon: 'none' });
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.address-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.content-wrapper {
  flex: 1;
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
  box-sizing: border-box;
}

.form-container {
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  display: block;
  margin-bottom: 30px;
  text-align: center;
}

.form-content {
  max-width: 600px;
  margin: 0 auto;
}

.region-group {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 15px;
}

.btn-group {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
}

.cancel-btn, .save-btn {
  width: 160px;
  height: 44px;
  line-height: 44px;
  font-size: 16px;
  border-radius: 4px;
  margin: 0;
}

.cancel-btn {
  background-color: #f5f7fa;
  color: #606266;
  border: 1px solid #dcdfe6;
}

.save-btn {
  background-color: #004178;
  color: white;
  border: none;
}

@media (max-width: 768px) {
  .content-wrapper {
    padding: 20px 10px;
  }
  
  .form-container {
    padding: 20px 15px;
  }
  
  .region-group {
    grid-template-columns: 1fr;
    gap: 0;
  }
  
  .btn-group {
    flex-direction: column;
  }
  
  .cancel-btn, .save-btn {
    width: 100%;
  }
}
</style>
