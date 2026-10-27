'use client';

import { type FormEvent, useEffect, useMemo, useState } from 'react';

const adminConfigUrl = 'http://localhost:8081/admin/config';
const adminHandoffsUrl = 'http://localhost:8081/admin/handoffs';
const adminProductsUrl = 'http://localhost:8081/admin/products';
const adminApiToken = process.env.NEXT_PUBLIC_ADMIN_API_TOKEN;

function adminFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  return fetch(input, {
    ...init,
    headers: {
      ...init.headers,
      ...(adminApiToken ? { 'X-Admin-Token': adminApiToken } : {}),
    },
  });
}

type Empresa = {
  nome: string;
  telefone: string;
  timezone: string;
  formasPagamento: string[];
  endereco: {
    logradouro: string;
    numero: string;
    bairro: string;
    cidade: string;
    estado: string;
    cep: string;
  } | null;
};

type HorarioFuncionamento = {
  abertura: string | null;
  fechamento: string | null;
};

type Produto = {
  nome: string;
  preco: number;
  unidadeSingular: string;
  unidadePlural: string;
  aliases: string[];
};

type ProdutoAdministrativo = Produto & {
  id: number;
  ativo: boolean;
};

type ProdutoForm = {
  nome: string;
  preco: string;
  unidadeSingular: string;
  unidadePlural: string;
  aliases: string;
};

const produtoVazio: ProdutoForm = {
  nome: '',
  preco: '',
  unidadeSingular: '',
  unidadePlural: '',
  aliases: '',
};

type AdminConfig = {
  empresa: Empresa;
  horarioFuncionamento: Record<string, HorarioFuncionamento>;
  produtos: Produto[];
};

type ConfigForm = {
  nome: string;
  telefone: string;
  timezone: string;
  formasPagamento: string;
  endereco: NonNullable<Empresa['endereco']>;
  horarioFuncionamento: Record<string, HorarioFuncionamento>;
};

type HandoffStatus = 'RECEIVED' | 'PREPARING' | 'OUT_FOR_DELIVERY' | 'DELIVERED';

type HumanHandoff = {
  customerId: string;
  resumoPedido: string;
  flowType?: string;
  criadoEm: string;
  status: HandoffStatus;
  atualizadoEm: string;
  mensagemCliente?: string | null;
};

const statusLabels: Record<HandoffStatus, string> = {
  RECEIVED: 'Recebido',
  PREPARING: 'Em separação',
  OUT_FOR_DELIVERY: 'Saiu para entrega',
  DELIVERED: 'Entregue',
};

const statusActions: Array<{ label: string; status: HandoffStatus }> = [
  { label: 'Em separação', status: 'PREPARING' },
  { label: 'Saiu para entrega', status: 'OUT_FOR_DELIVERY' },
  { label: 'Entregue', status: 'DELIVERED' },
];

const diasSemana = [
  ['segunda', 'Segunda'],
  ['terca', 'Terça'],
  ['quarta', 'Quarta'],
  ['quinta', 'Quinta'],
  ['sexta', 'Sexta'],
  ['sabado', 'Sábado'],
  ['domingo', 'Domingo'],
] as const;

function formatarEndereco(endereco: Empresa['endereco']) {
  if (!endereco) {
    return 'Endereço não informado';
  }

  return [
    endereco.logradouro,
    endereco.numero,
    endereco.bairro,
    endereco.cidade,
    endereco.estado,
    endereco.cep,
  ]
    .filter(Boolean)
    .join(', ');
}

function formatarHorario(horario?: HorarioFuncionamento) {
  if (!horario?.abertura || !horario?.fechamento) {
    return 'Fechado';
  }

  return `${horario.abertura} às ${horario.fechamento}`;
}

function criarFormularioConfig(config: AdminConfig): ConfigForm {
  return {
    nome: config.empresa.nome ?? '',
    telefone: config.empresa.telefone ?? '',
    timezone: config.empresa.timezone ?? '',
    formasPagamento: config.empresa.formasPagamento.join(', '),
    endereco: {
      logradouro: config.empresa.endereco?.logradouro ?? '',
      numero: config.empresa.endereco?.numero ?? '',
      bairro: config.empresa.endereco?.bairro ?? '',
      cidade: config.empresa.endereco?.cidade ?? '',
      estado: config.empresa.endereco?.estado ?? '',
      cep: config.empresa.endereco?.cep ?? '',
    },
    horarioFuncionamento: Object.fromEntries(
      diasSemana.map(([key]) => [key, config.horarioFuncionamento[key] ?? { abertura: null, fechamento: null }])
    ),
  };
}

function formatarPreco(preco: number) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(preco);
}

function formatarCriadoEm(criadoEm: string) {
  const data = new Date(criadoEm);

  if (Number.isNaN(data.getTime())) {
    return criadoEm;
  }

  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(data);
}

function formatarStatus(status: HandoffStatus) {
  return statusLabels[status] ?? status;
}

export default function AdminConfigPage() {
  const [config, setConfig] = useState<AdminConfig | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [configForm, setConfigForm] = useState<ConfigForm | null>(null);
  const [isConfigFormOpen, setIsConfigFormOpen] = useState(false);
  const [isSalvandoConfig, setIsSalvandoConfig] = useState(false);
  const [configSaveError, setConfigSaveError] = useState<string | null>(null);
  const [handoffs, setHandoffs] = useState<HumanHandoff[]>([]);
  const [isLoadingHandoffs, setIsLoadingHandoffs] = useState(true);
  const [handoffsError, setHandoffsError] = useState<string | null>(null);
  const [finalizandoHandoffs, setFinalizandoHandoffs] = useState<Set<string>>(new Set());
  const [finalizarHandoffError, setFinalizarHandoffError] = useState<string | null>(null);
  const [atualizandoStatusHandoffs, setAtualizandoStatusHandoffs] = useState<Set<string>>(new Set());
  const [atualizarStatusError, setAtualizarStatusError] = useState<string | null>(null);
  const [mensagemCopiadaCustomerId, setMensagemCopiadaCustomerId] = useState<string | null>(null);
  const [produtos, setProdutos] = useState<ProdutoAdministrativo[]>([]);
  const [isLoadingProdutos, setIsLoadingProdutos] = useState(true);
  const [produtosError, setProdutosError] = useState<string | null>(null);
  const [produtoEmEdicao, setProdutoEmEdicao] = useState<ProdutoAdministrativo | null>(null);
  const [produtoForm, setProdutoForm] = useState<ProdutoForm>(produtoVazio);
  const [isProductFormOpen, setIsProductFormOpen] = useState(false);
  const [isSalvandoProduto, setIsSalvandoProduto] = useState(false);
  const [produtoAtivandoId, setProdutoAtivandoId] = useState<number | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function carregarConfig() {
      try {
        setIsLoading(true);
        setError(null);

        const response = await adminFetch(adminConfigUrl);

        if (!response.ok) {
          throw new Error(`Erro ${response.status} ao carregar configuração.`);
        }

        const data = (await response.json()) as AdminConfig;

        if (isMounted) {
          setConfig(data);
        }
      } catch (err) {
        if (isMounted) {
          setError(err instanceof Error ? err.message : 'Não foi possível carregar a configuração.');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    carregarConfig();

    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    let isMounted = true;

    async function carregarProdutos() {
      try {
        setIsLoadingProdutos(true);
        setProdutosError(null);
        const response = await adminFetch(adminProductsUrl);
        if (!response.ok) {
          throw new Error(`Erro ${response.status} ao carregar produtos.`);
        }
        const data = (await response.json()) as ProdutoAdministrativo[];
        if (isMounted) {
          setProdutos(data);
        }
      } catch (err) {
        if (isMounted) {
          setProdutosError(err instanceof Error ? err.message : 'Não foi possível carregar os produtos.');
        }
      } finally {
        if (isMounted) {
          setIsLoadingProdutos(false);
        }
      }
    }

    carregarProdutos();
    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    let isMounted = true;

    async function carregarHandoffs() {
      try {
        setIsLoadingHandoffs(true);
        setHandoffsError(null);

        const response = await adminFetch(adminHandoffsUrl);

        if (!response.ok) {
          throw new Error(`Erro ${response.status} ao carregar atendimentos pendentes.`);
        }

        const data = (await response.json()) as HumanHandoff[];

        if (isMounted) {
          setHandoffs(data);
        }
      } catch (err) {
        if (isMounted) {
          setHandoffsError(
            err instanceof Error ? err.message : 'Não foi possível carregar os atendimentos pendentes.'
          );
        }
      } finally {
        if (isMounted) {
          setIsLoadingHandoffs(false);
        }
      }
    }

    carregarHandoffs();

    return () => {
      isMounted = false;
    };
  }, []);

  const enderecoCompleto = useMemo(() => formatarEndereco(config?.empresa.endereco ?? null), [config]);

  function iniciarEdicaoConfig() {
    if (!config) return;
    setConfigForm(criarFormularioConfig(config));
    setConfigSaveError(null);
    setIsConfigFormOpen(true);
  }

  function cancelarEdicaoConfig() {
    setConfigForm(null);
    setConfigSaveError(null);
    setIsConfigFormOpen(false);
  }

  async function salvarConfig(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!configForm) return;

    setIsSalvandoConfig(true);
    setConfigSaveError(null);
    try {
      const response = await adminFetch(adminConfigUrl, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          empresa: {
            nome: configForm.nome,
            telefone: configForm.telefone,
            timezone: configForm.timezone,
            formasPagamento: configForm.formasPagamento.split(',').map((forma) => forma.trim()).filter(Boolean),
            endereco: configForm.endereco,
          },
          horarioFuncionamento: configForm.horarioFuncionamento,
        }),
      });
      const body = (await response.json()) as AdminConfig | { erro?: string };
      if (!response.ok) {
        throw new Error('erro' in body && body.erro ? body.erro : `Erro ${response.status} ao salvar configuração.`);
      }
      setConfig(body as AdminConfig);
      cancelarEdicaoConfig();
    } catch (err) {
      setConfigSaveError(err instanceof Error ? err.message : 'Não foi possível salvar a configuração.');
    } finally {
      setIsSalvandoConfig(false);
    }
  }

  async function finalizarAtendimento(customerId: string) {
    const deveFinalizar = window.confirm(
      'Tem certeza que deseja finalizar este atendimento? Essa ação removerá o atendimento da lista.'
    );

    if (!deveFinalizar) {
      return;
    }

    setFinalizarHandoffError(null);
    setFinalizandoHandoffs((current) => new Set(current).add(customerId));

    try {
      const response = await adminFetch(`${adminHandoffsUrl}/${encodeURIComponent(customerId)}`, {
        method: 'DELETE',
      });

      if (!response.ok) {
        throw new Error(`Erro ${response.status} ao finalizar atendimento.`);
      }

      setHandoffs((current) => current.filter((handoff) => handoff.customerId !== customerId));
    } catch (err) {
      setFinalizarHandoffError(
        err instanceof Error ? err.message : 'Não foi possível finalizar o atendimento.'
      );
    } finally {
      setFinalizandoHandoffs((current) => {
        const next = new Set(current);
        next.delete(customerId);
        return next;
      });
    }
  }

  async function atualizarStatusAtendimento(customerId: string, status: HandoffStatus) {
    setAtualizarStatusError(null);
    setAtualizandoStatusHandoffs((current) => new Set(current).add(customerId));

    try {
      const response = await adminFetch(`${adminHandoffsUrl}/${encodeURIComponent(customerId)}/status`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ status }),
      });

      if (!response.ok) {
        throw new Error(`Erro ${response.status} ao atualizar status.`);
      }

      const handoffAtualizado = (await response.json()) as HumanHandoff;

      setHandoffs((current) =>
        current.map((handoff) =>
          handoff.customerId === customerId ? { ...handoff, ...handoffAtualizado } : handoff
        )
      );
    } catch (err) {
      setAtualizarStatusError(
        err instanceof Error ? err.message : 'Não foi possível atualizar o status.'
      );
    } finally {
      setAtualizandoStatusHandoffs((current) => {
        const next = new Set(current);
        next.delete(customerId);
        return next;
      });
    }
  }

  async function copiarMensagemCliente(customerId: string, mensagemCliente: string) {
    await navigator.clipboard.writeText(mensagemCliente);
    setMensagemCopiadaCustomerId(customerId);

    window.setTimeout(() => {
      setMensagemCopiadaCustomerId((current) => (current === customerId ? null : current));
    }, 3000);
  }

  function iniciarNovoProduto() {
    setProdutoEmEdicao(null);
    setProdutoForm(produtoVazio);
    setIsProductFormOpen(true);
    setProdutosError(null);
  }

  function iniciarEdicaoProduto(produto: ProdutoAdministrativo) {
    setProdutoEmEdicao(produto);
    setIsProductFormOpen(true);
    setProdutoForm({
      nome: produto.nome,
      preco: String(produto.preco),
      unidadeSingular: produto.unidadeSingular,
      unidadePlural: produto.unidadePlural,
      aliases: produto.aliases.join(', '),
    });
    setProdutosError(null);
  }

  function cancelarEdicaoProduto() {
    setProdutoEmEdicao(null);
    setProdutoForm(produtoVazio);
    setIsProductFormOpen(false);
  }

  async function salvarProduto(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const preco = Number(produtoForm.preco);
    if (!Number.isInteger(preco) || preco < 0) {
      setProdutosError('Informe um preço inteiro maior ou igual a zero.');
      return;
    }

    setIsSalvandoProduto(true);
    setProdutosError(null);
    try {
      const response = await adminFetch(
        produtoEmEdicao ? `${adminProductsUrl}/${produtoEmEdicao.id}` : adminProductsUrl,
        {
          method: produtoEmEdicao ? 'PUT' : 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            ...produtoForm,
            preco,
            aliases: produtoForm.aliases.split(',').map((alias) => alias.trim()).filter(Boolean),
          }),
        }
      );
      const body = (await response.json()) as ProdutoAdministrativo | { erro?: string };
      if (!response.ok) {
        throw new Error('erro' in body && body.erro ? body.erro : `Erro ${response.status} ao salvar produto.`);
      }
      const produtoSalvo = body as ProdutoAdministrativo;
      setProdutos((current) =>
        produtoEmEdicao
          ? current.map((produto) => (produto.id === produtoSalvo.id ? produtoSalvo : produto))
          : [...current, produtoSalvo]
      );
      cancelarEdicaoProduto();
    } catch (err) {
      setProdutosError(err instanceof Error ? err.message : 'Não foi possível salvar o produto.');
    } finally {
      setIsSalvandoProduto(false);
    }
  }

  async function alterarAtivacaoProduto(produto: ProdutoAdministrativo) {
    setProdutoAtivandoId(produto.id);
    setProdutosError(null);
    try {
      const response = await adminFetch(`${adminProductsUrl}/${produto.id}/active`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ativo: !produto.ativo }),
      });
      const body = (await response.json()) as ProdutoAdministrativo | { erro?: string };
      if (!response.ok) {
        throw new Error('erro' in body && body.erro ? body.erro : `Erro ${response.status} ao atualizar produto.`);
      }
      const produtoAtualizado = body as ProdutoAdministrativo;
      setProdutos((current) => current.map((item) => (item.id === produtoAtualizado.id ? produtoAtualizado : item)));
    } catch (err) {
      setProdutosError(err instanceof Error ? err.message : 'Não foi possível atualizar o produto.');
    } finally {
      setProdutoAtivandoId(null);
    }
  }

  return (
    <main className="min-h-screen bg-neutral-50 px-4 py-8 text-neutral-950 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-6xl flex-col gap-6">
        <header className="border-b border-neutral-200 pb-4">
          <p className="text-sm font-medium uppercase tracking-wide text-neutral-500">Admin</p>
          <h1 className="mt-2 text-3xl font-semibold">Configuração da empresa</h1>
          <p className="mt-2 text-sm text-neutral-600">Dados usados pelo atendimento e pela IA.</p>
        </header>

        <section className="rounded-lg border border-neutral-200 bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
            <h2 className="text-xl font-semibold">Atendimentos pendentes</h2>
            {!isLoadingHandoffs && !handoffsError && (
              <p className="text-sm text-neutral-500">{handoffs.length} {handoffs.length === 1 ? 'pendente' : 'pendentes'}</p>
            )}
          </div>

          {isLoadingHandoffs && <p className="mt-5 text-neutral-600">Carregando atendimentos pendentes...</p>}

          {handoffsError && (
            <div className="mt-5 rounded-lg border border-red-200 bg-red-50 p-4 text-red-900">
              <p className="text-sm">{handoffsError}</p>
            </div>
          )}

          {finalizarHandoffError && (
            <div className="mt-5 rounded-lg border border-red-200 bg-red-50 p-4 text-red-900">
              <p className="text-sm">{finalizarHandoffError}</p>
            </div>
          )}

          {atualizarStatusError && (
            <div className="mt-5 rounded-lg border border-red-200 bg-red-50 p-4 text-red-900">
              <p className="text-sm">{atualizarStatusError}</p>
            </div>
          )}

          {!isLoadingHandoffs && !handoffsError && handoffs.length === 0 && (
            <p className="mt-5 text-neutral-600">Nenhum atendimento pendente no momento.</p>
          )}

          {!isLoadingHandoffs && !handoffsError && handoffs.length > 0 && (
            <div className="mt-5 overflow-x-auto rounded-lg border border-neutral-200">
              <table className="w-full min-w-[960px] border-collapse text-left text-sm">
                <thead className="bg-neutral-100 text-neutral-600">
                  <tr>
                    <th className="px-4 py-3 font-medium">Cliente</th>
                    <th className="px-4 py-3 font-medium">Pedido</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium">Criado em</th>
                    <th className="px-4 py-3 font-medium">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-200">
                  {handoffs.map((handoff) => {
                    const isFinalizando = finalizandoHandoffs.has(handoff.customerId);
                    const isAtualizandoStatus = atualizandoStatusHandoffs.has(handoff.customerId);
                    const isMensagemCopiada = mensagemCopiadaCustomerId === handoff.customerId;
                    const mensagemCliente = handoff.mensagemCliente?.trim();

                    return (
                      <tr key={`${handoff.customerId}-${handoff.criadoEm}`}>
                        <td className="px-4 py-3 font-medium">{handoff.customerId}</td>
                        <td className="px-4 py-3 text-neutral-700">{handoff.resumoPedido}</td>
                        <td className="px-4 py-3 text-neutral-700">{formatarStatus(handoff.status)}</td>
                        <td className="px-4 py-3 text-neutral-700">{formatarCriadoEm(handoff.criadoEm)}</td>
                        <td className="px-4 py-3">
                          <div className="flex flex-wrap gap-2">
                            {statusActions.map((action) => (
                              <button
                                key={action.status}
                                type="button"
                                onClick={() => atualizarStatusAtendimento(handoff.customerId, action.status)}
                                disabled={isAtualizandoStatus || handoff.status === action.status}
                                className="rounded-md border border-neutral-300 px-3 py-1.5 text-sm font-medium text-neutral-800 transition hover:bg-neutral-100 disabled:cursor-not-allowed disabled:opacity-60"
                              >
                                {action.label}
                              </button>
                            ))}
                          </div>
                          <button
                            type="button"
                            onClick={() => finalizarAtendimento(handoff.customerId)}
                            disabled={isFinalizando}
                            className="mt-2 rounded-md border border-neutral-300 px-3 py-1.5 text-sm font-medium text-neutral-800 transition hover:bg-neutral-100 disabled:cursor-not-allowed disabled:opacity-60"
                          >
                            {isFinalizando ? 'Finalizando...' : 'Finalizar'}
                          </button>
                          {mensagemCliente && (
                            <div className="mt-3 max-w-xl rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950">
                              <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                                <p className="font-medium">Mensagem sugerida ao cliente:</p>
                                <button
                                  type="button"
                                  onClick={() => copiarMensagemCliente(handoff.customerId, mensagemCliente)}
                                  className="w-fit rounded-md border border-amber-300 bg-white px-3 py-1.5 text-sm font-medium text-amber-950 transition hover:bg-amber-100"
                                >
                                  {isMensagemCopiada ? 'Copiado!' : 'Copiar mensagem'}
                                </button>
                              </div>
                              <p className="mt-1 whitespace-pre-wrap break-words">{mensagemCliente}</p>
                            </div>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {isLoading && (
          <section className="rounded-lg border border-neutral-200 bg-white p-6 shadow-sm">
            <p className="text-neutral-600">Carregando configuração...</p>
          </section>
        )}

        {error && (
          <section className="rounded-lg border border-red-200 bg-red-50 p-6 text-red-900 shadow-sm">
            <h2 className="text-lg font-semibold">Não foi possível carregar os dados</h2>
            <p className="mt-2 text-sm">{error}</p>
          </section>
        )}

        {config && !isLoading && !error && (
          <>
            <section className="rounded-lg border border-neutral-200 bg-white p-6 shadow-sm">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <h2 className="text-xl font-semibold">Empresa</h2>
                {!isConfigFormOpen && (
                  <button type="button" onClick={iniciarEdicaoConfig} className="w-fit rounded-md bg-neutral-900 px-3 py-2 text-sm font-medium text-white transition hover:bg-neutral-700">
                    Editar configuração
                  </button>
                )}
              </div>

              {configSaveError && (
                <div className="mt-5 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-900">{configSaveError}</div>
              )}

              {isConfigFormOpen && configForm ? (
                <form onSubmit={salvarConfig} className="mt-5 grid gap-4 md:grid-cols-2">
                  <label className="text-sm font-medium text-neutral-700">Nome
                    <input required value={configForm.nome} onChange={(event) => setConfigForm((current) => current && { ...current, nome: event.target.value })} className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 font-normal" />
                  </label>
                  <label className="text-sm font-medium text-neutral-700">Telefone
                    <input value={configForm.telefone} onChange={(event) => setConfigForm((current) => current && { ...current, telefone: event.target.value })} className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 font-normal" />
                  </label>
                  <label className="text-sm font-medium text-neutral-700">Timezone
                    <input required placeholder="America/Sao_Paulo" value={configForm.timezone} onChange={(event) => setConfigForm((current) => current && { ...current, timezone: event.target.value })} className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 font-normal" />
                  </label>
                  <label className="text-sm font-medium text-neutral-700">Formas de pagamento (separadas por vírgula)
                    <input value={configForm.formasPagamento} onChange={(event) => setConfigForm((current) => current && { ...current, formasPagamento: event.target.value })} className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 font-normal" />
                  </label>
                  <fieldset className="grid gap-4 rounded-lg border border-neutral-200 p-4 md:col-span-2 md:grid-cols-3">
                    <legend className="px-1 text-sm font-medium text-neutral-700">Endereço</legend>
                    {(['logradouro', 'numero', 'bairro', 'cidade', 'estado', 'cep'] as const).map((field) => (
                      <label key={field} className="text-sm font-medium capitalize text-neutral-700">{field}
                        <input value={configForm.endereco[field]} onChange={(event) => setConfigForm((current) => current && { ...current, endereco: { ...current.endereco, [field]: event.target.value } })} className="mt-1 block w-full rounded-md border border-neutral-300 px-3 py-2 font-normal" />
                      </label>
                    ))}
                  </fieldset>
                  <fieldset className="rounded-lg border border-neutral-200 p-4 md:col-span-2">
                    <legend className="px-1 text-sm font-medium text-neutral-700">Horário de funcionamento</legend>
                    <div className="mt-3 grid gap-3">
                      {diasSemana.map(([key, label]) => (
                        <div key={key} className="grid items-center gap-2 sm:grid-cols-[120px_1fr_1fr]">
                          <span className="text-sm font-medium">{label}</span>
                          <input type="time" value={configForm.horarioFuncionamento[key]?.abertura ?? ''} onChange={(event) => setConfigForm((current) => current && ({ ...current, horarioFuncionamento: { ...current.horarioFuncionamento, [key]: { ...current.horarioFuncionamento[key], abertura: event.target.value || null } } }))} aria-label={`Abertura ${label}`} className="rounded-md border border-neutral-300 px-3 py-2" />
                          <input type="time" value={configForm.horarioFuncionamento[key]?.fechamento ?? ''} onChange={(event) => setConfigForm((current) => current && ({ ...current, horarioFuncionamento: { ...current.horarioFuncionamento, [key]: { ...current.horarioFuncionamento[key], fechamento: event.target.value || null } } }))} aria-label={`Fechamento ${label}`} className="rounded-md border border-neutral-300 px-3 py-2" />
                        </div>
                      ))}
                    </div>
                  </fieldset>
                  <div className="flex gap-2 md:col-span-2">
                    <button type="submit" disabled={isSalvandoConfig} className="rounded-md bg-neutral-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-60">{isSalvandoConfig ? 'Salvando...' : 'Salvar configuração'}</button>
                    <button type="button" onClick={cancelarEdicaoConfig} disabled={isSalvandoConfig} className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-800">Cancelar</button>
                  </div>
                </form>
              ) : (

              <dl className="mt-5 grid gap-5 md:grid-cols-2">
                <div>
                  <dt className="text-sm font-medium text-neutral-500">Nome</dt>
                  <dd className="mt-1 text-base">{config.empresa.nome}</dd>
                </div>

                <div>
                  <dt className="text-sm font-medium text-neutral-500">Telefone</dt>
                  <dd className="mt-1 text-base">{config.empresa.telefone}</dd>
                </div>

                <div>
                  <dt className="text-sm font-medium text-neutral-500">Timezone</dt>
                  <dd className="mt-1 text-base">{config.empresa.timezone}</dd>
                </div>

                <div>
                  <dt className="text-sm font-medium text-neutral-500">Formas de pagamento</dt>
                  <dd className="mt-2 flex flex-wrap gap-2">
                    {config.empresa.formasPagamento.map((forma) => (
                      <span key={forma} className="rounded-full bg-neutral-100 px-3 py-1 text-sm text-neutral-700">
                        {forma}
                      </span>
                    ))}
                  </dd>
                </div>

                <div className="md:col-span-2">
                  <dt className="text-sm font-medium text-neutral-500">Endereço</dt>
                  <dd className="mt-1 text-base">{enderecoCompleto}</dd>
                </div>
              </dl>
              )}
            </section>

            {!isConfigFormOpen && <section className="rounded-lg border border-neutral-200 bg-white p-6 shadow-sm">
              <h2 className="text-xl font-semibold">Horário de funcionamento</h2>

              <div className="mt-5 overflow-hidden rounded-lg border border-neutral-200">
                <table className="w-full border-collapse text-left text-sm">
                  <thead className="bg-neutral-100 text-neutral-600">
                    <tr>
                      <th className="px-4 py-3 font-medium">Dia</th>
                      <th className="px-4 py-3 font-medium">Horário</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-200">
                    {diasSemana.map(([key, label]) => (
                      <tr key={key}>
                        <td className="px-4 py-3 font-medium">{label}</td>
                        <td className="px-4 py-3 text-neutral-700">
                          {formatarHorario(config.horarioFuncionamento[key])}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
            }

            <section className="rounded-lg border border-neutral-200 bg-white p-6 shadow-sm">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <h2 className="text-xl font-semibold">Catálogo de produtos</h2>
                  {!isLoadingProdutos && !produtosError && (
                    <p className="mt-1 text-sm text-neutral-500">{produtos.length} produtos cadastrados</p>
                  )}
                </div>
                <button
                  type="button"
                  onClick={iniciarNovoProduto}
                  className="w-fit rounded-md bg-neutral-900 px-3 py-2 text-sm font-medium text-white transition hover:bg-neutral-700"
                >
                  Novo produto
                </button>
              </div>

              {produtosError && (
                <div className="mt-5 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-900">{produtosError}</div>
              )}

              {isProductFormOpen && (
                <form onSubmit={salvarProduto} className="mt-5 grid gap-4 rounded-lg border border-neutral-200 bg-neutral-50 p-4 md:grid-cols-2">
                  <h3 className="md:col-span-2 text-lg font-semibold">
                    {produtoEmEdicao ? `Editar ${produtoEmEdicao.nome}` : 'Novo produto'}
                  </h3>
                  {([
                    ['nome', 'Nome'],
                    ['preco', 'Preço (R$ inteiro)'],
                    ['unidadeSingular', 'Unidade singular'],
                    ['unidadePlural', 'Unidade plural'],
                  ] as const).map(([field, label]) => (
                    <label key={field} className="text-sm font-medium text-neutral-700">
                      {label}
                      <input
                        required
                        type={field === 'preco' ? 'number' : 'text'}
                        min={field === 'preco' ? 0 : undefined}
                        step={field === 'preco' ? 1 : undefined}
                        value={produtoForm[field]}
                        onChange={(event) => setProdutoForm((current) => ({ ...current, [field]: event.target.value }))}
                        className="mt-1 block w-full rounded-md border border-neutral-300 bg-white px-3 py-2 font-normal outline-none focus:border-neutral-900"
                      />
                    </label>
                  ))}
                  <label className="md:col-span-2 text-sm font-medium text-neutral-700">
                    Aliases (separados por vírgula)
                    <input
                      required
                      value={produtoForm.aliases}
                      onChange={(event) => setProdutoForm((current) => ({ ...current, aliases: event.target.value }))}
                      className="mt-1 block w-full rounded-md border border-neutral-300 bg-white px-3 py-2 font-normal outline-none focus:border-neutral-900"
                    />
                  </label>
                  <div className="flex gap-2 md:col-span-2">
                    <button type="submit" disabled={isSalvandoProduto} className="rounded-md bg-neutral-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-60">
                      {isSalvandoProduto ? 'Salvando...' : 'Salvar produto'}
                    </button>
                    <button type="button" onClick={cancelarEdicaoProduto} className="rounded-md border border-neutral-300 px-3 py-2 text-sm font-medium text-neutral-800">
                      Cancelar
                    </button>
                  </div>
                </form>
              )}

              {isLoadingProdutos && <p className="mt-5 text-neutral-600">Carregando produtos...</p>}

              {!isLoadingProdutos && !produtosError && produtos.length === 0 && (
                <p className="mt-5 text-neutral-600">Nenhum produto cadastrado.</p>
              )}

              {!isLoadingProdutos && produtos.length > 0 && (
                <div className="mt-5 grid gap-4 md:grid-cols-2">
                  {produtos.map((produto) => (
                    <article key={produto.id} className={`rounded-lg border p-4 ${produto.ativo ? 'border-neutral-200' : 'border-neutral-200 bg-neutral-50 opacity-70'}`}>
                      <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                        <div>
                          <h3 className="text-lg font-semibold capitalize">{produto.nome}</h3>
                          <p className="mt-1 text-sm text-neutral-600">{formatarPreco(produto.preco)} por {produto.unidadeSingular}</p>
                        </div>
                        <span className={`rounded-full px-3 py-1 text-sm ${produto.ativo ? 'bg-emerald-50 text-emerald-800' : 'bg-neutral-200 text-neutral-700'}`}>
                          {produto.ativo ? 'Ativo' : 'Inativo'}
                        </span>
                      </div>
                      <p className="mt-3 text-sm text-neutral-600">Plural: {produto.unidadePlural}</p>
                      <div className="mt-3 flex flex-wrap gap-2">
                        {produto.aliases.map((alias) => <span key={alias} className="rounded-full bg-neutral-100 px-3 py-1 text-sm text-neutral-700">{alias}</span>)}
                      </div>
                      <div className="mt-4 flex gap-2">
                        <button type="button" onClick={() => iniciarEdicaoProduto(produto)} className="rounded-md border border-neutral-300 px-3 py-1.5 text-sm font-medium text-neutral-800">Editar</button>
                        <button type="button" onClick={() => alterarAtivacaoProduto(produto)} disabled={produtoAtivandoId === produto.id} className="rounded-md border border-neutral-300 px-3 py-1.5 text-sm font-medium text-neutral-800 disabled:opacity-60">
                          {produtoAtivandoId === produto.id ? 'Atualizando...' : produto.ativo ? 'Desativar' : 'Ativar'}
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </div>
    </main>
  );
}
