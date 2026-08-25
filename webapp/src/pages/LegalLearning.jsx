import React, { useState, useEffect } from 'react';
import { db } from '../firebase';
import { 
  collection, 
  getDocs, 
  query, 
  orderBy, 
  limit, 
  doc, 
  setDoc, 
  addDoc,
  onSnapshot,
  deleteDoc
} from 'firebase/firestore';
import { BookOpen, Search, HelpCircle, History, Sparkles, Loader2, ArrowRight, Trash2 } from 'lucide-react';

export default function LegalLearning({ user }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);
  const [history, setHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(true);

  // Load search history from Firestore in real-time
  useEffect(() => {
    if (!user || !user.uid) return;
    setLoadingHistory(true);
    const historyRef = collection(db, 'users', user.uid, 'learningHistory');
    const q = query(historyRef, orderBy('timestamp', 'desc'), limit(10));
    
    const unsubscribe = onSnapshot(q, (querySnapshot) => {
      const docs = [];
      querySnapshot.forEach(d => {
        const data = d.data();
        const queryText = (data.query || data.question || '').trim();
        const explanationText = (data.explanation || data.answer || '').trim();
        if (queryText.length > 0) {
          docs.push({ 
            id: d.id, 
            ...data,
            query: queryText,
            question: queryText,
            explanation: explanationText,
            answer: explanationText
          });
        }
      });
      setHistory(docs);
      setLoadingHistory(false);
    }, (error) => {
      console.error('Error listening to learningHistory', error);
      setLoadingHistory(false);
    });

    return () => unsubscribe();
  }, [user]);

  const handleDeleteHistoryItem = async (e, itemId) => {
    if (e) e.stopPropagation();
    if (!window.confirm("Are you sure you want to delete this learning history item?")) return;
    try {
      console.log(`[HISTORY_DELETE] Deleting history ID = ${itemId}`);
      const docRef = doc(db, 'users', user.uid, 'learningHistory', String(itemId));
      await deleteDoc(docRef);
      console.log(`[HISTORY_DELETE] Delete successful`);
    } catch (err) {
      console.error(`[HISTORY_DELETE] Delete failed = ${err.message}`);
    }
  };

  const handleSearch = async (e, forcedQuery = null) => {
    if (e) e.preventDefault();
    const queryText = forcedQuery || searchQuery;
    if (!queryText.trim() || loading) return;

    setLoading(true);
    setResult('');

    try {
      const systemPrompt = "You are an expert Indian Legal AI Assistant. Explain IPC, BNS, CrPC, BNSS, Constitution and Evidence Act in simple language with sections, punishment and examples.";

      // Call secure backend proxy
      const apiEndpoint = import.meta.env.DEV ? 'http://localhost:5000/api/chat' : '/api/chat';
      const response = await fetch(apiEndpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: queryText,
          conversation: [],
          isLearning: true
        })
      });

      const responseData = await response.json();
      if (!response.ok || !responseData.success) {
        throw new Error(responseData.error || `HTTP ${response.status}`);
      }

      const explanation = responseData.reply;
      setResult(explanation);

      // Save to Firestore history
      if (user && user.uid) {
        const historyRef = collection(db, 'users', user.uid, 'learningHistory');
        const newDocId = 'learn_' + Date.now();
        const entry = {
          id: newDocId,
          query: queryText,
          question: queryText,
          explanation: explanation,
          answer: explanation,
          timestamp: Date.now() // Save as millisecond Long number for Android compatibility
        };
        await setDoc(doc(historyRef, newDocId), entry);
      }
    } catch (err) {
      console.error(err);
      setResult(`⚠️ ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const sampleSearches = [
    "IPC Section 302",
    "BNS Section 103",
    "Article 21 of Constitution",
    "Section 420 Punishment",
    "CrPC 154 FIR"
  ];

  const validHistory = history.filter(
    item =>
      item &&
      (item.query || item.question) &&
      (item.query || item.question).trim().length > 0
  );

  return (
    <div className="fade-in-up" style={styles.container}>
      <div style={styles.header}>
        <Sparkles size={36} color="var(--secondary)" />
        <h2>AI Legal Learning</h2>
        <p style={styles.subtitle}>Enter any section, article, or legal term from BNS, IPC, Constitution, or CrPC to learn in simple language.</p>
      </div>

      {/* Search Input Card */}
      <div className="glass-panel" style={styles.searchCard}>
        <form onSubmit={handleSearch} style={styles.searchForm}>
          <div style={styles.inputWrapper}>
            <Search size={20} color="var(--primary)" style={styles.searchIcon} />
            <input 
              type="text" 
              placeholder="e.g. IPC 302, Article 21, CrPC 154..." 
              className="input-field" 
              style={styles.searchField}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              disabled={loading}
              required
            />
          </div>
          <button type="submit" className="btn btn-primary" style={styles.searchBtn} disabled={loading}>
            {loading ? <Loader2 size={18} style={{animation: 'spin 1s linear infinite'}} /> : 'Explain'}
          </button>
        </form>

        {/* Quick Suggestion Tags */}
        <div style={styles.suggestionsRow}>
          <span style={styles.suggestLabel}>Quick Search:</span>
          {sampleSearches.map((s, idx) => (
            <button 
              key={idx} 
              type="button" 
              style={styles.suggestTag} 
              onClick={() => { setSearchQuery(s); handleSearch(null, s); }}
              disabled={loading}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      {/* Main Results Panel */}
      {result && (
        <div className="glass-panel fade-in-up" style={styles.resultCard}>
          <div style={styles.resultHeader}>
            <BookOpen size={20} color="var(--secondary)" />
            <h3>Explanation & Breakdown</h3>
          </div>
          <div style={styles.resultText}>{result}</div>
        </div>
      )}

      {/* History panel */}
      <div className="glass-panel" style={styles.historyCard}>
        <div style={styles.historyHeader}>
          <History size={18} color="var(--primary)" />
          <h3>Recent Learning Searches</h3>
        </div>
        {loadingHistory ? (
          <div style={styles.loader}>Loading search history...</div>
        ) : validHistory.length === 0 ? (
          <div style={styles.emptyHistory}>No recent learning searches.</div>
        ) : (
          <div style={styles.historyGrid}>
            {validHistory.map((h) => (
              <div 
                key={h.id} 
                className="glass-panel" 
                style={styles.historyItem}
                onClick={() => { setSearchQuery(h.query || h.question); setResult(h.explanation || h.answer); }}
              >
                <div style={styles.historyMeta}>
                  <span style={styles.historyTitle}>{h.query || h.question}</span>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <button 
                      onClick={(e) => handleDeleteHistoryItem(e, h.id)} 
                      style={{ background: 'none', border: 'none', color: 'var(--error)', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '4px' }}
                      title="Delete History Item"
                    >
                      <Trash2 size={14} />
                    </button>
                    <ArrowRight size={14} color="var(--primary)" style={{ flexShrink: 0 }} />
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

const styles = {
  container: {
    padding: '24px',
    maxWidth: '900px',
    margin: '0 auto',
    display: 'flex',
    flexDirection: 'column',
    gap: '24px'
  },
  header: {
    textAlign: 'center',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '8px'
  },
  subtitle: {
    fontSize: '14px',
    color: 'var(--text-muted)',
    maxWidth: '600px',
    lineHeight: '1.6'
  },
  searchCard: {
    padding: '24px'
  },
  searchForm: {
    display: 'flex',
    gap: '12px'
  },
  inputWrapper: {
    position: 'relative',
    flex: 1,
    display: 'flex',
    alignItems: 'center'
  },
  searchIcon: {
    position: 'absolute',
    left: '16px'
  },
  searchField: {
    paddingLeft: '48px',
    height: '48px'
  },
  searchBtn: {
    height: '48px',
    padding: '0 24px',
    flexShrink: 0
  },
  suggestionsRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    marginTop: '16px',
    flexWrap: 'wrap'
  },
  suggestLabel: {
    fontSize: '12px',
    color: 'var(--text-muted)',
    fontWeight: '700'
  },
  suggestTag: {
    padding: '6px 12px',
    background: 'rgba(255,255,255,0.03)',
    border: '1px solid var(--border)',
    borderRadius: '8px',
    fontSize: '12px',
    color: 'var(--text-main)',
    cursor: 'pointer',
    transition: 'all 0.2s',
    ':hover': {
      borderColor: 'var(--primary)'
    }
  },
  resultCard: {
    padding: '28px',
    borderLeft: '4px solid var(--secondary)'
  },
  resultHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    marginBottom: '16px'
  },
  resultText: {
    fontSize: '14px',
    lineHeight: '1.7',
    color: 'var(--text-main)',
    whiteSpace: 'pre-wrap'
  },
  historyCard: {
    padding: '24px'
  },
  historyHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    marginBottom: '16px'
  },
  loader: {
    fontSize: '13px',
    color: 'var(--text-muted)',
    textAlign: 'center',
    padding: '12px 0'
  },
  emptyHistory: {
    fontSize: '13px',
    color: 'var(--text-muted)',
    textAlign: 'center',
    padding: '12px 0'
  },
  historyGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
    gap: '12px'
  },
  historyItem: {
    padding: '14px',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  historyMeta: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  historyTitle: {
    fontSize: '13px',
    fontWeight: '600'
  }
};
