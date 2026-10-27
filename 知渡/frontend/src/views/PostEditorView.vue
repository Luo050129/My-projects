<template>
  <div class="panel">
    <h1 class="title">{{ isEdit ? '编辑帖子' : '发布新帖' }}</h1>

    <form @submit.prevent="submit">
      <div class="field">
        <label>标题</label>
        <input v-model.trim="form.title" type="text" maxlength="200" placeholder="一句话说清主题" />
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
        <button class="btn btn-primary" type="submit" :disabled="submitting">
          {{ submitting ? '提交中…' : isEdit ? '保存修改' : '发布' }}
        </button>
        <router-link to="/posts" class="btn">取消</router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createPost, fetchPost, updatePost } from '../api'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => Boolean(route.params.id))
const submitting = ref(false)
const error = ref('')
const form = reactive({ title: '', content: '', tags: '' })

onMounted(async () => {
  if (!isEdit.value) return
  try {
    const post = await fetchPost(route.params.id)
    form.title = post.title || ''
    form.content = post.content || ''
    form.tags = post.tags || ''
  } catch (e) {
    error.value = e.message || '加载失败'
  }
})

async function submit() {
  error.value = ''
  if (!form.title || !form.content) {
    error.value = '标题和正文不能为空'
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updatePost(route.params.id, { ...form })
      router.replace(`/posts/${route.params.id}`)
    } else {
      const id = await createPost({ ...form })
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
</style>
