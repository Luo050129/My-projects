<template>
  <div class="panel">
    <h1 class="title">{{ isEdit ? '编辑帖子' : '发布新帖' }}</h1>

    <form @submit.prevent="submit">
      <div class="field">
        <label>标题</label>
        <input v-model.trim="form.title" type="text" maxlength="200" placeholder="一句话说清主题" />
      </div>

      <div class="field">
        <label>封面图（可选）</label>
        <div
          class="cover-drop"
          :class="{ dragging, filled: Boolean(form.coverImage) }"
          @click="pickFile"
          @dragover.prevent="dragging = true"
          @dragleave.prevent="dragging = false"
          @drop.prevent="onDrop"
        >
          <img v-if="form.coverImage" :src="form.coverImage" alt="封面预览" class="cover-preview" />
          <span v-else class="muted small">
            {{ uploading ? '上传中…' : '点击或拖拽图片到此处 · PNG / JPG / GIF / WEBP · ≤5MB' }}
          </span>
        </div>
        <input
          ref="fileInput"
          type="file"
          accept="image/png,image/jpeg,image/gif,image/webp"
          hidden
          @change="onPick"
        />
        <div v-if="form.coverImage" class="cover-actions">
          <button type="button" class="btn btn-sm" :disabled="uploading" @click="pickFile">
            重新选择
          </button>
          <button type="button" class="btn btn-sm btn-danger" :disabled="uploading" @click="clearCover">
            移除封面
          </button>
        </div>
      </div>

      <div class="field">
        <label>正文</label>
        <textarea v-model="form.content" class="content-input" maxlength="8000" placeholder="写点什么…" />
        <p class="muted small counter">{{ form.content.length }} / 8000</p>
      </div>

      <div class="field">
        <label>标签（逗号分隔，可选）</label>
        <input v-model.trim="form.tags" type="text" maxlength="255" placeholder="Redis,缓存,并发" />
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>

      <div class="actions">
        <button class="btn btn-primary" type="submit" :disabled="submitting || uploading">
          {{ submitting ? '提交中…' : uploading ? '图片上传中…' : isEdit ? '保存修改' : '发布' }}
        </button>
        <router-link to="/posts" class="btn">取消</router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createPost, fetchPost, updatePost, uploadImage } from '../api'

const MAX_SIZE = 5 * 1024 * 1024
const ALLOWED = ['image/png', 'image/jpeg', 'image/gif', 'image/webp']

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => Boolean(route.params.id))
const submitting = ref(false)
const uploading = ref(false)
const dragging = ref(false)
const error = ref('')
const fileInput = ref(null)
const form = reactive({ title: '', content: '', tags: '', coverImage: '' })

onMounted(async () => {
  if (!isEdit.value) return
  try {
    const post = await fetchPost(route.params.id)
    form.title = post.title || ''
    form.content = post.content || ''
    form.tags = post.tags || ''
    form.coverImage = post.coverImage || ''
  } catch (e) {
    error.value = e.message || '加载失败'
  }
})

function pickFile() {
  if (uploading.value) return
  fileInput.value?.click()
}

function onPick(event) {
  const file = event.target.files?.[0]
  // 置空 value，否则重复选同一个文件不会触发 change
  event.target.value = ''
  if (file) handleFile(file)
}

function onDrop(event) {
  dragging.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) handleFile(file)
}

async function handleFile(file) {
  error.value = ''
  // 前端先拦一道：省掉明显的无效请求，但服务端仍会独立校验，前端校验不构成信任
  if (!ALLOWED.includes(file.type)) {
    error.value = '仅支持 PNG / JPG / GIF / WEBP 图片'
    return
  }
  if (file.size > MAX_SIZE) {
    error.value = `图片超过 5MB（当前 ${(file.size / 1024 / 1024).toFixed(1)}MB）`
    return
  }

  uploading.value = true
  try {
    const result = await uploadImage(file)
    form.coverImage = result.url
  } catch (e) {
    error.value = e.message || '上传失败'
  } finally {
    uploading.value = false
  }
}

function clearCover() {
  form.coverImage = ''
}

async function submit() {
  error.value = ''
  if (!form.title || !form.content) {
    error.value = '标题和正文不能为空'
    return
  }
  submitting.value = true
  try {
    // 空字符串转为 null，避免后端把 "" 当成非法封面地址
    const payload = { ...form, coverImage: form.coverImage || null }
    if (isEdit.value) {
      await updatePost(route.params.id, payload)
      router.replace(`/posts/${route.params.id}`)
    } else {
      const id = await createPost(payload)
      router.replace(`/posts/${id}`)
    }
  } catch (e) {
    error.value = e.message || '提交失败'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.title {
  font-size: 18px;
  margin: 0 0 18px;
}
.content-input {
  min-height: 320px;
}
.counter {
  text-align: right;
  margin: 6px 0 0;
}
.actions {
  display: flex;
  gap: 10px;
  margin-top: 6px;
}
.cover-drop {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  padding: 12px;
  border: 1px dashed var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.cover-drop:hover,
.cover-drop.dragging {
  border-color: var(--accent);
  background: var(--accent-soft);
}
.cover-drop.filled {
  padding: 0;
  border-style: solid;
  overflow: hidden;
}
.cover-preview {
  display: block;
  width: 100%;
  max-height: 320px;
  object-fit: cover;
  border-radius: 8px;
}
.cover-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
</style>
