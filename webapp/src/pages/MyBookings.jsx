import React, { useEffect, useState } from 'react';
import { db } from '../firebase';
import { collection, query, where, onSnapshot } from 'firebase/firestore';
import { Calendar, Phone, Clock, DollarSign, MessageSquare, AlertCircle, ShieldAlert } from 'lucide-react';

export default function MyBookings({ user, initialFilter }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState(initialFilter || 'ALL');

  // Sync initialFilter prop if it changes
  useEffect(() => {
    if (initialFilter) {
      setFilter(initialFilter);
    }
  }, [initialFilter]);

  useEffect(() => {
    if (!user || !user.uid) return;
    setLoading(true);

    const consultationsRef = collection(db, 'consultations');
    const q = query(consultationsRef, where('clientId', '==', user.uid));
    
    const unsubscribe = onSnapshot(q, (querySnapshot) => {
      const docs = [];
      querySnapshot.forEach(docSnapshot => {
        docs.push({ id: docSnapshot.id, ...docSnapshot.data() });
      });
      
      // Sort locally by date/time descending to avoid composite index requirements
      docs.sort((a, b) => {
        const aTime = a.createdAt?.seconds || a.createdAt?.toMillis?.() / 1000 || 0;
        const bTime = b.createdAt?.seconds || b.createdAt?.toMillis?.() / 1000 || 0;
        return bTime - aTime;
      });

      setBookings(docs);
      setLoading(false);
    }, (error) => {
      console.error('Error listening to bookings', error);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [user]);

  const filteredBookings = bookings.filter(b => {
    if (filter === 'ALL') return true;
    const status = (b.status || 'PENDING').toUpperCase();
    return status === filter;
  });

  const getStatusStyle = (status) => {
    switch (status?.toUpperCase()) {
      case 'ACCEPTED':
        return { background: 'rgba(16, 185, 129, 0.15)', color: 'var(--secondary)' };
      case 'REJECTED':
        return { background: 'rgba(239, 68, 68, 0.15)', color: 'var(--error)' };
      case 'COMPLETED':
        return { background: 'rgba(99, 102, 241, 0.15)', color: 'var(--primary)' };
      default: // PENDING
        return { background: 'rgba(245, 158, 11, 0.15)', color: 'var(--accent)' };
    }
  };

  return (
    <div className="fade-in-up" style={styles.container}>
      <div style={styles.header}>
        <Calendar size={32} color="var(--primary)" />
        <h2>My Consultations</h2>
        <p style={styles.subtitle}>Track your appointments, scheduled times, callback numbers, and approval status details.</p>
      </div>

      {/* Filter Tabs */}
      <div style={styles.tabContainer}>
        {['ALL', 'PENDING', 'ACCEPTED', 'REJECTED'].map((t) => (
          <button
            key={t}
            onClick={() => setFilter(t)}
            style={{
              ...styles.tabBtn,
              background: filter === t ? 'var(--primary)' : 'rgba(255, 255, 255, 0.03)',
              color: filter === t ? 'white' : 'var(--text-muted)',
              borderColor: filter === t ? 'var(--primary)' : 'var(--border)'
            }}
          >
            {t === 'ALL' ? 'All Consultations' : t.charAt(0) + t.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {loading ? (
        <div style={styles.loader}>Loading consultations history...</div>
      ) : filteredBookings.length === 0 ? (
        <div className="glass-panel" style={styles.emptyCard}>
          <AlertCircle size={36} color="var(--border)" style={{marginBottom: '12px'}} />
          <h3>No Consultation Bookings</h3>
          <p>{filter === 'ALL' ? "You have not booked any consultations yet. Find an advocate to schedule your first legal review." : `No consultations found with status: ${filter.toLowerCase()}.`}</p>
        </div>
      ) : (
        <div style={styles.list}>
          {filteredBookings.map((booking) => (
            <div key={booking.id} className="glass-panel" style={styles.card}>
              <div style={styles.cardHeader}>
                <div>
                  <h3 style={styles.lawyerName}>Advocate {booking.lawyerName}</h3>
                  <div style={styles.caseTitle}>Issue: {booking.caseTitle}</div>
                </div>
                <span style={{...styles.statusLabel, ...getStatusStyle(booking.status)}}>
                  {booking.status || 'PENDING'}
                </span>
              </div>
              
              <div style={styles.divider} />
              
              <p style={styles.description}>{booking.caseDescription}</p>

              <div style={styles.detailsGrid}>
                <div style={styles.detailItem}>
                  <Clock size={16} color="var(--primary)" />
                  <span>Scheduled on: <strong>{booking.date}</strong> at <strong>{booking.time}</strong></span>
                </div>
                <div style={styles.detailItem}>
                  <Phone size={16} color="var(--primary)" />
                  <span>Callback contact: <strong>{booking.contactNumber || 'Not provided'}</strong></span>
                </div>
                <div style={styles.detailItem}>
                  <DollarSign size={16} color="var(--primary)" />
                  <span>Consultation Fee: <strong>₹{booking.fee || 500}</strong> ({booking.consultationType || 'Online'})</span>
                </div>
              </div>
            </div>
          ))}
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
    color: 'var(--text-muted)'
  },
  loader: {
    textAlign: 'center',
    padding: '40px',
    color: 'var(--text-muted)'
  },
  emptyCard: {
    padding: '48px 24px',
    textAlign: 'center',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    color: 'var(--text-muted)'
  },
  list: {
    display: 'flex',
    flexDirection: 'column',
    gap: '20px'
  },
  card: {
    padding: '24px',
    display: 'flex',
    flexDirection: 'column',
    gap: '12px'
  },
  cardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start'
  },
  lawyerName: {
    fontSize: '18px',
    fontWeight: '800'
  },
  caseTitle: {
    fontSize: '14px',
    fontWeight: '600',
    color: 'var(--primary)',
    marginTop: '4px'
  },
  statusLabel: {
    fontSize: '10px',
    fontWeight: '900',
    padding: '6px 12px',
    borderRadius: '8px',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  divider: {
    height: '1px',
    background: 'var(--border)'
  },
  description: {
    fontSize: '14px',
    color: 'var(--text-muted)',
    lineHeight: '1.6'
  },
  detailsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
    gap: '12px',
    padding: '16px',
    background: 'rgba(255,255,255,0.01)',
    borderRadius: '12px',
    border: '1px solid var(--border)',
    marginTop: '4px'
  },
  detailItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    fontSize: '13px',
    color: 'var(--text-main)'
  },
  tabContainer: {
    display: 'flex',
    gap: '10px',
    justifyContent: 'center',
    flexWrap: 'wrap',
    marginBottom: '8px'
  },
  tabBtn: {
    padding: '8px 16px',
    borderRadius: '10px',
    border: '1px solid',
    fontSize: '13px',
    fontWeight: '700',
    cursor: 'pointer',
    transition: 'all 0.2s'
  }
};
