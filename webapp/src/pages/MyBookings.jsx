import React, { useEffect, useState } from 'react';
import { db } from '../firebase';
import { collection, query, where, onSnapshot, doc, updateDoc, setDoc } from 'firebase/firestore';
import { Calendar, Phone, Clock, DollarSign, MessageSquare, AlertCircle, ShieldAlert, Mail } from 'lucide-react';

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
    
    // Try dd/MM/yyyy
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

export default function MyBookings({ user, initialFilter }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState(initialFilter || 'ALL');
  const [selectedBookingForReview, setSelectedBookingForReview] = useState(null);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);
  const [selectedBookingForCancel, setSelectedBookingForCancel] = useState(null);
  const [cancellingRequest, setCancellingRequest] = useState(false);

  const handleReviewSubmit = async () => {
    if (!user || !user.uid) return;
    if (!selectedBookingForReview) return;
    
    const c = selectedBookingForReview;
    if (c.clientId !== user.uid && c.userId !== user.uid) {
      alert("You are not authorized to review this consultation.");
      return;
    }
    if (c.status?.toUpperCase() !== 'COMPLETED') {
      alert("You can only review completed consultations.");
      return;
    }
    if (c.hasReviewed) {
      alert("You have already reviewed this consultation.");
      return;
    }
    if (rating < 1 || rating > 5) {
      alert("Please select a rating between 1 and 5 stars.");
      return;
    }
    
    setSubmittingReview(true);
    const reviewId = c.id;
    console.log(`REVIEW:`);
    console.log(`Current User ID = ${user.uid}`);
    console.log(`Consultation ID = ${c.id}`);
    console.log(`Lawyer ID = ${c.lawyerId}`);
    console.log(`Status = ${c.status}`);
    console.log(`Has Reviewed = ${c.hasReviewed || false}`);
    console.log(`Review Eligible = true`);
    
    try {
      const reviewDoc = {
        consultationId: c.id,
        userId: user.uid,
        clientId: user.uid, // backward compatibility
        lawyerId: c.lawyerId,
        userName: user.name || user.displayName || 'Client',
        clientName: user.name || user.displayName || 'Client', // backward compatibility
        rating: Number(rating),
        comment: comment.trim(),
        createdAt: new Date()
      };
      
      console.log(`REVIEW_SUBMISSION:`);
      console.log(`Submitting rating = ${rating}`);
      await setDoc(doc(db, 'reviews', reviewId), reviewDoc);
      
      const consultationRef = doc(db, 'consultations', c.id);
      await updateDoc(consultationRef, {
        hasReviewed: true,
        reviewId: reviewId,
        updatedAt: new Date()
      });
      
      console.log(`REVIEW_SUBMISSION: Firestore write successful`);
      alert("Review submitted successfully.");
      setSelectedBookingForReview(null);
      setComment('');
      setRating(5);
    } catch (e) {
      console.error(`REVIEW ERROR:`, e);
      alert("Error submitting review: " + e.message);
    } finally {
      setSubmittingReview(false);
    }
  };

  const handleCancelConfirm = async () => {
    if (!user || !user.uid) return;
    if (!selectedBookingForCancel) return;
    
    const c = selectedBookingForCancel;
    setCancellingRequest(true);
    try {
      const consultationRef = doc(db, 'consultations', c.id);
      await updateDoc(consultationRef, {
        status: 'CANCELLED',
        cancelledAt: new Date(),
        updatedAt: new Date()
      });
      
      // Update nested subcollections
      try {
        const clientConsultationRef = doc(db, 'users', c.clientId, 'consultations', c.id);
        await updateDoc(clientConsultationRef, {
          status: 'CANCELLED',
          cancelledAt: new Date(),
          updatedAt: new Date()
        });
      } catch (nestedErr) {
        console.warn("Failed to update client nested subcollection:", nestedErr);
      }
      
      try {
        const lawyerConsultationRef = doc(db, 'users', c.lawyerId, 'consultations', c.id);
        await updateDoc(lawyerConsultationRef, {
          status: 'CANCELLED',
          cancelledAt: new Date(),
          updatedAt: new Date()
        });
      } catch (nestedErr) {
        console.warn("Failed to update lawyer nested subcollection:", nestedErr);
      }
      
      setSelectedBookingForCancel(null);
    } catch (e) {
      console.error("CANCELLATION ERROR:", e);
      alert("Error cancelling consultation request: " + e.message);
    } finally {
      setCancellingRequest(false);
    }
  };

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
    
    const unsubscribe = onSnapshot(q, async (querySnapshot) => {
      const docs = [];
      querySnapshot.forEach(docSnapshot => {
        docs.push({ id: docSnapshot.id, ...docSnapshot.data() });
      });

      await checkAndExpireConsultations(docs);
      
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
    
    if (filter === 'TODAY') {
      if (status !== 'ACCEPTED') return false;
      const apptTime = getAppointmentDateTime(b);
      if (!apptTime) return false;
      const now = new Date();
      const todayMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      const apptMidnight = new Date(apptTime.getFullYear(), apptTime.getMonth(), apptTime.getDate());
      return apptMidnight.getTime() === todayMidnight.getTime() && apptTime > now;
    }
    
    if (filter === 'UPCOMING') {
      if (status !== 'ACCEPTED') return false;
      const apptTime = getAppointmentDateTime(b);
      if (!apptTime) return false;
      const now = new Date();
      const todayMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      const apptMidnight = new Date(apptTime.getFullYear(), apptTime.getMonth(), apptTime.getDate());
      return apptMidnight.getTime() > todayMidnight.getTime() && apptTime > now;
    }

    return status === filter;
  });

  const getStatusStyle = (status) => {
    switch (status?.toUpperCase()) {
      case 'ACCEPTED':
        return { background: 'rgba(16, 185, 129, 0.15)', color: 'var(--secondary)' };
      case 'REJECTED':
        return { background: 'rgba(239, 68, 68, 0.15)', color: 'var(--error)' };
      case 'EXPIRED':
        return { background: 'rgba(239, 68, 68, 0.15)', color: 'var(--error)' };
      case 'CANCELLED':
        return { background: 'rgba(255, 255, 255, 0.05)', color: 'var(--text-muted)' };
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
        {['ALL', 'PENDING', 'ACCEPTED', 'TODAY', 'UPCOMING', 'COMPLETED', 'REJECTED', 'EXPIRED'].map((t) => (
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

              {booking.status?.toUpperCase() === 'EXPIRED' && (
                <div style={{
                  padding: '12px',
                  background: 'rgba(239, 68, 68, 0.05)',
                  borderRadius: '10px',
                  border: '1px dashed var(--error)',
                  color: 'var(--error)',
                  fontSize: '13px',
                  fontWeight: '600',
                  marginTop: '8px'
                }}>
                  Your consultation request expired because the lawyer did not respond before the scheduled appointment time.
                </div>
              )}

              <div style={styles.detailsGrid}>
                {(booking.status?.toUpperCase() === 'ACCEPTED' || booking.status?.toUpperCase() === 'COMPLETED') && booking.bookingId && (
                  <div style={styles.detailItem}>
                    <Calendar size={16} color="var(--secondary)" />
                    <span>Booking ID: <strong>{booking.bookingId}</strong></span>
                  </div>
                )}
                <div style={styles.detailItem}>
                  <Clock size={16} color="var(--primary)" />
                  <span>Scheduled on: <strong>{booking.date}</strong> at <strong>{booking.time}</strong></span>
                </div>
                {(booking.status?.toUpperCase() === 'ACCEPTED' || booking.status?.toUpperCase() === 'COMPLETED') && (
                  <>
                    {(booking.userPhone || booking.contactNumber) && (
                      <div style={styles.detailItem}>
                        <Phone size={16} color="var(--primary)" />
                        <span>Callback contact: <strong>{booking.userPhone || booking.contactNumber || 'Not provided'}</strong></span>
                      </div>
                    )}
                    {booking.userEmail && (
                      <div style={styles.detailItem}>
                        <Mail size={16} color="var(--primary)" />
                        <span>Email: <strong>{booking.userEmail}</strong></span>
                      </div>
                    )}
                  </>
                )}
                <div style={styles.detailItem}>
                  <DollarSign size={16} color="var(--primary)" />
                  <span>Consultation Fee: <strong>₹{booking.fee || 500}</strong> ({booking.consultationType || 'Online'})</span>
                </div>
              </div>
              {booking.status?.toUpperCase() === 'COMPLETED' && (
                <div style={{ marginTop: '12px' }}>
                  {booking.hasReviewed ? (
                    <div style={{ color: 'var(--secondary)', fontWeight: 'bold', fontSize: '14px', textAlign: 'center', padding: '10px' }}>
                      ✓ Review Submitted
                    </div>
                  ) : (
                    <button
                      className="btn btn-primary"
                      style={{ width: '100%' }}
                      onClick={() => setSelectedBookingForReview(booking)}
                    >
                      ⭐ Rate Your Experience
                    </button>
                  )}
                </div>
              )}
              {booking.status?.toUpperCase() === 'PENDING' && (
                <div style={{ marginTop: '12px' }}>
                  <button
                    className="btn btn-secondary"
                    style={{ width: '100%', borderColor: 'var(--error)', color: 'var(--error)' }}
                    onClick={() => setSelectedBookingForCancel(booking)}
                  >
                    Cancel Request
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Modal Dialog for Rating and Review */}
      {selectedBookingForReview && (
        <div style={modalStyles.overlay}>
          <div className="glass-panel" style={modalStyles.modal}>
            <h3 style={{marginBottom: '12px'}}>Rate Your Consultation</h3>
            <p style={{fontSize: '13px', color: 'var(--text-muted)', marginBottom: '16px'}}>
              How was your session with <strong>Advocate {selectedBookingForReview.lawyerName}</strong>?
            </p>
            
            <div style={{display: 'flex', gap: '8px', justifyContent: 'center', marginBottom: '20px'}}>
              {[1, 2, 3, 4, 5].map((star) => (
                <span
                  key={star}
                  onClick={() => setRating(star)}
                  style={{
                    fontSize: star <= rating ? '32px' : '28px',
                    cursor: 'pointer',
                    color: star <= rating ? '#FFB300' : 'var(--border)',
                    transition: 'all 0.1s',
                    padding: '0 4px'
                  }}
                >
                  ★
                </span>
              ))}
            </div>
            
            <textarea
              placeholder="Write a comment about your experience (optional)..."
              className="input-field"
              rows="4"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              style={{width: '100%', resize: 'none', marginBottom: '20px', padding: '12px', background: 'rgba(0,0,0,0.2)'}}
            />
            
            <div style={{display: 'flex', gap: '12px'}}>
              <button
                className="btn"
                style={{flex: 1, background: 'rgba(255,255,255,0.05)', color: 'var(--text-main)'}}
                onClick={() => {
                  setSelectedBookingForReview(null);
                  setComment('');
                  setRating(5);
                }}
                disabled={submittingReview}
              >
                Cancel
              </button>
              <button
                className="btn btn-primary"
                style={{flex: 1}}
                onClick={handleReviewSubmit}
                disabled={submittingReview || rating < 1 || rating > 5}
              >
                {submittingReview ? 'Submitting...' : 'Submit Review'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Dialog for Cancel Request */}
      {selectedBookingForCancel && (
        <div style={modalStyles.overlay}>
          <div className="glass-panel" style={modalStyles.modal}>
            <h3 style={{marginBottom: '12px'}}>Cancel Consultation Request?</h3>
            <p style={{fontSize: '14px', color: 'var(--text-main)', marginBottom: '24px'}}>
              Are you sure you want to cancel this consultation request?
            </p>
            
            <div style={{display: 'flex', gap: '12px'}}>
              <button
                className="btn"
                style={{flex: 1, background: 'rgba(255,255,255,0.05)', color: 'var(--text-main)'}}
                onClick={() => setSelectedBookingForCancel(null)}
                disabled={cancellingRequest}
              >
                Keep Request
              </button>
              <button
                className="btn"
                style={{flex: 1, background: 'var(--error)', color: 'white'}}
                onClick={handleCancelConfirm}
                disabled={cancellingRequest}
              >
                {cancellingRequest ? 'Cancelling...' : 'Yes, Cancel Request'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const modalStyles = {
  overlay: {
    position: 'fixed',
    top: 0, left: 0, right: 0, bottom: 0,
    background: 'rgba(15, 23, 42, 0.85)',
    backdropFilter: 'blur(8px)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
    padding: '20px'
  },
  modal: {
    width: '100%',
    maxWidth: '450px',
    padding: '28px',
    display: 'flex',
    flexDirection: 'column',
    position: 'relative'
  }
};

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
