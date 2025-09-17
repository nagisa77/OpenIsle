package com.openisle.dto;

import com.openisle.model.Comment;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * 评论分页响应DTO，包含分页元数据
 * 用于评论分页功能，处理置顶评论和普通评论
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentPaginationDto {
    /**
     * 当前页的评论列表（第一页包含置顶评论）
     */
    private List<CommentDto> comments;
    
    /**
     * 当前页码（从0开始）
     */
    private int currentPage;
    
    /**
     * 每页大小
     */
    private int pageSize;
    
    /**
     * 总评论数（不包含置顶评论，避免重复计数）
     */
    private long totalElements;
    
    /**
     * 总页数
     */
    private int totalPages;
    
    /**
     * 是否有下一页
     */
    private boolean hasNext;
    
    /**
     * 是否为第一页
     */
    private boolean first;
    
    /**
     * 是否为最后一页
     */
    private boolean last;
    
    /**
     * 当前页的评论数量
     */
    private int numberOfElements;
    
    /**
     * 临时存储Comment实体，用于Controller层转换，不序列化到JSON
     */
    @JsonIgnore
    private List<Comment> commentEntities;
}