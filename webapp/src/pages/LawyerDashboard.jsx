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

function getAppointmentDateTime(c) {
  if (c.appointmentDateTime) {
    if (c.appointmentDateTime.toDate) {
      return c.appointmentDateTime.toDate();
    }
    if (c.appointmentDateTime.seconds) {
      return new Date(c.appointmentDateTime.seconds * 1000);
    }
  }

  if (!c.date && !c.dateTime) return null;
  const dateStr = c.date || c.dateTime;
  const cleanDateStr = dateStr.trim();
  
  let parsedDate = null;
  if (cleanDateStr.toLowerCase() === 'today') {
    parsedDate = new Date();
    parsedDate.setHours(0, 0, 0, 0);
  } else if (cleanDateStr.toLowerCase() === 'tomorrow') {
    parsedDate = new Date();
    parsedDate.setDate(parsedDate.getDate() + 1);
    parsedDate.setHours(0, 0, 0, 0);
  } else {
    let datePart = cleanDateStr;
    if (cleanDateStr.includes(',')) {
      datePart = cleanDateStr.split(',')[0].trim();
    } else if (cleanDateStr.toLowerCase().includes(' at ')) {
      datePart = cleanDateStr.split(/\s+at\s+/i)[0].trim();
    }
    
    const parts = datePart.split('/');
    if (parts.length === 3) {
      const day = parseInt(parts[0], 10);
      const month = parseInt(parts[1], 10) - 1;
      const year = parseInt(parts[2], 10);
      if (!isNaN(day) && !isNaN(month) && !isNaN(year)) {
        parsedDate = new Date(year, month, day, 0, 0, 0, 0);
      }
    } else {
      const partsDash = datePart.split('-');
      if (partsDash.length === 3) {
        const year = parseInt(partsDash[0], 10);
        const month = parseInt(partsDash[1], 10) - 1;
        const day = parseInt(partsDash[2], 10);
        if (!isNaN(day) && !isNaN(month) && !isNaN(year)) {
          parsedDate = new Date(year, month, day, 0, 0, 0, 0);
        }
      }
    }
    
    if (!parsedDate) {
      const ts = Date.parse(datePart);
      if (!isNaN(ts)) {
        parsedDate = new Date(ts);
        parsedDate.setHours(0, 0, 0, 0);
      }
    }
  }
  
  if (!parsedDate) return null;
  
  if (c.time) {
    const cleanTime = c.time.trim();
    const match = cleanTime.match(/^(\d+):(\d+)\s*(AM|PM)?$/i);
    if (match) {
      let hours = parseInt(match[1], 10);
      const minutes = parseInt(match[2], 10);
      const ampm = match[3];
      
      if (ampm) {
        if (ampm.toUpperCase() === 'PM' && hours < 12) hours += 12;
        if (ampm.toUpperCase() === 'AM' && hours === 12) hours = 0;
      }
      parsedDate.setHours(hours, minutes, 0, 0);
      return parsedDate;
    }
  }
  
  parsedDate.setHours(23, 59, 0, 0);
  return parsedDate;
}

function getActivityTimestamp(c) {
  if (!c) return 0;
  const times = [
    c.updatedAt,
    c.createdAt,
    c.acceptedAt,
    c.rejectedAt,
    c.completedAt,
    c.expiredAt
  ];
  let maxMs = 0;
  for (const t of times) {
    if (t) {
      let ms = 0;
      if (typeof t.toMillis === 'function') {
        ms = t.toMillis();
      } else if (t.seconds) {
        ms = t.seconds * 1000;
      } else if (t instanceof Date) {
        ms = t.getTime();
      } else {
        const parsed = Date.parse(t);
        if (!isNaN(parsed)) ms = parsed;
      }
      if (ms > maxMs) {
        maxMs = ms;
      }
    }
  }
  return maxMs;
}

async function checkAndExpireConsultations(docs) {
  const now = new Date();
  const promises = [];
  
  for (const c of docs) {
    const safeStatus = (c.status || 'PENDING').toUpperCase();
    const apptTime = getAppointmentDateTime(c);
    
    console.log(`CONSULTATION_COMPLETION:`);
    console.log(`Consultation ID = ${c.consultationId || c.id}`);
    console.log(`Status = ${safeStatus}`);
    console.log(`Appointment Date = ${c.date || c.dateTime || c.appointmentDate || ''}`);
    console.log(`Appointment Time = ${c.time || c.appointmentTime || ''}`);
    console.log(`Parsed Appointment DateTime = ${apptTime ? apptTime.toISOString() : 'null'}`);
    console.log(`Current DateTime = ${now.toISOString()}`);
    
    if (safeStatus === 'PENDING') {
      if (apptTime && apptTime < now) {
        console.log(`Eligible for completion = false`);
        const docRef = doc(db, 'consultations', c.id);
        const updates = {
          status: 'EXPIRED',
          autoRejected: true,
          expiredAt: new Date(),
          expiryReason: "Lawyer did not respond before the scheduled appointment time"
        };
        promises.push(updateDoc(docRef, updates).catch(err => console.error("Error auto-expiring:", err)));
      } else {
        console.log(`Eligible for completion = false`);
      }
    } else if (safeStatus === 'ACCEPTED') {
      if (apptTime && now >= apptTime) {
        console.log(`Eligible for completion = true`);
        const docRef = doc(db, 'consultations', c.id);
        const updates = {
          status: 'COMPLETED',
          completedAt: new Date()
        };
        promises.push(updateDoc(docRef, updates).catch(err => console.error("Error completing consultation:", err)));
      } else {
        console.log(`Eligible for completion = false`);
      }
    } else {
      console.log(`Eligible for completion = false`);
    }
  }
  
  if (promises.length > 0) {
    await Promise.all(promises);
  }
}

export default function LawyerDashboard({ user, onNavigate }) {
  const [profile, setProfile] = useState(null);
  const [consultations, setConsultations] = useState([]);
  const [isOnline, setIsOnline] = useState(true);
  const [isInPersonOnline, setIsInPersonOnline] = useState(true);
  const [localInPersonAvailableOverride, setLocalInPersonAvailableOverride] = useState(null);
  const [selectedTab, setSelectedTab] = useState('Pending');
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    if (!user || !user.uid) return;
    setLoading(true);
    let unsubscribeProfile = () => {};
    let unsubscribeConsultations = () => {};

    async function loadLawyerData() {
      try {
        console.log(`LAWYER_AVAILABILITY: Dashboard opened`);
        console.log(`LAWYER_AVAILABILITY: Lawyer ID = ${user.uid}`);
        unsubscribeProfile = onSnapshot(doc(db, 'lawyers', user.uid), (docSnapshot) => {
          if (docSnapshot.exists()) {
            const profileData = docSnapshot.data();
            setProfile(profileData);
            const isAvail = profileData.availability_status !== undefined ? profileData.availability_status === true : (profileData.isAvailable !== false && profileData.onlineAvailable !== false);
            console.log(`LAWYER_AVAILABILITY: Listener received = ${isAvail}`);
            console.log(`LAWYER_AVAILABILITY: Loaded status = ${isAvail}`);
            setIsOnline(isAvail);

            const isInPersonAvail = profileData.in_person_consultation_available !== undefined ? profileData.in_person_consultation_available === true : profileData.isInPersonAvailable !== false;
            console.log(`IN_PERSON_AVAILABILITY: Loaded = ${isInPersonAvail}`);
            setIsInPersonOnline(isInPersonAvail);
          }
        }, (error) => {
          console.error(`LAWYER_AVAILABILITY ERROR:`, error);
        });

        const consultationsRef = collection(db, 'consultations');
        const q = query(consultationsRef, where('lawyerId', '==', user.uid));
        
        console.log("CONSULTATION_SYNC: Firestore listener attached");
        unsubscribeConsultations = onSnapshot(q, async (querySnapshot) => {
          console.log("CONSULTATION_SYNC: Snapshot update received");
          console.log(`CONSULTATION_SYNC: Total consultations = ${querySnapshot.size}`);
          const docs = [];
          querySnapshot.forEach(docSnapshot => {
            docs.push({ id: docSnapshot.id, ...docSnapshot.data() });
          });

          await checkAndExpireConsultations(docs);

          const sortedDocs = [...docs].sort((a, b) => getActivityTimestamp(b) - getActivityTimestamp(a));
          setConsultations(sortedDocs);
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
      const docSnap = await getDoc(docRef);
      let existingBookingId = '';
      if (docSnap.exists()) {
        const currentStatus = docSnap.data().status || 'PENDING';
        if (currentStatus.toUpperCase() === 'EXPIRED') {
          alert('Cannot update status of expired consultation.');
          return;
        }
        existingBookingId = docSnap.data().bookingId || '';
      }

      const updates = { 
        status: newStatus,
        updatedAt: new Date()
      };

      if (newStatus.toUpperCase() === 'ACCEPTED') {
        const bookingId = existingBookingId || `NYA-2026-${Math.random().toString(36).substring(2, 10).toUpperCase()}`;
        updates.bookingId = bookingId;
        updates.acceptedAt = new Date();
      } else if (newStatus.toUpperCase() === 'REJECTED') {
        updates.rejectedAt = new Date();
      }

      await updateDoc(docRef, updates);
      
      setConsultations(prev => {
        const updated = prev.map(c => c.id === consultationId ? { ...c, ...updates } : c);
        return [...updated].sort((a, b) => getActivityTimestamp(b) - getActivityTimestamp(a));
      });
    } catch (e) {
      alert('Error updating consultation status: ' + e.message);
    }
  };

  const handleToggleOnline = async () => {
    if (!user || !user.uid) return;
    const previous = isOnline;
    const newStatus = !isOnline;
    console.log(`LAWYER_AVAILABILITY: Toggle changed by user = ${newStatus}`);
    setIsOnline(newStatus);
    try {
      const updates = {
        isAvailable: newStatus,
        onlineAvailable: newStatus,
        availability_status: newStatus,
        video_consultation_available: newStatus,
        availabilityUpdatedAt: new Date(),
        updatedAt: new Date()
      };
      console.log(`LAWYER_AVAILABILITY: Writing to Firestore = ${newStatus}`);
      await updateDoc(doc(db, 'lawyers', user.uid), updates);
      await updateDoc(doc(db, 'users', user.uid), updates);
      console.log(`LAWYER_AVAILABILITY: Firestore update successful = ${newStatus}`);
    } catch (e) {
      console.error(`LAWYER_AVAILABILITY ERROR:`, e);
      setIsOnline(previous);
      alert('Error updating status: ' + e.message);
    }
  };

  const handleToggleInPerson = async () => {
    if (!user || !user.uid) return;
    const currentStatus = localInPersonAvailableOverride !== null ? localInPersonAvailableOverride : isInPersonOnline;
    const newStatus = !currentStatus;
    console.log(`IN_PERSON_AVAILABILITY: Lawyer changed status = ${newStatus}`);
    setLocalInPersonAvailableOverride(newStatus);
    try {
      const updates = {
        isInPersonAvailable: newStatus,
        in_person_consultation_available: newStatus,
        inPersonAvailabilityUpdatedAt: new Date(),
        updatedAt: new Date()
      };
      console.log(`IN_PERSON_AVAILABILITY: Updating Firestore`);
      await updateDoc(doc(db, 'lawyers', user.uid), updates);
      await updateDoc(doc(db, 'users', user.uid), updates);
      console.log(`IN_PERSON_AVAILABILITY: Update successful`);
      setLocalInPersonAvailableOverride(null);
    } catch (e) {
      console.error(`IN_PERSON_AVAILABILITY ERROR:`, e);
      setLocalInPersonAvailableOverride(null);
      alert('Error updating in-person availability: ' + e.message);
    }
  };

  const isSameCalendarDate = (date1, date2) => {
    return date1.getFullYear() === date2.getFullYear() &&
           date1.getMonth() === date2.getMonth() &&
           date1.getDate() === date2.getDate();
  };

  const now = new Date();

  const pendingRequests = consultations.filter(c => c.status?.toUpperCase() === 'PENDING');
  const acceptedRequests = consultations.filter(c => c.status?.toUpperCase() === 'ACCEPTED');
  const completedRequests = consultations.filter(c => c.status?.toUpperCase() === 'COMPLETED');
  const rejectedRequests = consultations.filter(c => c.status?.toUpperCase() === 'REJECTED');

  const totalEarnings = consultations
    .filter(c => c.status?.toUpperCase() === 'ACCEPTED' || c.status?.toUpperCase() === 'COMPLETED')
    .reduce((acc, c) => acc + (parseFloat(c.fee) || 0), 0);

  const todayAppointments = acceptedRequests.filter(c => {
    const apptTime = getAppointmentDateTime(c);
    if (!apptTime) return false;
    return isSameCalendarDate(apptTime, now) && apptTime > now;
  });

  const upcomingAppointments = acceptedRequests.filter(c => {
    const apptTime = getAppointmentDateTime(c);
    if (!apptTime) return false;
    return apptTime > now && !isSameCalendarDate(apptTime, now);
  });

  const getFilteredList = () => {
    switch (selectedTab) {
      case 'Pending': return pendingRequests;
      case 'Accepted': return acceptedRequests;
      case 'Completed': return completedRequests;
      case 'Rejected': return rejectedRequests;
      case 'Expired': return consultations.filter(c => c.status?.toUpperCase() === 'EXPIRED');
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
          {profile?.verificationStatus === 'VERIFIED' && (
            <div style={styles.verifyBadge}>
              <ShieldCheck size={16} color="var(--secondary)" />
              <span style={{color: 'var(--secondary)', fontWeight: 'bold'}}>
                ✓ Verified Advocate
              </span>
            </div>
          )}
          <p style={styles.subtitle}>Manage your consultation slots, approve pending appointments, and view earning stats.</p>
        </div>
        <div style={{display: 'flex', flexDirection: 'column', gap: '16px'}}>
          <div className="glass-panel" style={styles.statusToggle}>
            <div style={{textAlign: 'right'}}>
              <div style={styles.toggleLabel}>Availability Status</div>
              <div style={{...styles.toggleState, color: isOnline ? 'var(--secondary)' : 'var(--text-muted)', fontWeight: 'bold'}}>
                {isOnline ? '🟢 Online' : '⚫ Offline'}
              </div>
              <div style={{fontSize: '13px', color: 'var(--text-main)', marginTop: '2px'}}>
                {isOnline ? 'Available for Consultations' : 'Currently Offline'}
              </div>
              <div style={{fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px', maxWidth: '300px'}}>
                When offline, you will not appear in Find Lawyer and users cannot send new consultation requests.
              </div>
            </div>
            <label style={styles.switch}>
              <input 
                type="checkbox" 
                checked={isOnline} 
                onChange={handleToggleOnline} 
                style={styles.checkbox} 
              />
              <span style={{
                ...styles.slider,
                backgroundColor: isOnline ? 'var(--secondary)' : '#64748b',
                boxShadow: isOnline ? '0 0 10px rgba(16, 185, 129, 0.4)' : 'none'
              }}>
                <span style={{
                  ...styles.thumb,
                  transform: isOnline ? 'translateX(28px)' : 'translateX(0)'
                }} />
              </span>
            </label>
          </div>

          <div className="glass-panel" style={styles.statusToggle}>
            <div style={{textAlign: 'right'}}>
              <div style={styles.toggleLabel}>In-Person Consultation Availability</div>
              <div style={{...styles.toggleState, color: (localInPersonAvailableOverride !== null ? localInPersonAvailableOverride : isInPersonOnline) ? 'var(--secondary)' : 'var(--text-muted)', fontWeight: 'bold'}}>
                {(localInPersonAvailableOverride !== null ? localInPersonAvailableOverride : isInPersonOnline) ? '🟢 Available' : '⚫ Not Available'}
              </div>
              <div style={{fontSize: '13px', color: 'var(--text-main)', marginTop: '2px'}}>
                {(localInPersonAvailableOverride !== null ? localInPersonAvailableOverride : isInPersonOnline) ? 'Available for Offline Meetings' : 'Not Available for Offline Meetings'}
              </div>
              <div style={{fontSize: '11px', color: 'var(--text-muted)', marginTop: '4px', maxWidth: '300px'}}>
                When turned off, users can still book online consultations if your main availability status is enabled.
              </div>
            </div>
            <label style={styles.switch}>
              <input 
                type="checkbox" 
                checked={localInPersonAvailableOverride !== null ? localInPersonAvailableOverride : isInPersonOnline} 
                onChange={handleToggleInPerson} 
                style={styles.checkbox} 
              />
              <span style={{
                ...styles.slider,
                backgroundColor: (localInPersonAvailableOverride !== null ? localInPersonAvailableOverride : isInPersonOnline) ? 'var(--secondary)' : '#64748b',
                boxShadow: (localInPersonAvailableOverride !== null ? localInPersonAvailableOverride : isInPersonOnline) ? '0 0 10px rgba(16, 185, 129, 0.4)' : 'none'
              }}>
                <span style={{
                  ...styles.thumb,
                  transform: (localInPersonAvailableOverride !== null ? localInPersonAvailableOverride : isInPersonOnline) ? 'translateX(28px)' : 'translateX(0)'
                }} />
              </span>
            </label>
          </div>
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
          {['Pending', 'Accepted', 'Rejected', 'Completed', 'Expired', 'All'].map((tab) => (
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
                              c.status?.toUpperCase() === 'COMPLETED' ? 'rgba(99, 102, 241, 0.15)' :
                              c.status?.toUpperCase() === 'EXPIRED' ? 'rgba(100, 116, 139, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                  color: c.status?.toUpperCase() === 'ACCEPTED' ? 'var(--secondary)' :
                         c.status?.toUpperCase() === 'PENDING' ? 'var(--accent)' :
                         c.status?.toUpperCase() === 'COMPLETED' ? 'var(--tertiary)' :
                         c.status?.toUpperCase() === 'EXPIRED' ? 'var(--text-muted)' : 'var(--error)'
                }}>
                  {c.status || 'PENDING'}
                </span>
              </div>
              <div style={styles.caseTitle}>Issue: {c.caseTitle || 'General Legal consultation'}</div>
              <p style={styles.caseDesc}>{c.caseDescription || 'No case details provided.'}</p>
              
              <div style={styles.reqDetails}>
                <div><strong>Schedule:</strong> {c.date} at {c.time}</div>
                <div><strong>Type:</strong> {c.consultationType || 'Online'}</div>
                {c.status?.toUpperCase() === 'ACCEPTED' && (
                  <>
                    {c.bookingId && <div><strong>Booking ID:</strong> {c.bookingId}</div>}
                    <div><strong>Contact:</strong> {c.userPhone || c.contactNumber || 'Not provided'}</div>
                    {c.userEmail && <div><strong>Email:</strong> {c.userEmail}</div>}
                  </>
                )}
              </div>

              {c.status?.toUpperCase() === 'EXPIRED' && (
                <div style={{
                  padding: '12px',
                  background: 'rgba(100, 116, 139, 0.05)',
                  borderRadius: '10px',
                  border: '1px dashed var(--border)',
                  color: 'var(--text-muted)',
                  fontSize: '13px',
                  fontWeight: '600',
                  marginTop: '8px'
                }}>
                  Automatically expired because no response was given before the appointment time.
                </div>
              )}

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
      {c.bookingId && (
        <div style={styles.appMeta}>
          <ShieldCheck size={14} color="var(--secondary)" />
          <span>Booking ID: {c.bookingId}</span>
        </div>
      )}
      <div style={styles.appMeta}>
        <Clock size={14} color="var(--primary)" />
        <span>Date: {c.date} | Time: {c.time}</span>
      </div>
      {(c.userPhone || c.contactNumber) && (
        <div style={styles.appMeta}>
          <Phone size={14} color="var(--primary)" />
          <span>Phone: {c.userPhone || c.contactNumber}</span>
        </div>
      )}
      {c.userEmail && (
        <div style={styles.appMeta}>
          <Mail size={14} color="var(--primary)" />
          <span>Email: {c.userEmail}</span>
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
    justifyContent: 'space-between',
    gap: '24px',
    padding: '16px 20px',
    width: '100%',
    minWidth: '320px'
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
    width: '56px',
    height: '28px',
    cursor: 'pointer'
  },
  checkbox: {
    opacity: 0,
    width: 0,
    height: 0,
    position: 'absolute'
  },
  slider: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    borderRadius: '28px',
    transition: 'background-color 0.25s ease, box-shadow 0.25s ease',
    border: '1px solid rgba(255, 255, 255, 0.15)'
  },
  thumb: {
    position: 'absolute',
    top: '3px',
    left: '4px',
    width: '20px',
    height: '20px',
    borderRadius: '50%',
    backgroundColor: '#ffffff',
    transition: 'transform 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
    boxShadow: '0 2px 5px rgba(0, 0, 0, 0.4)'
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
