package com.mine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 프론트엔드 호환성을 유지하면서 불필요한 pageable, sort 객체를 제거한 공통 페이징 DTO.
 * 기존 Spring PageImpl JSON 스펙과 동일한 필드명을 유지하여 클라이언트 충돌을 방지합니다.
 */
@Schema(description = "스프링 호환성을 유지한 공통 다이어트 페이징 DTO")
@Getter
@AllArgsConstructor
public class PageResponse<T> {

    @Schema(description = "데이터 목록")
    private List<T> content;

    @Schema(description = "전체 페이지 수", example = "5")
    private int totalPages;

    @Schema(description = "전체 데이터 개수", example = "50")
    private long totalElements;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean last;

    @Schema(description = "한 페이지당 데이터 개수", example = "10")
    private int size;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private int number;

    @Schema(description = "현재 페이지 데이터 개수", example = "10")
    private int numberOfElements;

    @Schema(description = "첫 페이지 여부", example = "true")
    private boolean first;

    @Schema(description = "데이터가 비어있는지 여부", example = "false")
    private boolean empty;

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast(),
                page.getSize(),
                page.getNumber(),
                page.getNumberOfElements(),
                page.isFirst(),
                page.isEmpty()
        );
    }
}
