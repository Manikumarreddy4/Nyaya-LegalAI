import React, { useState, useEffect } from 'react';
import { db } from '../firebase';
import { 
  collection, 
  doc, 
  getDoc, 
  updateDoc, 
  setDoc, 
  onSnapshot, 
  query, 
  orderBy, 
  where, 
  getDocs, 
  deleteDoc 
} from 'firebase/firestore';
import { 
  User, 
  Phone, 
  Mail, 
  Award, 
  Clock, 
  MapPin, 
  DollarSign, 
  BookOpen, 
  FileText, 
  CheckCircle,
  Settings,
  MessageSquare,
  School,
  Calendar,
  HelpCircle,
  ChevronRight,
  ArrowLeft,
  Trash2,
  CheckSquare,
  Square,
  Check,
  Info,
  Smartphone,
  UserCheck
} from 'lucide-react';

export default function Profile({ user, onProfileUpdate }) {
  // Navigation State
  const [activeSection, setActiveSection] = useState('menu'); // 'menu', 'edit-profile', 'chat-history', 'learning-history', 'consultation-history', 'tutorial'
  
  // Settings Modal State
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [darkMode, setDarkMode] = useState(false);
  const [themeColor, setThemeColor] = useState('Default');
  const [fontColor, setFontColor] = useState('Default');
  const [settingsLanguage, setSettingsLanguage] = useState('en');
  const [settingsSaving, setSettingsSaving] = useState(false);
  const [settingsMessage, setSettingsMessage] = useState('');

  // Profile Form State
  const [profile, setProfile] = useState(null);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [specialization, setSpecialization] = useState('');
  const [experience, setExperience] = useState('');
  const [location, setLocation] = useState('');
  const [fee, setFee] = useState('');
  const [bio, setBio] = useState('');
  const [barId, setBarId] = useState('');
  const [languages, setLanguages] = useState('');
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileMessage, setProfileMessage] = useState('');

  // AI Chat History State
  const [chatSessions, setChatSessions] = useState([]);
  const [selectedChatIds, setSelectedChatIds] = useState([]);
  const [chatLoading, setChatLoading] = useState(true);

  // Learning History State
  const [learningHistory, setLearningHistory] = useState([]);
  const [selectedLearningIds, setSelectedLearningIds] = useState([]);
  const [learningLoading, setLearningLoading] = useState(true);

  // Consultation History State
  const [consultations, setConsultations] = useState([]);
  const [consultationsLoading, setConsultationsLoading] = useState(true);
  const [consultationFilter, setConsultationFilter] = useState('ALL');

  // Real-time Profile Listener
  useEffect(() => {
    if (!user || !user.uid) return;
    setProfileLoading(true);
    const userDocRef = doc(db, 'users', user.uid);
    const unsubscribe = onSnapshot(userDocRef, (docSnapshot) => {
      if (docSnapshot.exists()) {
        const data = docSnapshot.data();
        setProfile(data);
        setName(data.name || '');
        setPhone(data.phone || '');
        setSpecialization(data.specialization || '');
        setExperience((data.experience || '').replace(' Years', ''));
        setLocation(data.location || data.city || '');
        setFee(String(data.consultationFee || '500'));
        setBio(data.bio || '');
        setBarId(data.barCouncilNumber || data.enrollmentNumber || '');
        setLanguages(data.languages || '');
      }
      setProfileLoading(false);
    }, (error) => {
      console.error('Error listening to user profile:', error);
      setProfileLoading(false);
    });
    return () => unsubscribe();
  }, [user]);

  // Real-time Settings Listener
  useEffect(() => {
    if (!user || !user.uid) return;
    const settingsDocRef = doc(db, 'users', user.uid, 'settings', 'config');
    const unsubscribe = onSnapshot(settingsDocRef, (docSnapshot) => {
      if (docSnapshot.exists()) {
        const data = docSnapshot.data();
        setDarkMode(data.darkMode ?? false);
        setThemeColor(data.themeColor ?? 'Default');
        setFontColor(data.fontColor ?? 'Default');
        setSettingsLanguage(data.language ?? 'en');
      }
    }, (error) => {
      console.error('Error listening to user settings:', error);
    });
    return () => unsubscribe();
  }, [user]);

  // Real-time Chat Sessions Listener
  useEffect(() => {
    if (!user || !user.uid || activeSection !== 'chat-history') return;
    setChatLoading(true);
    const sessionsRef = collection(db, 'users', user.uid, 'chatSessions');
    const q = query(sessionsRef, orderBy('updatedAt', 'desc'));
    const unsubscribe = onSnapshot(q, (querySnapshot) => {
      const docs = [];
      querySnapshot.forEach(d => {
        const data = d.data();
        if (data.chatbotType !== 'AI_ASSISTANT') return;
        
        // Skip sessions with empty or default values
        const title = data.title || '';
        const isTitleEmptyOrDefault = !title.trim() || 
          title.startsWith('New Legal Query') || 
          title.startsWith('New Chat') || 
          title.startsWith('Untitled');
        if (!isTitleEmptyOrDefault) {
          docs.push({
            id: d.id,
            ...data,
            sessionId: data.sessionId || d.id
          });
        }
      });
      setChatSessions(docs);
      setChatLoading(false);
    }, (error) => {
      console.error('Error listening to chat sessions:', error);
      setChatLoading(false);
    });
    return () => unsubscribe();
  }, [user, activeSection]);

  // Real-time Learning History Listener
  useEffect(() => {
    if (!user || !user.uid || activeSection !== 'learning-history') return;
    setLearningLoading(true);
    const historyRef = collection(db, 'users', user.uid, 'learningHistory');
    const q = query(historyRef, orderBy('timestamp', 'desc'));
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
      setLearningHistory(docs);
      setLearningLoading(false);
    }, (error) => {
      console.error('Error listening to learning history:', error);
      setLearningLoading(false);
    });
    return () => unsubscribe();
  }, [user, activeSection]);

  // Real-time Consultations Listener
  useEffect(() => {
    if (!user || !user.uid || activeSection !== 'consultation-history') return;
    setConsultationsLoading(true);
    const consultationsRef = collection(db, 'consultations');
    const q = query(consultationsRef, where('clientId', '==', user.uid));
    const unsubscribe = onSnapshot(q, (querySnapshot) => {
      const docs = [];
      querySnapshot.forEach(docSnapshot => {
        docs.push({ id: docSnapshot.id, ...docSnapshot.data() });
      });
      // Sort locally by date/time descending to avoid index requirement warnings
      docs.sort((a, b) => {
        const aTime = a.createdAt?.seconds || a.createdAt?.toMillis?.() / 1000 || 0;
        const bTime = b.createdAt?.seconds || b.createdAt?.toMillis?.() / 1000 || 0;
        return bTime - aTime;
      });
      setConsultations(docs);
      setConsultationsLoading(false);
    }, (error) => {
      console.error('Error listening to consultations:', error);
      setConsultationsLoading(false);
    });
    return () => unsubscribe();
  }, [user, activeSection]);

  // Handle Edit Profile Save
  const handleSaveProfile = async (e) => {
    e.preventDefault();
    if (!name || !phone) {
      alert('Name and Phone are required.');
      return;
    }
    setProfileSaving(true);
    setProfileMessage('');
    try {
      const updates = {
        name: name.trim(),
        phone: phone.trim()
      };

      if (user.role === 'LAWYER') {
        updates.specialization = specialization.trim();
        updates.experience = experience.trim() + " Years";
        updates.location = location.trim();
        updates.city = location.trim();
        updates.consultationFee = parseFloat(fee) || 500;
        updates.bio = bio.trim();
        updates.barCouncilNumber = barId.trim();
        updates.enrollmentNumber = barId.trim();
        updates.languages = languages.trim();
        
        await updateDoc(doc(db, 'users', user.uid), updates);
        await updateDoc(doc(db, 'lawyers', user.uid), updates);
      } else {
        await updateDoc(doc(db, 'users', user.uid), updates);
      }

      setProfileMessage('✓ Profile updated successfully.');
      onProfileUpdate({ ...user, name: name.trim(), phone: phone.trim() });
      setTimeout(() => setActiveSection('menu'), 1000);
    } catch (err) {
      console.error(err);
      alert('Error updating profile: ' + err.message);
    } finally {
      setProfileSaving(false);
    }
  };

  // Handle Save Customization Settings
  const handleSaveSettings = async () => {
    setSettingsSaving(true);
    setSettingsMessage('');
    try {
      const settingsDocRef = doc(db, 'users', user.uid, 'settings', 'config');
      await setDoc(settingsDocRef, {
        darkMode,
        themeColor,
        fontColor,
        language: settingsLanguage
      });
      setSettingsMessage('✓ Customization settings saved successfully.');
      setTimeout(() => {
        setSettingsOpen(false);
        setSettingsMessage('');
      }, 1000);
    } catch (e) {
      console.error(e);
      alert('Error saving customization settings: ' + e.message);
    } finally {
      setSettingsSaving(false);
    }
  };

  // --- Deletion Functions (Gmail-like selection behavior) ---

  // AI Chat History Bulk Delete
  const handleDeleteSelectedChats = async () => {
    if (selectedChatIds.length === 0) return;
    if (!window.confirm(`Are you sure you want to delete the ${selectedChatIds.length} selected conversation(s)?`)) return;
    
    try {
      for (const sessId of selectedChatIds) {
        // Delete messages first
        const messagesRef = collection(db, 'users', user.uid, 'chatSessions', String(sessId), 'messages');
        const msgSnapshot = await getDocs(messagesRef);
        for (const msgDoc of msgSnapshot.docs) {
          await deleteDoc(msgDoc.ref);
        }
        // Delete session doc
        const sessionRef = doc(db, 'users', user.uid, 'chatSessions', String(sessId));
        await deleteDoc(sessionRef);
      }
      setSelectedChatIds([]);
    } catch (err) {
      console.error("Bulk chat delete error:", err);
      alert("Error deleting chats: " + err.message);
    }
  };

  // Single Chat Delete
  const handleSingleChatDelete = async (e, sessId) => {
    e.stopPropagation();
    if (!window.confirm("Are you sure you want to delete this conversation?")) return;
    try {
      const messagesRef = collection(db, 'users', user.uid, 'chatSessions', String(sessId), 'messages');
      const msgSnapshot = await getDocs(messagesRef);
      for (const msgDoc of msgSnapshot.docs) {
        await deleteDoc(msgDoc.ref);
      }
      const sessionRef = doc(db, 'users', user.uid, 'chatSessions', String(sessId));
      await deleteDoc(sessionRef);
      setSelectedChatIds(prev => prev.filter(id => id !== sessId));
    } catch (err) {
      console.error("Delete conversation error:", err);
    }
  };

  // AI Learning History Bulk Delete
  const handleDeleteSelectedLearning = async () => {
    if (selectedLearningIds.length === 0) return;
    if (!window.confirm(`Are you sure you want to delete the ${selectedLearningIds.length} selected learning history item(s)?`)) return;
    
    try {
      for (const itemId of selectedLearningIds) {
        const docRef = doc(db, 'users', user.uid, 'learningHistory', String(itemId));
        await deleteDoc(docRef);
      }
      setSelectedLearningIds([]);
    } catch (err) {
      console.error("Bulk learning delete error:", err);
      alert("Error deleting learning history: " + err.message);
    }
  };

  // Single Learning Delete
  const handleSingleLearningDelete = async (e, itemId) => {
    e.stopPropagation();
    if (!window.confirm("Are you sure you want to delete this learning item?")) return;
    try {
      const docRef = doc(db, 'users', user.uid, 'learningHistory', String(itemId));
      await deleteDoc(docRef);
      setSelectedLearningIds(prev => prev.filter(id => id !== itemId));
    } catch (err) {
      console.error("Delete learning history error:", err);
    }
  };

  // --- Rendering Helpers ---

  if (profileLoading) {
    return <div style={styles.loader}>Loading Profile & Settings...</div>;
  }

  // Filtered Bookings for Consultation History
  const filteredBookings = consultations.filter(c => {
    if (consultationFilter === 'ALL') return true;
    return (c.status || 'PENDING').toUpperCase() === consultationFilter;
  });

  const getStatusStyle = (status) => {
    switch (status?.toUpperCase()) {
      case 'ACCEPTED':
        return { background: 'rgba(16, 185, 129, 0.15)', color: 'var(--secondary)' };
      case 'REJECTED':
      case 'EXPIRED':
        return { background: 'rgba(239, 68, 68, 0.15)', color: 'var(--error)' };
      case 'COMPLETED':
        return { background: 'rgba(99, 102, 241, 0.15)', color: 'var(--primary)' };
      default: // PENDING
        return { background: 'rgba(245, 158, 11, 0.15)', color: 'var(--accent)' };
    }
  };

  return (
    <div className="fade-in-up" style={styles.container}>
      
      {/* -------------------- VIEW 1: MAIN MENU -------------------- */}
      {activeSection === 'menu' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          
          {/* Section 1: Profile Information Card */}
          <div className="glass-panel" style={styles.profileCard}>
            <div style={styles.profileInfoLayout}>
              <div style={styles.avatarCircle}>
                <User size={36} color="white" />
              </div>
              <div style={{ flex: 1 }}>
                <h2 style={styles.profileName}>{profile?.name || user?.name || 'Nyaya User'}</h2>
                <p style={styles.profileEmail}>{profile?.email || user?.email}</p>
                <div style={styles.roleBadge}>
                  <UserCheck size={12} style={{ marginRight: '4px' }} />
                  <span>{user?.role === 'LAWYER' ? 'Registered Advocate' : 'Client User'}</span>
                </div>
              </div>
              <button 
                className="btn btn-secondary" 
                onClick={() => setActiveSection('edit-profile')}
                style={styles.editBtn}
              >
                Edit Profile
              </button>
            </div>
          </div>

          {/* Section 2: App Customization */}
          <div style={styles.sectionHeader}>App Customization</div>
          <div 
            className="glass-panel" 
            style={styles.menuItem} 
            onClick={() => setSettingsOpen(true)}
          >
            <div style={styles.menuIconBox}>
              <Settings size={20} color="var(--primary)" />
            </div>
            <div style={{ flex: 1 }}>
              <div style={styles.menuTitle}>Settings</div>
              <div style={styles.menuDescription}>Themes, fonts, colors, and dark mode preferences</div>
            </div>
            <ChevronRight size={18} color="var(--text-muted)" />
          </div>
          {/* Section 4: Support & Info */}
          <div style={styles.sectionHeader}>Support & Info</div>
          <div className="glass-panel" style={styles.menuItem} onClick={() => setActiveSection('tutorial')}>
            <div style={styles.menuIconBox}>
              <HelpCircle size={20} color="var(--tertiary)" />
            </div>
            <div style={{ flex: 1 }}>
              <div style={styles.menuTitle}>App Tutorial</div>
              <div style={styles.menuDescription}>Learn how to use Nyaya AI features step-by-step</div>
            </div>
            <ChevronRight size={18} color="var(--text-muted)" />
          </div>

        </div>
      )}

      {/* -------------------- VIEW 2: EDIT PROFILE -------------------- */}
      {activeSection === 'edit-profile' && (
        <div className="glass-panel" style={styles.card}>
          <div style={styles.viewHeader}>
            <button className="btn btn-secondary" style={styles.backBtn} onClick={() => setActiveSection('menu')}>
              <ArrowLeft size={16} /> Back
            </button>
            <h2 style={{ fontSize: '20px' }}>Edit Profile Information</h2>
          </div>

          <form onSubmit={handleSaveProfile} style={styles.form}>
            <div style={styles.formSectionTitle}>Personal Details</div>
            
            <div style={styles.formGroup}>
              <label style={styles.label}>Full Name</label>
              <div style={styles.inputWrapper}>
                <User size={16} style={styles.icon} />
                <input 
                  type="text" 
                  className="input-field" 
                  value={name} 
                  onChange={(e) => setName(e.target.value)} 
                  required 
                />
              </div>
            </div>

            <div style={styles.formGroup}>
              <label style={styles.label}>Email Address (Read-only)</label>
              <div style={styles.inputWrapper}>
                <Mail size={16} style={styles.icon} />
                <input 
                  type="email" 
                  className="input-field" 
                  value={profile?.email || user?.email || ''} 
                  disabled 
                  style={{ opacity: 0.5, cursor: 'not-allowed' }}
                />
              </div>
            </div>

            <div style={styles.formGroup}>
              <label style={styles.label}>Phone Number</label>
              <div style={styles.inputWrapper}>
                <Phone size={16} style={styles.icon} />
                <input 
                  type="tel" 
                  className="input-field" 
                  value={phone} 
                  onChange={(e) => setPhone(e.target.value)} 
                  required 
                />
              </div>
            </div>

            {user?.role === 'LAWYER' && (
              <>
                <div style={styles.formSectionTitle}>Professional Credentials</div>
                
                <div style={styles.formGroup}>
                  <label style={styles.label}>Bar Council Enrollment Number</label>
                  <div style={styles.inputWrapper}>
                    <FileText size={16} style={styles.icon} />
                    <input 
                      type="text" 
                      className="input-field" 
                      value={barId} 
                      onChange={(e) => setBarId(e.target.value)} 
                      required 
                    />
                  </div>
                </div>

                <div style={styles.formGroup}>
                  <label style={styles.label}>Specialization</label>
                  <div style={styles.inputWrapper}>
                    <Award size={16} style={styles.icon} />
                    <input 
                      type="text" 
                      className="input-field" 
                      value={specialization} 
                      onChange={(e) => setSpecialization(e.target.value)} 
                      required 
                    />
                  </div>
                </div>

                <div style={styles.formGroup}>
                  <label style={styles.label}>Languages Known</label>
                  <div style={styles.inputWrapper}>
                    <BookOpen size={16} style={styles.icon} />
                    <input 
                      type="text" 
                      className="input-field" 
                      value={languages} 
                      onChange={(e) => setLanguages(e.target.value)} 
                      placeholder="e.g. English, Hindi, Punjabi"
                      required 
                    />
                  </div>
                </div>

                <div style={styles.formRow}>
                  <div style={{ ...styles.formGroup, flex: 1 }}>
                    <label style={styles.label}>Experience (Years)</label>
                    <div style={styles.inputWrapper}>
                      <Clock size={16} style={styles.icon} />
                      <input 
                        type="number" 
                        className="input-field" 
                        value={experience} 
                        onChange={(e) => setExperience(e.target.value)} 
                        required 
                      />
                    </div>
                  </div>
                  
                  <div style={{ ...styles.formGroup, flex: 1 }}>
                    <label style={styles.label}>Consultation Fee (INR)</label>
                    <div style={styles.inputWrapper}>
                      <DollarSign size={16} style={styles.icon} />
                      <input 
                        type="number" 
                        className="input-field" 
                        value={fee} 
                        onChange={(e) => setFee(e.target.value)} 
                        required 
                      />
                    </div>
                  </div>
                </div>

                <div style={styles.formGroup}>
                  <label style={styles.label}>Location / City</label>
                  <div style={styles.inputWrapper}>
                    <MapPin size={16} style={styles.icon} />
                    <input 
                      type="text" 
                      className="input-field" 
                      value={location} 
                      onChange={(e) => setLocation(e.target.value)} 
                      required 
                    />
                  </div>
                </div>

                <div style={styles.formGroup}>
                  <label style={styles.label}>Professional Bio</label>
                  <div style={styles.inputWrapper}>
                    <BookOpen size={16} style={{ ...styles.icon, top: '16px' }} />
                    <textarea 
                      className="input-field" 
                      rows="3" 
                      value={bio} 
                      onChange={(e) => setBio(e.target.value)} 
                      style={{ resize: 'none', paddingLeft: '48px' }}
                    />
                  </div>
                </div>
              </>
            )}

            {profileMessage && (
              <div style={styles.successBox}>
                <CheckCircle size={18} color="var(--secondary)" />
                <span>{profileMessage}</span>
              </div>
            )}

            <button type="submit" className="btn btn-primary" style={styles.submitBtn} disabled={profileSaving}>
              {profileSaving ? 'Saving changes...' : 'Save Profile Changes'}
            </button>
          </form>
        </div>
      )}

      {/* -------------------- VIEW 3: AI CHAT HISTORY -------------------- */}
      {activeSection === 'chat-history' && (
        <div className="glass-panel" style={styles.card}>
          <div style={styles.viewHeader}>
            <button className="btn btn-secondary" style={styles.backBtn} onClick={() => setActiveSection('menu')}>
              <ArrowLeft size={16} /> Back
            </button>
            <h2 style={{ fontSize: '20px' }}>AI Chat History</h2>
          </div>

          {chatLoading ? (
            <div style={styles.subLoader}>Loading previous chats...</div>
          ) : chatSessions.length === 0 ? (
            <div style={styles.emptyView}>
              <MessageSquare size={36} color="var(--border)" style={{ marginBottom: '12px' }} />
              <p>No previous conversations found.</p>
            </div>
          ) : (
            <div>
              {/* Bulk operations bar */}
              <div style={styles.bulkActionBar}>
                <div 
                  onClick={() => {
                    if (selectedChatIds.length === chatSessions.length) {
                      setSelectedChatIds([]);
                    } else {
                      setSelectedChatIds(chatSessions.map(s => s.sessionId));
                    }
                  }}
                  style={styles.selectAllToggle}
                >
                  {selectedChatIds.length === chatSessions.length ? (
                    <CheckSquare size={18} color="var(--primary)" />
                  ) : (
                    <Square size={18} color="var(--text-muted)" />
                  )}
                  <span style={{ fontSize: '13px', fontWeight: '700' }}>Select All ({selectedChatIds.length}/{chatSessions.length})</span>
                </div>
                
                {selectedChatIds.length > 0 && (
                  <button className="btn btn-secondary" onClick={handleDeleteSelectedChats} style={styles.bulkDeleteBtn}>
                    <Trash2 size={16} color="var(--error)" /> Delete Selected
                  </button>
                )}
              </div>

              {/* Chats List */}
              <div style={styles.historyList}>
                {chatSessions.map(session => {
                  const isSelected = selectedChatIds.includes(session.sessionId);
                  return (
                    <div 
                      key={session.id} 
                      style={{
                        ...styles.historyItem,
                        borderLeft: isSelected ? '4px solid var(--primary)' : '4px solid transparent',
                        background: isSelected ? 'rgba(99, 102, 241, 0.04)' : 'rgba(255, 255, 255, 0.01)'
                      }}
                      onClick={() => {
                        if (isSelected) {
                          setSelectedChatIds(prev => prev.filter(id => id !== session.sessionId));
                        } else {
                          setSelectedChatIds(prev => [...prev, session.sessionId]);
                        }
                      }}
                    >
                      <div style={styles.checkboxWrapper} onClick={(e) => e.stopPropagation()}>
                        <input 
                          type="checkbox"
                          checked={isSelected}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedChatIds(prev => [...prev, session.sessionId]);
                            } else {
                              setSelectedChatIds(prev => prev.filter(id => id !== session.sessionId));
                            }
                          }}
                          style={styles.checkboxInput}
                        />
                      </div>

                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={styles.historyItemTitle}>{session.title}</div>
                        <div style={styles.historyItemMeta}>
                          {new Date(session.updatedAt || session.createdAt || Date.now()).toLocaleString()}
                        </div>
                      </div>

                      <button 
                        style={styles.deleteItemBtn} 
                        onClick={(e) => handleSingleChatDelete(e, session.sessionId)}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}

      {/* -------------------- VIEW 4: LEARNING HISTORY -------------------- */}
      {activeSection === 'learning-history' && (
        <div className="glass-panel" style={styles.card}>
          <div style={styles.viewHeader}>
            <button className="btn btn-secondary" style={styles.backBtn} onClick={() => setActiveSection('menu')}>
              <ArrowLeft size={16} /> Back
            </button>
            <h2 style={{ fontSize: '20px' }}>AI Legal Learning History</h2>
          </div>

          {learningLoading ? (
            <div style={styles.subLoader}>Loading learning history...</div>
          ) : learningHistory.length === 0 ? (
            <div style={styles.emptyView}>
              <School size={36} color="var(--border)" style={{ marginBottom: '12px' }} />
              <p>No previous searches found.</p>
            </div>
          ) : (
            <div>
              {/* Bulk operations bar */}
              <div style={styles.bulkActionBar}>
                <div 
                  onClick={() => {
                    if (selectedLearningIds.length === learningHistory.length) {
                      setSelectedLearningIds([]);
                    } else {
                      setSelectedLearningIds(learningHistory.map(h => h.id));
                    }
                  }}
                  style={styles.selectAllToggle}
                >
                  {selectedLearningIds.length === learningHistory.length ? (
                    <CheckSquare size={18} color="var(--primary)" />
                  ) : (
                    <Square size={18} color="var(--text-muted)" />
                  )}
                  <span style={{ fontSize: '13px', fontWeight: '700' }}>Select All ({selectedLearningIds.length}/{learningHistory.length})</span>
                </div>
                
                {selectedLearningIds.length > 0 && (
                  <button className="btn btn-secondary" onClick={handleDeleteSelectedLearning} style={styles.bulkDeleteBtn}>
                    <Trash2 size={16} color="var(--error)" /> Delete Selected
                  </button>
                )}
              </div>

              {/* History List */}
              <div style={styles.historyList}>
                {learningHistory.map(item => {
                  const isSelected = selectedLearningIds.includes(item.id);
                  return (
                    <div 
                      key={item.id} 
                      style={{
                        ...styles.historyItem,
                        borderLeft: isSelected ? '4px solid var(--primary)' : '4px solid transparent',
                        background: isSelected ? 'rgba(99, 102, 241, 0.04)' : 'rgba(255, 255, 255, 0.01)'
                      }}
                      onClick={() => {
                        if (isSelected) {
                          setSelectedLearningIds(prev => prev.filter(id => id !== item.id));
                        } else {
                          setSelectedLearningIds(prev => [...prev, item.id]);
                        }
                      }}
                    >
                      <div style={styles.checkboxWrapper} onClick={(e) => e.stopPropagation()}>
                        <input 
                          type="checkbox"
                          checked={isSelected}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedLearningIds(prev => [...prev, item.id]);
                            } else {
                              setSelectedLearningIds(prev => prev.filter(id => id !== item.id));
                            }
                          }}
                          style={styles.checkboxInput}
                        />
                      </div>

                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={styles.historyItemTitle}>Query: "{item.query}"</div>
                        <p style={styles.historyItemSnippet}>
                          {item.explanation ? item.explanation.substring(0, 120) + '...' : ''}
                        </p>
                        <div style={styles.historyItemMeta}>
                          {new Date(item.timestamp || Date.now()).toLocaleString()}
                        </div>
                      </div>

                      <button 
                        style={styles.deleteItemBtn} 
                        onClick={(e) => handleSingleLearningDelete(e, item.id)}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}

      {/* -------------------- VIEW 5: CONSULTATION HISTORY -------------------- */}
      {activeSection === 'consultation-history' && (
        <div className="glass-panel" style={styles.card}>
          <div style={styles.viewHeader}>
            <button className="btn btn-secondary" style={styles.backBtn} onClick={() => setActiveSection('menu')}>
              <ArrowLeft size={16} /> Back
            </button>
            <h2 style={{ fontSize: '20px' }}>Consultation Booking History</h2>
          </div>

          {/* Mini-tabs filter */}
          <div style={styles.miniTabsContainer}>
            {['ALL', 'PENDING', 'ACCEPTED', 'COMPLETED', 'REJECTED', 'EXPIRED'].map((tab) => (
              <button
                key={tab}
                onClick={() => setConsultationFilter(tab)}
                style={{
                  ...styles.miniTab,
                  background: consultationFilter === tab ? 'var(--primary)' : 'rgba(255, 255, 255, 0.03)',
                  color: consultationFilter === tab ? 'white' : 'var(--text-muted)'
                }}
              >
                {tab.charAt(0) + tab.slice(1).toLowerCase()}
              </button>
            ))}
          </div>

          {consultationsLoading ? (
            <div style={styles.subLoader}>Loading bookings...</div>
          ) : filteredBookings.length === 0 ? (
            <div style={styles.emptyView}>
              <Calendar size={36} color="var(--border)" style={{ marginBottom: '12px' }} />
              <p>No consultations found matching: {consultationFilter.toLowerCase()}</p>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginTop: '12px' }}>
              {filteredBookings.map(c => (
                <div key={c.id} style={styles.consultationCard}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                      <h4 style={{ fontWeight: '800', fontSize: '15px' }}>Advocate {c.lawyerName}</h4>
                      <p style={{ fontSize: '13px', color: 'var(--primary)', fontWeight: '600', marginTop: '2px' }}>
                        Issue: {c.caseTitle}
                      </p>
                    </div>
                    <span style={{ ...styles.statusLabel, ...getStatusStyle(c.status) }}>
                      {c.status || 'PENDING'}
                    </span>
                  </div>
                  <div style={styles.consultationCardDivider} />
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', fontSize: '12px' }}>
                    <div>Date & Time: <strong>{c.date}</strong> at <strong>{c.time}</strong></div>
                    <div>Consultation Fee: <strong>₹{c.fee || 500}</strong> ({c.consultationType || 'Online'})</div>
                    {c.bookingId && <div>Booking ID: <strong>{c.bookingId}</strong></div>}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* -------------------- VIEW 6: APP TUTORIAL -------------------- */}
      {activeSection === 'tutorial' && (
        <div className="glass-panel" style={styles.card}>
          <div style={styles.viewHeader}>
            <button className="btn btn-secondary" style={styles.backBtn} onClick={() => setActiveSection('menu')}>
              <ArrowLeft size={16} /> Back
            </button>
            <h2 style={{ fontSize: '20px' }}>App Tutorial</h2>
          </div>

          <p style={{ color: 'var(--text-muted)', fontSize: '13px', marginBottom: '20px' }}>
            Welcome to Nyaya AI Legal Platform! Follow this guide to learn how to make the most of the application services:
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            
            <div style={styles.tutorialStep}>
              <div style={styles.tutorialStepHeader}>
                <MessageSquare size={18} color="var(--secondary)" />
                <h4>1. AI Legal Assistant / AI Help</h4>
              </div>
              <p style={styles.tutorialStepText}>
                Need answers for a legal issue? Navigate to the **AI Assistant** section, type your issue or query (e.g., "property dispute with neighbor"), and receive contextual explanations, relevant legal references, and advice.
              </p>
            </div>

            <div style={styles.tutorialStep}>
              <div style={styles.tutorialStepHeader}>
                <School size={18} color="var(--accent)" />
                <h4>2. AI Legal Learning</h4>
              </div>
              <p style={styles.tutorialStepText}>
                Search specific articles or legal sections (e.g., "Article 21" or "IPC 302") to get a simplified explanation, clear illustrations, and details on punishments or applications under Indian laws.
              </p>
            </div>

            <div style={styles.tutorialStep}>
              <div style={styles.tutorialStepHeader}>
                <BookOpen size={18} color="var(--primary)" />
                <h4>3. Law Search & Encyclopedia</h4>
              </div>
              <p style={styles.tutorialStepText}>
                Browse historical and updated laws including the Constitution, IPC (Indian Penal Code), BNS (Bharatiya Nyaya Sanhita), CrPC, and other legal acts to read exact statutory definitions.
              </p>
            </div>

            <div style={styles.tutorialStep}>
              <div style={styles.tutorialStepHeader}>
                <Calendar size={18} color="var(--tertiary)" />
                <h4>4. Find Lawyer & Book Consultations</h4>
              </div>
              <p style={styles.tutorialStepText}>
                Filter registered legal advocates by location, fees, and specialization. Click on profiles to inspect bar credentials, experience details, and request booking slots for online or physical consults.
              </p>
            </div>

            <div style={styles.tutorialStep}>
              <div style={styles.tutorialStepHeader}>
                <Settings size={18} color="var(--text-main)" />
                <h4>5. Profile & Customization Settings</h4>
              </div>
              <p style={styles.tutorialStepText}>
                Keep your details updated in **My Profile**. Customize layout theme colors, toggle dark mode, view your consultation status history, and review previous chat queries.
              </p>
            </div>

          </div>
        </div>
      )}

      {/* -------------------- APP SETTINGS MODAL -------------------- */}
      {settingsOpen && (
        <div style={styles.modalOverlay}>
          <div className="glass-panel" style={styles.modal}>
            <h3 style={{ marginBottom: '16px', fontSize: '18px' }}>App Customization Settings</h3>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '24px' }}>
              
              {/* Dark Mode Option */}
              <div style={styles.modalOptionRow}>
                <div>
                  <div style={{ fontWeight: '700', fontSize: '14px' }}>Dark Mode</div>
                  <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Enable dark background theme layout</div>
                </div>
                <input 
                  type="checkbox"
                  checked={darkMode}
                  onChange={(e) => setDarkMode(e.target.checked)}
                  style={{ width: '20px', height: '20px', cursor: 'pointer' }}
                />
              </div>

              {/* Theme Color Option */}
              <div style={styles.modalOptionGroup}>
                <label style={styles.modalLabel}>Theme Accent Color</label>
                <select 
                  className="input-field"
                  value={themeColor} 
                  onChange={(e) => setThemeColor(e.target.value)}
                  style={styles.modalSelect}
                >
                  <option value="Default">Default (Indigo/Violet)</option>
                  <option value="Blue">Ocean Blue</option>
                  <option value="Green">Emerald Green</option>
                  <option value="Purple">Purple Rain</option>
                  <option value="Amber">Sunset Amber</option>
                </select>
              </div>

              {/* Font Color / Size Option */}
              <div style={styles.modalOptionGroup}>
                <label style={styles.modalLabel}>Font Size</label>
                <select 
                  className="input-field" 
                  value={fontColor} 
                  onChange={(e) => setFontColor(e.target.value)}
                  style={styles.modalSelect}
                >
                  <option value="Default">Default (Normal)</option>
                  <option value="Medium">Medium</option>
                  <option value="Large">Large</option>
                </select>
              </div>

              {/* Language Selection */}
              <div style={styles.modalOptionGroup}>
                <label style={styles.modalLabel}>App Language</label>
                <select 
                  className="input-field"
                  value={settingsLanguage} 
                  onChange={(e) => setSettingsLanguage(e.target.value)}
                  style={styles.modalSelect}
                >
                  <option value="en">English</option>
                  <option value="hi">हिन्दी (Hindi)</option>
                  <option value="ta">தமிழ் (Tamil)</option>
                  <option value="te">తెలుగు (Telugu)</option>
                </select>
              </div>

            </div>

            {settingsMessage && (
              <div style={{ ...styles.successBox, marginBottom: '16px' }}>
                <CheckCircle size={16} color="var(--secondary)" />
                <span>{settingsMessage}</span>
              </div>
            )}

            <div style={{ display: 'flex', gap: '12px' }}>
              <button 
                className="btn" 
                style={{ flex: 1, background: 'rgba(255,255,255,0.04)', color: 'var(--text-main)' }}
                onClick={() => {
                  setSettingsOpen(false);
                  setSettingsMessage('');
                }}
                disabled={settingsSaving}
              >
                Cancel
              </button>
              <button 
                className="btn btn-primary" 
                style={{ flex: 1 }}
                onClick={handleSaveSettings}
                disabled={settingsSaving}
              >
                {settingsSaving ? 'Saving...' : 'Save Settings'}
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}

const styles = {
  container: {
    padding: '24px',
    maxWidth: '800px',
    margin: '0 auto',
    width: '100%'
  },
  card: {
    padding: '24px',
    display: 'flex',
    flexDirection: 'column',
    gap: '20px'
  },
  profileCard: {
    padding: '24px'
  },
  profileInfoLayout: {
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
    flexWrap: 'wrap'
  },
  avatarCircle: {
    width: '64px',
    height: '64px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, var(--primary), var(--tertiary))',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  },
  profileName: {
    fontSize: '18px',
    fontWeight: '800'
  },
  profileEmail: {
    fontSize: '13px',
    color: 'var(--text-muted)',
    marginTop: '2px'
  },
  roleBadge: {
    display: 'inline-flex',
    alignItems: 'center',
    background: 'rgba(99, 102, 241, 0.12)',
    color: 'var(--primary)',
    padding: '4px 10px',
    borderRadius: '6px',
    fontSize: '11px',
    fontWeight: '700',
    marginTop: '6px',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  editBtn: {
    padding: '8px 16px',
    fontSize: '13px',
    marginLeft: 'auto'
  },
  sectionHeader: {
    fontSize: '11px',
    fontWeight: '900',
    color: 'var(--primary)',
    textTransform: 'uppercase',
    letterSpacing: '1px',
    marginTop: '16px',
    paddingLeft: '4px'
  },
  menuGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px'
  },
  menuItem: {
    padding: '16px',
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
    cursor: 'pointer',
    transition: 'all 0.2s',
    '&:hover': {
      background: 'rgba(255,255,255,0.03)',
      transform: 'translateY(-2px)'
    }
  },
  menuIconBox: {
    width: '40px',
    height: '40px',
    borderRadius: '10px',
    background: 'rgba(255,255,255,0.02)',
    border: '1px solid var(--border)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  },
  menuTitle: {
    fontSize: '14px',
    fontWeight: '700',
    color: 'var(--text-main)'
  },
  menuDescription: {
    fontSize: '12px',
    color: 'var(--text-muted)',
    marginTop: '2px'
  },
  viewHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
    borderBottom: '1px solid var(--border)',
    paddingBottom: '16px',
    marginBottom: '8px'
  },
  backBtn: {
    padding: '6px 12px',
    fontSize: '12px',
    borderRadius: '8px'
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  formSectionTitle: {
    fontSize: '11px',
    fontWeight: '800',
    color: 'var(--primary)',
    textTransform: 'uppercase',
    letterSpacing: '0.5px',
    marginTop: '12px',
    borderBottom: '1px solid var(--border)',
    paddingBottom: '6px'
  },
  formGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px'
  },
  label: {
    fontSize: '12px',
    fontWeight: '700',
    color: 'var(--text-main)'
  },
  inputWrapper: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center'
  },
  icon: {
    position: 'absolute',
    left: '16px',
    color: 'var(--primary)'
  },
  formRow: {
    display: 'flex',
    gap: '16px'
  },
  submitBtn: {
    height: '44px',
    marginTop: '12px',
    fontSize: '14px'
  },
  successBox: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '12px',
    background: 'rgba(16, 185, 129, 0.1)',
    border: '1px solid rgba(16, 185, 129, 0.2)',
    borderRadius: '12px',
    color: 'var(--text-main)',
    fontSize: '13px'
  },
  loader: {
    textAlign: 'center',
    padding: '48px',
    color: 'var(--text-muted)',
    fontSize: '14px'
  },
  subLoader: {
    textAlign: 'center',
    padding: '32px',
    color: 'var(--text-muted)',
    fontSize: '13px'
  },
  emptyView: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '48px 16px',
    color: 'var(--text-muted)',
    fontSize: '13px'
  },
  bulkActionBar: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '12px 8px',
    borderBottom: '1px solid var(--border)',
    marginBottom: '12px'
  },
  selectAllToggle: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    cursor: 'pointer',
    userSelect: 'none'
  },
  bulkDeleteBtn: {
    padding: '6px 12px',
    fontSize: '12px',
    borderRadius: '8px',
    display: 'flex',
    alignItems: 'center',
    gap: '6px'
  },
  historyList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px'
  },
  historyItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px',
    borderRadius: '12px',
    border: '1px solid var(--border)',
    cursor: 'pointer',
    transition: 'all 0.15s ease'
  },
  checkboxWrapper: {
    display: 'flex',
    alignItems: 'center',
    padding: '4px'
  },
  checkboxInput: {
    width: '16px',
    height: '16px',
    cursor: 'pointer'
  },
  historyItemTitle: {
    fontSize: '13px',
    fontWeight: '700',
    color: 'var(--text-main)',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis'
  },
  historyItemSnippet: {
    fontSize: '12px',
    color: 'var(--text-muted)',
    marginTop: '2px',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis'
  },
  historyItemMeta: {
    fontSize: '11px',
    color: 'var(--text-muted)',
    marginTop: '4px'
  },
  deleteItemBtn: {
    background: 'none',
    border: 'none',
    color: 'var(--text-muted)',
    cursor: 'pointer',
    padding: '8px',
    borderRadius: '6px',
    transition: 'all 0.1s',
    '&:hover': {
      color: 'var(--error)',
      background: 'rgba(239, 68, 68, 0.08)'
    }
  },
  miniTabsContainer: {
    display: 'flex',
    gap: '8px',
    overflowX: 'auto',
    paddingBottom: '8px',
    borderBottom: '1px solid var(--border)'
  },
  miniTab: {
    padding: '6px 12px',
    borderRadius: '8px',
    border: '1px solid var(--border)',
    fontSize: '12px',
    fontWeight: '700',
    cursor: 'pointer',
    whiteSpace: 'nowrap'
  },
  consultationCard: {
    padding: '16px',
    background: 'rgba(255, 255, 255, 0.01)',
    borderRadius: '12px',
    border: '1px solid var(--border)'
  },
  consultationCardDivider: {
    height: '1px',
    background: 'var(--border)',
    margin: '12px 0'
  },
  statusLabel: {
    fontSize: '9px',
    fontWeight: '900',
    padding: '4px 8px',
    borderRadius: '6px',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  tutorialStep: {
    padding: '16px',
    background: 'rgba(255, 255, 255, 0.01)',
    borderRadius: '12px',
    border: '1px solid var(--border)'
  },
  tutorialStepHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontWeight: '800',
    fontSize: '14px',
    color: 'var(--text-main)',
    marginBottom: '8px'
  },
  tutorialStepText: {
    fontSize: '12.5px',
    color: 'var(--text-muted)',
    lineHeight: '1.6'
  },
  // Modal styles
  modalOverlay: {
    position: 'fixed',
    top: 0, left: 0, right: 0, bottom: 0,
    background: 'rgba(15, 23, 42, 0.8)',
    backdropFilter: 'blur(12px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
    padding: '20px'
  },
  modal: {
    width: '100%',
    maxWidth: '440px',
    padding: '24px',
    display: 'flex',
    flexDirection: 'column'
  },
  modalOptionRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingBottom: '12px',
    borderBottom: '1px solid var(--border)'
  },
  modalOptionGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px'
  },
  modalLabel: {
    fontSize: '12px',
    fontWeight: '700',
    color: 'var(--text-main)'
  },
  modalSelect: {
    fontSize: '13px'
  }
};
