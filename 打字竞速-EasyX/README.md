# 打字竞速（EasyX）

> 双人在线打字竞速游戏。两名玩家各控制一个角色，在环形赛道上按打字进度赛跑，通过 HTTP 与服务端同步进度，先到终点者胜。
>
> 代码里 1P 角色素材是「哈基米」（hajimi），2P 是「曼波」（manbo）。

---

## 一、这是什么

从 `load.h` 反推出的完整玩法：

1. 客户端启动 → 向服务端 POST `/login`，拿到玩家编号（1 或 2；返回 ≤0 表示房间已满）
2. POST `/query_text` 拉取本局要打的文本，按行切分
3. 玩家打字推进 `progress_1` / `progress_2`
4. 后台线程每 10ms 把自己的进度 POST 给服务端，并取回对手进度
5. 角色按进度百分比沿 `Path` 定义的折线赛道移动

---

## 二、技术栈

| 层 | 选型 |
| --- | --- |
| 窗口 / 图形 | **EasyX**（`graphics.h`、`IMAGE`、`AlphaBlend` 做透明贴图） |
| 网络 | **cpp-httplib**（`#include "../thirdparty/httplib.h"`） |
| 音频 | Windows MCI（`mciSendString` 播放 mp3） |
| 并发 | `<thread>` 后台同步线程 + `<atomic>` 进度变量 |
| 计时 | `<chrono>` + 自研 `Timer` |

---

## 三、模块结构

依赖方向：`vector2 → camera → util → atlas → animation → player → path → load`

| 文件 | 职责 | 关键点 |
| --- | --- | --- |
| `vector2.h` | 2D 向量 | 运算符重载、点积、长度、归一化、`approx` 近似比较 |
| `camera.h` | 2D 摄像机 | 位置 + 尺寸，`look_at` 让目标居中 |
| `time.h` | 帧计时器 | `wait_time` 到期触发回调，支持单次 / 循环 / 暂停 |
| `util.h` | 绘图与音频工具 | `putimage_ex` 带 AlphaBlend 的贴图（自动扣摄像机偏移）；`load_audio` / `play_audio` 封装 MCI |
| `atlas.h` | 图集 | 按序列加载 `xxx_%d.png` 多帧图片 |
| `animation.h` | 帧动画 | 帧列表 + `Timer` 驱动、循环开关、`on_finished` 回调、按摄像机渲染 |
| `player.h` | 玩家角色 | 八方向（上/下/左/右 × idle/run）动画切换、朝目标点匀速移动、自动判断朝向 |
| `path.h` | 折线赛道 | 由路径点算总长，`get_position_at_progress(0~1)` 按比例取坐标 |
| `load.h` | 资源加载 + 网络 | 全局状态、加载全部图片/音频/字体、登录与进度同步线程 |

---

## 四、当前状态：只有头文件，**无法直接编译运行**

这个目录是从知识库仓库里提取出来的**骨架**，缺以下部分才能跑起来：

| # | 缺什么 | 说明 |
| --- | --- | --- |
| 1 | **`main.cpp` / WinMain** | 没有程序入口，也没有主循环（输入、更新、渲染） |
| 2 | **`thirdparty/httplib.h`** | cpp-httplib 单头库，需自行放入 `thirdparty/` 目录 |
| 3 | **`resources/`** | 见下方清单，共 71 张 png + 11 个 mp3 + 1 个字体 |
| 4 | **`config.cfg`** | 内容就是服务端地址（如 `127.0.0.1:8080`），缺失会弹窗退出 |
| 5 | **服务端** | 需实现 `/login`、`/query_text`、`/update_1`、`/update_2` |
| 6 | **项目文件** | 没有 `.vcxproj` / `CMakeLists.txt`，需自行建 |

### resources 清单（从 `load.h` 逐行提取）

**角色图集**（每组 4 帧，命名 `前缀_1.png` ~ `前缀_4.png`）

```
hajimi_idle_back / hajimi_idle_front / hajimi_idle_left / hajimi_idle_right
hajimi_run_back  / hajimi_run_front  / hajimi_run_left  / hajimi_run_right
manbo_idle_back  / manbo_idle_front  / manbo_idle_left  / manbo_idle_right
manbo_run_back   / manbo_run_front   / manbo_run_left   / manbo_run_right
```
→ 16 组 × 4 帧 = 64 张

**界面图片**：`main_menu_background.png`、`ui_1.png`、`ui_2.png`、`ui_3.png`、`ui_fight.png`、`ui_textbox.png`、`background.png`

**音频**：`bgm.mp3`、`1p_win.mp3`、`2p_win.mp3`、`click_1~4.mp3`、`ui_1~3.mp3`、`ui_fight.mp3`

**字体**：`IPix.ttf`

### 服务端接口（从代码反推，可直接照此实现）

| 方法 | 路径 | 请求体 | 响应体 |
| --- | --- | --- | --- |
| POST | `/login` | 无 | 纯文本数字：玩家编号（1 / 2），≤0 表示房间满 |
| POST | `/query_text` | 无 | 纯文本，多行，每行一句待打文本 |
| POST | `/update_1` | 纯文本，玩家 1 当前进度 | 纯文本，玩家 2 当前进度 |
| POST | `/update_2` | 纯文本，玩家 2 当前进度 | 纯文本，玩家 1 当前进度 |

---

## 五、代码审查发现的几个问题

提取时通读了一遍，有几处值得修：

1. **头文件里定义全局变量和非 inline 函数** —— `load.h` 里直接写了 `std::string str_address;`、`Atlas atlas_1P_idle_up;`、`IMAGE img_background;` 和 `void load_resources(HWND)`。只要有**两个** `.cpp` include 它，链接期就会多重定义（ODR 冲突）。应改成 `load.h` 声明 + `load.cpp` 定义（`extern`）。
2. **拼写错误** —— 第 111 行 `_T("resoureces/ui_fight.mp3")`，`resoureces` 应为 `resources`；这一行会导致战斗音效加载失败但不报错（MCI 静默失败）。
3. **`Timer` 单次模式逻辑可疑** —— `can_shot = (!one_shot || (one_shot && shotted))`：`shotted` 在判断之后才置 `true`，所以单次模式下**第一次到期反而不会触发**回调。看起来应该是 `!shotted`。
4. **`Path::get_position_at_progress` 没有兜底 return** —— 循环走完仍未命中时函数没有返回值，属未定义行为（实际因为 `progress` 已 clamp 到 [0,1]，命中概率极高，但编译器会告警）。
5. **资源路径全部硬编码** —— 文件名散落在 `load()` 的 30 多行里，换素材要改代码。建议用配置表驱动。

---

## 六、如果继续做，建议顺序

1. 先把 `main.cpp` 写出来（`initgraph` + 消息循环 + `BeginBatchDraw`），只渲染一张 `background.png`，验证 EasyX 环境通了
2. 补 `load.cpp`，把头文件里的定义挪出来
3. 用假数据驱动 `Player`：不给网络，直接 `set_target_position` 让它走
4. 接 `Path`：按进度驱动两个 Player 的位置
5. 最后接 HTTP，先用本地一个最小服务端（Python `http.server` 几十行就够）验证协议
