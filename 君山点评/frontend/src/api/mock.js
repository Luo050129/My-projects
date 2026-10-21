// 本地模拟数据：当后端（Spring Boot :8081）未启动或不可达时，前端自动回退到此处，
// 保证页面在任意环境下都能「完整可用」。所有日期均落在 2025.10 ~ 2026.01 区间内。

export const mockShopTypes = [
  { id: 1, name: '美食', icon: '🍜' },
  { id: 2, name: 'KTV', icon: '🎤' },
  { id: 3, name: '丽人·美发', icon: '💇' },
  { id: 4, name: '健身运动', icon: '🏋️' },
  { id: 5, name: '按摩·足疗', icon: '💆' },
  { id: 6, name: '美容SPA', icon: '🌸' },
  { id: 7, name: '亲子游乐', icon: '🧸' },
  { id: 8, name: '酒吧', icon: '🍸' }
]

export const mockShops = [
  {
    id: 1,
    name: '103茶餐厅',
    typeId: 1,
    area: '大关',
    address: '金华路锦昌文华苑29号',
    avgPrice: 80,
    sold: 2156,
    comments: 372,
    score: 4.7,
    businessHours: '10:00-22:00',
    openStatus: 1,
    images: ['/images/shop1.png'],
    createTime: '2025-10-01 08:12:00'
  },
  {
    id: 2,
    name: '新白鹿餐厅(运河上街店)',
    typeId: 1,
    area: '运河上街',
    address: '台州路2号运河上街购物中心F5',
    avgPrice: 61,
    sold: 5301,
    comments: 1203,
    score: 4.8,
    businessHours: '10:30-21:00',
    openStatus: 1,
    images: ['/images/shop2.png'],
    createTime: '2025-10-22 11:40:00'
  },
  {
    id: 3,
    name: '幸福里老北京涮锅（丝联店）',
    typeId: 1,
    area: '拱宸桥',
    address: '金华南路189号丝联166号',
    avgPrice: 130,
    sold: 982,
    comments: 540,
    score: 4.6,
    businessHours: '11:00-13:50,17:00-20:50',
    openStatus: 1,
    images: ['/images/shop3.png'],
    createTime: '2025-11-05 18:24:00'
  },
  {
    id: 4,
    name: '浅草屋寿司（运河上街店）',
    typeId: 1,
    area: '运河上街',
    address: '拱墅区金华路80号运河上街B1',
    avgPrice: 88,
    sold: 1340,
    comments: 412,
    score: 4.5,
    businessHours: '11:00-21:30',
    openStatus: 1,
    images: ['/images/shop4.png'],
    createTime: '2025-11-18 19:51:00'
  },
  {
    id: 5,
    name: '开乐迪KTV（运河上街店）',
    typeId: 2,
    area: '运河上街',
    address: '台州路2号运河上街购物中心F4',
    avgPrice: 67,
    sold: 2210,
    comments: 689,
    score: 4.4,
    businessHours: '00:00-24:00',
    openStatus: 1,
    images: ['/images/shop5.png'],
    createTime: '2025-12-01 20:25:00'
  },
  {
    id: 6,
    name: '魅(杭州远洋乐堤港店)',
    typeId: 2,
    area: '远洋乐堤港',
    address: '丽水路58号远洋乐堤港F4',
    avgPrice: 88,
    sold: 1567,
    comments: 233,
    score: 4.3,
    businessHours: '10:00-02:00',
    openStatus: 0,
    images: ['/images/shop6.png'],
    createTime: '2025-12-15 20:34:00'
  },
  {
    id: 7,
    name: '悦己美容SPA馆',
    typeId: 6,
    area: '大关',
    address: '上塘路288号悦享大厦3楼',
    avgPrice: 320,
    sold: 645,
    comments: 188,
    score: 4.9,
    businessHours: '10:00-21:00',
    openStatus: 1,
    images: ['/images/shop7.png'],
    createTime: '2026-01-04 10:00:00'
  },
  {
    id: 8,
    name: '力美健身工作室',
    typeId: 4,
    area: '拱宸桥',
    address: '金华路12号健身中心2楼',
    avgPrice: 199,
    sold: 430,
    comments: 96,
    score: 4.7,
    businessHours: '07:00-22:30',
    openStatus: 1,
    images: ['/images/shop8.png'],
    createTime: '2026-01-20 09:30:00'
  }
]

export const mockReviews = [
  {
    id: 1,
    shopId: 2,
    title: '运河上街性价比之王，糖醋排骨绝了',
    content: '周末和朋友来吃的，等位大概20分钟。糖醋排骨酸甜可口，蛋黄南瓜也很好吃，人均60出头就能吃饱，环境干净，服务员态度也好。强烈推荐！',
    author: '小鱼同学',
    avatar: '/images/avatar1.png',
    likes: 128,
    comments: 12,
    date: '2025-11-08 16:05:00',
    images: ['/images/shop2.png']
  },
  {
    id: 2,
    shopId: 1,
    title: '老字号茶餐厅，下午茶很舒服',
    content: '招牌丝袜奶茶和菠萝油是标配，配上咸宁糕刚刚好。店里复古港风装修，拍照出片，适合三五好友闲聊一下午。',
    author: '可可今天不吃肉',
    avatar: '/images/avatar2.png',
    likes: 86,
    comments: 7,
    date: '2025-11-22 14:20:00',
    images: ['/images/shop1.png']
  },
  {
    id: 3,
    shopId: 3,
    title: '老北京铜锅涮肉，羊肉很新鲜',
    content: '手切羊肉肉质紧实不散，麻酱小料是灵魂。锅底清汤就能吃出好肉的本味，冬天来一顿特别暖胃。',
    author: '温润如玉',
    avatar: '/images/avatar3.png',
    likes: 64,
    comments: 5,
    date: '2025-12-03 19:30:00',
    images: ['/images/shop3.png']
  },
  {
    id: 4,
    shopId: 5,
    title: '包厢音响给力，生日聚会首选',
    content: '提前订了大包厢，麦克风音质不错，曲库也比较新。服务响应快，送了果盘，体验很赞。',
    author: '晚风',
    avatar: '/images/avatar4.png',
    likes: 52,
    comments: 3,
    date: '2025-12-19 21:10:00',
    images: ['/images/shop5.png']
  }
]

export function getMockShopById(id) {
  return mockShops.find((s) => String(s.id) === String(id)) || null
}

export function getMockShops({ typeId, name } = {}) {
  let list = mockShops.slice()
  if (typeId) list = list.filter((s) => String(s.typeId) === String(typeId))
  if (name) list = list.filter((s) => s.name.includes(name))
  return list
}

export function getMockReviewsByShop(shopId) {
  return mockReviews.filter((r) => String(r.shopId) === String(shopId))
}
