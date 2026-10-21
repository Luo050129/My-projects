# -*- coding: utf-8 -*-
"""预热君山点评 Redis 缓存：逻辑过期店铺数据 + GEO 坐标"""
import json
import socket
import datetime
import pymysql

def redis_cmd(sock, *parts):
    """发送 RESP 命令，返回状态行"""
    cmd = b'*%d\r\n' % len(parts)
    for p in parts:
        if isinstance(p, str):
            p = p.encode('utf-8')
        cmd += b'$%d\r\n%s\r\n' % (len(p), p)
    sock.sendall(cmd)
    return sock.recv(65536)

# 1. 读 MySQL
conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', password='050129',
                       database='dingping', charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor)
cur = conn.cursor()
cur.execute("SELECT * FROM tb_shop")
shops = cur.fetchall()
conn.close()
print('loaded shops:', len(shops))

# 2. 连 Redis（原始 socket，兼容老版本）
sock = socket.create_connection(('127.0.0.1', 6380), timeout=5)
sock.settimeout(5)
print('redis ping:', redis_cmd(sock, 'PING'))

expire = (datetime.datetime.now() + datetime.timedelta(minutes=30)).strftime('%Y-%m-%dT%H:%M:%S')

# 3. 逻辑过期缓存（cache:shop:{id}）
def fmt_dt(v):
    return v.strftime('%Y-%m-%dT%H:%M:%S') if isinstance(v, datetime.datetime) else v

for s in shops:
    data = {k: fmt_dt(v) for k, v in s.items()}
    payload = json.dumps({"data": data, "expireTime": expire}, ensure_ascii=False)
    key = 'cache:shop:%s' % s['id']
    r = redis_cmd(sock, 'SET', key, payload)
    print('set', key, r[:20])

# 4. GEO 坐标（shop:geo:{typeId}）
from collections import defaultdict
groups = defaultdict(list)
for s in shops:
    groups[s['type_id']].append(s)
for tid, lst in groups.items():
    key = 'shop:geo:%s' % tid
    args = [key]
    for s in lst:
        args += [str(s['x']), str(s['y']), str(s['id'])]
    r = redis_cmd(sock, 'GEOADD', *args)
    print('geoadd', key, '->', r[:20])

sock.close()
print('PREHEAT DONE')
