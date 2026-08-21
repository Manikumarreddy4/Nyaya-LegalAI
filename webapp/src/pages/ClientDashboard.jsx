import React, { useEffect, useState } from 'react';
import { db } from '../firebase';
import { collection, query, getDocs, limit, orderBy, where, onSnapshot } from 'firebase/firestore';
import { MessageSquare, School, BookOpen, Search, Calendar, ChevronRight, Clock, ShieldAlert, Bot, Sparkles } from 'lucide-react';

export default function ClientDashboard({ user, onNavigate }) {
  const [recentChats, setRecentChats] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [stats, setStats] = useState({ totalChats: 0, aiHelp: 0, learning: 0 });
  const [statsLoading, setStatsLoading] = useState(true);

  // Load real-time stats for Activity Overview
  useEffect(() => {
    if (!user || !user.uid) return;

    let unsubProblemHistory = () => {};
    let unsubChatSessions = () => {};
    let unsubLearningHistory = () => {};

    let problemSessions = [];
    let chatSessions = [];
    let learningDocs = [];

    const updateStats = () => {
      // Calculate AI Help: count of unique AI Assistant conversations
      const aiAssistantSessionIds = new Set();
      
      problemSessions.forEach(s => {
        aiAssistantSessionIds.add(s.id || s.sessionId);
      });
      
      chatSessions.forEach(s => {
        if (s.chatbotType === 'AI_ASSISTANT') {
          aiAssistantSessionIds.add(s.id || s.sessionId);
        }
      });
      
      const aiHelpCount = aiAssistantSessionIds.size;

      // Calculate Legal Learning: count of learning searches + chatSessions of type LEGAL_LEARNING
      const learningActivityIds = new Set();
      learningDocs.forEach(d => {
        learningActivityIds.add(d.id);
      });
      chatSessions.forEach(s => {
        if (s.chatbotType === 'LEGAL_LEARNING') {
          learningActivityIds.add(s.id || s.sessionId);
        }
      });
      const learningCount = learningActivityIds.size;

      // Total Chats: AI Help + Legal Learning
      const totalChatsCount = aiHelpCount + learningCount;

      setStats({
        totalChats: totalChatsCount,
        aiHelp: aiHelpCount,
        learning: learningCount
      });
      setStatsLoading(false);
    };

    // 1. Listen to problemHistory (AI Assistant sessions)
    const problemHistoryRef = collection(db, 'users', user.uid, 'problemHistory');
    unsubProblemHistory = onSnapshot(problemHistoryRef, (snapshot) => {
      problemSessions = [];
      snapshot.forEach(docSnapshot => {
        problemSessions.push({ id: docSnapshot.id, ...docSnapshot.data() });
      });
      updateStats();
    }, (error) => {
      console.error('Error listening to problemHistory', error);
    });

    // 2. Listen to chatSessions (Unified sessions from Android)
    const chatSessionsRef = collection(db, 'users', user.uid, 'chatSessions');
    unsubChatSessions = onSnapshot(chatSessionsRef, (snapshot) => {
      chatSessions = [];
      snapshot.forEach(docSnapshot => {
        chatSessions.push({ id: docSnapshot.id, ...docSnapshot.data() });
      });
      updateStats();
    }, (error) => {
      console.error('Error listening to chatSessions', error);
    });

    // 3. Listen to learningHistory (Learning searches)
    const learningHistoryRef = collection(db, 'users', user.uid, 'learningHistory');
    unsubLearningHistory = onSnapshot(learningHistoryRef, (snapshot) => {
      learningDocs = [];
      snapshot.forEach(docSnapshot => {
        learningDocs.push({ id: docSnapshot.id, ...docSnapshot.data() });
      });
      updateStats();
    }, (error) => {
      console.error('Error listening to learningHistory', error);
    });

    return () => {
      unsubProblemHistory();
      unsubChatSessions();
      unsubLearningHistory();
    };
  }, [user]);

  useEffect(() => {
    async function loadRecentChats() {
      if (!user || !user.uid) return;
      try {
        const chatsRef = collection(db, 'users', user.uid, 'problemHistory');
        const q = query(chatsRef, orderBy('updatedAt', 'desc'), limit(3));
        const querySnapshot = await getDocs(q);
        
        const sessions = [];
        querySnapshot.forEach(doc => {
          const data = doc.data();
          sessions.push({
            id: doc.id,
            ...data,
            // Format Timestamp
            dateStr: data.updatedAt ? new Date(data.updatedAt.seconds * 1000).toLocaleDateString() : 'Recent'
          });
        });
        setRecentChats(sessions);
      } catch (e) {
        console.error('Error loading recent chats', e);
      } finally {
        setLoading(false);
      }
    }
    loadRecentChats();
  }, [user]);

  const categories = [
    {
      id: 'assistant',
      name: 'AI Legal Assistant',
      desc: 'Discuss your legal issues and get step-by-step guidance.',
      icon: <MessageSquare size={24} color="white" />,
      color: 'linear-gradient(135deg, #6366f1, #4f46e5)',
      route: 'legal-assistant'
    },
    {
      id: 'learning',
      name: 'AI Legal Learning',
      desc: 'Search legal sections and get detailed summaries.',
      icon: <School size={24} color="white" />,
      color: 'linear-gradient(135deg, #10b981, #059669)',
      route: 'legal-learning'
    },
    {
      id: 'search',
      name: 'Law Search & Encyclopedia',
      desc: 'Browse Central/State Acts, Constitution, and IPC.',
      icon: <BookOpen size={24} color="white" />,
      color: 'linear-gradient(135deg, #a855f7, #9333ea)',
      route: 'law-search'
    },
    {
      id: 'lawyers',
      name: 'Find Lawyer',
      desc: 'Find vetted legal professionals and book slots.',
      icon: <Search size={24} color="white" />,
      color: 'linear-gradient(135deg, #f59e0b, #d97706)',
      route: 'find-lawyer'
    },
    {
      id: 'bookings',
      name: 'My Bookings',
      desc: 'Check your booked consultations and statuses.',
      icon: <Calendar size={24} color="white" />,
      color: 'linear-gradient(135deg, #ec4899, #db2777)',
      route: 'my-bookings'
    }
  ];

  return (
    <div className="fade-in-up" style={styles.container}>
      {/* Top Welcome Card */}
      <div className="glass-panel" style={styles.welcomeCard}>
        <h1 style={styles.title}>Hello, {user?.name || 'User'}</h1>
        <p style={styles.subtitle}>Welcome to Nyaya Legal AI platform. How can we help you today?</p>
      </div>

      {/* Activity Overview */}
      <h3 style={styles.sectionHeader}>Activity Overview</h3>
      {statsLoading ? (
        <div style={styles.statsLoader}>Loading activity statistics...</div>
      ) : (
        <div style={styles.statsGrid}>
          <div 
            className="glass-panel activity-card" 
            style={styles.statCard} 
            onClick={() => onNavigate('legal-assistant')}
          >
            <div style={{ ...styles.statIconContainer, background: 'rgba(168, 85, 247, 0.1)', color: 'var(--tertiary)' }}>
              <Bot size={20} />
            </div>
            <div style={styles.statInfo}>
              <span style={styles.statLabel}>🤖 AI Help</span>
              <span style={styles.statNumber}>{stats.aiHelp}</span>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>AI interactions</span>
            </div>
          </div>

          <div 
            className="glass-panel activity-card" 
            style={styles.statCard} 
            onClick={() => onNavigate('legal-learning')}
          >
            <div style={{ ...styles.statIconContainer, background: 'rgba(16, 185, 129, 0.1)', color: 'var(--secondary)' }}>
              <BookOpen size={20} />
            </div>
            <div style={styles.statInfo}>
              <span style={styles.statLabel}>📚 Legal Learning</span>
              <span style={styles.statNumber}>{stats.learning}</span>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Learning activities</span>
            </div>
          </div>

          <div 
            className="glass-panel activity-card" 
            style={styles.statCard} 
            onClick={() => onNavigate('legal-assistant')}
          >
            <div style={{ ...styles.statIconContainer, background: 'rgba(99, 102, 241, 0.1)', color: 'var(--primary)' }}>
              <MessageSquare size={20} />
            </div>
            <div style={styles.statInfo}>
              <span style={styles.statLabel}>💬 Total Chats</span>
              <span style={styles.statNumber}>{stats.totalChats}</span>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Conversations</span>
            </div>
          </div>
        </div>
      )}

      {/* Grid Menu */}
      <h3 style={styles.sectionHeader}>Categories & Services</h3>
      <div style={styles.grid}>
        {categories.map((cat) => (
          <div 
            key={cat.id} 
            className="glass-panel" 
            style={styles.card} 
            onClick={() => onNavigate(cat.route)}
          >
            <div style={{...styles.iconContainer, background: cat.color}}>
              {cat.icon}
            </div>
            <div style={styles.cardContent}>
              <h4 style={styles.cardTitle}>{cat.name}</h4>
              <p style={styles.cardDesc}>{cat.desc}</p>
            </div>
            <ChevronRight size={18} style={styles.chevron} />
          </div>
        ))}
      </div>

      {/* Recent Activity */}
      <div style={styles.activityRow}>
        <div className="glass-panel" style={styles.recentPanel}>
          <div style={styles.panelHeader}>
            <Clock size={20} color="var(--primary)" />
            <h3 style={styles.panelTitle}>Recent AI Consultations</h3>
          </div>
          
          {loading ? (
            <div style={styles.loader}>Loading recent activity...</div>
          ) : recentChats.length === 0 ? (
            <div style={styles.emptyText}>No recent chats found. Start a conversation with our AI Assistant!</div>
          ) : (
            <div style={styles.list}>
              {recentChats.map((chat) => (
                <div 
                  key={chat.id} 
                  style={styles.listItem} 
                  onClick={() => onNavigate('legal-assistant', { sessionId: chat.id })}
                >
                  <div style={styles.listItemLeft}>
                    <MessageSquare size={16} color="var(--text-muted)" />
                    <span style={styles.listItemTitle}>{chat.title || 'Untitled Session'}</span>
                  </div>
                  <span style={styles.listItemDate}>{chat.dateStr}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Disclaimer / Notice */}
        <div className="glass-panel" style={styles.disclaimerPanel}>
          <div style={styles.panelHeader}>
            <ShieldAlert size={20} color="var(--accent)" />
            <h3 style={styles.panelTitle}>Legal Disclaimer</h3>
          </div>
          <p style={styles.disclaimerText}>
            Nyaya Legal AI is an automated generative AI legal assistant. The information provided by the chatbot is for informational/learning purposes only and does NOT constitute formal legal advice. Please seek professional counsel from a vetted advocate for complex legal scenarios.
          </p>
        </div>
      </div>
    </div>
  );
}

const styles = {
  container: {
    padding: '24px',
    maxWidth: '1200px',
    margin: '0 auto',
    display: 'flex',
    flexDirection: 'column',
    gap: '24px'
  },
  welcomeCard: {
    padding: '40px 32px',
    background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(168, 85, 247, 0.15))',
    border: '1px solid rgba(255, 255, 255, 0.1)'
  },
  title: {
    fontSize: '32px',
    fontWeight: '800',
    marginBottom: '8px'
  },
  subtitle: {
    fontSize: '15px',
    color: 'var(--text-muted)'
  },
  sectionHeader: {
    fontSize: '20px',
    fontWeight: '700',
    marginTop: '8px'
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
    gap: '20px'
  },
  card: {
    display: 'flex',
    alignItems: 'center',
    padding: '20px',
    cursor: 'pointer',
    transition: 'all 0.2s',
    position: 'relative'
  },
  iconContainer: {
    width: '48px',
    height: '48px',
    borderRadius: '14px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    boxShadow: '0 4px 12px rgba(0,0,0,0.2)',
    flexShrink: 0
  },
  cardContent: {
    marginLeft: '16px',
    marginRight: '24px'
  },
  cardTitle: {
    fontSize: '16px',
    fontWeight: '700',
    marginBottom: '4px'
  },
  cardDesc: {
    fontSize: '13px',
    color: 'var(--text-muted)',
    lineHeight: '1.4'
  },
  chevron: {
    position: 'absolute',
    right: '16px',
    color: 'var(--text-muted)'
  },
  activityRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
    gap: '24px'
  },
  recentPanel: {
    padding: '24px'
  },
  disclaimerPanel: {
    padding: '24px',
    background: 'rgba(245, 158, 11, 0.03)',
    borderColor: 'rgba(245, 158, 11, 0.1)'
  },
  panelHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    marginBottom: '16px'
  },
  panelTitle: {
    fontSize: '16px',
    fontWeight: '700'
  },
  loader: {
    fontSize: '14px',
    color: 'var(--text-muted)',
    textAlign: 'center',
    padding: '20px 0'
  },
  emptyText: {
    fontSize: '14px',
    color: 'var(--text-muted)',
    lineHeight: '1.5',
    textAlign: 'center',
    padding: '20px 0'
  },
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px'
  },
  listItem: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '14px 16px',
    background: 'rgba(255, 255, 255, 0.02)',
    borderRadius: '12px',
    border: '1px solid var(--border)',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  listItemLeft: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px'
  },
  listItemTitle: {
    fontSize: '14px',
    fontWeight: '600',
    color: 'var(--text-main)',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    maxWidth: '240px'
  },
  listItemDate: {
    fontSize: '12px',
    color: 'var(--text-muted)'
  },
  disclaimerText: {
    fontSize: '14px',
    color: 'var(--text-muted)',
    lineHeight: '1.6'
  },
  statsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
    gap: '20px',
    marginBottom: '8px'
  },
  statCard: {
    display: 'flex',
    alignItems: 'center',
    padding: '16px 20px',
    cursor: 'pointer',
    gap: '16px'
  },
  statIconContainer: {
    width: '44px',
    height: '44px',
    borderRadius: '12px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0
  },
  statInfo: {
    display: 'flex',
    flexDirection: 'column',
    gap: '2px'
  },
  statLabel: {
    fontSize: '11px',
    fontWeight: '800',
    textTransform: 'uppercase',
    letterSpacing: '0.5px',
    color: 'var(--text-muted)'
  },
  statNumber: {
    fontSize: '24px',
    fontWeight: '900',
    color: 'var(--text-main)'
  },
  statsLoader: {
    padding: '16px',
    fontSize: '14px',
    color: 'var(--text-muted)'
  }
};
