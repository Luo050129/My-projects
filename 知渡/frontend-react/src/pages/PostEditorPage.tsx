import { useEffect, useRef, useState } from 'react'
import type { ChangeEvent, DragEvent, FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { createPost, fetchPost, updatePost, uploadImage } from '../api/endpoints'
import { useToast } from '../components/ToastProvider'
import { validateCover } from '../utils/format'
import styles from './PostEditorPage.module.css'

interface FormState {
  title: string
  content: string
  tags: string
  coverImage: string
}

const EMPTY: FormState = { title: '', content: '', tags: '', coverImage: '' }

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

  // 编辑态：回填原帖内容。用 ignore 标记避免组件已卸载后 setState
  useEffect(() => {
    if (!id) return
    const controller = new AbortController()

    fetchPost(id, controller.signal)
      .then((post) => {
        setForm({
          title: post.title ?? '',
          content: post.content ?? '',
          tags: post.tags ?? '',
          coverImage: post.coverImage ?? ''
        })
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return
        setError(err instanceof Error ? err.message : '加载失败')
      })

    return () => controller.abort()
  }, [id])

  const patch = (part: Partial<FormState>) => setForm((prev) => ({ ...prev, ...part }))

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
      patch({ coverImage: result.url })
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

    setSubmitting(true)
    try {
      // 空字符串转 null：避免后端把 "" 当成非法封面地址
      const payload = { ...form, coverImage: form.coverImage || null }

      if (id) {
        await updatePost(id, payload)
        showToast('修改已保存', 'ok')
        navigate(`/posts/${id}`, { replace: true })
      } else {
        const newId = await createPost(payload)
        showToast('发布成功', 'ok')
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

        <div className="field">
          <label>封面图（可选）</label>
          <div
            className={[styles.drop, dragging ? styles.dragging : '', form.coverImage ? styles.filled : '']
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

        <div className="field">
          <label htmlFor="content">正文</label>
          <textarea
            id="content"
            className={styles.contentInput}
            maxLength={8000}
            placeholder="写点什么…"
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

        {error && <p className="error-text">{error}</p>}

        <div className={styles.actions}>
          <button
            className="btn btn-primary"
            type="submit"
            disabled={submitting || uploading}
          >
            {submitting ? '提交中…' : uploading ? '图片上传中…' : isEdit ? '保存修改' : '发布'}
          </button>
          <Link to="/posts" className="btn">
            取消
          </Link>
        </div>
      </form>
    </div>
  )
}
