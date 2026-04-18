<script setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Copy, Check, Code, Shield, Zap, Wrench } from 'lucide-vue-next'
import { toast } from '@/utils/toast'

const { t } = useI18n()
const copiedSection = ref(null)
const selectedPlatform = ref('windows')

const handleCopy = async (text, section) => {
  try {
    await navigator.clipboard.writeText(text)
    copiedSection.value = section
    toast.success(t('common.success'))
    setTimeout(() => {
      copiedSection.value = null
    }, 2000)
  } catch (err) {
    const textArea = document.createElement('textarea')
    textArea.value = text
    textArea.style.position = 'fixed'
    textArea.style.left = '-999999px'
    textArea.style.top = '-999999px'
    document.body.appendChild(textArea)
    textArea.focus()
    textArea.select()
    try {
      document.execCommand('copy')
      copiedSection.value = section
      toast.success(t('common.success'))
      setTimeout(() => {
        copiedSection.value = null
      }, 2000)
    } catch (e) {
      toast.error(t('common.error'))
    } finally {
      document.body.removeChild(textArea)
    }
  }
}

const baseUrl = 'https://ai.xlinks.site/v1/'
const windowsConfigPath = '%userprofile%/.codex/config.toml'
const windowsAuthPath = '%userprofile%/.codex/auth.json'
const unixConfigPath = '~/.codex/config.toml'
const unixAuthPath = '~/.codex/auth.json'
const windowsOpencodePath = '%userprofile%/.config/opencode/opencode.jsonc'
const unixOpencodePath = '~/.config/opencode/opencode.jsonc'

const codexConfig = `model_provider = "OpenAI"
model = "gpt-5.4"
review_model = "gpt-5.4"
model_reasoning_effort = "xhigh"
disable_response_storage = true
network_access = "enabled"
windows_wsl_setup_acknowledged = true
model_context_window = 1000000
model_auto_compact_token_limit = 900000

[model_providers.OpenAI]
name = "OpenAI"
base_url = "https://ai.xlinks.site/v1/"
wire_api = "responses"
requires_openai_auth = true`

const codexAuth = `{
  "OPENAI_API_KEY": "YOUR_API_KEY"
}`

const opencodeConfig = `{
  "$schema": "https://opencode.ai/config.json",
  "disabled_providers": [],
  "provider": {
    "xlinks": {
      "name": "xlinks",
      "npm": "@ai-sdk/openai-compatible",
      "models": {
        "gpt-5.4": {
          "name": "gpt-5.4"
        }
      },
      "options": {
        "baseURL": "https://ai.xlinks.site/v1/",
        "apiKey": "YOUR_API_KEY"
      }
    }
  }
}`

const platformFiles = {
  windows: [
    { key: 'config', name: 'config.toml', path: windowsConfigPath },
    { key: 'auth', name: 'auth.json', path: windowsAuthPath },
  ],
  unix: [
    { key: 'config', name: 'config.toml', path: unixConfigPath },
    { key: 'auth', name: 'auth.json', path: unixAuthPath },
  ],
}

const tocSections = computed(() => [
  { id: 'quick-start', label: t('docs.tocQuickStart') },
  { id: 'api-config', label: t('docs.tocApiConfig') },
  { id: 'codex-config', label: t('docs.tocCodexConfig') },
  { id: 'opencode-config', label: t('docs.tocOpencodeConfig') },
  { id: 'model-config', label: t('docs.tocModelConfig') },
  { id: 'troubleshooting', label: t('docs.tocTroubleshooting') },
  { id: 'notice', label: t('docs.tocNotice') },
])
</script>

<template>
  <div class="p-4 md:p-8 max-w-7xl mx-auto">
    <div class="grid grid-cols-1 lg:grid-cols-[240px_minmax(0,1fr)] gap-6">
      <aside class="hidden lg:block">
        <div class="sticky top-24 bg-white rounded-2xl border border-slate-200 p-4 shadow-sm">
          <h3 class="text-sm font-semibold text-slate-900 mb-3">{{ t('docs.tocTitle') }}</h3>
          <nav class="space-y-1">
            <a
              v-for="section in tocSections"
              :key="section.id"
              :href="`#${section.id}`"
              class="block rounded-lg px-3 py-2 text-sm text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors"
            >
              {{ section.label }}
            </a>
          </nav>
        </div>
      </aside>

      <main class="space-y-6">
        <section id="quick-start" class="scroll-mt-24 bg-gradient-hero rounded-3xl p-8 text-white shadow-2xl">
          <div class="flex items-center gap-3 mb-4">
            <div class="w-12 h-12 bg-white/30 rounded-2xl flex items-center justify-center backdrop-blur-sm shadow-lg">
              <Zap class="w-6 h-6 text-white" />
            </div>
            <h2 class="text-2xl font-bold text-white">{{ t('docs.quickStart') }}</h2>
          </div>
          <p class="text-white/95 mb-6 text-base font-medium">{{ t('docs.quickStartDesc') }}</p>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div class="bg-white/20 backdrop-blur-md border-2 border-white/30 rounded-2xl p-4 shadow-lg">
              <div class="text-3xl font-bold mb-2 text-white">1</div>
              <div class="text-sm text-white font-medium">{{ t('docs.step1') }}</div>
            </div>
            <div class="bg-white/20 backdrop-blur-md border-2 border-white/30 rounded-2xl p-4 shadow-lg">
              <div class="text-3xl font-bold mb-2 text-white">2</div>
              <div class="text-sm text-white font-medium">{{ t('docs.step2') }}</div>
            </div>
            <div class="bg-white/20 backdrop-blur-md border-2 border-white/30 rounded-2xl p-4 shadow-lg">
              <div class="text-3xl font-bold mb-2 text-white">3</div>
              <div class="text-sm text-white font-medium">{{ t('docs.step3') }}</div>
            </div>
          </div>
        </section>

        <section id="api-config" class="scroll-mt-24 bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
          <div class="bg-gradient-to-r from-slate-900 to-slate-800 p-6 text-white flex items-center gap-3">
            <Code class="w-6 h-6" />
            <h2 class="text-xl font-bold">{{ t('docs.apiConfig') }}</h2>
          </div>
          <div class="p-6 space-y-6">
            <div>
              <h3 class="text-lg font-semibold text-slate-900 mb-3">Base URL</h3>
              <p class="text-sm text-slate-600 mb-3">{{ t('docs.baseUrlDesc') }}</p>
              <div class="relative bg-slate-900 rounded-xl p-4 font-mono text-sm">
                <code class="text-green-400">{{ baseUrl }}</code>
                <button
                  @click="handleCopy(baseUrl, 'baseUrl')"
                  class="absolute top-3 right-3 p-2 bg-slate-800 hover:bg-slate-700 rounded-lg transition-colors"
                >
                  <Check v-if="copiedSection === 'baseUrl'" class="w-4 h-4 text-green-400" />
                  <Copy v-else class="w-4 h-4 text-slate-400" />
                </button>
              </div>
            </div>

            <div>
              <h3 class="text-lg font-semibold text-slate-900 mb-3">API Key</h3>
              <p class="text-sm text-slate-600 mb-3">{{ t('docs.apiKeyDesc') }}</p>
              <div class="relative bg-slate-900 rounded-xl p-4 font-mono text-sm">
                <code class="text-yellow-400">Authorization: Bearer YOUR_API_KEY</code>
                <button
                  @click="handleCopy('Authorization: Bearer YOUR_API_KEY', 'apiKey')"
                  class="absolute top-3 right-3 p-2 bg-slate-800 hover:bg-slate-700 rounded-lg transition-colors"
                >
                  <Check v-if="copiedSection === 'apiKey'" class="w-4 h-4 text-green-400" />
                  <Copy v-else class="w-4 h-4 text-slate-400" />
                </button>
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <h4 class="text-sm font-semibold text-slate-900 mb-2">{{ t('docs.endpointTitle') }}</h4>
                <ul class="space-y-1 text-sm text-slate-600">
                  <li><code class="text-xs bg-white px-2 py-0.5 rounded">POST /chat/completions</code></li>
                  <li><code class="text-xs bg-white px-2 py-0.5 rounded">POST /responses</code></li>
                  <li><code class="text-xs bg-white px-2 py-0.5 rounded">GET /models</code></li>
                </ul>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <h4 class="text-sm font-semibold text-slate-900 mb-2">{{ t('docs.authTipsTitle') }}</h4>
                <ul class="space-y-1 text-sm text-slate-600">
                  <li>{{ t('docs.authTip1') }}</li>
                  <li>{{ t('docs.authTip2') }}</li>
                  <li>{{ t('docs.authTip3') }}</li>
                </ul>
              </div>
            </div>
          </div>
        </section>

        <section id="codex-config" class="scroll-mt-24 bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
          <div class="bg-gradient-to-r from-slate-900 to-slate-800 p-6 text-white flex items-center gap-3">
            <Code class="w-6 h-6" />
            <h2 class="text-xl font-bold">{{ t('docs.codexClientConfig') }}</h2>
          </div>
          <div class="p-6 space-y-6">
            <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-2">
              <div class="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  @click="selectedPlatform = 'windows'"
                  :class="selectedPlatform === 'windows'
                    ? 'bg-slate-900 text-white shadow-sm'
                    : 'bg-transparent text-slate-600 hover:bg-white hover:text-slate-900'"
                  class="rounded-xl px-4 py-3 text-sm font-semibold transition-colors"
                >
                  {{ t('docs.windows') }}
                </button>
                <button
                  type="button"
                  @click="selectedPlatform = 'unix'"
                  :class="selectedPlatform === 'unix'
                    ? 'bg-slate-900 text-white shadow-sm'
                    : 'bg-transparent text-slate-600 hover:bg-white hover:text-slate-900'"
                  class="rounded-xl px-4 py-3 text-sm font-semibold transition-colors"
                >
                  {{ t('docs.macosLinux') }}
                </button>
              </div>
            </div>

            <div>
              <h3 class="text-lg font-semibold text-slate-900 mb-3">{{ t('docs.codexStepConfig') }}</h3>
              <div class="mb-3 flex items-center gap-2">
                <code class="flex-1 rounded-xl bg-slate-100 px-3 py-2 text-xs text-slate-700 break-all">
                  {{ platformFiles[selectedPlatform][0].path }}
                </code>
                <button
                  @click="handleCopy(platformFiles[selectedPlatform][0].path, `${selectedPlatform}-config-path`)"
                  class="shrink-0 rounded-xl border border-slate-200 bg-white px-3 py-2 text-slate-600 transition-colors hover:bg-slate-50"
                >
                  <Check
                    v-if="copiedSection === `${selectedPlatform}-config-path`"
                    class="w-4 h-4 text-green-500"
                  />
                  <Copy v-else class="w-4 h-4" />
                </button>
              </div>
              <div class="relative bg-slate-900 rounded-xl p-4 font-mono text-sm overflow-x-auto">
                <pre class="text-slate-300">{{ codexConfig }}</pre>
                <button
                  @click="handleCopy(codexConfig, 'codexConfig')"
                  class="absolute top-3 right-3 p-2 bg-slate-800 hover:bg-slate-700 rounded-lg transition-colors"
                >
                  <Check v-if="copiedSection === 'codexConfig'" class="w-4 h-4 text-green-400" />
                  <Copy v-else class="w-4 h-4 text-slate-400" />
                </button>
              </div>
            </div>

            <div>
              <h3 class="text-lg font-semibold text-slate-900 mb-3">{{ t('docs.codexStepAuth') }}</h3>
              <div class="mb-3 flex items-center gap-2">
                <code class="flex-1 rounded-xl bg-slate-100 px-3 py-2 text-xs text-slate-700 break-all">
                  {{ platformFiles[selectedPlatform][1].path }}
                </code>
                <button
                  @click="handleCopy(platformFiles[selectedPlatform][1].path, `${selectedPlatform}-auth-path`)"
                  class="shrink-0 rounded-xl border border-slate-200 bg-white px-3 py-2 text-slate-600 transition-colors hover:bg-slate-50"
                >
                  <Check
                    v-if="copiedSection === `${selectedPlatform}-auth-path`"
                    class="w-4 h-4 text-green-500"
                  />
                  <Copy v-else class="w-4 h-4" />
                </button>
              </div>
              <div class="relative bg-slate-900 rounded-xl p-4 font-mono text-sm overflow-x-auto">
                <pre class="text-slate-300">{{ codexAuth }}</pre>
                <button
                  @click="handleCopy(codexAuth, 'codexAuth')"
                  class="absolute top-3 right-3 p-2 bg-slate-800 hover:bg-slate-700 rounded-lg transition-colors"
                >
                  <Check v-if="copiedSection === 'codexAuth'" class="w-4 h-4 text-green-400" />
                  <Copy v-else class="w-4 h-4 text-slate-400" />
                </button>
              </div>
            </div>
          </div>
        </section>

        <section id="opencode-config" class="scroll-mt-24 bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
          <div class="bg-gradient-to-r from-slate-900 to-slate-800 p-6 text-white flex items-center gap-3">
            <Code class="w-6 h-6" />
            <h2 class="text-xl font-bold">{{ t('docs.opencodeConfig') }}</h2>
          </div>
          <div class="p-6 space-y-6">
            <div class="rounded-2xl border border-slate-200 bg-slate-50/70 p-2">
              <div class="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  @click="selectedPlatform = 'windows'"
                  :class="selectedPlatform === 'windows'
                    ? 'bg-slate-900 text-white shadow-sm'
                    : 'bg-transparent text-slate-600 hover:bg-white hover:text-slate-900'"
                  class="rounded-xl px-4 py-3 text-sm font-semibold transition-colors"
                >
                  {{ t('docs.windows') }}
                </button>
                <button
                  type="button"
                  @click="selectedPlatform = 'unix'"
                  :class="selectedPlatform === 'unix'
                    ? 'bg-slate-900 text-white shadow-sm'
                    : 'bg-transparent text-slate-600 hover:bg-white hover:text-slate-900'"
                  class="rounded-xl px-4 py-3 text-sm font-semibold transition-colors"
                >
                  {{ t('docs.macosLinux') }}
                </button>
              </div>
            </div>
            <div>
              <h3 class="text-lg font-semibold text-slate-900 mb-3">{{ t('docs.opencodeStepConfig') }}</h3>
              <div class="mb-3 flex items-center gap-2">
                <code class="flex-1 rounded-xl bg-slate-100 px-3 py-2 text-xs text-slate-700 break-all">
                  {{ selectedPlatform === 'windows' ? windowsOpencodePath : unixOpencodePath }}
                </code>
                <button
                  @click="handleCopy(selectedPlatform === 'windows' ? windowsOpencodePath : unixOpencodePath, `${selectedPlatform}-opencode-path`)"
                  class="shrink-0 rounded-xl border border-slate-200 bg-white px-3 py-2 text-slate-600 transition-colors hover:bg-slate-50"
                >
                  <Check
                    v-if="copiedSection === `${selectedPlatform}-opencode-path`"
                    class="w-4 h-4 text-green-500"
                  />
                  <Copy v-else class="w-4 h-4" />
                </button>
              </div>
              <div class="relative bg-slate-900 rounded-xl p-4 font-mono text-sm overflow-x-auto">
                <pre class="text-slate-300">{{ opencodeConfig }}</pre>
                <button
                  @click="handleCopy(opencodeConfig, 'opencodeConfig')"
                  class="absolute top-3 right-3 p-2 bg-slate-800 hover:bg-slate-700 rounded-lg transition-colors"
                >
                  <Check v-if="copiedSection === 'opencodeConfig'" class="w-4 h-4 text-green-400" />
                  <Copy v-else class="w-4 h-4 text-slate-400" />
                </button>
              </div>
            </div>
          </div>
        </section>

        <section id="model-config" class="scroll-mt-24 bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
          <div class="bg-gradient-to-r from-slate-900 to-slate-800 p-6 text-white flex items-center gap-3">
            <Shield class="w-6 h-6" />
            <h2 class="text-xl font-bold">{{ t('docs.modelConfig') }}</h2>
          </div>
          <div class="p-6">
            <div class="space-y-4">
              <div class="flex items-start gap-3">
                <div class="w-6 h-6 bg-primary/10 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                  <Check class="w-4 h-4 text-primary" />
                </div>
                <div>
                  <h4 class="font-semibold text-slate-900 mb-1">{{ t('docs.supportedModels') }}</h4>
                  <p class="text-sm text-slate-600">{{ t('docs.supportedModelsDesc') }}</p>
                </div>
              </div>

              <div class="flex items-start gap-3">
                <div class="w-6 h-6 bg-primary/10 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                  <Check class="w-4 h-4 text-primary" />
                </div>
                <div>
                  <h4 class="font-semibold text-slate-900 mb-1">{{ t('docs.recommendedModels') }}</h4>
                  <p class="text-sm text-slate-600 mb-2">{{ t('docs.recommendedModelsDesc') }}</p>
                  <ul class="text-sm text-slate-600 space-y-1 ml-4">
                    <li><code class="text-xs bg-slate-100 px-2 py-0.5 rounded">gpt-5.2</code> - {{ t('docs.haikuDesc') }}</li>
                    <li><code class="text-xs bg-slate-100 px-2 py-0.5 rounded">gpt-5.3</code> - {{ t('docs.sonnetDesc') }}</li>
                    <li><code class="text-xs bg-slate-100 px-2 py-0.5 rounded">gpt-5.4</code> - {{ t('docs.opusDesc') }}</li>
                  </ul>
                </div>
              </div>

              <div class="flex items-start gap-3">
                <div class="w-6 h-6 bg-primary/10 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                  <Check class="w-4 h-4 text-primary" />
                </div>
                <div>
                  <h4 class="font-semibold text-slate-900 mb-1">{{ t('docs.apiCompatibility') }}</h4>
                  <p class="text-sm text-slate-600">{{ t('docs.apiCompatibilityDesc') }}</p>
                </div>
              </div>

              <div class="flex items-start gap-3">
                <div class="w-6 h-6 bg-primary/10 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                  <Check class="w-4 h-4 text-primary" />
                </div>
                <div>
                  <h4 class="font-semibold text-slate-900 mb-1">{{ t('docs.concurrencyLimit') }}</h4>
                  <p class="text-sm text-slate-600">{{ t('docs.concurrencyLimitDesc') }}</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section id="troubleshooting" class="scroll-mt-24 bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden">
          <div class="bg-gradient-to-r from-slate-900 to-slate-800 p-6 text-white flex items-center gap-3">
            <Wrench class="w-6 h-6" />
            <h2 class="text-xl font-bold">{{ t('docs.tocTroubleshooting') }}</h2>
          </div>
          <div class="p-6 space-y-4">
            <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
              <h4 class="text-sm font-semibold text-slate-900 mb-1">401 Unauthorized</h4>
              <p class="text-sm text-slate-600">{{ t('docs.trouble401Desc') }}</p>
            </div>
            <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
              <h4 class="text-sm font-semibold text-slate-900 mb-1">404 / 405</h4>
              <p class="text-sm text-slate-600">{{ t('docs.trouble404Desc') }}</p>
            </div>
            <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
              <h4 class="text-sm font-semibold text-slate-900 mb-1">429 Too Many Requests</h4>
              <p class="text-sm text-slate-600">{{ t('docs.trouble429Desc') }}</p>
            </div>
          </div>
        </section>

        <section id="notice" class="scroll-mt-24 bg-amber-50 border border-amber-200 rounded-2xl p-6">
          <h3 class="text-lg font-semibold text-amber-900 mb-3">{{ t('docs.notice') }}</h3>
          <ul class="space-y-2 text-sm text-amber-800">
            <li class="flex items-start gap-2">
              <span class="text-amber-600 font-bold">*</span>
              <span>{{ t('docs.notice1') }}</span>
            </li>
            <li class="flex items-start gap-2">
              <span class="text-amber-600 font-bold">*</span>
              <span>{{ t('docs.notice2') }}</span>
            </li>
            <li class="flex items-start gap-2">
              <span class="text-amber-600 font-bold">*</span>
              <span>{{ t('docs.notice3') }}</span>
            </li>
            <li class="flex items-start gap-2">
              <span class="text-amber-600 font-bold">*</span>
              <span>{{ t('docs.notice4') }}</span>
            </li>
          </ul>
        </section>
      </main>
    </div>
  </div>
</template>
