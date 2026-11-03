package com.senac.tsi.Formula1Api.v2;

import io.swagger.v3.oas.annotations.media.Schema;

// mudanca da v2: a equipe vem so com id e nome, sem os patrocinadores e o resto que a v1 trazia
@Schema(description = "Equipe resumida")
public record TeamSummary(
        @Schema(example = "1") long id,
        @Schema(example = "McLaren") String name) {
}
