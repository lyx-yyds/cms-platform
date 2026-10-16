<template>
  <div class="page-container">
    <h2 class="page-title">{{ isEdit ? '编辑文章' : '新建文章' }}</h2>
    <p class="page-subtitle">稿件工作台 · 富文本编辑器（wangEditor）· 图片上传已接入 MinIO（第五阶段）</p>

    <!-- 编辑卡：白卡无阴影 -->
    <div class="editor-card">
      <el-input
        v-model="form.title"
        placeholder="文章标题"
        size="large"
        class="title-input"
        maxlength="80"
        show-word-limit
      />

      <div class="form-row">
        <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 200px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select
          v-model="form.tagIds"
          placeholder="选择标签（可多选）"
          multiple
          collapse-tags
          clearable
          style="width: 280px"
        >
          <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <el-checkbox v-model="form.isTop">置顶</el-checkbox>
        <el-checkbox v-model="form.isRecommend">推荐</el-checkbox>
      </div>

      <div class="editor-box">
        <Toolbar
          class="editor-toolbar"
          :editor="editorRef"
          :default-config="toolbarConfig"
          mode="simple"
        />
        <Editor
          v-model="editorHtml"
          class="editor-body"
          :default-config="editorConfig"
          mode="simple"
          @on-created="handleCreated"
        />
      </div>

      <div class="summary-row">
        <span class="summary-label">摘要</span>
        <el-input
          v-model="form.summary"
          type="textarea"
          :rows="2"
          placeholder="选填 · 留空将自动截取正文前 200 字"
        />
      </div>

      <div class="form-row">
        <span class="summary-label">封面图</span>
        <el-input
          v-model="form.coverImage"
          placeholder="封面图 URL（可点击右侧上传，白名单 jpg/png/gif/webp ≤5MB）"
          clearable
          style="flex: 1"
        />
        <el-upload :show-file-list="false" :http-request="uploadCover" :accept="'.jpg,.jpeg,.png,.gif,.webp'" :disabled="uploadingCover">
          <el-button :loading="uploadingCover">
            <el-icon><Upload /></el-icon>
            上传封面
          </el-button>
        </el-upload>
      </div>

      <el-collapse class="seo-collapse">
        <el-collapse-item title="SEO 设置（选填）">
          <div class="form-row">
            <el-input v-model="form.seoTitle" placeholder="SEO 标题" style="width: 280px" />
            <el-input v-model="form.seoKeywords" placeholder="SEO 关键词" style="width: 280px" />
          </div>
          <el-input v-model="form.seoDescription" type="textarea" :rows="2" placeholder="SEO 描述" />
        </el-collapse-item>
      </el-collapse>
    </div>

    <!-- 底部操作 -->
    <div class="editor-actions">
      <el-button @click="importMarkdown" :disabled="!editorRef">
        <el-icon><Upload /></el-icon>
        导入 Markdown
      </el-button>
      <el-button type="primary" plain @click="openAiPanel" :disabled="generating">
        <el-icon><MagicStick /></el-icon>
        AI 写作助手
      </el-button>
      <div class="toolbar-spacer" />
      <el-button @click="router.push('/article/list')">取消</el-button>
      <el-button :loading="saving" @click="save(0)">存为草稿</el-button>
      <el-button type="primary" :loading="saving" @click="save(1)">发布</el-button>
    </div>

    <!-- Markdown 导入弹窗 -->
    <el-dialog v-model="mdDialogVisible" title="导入 Markdown" width="560px">
      <el-input
        v-model="mdText"
        type="textarea"
        :rows="12"
        placeholder="粘贴 Markdown 内容，导入后可在编辑器中继续编辑"
      />
      <template #footer>
        <el-button @click="mdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmImport">导入</el-button>
      </template>
    </el-dialog>

    <!-- AI 写作助手抽屉（文档 7.4：草稿/续写/润色 SSE 流式 + 摘要 SEO） -->
    <el-drawer v-model="aiVisible" title="AI 写作助手" size="480px" :close-on-click-modal="false">
      <div class="ai-drawer">
        <p class="ai-model-hint">模型：qwen2.5:3b（Ollama 本地 · DashScope 可用时自动切换）· 每分钟限 5 次</p>
        <el-tabs v-model="aiTab">
          <el-tab-pane label="生成草稿" name="draft">
            <el-input v-model="aiDraft.title" placeholder="标题（留空取当前标题）" />
            <el-input v-model="aiDraft.keywords" placeholder="关键词（逗号分隔）" style="margin-top: 10px" />
            <el-input
              v-model="aiDraft.outline"
              type="textarea"
              :rows="4"
              placeholder="大纲（选填，每行一条）"
              style="margin-top: 10px"
            />
          </el-tab-pane>
          <el-tab-pane label="续写" name="continue">
            <p class="ai-hint">基于当前编辑器内容（取末尾 4000 字）自然续写</p>
            <el-input v-model="aiContinue.instruction" placeholder="附加指令（选填，如「以更口语化的风格」）" />
          </el-tab-pane>
          <el-tab-pane label="润色" name="polish">
            <p class="ai-hint">对当前编辑器全文润色改写，保留原文结构与信息</p>
            <el-input v-model="aiPolish.instruction" placeholder="附加指令（选填，如「更正式、面向技术评审」）" />
          </el-tab-pane>
          <el-tab-pane label="摘要+SEO" name="enhance">
            <p class="ai-hint">根据标题与正文生成摘要与 SEO 元信息，一键回填到表单</p>
          </el-tab-pane>
        </el-tabs>

        <div class="ai-actions">
          <el-button type="primary" :loading="generating" @click="runAi">
            {{ generating ? '生成中…' : (aiTab === 'enhance' ? '生成并回填' : '开始生成') }}
          </el-button>
          <el-button v-if="aiResult && !generating" @click="insertAiResult">
            {{ aiTab === 'continue' ? '追加到正文' : '插入编辑器' }}
          </el-button>
          <el-button v-if="generating" @click="stopAi">停止</el-button>
        </div>

        <div class="ai-output" v-loading="generating" element-loading-text="AI 生成中…">
          <div v-if="aiResult" class="ai-markdown" v-html="renderMarkdown(aiResult)" />
          <el-empty v-else-if="!generating" description="点击生成开始（SSE 逐字渲染）" :image-size="64" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, Upload } from '@element-plus/icons-vue'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { marked } from 'marked'
import {
  createArticleApi,
  getArticleApi,
  listCategoriesApi,
  listTagsApi,
  updateArticleApi,
  type CategoryItem,
  type TagItem
} from '@/api/article'
import { uploadMediaApi } from '@/api/media'
import type { UploadRequestOptions } from 'element-plus'
import {
  bindArticleApi,
  enhanceApi,
  generationDetailApi,
  writeContinueStream,
  writeDraftStream,
  writePolishStream,
  type AiWriteReq
} from '@/api/ai'

const router = useRouter()
const route = useRoute()
const articleId = computed(() => (route.params.id ? Number(route.params.id) : undefined))
const isEdit = computed(() => articleId.value !== undefined)

// ===== 编辑器 =====
const editorRef = shallowRef<IDomEditor>()
const editorHtml = ref('<p><br></p>')
const toolbarConfig: Partial<IToolbarConfig> = {
  // 编辑部克制：隐藏视频上传、附件、全屏
  excludeKeys: ['group-video', 'uploadAttachment', 'fullScreen', 'group-more']
}
const editorConfig: Partial<IEditorConfig> = {
  placeholder: '在这里开始写作……',
  scroll: false,
  MENU_CONF: {
    uploadImage: {
      // 图片上传走 /api/media/upload（MinIO），插入返回 URL（文档 7.2）
      async customUpload(file: File, insertFn: (url: string, alt: string, href: string) => void) {
        const media = await uploadMediaApi(file)
        insertFn(media.url, media.originalName, media.url)
      }
    }
  }
}

function handleCreated(editor: IDomEditor) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) {
    editor.destroy()
  }
})

// ===== 表单 =====
interface ArticleFormState {
  title: string
  categoryId?: number
  tagIds: number[]
  summary?: string
  coverImage?: string
  isTop: boolean
  isRecommend: boolean
  seoTitle?: string
  seoDescription?: string
  seoKeywords?: string
}

const form = reactive<ArticleFormState>({
  title: '',
  categoryId: undefined,
  tagIds: [],
  summary: '',
  coverImage: '',
  isTop: false,
  isRecommend: false,
  seoTitle: '',
  seoDescription: '',
  seoKeywords: ''
})

const categories = ref<CategoryItem[]>([])
const tags = ref<TagItem[]>([])
const saving = ref(false)
const uploadingCover = ref(false)

/** 封面上传：走 /api/media/upload，成功后回填 URL */
async function uploadCover(options: UploadRequestOptions) {
  uploadingCover.value = true
  try {
    const media = await uploadMediaApi(options.file as File)
    form.coverImage = media.url
    ElMessage.success('封面已上传')
  } finally {
    uploadingCover.value = false
  }
}

// ===== Markdown 导入 =====
const mdDialogVisible = ref(false)
const mdText = ref('')

function importMarkdown() {
  mdText.value = ''
  mdDialogVisible.value = true
}

function confirmImport() {
  const editor = editorRef.value
  if (!editor) return
  if (!mdText.value.trim()) {
    ElMessage.warning('请输入 Markdown 内容')
    return
  }
  const html = marked.parse(mdText.value) as string
  editor.setHtml(html)
  mdDialogVisible.value = false
  ElMessage.success('已导入，可继续在编辑器中编辑')
}

// ===== AI 写作助手（文档 7.4） =====
const aiVisible = ref(false)
const aiTab = ref('draft')
const generating = ref(false)
const aiResult = ref('')
let aiAbort: AbortController | null = null

const aiDraft = reactive({ title: '', keywords: '', outline: '' })
const aiContinue = reactive({ instruction: '' })
const aiPolish = reactive({ instruction: '' })

function openAiPanel() {
  if (!editorRef.value) {
    ElMessage.warning('请先打开编辑器')
    return
  }
  aiResult.value = ''
  aiVisible.value = true
}

function renderMarkdown(src: string): string {
  return marked.parse(src) as string
}

/** 取编辑器纯文本（续写/润色输入），截断 4000 字 */
function editorPlainText(): string {
  const editor = editorRef.value
  const text = editor ? editor.getText() : ''
  return text.length > 4000 ? text.slice(-4000) : text
}

async function runAi() {
  if (aiTab.value === 'enhance') {
    await runEnhance()
    return
  }
  const editor = editorRef.value
  if (!editor) {
    ElMessage.warning('编辑器未就绪')
    return
  }
  const req: AiWriteReq = { articleId: articleId.value }
  let streamFn: (r: AiWriteReq, cb: (t: string) => void, s?: AbortSignal) => Promise<void>
  if (aiTab.value === 'draft') {
    const title = aiDraft.title.trim() || form.title.trim()
    if (!title) {
      ElMessage.warning('请填写标题')
      return
    }
    req.title = title
    req.keywords = aiDraft.keywords.trim()
    req.outline = aiDraft.outline.trim()
    streamFn = writeDraftStream
  } else if (aiTab.value === 'continue') {
    req.content = editorPlainText()
    req.instruction = aiContinue.instruction.trim() || undefined
    streamFn = writeContinueStream
  } else {
    req.content = editorPlainText()
    req.instruction = aiPolish.instruction.trim() || undefined
    streamFn = writePolishStream
  }
  generating.value = true
  aiResult.value = ''
  aiAbort = new AbortController()
  try {
    await streamFn(
      req,
      (t) => {
        aiResult.value += t
      },
      aiAbort.signal
    )
  } catch (e) {
    const err = e as { message?: string }
    if (err?.message && !/abort/i.test(err.message)) {
      ElMessage.error(err.message)
    }
  } finally {
    generating.value = false
    aiAbort = null
  }
}

function stopAi() {
  aiAbort?.abort()
}

/** 插入编辑器：草稿/润色替换全文，续写追加到末尾 */
function insertAiResult() {
  const editor = editorRef.value
  if (!editor || !aiResult.value) return
  const html = marked.parse(aiResult.value) as string
  if (aiTab.value === 'continue') {
    editor.dangerouslyInsertHtml(html)
    ElMessage.success('已追加到正文末尾')
  } else {
    editor.setHtml(html)
    ElMessage.success('已替换正文')
  }
}

async function runEnhance() {
  const editor = editorRef.value
  if (!editor) return
  generating.value = true
  aiResult.value = ''
  try {
    const resp = await enhanceApi({ title: form.title.trim(), content: editorPlainText() })
    form.summary = resp.summary
    form.seoTitle = resp.seoTitle
    form.seoDescription = resp.seoDescription
    form.seoKeywords = resp.seoKeywords
    aiResult.value = [
      `**摘要**：${resp.summary}`,
      `**SEO 标题**：${resp.seoTitle}`,
      `**SEO 描述**：${resp.seoDescription}`,
      `**SEO 关键词**：${resp.seoKeywords}`
    ].join('\n\n')
    ElMessage.success('已回填摘要与 SEO 设置')
  } catch (e) {
    const err = e as { message?: string }
    ElMessage.error(err?.message || '生成失败')
  } finally {
    generating.value = false
  }
}

// ===== 保存 =====
async function save(status: number) {
  if (!form.title.trim()) {
    ElMessage.warning('请填写文章标题')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.title.trim(),
      content: editorHtml.value,
      summary: form.summary || undefined,
      coverImage: form.coverImage || undefined,
      categoryId: form.categoryId,
      status,
      isTop: form.isTop,
      isRecommend: form.isRecommend,
      tagIds: form.tagIds,
      seoTitle: form.seoTitle || undefined,
      seoDescription: form.seoDescription || undefined,
      seoKeywords: form.seoKeywords || undefined
    }
    if (isEdit.value) {
      await updateArticleApi(articleId.value!, payload)
      ElMessage.success(status === 1 ? '已发布' : '已保存为草稿')
    } else {
      const newId = await createArticleApi(payload)
      // 从 AI 生成记录进入：保存成功后回填关联，形成「生成→文章」闭环
      if (aiRecordId.value) {
        await bindArticleApi(aiRecordId.value, newId)
      }
      ElMessage.success(status === 1 ? '已发布' : '已保存为草稿')
    }
    router.push('/article/list')
  } finally {
    saving.value = false
  }
}

// ===== 初始化 =====
const aiRecordId = ref<number | undefined>(
  route.query.aiRecordId ? Number(route.query.aiRecordId) : undefined
)

onMounted(async () => {
  const [catRes, tagRes] = await Promise.all([listCategoriesApi(), listTagsApi()])
  categories.value = catRes
  tags.value = tagRes
  // 从 AI 生成记录续编：预填标题（取结果首个 markdown 标题）与正文
  if (aiRecordId.value) {
    try {
      const record = await generationDetailApi(aiRecordId.value)
      if (record.result) {
        const firstTitle = record.result.match(/^#\s+(.+)$/m)?.[1]?.trim()
        if (firstTitle && !form.title) {
          form.title = firstTitle.slice(0, 80)
        }
        editorHtml.value = marked.parse(record.result) as string
      }
    } catch (e) {
      const err = e as { message?: string }
      ElMessage.warning(`AI 记录加载失败：${err?.message || '未知错误'}`)
    }
  }
  if (isEdit.value) {
    const detail = await getArticleApi(articleId.value!)
    form.title = detail.title
    form.categoryId = detail.categoryId
    form.summary = detail.summary || ''
    form.coverImage = detail.coverImage || ''
    form.isTop = detail.isTop
    form.isRecommend = detail.isRecommend
    form.tagIds = []
    editorHtml.value = detail.content || '<p><br></p>'
    form.seoTitle = detail.seoTitle || ''
    form.seoDescription = detail.seoDescription || ''
    form.seoKeywords = detail.seoKeywords || ''
    // 标签名 → id 回填
    if (detail.tags?.length) {
      const tagIdMap = new Map(tags.value.map((t) => [t.name, t.id]))
      form.tagIds = detail.tags.map((name) => tagIdMap.get(name)).filter((id): id is number => !!id)
    }
  }
})
</script>

<style scoped lang="scss">
@use '@/assets/styles/variables.scss' as *;

/* ===== AI 写作助手（文档 7.4） ===== */
.ai-model-hint {
  font-size: 11.5px;
  color: $text-tertiary;
  margin: 0 0 10px;
  line-height: 1.6;
}

.ai-hint {
  font-size: 12px;
  color: $text-tertiary;
  margin: 0 0 10px;
  line-height: 1.6;
}

.ai-actions {
  display: flex;
  gap: 10px;
  margin: 12px 0;
}

.ai-output {
  min-height: 220px;
  max-height: 420px;
  overflow-y: auto;
  background-color: $bg-subtle;
  border: 1px solid $border-default;
  border-radius: $radius-sm;
  padding: 12px;
  box-sizing: border-box;
}

.ai-markdown {
  font-size: 13px;
  line-height: 1.7;
  color: $text-primary;

  :deep(h1), :deep(h2), :deep(h3) {
    font-weight: 600;
    margin: 10px 0 6px;
  }

  :deep(p) {
    margin: 6px 0;
  }

  :deep(ul), :deep(ol) {
    padding-left: 20px;
    margin: 6px 0;
  }

  :deep(pre) {
    background-color: $bg-surface;
    border: 1px solid $border-default;
    border-radius: $radius-sm;
    padding: 8px 10px;
    overflow-x: auto;
    font-size: 12px;
    font-family: $font-mono;
  }

  :deep(code) {
    font-family: $font-mono;
  }
}

.editor-card {
  background-color: $bg-surface;
  border: 1px solid $border-default;
  border-radius: $radius-md;
  padding: 24px;
  box-sizing: border-box;
  margin-bottom: 16px;
}

.title-input {
  margin-bottom: 16px;

  :deep(.el-input__wrapper) {
    font-size: 20px;
    font-weight: 600;
    font-family: $font-display;
  }
}

.form-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.editor-box {
  border: 1px solid $border-default;
  border-radius: $radius-sm;
  overflow: hidden;
  margin-bottom: 16px;
}

.editor-toolbar {
  background-color: $bg-subtle;
  border-bottom: 1px solid $border-default;
}

.editor-body {
  min-height: 420px;
  background-color: $bg-surface;
}

.summary-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.summary-label {
  flex: 0 0 72px;
  line-height: 32px;
  color: $text-secondary;
  font-size: 13px;
}

.seo-collapse {
  border: none;
  :deep(.el-collapse-item__header) {
    border-bottom: 1px solid $border-default;
    color: $text-secondary;
  }
  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }
}

.editor-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-spacer {
  flex: 1;
}
</style>
