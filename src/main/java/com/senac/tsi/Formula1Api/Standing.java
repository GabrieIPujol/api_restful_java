package com.senac.tsi.Formula1Api;

import io.swagger.v3.oas.annotations.media.Schema;

// uma linha da tabela de classificacao, serve tanto pra piloto quanto pra equipe
// nao vira tabela no banco, e montada na hora pelas consultas de standings
// record ja gera construtor, getters, equals e toString sozinho
@Schema(description = "Linha da tabela de classificacao (pilotos ou construtores)")
public record Standing(
        @Schema(description = "Posicao na tabela", example = "1") long position,
        @Schema(description = "Id do piloto ou da equipe", example = "1") long id,
        @Schema(example = "Lando Norris") String name,
        @Schema(description = "Soma dos pontos", example = "43") long points) {

    // usado pelo "select new" das consultas, a posicao comeca em 0 e o controller calcula depois
    public Standing(Long id, String name, Long points) {
        this(0, id, name, points);
    }

    // record nao tem setter, entao devolve uma copia ja com a posicao
    public Standing withPosition(long position) {
        return new Standing(position, id, name, points);
    }
}
