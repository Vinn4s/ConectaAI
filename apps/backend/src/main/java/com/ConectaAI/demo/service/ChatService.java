package com.ConectaAI.demo.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.ConectaAI.demo.dto.ChatResponse;
import com.ConectaAI.demo.model.Produto;

@Service
public class ChatService {

    private final GroqService groqService;
    private final IntentInterpreterService intentInterpreterService;
    private final CompanyConfigService companyConfigService;
    private final Map<String, Produto> produtos;

    private final Map<String, PedidoPendente> pedidosPendentes = new ConcurrentHashMap<>();
    private final Set<String> clientesAguardandoHumano = ConcurrentHashMap.newKeySet();

    private static final String NUMEROS_PATTERN =
        "\\d+|um|uma|dois|duas|três|tres|quatro|cinco|seis|sete|oito|nove|dez";

    public ChatService(
        GroqService groqService,
        IntentInterpreterService intentInterpreterService,
        CompanyConfigService companyConfigService,
        ProductCatalogService productCatalogService
    ) {
        this.groqService = groqService;
        this.intentInterpreterService = intentInterpreterService;
        this.companyConfigService = companyConfigService;
        this.produtos = productCatalogService.getProdutos();
    }

    public ChatResponse processMessage(String customerId, String message) {

        String cliente = normalizarCustomerId(customerId);
        String originalMessage = message == null ? "" : message;
        String msg = originalMessage.toLowerCase();

        if (msg.trim().equals("/reset")) {
            pedidosPendentes.remove(cliente);
            clientesAguardandoHumano.remove(cliente);

            return new ChatResponse(
                "Atendimento reiniciado para testes.",
                false,
                "INFO"
            );
        }

        if (clientesAguardandoHumano.contains(cliente)) {
            return new ChatResponse(
                "Seu atendimento já foi encaminhado para um atendente humano. Aguarde um instante, por favor. 😊",
                true,
                "HUMAN_WAITING"
            );
        }

        if (isPerguntaCatalogoGeral(msg)) {
        return new ChatResponse(
        montarCatalogoCompleto(),
        false,
        "INFO"
        );
        }

        boolean hasPendingOrder = pedidosPendentes.containsKey(cliente);

        String intent = intentInterpreterService
            .analyze(originalMessage, hasPendingOrder)
            .getIntent();

        System.out.println("Intenção detectada: " + intent);

        if ("ASK_HOURS".equals(intent)) {
            return new ChatResponse(
                companyConfigService.getRespostaHorarioFuncionamento(),
                false,
                "INFO"
            );
        }

        List<Produto> produtosEncontrados = encontrarProdutos(msg);

        if (pedidosPendentes.containsKey(cliente)) {

            PedidoPendente pedidoAtual = pedidosPendentes.get(cliente);

            if ("CONFIRM_ORDER".equals(intent)) {
                pedidosPendentes.remove(cliente);
                clientesAguardandoHumano.add(cliente);

                return new ChatResponse(
                    montarMensagemConfirmacao(pedidoAtual),
                    true,
                    "SALE"
                );
            }

            if ("CANCEL_ORDER".equals(intent)) {
                pedidosPendentes.remove(cliente);

                return new ChatResponse(
                    "Tudo bem, cancelei esse pedido. Posso ajudar com mais alguma coisa?",
                    false,
                    "INFO"
                );
            }

            if ("PRICE_OBJECTION".equals(intent)) {
                return new ChatResponse(
                    "Entendo 😅 Seu pedido atual continua sendo: " +
                    montarResumoPedidoCurto(pedidoAtual) +
                    ". Deseja confirmar, alterar ou cancelar?",
                    false,
                    "INFO"
                );
            }

            if ("ASK_PRODUCT_INFO".equals(intent) && !produtosEncontrados.isEmpty()) {
                return new ChatResponse(
                    montarRespostaInformativa(produtosEncontrados) +
                    "\n\nSeu pedido atual continua sendo: " +
                    montarResumoPedidoCurto(pedidoAtual) +
                    ". Deseja confirmar, alterar ou cancelar?",
                    false,
                    "INFO"
                );
            }

            if (("ADD_TO_ORDER".equals(intent) || "BUY".equals(intent)) && !produtosEncontrados.isEmpty()) {
                List<ItemPedido> novosItens = extrairItensPedido(msg);

                PedidoPendente pedidoAtualizado = mesclarPedido(pedidoAtual, novosItens);
                pedidosPendentes.put(cliente, pedidoAtualizado);

                return new ChatResponse(
                    montarMensagemPedidoAtualizado(pedidoAtualizado),
                    false,
                    "INFO"
                );
            }

            if ("REPLACE_ORDER".equals(intent) && !produtosEncontrados.isEmpty()) {
                List<ItemPedido> novosItens = extrairItensPedido(msg);

                PedidoPendente pedidoAtualizado = new PedidoPendente(novosItens);
                pedidosPendentes.put(cliente, pedidoAtualizado);

                return new ChatResponse(
                    montarMensagemPedidoAtualizado(pedidoAtualizado),
                    false,
                    "INFO"
                );
            }

            if ("REMOVE_FROM_ORDER".equals(intent)) {
                List<ItemPedido> itensParaRemover = extrairItensPedido(msg);

                if (itensParaRemover.isEmpty()) {
                    return new ChatResponse(
                        "Qual item você deseja remover do pedido?",
                        false,
                        "INFO"
                    );
                }

                PedidoPendente pedidoAtualizado = removerItensDoPedido(pedidoAtual, itensParaRemover);

                if (pedidoAtualizado.itens().isEmpty()) {
                    pedidosPendentes.remove(cliente);

                    return new ChatResponse(
                        "Removi os itens informados. Seu pedido ficou vazio. Posso ajudar com outro pedido?",
                        false,
                        "INFO"
                    );
                }

                pedidosPendentes.put(cliente, pedidoAtualizado);

                return new ChatResponse(
                    montarMensagemPedidoAtualizado(pedidoAtualizado),
                    false,
                    "INFO"
                );
            }

            return new ChatResponse(
                "Seu pedido atual é: " +
                montarResumoPedidoCurto(pedidoAtual) +
                ". Deseja confirmar, alterar ou cancelar?",
                false,
                "INFO"
            );
        }

        if (!produtosEncontrados.isEmpty()) {

            if ("ASK_PRODUCT_INFO".equals(intent)) {
                return new ChatResponse(
                    montarRespostaInformativa(produtosEncontrados),
                    false,
                    "INFO"
                );
            }

            if ("BUY".equals(intent)) {
                List<ItemPedido> itens = extrairItensPedido(msg);

                if (itens.isEmpty()) {
                    return new ChatResponse(
                        "Não consegui identificar os produtos do pedido. Pode me dizer quais itens deseja?",
                        false,
                        "INFO"
                    );
                }

                PedidoPendente pedido = new PedidoPendente(itens);
                pedidosPendentes.put(cliente, pedido);

                return new ChatResponse(
                    montarMensagemPedido(pedido),
                    false,
                    "INFO"
                );
            }

            if ("PRICE_OBJECTION".equals(intent)) {
                return new ChatResponse(
                    montarRespostaObjeçãoPreco(produtosEncontrados),
                    false,
                    "INFO"
                );
            }

            return new ChatResponse(
                montarRespostaInformativa(produtosEncontrados) + " Deseja comprar?",
                false,
                "INFO"
            );
        }



        return new ChatResponse(
            groqService.callGroq(originalMessage),
            false,
            "INFO"
        );
    }

    private List<Produto> encontrarProdutos(String msg) {
        List<Produto> encontrados = new ArrayList<>();

        for (Produto produto : produtos.values()) {
            for (String alias : produto.aliases()) {
                if (msg.contains(alias)) {
                    encontrados.add(produto);
                    break;
                }
            }
        }

        return encontrados;
    }

    private List<ItemPedido> extrairItensPedido(String msg) {
        Map<String, ItemPedido> itens = new LinkedHashMap<>();

        for (Produto produto : produtos.values()) {
            for (String alias : produto.aliases()) {

                String regex =
                    "(?:(\\b(?:" + NUMEROS_PATTERN + ")\\b)\\s*)?" +
                    "(?:(?:pacotes?|unidades?|unds?|un|kg|quilos?)\\s*)?" +
                    "(?:de\\s+)?" +
                    "\\b" + Pattern.quote(alias) + "\\b";

                Pattern pattern = Pattern.compile(
                    regex,
                    Pattern.CASE_INSENSITIVE |
                    Pattern.UNICODE_CASE |
                    Pattern.UNICODE_CHARACTER_CLASS
                );

                Matcher matcher = pattern.matcher(msg);

                if (matcher.find()) {
                    String quantidadeTexto = matcher.group(1);
                    int quantidade = quantidadeTexto == null ? 1 : converterQuantidade(quantidadeTexto);

                    ItemPedido existente = itens.get(produto.nome());

                    if (existente == null) {
                        itens.put(produto.nome(), new ItemPedido(produto, quantidade));
                    } else {
                        itens.put(
                            produto.nome(),
                            new ItemPedido(produto, existente.quantidade() + quantidade)
                        );
                    }

                    break;
                }
            }
        }

        return new ArrayList<>(itens.values());
    }

    private String montarRespostaInformativa(List<Produto> produtosEncontrados) {
        if (produtosEncontrados.size() == 1) {
            Produto produto = produtosEncontrados.get(0);

            return "Sim, temos " + produto.nome() +
                " disponível. O valor é R$ " + produto.preco() + ".";
        }

        StringBuilder resposta = new StringBuilder("Sim, temos disponível:\n");

        for (Produto produto : produtosEncontrados) {
            resposta.append("- ")
                .append(produto.nome())
                .append(": R$ ")
                .append(produto.preco())
                .append("\n");
        }

        return resposta.toString().trim();
    }

    private String montarMensagemPedido(PedidoPendente pedido) {
        int total = calcularTotal(pedido);

        if (pedido.itens().size() == 1) {
            ItemPedido item = pedido.itens().get(0);

            return "Perfeito! " + montarDescricaoItem(item) +
                ", total de R$ " + total +
                ". Deseja confirmar o pedido?";
        }

        StringBuilder resposta = new StringBuilder("Perfeito! Seu pedido ficou:\n");

        for (ItemPedido item : pedido.itens()) {
            resposta.append("- ")
                .append(montarDescricaoItem(item))
                .append(": R$ ")
                .append(calcularSubtotal(item))
                .append("\n");
        }

        resposta.append("\nTotal: R$ ")
            .append(total)
            .append(".\nDeseja confirmar o pedido?");

        return resposta.toString();
    }

    private String montarMensagemConfirmacao(PedidoPendente pedido) {
        if (pedido.itens().size() == 1) {
            return "Perfeito! 😊 Pedido confirmado: " +
                montarDescricaoItem(pedido.itens().get(0)) +
                ". Vou te encaminhar para finalizar a compra.";
        }

        StringBuilder resposta = new StringBuilder("Perfeito! 😊 Pedido confirmado:\n");

        for (ItemPedido item : pedido.itens()) {
            resposta.append("- ")
                .append(montarDescricaoItem(item))
                .append("\n");
        }

        resposta.append("Vou te encaminhar para finalizar a compra.");

        return resposta.toString();
    }

    private String montarDescricaoItem(ItemPedido item) {
        Produto produto = item.produto();

        String unidade = item.quantidade() == 1
            ? produto.unidadeSingular()
            : produto.unidadePlural();

        return item.quantidade() + " " + unidade + " de " + produto.nome();
    }

    private int calcularTotal(PedidoPendente pedido) {
        int total = 0;

        for (ItemPedido item : pedido.itens()) {
            total += calcularSubtotal(item);
        }

        return total;
    }

    private int calcularSubtotal(ItemPedido item) {
        return item.quantidade() * item.produto().preco();
    }

    private int converterQuantidade(String quantidadeTexto) {
        if (quantidadeTexto == null || quantidadeTexto.isBlank()) {
            return 1;
        }

        String valor = quantidadeTexto.toLowerCase();

        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException ignored) {
        }

        return switch (valor) {
            case "um", "uma" -> 1;
            case "dois", "duas" -> 2;
            case "três", "tres" -> 3;
            case "quatro" -> 4;
            case "cinco" -> 5;
            case "seis" -> 6;
            case "sete" -> 7;
            case "oito" -> 8;
            case "nove" -> 9;
            case "dez" -> 10;
            default -> 1;
        };
    }

    private String montarRespostaObjeçãoPreco(List<Produto> produtosEncontrados) {
        if (produtosEncontrados.size() == 1) {
            Produto produto = produtosEncontrados.get(0);

            return "Entendo 😅 O valor atual de " + produto.nome() +
                " é R$ " + produto.preco() +
                ". Posso te ajudar com outro produto ou informação?";
        }

        return "Entendo 😅 Esses são os valores atuais dos produtos. Posso te ajudar com outro produto ou informação?";
    }

    private PedidoPendente mesclarPedido(PedidoPendente pedidoAtual, List<ItemPedido> novosItens) {
        Map<String, ItemPedido> mapa = new LinkedHashMap<>();

        for (ItemPedido item : pedidoAtual.itens()) {
            mapa.put(item.produto().nome(), item);
        }

        for (ItemPedido item : novosItens) {
            ItemPedido existente = mapa.get(item.produto().nome());

            if (existente == null) {
                mapa.put(item.produto().nome(), item);
            } else {
                mapa.put(
                    item.produto().nome(),
                    new ItemPedido(
                        item.produto(),
                        existente.quantidade() + item.quantidade()
                    )
                );
            }
        }

        return new PedidoPendente(new ArrayList<>(mapa.values()));
    }

    private PedidoPendente removerItensDoPedido(PedidoPendente pedidoAtual, List<ItemPedido> itensParaRemover) {
        Map<String, ItemPedido> mapa = new LinkedHashMap<>();

        for (ItemPedido item : pedidoAtual.itens()) {
            mapa.put(item.produto().nome(), item);
        }

        for (ItemPedido itemRemover : itensParaRemover) {
            ItemPedido existente = mapa.get(itemRemover.produto().nome());

            if (existente == null) {
                continue;
            }

            int novaQuantidade = existente.quantidade() - itemRemover.quantidade();

            if (novaQuantidade <= 0) {
                mapa.remove(itemRemover.produto().nome());
            } else {
                mapa.put(
                    existente.produto().nome(),
                    new ItemPedido(existente.produto(), novaQuantidade)
                );
            }
        }

        return new PedidoPendente(new ArrayList<>(mapa.values()));
    }

    private String montarMensagemPedidoAtualizado(PedidoPendente pedido) {
        int total = calcularTotal(pedido);

        if (pedido.itens().size() == 1) {
            ItemPedido item = pedido.itens().get(0);

            return "Certo, atualizei seu pedido: " +
                montarDescricaoItem(item) +
                ", total de R$ " + total +
                ". Deseja confirmar?";
        }

        StringBuilder resposta = new StringBuilder("Certo, atualizei seu pedido:\n");

        for (ItemPedido item : pedido.itens()) {
            resposta.append("- ")
                .append(montarDescricaoItem(item))
                .append(": R$ ")
                .append(calcularSubtotal(item))
                .append("\n");
        }

        resposta.append("\nTotal: R$ ")
            .append(total)
            .append(".\nDeseja confirmar?");

        return resposta.toString();
    }

    private String montarResumoPedidoCurto(PedidoPendente pedido) {
        StringBuilder resumo = new StringBuilder();

        for (int i = 0; i < pedido.itens().size(); i++) {
            resumo.append(montarDescricaoItem(pedido.itens().get(i)));

            if (i < pedido.itens().size() - 1) {
                resumo.append(", ");
            }
        }

        return resumo.toString();
    }

    private String normalizarCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return "cliente-sem-id";
        }

        return customerId;
    }

    private record ItemPedido(
        Produto produto,
        int quantidade
    ) {}

    private record PedidoPendente(
        List<ItemPedido> itens
    ) {}

    private boolean isPerguntaCatalogoGeral(String msg) {
    return msg.contains("catálogo") ||
           msg.contains("catalogo") ||
           msg.contains("produtos") ||
           msg.contains("o que vocês têm") ||
           msg.contains("o que voces tem") ||
           msg.contains("o que tem") ||
           msg.contains("o que vende") ||
           msg.contains("vocês vendem") ||
           msg.contains("voces vendem");
}

private String montarCatalogoCompleto() {
    StringBuilder resposta = new StringBuilder("Nossa lista de produtos inclui:\n\n");

    for (Produto produto : produtos.values()) {
        resposta.append("- ")
            .append(capitalizar(produto.nome()))
            .append(" - R$ ")
            .append(produto.preco())
            .append(" por ")
            .append(produto.unidadeSingular())
            .append("\n");
    }

    resposta.append("\nVocê gostaria de saber mais sobre algum desses produtos?");

    return resposta.toString().trim();
}

private String capitalizar(String texto) {
    if (texto == null || texto.isBlank()) {
        return texto;
    }

    return texto.substring(0, 1).toUpperCase() + texto.substring(1);
}
}
