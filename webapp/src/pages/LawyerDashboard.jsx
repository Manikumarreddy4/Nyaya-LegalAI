import React, { useEffect, useState } from 'react';
import { db } from '../firebase';
import { 
  collection, 
  query, 
  where, 
  doc, 
  updateDoc, 
  getDoc,
  onSnapshot
} from 'firebase/firestore';
import { 
  Check, 
  X, 
  Users, 
  Calendar, 
  DollarSign, 
  Clock, 
  AlertCircle, 
  Bell, 
  Phone, 
  MapPin, 
  Mail, 
  ShieldCheck 
} from 'lucide-react';

export default function LawyerDashboard({ user, onNavigate }) {
  const [profile, setProfile] = useState(null);
  const [consultations, setConsultations] = useState([]);
  const [isOnline, setIsOnline] = useState(true);
  const [selectedTab, setSelectedTab] = useState('Pending');
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    if (!user || !user.uid) return;
    setLoading(true);
    let unsubscribeProfile = () => {};
    let unsubscribeConsultations = () => {};

    async function loadLawyerData() {
      try {
        // Load lawyer profile details in real-time
        unsubscribeProfile = onSnapshot(doc(db, 'lawyers', user.uid), (docSnapshot) => {
          if (docSnapshot.exists()) {
            const profileData = docSnapshot.data();
            setProfile(profileData);
            setIsOnline(profileData.onlineAvailable !== false);
          }
        }, (error) => {
          console.error('Error listening to lawyer profile', error);
        });

        // Setup real-time consultations listener
        const consultationsRef = collection(db, 'consultations');
        const q = query(consultationsRef, where('lawyerId', '==', user.uid));
        
        unsubscribeConsultations = onSnapshot(q, (querySnapshot) => {
          const docs = [];
          querySnapshot.forEach(docSnapshot => {
            docs.push({ id: docSnapshot.id, ...docSnapshot.data() });
          });
          setConsultations(docs);
          setLoading(false);
        }, (error) => {
          console.error('Error listening to lawyer consultations', error);
          setLoading(false);
        });
      } catch (e) {
        console.error('Error loading lawyer data', e);
        setLoading(false);
      }
    }
    loadLawyerData();

    return () => {
      unsubscribeProfile();
      unsubscribeConsultations();
    };
  }, [user]);

  const handleUpdateStatus = async (consultationId, newStatus) => {
    try {
      const docRef = doc(db, 'consultations', consultationId);
      await updateDoc(docRef, { status: newStatus });
      
      // Update locally
      setConsultations(prev => 
        prev.map(c => c.id === consultationId ? { ...c, status: newStatus } : c)
      );
    } catch (e) {
      alert('Error updating consultation status: ' + e.message);
    }
  };

  const handleToggleOnline = async () => {
    if (!user || !user.uid) return;
    try {
      const newStatus = !isOnline;
      await updateDoc(doc(db, 'lawyers', user.uid), { onlineAvailable: newStatus });
      await updateDoc(doc(db, 'users', user.uid), { onlineAvailable: newStatus });
      setIsOnline(newStatus);
    } catch (e) {
      alert('Error updating status: ' + e.message);
    }
  };

  // Helper date matching (today vs upcoming)
  const getTodayDateStr = () => {
    const d = new Date();
    const month = d.toLocaleString('default', { month: 'short' });
    const date = String(d.getDate()).padStart(2, '0');
    const year = d.getFullYear();
    return `${date} ${month} ${year}`;
  };

  const todayStr = getTodayDateStr();

  const pendingRequests = consultations.filter(c => c.status?.toUpperCase() === 'PENDING');
  const acceptedRequests = consultations.filter(c => c.status?.toUpperCase() === 'ACCEPTED');
  const completedRequests = consultations.filter(c => c.status?.toUpperCase() === 'COMPLETED');
  const rejectedRequests = consultations.filter(c => c.status?.toUpperCase() === 'REJECTED');

  const totalEarnings = consultations
    .filter(c => c.status?.toUpperCase() === 'ACCEPTED' || c.status?.toUpperCase() === 'COMPLETED')
    .reduce((acc, c) => acc + (parseFloat(c.fee) || 0), 0);

  const todayAppointments = acceptedRequests.filter(c => {
    const dateVal = c.date || '';
    return dateVal.toLowerCase().includes('today') || dateVal.includes(todayStr);
  });

  const upcomingAppointments = acceptedRequests.filter(c => !todayAppointments.includes(c));

  const getFilteredList = () => {
    switch (selectedTab) {
      case 'Pending': return pendingRequests;
      case 'Accepted': return acceptedRequests;
      case 'Completed': return completedRequests;
      case 'Rejected': return rejectedRequests;
      default: return consultations;
    }
  };

  const displayList = getFilteredList();

  return (
    <div className="fade-in-up" style={styles.container}>
      {/* Welcome Banner */}
      <div className="glass-panel" style={styles.welcomeBanner}>
        <div style={styles.bannerLeft}>
          <h1 style={styles.title}>Welcome, Advocate {profile?.name || user?.name || 'Advocate'}</h1>
          <div style={styles.verifyBadge}>
            <ShieldCheck size={16} color={profile?.verificationStatus === 'VERIFIED' ? 'var(--secondary)' : 'var(--accent)'} />
            <span style={{color: profile?.verificationStatus === 'VERIFIED' ? 'var(--secondary)' : 'var(--accent)', fontWeight: 'bold'}}>
              {profile?.verificationStatus === 'VERIFIED' ? '✓ Verified Advocate' : 'Verification Under Review'}
            </span>
          </div>
          <p style={styles.subtitle}>Manage your consultation slots, approve pending appointments, and view earning stats.</p>
        </div>
        <div className="glass-panel" style={styles.statusToggle}>
          <div style={{textAlign: 'right'}}>
            <div style={styles.toggleLabel}>Availability Status</div>
            <div style={{...styles.toggleState, color: isOnline ? 'var(--secondary)' : 'var(--error)'}}>
              {isOnline ? 'Available / Accepting Clients' : 'Offline / Do Not Disturb'}
            </div>
          </div>
          <label style={styles.switch}>
            <input type="checkbox" checked={isOnline} onChange={handleToggleOnline} />
            <span style={styles.slider}></span>
          </label>
        </div>
      </div>

      {/* Advocate Details Card */}
      <div className="glass-panel" style={styles.detailsCard}>
        <h3 style={styles.cardHeader}>Advocate Profile Details</h3>
        <div style={styles.detailsGrid}>
          <div style={styles.detailItem}>
            <Mail size={16} color="var(--primary)" />
            <div>
              <div style={styles.detailLabel}>Email</div>
              <div style={styles.detailValue}>{profile?.email || user?.email}</div>
            </div>
          </div>
          <div style={styles.detailItem}>
            <Phone size={16} color="var(--primary)" />
            <div>
              <div style={styles.detailLabel}>Phone</div>
              <div style={styles.detailValue}>{profile?.phone || user?.phone || 'Not provided'}</div>
            </div>
          </div>
          <div style={styles.detailItem}>
            <MapPin size={16} color="var(--primary)" />
            <div>
              <div style={styles.detailLabel}>Location / City</div>
              <div style={styles.detailValue}>{profile?.location || 'Not specified'}</div>
            </div>
          </div>
          <div style={styles.detailItem}>
            <Users size={16} color="var(--primary)" />
            <div>
              <div style={styles.detailLabel}>Specialization</div>
              <div style={styles.detailValue}>{profile?.specialization || 'General Practice'}</div>
            </div>
          </div>
          <div style={styles.detailItem}>
            <Clock size={16} color="var(--primary)" />
            <div>
              <div style={styles.detailLabel}>Experience</div>
              <div style={styles.detailValue}>{profile?.experience || '0 Years'}</div>
            </div>
          </div>
          <div style={styles.detailItem}>
            <ShieldCheck size={16} color="var(--primary)" />
            <div>
              <div style={styles.detailLabel}>Bar Council Enrollment Number</div>
              <div style={styles.detailValue}>{profile?.barCouncilNumber || 'Not configured'}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Stats Counter Row */}
      <div style={styles.statsRow}>
        <div className="glass-panel" style={styles.statCard}>
          <Clock size={24} color="var(--accent)" />
          <div style={styles.statVal}>{pendingRequests.length}</div>
          <div style={styles.statLbl}>Pending Requests</div>
        </div>
        <div className="glass-panel" style={styles.statCard}>
          <Check size={24} color="var(--secondary)" />
          <div style={styles.statVal}>{acceptedRequests.length}</div>
          <div style={styles.statLbl}>Accepted Meetings</div>
        </div>
        <div className="glass-panel" style={styles.statCard}>
          <DollarSign size={24} color="var(--tertiary)" />
          <div style={styles.statVal}>₹{totalEarnings}</div>
          <div style={styles.statLbl}>Total Earnings</div>
        </div>
      </div>

      {/* Real-time Notifications bar */}
      {pendingRequests.length > 0 && (
        <div className="glass-panel" style={styles.notificationBar}>
          <Bell size={20} color="var(--accent)" className="shimmer" style={{borderRadius: '50%', padding: '2px'}} />
          <span style={styles.notificationText}>
            You have <strong>{pendingRequests.length}</strong> pending consultation booking requests from clients! Action required.
          </span>
        </div>
      )}

      {/* Today's Appointments Section */}
      <h3 style={styles.sectionHeader}>Today's Appointments ({todayAppointments.length})</h3>
      {todayAppointments.length === 0 ? (
        <div className="glass-panel" style={styles.emptyCard}>No appointments scheduled for today.</div>
      ) : (
        <div style={styles.appointmentList}>
          {todayAppointments.map((c) => (
            <AppointmentCard key={c.id} c={c} />
          ))}
        </div>
      )}

      {/* Upcoming Appointments Section */}
      <h3 style={styles.sectionHeader}>Upcoming Appointments ({upcomingAppointments.length})</h3>
      {upcomingAppointments.length === 0 ? (
        <div className="glass-panel" style={styles.emptyCard}>No upcoming appointments.</div>
      ) : (
        <div style={styles.appointmentList}>
          {upcomingAppointments.map((c) => (
            <AppointmentCard key={c.id} c={c} />
          ))}
        </div>
      )}

      {/* Request Management Table & Filters */}
      <div style={styles.headerRow}>
        <h3 style={styles.sectionHeader}>Consultation Requests & History</h3>
        <div style={styles.tabsRow}>
          {['Pending', 'Accepted', 'Rejected', 'Completed', 'All'].map((tab) => (
            <button 
              key={tab} 
              style={{
                ...styles.tabBtn, 
                background: selectedTab === tab ? 'var(--primary)' : 'rgba(255,255,255,0.05)',
                color: selectedTab === tab ? 'white' : 'var(--text-muted)'
              }}
              onClick={() => setSelectedTab(tab)}
            >
              {tab}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div style={styles.loadingSpinner}>Loading requests...</div>
      ) : displayList.length === 0 ? (
        <div className="glass-panel" style={styles.emptyCard}>No consultations match this filter.</div>
      ) : (
        <div style={styles.requestGrid}>
          {displayList.map((c) => (
            <div key={c.id} className="glass-panel" style={styles.requestCard}>
              <div style={styles.reqHeader}>
                <h4 style={styles.clientName}>{c.clientName || c.userName || 'Client'}</h4>
                <span style={{
                  ...styles.statusLabel,
                  background: c.status?.toUpperCase() === 'ACCEPTED' ? 'rgba(16, 185, 129, 0.15)' :
                              c.status?.toUpperCase() === 'PENDING' ? 'rgba(245, 158, 11, 0.15)' :
                              c.status?.toUpperCase() === 'COMPLETED' ? 'rgba(99, 102, 241, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                  color: c.status?.toUpperCase() === 'ACCEPTED' ? 'var(--secondary)' :
                         c.status?.toUpperCase() === 'PENDING' ? 'var(--accent)' :
                         c.status?.toUpperCase() === 'COMPLETED' ? 'var(--tertiary)' : 'var(--error)'
                }}>
                  {c.status || 'PENDING'}
                </span>
              </div>
              <div style={styles.caseTitle}>Issue: {c.caseTitle || 'General Legal consultation'}</div>
              <p style={styles.caseDesc}>{c.caseDescription || 'No case details provided.'}</p>
              
              <div style={styles.reqDetails}>
                <div><strong>Schedule:</strong> {c.date} at {c.time}</div>
                <div><strong>Type:</strong> {c.consultationType || 'Online'}</div>
                <div><strong>Contact:</strong> {c.contactNumber || 'Not provided'}</div>
              </div>

              {c.status?.toUpperCase() === 'PENDING' && (
                <div style={styles.actionsRow}>
                  <button 
                    className="btn btn-primary" 
                    style={{...styles.actionBtn, background: 'var(--secondary)'}}
                    onClick={() => handleUpdateStatus(c.id, 'ACCEPTED')}
                  >
                    <Check size={16} /> Accept
                  </button>
                  <button 
                    className="btn btn-secondary" 
                    style={{...styles.actionBtn, borderColor: 'var(--error)', color: 'var(--error)'}}
                    onClick={() => handleUpdateStatus(c.id, 'REJECTED')}
                  >
                    <X size={16} /> Reject
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// Sub-component for clean appointments
function AppointmentCard({ c }) {
  return (
    <div className="glass-panel" style={styles.appCard}>
      <div style={styles.appHeader}>
        <div>
          <h4 style={styles.appClient}>{c.clientName || c.userName || 'Client'}</h4>
          <div style={styles.appCase}>Issue: {c.caseTitle || 'General Legal Consultation'}</div>
        </div>
        <div style={styles.appType}>{c.consultationType || 'Online'}</div>
      </div>
      <div style={styles.appMeta}>
        <Clock size={14} color="var(--primary)" />
        <span>Date: {c.date} | Time: {c.time}</span>
      </div>
      {c.contactNumber && (
        <div style={styles.appMeta}>
          <Phone size={14} color="var(--primary)" />
          <span>Phone: {c.contactNumber}</span>
        </div>
      )}
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
  welcomeBanner: {
    padding: '32px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
    gap: '20px',
    background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(16, 185, 129, 0.1))'
  },
  bannerLeft: {
    flex: 1
  },
  title: {
    fontSize: '28px',
    fontWeight: '800',
    marginBottom: '8px'
  },
  verifyBadge: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '6px',
    padding: '6px 12px',
    background: 'rgba(255,255,255,0.03)',
    border: '1px solid var(--border)',
    borderRadius: '10px',
    fontSize: '13px',
    marginBottom: '12px'
  },
  subtitle: {
    fontSize: '14px',
    color: 'var(--text-muted)'
  },
  statusToggle: {
    display: 'flex',
    alignItems: 'center',
    gap: '16px',
    padding: '16px 20px'
  },
  toggleLabel: {
    fontSize: '12px',
    color: 'var(--text-muted)',
    fontWeight: '600'
  },
  toggleState: {
    fontSize: '14px',
    fontWeight: '800'
  },
  switch: {
    position: 'relative',
    display: 'inline-block',
    width: '48px',
    height: '24px'
  },
  slider: {
    position: 'absolute',
    cursor: 'pointer',
    top: 0, left: 0, right: 0, bottom: 0,
    backgroundColor: '#334155',
    transition: '.4s',
    borderRadius: '24px'
  },
  detailsCard: {
    padding: '24px'
  },
  cardHeader: {
    fontSize: '18px',
    fontWeight: '700',
    marginBottom: '16px',
    color: 'var(--primary)'
  },
  detailsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
    gap: '16px'
  },
  detailItem: {
    display: 'flex',
    gap: '12px',
    alignItems: 'center'
  },
  detailLabel: {
    fontSize: '11px',
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  detailValue: {
    fontSize: '14px',
    fontWeight: '600',
    color: 'var(--text-main)'
  },
  statsRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
    gap: '20px'
  },
  statCard: {
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    textAlign: 'center'
  },
  statVal: {
    fontSize: '28px',
    fontWeight: '850',
    margin: '8px 0 4px'
  },
  statLbl: {
    fontSize: '13px',
    color: 'var(--text-muted)',
    fontWeight: '600'
  },
  notificationBar: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '16px 20px',
    borderColor: 'rgba(245, 158, 11, 0.2)',
    background: 'rgba(245, 158, 11, 0.05)'
  },
  notificationText: {
    fontSize: '14px',
    color: 'var(--text-main)'
  },
  sectionHeader: {
    fontSize: '20px',
    fontWeight: '700',
    marginTop: '12px'
  },
  emptyCard: {
    padding: '24px',
    textAlign: 'center',
    color: 'var(--text-muted)',
    fontSize: '14px'
  },
  appointmentList: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
    gap: '16px'
  },
  appCard: {
    padding: '16px',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px'
  },
  appHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start'
  },
  appClient: {
    fontSize: '15px',
    fontWeight: '700'
  },
  appCase: {
    fontSize: '13px',
    color: 'var(--primary)',
    fontWeight: '600'
  },
  appType: {
    fontSize: '11px',
    fontWeight: '800',
    padding: '4px 8px',
    background: 'rgba(255,255,255,0.05)',
    borderRadius: '6px',
    color: 'var(--text-muted)'
  },
  appMeta: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontSize: '13px',
    color: 'var(--text-muted)'
  },
  headerRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: '16px',
    marginTop: '16px'
  },
  tabsRow: {
    display: 'flex',
    gap: '8px',
    flexWrap: 'wrap'
  },
  tabBtn: {
    padding: '8px 16px',
    borderRadius: '12px',
    border: '1px solid var(--border)',
    fontWeight: '600',
    fontSize: '13px',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  requestGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))',
    gap: '20px'
  },
  requestCard: {
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    gap: '12px'
  },
  reqHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  clientName: {
    fontSize: '16px',
    fontWeight: '750'
  },
  statusLabel: {
    fontSize: '10px',
    fontWeight: '900',
    padding: '4px 8px',
    borderRadius: '6px',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  caseTitle: {
    fontSize: '14px',
    fontWeight: '600',
    color: 'var(--text-main)'
  },
  caseDesc: {
    fontSize: '13px',
    color: 'var(--text-muted)',
    lineHeight: '1.5'
  },
  reqDetails: {
    display: 'flex',
    flexDirection: 'column',
    gap: '4px',
    fontSize: '13px',
    color: 'var(--text-muted)',
    padding: '12px',
    background: 'rgba(255,255,255,0.01)',
    borderRadius: '10px',
    border: '1px solid var(--border)'
  },
  actionsRow: {
    display: 'flex',
    gap: '12px',
    marginTop: '4px'
  },
  actionBtn: {
    flex: 1,
    height: '40px',
    fontSize: '13px'
  },
  loadingSpinner: {
    textAlign: 'center',
    padding: '40px',
    color: 'var(--text-muted)'
  }
};
