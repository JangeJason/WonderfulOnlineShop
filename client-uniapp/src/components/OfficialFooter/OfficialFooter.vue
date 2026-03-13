<template>
  <view class="official-footer-wrapper">
    <!-- #ifdef H5 -->
    <section class="contact-section">
        <div class="container contact-container">
            <div class="contact-grid">
                <div class="contact-column">
                    <div class="contact-icon">
                        <img src="/static/tabbar/profile.svg" alt="联系图标" />
                    </div>
                    <h4>深圳-吴先生</h4>
                    <p>电话:+86 135 0284 2742</p>
                    <p class="Email">Email: huandifu@vip.163.com</p>
                </div>
                <!-- 中间列 -->
                <div class="contact-column center">
                    <h2 class="desktop-only-title">深圳市环地福纸制品有限公司</h2>
                    <h3 class="desktop-only-title">HongKong Wonderful Printing Limited</h3>
                    <h2 class="mobile-only-title">联系环地福</h2>
                    <h3 class="mobile-only-title">Contact with WONDERFUL</h3>
                    <p class="address">深圳市龙岗区龙东银龙工业城A4栋</p>
                    <p class="Engaddress">Building A4, Yinlong Industrial Zone,LongDong, LongGang District, Shenzhen, China.</p>
                </div>
                <div class="contact-column">
                    <div class="contact-icon">
                        <img src="/static/tabbar/profile.svg" alt="联系图标" />
                    </div>
                    <h4>深圳-杨先生</h4>
                    <p>电话:+86 </p>
                    <p class="Email">Email:1234567@qq.com</p>
                </div>
            </div>
        </div>
    </section>

    <footer class="main-footer">
        <div class="container footer-container">
            <div class="footer-grid">
                <div class="footer-column">
                    <ul>
                        <li><a href="#">公司介绍</a></li>
                        <li><a href="#">设计</a></li>
                        <li><a href="#">印刷</a></li>
                        <li><a href="#">印后</a></li>
                        <li><a href="#">产品</a></li>
                        <li><a href="#">工艺</a></li>
                        <li><a href="#">服务</a></li>
                    </ul>
                </div>
                <div class="footer-column">
                    <ul>
                        <li><a href="#">全球网站</a></li>
                        <li><a href="#">隐私声明</a></li>
                        <li><a href="#">法律声明</a></li>
                    </ul>
                </div>
                <div class="footer-column">
                    <ul>
                        <li><a href="#">备用链接 1</a></li>
                        <li><a href="#">备用链接 2</a></li>
                    </ul>
                </div>
            </div>
        </div>
    </footer>

    <div class="copyright-bar">
        <small>© 2025 深圳市环地福纸制品有限公司</small>
    </div>
    <!-- #endif -->

    <!-- #ifndef H5 -->
    <view class="mp-footer">
      <view class="mp-company">深圳市环地福纸制品有限公司</view>
      <view class="mp-address">深圳市龙岗区龙东银龙工业城A4栋</view>
      <view class="mp-contact-row">
        <text>吴先生: +86 135 0284 2742</text>
      </view>
      <view class="mp-contact-row">
        <text>邮箱: huandifu@vip.163.com</text>
      </view>
      <view class="mp-copyright">© 2025 深圳市环地福纸制品有限公司</view>
    </view>
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
</script>

<style scoped>
/* #ifdef H5 */
.official-footer-wrapper {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  width: 100%;
  background-color: #fff;
  color: #333;
}
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
}
.contact-section {
  background-color: #f0f5fa;
  padding: 60px 0;
  margin-top: 40px;
}
.contact-container { display: block; }
.contact-grid {
  display: grid;
  grid-template-columns: 1fr 1.5fr 1fr;
  align-items: center;
  gap: 30px;
}
.contact-column { text-align: center; }
.contact-column.center {
  display: flex; flex-direction: column; align-items: center;
}
.contact-logo { max-height: 60px; max-width: 100%; }
.contact-column h2 { font-size: 24px; color: black; margin: 20px 0 0 0; font-weight: bold; }
.contact-column h3 { font-size: 18px; color: black; margin: 4px 0 0 0; font-weight: bold; }
.contact-column h4 { font-size: 20px; color: #004178; margin: 15px 0; }
.contact-column p { font-size: 14px; color: #555; margin: 10px 0 0 0; }
.contact-column .Engaddress { margin-top: 4px; font-size: 12px; }
.contact-column .Email { font-size: 14px; color: #555; margin: 4px 0 10px 0; }
.contact-icon img { width: 36px; height: 36px; }

.desktop-only-title { display: block; }
.mobile-only-title { display: none; }

.main-footer {
  background-color: #fff;
  color: #555;
  padding: 40px 0;
  margin-top: 0;
}
.footer-container { display: block; }
.footer-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
  border-top: 1px solid #e1e6eb;
  padding-top: 40px;
}
.footer-column h4 { display: none; }
.footer-column ul { list-style: none; margin: 0; padding: 0; }
.footer-column li { margin-bottom: 12px; }
.footer-column a {
  font-size: 14px; color: #555; text-decoration: none; transition: color 0.2s ease;
}
.footer-column a:hover { color: #004178; text-decoration: underline; }

.copyright-bar {
  padding: 10px 0;
  text-align: center;
  color: #fff;
  background: linear-gradient(to right, #005a9e, #3a9d5d);
}
.copyright-bar small { font-size: 13px; font-weight: 500; }
/* #endif */

/* #ifndef H5 */
.mp-footer {
  background: #fff;
  border-top: 1rpx solid #e2e8f0;
  padding: 24rpx;
  text-align: center;
}
.mp-company {
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
}
.mp-address {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #64748b;
}
.mp-contact-row {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #334155;
}
.mp-copyright {
  margin-top: 12rpx;
  font-size: 20rpx;
  color: #94a3b8;
}
/* #endif */

/* #ifdef H5 */
@media screen and (max-width: 768px) {
  .contact-section { padding: 40px 0; margin-top: 20px; }
  .contact-grid { grid-template-columns: 1fr; gap: 40px; }
  .contact-column.center { order: -1; }
  .footer-grid { grid-template-columns: 1fr; text-align: center; }
  .desktop-only-title { display: none !important; }
  .mobile-only-title { display: block !important; margin-bottom: 10px; }
}
/* #endif */
</style>
