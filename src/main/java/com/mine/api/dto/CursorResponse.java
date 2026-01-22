package com.mine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorResponse<T> {
    @Schema(description = "데이터 목록")
    private List<T> content;

    @Schema(description = "다음 커서 ID (다음 요청 시 이 값을 cursorId 파라미터로 전달)", example = "123")
    private Long nextCursor;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;
}
