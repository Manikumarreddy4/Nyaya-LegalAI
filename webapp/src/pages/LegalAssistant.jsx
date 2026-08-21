import React, { useState, useEffect, useRef } from 'react';
import { db } from '../firebase';
import { 
  collection, 
  addDoc, 
  getDocs, 
  query, 
  orderBy, 
  doc, 
  setDoc, 
  serverTimestamp 
} from 'firebase/firestore';
import { MessageSquare, Plus, Send, AlertTriangle, ShieldCheck, ChevronRight, HelpCircle, Loader2 } from 'lucide-react';

export default function LegalAssistant({ user, preselectedSessionId }) {
  const [sessions, setSessions] = useState([]);
  const [currentSessionId, setCurrentSessionId] = useState(preselectedSessionId || null);
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingSessions, setLoadingSessions] = useState(true);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    if (preselectedSessionId) {
      setCurrentSessionId(preselectedSessionId);
    }
  }, [preselectedSessionId]);

  // Load chat sessions from Firestore
  useEffect(() => {
    async function loadSessions() {
      if (!user || !user.uid) return;
      try {
        const sessionsRef = collection(db, 'users', user.uid, 'problemHistory');
        const q = query(sessionsRef, orderBy('updatedAt', 'desc'));
        const querySnapshot = await getDocs(q);
        const docs = [];
        querySnapshot.forEach(d => {
          docs.push({ id: d.id, ...d.data() });
        });
        setSessions(docs);
        
        if (docs.length > 0 && !currentSessionId) {
          setCurrentSessionId(docs[0].id);
        }
      } catch (e) {
        console.error('Error fetching sessions', e);
      } finally {
        setLoadingSessions(false);
      }
    }
    loadSessions();
  }, [user]);

  // Load messages for current session
  useEffect(() => {
    async function loadMessages() {
      if (!user || !user.uid || !currentSessionId) {
        setMessages([]);
        return;
      }
      try {
        const messagesRef = collection(db, 'users', user.uid, 'problemHistory', currentSessionId, 'messages');
        const q = query(messagesRef, orderBy('timestamp', 'asc'));
        const querySnapshot = await getDocs(q);
        const msgs = [];
        querySnapshot.forEach(d => {
          msgs.push({ id: d.id, ...d.data() });
        });
        setMessages(msgs);
      } catch (e) {
        console.error('Error loading messages', e);
      }
    }
    loadMessages();
  }, [user, currentSessionId]);

  // Auto-scroll to bottom of chat
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const handleStartNewSession = async () => {
    if (!user || !user.uid) return;
    try {
      const sessionId = 'session_' + Date.now();
      const newSession = {
        sessionId,
        userId: user.uid,
        chatbotType: 'AI_ASSISTANT',
        title: 'New Legal Query ' + new Date().toLocaleDateString(),
        updatedAt: new Date(),
        isPinned: false
      };
      
      // Save session doc in firestore
      await setDoc(doc(db, 'users', user.uid, 'problemHistory', sessionId), newSession);
      
      setSessions(prev => [newSession, ...prev]);
      setCurrentSessionId(sessionId);
      setMessages([]);
    } catch (e) {
      console.error(e);
      alert('Failed to start new chat: ' + e.message);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage(e);
    }
  };

  const handleSendMessage = async (e, forcedText = null) => {
    if (e) e.preventDefault();
    const textToSend = forcedText || inputMessage;
    if (!textToSend.trim() || loading || !user || !user.uid) return;

    let sessionId = currentSessionId;
    
    // Create new session if none selected
    if (!sessionId) {
      try {
        sessionId = 'session_' + Date.now();
        const newSession = {
          sessionId,
          userId: user.uid,
          chatbotType: 'AI_ASSISTANT',
          title: textToSend.substring(0, 30) + '...',
          updatedAt: new Date(),
          isPinned: false
        };
        await setDoc(doc(db, 'users', user.uid, 'problemHistory', sessionId), newSession);
        setSessions(prev => [newSession, ...prev]);
        setCurrentSessionId(sessionId);
      } catch (err) {
        console.error(err);
        return;
      }
    }

    const userMessage = {
      messageId: 'msg_' + Date.now() + '_user',
      sessionId,
      sender: 'User',
      message: textToSend,
      timestamp: new Date()
    };

    setInputMessage('');
    setMessages(prev => [...prev, userMessage]);
    setLoading(true);

    try {
      // Save user message to Firestore
      const userMsgRef = doc(db, 'users', user.uid, 'problemHistory', sessionId, 'messages', userMessage.messageId);
      await setDoc(userMsgRef, userMessage);

      // Call secure backend proxy
      console.log('[AI] Sending message to backend');
      const apiEndpoint = import.meta.env.DEV ? 'http://localhost:5000/api/chat' : '/api/chat';
      const response = await fetch(apiEndpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: textToSend,
          conversation: messages.slice(-10) // Only send the last 10 messages of history
        })
      });

      const responseData = await response.json();
      console.log('[AI] API response status:', response.status);
      console.log('[AI] API response data:', responseData);

      if (!response.ok || !responseData.success) {
        throw new Error(responseData.error || `HTTP ${response.status}`);
      }

      const assistantMessage = {
        messageId: 'msg_' + Date.now() + '_bot',
        sessionId,
        sender: 'Bot',
        message: responseData.reply,
        timestamp: new Date()
      };

      // Save assistant message to Firestore
      const botMsgRef = doc(db, 'users', user.uid, 'problemHistory', sessionId, 'messages', assistantMessage.messageId);
      await setDoc(botMsgRef, assistantMessage);

      // Update session title and time in Firestore
      const sessionDocRef = doc(db, 'users', user.uid, 'problemHistory', sessionId);
      await setDoc(sessionDocRef, {
        updatedAt: new Date(),
        title: textToSend.substring(0, 30) + '...'
      }, { merge: true });

      // Refresh sessions local list
      setSessions(prev => 
        prev.map(s => s.sessionId === sessionId ? { ...s, updatedAt: new Date(), title: textToSend.substring(0, 30) + '...' } : s)
          .sort((a, b) => b.updatedAt - a.updatedAt)
      );

      setMessages(prev => [...prev, assistantMessage]);
    } catch (err) {
      console.error('[AI] Frontend Error:', err);
      const errMsg = {
        messageId: 'msg_' + Date.now() + '_err',
        sessionId,
        sender: 'Bot',
        message: `⚠️ ${err.message}`,
        timestamp: new Date()
      };
      setMessages(prev => [...prev, errMsg]);
    } finally {
      setLoading(false);
    }
  };

  const suggestions = [
    { title: "Threatened by someone", text: "What should I do if someone threatens me?" },
    { title: "Understanding BNS 103", text: "What is BNS Section 103 and how is it applied?" },
    { title: "Tenant rights", text: "What are my rights as a tenant if the landlord locks me out?" },
    { title: "Explain Article 21", text: "Explain Article 21 of the Indian Constitution in simple words." }
  ];

  return (
    <div className="fade-in-up" style={styles.container}>
      {/* Session List Sidebar */}
      <div className="glass-panel" style={styles.sidebar}>
        <div style={styles.sidebarHeader}>
          <h3>Chat History</h3>
          <button onClick={handleStartNewSession} style={styles.newChatBtn} title="New Session">
            <Plus size={18} />
          </button>
        </div>

        {loadingSessions ? (
          <div style={styles.sidebarLoader}>Loading...</div>
        ) : sessions.length === 0 ? (
          <div style={styles.emptySessions}>No history yet. Start a chat!</div>
        ) : (
          <div style={styles.sessionList}>
            {sessions.map((s) => (
              <div 
                key={s.id} 
                style={{
                  ...styles.sessionItem,
                  background: currentSessionId === s.sessionId ? 'var(--primary)' : 'transparent'
                }}
                onClick={() => setCurrentSessionId(s.sessionId)}
              >
                <MessageSquare size={16} style={{flexShrink: 0}} />
                <span style={styles.sessionTitle}>{s.title}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Main Chat Area */}
      <div className="glass-panel" style={styles.chatArea}>
        <div style={styles.chatHeader}>
          <div>
            <h3>AI Legal Assistant</h3>
            <p style={styles.subTitle}>Indian Law Problem Resolver</p>
          </div>
          <span style={styles.warningTag}>AI advice is advisory only</span>
        </div>

        {/* Message Panel */}
        <div style={styles.messagePanel}>
          {messages.length === 0 ? (
            <div style={styles.introContainer}>
              <HelpCircle size={40} color="var(--primary)" style={{marginBottom: '16px'}} />
              <h3>How can I assist you today?</h3>
              <p style={styles.introSubtitle}>Describe your scenario or select one of the templates below to begin:</p>
              
              <div style={styles.suggestionsGrid}>
                {suggestions.map((s, idx) => (
                  <div 
                    key={idx} 
                    className="glass-panel" 
                    style={styles.suggestionCard}
                    onClick={() => handleSendMessage(null, s.text)}
                  >
                    <h4>{s.title}</h4>
                    <p>{s.text}</p>
                    <ChevronRight size={14} style={styles.arrow} />
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div style={styles.messageList}>
              {messages.map((m) => (
                <div 
                  key={m.id} 
                  style={{
                    ...styles.messageRow,
                    justifyContent: m.sender === 'User' ? 'flex-end' : 'flex-start'
                  }}
                >
                  <div 
                    className="glass-panel" 
                    style={{
                      ...styles.messageBubble,
                      background: m.sender === 'User' ? 'var(--primary)' : 'rgba(255,255,255,0.03)',
                      borderColor: m.sender === 'User' ? 'var(--primary)' : 'var(--border)'
                    }}
                  >
                    <div style={styles.senderLabel}>{m.sender}</div>
                    <div style={styles.messageText}>{m.message}</div>
                  </div>
                </div>
              ))}
              {loading && (
                <div style={styles.loadingRow}>
                  <Loader2 className="shimmer" size={24} style={{animation: 'spin 1s linear infinite', color: 'var(--primary)'}} />
                  <span>Thinking...</span>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>
          )}
        </div>

        {/* Input Form */}
        <form onSubmit={handleSendMessage} style={styles.inputForm}>
          <textarea 
            placeholder="Type your legal situation here (e.g. 'I was cheated online...'). Press Enter to send, Shift + Enter for new line." 
            className="input-field" 
            style={{ flex: 1, resize: 'none', height: '48px', padding: '12px 16px', borderRadius: '12px' }}
            value={inputMessage} 
            onChange={(e) => setInputMessage(e.target.value)}
            onKeyDown={handleKeyDown}
            disabled={loading}
          />
          <button type="submit" className="btn btn-primary" style={styles.sendBtn} disabled={loading || !inputMessage.trim()}>
            <Send size={18} />
          </button>
        </form>
      </div>
    </div>
  );
}

const styles = {
  container: {
    display: 'grid',
    gridTemplateColumns: '280px 1fr',
    gap: '20px',
    padding: '24px',
    maxWidth: '1200px',
    margin: '0 auto',
    height: 'calc(100vh - 100px)',
    '@media (max-width: 768px)': {
      gridTemplateColumns: '1fr',
      height: 'auto'
    }
  },
  sidebar: {
    padding: '16px',
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
    height: '100%',
    overflowY: 'auto'
  },
  sidebarHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  newChatBtn: {
    background: 'var(--surface)',
    border: '1px solid var(--border)',
    borderRadius: '8px',
    padding: '6px',
    cursor: 'pointer',
    color: 'var(--text-main)',
    transition: 'all 0.2s',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  },
  sidebarLoader: {
    textAlign: 'center',
    color: 'var(--text-muted)',
    fontSize: '14px',
    padding: '20px 0'
  },
  emptySessions: {
    textAlign: 'center',
    fontSize: '13px',
    color: 'var(--text-muted)',
    padding: '20px'
  },
  sessionList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    overflowY: 'auto'
  },
  sessionItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '12px',
    borderRadius: '12px',
    cursor: 'pointer',
    transition: 'all 0.2s',
    color: 'var(--text-main)'
  },
  sessionTitle: {
    fontSize: '13px',
    fontWeight: '600',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis'
  },
  chatArea: {
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
    overflow: 'hidden'
  },
  chatHeader: {
    padding: '16px 20px',
    borderBottom: '1px solid var(--border)',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: '10px'
  },
  subTitle: {
    fontSize: '12px',
    color: 'var(--text-muted)'
  },
  warningTag: {
    fontSize: '11px',
    fontWeight: '700',
    color: 'var(--accent)',
    background: 'rgba(245, 158, 11, 0.1)',
    padding: '4px 10px',
    borderRadius: '8px',
    border: '1px solid rgba(245, 158, 11, 0.2)'
  },
  messagePanel: {
    flex: 1,
    padding: '20px',
    overflowY: 'auto',
    display: 'flex',
    flexDirection: 'column'
  },
  introContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    flex: 1,
    textAlign: 'center',
    maxWidth: '600px',
    margin: '0 auto',
    padding: '20px'
  },
  introSubtitle: {
    color: 'var(--text-muted)',
    fontSize: '14px',
    marginBottom: '24px'
  },
  suggestionsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
    gap: '16px',
    width: '100%'
  },
  suggestionCard: {
    padding: '16px',
    cursor: 'pointer',
    textAlign: 'left',
    position: 'relative',
    transition: 'all 0.2s',
    ':hover': {
      borderColor: 'var(--primary)'
    }
  },
  arrow: {
    position: 'absolute',
    right: '12px',
    bottom: '12px',
    color: 'var(--text-muted)'
  },
  messageList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  messageRow: {
    display: 'flex',
    width: '100%'
  },
  messageBubble: {
    maxWidth: '80%',
    padding: '16px',
    borderRadius: '16px'
  },
  senderLabel: {
    fontSize: '11px',
    fontWeight: '800',
    textTransform: 'uppercase',
    color: 'var(--accent)',
    marginBottom: '4px'
  },
  messageText: {
    fontSize: '14px',
    lineHeight: '1.6',
    whiteSpace: 'pre-wrap'
  },
  loadingRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    fontSize: '13px',
    color: 'var(--text-muted)'
  },
  inputForm: {
    padding: '16px 20px',
    borderTop: '1px solid var(--border)',
    display: 'flex',
    gap: '12px'
  },
  sendBtn: {
    width: '48px',
    height: '48px',
    padding: 0
  }
};
