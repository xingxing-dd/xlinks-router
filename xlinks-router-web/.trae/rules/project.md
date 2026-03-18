1. 核心理念
像素级完美：严格还原设计稿，注重UI细节。

生产级架构：代码具备可扩展性、可维护性和高性能。

2. 技术栈（强制性）
构建工具：Vite

核心框架：Vue 3 (Composition API + <script setup>)

样式方案：Tailwind CSS + CSS变量

UI组件库：Radix Vue (Headless) + Shadcn-vue

图标库：lucide-vue-next

路由管理：Vue Router 4

状态管理：Pinia

国际化：vue-i18n

3. 编码标准与规则
❌ 禁止行为 (DO NOT DO)
不得修改 Tokenrouter/：将该目录视为只读源，禁止修改、删除或添加任何文件。

禁止使用选项式API：强制使用 <script setup>。

禁止使用React模式：模板中不得出现className、useEffect、useState或Array.map()，应使用Vue对应的语法。

禁止编写“意大利面条式代码”：超过50行的逻辑必须提取到composables/目录下的独立组合式函数中。

禁止硬编码样式：不得使用内联样式，所有样式都需通过Tailwind工具类或CSS变量实现。

✅ 最佳实践 (MUST DO)
命名与结构
页面视图：采用基于文件夹的结构：views/<功能名称>/index.vue。

例如：LoginPage 对应 views/login/index.vue。

页面内部组件：存放于 views/<功能名称>/components/。

共享组件：使用PascalCase.vue命名。

例如：OrderCard.vue。

文件命名：其他所有文件（如JS、组合式函数）使用kebab-case。

例如：order-card.vue, use-order-list.js。

组合式函数：使用camelCase并以“use”开头。

例如：useOrderList。

代码实现
响应式：使用 ref 声明基础类型，使用 reactive 声明对象。

Props 定义：必须使用defineProps并明确进行类型验证（即使在JavaScript项目中）。

条件样式：必须使用clsx 或 cn 工具函数来处理动态类名。

国际化 (i18n)：所有用户界面文本必须通过useI18n()组合式函数提供，键名格式为：模块.功能.描述。

4. React 到 Vue 语法对照表
React (源)	Vue 3 (目标)
className="p-4"	class="p-4"
style={{ color: 'red' }}	:style="{ color: 'red' }"
{isOpen && <Modal />}	<Modal v-if="isOpen" />
items.map(item => <div key={item.id} />)	<div v-for="item in items" :key="item.id">
const [count, setCount] = useState(0)	const count = ref(0)
useEffect(() => {...}, [])	onMounted(() => {...})
useContext(AuthContext)	const authStore = useAuthStore() (从Pinia获取)
5. 项目目录结构参考
text
src/
├── assets/            # 静态资源 (图片, 字体)
├── components/        # 全局共享组件
│   ├── ui/            # Shadcn-vue 基础组件
│   └── common/        # 业务共享组件
├── composables/       # 可复用的组合式逻辑 (useXxx)
├── layouts/           # 布局组件 (如 MainLayout, AuthLayout)
├── locales/           # 国际化语言包
├── router/            # Vue Router 路由配置
├── services/          # API 服务层
├── stores/            # Pinia 状态存储
├── styles/            # 全局样式, Tailwind 指令
├── utils/             # 通用工具函数 (如 cn, request)
├── views/             # 页面级视图
│   └── [feature]/     # 功能模块
│       ├── index.vue  # 主页面
│       └── components/ # 页面私有组件
└── main.js            # 应用入口