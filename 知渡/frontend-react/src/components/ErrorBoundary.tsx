import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'

interface Props {
  children: ReactNode
}

interface State {
  error: Error | null
}

/**
 * 渲染期异常兜底。
 *
 * Vue 版的某个组件在 render 中抛错时，整棵组件树会卸载，用户看到纯白页面、控制台外无任何提示。
 * React 的错误边界把异常拦在子树内，至少能告诉用户"出错了 + 怎么恢复"。
 * 注意：它只捕获渲染 / 生命周期 / 构造函数里的错误，事件回调和异步错误仍需各自 try-catch。
 */
export class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    console.error('[ErrorBoundary] 组件渲染失败：', error, info.componentStack)
  }

  private handleReset = (): void => {
    this.setState({ error: null })
  }

  render(): ReactNode {
    const { error } = this.state
    if (!error) return this.props.children

    return (
      <div className="panel" style={{ marginTop: 32 }}>
        <h2 style={{ marginTop: 0, fontSize: 18 }}>页面出错了</h2>
        <p className="muted small" style={{ wordBreak: 'break-word' }}>
          {error.message || '未知错误'}
        </p>
        <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
          <button type="button" className="btn btn-primary" onClick={this.handleReset}>
            重试
          </button>
          <button
            type="button"
            className="btn"
            onClick={() => window.location.assign('/posts')}
          >
            回到列表
          </button>
        </div>
      </div>
    )
  }
}
