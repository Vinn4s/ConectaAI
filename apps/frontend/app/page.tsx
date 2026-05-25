'use client';

import { useState } from 'react';

const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL ?? 'http://localhost:8081';

export default function Home() {
  const [message, setMessage] = useState('');
  const [chat, setChat] = useState<any[]>([]);

  const sendMessage = async () => {
    if (!message) return;

    const userMessage = { sender: 'user', text: message };
    setChat((prev) => [...prev, userMessage]);

    const response = await fetch(`${backendUrl.replace(/\/$/, '')}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
    customerId: "frontend-test-user",
    message: message
    }),
    });

    const data = await response.json();

    const botMessage = {
      sender: 'bot',
      text: data.message,
      type: data.type,
    };

    setChat((prev) => [...prev, botMessage]);

    setMessage('');
  };

  return (
    <div style={{ padding: 20 }}>
      <h1>ConectaAI Chat</h1>

      <div style={{ border: '1px solid #ccc', padding: 10, height: 300, overflowY: 'scroll' }}>
  {chat.map((msg, index) => (
    <div 
      key={index}
      style={{
        backgroundColor: msg.type === "SALE" ? "#d4edda" : "#f1f1f1",
        padding: 8,
        marginBottom: 5,
        borderRadius: 5
      }}
    >
      <strong>{msg.sender}:</strong> {msg.text}
    </div>
  ))}
</div>

      <input
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        placeholder="Digite sua mensagem..."
      />

      <button onClick={sendMessage}>Enviar</button>
    </div>
  );
}
