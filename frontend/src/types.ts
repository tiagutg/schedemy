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

/** Dados mínimos de um usuário (`GET /usuarios/resumo`), sem contato — RNF 18. */
export type UsuarioResumo = Pick<Usuario, "id" | "nome" | "tipoUsuario" | "matriculaRa" | "departamento" | "ativo">;

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
  idOrganizador: number;
  nomeOrganizador: string;
  duracaoMinutos?: number;
  /** Agendamento original, quando este é uma remarcação. */
  idAgendamentoOrigem?: number | null;
  participantes: Participante[];
  salaVirtual?: { linkTeams: string } | null;
  criadoEm?: string;
}

/** RF 21/22: pedido de alteração de reunião confirmada; o primeiro docente que responder decide. */
export interface SolicitacaoEdicao {
  id: number;
  idAgendamento: number;
  idSolicitante: number;
  nomeSolicitante: string;
  status: "PENDENTE" | "ACEITA" | "RECUSADA" | "CANCELADA" | "EXPIRADA";
  novaData: string;
  novoHorarioInicio: string;
  novoHorarioFim: string;
  idNovaDuracao: number;
  novaDuracaoMinutos: number;
  novoFormato: string;
  novoTopico: string;
  novoResumo?: string | null;
  novosConvidados: { id: number; nome: string; tipoUsuario: string }[];
  motivo: string;
  criadoEm: string;
  respondidoEm?: string | null;
  /** RF 22: quem aceitou ou recusou; vazio se expirou, foi cancelado ou é anterior a 21/11/2026. */
  idRespondidoPor?: number | null;
  nomeRespondidoPor?: string | null;
  /** RF 11: só na recusa. */
  motivoRecusa?: string | null;
  /** A reunião como está hoje (para a lista do painel). */
  topicoAtual: string;
  dataAtual: string;
  horarioInicioAtual: string;
}

/** RF 14: horários de início em que todos os envolvidos estão livres ("HH:mm:ss"). */
export interface HorariosLivres {
  data: string;
  duracaoMinutos: number;
  horarios: string[];
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
  idUsuario: number;
  nomeUsuario: string;
  mensagem: string;
  tipoEvento: string;
  canalEnvio: string;
  idAgendamento?: number | null;
  /** Canal SISTEMA: PENDENTE = não lida, ENVIADO = lida. */
  statusEnvio: string;
  enviadoEm?: string | null;
  criadoEm: string;
}

export interface RegistroMotivo {
  id: number;
  idAgendamento: number;
  idUsuario: number;
  nomeUsuario: string;
  tipoOperacao: "CANCELAMENTO" | "SOLICITACAO_EDICAO" | "RECUSA_AGENDAMENTO" | "RECUSA_REMARCACAO" | string;
  justificativa: string;
  criadoEm: string;
}

/** Corpo padronizado de erro da API (ErrorResponseDTO). */
/** RF 06/RF 07: reunião que uma mudança na agenda do docente cancelaria. */
export interface ReuniaoAfetada {
  id: number;
  topico: string;
  dataReuniao: string;
  horarioInicio: string;
  horarioFim?: string | null;
  status: string;
  alunos: string[];
  /** A reunião é cancelada para todos, inclusive estes docentes. */
  outrosDocentes: string[];
  /** RN 05: começa em menos de 1 hora (é cancelada mesmo assim). */
  menosDeUmaHora: boolean;
}

/** Confirmação enviada ao salvar: os ids vistos na prévia e o motivo (RF 11). */
export interface CancelamentoDeAfetadas {
  idsAgendamentos: number[];
  motivo: string;
}

export interface ErrorResponse {
  mensagem?: string;
  camposInvalidos?: { campo: string; mensagem: string }[] | null;
  /** RF 06: 422 de mudança na agenda que cancelaria reuniões ainda não confirmadas. */
  reunioesAfetadas?: ReuniaoAfetada[] | null;
  [campo: string]: unknown;
}
