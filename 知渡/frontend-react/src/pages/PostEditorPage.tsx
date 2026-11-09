import { useEffect, useRef, useState } from 'react'
import type { ChangeEvent, DragEvent, FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { createPost, fetchPost, updatePost, uploadImage } from '../api/endpoints'
import type { PostPayload, PostType } from '../api/types'
import { VISIBILITY_PRIVATE, VISIBILITY_PUBLIC } from '../api/types'
import { useToast } from '../components/ToastProvider'
import { validateCover } from '../utils/format'
import styles from './PostEditorPage.module.css'

interface FormState {
  title: string
  content: string
  tags: string
  coverImage: string
  postType: PostType
  visibility: number
  aiGenerated: boolean
}

const EMPTY: FormState = {
  title: '',
  content: '',
  tags: '',
  coverImage: '',
  postType: 'TEXT',
  visibility: VISIBILITY_PUBLIC,
  aiGenerated: false
}

const TYPE_OPTIONS: { value: PostType; label: string; desc: string }[] = [
  { value: 'TEXT', label: '纯文字', desc: '以正文为主，不需要配图' },
  { value: 'IMAGE', label: '图文', desc: '需要一张封面，正文可写简短说明' }
]

export default function PostEditorPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const { showToast } = useToast()

  const [form, setForm] = useState<FormState>(EMPTY)
  const [submitting, setSubmitting] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [dragging, setDragging] = useState(false)
  const [error, setError] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  // 编辑态：回填原帖内容。用 abort 避免组件已卸载后 setState
  useEffect(() => {
    if (!id) return
    const controller = new AbortController()

    fetchPost(id, controller.signal)
      .then((post) => {
        setForm({
          title: post.title ?? '',
          content: post.content ?? '',
          tags: post.tags ?? '',
          coverImage: post.coverImage ?? '',
          postType: post.postType ?? 'TEXT',
          visibility: post.visibility ?? VISIBILITY_PUBLIC,
          aiGenerated: post.aiGenerated ?? false
        })
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setError(err instanceof Error ? err.message : '加载失败')
      })

    return () => controller.abort()
  }, [id])

  const patch = (part: Partial<FormState>) => setForm((prev) => ({ ...prev, ...part }))

  /**
   * 切换形态时清掉封面：选「纯文字」却留着封面，后端会按图文处理（它只认有没有图），
   * 用户看到的结果就和选择不一致了。
   */
  const switchType = (next: PostType) => {
    if (next === form.postType) return
    setError('')
    patch({ postType: next, coverImage: next === 'TEXT' ? '' : form.coverImage })
  }

  const handleFile = async (file: File) => {
    const message = validateCover(file)
    if (message) {
      setError(message)
      return
    }

    setError('')
    setUploading(true)
    try {
      const result = await uploadImage(file)
      // 上传封面本身就意味着这是图文帖，顺手把类型切成 IMAGE
      patch({ coverImage: result.url, postType: 'IMAGE' })
      showToast('封面已上传', 'ok')
    } catch (err) {
      setError(err instanceof Error ? err.message : '上传失败')
    } finally {
      setUploading(false)
    }
  }

  const handlePick = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    // 清空 value，否则连续选同一个文件不会再触发 change
    event.target.value = ''
    if (file) void handleFile(file)
  }

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    setDragging(false)
    if (uploading) return
    const file = event.dataTransfer.files?.[0]
    if (file) void handleFile(file)
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')

    if (!form.title.trim() || !form.content.trim()) {
      setError('标题和正文不能为空')
      return
    }
    if (form.postType === 'IMAGE' && !form.coverImage) {
      setError('图文帖需要先上传一张封面')
      return
    }

    const payload: PostPayload = {
      title: form.title.trim(),
      content: form.content.trim(),
      tags: form.tags.trim(),
      coverImage: form.postType === 'IMAGE' ? form.coverImage || null : null,
      postType: form.postType,
      visibility: form.visibility,
      aiGenerated: form.aiGenerated
    }

    setSubmitting(true)
    try {
      if (id) {
        await updatePost(id, payload)
        showToast('修改已保存', 'ok')
        navigate(`/posts/${id}`, { replace: true })
      } else {
        const newId = await createPost(payload)
        showToast(form.visibility === VISIBILITY_PRIVATE ? '已发布（仅自己可见）' : '发布成功', 'ok')
        navigate(`/posts/${newId}`, { replace: true })
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '提交失败')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="panel">
      <h1 className={styles.title}>{isEdit ? '编辑帖子' : '发布新帖'}</h1>

      <form onSubmit={handleSubmit}>
        <div className="field">
          <label>帖子样式</label>
          <div className={styles.typeGroup} role="radiogroup" aria-label="帖子样式">
            {TYPE_OPTIONS.map((option) => (
              <button
                key={option.value}
                type="button"
                role="radio"
                aria-checked={form.postType === option.value}
                className={`${styles.typeCard} ${
                  form.postType === option.value ? styles.typeCardActive : ''
                }`}
                onClick={() => switchType(option.value)}
              >
                <span className={styles.typeLabel}>{option.label}</span>
                <span className={styles.typeDesc}>{option.desc}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="field">
          <label htmlFor="title">标题</label>
          <input
            id="title"
            type="text"
            maxLength={200}
            placeholder="一句话说清主题"
            value={form.title}
            onChange={(event) => patch({ title: event.target.value })}
          />
        </div>

        {form.postType === 'IMAGE' && (
          <div className="field">
            <label>封面图（图文帖必填）</label>
            <div
              className={[
                styles.drop,
                dragging ? styles.dragging : '',
                form.coverImage ? styles.filled : ''
              ]
                .filter(Boolean)
                .join(' ')}
              onClick={() => !uploading && fileInputRef.current?.click()}
              onDragOver={(event) => {
                event.preventDefault()
                setDragging(true)
              }}
              onDragLeave={() => setDragging(false)}
              onDrop={handleDrop}
              role="button"
              tabIndex={0}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') fileInputRef.current?.click()
              }}
            >
              {form.coverImage ? (
                <img src={form.coverImage} alt="封面预览" className={styles.preview} />
              ) : (
                <span className="muted small">
                  {uploading ? '上传中…' : '点击或拖拽图片到此处 · PNG / JPG / GIF / WEBP · ≤5MB'}
                </span>
              )}
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/png,image/jpeg,image/gif,image/webp"
              hidden
              onChange={handlePick}
            />

            {form.coverImage && (
              <div className={styles.coverActions}>
                <button
                  type="button"
                  className="btn btn-sm"
                  disabled={uploading}
                  onClick={() => fileInputRef.current?.click()}
                >
                  重新选择
                </button>
                <button
                  type="button"
                  className="btn btn-sm btn-danger"
                  disabled={uploading}
                  onClick={() => patch({ coverImage: '' })}
                >
                  移除封面
                </button>
              </div>
            )}
          </div>
        )}

        <div className="field">
          <label htmlFor="content">
            {form.postType === 'IMAGE' ? '正文说明' : '正文'}
          </label>
          <textarea
            id="content"
            className={form.postType === 'IMAGE' ? styles.contentShort : styles.contentInput}
            maxLength={8000}
            placeholder={form.postType === 'IMAGE' ? '补充这张图想说什么…' : '写点什么…'}
            value={form.content}
            onChange={(event) => patch({ content: event.target.value })}
          />
          <p className={`muted small ${styles.counter}`}>{form.content.length} / 8000</p>
        </div>

        <div className="field">
          <label htmlFor="tags">标签（逗号分隔，可选）</label>
          <input
            id="tags"
            type="text"
            maxLength={255}
            placeholder="Redis,缓存,并发"
            value={form.tags}
            onChange={(event) => patch({ tags: event.target.value })}
          />
        </div>

        <div className={`field ${styles.visibilityBox}`}>
          <label className={styles.switchRow} htmlFor="private">
            <input
              id="private"
              type="checkbox"
              checked={form.visibility === VISIBILITY_PRIVATE}
              onChange={(event) =>
                patch({
                  visibility: event.target.checked ? VISIBILITY_PRIVATE : VISIBILITY_PUBLIC
                })
              }
            />
            <span>
              <strong>设为私密</strong>
              <span className={`muted small ${styles.switchHint}`}>
                只出现在你自己的个人中心，别人看不到，也不会进首页列表
              </span>
            </span>
          </label>
        </div>

        <div className={`field ${styles.visibilityBox}`}>
          <label className={styles.switchRow} htmlFor="ai-generated">
            <input
              id="ai-generated"
              type="checkbox"
              checked={form.aiGenerated}
              onChange={(event) => patch({ aiGenerated: event.target.checked })}
            />
            <span>
              <strong>内容由 AI 生成或辅助创作</strong>
              <span className={`muted small ${styles.switchHint}`}>
                依《人工智能生成合成内容标识办法》主动声明；发布后会在列表与详情展示「AI 辅助创作」标识
              </span>
            </span>
          </label>
        </div>

        {error && <p className="error-text">{error}</p>}

        <div className={styles.actions}>
          <button className="btn btn-primary" type="submit" disabled={submitting || uploading}>
            {submitting
              ? '提交中…'
              : uploading
                ? '图片上传中…'
                : isEdit
                  ? '保存修改'
                  : form.visibility === VISIBILITY_PRIVATE
                    ? '私密发布'
                    : '发布'}
          </button>
          <Link to="/posts" className="btn">
            取消
          </Link>
        </div>
      </form>
    </div>
  )
}
