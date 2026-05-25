import makeWASocket, {
  DisconnectReason,
  useMultiFileAuthState,
  fetchLatestBaileysVersion
} from '@whiskeysockets/baileys';

import qrcode from 'qrcode-terminal';
import axios from 'axios';
import pino from 'pino';

const BACKEND_BASE_URL = process.env.BACKEND_URL ?? 'http://localhost:8081';
const BACKEND_URL = `${BACKEND_BASE_URL.replace(/\/$/, '')}/chat`;

const BOT_START_TIME = Math.floor(Date.now() / 1000);
const processedMessages = new Set();

const ALLOWED_JIDS = [
  '558396797455@s.whatsapp.net',
  '5583996797455@s.whatsapp.net',
  '38260105564200@lid'
];



function extractTextMessage(message) {
  if (!message) return null;

  if (message.conversation) {
    return message.conversation;
  }

  if (message.extendedTextMessage?.text) {
    return message.extendedTextMessage.text;
  }

  if (message.imageMessage?.caption) {
    return message.imageMessage.caption;
  }

  if (message.videoMessage?.caption) {
    return message.videoMessage.caption;
  }

  return null;
}

async function sendToBackend(text, customerId) {
  const response = await axios.post(BACKEND_URL, {
    customerId: customerId,
    message: text
  });

  return response.data;
}

async function startBot() {
  const { state, saveCreds } = await useMultiFileAuthState('./auth_info_baileys');

  const { version } = await fetchLatestBaileysVersion();

  const sock = makeWASocket({
    version,
    auth: state,
    logger: pino({ level: 'silent' }),
    browser: ['ConectaAI', 'Chrome', '1.0.0']
  });

  sock.ev.on('creds.update', saveCreds);

  sock.ev.on('connection.update', (update) => {
    const { connection, lastDisconnect, qr } = update;

    if (qr) {
      console.log('\n📲 Escaneie este QR Code no WhatsApp:\n');
      qrcode.generate(qr, { small: true });
    }

    if (connection === 'open') {
      console.log('✅ WhatsApp conectado ao ConectaAI!');
    }

    if (connection === 'close') {
      const statusCode = lastDisconnect?.error?.output?.statusCode;

      console.log('❌ Conexão fechada. Código:', statusCode);

      const shouldReconnect = statusCode !== DisconnectReason.loggedOut;

      if (shouldReconnect) {
        console.log('🔄 Tentando reconectar...');
        startBot();
      } else {
        console.log('🚪 Sessão encerrada. Apague a pasta auth_info_baileys e conecte de novo.');
      }
    }
  });

  sock.ev.on('messages.upsert', async ({ messages, type }) => {
  try {
    // Só processa mensagens novas recebidas em tempo real
    if (type !== 'notify') {
  return;
}

    const msg = messages[0];

    if (!msg?.message) return;

    // Ignora mensagens enviadas por você mesmo
    if (msg.key.fromMe) return;

    const remoteJid = msg.key.remoteJid;

    if (!remoteJid) return;

    // Ignora grupos
    if (remoteJid.endsWith('@g.us')) {
    return;
  }

    // Ignora status
    if (remoteJid === 'status@broadcast') {
    return;
  }

  const isPrivateChat =
  remoteJid.endsWith('@s.whatsapp.net') ||
  remoteJid.endsWith('@lid');

if (!isPrivateChat) {
  console.log('Mensagem ignorada: JID não privado.', remoteJid);
  return;
}

    // Whitelist opcional para testes
    if (ALLOWED_JIDS.length > 0 && !ALLOWED_JIDS.includes(remoteJid)) {
      console.log('Mensagem ignorada: contato fora da whitelist.', remoteJid);
      return;
    }

    // Evita processar mensagem duplicada
    const messageId = msg.key.id;

    if (processedMessages.has(messageId)) {
      console.log('Mensagem duplicada ignorada:', messageId);
      return;
    }

    processedMessages.add(messageId);

    // Evita processar histórico antigo
    const messageTimestamp = Number(msg.messageTimestamp);

    if (messageTimestamp && messageTimestamp < BOT_START_TIME - 10) {
      console.log('Mensagem antiga ignorada:', messageTimestamp);
      return;
    }

    const text = extractTextMessage(msg.message);

    if (!text) {
      console.log('Mensagem ignorada: conteúdo sem texto.');
        return;
    }

    console.log(`📩 Mensagem recebida de ${remoteJid}: ${text}`);

    const botResponse = await sendToBackend(text, remoteJid);

    console.log('🤖 Resposta do backend:', botResponse);

    if (botResponse.type === 'HUMAN_WAITING') {
      console.log('⏸️ Cliente aguardando atendimento humano. Bot não respondeu:', remoteJid);
        return;
    }

    await sock.sendMessage(remoteJid, {
      text: botResponse.message
    });

    if (botResponse.transferToHuman) {
      console.log('⚠️ Atendimento humano solicitado para:', remoteJid);
    }

  } catch (error) {
    console.error('Erro ao processar mensagem:', error.message);
  }
});
}

startBot();
