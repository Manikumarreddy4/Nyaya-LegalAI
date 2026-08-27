import React, { useState, useEffect } from 'react';
import { db } from '../firebase';
import { collection, doc, deleteDoc, onSnapshot, getDocs } from 'firebase/firestore';
import { ArrowLeft, Search, Trash2, MoreVertical, MessageSquare, School, CheckSquare, Square, X, Clock } from 'lucide-react';

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

async function fetchLastBotMessage(uid, sessionId) {
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
}

export default function ChatHistory({ user, onNavigate, initialTab }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTabIndex, setSelectedTabIndex] = useState(0); // 0: AI Assistant, 1: Legal Learning
  
  // Fully Separate History States
  const [chatSessions, setChatSessions] = useState([]);
  const [learningHistory, setLearningHistory] = useState([]);
  
  // Fully Separate Loading States
  const [loadingChat, setLoadingChat] = useState(true);
  const [loadingLearn, setLoadingLearn] = useState(true);
  
  const [previews, setPreviews] = useState({});
  
  // Fully Separate Selection States
  const [chatSelectionMode, setChatSelectionMode] = useState(false);
  const [learnSelectionMode, setLearnSelectionMode] = useState(false);
  
  const [selectedChatItems, setSelectedChatItems] = useState(new Set());
  const [selectedLearnItems, setSelectedLearnItems] = useState(new Set());
  
  // Active dropdown menu ID
  const [activeMenuId, setActiveMenuId] = useState(null);

  const tabs = ["AI Problem Assistant", "Legal Learning"];

  // Initialize selected tab based on initialTab prop
  useEffect(() => {
    if (initialTab === 'LEGAL_LEARNING' || initialTab === 1 || initialTab === 'legal-learning') {
      setSelectedTabIndex(1);
    } else {
      setSelectedTabIndex(0);
    }
  }, [initialTab]);

  // Reset dropdowns when tab changes
  useEffect(() => {
    setActiveMenuId(null);
  }, [selectedTabIndex]);

  // Real-time listener for chatSessions (AI Problem Assistant / Legal Help)
  useEffect(() => {
    if (!user || !user.uid) return;
    setLoadingChat(true);

    const chatRef = collection(db, 'users', user.uid, 'chatSessions');
    const unsubscribe = onSnapshot(chatRef, (snap) => {
      const list = [];
      snap.forEach(docSnap => {
        const data = docSnap.data();
        if (data.chatbotType === 'AI_ASSISTANT') {
          const title = (data.title || '').trim();
          const isTitleEmptyOrDefault = title.length === 0 || 
            title.startsWith('New Legal Query') || 
            title.startsWith('New Chat') || 
            title.startsWith('Untitled');
          if (!isTitleEmptyOrDefault) {
            // Guard: ensure the document ID is placed AFTER doc data to prevent overwrite
            list.push({ ...data, id: docSnap.id });
          }
        }
      });
      setChatSessions(list);
      setLoadingChat(false);
    }, (err) => {
      console.error("Chat sync error:", err);
      setLoadingChat(false);
    });

    return () => unsubscribe();
  }, [user]);

  // Real-time listener for learningHistory (AI Legal Learning)
  useEffect(() => {
    if (!user || !user.uid) return;
    setLoadingLearn(true);

    const learnRef = collection(db, 'users', user.uid, 'learningHistory');
    const unsubscribe = onSnapshot(learnRef, (snap) => {
      const list = [];
      snap.forEach(docSnap => {
        const data = docSnap.data();
        const queryText = (data.query || data.question || '').trim();
        const isQuestionEmptyOrDefault = queryText.length === 0 || 
          queryText.startsWith('New Search') || 
          queryText.startsWith('Untitled');
        if (!isQuestionEmptyOrDefault) {
          // Guard: ensure the document ID is placed AFTER doc data to prevent overwrite
          list.push({ ...data, id: docSnap.id });
        }
      });
      setLearningHistory(list);
      setLoadingLearn(false);
    }, (err) => {
      console.error("Learn history sync error:", err);
      setLoadingLearn(false);
    });

    return () => unsubscribe();
  }, [user]);

  // Process data based on active tab
  const getMappedItems = () => {
    if (selectedTabIndex === 0) {
      return chatSessions.map(c => {
        let ms = 0;
        if (c.updatedAt) {
          ms = typeof c.updatedAt === 'number' ? c.updatedAt : (c.updatedAt.seconds * 1000 || 0);
        } else if (c.createdAt) {
          ms = typeof c.createdAt === 'number' ? c.createdAt : (c.createdAt.seconds * 1000 || 0);
        }
        return {
          id: c.id,
          type: 'AI_ASSISTANT',
          title: c.title || 'AI Assistant Conversation',
          timestamp: ms,
          rawItem: c
        };
      });
    } else {
      return learningHistory.map(l => {
        let ms = 0;
        if (l.timestamp) {
          ms = typeof l.timestamp === 'number' ? l.timestamp : (l.timestamp.seconds * 1000 || 0);
        }
        return {
          id: l.id,
          type: 'LEGAL_LEARNING',
          title: l.query || l.question || 'Legal Search',
          timestamp: ms,
          preview: l.explanation || l.answer || '',
          rawItem: l
        };
      });
    }
  };

  const rawItems = getMappedItems();
  const sortedItems = [...rawItems].sort((a, b) => b.timestamp - a.timestamp);
  const filteredItems = sortedItems.filter(item => {
    if (!searchQuery.trim()) return true;
    return item.title.toLowerCase().includes(searchQuery.toLowerCase());
  });

  // Load bot previews for AI assistant sessions as needed
  useEffect(() => {
    if (!user || !user.uid || filteredItems.length === 0) return;
    filteredItems.forEach(item => {
      if (item.type === 'AI_ASSISTANT' && !previews[item.id]) {
        fetchLastBotMessage(user.uid, item.id).then(msg => {
          setPreviews(prev => ({ ...prev, [item.id]: msg }));
        });
      }
    });
  }, [user, filteredItems, previews]);

  // Handle individual delete action
  const handleDeleteItem = async (item) => {
    if (!user || !user.uid) return;
    const isChat = item.type === 'AI_ASSISTANT';
    if (!window.confirm(`Are you sure you want to delete this ${isChat ? 'chat session' : 'search history item'}?`)) return;

    try {
      if (isChat) {
        // Delete messages subcollection
        const messagesRef = collection(db, 'users', user.uid, 'chatSessions', item.id, 'messages');
        const snap = await getDocs(messagesRef);
        for (const d of snap.docs) {
          await deleteDoc(d.ref);
        }
        // Delete session doc
        await deleteDoc(doc(db, 'users', user.uid, 'chatSessions', item.id));
        
        setSelectedChatItems(prev => {
          const next = new Set(prev);
          next.delete(item.id);
          return next;
        });
      } else {
        // Delete learningHistory entry
        await deleteDoc(doc(db, 'users', user.uid, 'learningHistory', item.id));
        
        setSelectedLearnItems(prev => {
          const next = new Set(prev);
          next.delete(item.id);
          return next;
        });
      }
    } catch (err) {
      console.error("Delete failed:", err);
      alert("Error deleting item: " + err.message);
    }
  };

  // Toggle item selection
  const handleToggleSelectItem = (id) => {
    if (selectedTabIndex === 0) {
      setSelectedChatItems(prev => {
        const next = new Set(prev);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        return next;
      });
    } else {
      setSelectedLearnItems(prev => {
        const next = new Set(prev);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        return next;
      });
    }
  };

  // Handle bulk delete selected items
  const handleDeleteSelected = async () => {
    const isChat = selectedTabIndex === 0;
    const currentSet = isChat ? selectedChatItems : selectedLearnItems;
    if (!user || !user.uid || currentSet.size === 0) return;
    if (!window.confirm(`Are you sure you want to delete the ${currentSet.size} selected items permanently?`)) return;

    try {
      const promises = [];
      for (const id of currentSet) {
        if (isChat) {
          // Delete messages first
          const messagesRef = collection(db, 'users', user.uid, 'chatSessions', id, 'messages');
          const snap = await getDocs(messagesRef);
          snap.forEach(d => {
            promises.push(deleteDoc(d.ref));
          });
          // Delete session doc
          promises.push(deleteDoc(doc(db, 'users', user.uid, 'chatSessions', id)));
        } else {
          // Delete learningHistory entry
          promises.push(deleteDoc(doc(db, 'users', user.uid, 'learningHistory', id)));
        }
      }
      await Promise.all(promises);
      if (isChat) {
        setSelectedChatItems(new Set());
        setChatSelectionMode(false);
      } else {
        setSelectedLearnItems(new Set());
        setLearnSelectionMode(false);
      }
    } catch (err) {
      console.error("Delete selected failed:", err);
      alert("Error deleting selected items: " + err.message);
    }
  };

  // Select all or Deselect all
  const handleToggleSelectAll = () => {
    const visibleIds = filteredItems.map(i => i.id);
    const currentSet = selectedTabIndex === 0 ? selectedChatItems : selectedLearnItems;
    const allSelected = visibleIds.length > 0 && visibleIds.every(id => currentSet.has(id));
    
    if (selectedTabIndex === 0) {
      setSelectedChatItems(prev => {
        const next = new Set(prev);
        if (allSelected) {
          visibleIds.forEach(id => next.delete(id));
        } else {
          visibleIds.forEach(id => next.add(id));
        }
        return next;
      });
    } else {
      setSelectedLearnItems(prev => {
        const next = new Set(prev);
        if (allSelected) {
          visibleIds.forEach(id => next.delete(id));
        } else {
          visibleIds.forEach(id => next.add(id));
        }
        return next;
      });
    }
  };

  const handleItemClick = (item) => {
    const isSelectionMode = selectedTabIndex === 0 ? chatSelectionMode : learnSelectionMode;
    if (isSelectionMode) {
      handleToggleSelectItem(item.id);
    } else {
      if (item.type === 'AI_ASSISTANT') {
        onNavigate('legal-assistant', { sessionId: item.id });
      } else {
        onNavigate('legal-learning');
      }
    }
  };

  const currentSelectionMode = selectedTabIndex === 0 ? chatSelectionMode : learnSelectionMode;
  const currentSet = selectedTabIndex === 0 ? selectedChatItems : selectedLearnItems;
  const allSelected = filteredItems.length > 0 && filteredItems.every(i => currentSet.has(i.id));
  const activeLoading = selectedTabIndex === 0 ? loadingChat : loadingLearn;

  return (
    <div className="fade-in-up" style={styles.container}>
      {/* Custom Header Bar */}
      <div className="glass-panel" style={styles.headerBar}>
        {currentSelectionMode ? (
          <div style={styles.selectionHeader}>
            <button 
              className="btn btn-secondary" 
              style={styles.circleBtn} 
              onClick={() => {
                if (selectedTabIndex === 0) {
                  setSelectedChatItems(new Set());
                  setChatSelectionMode(false);
                } else {
                  setSelectedLearnItems(new Set());
                  setLearnSelectionMode(false);
                }
              }}
            >
              <X size={18} />
            </button>
            <span style={styles.selectionTitle}>{currentSet.size} selected</span>
            <div style={{ marginLeft: 'auto', display: 'flex', gap: '12px', alignItems: 'center' }}>
              <button 
                className="btn btn-secondary"
                style={{ fontSize: '13px', fontWeight: 'bold' }}
                onClick={handleToggleSelectAll}
              >
                {allSelected ? "Deselect All" : "Select All"}
              </button>
              <button 
                className="btn btn-primary"
                style={{ background: 'var(--error)', borderColor: 'var(--error)', padding: '8px 16px', display: 'flex', alignItems: 'center', gap: '8px' }}
                onClick={handleDeleteSelected}
                disabled={currentSet.size === 0}
              >
                <Trash2 size={16} /> Delete Selected
              </button>
            </div>
          </div>
        ) : (
          <div style={styles.normalHeader}>
            <button 
              className="btn btn-secondary" 
              style={styles.circleBtn}
              onClick={() => onNavigate('dashboard')}
            >
              <ArrowLeft size={18} />
            </button>
            <h2 style={styles.pageTitle}>Chat History</h2>
            <button 
              className="btn btn-secondary" 
              style={{ ...styles.circleBtn, marginLeft: 'auto', color: 'var(--error)' }}
              onClick={() => {
                if (selectedTabIndex === 0) setChatSelectionMode(true);
                else setLearnSelectionMode(true);
              }}
              title="Delete Items"
            >
              <Trash2 size={18} />
            </button>
          </div>
        )}
      </div>

      {/* Search Bar */}
      <div style={styles.searchWrapper}>
        <Search size={18} color="var(--primary)" style={styles.searchIcon} />
        <input 
          type="text" 
          placeholder="Search history..." 
          className="input-field" 
          style={styles.searchBar}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>

      {/* Tab Selector */}
      <div style={styles.tabContainer}>
        {tabs.map((tab, idx) => (
          <button 
            key={idx}
            className={`tab-btn ${selectedTabIndex === idx ? 'active' : ''}`}
            onClick={() => {
              setSelectedTabIndex(idx);
              setActiveMenuId(null);
            }}
            style={{
              ...styles.tabBtn,
              flex: 1,
              borderBottom: selectedTabIndex === idx ? '2px solid var(--primary)' : '1px solid var(--border)',
              color: selectedTabIndex === idx ? 'var(--primary)' : 'var(--text-muted)',
              fontWeight: selectedTabIndex === idx ? '800' : '600'
            }}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* History Items list */}
      {activeLoading ? (
        <div style={styles.loader}>Loading history...</div>
      ) : filteredItems.length === 0 ? (
        <div className="glass-panel" style={styles.emptyCard}>
          <Clock size={36} color="var(--border)" style={{ marginBottom: '12px' }} />
          <h3>No History Items Found</h3>
          <p>{searchQuery ? "No history matching your search query." : "You have no recorded history in this category."}</p>
        </div>
      ) : (
        <div style={styles.list}>
          {filteredItems.map((item) => {
            const isSelected = currentSet.has(item.id);
            return (
              <div 
                key={item.id} 
                className={`glass-panel history-item-card ${isSelected ? 'selected' : ''}`} 
                style={{
                  ...styles.itemCard,
                  borderColor: isSelected ? 'var(--primary)' : 'var(--border)',
                  background: isSelected ? 'rgba(99, 102, 241, 0.05)' : 'rgba(255, 255, 255, 0.01)'
                }}
                onClick={() => handleItemClick(item)}
              >
                <div style={styles.cardHeader}>
                  {currentSelectionMode && (
                    <div style={{ marginRight: '8px' }} onClick={(e) => { e.stopPropagation(); handleToggleSelectItem(item.id); }}>
                      {isSelected ? (
                        <CheckSquare size={20} color="var(--primary)" />
                      ) : (
                        <Square size={20} color="var(--text-muted)" />
                      )}
                    </div>
                  )}
                  <div style={styles.iconBox}>
                    {item.type === 'AI_ASSISTANT' ? (
                      <MessageSquare size={16} color="var(--primary)" />
                    ) : (
                      <School size={16} color="var(--secondary)" />
                    )}
                  </div>
                  <div style={styles.metaInfo}>
                    <span style={styles.moduleName}>
                      {item.type === 'AI_ASSISTANT' ? 'AI PROBLEM ASSISTANT' : 'LEGAL LEARNING'}
                    </span>
                    <span style={styles.timestampLabel}>{formatTimestamp(item.timestamp)}</span>
                  </div>
                  
                  {/* Item Actions */}
                  {!currentSelectionMode && (
                    <div style={{ marginLeft: 'auto', position: 'relative' }}>
                      <button 
                        onClick={(e) => {
                          e.stopPropagation();
                          setActiveMenuId(activeMenuId === item.id ? null : item.id);
                        }}
                        style={styles.menuBtn}
                      >
                        <MoreVertical size={16} />
                      </button>
                      
                      {activeMenuId === item.id && (
                        <div className="glass-panel dropdown-menu" style={styles.dropdownMenu}>
                          <button 
                            style={styles.dropdownItem}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDeleteItem(item);
                              setActiveMenuId(null);
                            }}
                          >
                            <Trash2 size={14} color="var(--error)" style={{ marginRight: '8px' }} />
                            <span style={{ color: 'var(--error)' }}>Delete</span>
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>

                <h4 style={styles.itemTitle}>{item.title}</h4>
                <p style={styles.itemPreview}>
                  {item.type === 'AI_ASSISTANT' 
                    ? (previews[item.id] || 'Loading preview...') 
                    : item.preview
                  }
                </p>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

const styles = {
  container: {
    padding: '24px',
    maxWidth: '850px',
    margin: '0 auto',
    display: 'flex',
    flexDirection: 'column',
    gap: '20px'
  },
  headerBar: {
    padding: '16px 20px',
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'center'
  },
  normalHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
    width: '100%'
  },
  selectionHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
    width: '100%'
  },
  circleBtn: {
    width: '40px',
    height: '40px',
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 0,
    flexShrink: 0
  },
  pageTitle: {
    fontSize: '20px',
    fontWeight: '800'
  },
  selectionTitle: {
    fontSize: '16px',
    fontWeight: '700'
  },
  searchWrapper: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
    width: '100%'
  },
  searchIcon: {
    position: 'absolute',
    left: '16px'
  },
  searchBar: {
    width: '100%',
    paddingLeft: '48px',
    height: '48px',
    borderRadius: '16px'
  },
  tabContainer: {
    display: 'flex',
    width: '100%'
  },
  tabBtn: {
    padding: '12px 16px',
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    fontSize: '14px',
    transition: 'all 0.2s',
    textAlign: 'center'
  },
  loader: {
    textAlign: 'center',
    padding: '40px',
    color: 'var(--text-muted)'
  },
  emptyCard: {
    padding: '48px 20px',
    textAlign: 'center',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    color: 'var(--text-muted)'
  },
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  itemCard: {
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    cursor: 'pointer',
    position: 'relative',
    transition: 'all 0.2s'
  },
  cardHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px'
  },
  iconBox: {
    width: '36px',
    height: '36px',
    borderRadius: '10px',
    background: 'rgba(255, 255, 255, 0.03)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0
  },
  metaInfo: {
    display: 'flex',
    flexDirection: 'column',
    gap: '2px'
  },
  moduleName: {
    fontSize: '10px',
    fontWeight: '800',
    color: 'var(--secondary)',
    letterSpacing: '0.5px'
  },
  timestampLabel: {
    fontSize: '11px',
    color: 'var(--text-muted)'
  },
  menuBtn: {
    background: 'none',
    border: 'none',
    color: 'var(--text-muted)',
    cursor: 'pointer',
    padding: '4px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  },
  dropdownMenu: {
    position: 'absolute',
    right: 0,
    top: '28px',
    width: '120px',
    padding: '4px',
    zIndex: 100,
    borderRadius: '10px',
    boxShadow: '0 4px 12px rgba(0,0,0,0.3)'
  },
  dropdownItem: {
    display: 'flex',
    alignItems: 'center',
    padding: '10px 12px',
    width: '100%',
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    fontSize: '13px',
    textAlign: 'left'
  },
  itemTitle: {
    fontSize: '16px',
    fontWeight: '700',
    color: 'var(--text-main)'
  },
  itemPreview: {
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
