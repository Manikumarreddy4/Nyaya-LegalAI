import React, { useEffect, useState } from 'react';
import { db } from '../firebase';
import { collection, query, getDocs, limit, orderBy, where, onSnapshot } from 'firebase/firestore';
import { MessageSquare, School, BookOpen, Search, Calendar, ChevronRight, Clock, ShieldAlert, Bot, Sparkles } from 'lucide-react';

export default function ClientDashboard({ user, onNavigate }) {
  const [recentActivities, setRecentActivities] = useState([]);
  const [loadingActivities, setLoadingActivities] = useState(true);
  const [previews, setPreviews] = useState({});
  
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
      console.log(`[ACTIVITY_SYNC] ACTIVITY_SYNC: Current user ID = ${user.uid}`);
      console.log(`[ACTIVITY_SYNC] ACTIVITY_SYNC: Firestore snapshot received`);

      // Calculate AI Help: count of unique AI Assistant conversations
      const aiAssistantSessionIds = new Set();
      
      problemSessions.forEach(s => {
        const titleText = (s.title || '').trim();
        const isTitleEmptyOrDefault = titleText.length === 0 || 
          titleText.startsWith('New Legal Query') || 
          titleText.startsWith('New Chat') || 
          titleText.startsWith('Untitled');
        if (!isTitleEmptyOrDefault) {
          aiAssistantSessionIds.add(String(s.id || s.sessionId));
        }
      });
      
      chatSessions.forEach(s => {
        if (s.chatbotType === 'AI_ASSISTANT') {
          const titleText = (s.title || '').trim();
          const isTitleEmptyOrDefault = titleText.length === 0 || 
            titleText.startsWith('New Legal Query') || 
            titleText.startsWith('New Chat') || 
            titleText.startsWith('Untitled');
          if (!isTitleEmptyOrDefault) {
            aiAssistantSessionIds.add(String(s.id || s.sessionId));
          }
        }
      });
      
      const aiHelpCount = aiAssistantSessionIds.size;

      // Calculate Legal Learning: count of learning searches
      const learningActivityIds = new Set();
      learningDocs.forEach(d => {
        const questionText = (d.query || d.question || '').trim();
        const isQuestionEmptyOrDefault = questionText.length === 0 || 
          questionText.startsWith('New Search') || 
          questionText.startsWith('Untitled');
        if (!isQuestionEmptyOrDefault) {
          learningActivityIds.add(String(d.id));
        }
      });
      const learningCount = learningActivityIds.size;

      // Total Chats: AI Help + Legal Learning
      const totalChatsCount = aiHelpCount + learningCount;

      console.log(`[ACTIVITY_SYNC] ACTIVITY_SYNC: AI Help count = ${aiHelpCount}`);
      console.log(`[ACTIVITY_SYNC] ACTIVITY_SYNC: Legal Learning count = ${learningCount}`);
      console.log(`[ACTIVITY_SYNC] ACTIVITY_SYNC: Total Chats = ${totalChatsCount}`);

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
        const data = docSnapshot.data();
        problemSessions.push({ ...data, id: docSnapshot.id });
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
        const data = docSnapshot.data();
        chatSessions.push({ ...data, id: docSnapshot.id });
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
        const data = docSnapshot.data();
        learningDocs.push({ ...data, id: docSnapshot.id });
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

  // Helper to fetch last bot message preview
  const fetchLastBotMessage = async (uid, sessionId) => {
    try {
      const messagesRef = collection(db, 'users', uid, 'chatSessions', sessionId, 'messages');
      const snap = await getDocs(messagesRef);
      let botMsg = null;
      let latestTime = 0;
      snap.forEach(d => {
        const data = d.data();
        const time = data.timestamp || 0;
        if (data.sender === 'Bot' && time >= latestTime) {
          botMsg = data.message || data.content;
          latestTime = time;
        }
      });
      return botMsg || '';
    } catch (err) {
      console.error("Error fetching last bot message:", err);
      return '';
    }
  };

  // Listen to both chatSessions and learningHistory collections
  useEffect(() => {
    if (!user || !user.uid) return;
    setLoadingActivities(true);

    let unsubChat = () => {};
    let unsubLearn = () => {};

    let chatList = [];
    let learnList = [];

    const combineAndSort = () => {
      // Process chat list (AI Assistant)
      const chats = chatList
        .filter(c => {
          if (c.chatbotType !== 'AI_ASSISTANT') return false;
          const title = (c.title || '').trim();
          return title.length > 0 && 
            !title.startsWith('New Legal Query') && 
            !title.startsWith('New Chat') && 
            !title.startsWith('Untitled');
        })
        .map(c => {
          let ms = 0;
          if (c.updatedAt) {
            ms = typeof c.updatedAt === 'number' ? c.updatedAt : (c.updatedAt.seconds * 1000 || 0);
          } else if (c.createdAt) {
            ms = typeof c.createdAt === 'number' ? c.createdAt : (c.createdAt.seconds * 1000 || 0);
          }
          return {
            id: c.id,
            type: 'AI_ASSISTANT',
            chatbotType: 'AI_ASSISTANT',
            title: c.title || 'AI Assistant Conversation',
            timestamp: ms,
            rawItem: c
          };
        });

      // Process learning list (Legal Learning)
      const learns = learnList
        .filter(l => {
          const queryText = (l.query || l.question || '').trim();
          return queryText.length > 0 && 
            !queryText.startsWith('New Search') && 
            !queryText.startsWith('Untitled');
        })
        .map(l => {
          let ms = 0;
          if (l.timestamp) {
            ms = typeof l.timestamp === 'number' ? l.timestamp : (l.timestamp.seconds * 1000 || 0);
          }
          return {
            id: l.id,
            type: 'LEGAL_LEARNING',
            chatbotType: 'LEGAL_LEARNING',
            title: l.query || l.question || 'Legal Search',
            timestamp: ms,
            preview: l.explanation || l.answer || '',
            rawItem: l
          };
        });

      // Combine and sort by timestamp desc
      const combined = [...chats, ...learns].sort((a, b) => b.timestamp - a.timestamp);
      setRecentActivities(combined);
      setLoadingActivities(false);
    };

    const chatRef = collection(db, 'users', user.uid, 'chatSessions');
    unsubChat = onSnapshot(chatRef, (snap) => {
      chatList = [];
      snap.forEach(docSnap => {
        chatList.push({ id: docSnap.id, ...docSnap.data() });
      });
      combineAndSort();
    }, (err) => {
      console.error("Error listening to chatSessions:", err);
      setLoadingActivities(false);
    });

    const learnRef = collection(db, 'users', user.uid, 'learningHistory');
    unsubLearn = onSnapshot(learnRef, (snap) => {
      learnList = [];
      snap.forEach(docSnap => {
        learnList.push({ id: docSnap.id, ...docSnap.data() });
      });
      combineAndSort();
    }, (err) => {
      console.error("Error listening to learningHistory:", err);
      setLoadingActivities(false);
    });

    return () => {
      unsubChat();
      unsubLearn();
    };
  }, [user]);

  // Load bot previews for AI assistant sessions as needed
  useEffect(() => {
    if (!user || !user.uid || recentActivities.length === 0) return;
    recentActivities.forEach(activity => {
      if (activity.type === 'AI_ASSISTANT' && !previews[activity.id]) {
        fetchLastBotMessage(user.uid, activity.id).then(msg => {
          setPreviews(prev => ({ ...prev, [activity.id]: msg }));
        });
      }
    });
  }, [user, recentActivities, previews]);

  function formatTimestamp(ms) {
    if (!ms) return '';
    const date = new Date(ms);
    const day = String(date.getDate()).padStart(2, '0');
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const month = months[date.getMonth()];
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${day} ${month} ${year}, ${hours}:${minutes}`;
  }

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
            className="glass-panel" 
            style={styles.statCard}
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
      <div style={styles.activitySection}>
        <div style={styles.sectionHeaderRow}>
          <h3 style={styles.sectionHeader}>Recent Activity</h3>
          <button 
            className="btn btn-secondary" 
            style={styles.viewAllBtn} 
            onClick={() => onNavigate('chat-history')}
          >
            View All
          </button>
        </div>

        {loadingActivities ? (
          <div style={styles.loader}>Loading recent activity...</div>
        ) : recentActivities.length === 0 ? (
          <div className="glass-panel" style={styles.emptyActivityCard}>
            <Clock size={32} color="var(--border)" style={{marginBottom: '12px'}} />
            <p>No recent activity found. Start learning or ask our AI assistant!</p>
          </div>
        ) : (
          <div style={styles.activityGrid}>
            {recentActivities.slice(0, 3).map((activity) => (
              <div 
                key={activity.id} 
                className="glass-panel activity-item-card" 
                style={styles.activityItemCard}
                onClick={() => {
                  if (activity.type === 'AI_ASSISTANT') {
                    onNavigate('legal-assistant', { sessionId: activity.id });
                  } else {
                    onNavigate('legal-learning');
                  }
                }}
              >
                <div style={styles.activityCardHeader}>
                  <div style={styles.activityIconContainer}>
                    {activity.type === 'AI_ASSISTANT' ? (
                      <MessageSquare size={16} color="var(--primary)" />
                    ) : (
                      <School size={16} color="var(--secondary)" />
                    )}
                  </div>
                  <div style={styles.activityMeta}>
                    <span style={styles.activityModuleName}>
                      {activity.type === 'AI_ASSISTANT' ? 'AI PROBLEM ASSISTANT' : 'LEGAL LEARNING'}
                    </span>
                    <span style={styles.activityTime}>{formatTimestamp(activity.timestamp)}</span>
                  </div>
                </div>
                <h4 style={styles.activityTitle}>{activity.title}</h4>
                <p style={styles.activityPreview}>
                  {activity.type === 'AI_ASSISTANT' 
                    ? (previews[activity.id] || 'Loading preview...') 
                    : activity.preview
                  }
                </p>
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
    cursor: 'default',
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
  },
  activitySection: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
    marginTop: '12px'
  },
  sectionHeaderRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  viewAllBtn: {
    padding: '6px 14px',
    fontSize: '13px',
    fontWeight: '700'
  },
  emptyActivityCard: {
    padding: '36px 20px',
    textAlign: 'center',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    color: 'var(--text-muted)'
  },
  activityGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
    gap: '20px'
  },
  activityItemCard: {
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  activityCardHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px'
  },
  activityIconContainer: {
    width: '32px',
    height: '32px',
    borderRadius: '8px',
    background: 'rgba(255, 255, 255, 0.03)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0
  },
  activityMeta: {
    display: 'flex',
    flexDirection: 'column',
    gap: '2px'
  },
  activityModuleName: {
    fontSize: '10px',
    fontWeight: '800',
    color: 'var(--secondary)',
    letterSpacing: '0.5px'
  },
  activityTime: {
    fontSize: '11px',
    color: 'var(--text-muted)'
  },
  activityTitle: {
    fontSize: '15px',
    fontWeight: '700',
    color: 'var(--text-main)',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis'
  },
  activityPreview: {
    fontSize: '13px',
    color: 'var(--text-muted)',
    lineHeight: '1.5',
    maxHeight: '40px',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    display: '-webkit-box',
    WebkitLineClamp: 2,
    WebkitBoxOrient: 'vertical'
  }
};
