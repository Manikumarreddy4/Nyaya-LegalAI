import React, { useState, useEffect } from 'react';
import { db } from '../firebase';
import { doc, onSnapshot, collection, query, where } from 'firebase/firestore';
import { 
  ArrowLeft, 
  MapPin, 
  Award, 
  Clock, 
  DollarSign, 
  CheckCircle, 
  ShieldAlert, 
  FileText, 
  Phone, 
  MessageSquare, 
  Video, 
  Building, 
  Calendar, 
  Star, 
  ShieldCheck 
} from 'lucide-react';

export default function LawyerProfile({ user, lawyerId, onNavigate }) {
  const [lawyer, setLawyer] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [reviewsLoading, setReviewsLoading] = useState(true);
  const [error, setError] = useState(null);

  // 1. Fetch Lawyer Document in Real-time
  useEffect(() => {
    if (!lawyerId) {
      setError('No advocate ID specified.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    const unsubscribe = onSnapshot(doc(db, 'lawyers', lawyerId), (docSnapshot) => {
      if (docSnapshot.exists()) {
        const data = docSnapshot.data();
        // Resolve name
        const resolvedName = (data.name || data.fullName || data.displayName || '').trim();
        setLawyer({ ...data, name: resolvedName || 'Advocate' });
        setError(null);
      } else {
        setError('Advocate profile not found.');
      }
      setLoading(false);
    }, (err) => {
      console.error("LAWYER_PROFILE ERROR: Failed to load advocate details:", err);
      setError('Failed to load advocate details. Please check your network and try again.');
      setLoading(false);
    });

    return () => unsubscribe();
  }, [lawyerId]);

  // 2. Fetch Client Reviews in Real-time
  useEffect(() => {
    if (!lawyerId) return;

    setReviewsLoading(true);
    const q = query(collection(db, 'reviews'), where('lawyerId', '==', lawyerId));

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const list = [];
      snapshot.forEach((docSnap) => {
        list.push({ id: docSnap.id, ...docSnap.data() });
      });

      // Sort locally by date descending
      list.sort((a, b) => {
        const aTime = a.createdAt?.seconds || a.createdAt?.toMillis?.() / 1000 || 0;
        const bTime = b.createdAt?.seconds || b.createdAt?.toMillis?.() / 1000 || 0;
        return bTime - aTime;
      });

      setReviews(list);
      setReviewsLoading(false);
    }, (err) => {
      console.error("LAWYER_PROFILE ERROR: Failed to load reviews:", err);
      setReviewsLoading(false);
    });

    return () => unsubscribe();
  }, [lawyerId]);

  const handleBookClick = () => {
    if (!lawyer) return;
    // Redirect to FindLawyer page and pre-select this advocate in booking mode
    onNavigate('find-lawyer', { 
      lawyer: { ...lawyer, id: lawyerId }, 
      startBooking: true 
    });
  };

  if (loading) {
    return (
      <div style={styles.loaderContainer}>
        <div style={styles.loaderSpinner}></div>
        <p style={{ marginTop: '16px', color: 'var(--text-muted)' }}>Loading advocate profile details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.errorContainer}>
        <ShieldAlert size={48} color="var(--error)" />
        <h3 style={{ marginTop: '16px', marginBottom: '8px' }}>Error</h3>
        <p style={{ color: 'var(--text-muted)', marginBottom: '24px' }}>{error}</p>
        <button className="btn btn-primary" onClick={() => onNavigate('find-lawyer')}>
          Back to Directory
        </button>
      </div>
    );
  }

  // Determine availability states
  const isOnlineAvailable = lawyer && (
    (lawyer.availability_status !== undefined ? lawyer.availability_status === true : (lawyer.isAvailable !== false && lawyer.onlineAvailable !== false)) &&
    (lawyer.video_consultation_available !== undefined ? lawyer.video_consultation_available === true : lawyer.onlineAvailable !== false)
  );
  const isInPersonAvailable = lawyer && (
    (lawyer.availability_status !== undefined ? lawyer.availability_status === true : (lawyer.isAvailable !== false && lawyer.onlineAvailable !== false)) &&
    (lawyer.in_person_consultation_available !== undefined ? lawyer.in_person_consultation_available === true : lawyer.isInPersonAvailable !== false)
  );

  return (
    <div className="fade-in-up" style={styles.container}>
      {/* Back Button */}
      <button style={styles.backBtn} onClick={() => onNavigate('find-lawyer', { selectedLawyer: lawyer })}>
        <ArrowLeft size={16} /> Back to Directory
      </button>

      <h1 style={styles.pageTitle}>Lawyer Profile</h1>

      {/* Suggested layout adapted professionally for desktop grid */}
      <div style={styles.mainLayout}>
        {/* TOP PROFILE CARD */}
        <div className="glass-panel" style={styles.profileSummaryCard}>
          <div style={styles.summaryLeft}>
            <div style={styles.avatarWrapper}>
              {lawyer.profilePhotoUrl || lawyer.profileImage ? (
                <img 
                  src={lawyer.profilePhotoUrl || lawyer.profileImage} 
                  alt={lawyer.name} 
                  style={styles.avatarImg} 
                />
              ) : (
                <div style={styles.avatarTextPlaceholder}>
                  {lawyer.name.charAt(0).toUpperCase()}
                </div>
              )}
            </div>
            <div style={styles.summaryMeta}>
              <div style={styles.nameRow}>
                <h2 style={styles.lawyerName}>{lawyer.name}</h2>
                {lawyer.verificationStatus === 'VERIFIED' && (
                  <div style={styles.verifiedBadge}>
                    <ShieldCheck size={14} color="var(--secondary)" />
                    <span>Verified Advocate</span>
                  </div>
                )}
              </div>
              <div style={styles.specializationTag}>{lawyer.specialization || 'General Practice'}</div>
              {lawyer.bio && <p style={styles.bioText}>{lawyer.bio}</p>}
            </div>
          </div>

          <div style={styles.summaryRight}>
            <div style={styles.statBox}>
              <div style={styles.statIconWrapper}><Star size={16} color="#FFB300" /></div>
              <div>
                <div style={styles.statLabel}>Rating</div>
                <div style={styles.statValue}>★ {Number(lawyer.rating || 5.0).toFixed(1)}</div>
              </div>
            </div>
            <div style={styles.statBox}>
              <div style={styles.statIconWrapper}><Award size={16} color="var(--primary)" /></div>
              <div>
                <div style={styles.statLabel}>Experience</div>
                <div style={styles.statValue}>{lawyer.experience || '0 Years'}</div>
              </div>
            </div>
            <div style={styles.statBox}>
              <div style={styles.statIconWrapper}><Calendar size={16} color="var(--secondary)" /></div>
              <div>
                <div style={styles.statLabel}>Consultations</div>
                <div style={styles.statValue}>{lawyer.consultationCount || 0} Complete</div>
              </div>
            </div>
          </div>
        </div>

        {/* TWO COLUMN ROW FOR CREDENTIALS AND OFFICE */}
        <div style={styles.twoColumnRow}>
          {/* BAR COUNCIL & CREDENTIALS */}
          <div className="glass-panel" style={styles.infoCard}>
            <h3 style={styles.cardHeading}>Bar Council & Credentials</h3>
            <div style={styles.cardList}>
              {(lawyer.enrollmentNumber || lawyer.barCouncilNumber) && (
                <div style={styles.cardListItem}>
                  <strong style={styles.listItemLabel}>Enrollment ID:</strong>
                  <span style={styles.listItemValue}>{lawyer.enrollmentNumber || lawyer.barCouncilNumber}</span>
                </div>
              )}
              {(lawyer.stateBarCouncil || lawyer.barCouncil) && (
                <div style={styles.cardListItem}>
                  <strong style={styles.listItemLabel}>State Bar Council:</strong>
                  <span style={styles.listItemValue}>{lawyer.stateBarCouncil || lawyer.barCouncil}</span>
                </div>
              )}
              {lawyer.qualification && (
                <div style={styles.cardListItem}>
                  <strong style={styles.listItemLabel}>Qualification:</strong>
                  <span style={styles.listItemValue}>{lawyer.qualification}</span>
                </div>
              )}
              {lawyer.university && (
                <div style={styles.cardListItem}>
                  <strong style={styles.listItemLabel}>University:</strong>
                  <span style={styles.listItemValue}>{lawyer.university}</span>
                </div>
              )}
              {!(lawyer.enrollmentNumber || lawyer.barCouncilNumber) && !(lawyer.stateBarCouncil || lawyer.barCouncil) && !lawyer.qualification && (
                <p style={{ fontStyle: 'italic', color: 'var(--text-muted)', fontSize: '13px' }}>Credentials details not configured.</p>
              )}
            </div>
          </div>

          {/* OFFICE LOCATION & LANGUAGES */}
          <div className="glass-panel" style={styles.infoCard}>
            <h3 style={styles.cardHeading}>Office Location & Languages</h3>
            <div style={styles.cardList}>
              <div style={styles.cardListItem}>
                <strong style={styles.listItemLabel}>Current City:</strong>
                <span style={styles.listItemValue}>{lawyer.city || lawyer.location || 'Not Specified'}</span>
              </div>
              {lawyer.officeAddress && (
                <div style={styles.cardListItem}>
                  <strong style={styles.listItemLabel}>Office Address:</strong>
                  <span style={styles.listItemValue}>{lawyer.officeAddress}</span>
                </div>
              )}
              <div style={styles.cardListItem}>
                <strong style={styles.listItemLabel}>Languages Known:</strong>
                <span style={styles.listItemValue}>{lawyer.languages || 'English'}</span>
              </div>
              {lawyer.phone && (
                <div style={styles.cardListItem}>
                  <strong style={styles.listItemLabel}>Office Phone:</strong>
                  <span style={styles.listItemValue}>{lawyer.phone}</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* CONSULTATION & AVAILABILITY */}
        <div className="glass-panel" style={styles.availabilitySection}>
          <h3 style={styles.cardHeading}>Consultation & Availability Details</h3>
          <p style={styles.operatingHoursText}>
            General operating hours: <strong>{lawyer.availableDays || 'Mon - Sat'}</strong>, <strong>{lawyer.availableTime || '09:00 AM - 06:00 PM'}</strong>
          </p>

          <div style={styles.availabilityCardsGrid}>
            {/* ONLINE CONSULTATION CARD */}
            <div style={{
              ...styles.availabilityCard,
              border: `1px solid ${isOnlineAvailable ? 'rgba(16, 185, 129, 0.25)' : 'rgba(255, 255, 255, 0.05)'}`,
              background: isOnlineAvailable ? 'rgba(16, 185, 129, 0.03)' : 'rgba(255, 255, 255, 0.01)'
            }}>
              <div style={styles.availCardHeader}>
                <Video size={20} color={isOnlineAvailable ? 'var(--secondary)' : 'var(--text-muted)'} />
                <h4 style={{ fontSize: '15px', fontWeight: '700' }}>Online Consultation</h4>
              </div>
              <p style={styles.availCardDesc}>Available for video calls and online callback sessions.</p>
              <div style={styles.statusPillRow}>
                <span>Status:</span>
                <span style={{
                  ...styles.statusPill,
                  backgroundColor: isOnlineAvailable ? 'rgba(16, 185, 129, 0.15)' : 'rgba(255, 255, 255, 0.06)',
                  color: isOnlineAvailable ? 'var(--secondary)' : 'var(--text-muted)',
                }}>
                  {isOnlineAvailable ? '✓ ON' : '✕ OFF'}
                </span>
              </div>
            </div>

            {/* IN-PERSON CONSULTATION CARD */}
            <div style={{
              ...styles.availabilityCard,
              border: `1px solid ${isInPersonAvailable ? 'rgba(16, 185, 129, 0.25)' : 'rgba(255, 255, 255, 0.05)'}`,
              background: isInPersonAvailable ? 'rgba(16, 185, 129, 0.03)' : 'rgba(255, 255, 255, 0.01)'
            }}>
              <div style={styles.availCardHeader}>
                <Building size={20} color={isInPersonAvailable ? 'var(--secondary)' : 'var(--text-muted)'} />
                <h4 style={{ fontSize: '15px', fontWeight: '700' }}>In-Person Consultation</h4>
              </div>
              <p style={styles.availCardDesc}>Available for offline face-to-face meetings at office chambers.</p>
              <div style={styles.statusPillRow}>
                <span>Status:</span>
                <span style={{
                  ...styles.statusPill,
                  backgroundColor: isInPersonAvailable ? 'rgba(16, 185, 129, 0.15)' : 'rgba(255, 255, 255, 0.06)',
                  color: isInPersonAvailable ? 'var(--secondary)' : 'var(--text-muted)',
                }}>
                  {isInPersonAvailable ? '✓ ON' : '✕ OFF'}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* CLIENT REVIEWS */}
        <div className="glass-panel" style={styles.reviewsSection}>
          <h3 style={styles.cardHeading}>★ Client Reviews</h3>
          {reviewsLoading ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>Loading reviews...</p>
          ) : reviews.length === 0 ? (
            <div style={styles.noReviewsBox}>
              <p style={{ fontStyle: 'italic', color: 'var(--text-muted)', margin: 0 }}>No client reviews yet.</p>
            </div>
          ) : (
            <div style={styles.reviewsList}>
              {reviews.map((rev) => (
                <div key={rev.id} style={styles.reviewItem}>
                  <div style={styles.reviewHeader}>
                    <div>
                      <span style={styles.reviewerName}>{rev.clientName || rev.userName || 'Verified Client'}</span>
                      <span style={styles.reviewDate}>
                        {rev.createdAt ? new Date(rev.createdAt.seconds ? rev.createdAt.seconds * 1000 : rev.createdAt).toLocaleDateString() : ''}
                      </span>
                    </div>
                    <div style={styles.reviewRatingStars}>
                      {[1, 2, 3, 4, 5].map((star) => (
                        <span 
                          key={star} 
                          style={{ 
                            color: star <= Number(rev.rating || 5) ? '#FFB300' : 'rgba(255,255,255,0.08)',
                            fontSize: '14px',
                            marginRight: '2px'
                          }}
                        >
                          ★
                        </span>
                      ))}
                    </div>
                  </div>
                  <p style={styles.reviewComment}>{rev.comment || 'No comment provided.'}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* BOOKING BAR STICKY ACTION BAR */}
      <div className="glass-panel" style={styles.bookingActionBar}>
        <div style={styles.feeInfoCol}>
          <span style={styles.feeLabel}>Consultation Fee</span>
          <span style={styles.feeAmount}>₹{lawyer.consultationFee || 500}</span>
        </div>
        <button 
          className="btn btn-primary"
          style={{
            ...styles.bookSubmitBtn,
            background: (!isOnlineAvailable && !isInPersonAvailable) ? 'var(--border)' : 'linear-gradient(135deg, var(--primary), var(--tertiary))',
            cursor: (!isOnlineAvailable && !isInPersonAvailable) ? 'not-allowed' : 'pointer'
          }}
          disabled={!isOnlineAvailable && !isInPersonAvailable}
          onClick={handleBookClick}
        >
          {(!isOnlineAvailable && !isInPersonAvailable) ? 'Advocate Unavailable' : 'Book Consultation'}
        </button>
      </div>
    </div>
  );
}

const styles = {
  container: {
    padding: '24px',
    maxWidth: '900px',
    margin: '0 auto',
    paddingBottom: '120px' // spacing so it doesn't get hidden behind the sticky booking bar
  },
  backBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    background: 'none',
    border: 'none',
    color: 'var(--primary)',
    fontWeight: '700',
    cursor: 'pointer',
    fontSize: '14px',
    marginBottom: '20px',
    alignSelf: 'flex-start',
    padding: '4px 0',
    transition: 'transform 0.2s',
    ':hover': {
      transform: 'translateX(-4px)'
    }
  },
  pageTitle: {
    fontSize: '28px',
    fontWeight: '900',
    marginBottom: '24px'
  },
  loaderContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '400px'
  },
  loaderSpinner: {
    width: '40px',
    height: '40px',
    borderRadius: '50%',
    border: '3px solid rgba(99, 102, 241, 0.1)',
    borderTopColor: 'var(--primary)',
    animation: 'spin 1s linear infinite'
  },
  errorContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    textAlign: 'center',
    minHeight: '400px',
    padding: '24px'
  },
  mainLayout: {
    display: 'flex',
    flexDirection: 'column',
    gap: '20px'
  },
  profileSummaryCard: {
    padding: '24px',
    display: 'flex',
    justifyContent: 'space-between',
    flexWrap: 'wrap',
    gap: '24px',
    background: 'linear-gradient(135deg, rgba(30, 41, 59, 0.8), rgba(99, 102, 241, 0.05))',
    border: '1px solid rgba(99, 102, 241, 0.15)'
  },
  summaryLeft: {
    display: 'flex',
    gap: '20px',
    flex: '1 1 500px'
  },
  avatarWrapper: {
    width: '90px',
    height: '90px',
    borderRadius: '50%',
    overflow: 'hidden',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'linear-gradient(135deg, var(--primary), var(--tertiary))',
    flexShrink: 0,
    boxShadow: '0 4px 12px rgba(99, 102, 241, 0.3)'
  },
  avatarImg: {
    width: '100%',
    height: '100%',
    objectFit: 'cover'
  },
  avatarTextPlaceholder: {
    color: '#ffffff',
    fontWeight: '900',
    fontSize: '36px'
  },
  summaryMeta: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px',
    flex: 1
  },
  nameRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    flexWrap: 'wrap'
  },
  lawyerName: {
    fontSize: '22px',
    fontWeight: '900'
  },
  verifiedBadge: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '6px',
    padding: '4px 10px',
    borderRadius: '8px',
    background: 'rgba(16, 185, 129, 0.15)',
    color: 'var(--secondary)',
    fontSize: '11px',
    fontWeight: '700',
    border: '1px solid rgba(16, 185, 129, 0.25)'
  },
  pendingBadge: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: '6px',
    padding: '4px 10px',
    borderRadius: '8px',
    background: 'rgba(245, 158, 11, 0.15)',
    color: 'var(--accent)',
    fontSize: '11px',
    fontWeight: '700',
    border: '1px solid rgba(245, 158, 11, 0.25)'
  },
  specializationTag: {
    color: 'var(--secondary)',
    fontSize: '13px',
    fontWeight: '800',
    textTransform: 'uppercase',
    letterSpacing: '0.8px',
    marginTop: '2px'
  },
  bioText: {
    fontSize: '13.5px',
    lineHeight: '1.6',
    color: 'var(--text-muted)',
    marginTop: '8px',
    background: 'rgba(0,0,0,0.1)',
    padding: '10px 14px',
    borderRadius: '10px',
    borderLeft: '3px solid var(--primary)'
  },
  summaryRight: {
    display: 'flex',
    flexDirection: 'column',
    gap: '10px',
    justifyContent: 'center',
    flex: '1 1 200px',
    minWidth: '200px'
  },
  statBox: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '8px 16px',
    borderRadius: '12px',
    background: 'rgba(255,255,255,0.02)',
    border: '1px solid var(--border)'
  },
  statIconWrapper: {
    width: '32px',
    height: '32px',
    borderRadius: '8px',
    background: 'rgba(255,255,255,0.04)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  },
  statLabel: {
    fontSize: '10px',
    textTransform: 'uppercase',
    color: 'var(--text-muted)',
    letterSpacing: '0.5px'
  },
  statValue: {
    fontSize: '14px',
    fontWeight: '800'
  },
  twoColumnRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
    gap: '20px'
  },
  infoCard: {
    padding: '24px'
  },
  cardHeading: {
    fontSize: '16px',
    fontWeight: '800',
    color: 'var(--primary)',
    marginBottom: '16px',
    borderBottom: '1px solid var(--border)',
    paddingBottom: '8px'
  },
  cardList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px'
  },
  cardListItem: {
    display: 'flex',
    flexDirection: 'column',
    gap: '2px',
    fontSize: '13.5px'
  },
  listItemLabel: {
    fontSize: '11px',
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  listItemValue: {
    color: 'var(--text-main)',
    fontWeight: '600'
  },
  availabilitySection: {
    padding: '24px'
  },
  operatingHoursText: {
    fontSize: '13px',
    color: 'var(--text-muted)',
    marginBottom: '16px'
  },
  availabilityCardsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
    gap: '16px'
  },
  availabilityCard: {
    padding: '20px',
    borderRadius: '16px',
    display: 'flex',
    flexDirection: 'column',
    gap: '10px'
  },
  availCardHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px'
  },
  availCardDesc: {
    fontSize: '12.5px',
    color: 'var(--text-muted)',
    lineHeight: '1.5'
  },
  statusPillRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    fontSize: '13px',
    marginTop: '6px'
  },
  statusPill: {
    padding: '4px 12px',
    borderRadius: '8px',
    fontSize: '11px',
    fontWeight: '800',
    letterSpacing: '0.5px'
  },
  reviewsSection: {
    padding: '24px'
  },
  noReviewsBox: {
    padding: '16px',
    textAlign: 'center',
    background: 'rgba(255,255,255,0.01)',
    borderRadius: '12px',
    border: '1px dashed var(--border)'
  },
  reviewsList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '14px'
  },
  reviewItem: {
    padding: '16px',
    background: 'rgba(255, 255, 255, 0.02)',
    borderRadius: '14px',
    border: '1px solid var(--border)'
  },
  reviewHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
    flexWrap: 'wrap',
    gap: '8px'
  },
  reviewerName: {
    fontWeight: '800',
    fontSize: '14px',
    marginRight: '12px'
  },
  reviewDate: {
    fontSize: '11px',
    color: 'var(--text-muted)'
  },
  reviewRatingStars: {
    display: 'flex',
    alignItems: 'center'
  },
  reviewComment: {
    fontSize: '13px',
    lineHeight: '1.6',
    color: 'var(--text-muted)',
    margin: 0
  },
  bookingActionBar: {
    position: 'fixed',
    bottom: '24px',
    left: '50%',
    transform: 'translateX(-50%)',
    width: 'calc(100% - 48px)',
    maxWidth: '900px',
    padding: '16px 28px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    zIndex: 200,
    border: '1px solid rgba(255, 255, 255, 0.12)',
    background: 'rgba(30, 41, 59, 0.95)',
    boxShadow: '0 -8px 24px rgba(0,0,0,0.5), 0 8px 16px rgba(0,0,0,0.4)',
    borderRadius: '24px'
  },
  feeInfoCol: {
    display: 'flex',
    flexDirection: 'column',
    gap: '2px'
  },
  feeLabel: {
    fontSize: '11px',
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.8px'
  },
  feeAmount: {
    fontSize: '24px',
    fontWeight: '900',
    color: 'var(--text-main)'
  },
  bookSubmitBtn: {
    padding: '12px 32px',
    borderRadius: '14px',
    fontSize: '15px',
    fontWeight: '800',
    height: '46px'
  }
};
