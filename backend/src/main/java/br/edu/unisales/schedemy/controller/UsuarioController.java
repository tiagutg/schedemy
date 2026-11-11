package br.edu.unisales.schedemy.controller;

import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.UsuarioRequestDTO;
import br.edu.unisales.schedemy.dto.response.PageResponseDTO;
import br.edu.unisales.schedemy.dto.response.UsuarioResponseDTO;
import br.edu.unisales.schedemy.dto.response.UsuarioResumoResponseDTO;
import br.edu.unisales.schedemy.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Cadastro unificado de alunos, professores, coordenadores e recepcionistas")
public class UsuarioController {
    private final UsuarioService usuarioService;

    @PostMapping
    @Operation(summary = "Cadastrar usuario", description = "Cria um novo usuario no sistema.")
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioRequestDTO dto) {
        UsuarioResponseDTO criado = usuarioService.criar(dto);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + criado.id())).body(criado);
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario logado", description = "Cadastro do dono da credencial usada na requisicao.")
    public ResponseEntity<UsuarioResponseDTO> buscarLogado() {
        return ResponseEntity.ok(usuarioService.buscarLogado());
    }

    @GetMapping("/resumo")
    @Operation(summary = "Listar usuarios (resumo)",
            description = "Para escolher convidados e filtros: id, nome, tipo, RA, departamento e situacao. "
                    + "Sem e-mail e telefone (RNF 18). Aberto a qualquer usuario autenticado.")
    public ResponseEntity<PageResponseDTO<UsuarioResumoResponseDTO>> listarResumo(
            @Parameter(description = "Filtra pelo tipo de usuario") @RequestParam(required = false) TipoUsuario tipo,
            @Parameter(description = "Filtra por parte do nome ou do RA (RF 33)") @RequestParam(required = false) String nome,
            @Parameter(description = "Filtra por usuarios ativos/inativos") @RequestParam(required = false) Boolean ativo,
            @ParameterObject @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.de(usuarioService.listarResumo(tipo, nome, ativo, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuario por ID", description = "Cadastro completo: somente administracao e recepcao.")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar usuarios",
            description = "Cadastro completo com filtros, paginacao e ordenacao: somente administracao e recepcao. "
                    + "Os demais perfis usam /usuarios/resumo.")
    public ResponseEntity<PageResponseDTO<UsuarioResponseDTO>> listar(
            @Parameter(description = "Filtra pelo tipo de usuario") @RequestParam(required = false) TipoUsuario tipo,
            @Parameter(description = "Filtra por parte do nome ou do RA (RF 33)") @RequestParam(required = false) String nome,
            @Parameter(description = "Filtra por usuarios ativos/inativos") @RequestParam(required = false) Boolean ativo,
            @ParameterObject @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(PageResponseDTO.de(usuarioService.listar(tipo, nome, ativo, pageable)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuario")
    public ResponseEntity<UsuarioResponseDTO> atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover usuario")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
    }
}