'use client';

import { useEffect, useMemo, useState } from 'react';

const adminConfigUrl = 'http://localhost:8081/admin/config';
const adminHandoffsUrl = 'http://localhost:8081/admin/handoffs';

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

type AdminConfig = {
  empresa: Empresa;
  horarioFuncionamento: Record<string, HorarioFuncionamento>;
  produtos: Produto[];
};

type HumanHandoff = {
  customerId: string;
  resumoPedido: string;
  criadoEm: string;
};

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

export default function AdminConfigPage() {
  const [config, setConfig] = useState<AdminConfig | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [handoffs, setHandoffs] = useState<HumanHandoff[]>([]);
  const [isLoadingHandoffs, setIsLoadingHandoffs] = useState(true);
  const [handoffsError, setHandoffsError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function carregarConfig() {
      try {
        setIsLoading(true);
        setError(null);

        const response = await fetch(adminConfigUrl);

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

    async function carregarHandoffs() {
      try {
        setIsLoadingHandoffs(true);
        setHandoffsError(null);

        const response = await fetch(adminHandoffsUrl);

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

  return (
    <main className="min-h-screen bg-neutral-50 px-4 py-8 text-neutral-950 sm:px-6 lg:px-8">
      <div className="mx-auto flex max-w-6xl flex-col gap-6">
        <header className="border-b border-neutral-200 pb-4">
          <p className="text-sm font-medium uppercase tracking-wide text-neutral-500">Admin</p>
          <h1 className="mt-2 text-3xl font-semibold">Configuração da empresa</h1>
          <p className="mt-2 text-sm text-neutral-600">Visualização dos dados usados pelo atendimento.</p>
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

          {!isLoadingHandoffs && !handoffsError && handoffs.length === 0 && (
            <p className="mt-5 text-neutral-600">Nenhum atendimento pendente no momento.</p>
          )}

          {!isLoadingHandoffs && !handoffsError && handoffs.length > 0 && (
            <div className="mt-5 overflow-x-auto rounded-lg border border-neutral-200">
              <table className="w-full min-w-[720px] border-collapse text-left text-sm">
                <thead className="bg-neutral-100 text-neutral-600">
                  <tr>
                    <th className="px-4 py-3 font-medium">Cliente</th>
                    <th className="px-4 py-3 font-medium">Pedido</th>
                    <th className="px-4 py-3 font-medium">Criado em</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-200">
                  {handoffs.map((handoff) => (
                    <tr key={`${handoff.customerId}-${handoff.criadoEm}`}>
                      <td className="px-4 py-3 font-medium">{handoff.customerId}</td>
                      <td className="px-4 py-3 text-neutral-700">{handoff.resumoPedido}</td>
                      <td className="px-4 py-3 text-neutral-700">{formatarCriadoEm(handoff.criadoEm)}</td>
                    </tr>
                  ))}
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
              <h2 className="text-xl font-semibold">Empresa</h2>

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
            </section>

            <section className="rounded-lg border border-neutral-200 bg-white p-6 shadow-sm">
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

            <section className="rounded-lg border border-neutral-200 bg-white p-6 shadow-sm">
              <div className="flex flex-col gap-1 sm:flex-row sm:items-end sm:justify-between">
                <h2 className="text-xl font-semibold">Catálogo de produtos</h2>
                <p className="text-sm text-neutral-500">{config.produtos.length} produtos cadastrados</p>
              </div>

              <div className="mt-5 grid gap-4 md:grid-cols-2">
                {config.produtos.map((produto) => (
                  <article key={produto.nome} className="rounded-lg border border-neutral-200 p-4">
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                      <div>
                        <h3 className="text-lg font-semibold capitalize">{produto.nome}</h3>
                        <p className="mt-1 text-sm text-neutral-600">
                          {formatarPreco(produto.preco)} por {produto.unidadeSingular}
                        </p>
                      </div>

                      <span className="rounded-full bg-neutral-100 px-3 py-1 text-sm text-neutral-700">
                        {produto.unidadePlural}
                      </span>
                    </div>

                    <div className="mt-4">
                      <p className="text-sm font-medium text-neutral-500">Aliases</p>
                      <div className="mt-2 flex flex-wrap gap-2">
                        {produto.aliases.map((alias) => (
                          <span key={alias} className="rounded-full bg-neutral-50 px-3 py-1 text-sm text-neutral-700">
                            {alias}
                          </span>
                        ))}
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </section>
          </>
        )}
      </div>
    </main>
  );
}
