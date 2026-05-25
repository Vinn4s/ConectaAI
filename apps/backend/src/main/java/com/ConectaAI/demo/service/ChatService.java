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

@Service
public class ChatService {

    private final GroqService groqService;

    public ChatService(GroqService groqService) {
        this.groqService = groqService;
    }

    private final Map<String, PedidoPendente> pedidosPendentes = new ConcurrentHashMap<>();

    private final Set<String> clientesAguardandoHumano = ConcurrentHashMap.newKeySet();

    private final Map<String, Produto> produtos = criarCatalogo();

    private static final String NUMEROS_PATTERN =
        "\\d+|um|uma|dois|duas|três|tres|quatro|cinco|seis|sete|oito|nove|dez";

    private Map<String, Produto> criarCatalogo() {
        Map<String, Produto> catalogo = new LinkedHashMap<>();

        catalogo.put("arroz", new Produto(
            "arroz",
            20,
            "pacote",
            "pacotes",
            List.of("arroz")
        ));

        catalogo.put("feijão", new Produto(
            "feijão",
            8,
            "pacote",
            "pacotes",
            List.of("feijão", "feijao")
        ));

        catalogo.put("refrigerante", new Produto(
            "refrigerante",
            6,
            "unidade",
            "unidades",
            List.of("refrigerante", "refrigerantes", "refri")
        ));

        catalogo.put("chocolate", new Produto(
            "chocolate",
            5,
            "unidade",
            "unidades",
            List.of("chocolate", "chocolates")
        ));

        return catalogo;
    }

    public ChatResponse processMessage(String customerId, String message) {

        String cliente = normalizarCustomerId(customerId);
        String msg = message == null ? "" : message.toLowerCase();

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

        if (pedidosPendentes.containsKey(cliente)) {

            if (isConfirmacao(msg)) {
                PedidoPendente pedidoConfirmado = pedidosPendentes.get(cliente);

                pedidosPendentes.remove(cliente);
                clientesAguardandoHumano.add(cliente);

                return new ChatResponse(
                    montarMensagemConfirmacao(pedidoConfirmado),
                    true,
                    "SALE"
                );
            }

            if (isReclamacaoPreco(msg)) {
                return new ChatResponse(
                    "Entendo 😅 Esse é o valor atual do produto. Deseja manter o pedido?",
                    false,
                    "INFO"
                );
            }

            if (isCancelamento(msg)) {
                pedidosPendentes.remove(cliente);

                return new ChatResponse(
                    "Tudo bem, cancelei esse pedido. Posso ajudar com mais alguma coisa?",
                    false,
                    "INFO"
                );
            }
        }

        List<Produto> produtosEncontrados = encontrarProdutos(msg);

        if (!produtosEncontrados.isEmpty()) {

            if (isPerguntaSobreProduto(msg)) {
                return new ChatResponse(
                    montarRespostaInformativa(produtosEncontrados),
                    false,
                    "INFO"
                );
            }

            if (isIntencaoDeCompra(msg)) {
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

            return new ChatResponse(
                montarRespostaInformativa(produtosEncontrados) + " Deseja comprar?",
                false,
                "INFO"
            );
        }

        return new ChatResponse(
            groqService.callGroq(message),
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

    private boolean isPerguntaSobreProduto(String msg) {
        return msg.contains("?") ||
               msg.contains("tem ") ||
               msg.contains("têm ") ||
               msg.contains("temos") ||
               msg.contains("estoque") ||
               msg.contains("disponível") ||
               msg.contains("disponivel") ||
               msg.contains("quanto custa") ||
               msg.contains("qual o valor") ||
               msg.contains("preço") ||
               msg.contains("preco") ||
               msg.contains("vende") ||
               msg.contains("vocês têm") ||
               msg.contains("voces tem");
    }

    private boolean isIntencaoDeCompra(String msg) {
        return msg.contains("quero comprar") ||
               msg.contains("quero ") ||
               msg.contains("vou levar") ||
               msg.contains("vou querer") ||
               msg.contains("me separa") ||
               msg.contains("separa ") ||
               msg.contains("pode separar") ||
               msg.contains("comprar") ||
               msg.contains("pedido");
    }

    private boolean isConfirmacao(String msg) {
    String texto = msg
        .toLowerCase()
        .replaceAll("[!?.]", "")
        .trim();

    return texto.equals("sim") ||
           texto.equals("s") ||
           texto.contains("sim") ||
           texto.contains("desejo sim") ||
           texto.contains("desejo manter") ||
           texto.contains("quero sim") ||
           texto.contains("quero manter") ||
           texto.contains("pode confirmar") ||
           texto.contains("confirmo") ||
           texto.contains("pode ser") ||
           texto.contains("beleza") ||
           texto.contains("isso mesmo") ||
           texto.contains("isso");
}

    private boolean isCancelamento(String msg) {
        return msg.equals("não") ||
               msg.equals("nao") ||
               msg.contains("não quero") ||
               msg.contains("nao quero") ||
               msg.contains("cancelar") ||
               msg.contains("cancela") ||
               msg.contains("deixa pra lá") ||
               msg.contains("deixa pra la");
    }

    private boolean isReclamacaoPreco(String msg) {
        return msg.contains("caro") ||
               msg.contains("muito caro") ||
               msg.contains("absurdo") ||
               msg.contains("barato não") ||
               msg.contains("barato nao");
    }

    private String normalizarCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return "cliente-sem-id";
        }

        return customerId;
    }

    private record Produto(
        String nome,
        int preco,
        String unidadeSingular,
        String unidadePlural,
        List<String> aliases
    ) {}

    private record ItemPedido(
        Produto produto,
        int quantidade
    ) {}

    private record PedidoPendente(
        List<ItemPedido> itens
    ) {}
}