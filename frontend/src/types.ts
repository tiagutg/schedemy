// Tipos dos DTOs trafegados pela Schedemy API.

/** Parâmetros de query aceitos pelo cliente HTTP (valores vazios são ignorados). */
export type Params = Record<string, unknown>;

/** Filtros de listagem mantidos pelas telas (valores vindos de inputs/selects). */
export type Filtros = Record<string, string | undefined>;

/** PageResponseDTO do back-end. */
export interface Pagina<T> {
  conteudo: T[];
  paginaAtual: number;
  tamanhoPagina: number;
  totalElementos: number;
  totalPaginas: number;
  primeira: boolean;
  ultima: boolean;
}

export type TipoUsuario = "ADMIN" | "ALUNO" | "PROFESSOR" | "COORDENADOR" | "RECEPCIONISTA";

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  telefoneWhatsapp?: string | null;
  tipoUsuario: TipoUsuario;
  matriculaRa?: string | null;
  numIdentificacao?: string | null;
  departamento?: string | null;
  ativo: boolean;
}

export interface Participante {
  idUsuario: number;
  nomeUsuario: string;
  papel: string;
  statusResposta: string;
}

export interface Agendamento {
  id: number;
  topico: string;
  resumo?: string | null;
  dataReuniao: string;
  horarioInicio: string;
  horarioFim?: string | null;
  formato: string;
  status: string;
  nomeOrganizador: string;
  duracaoMinutos?: number;
  participantes: Participante[];
  salaVirtual?: { linkTeams: string } | null;
}

export interface Duracao {
  id: number;
  minutos: number;
  ativo: boolean;
}

export interface Disponibilidade {
  id: number;
  idUsuario: number;
  nomeUsuario: string;
  diaSemana: number;
  horarioInicio: string;
  horarioFim: string;
  ativo: boolean;
}

export interface Bloqueio {
  id: number;
  idUsuario: number;
  nomeUsuario: string;
  dataInicio: string;
  dataFim: string;
  horarioInicio?: string | null;
  horarioFim?: string | null;
  eRecorrente: boolean;
  diaSemanaRecorrente?: number | null;
  motivo?: string | null;
}

export interface Avaliacao {
  id: number;
  idAgendamento: number;
  nota: number;
  comentario?: string | null;
  criadoEm: string;
}

export interface Notificacao {
  id: number;
  mensagem: string;
  tipoEvento: string;
  canalEnvio: string;
  idAgendamento: number;
  statusEnvio: string;
  criadoEm: string;
}

export interface RegistroMotivo {
  id: number;
  [campo: string]: unknown;
}

/** Corpo padronizado de erro da API (ErrorResponseDTO). */
export interface ErrorResponse {
  mensagem?: string;
  camposInvalidos?: { campo: string; mensagem: string }[] | null;
  [campo: string]: unknown;
}
