package com.senac.tsi.Formula1Api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Linha da tabela de classificacao (pilotos ou construtores)")
public record Standing(
        @Schema(description = "Posicao na tabela", example = "1") long position,
        @Schema(description = "Id do piloto ou da equipe", example = "1") long id,
        @Schema(example = "Lando Norris") String name,
        @Schema(description = "Soma dos pontos", example = "43") long points) {

    // Usado pelas consultas JPQL (select new ...); a posicao e calculada depois
    public Standing(Long id, String name, Long points) {
        this(0, id, name, points);
    }

    public Standing withPosition(long position) {
        return new Standing(position, id, name, points);
    }
}
