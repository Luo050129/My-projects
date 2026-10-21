package com.luoxianjun.community.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询统一返回结构。
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> records;
    /** 总记录数 */
    private long total;
    /** 当前页 */
    private long current;
    /** 每页大小 */
    private long size;

    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        PageResult<T> p = new PageResult<>();
        p.setRecords(records);
        p.setTotal(total);
        p.setCurrent(current);
        p.setSize(size);
        return p;
    }
}
