# Wonderful Online Shop - Handover Document

## 1. 项目简介 (Project Overview)
本项目是一个基于动态计价和参数可选的在线印刷/包装定制商城。系统由管理后台 (Admin Web)、用户小程序端 (Client UniApp) 以及核心业务服务端 (Spring Boot) 三个主要部分构成。它允许管理员创建复杂的商品、自定义计价公式、并为每个定制参数配置带图选项和价格浮动策略。

## 2. 技术栈 (Tech Stack)
*   **Backend (服务端)**: Java 17, Spring Boot 3.x, MyBatis-Plus, Sa-Token (鉴权体系), HikariCP, MySQL 8
*   **Admin Web (管理后台)**: Vue 3 (Composition API), Vite, TypeScript, Ant Design Vue (UI 框架)
*   **Client UniApp (用户客户端)**: Vue 3, UniApp (支持编译至微信小程序及 H5)

## 3. 代码库结构 (Repository Structure)
整个项目基于 Monorepo/多模块平行结构划分：
*   `/backend/` - Spring Boot 后端工程代码
    *   `/src/main/java/com/wonderful/onlineshop/product/` - 商品、分类、参数配置等核心实体与数据层
    *   `/src/main/java/com/wonderful/onlineshop/quote/` - 动态计价引擎服务
    *   `/src/main/java/com/wonderful/onlineshop/controller/` - API 路由接口层
*   `/admin-web/` - Vue 3 后台管理系统
    *   `/src/views/` - 后台核心页面（如 [ProductParametersView.vue](file:///Users/jangejason/Documents/Jange/%E6%AF%95%E4%B8%9A%E8%AE%BE%E8%AE%A1/WonderfulOnlineShop/admin-web/src/views/ProductParametersView.vue) 参数选项配置）
*   `/client-uniapp/` - 面向买家的移动端应用
    *   `/src/pages/index/` - 首页（包含智能大类级联展示）
    *   `/src/pages/product-detail/` - 商品详情页与动态选配计价界面

## 4. 核心功能及实现逻辑 (Core Features & Logic)

### 4.1 动态计价系统 (Dynamic Pricing)
商品的价格不依赖单一固定值，而是通过 SpEL 表达式（计价公式引擎）动态计算。
*   **计价参数 ([ProductParameter](file:///Users/jangejason/Documents/Jange/%E6%AF%95%E4%B8%9A%E8%AE%BE%E8%AE%A1/WonderfulOnlineShop/admin-web/src/api/parameter.ts#3-14))**: 支持 `INPUT` (用户手动输入, 如长/宽数量) 和 `SELECT` (列表勾选, 如材质/工艺)。
*   **动态公式**: 后台 [QuoteService.java](file:///Users/jangejason/Documents/Jange/%E6%AF%95%E4%B8%9A%E8%AE%BE%E8%AE%A1/WonderfulOnlineShop/backend/src/main/java/com/wonderful/onlineshop/quote/service/QuoteService.java) 使用 Spring SpEL 实时执行绑定在 [Product](file:///Users/jangejason/Documents/Jange/%E6%AF%95%E4%B8%9A%E8%AE%BE%E8%AE%A1/WonderfulOnlineShop/admin-web/src/api/product.ts#3-15) 上的公式（例如 `#L * #W * 0.01`），并自动叠加各个选择型选项带来的正负基础价格偏移量 `priceAdjustment`。

### 4.2 商品分类展示 (Category Display)
*   **前端智能分组 ([index.vue](file:///Users/jangejason/Documents/Jange/%E6%AF%95%E4%B8%9A%E8%AE%BE%E8%AE%A1/WonderfulOnlineShop/client-uniapp/src/pages/index/index.vue))**: 用户在客户端点击一级“大分类”时，系统会在前端并发请求所有关联的一级子分类，按小标题瀑布流 (Group Header) 形式进行网格化聚合展示，增强导购体验。点击具体细分小类时走常规分页流。

### 4.3 选项图片与文件上传 (Image Upload for Options)
*   选项支持绑定缩略小图，前端会在选择芯片 (`.option-chip`) 以及后台预览表单中采用 `<a-image>` 与 `<image @click.stop>` 机制进行渲染与点击全屏放大。
*   **注意陷阱**: 上传接口 `/api/upload` 实际返回的 JSON Structure 内含属性为 `data.url`。如果直接接收完整的 `data` 会导致后端 Jackson 解析为 `[object Object]` 进行 `String` 匹配时抛出 `Invalid request body` 报错。在 [parameter.ts](file:///Users/jangejason/Documents/Jange/%E6%AF%95%E4%B8%9A%E8%AE%BE%E8%AE%A1/WonderfulOnlineShop/admin-web/src/api/parameter.ts) 与提交逻辑里已进行严格的 `typeof === 'string'` 和 `.url` 清洗摘取。

## 5. 数据库重要修改及演进 (Important DB Alterations)
*   在 `parameter_options` 表中加入了 `image_url (VARCHAR 500)`。
*   系统启动初始化表结构依赖于 [/backend/src/main/resources/schema.sql](file:///Users/jangejason/Documents/Jange/%E6%AF%95%E4%B8%9A%E8%AE%BE%E8%AE%A1/WonderfulOnlineShop/backend/src/main/resources/schema.sql) (如有) 或通过 Mybatis-plus 建表脚本维护。

## 6. 环境运行及维护 (Running & Maintenance)
*   本地开发请确保 MySQL 服务运行，可参阅 [application.yml](file:///Users/jangejason/Documents/Jange/%E6%AF%95%E4%B8%9A%E8%AE%BE%E8%AE%A1/WonderfulOnlineShop/backend/src/main/resources/application.yml) 获取默认数据库密码 (如 `Wujiang520`) 与库名。
*   默认配置 `spring.sql.init.mode=never`，不会自动执行 `schema.sql/data.sql`，如需导入样例数据请手动执行 SQL 文件。
*   提供了统一的脚本 [./start.sh](file:///Users/jangejason/Documents/Jange/%E6%AF%95%E4%B8%9A%E8%AE%BE%E8%AE%A1/WonderfulOnlineShop/start.sh) 和 [./stop.sh](file:///Users/jangejason/Documents/Jange/%E6%AF%95%E4%B8%9A%E8%AE%BE%E8%AE%A1/WonderfulOnlineShop/stop.sh) 来批量启停三大子系统，可一键重启服务。

## 7. 后续开发建议 (Next Steps for Developers)
1. **统一文件存储**: 目前 [FileUploadController](file:///Users/jangejason/Documents/Jange/%E6%AF%95%E4%B8%9A%E8%AE%BE%E8%AE%A1/WonderfulOnlineShop/backend/src/main/java/com/wonderful/onlineshop/controller/FileUploadController.java#16-53) 上传到本地文件系统（`uploads/` 目录）。未来如需部署至多节点分布式服务器，建议替换为 OSS（阿里云OSS / 腾讯云COS / AWS S3）。
2. **公式安全性**: 后端的脚本执行器拥有较高权限。请确保 [AdminProductController](file:///Users/jangejason/Documents/Jange/%E6%AF%95%E4%B8%9A%E8%AE%BE%E8%AE%A1/WonderfulOnlineShop/backend/src/main/java/com/wonderful/onlineshop/controller/AdminProductController.java#15-101) 的价格公式入库时做充分的代码安全性/非法注入检查。
3. **UniApp 小程序适配**: 当前客户端部分页面由于依赖 CSS 网格和嵌套渲染，后续向微信小程序底层编译打包时，建议进行微信开发者工具的实体真机调试以消除潜在的样式/Scroll兼容问题。
