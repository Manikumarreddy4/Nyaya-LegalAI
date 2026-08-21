import React, { useState, useEffect } from 'react';
import { db } from '../firebase';
import { collection, getDocs, doc, setDoc, query, where, onSnapshot } from 'firebase/firestore';
import { Search, MapPin, Award, Clock, DollarSign, ArrowLeft, Calendar, FileText, Phone, CheckCircle, ShieldAlert } from 'lucide-react';

export default function FindLawyer({ user, onNavigate }) {
  const [lawyers, setLawyers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  
  // Selection States
  const [selectedLawyer, setSelectedLawyer] = useState(null);
  const [bookingMode, setBookingMode] = useState(false);
  const [bookingSuccess, setBookingSuccess] = useState(false);

  // Booking Form Fields
  const [caseTitle, setCaseTitle] = useState('');
  const [caseDescription, setCaseDescription] = useState('');
  const [contactNumber, setContactNumber] = useState('');
  const [consultationType, setConsultationType] = useState('Online');
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');
  const [bookingLoading, setBookingLoading] = useState(false);

  // Load Lawyers from Firestore in real-time
  useEffect(() => {
    const q = collection(db, 'lawyers');
    const unsubscribe = onSnapshot(q, (querySnapshot) => {
      const list = [];
      console.log('FIND_LAWYER: Collection name: lawyers');
      console.log(`FIND_LAWYER: Number of documents loaded: ${querySnapshot.size}`);
      
      querySnapshot.forEach(docSnapshot => {
        const data = docSnapshot.data();
        const roleLower = (data.role || '').toLowerCase().trim();
        const isLawyer = roleLower === 'lawyer' || roleLower === 'advocate';
        const resolvedName = (data.name || data.fullName || data.displayName || '').trim();
        
        if (isLawyer && resolvedName && resolvedName.toLowerCase() !== 'advocate') {
          console.log(`FIND_LAWYER: Loaded lawyer: uid=${docSnapshot.id}, name=${resolvedName}, role=${data.role}, specialization=${data.specialization || 'General'}, location=${data.location || data.city || 'Not specified'}`);
          list.push({ id: docSnapshot.id, ...data, name: resolvedName });
        } else {
          console.warn(`FIND_LAWYER: Skipping non-lawyer or incomplete profile: uid=${docSnapshot.id}, name=${data.name || 'N/A'}, role=${data.role || 'N/A'}`);
        }
      });
      setLawyers(list);
      setLoading(false);
    }, (error) => {
      console.error('Error listening to lawyers', error);
      setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  const handleBookingSubmit = async (e) => {
    e.preventDefault();
    if (!user || !user.uid) {
      alert('You must be logged in to book a consultation.');
      return;
    }
    if (!caseTitle || !caseDescription || !contactNumber || !date || !time) {
      alert('Please fill in all booking fields.');
      return;
    }

    setBookingLoading(true);
    try {
      const consultationId = 'consult_' + Date.now();
      const newBooking = {
        consultationId,
        clientId: user.uid,
        userId: user.uid, // compatible field
        lawyerId: selectedLawyer.lawyerId || selectedLawyer.id,
        clientName: user.name || 'Client',
        userName: user.name || 'Client', // compatible field
        lawyerName: selectedLawyer.name,
        caseTitle: caseTitle.trim(),
        caseDescription: caseDescription.trim(),
        consultationType,
        date,
        time,
        contactNumber: contactNumber.trim(),
        status: 'PENDING',
        fee: parseFloat(selectedLawyer.consultationFee) || 500,
        createdAt: new Date()
      };

      // Save consultation under /consultations/{id}
      await setDoc(doc(db, 'consultations', consultationId), newBooking);
      
      setBookingSuccess(true);
      setCaseTitle('');
      setCaseDescription('');
      setContactNumber('');
      setDate('');
      setTime('');
    } catch (err) {
      console.error(err);
      alert('Failed to book consultation: ' + err.message);
    } finally {
      setBookingLoading(false);
    }
  };

  const filteredLawyers = lawyers.filter(l => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return l.name.toLowerCase().includes(q) || 
           (l.specialization || '').toLowerCase().includes(q) ||
           (l.location || '').toLowerCase().includes(q);
  });

  return (
    <div className="fade-in-up" style={styles.container}>
      {/* 1. LISTING MODE */}
      {!selectedLawyer && (
        <>
          <div style={styles.header}>
            <h2>Find Legal Counsel</h2>
            <p style={styles.subtitle}>Browse through our vetted panel of advocates specializing across various fields of Indian Law.</p>
          </div>

          {/* Search bar */}
          <div className="glass-panel" style={styles.searchBar}>
            <Search size={18} color="var(--primary)" style={styles.searchIcon} />
            <input 
              type="text" 
              placeholder="Search by name, specialization, or location..." 
              className="input-field"
              style={styles.searchInput}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          {loading ? (
            <div style={styles.loader}>Loading advocates directory...</div>
          ) : filteredLawyers.length === 0 ? (
            <div style={styles.emptyText}>No lawyers found matching your query. Please expand your search criteria.</div>
          ) : (
            <div style={styles.grid}>
              {filteredLawyers.map((lawyer) => (
                <div 
                  key={lawyer.id} 
                  className="glass-panel" 
                  style={styles.lawyerCard}
                  onClick={() => setSelectedLawyer(lawyer)}
                >
                  <h3 style={styles.lawyerName}>{lawyer.name}</h3>
                  
                  <div style={styles.metaRow}>
                    <Award size={14} color="var(--primary)" />
                    <span style={styles.metaText}>{lawyer.specialization || 'General Practice'}</span>
                  </div>

                  <div style={styles.metaRow}>
                    <Clock size={14} color="var(--primary)" />
                    <span style={styles.metaText}>{lawyer.experience || '0 Years'} Experience</span>
                  </div>

                  <div style={styles.metaRow}>
                    <MapPin size={14} color="var(--primary)" />
                    <span style={styles.metaText}>{lawyer.location || 'Location not specified'}</span>
                  </div>

                  <div style={styles.metaRow}>
                    <DollarSign size={14} color="var(--primary)" />
                    <span style={styles.metaText}>₹{lawyer.consultationFee || 500} consultation fee</span>
                  </div>

                  {lawyer.onlineAvailable === false && (
                    <div style={{ ...styles.metaRow, color: 'var(--error)', fontWeight: 'bold' }}>
                      <ShieldAlert size={14} color="var(--error)" />
                      <span>Currently Unavailable</span>
                    </div>
                  )}

                  <button 
                    className="btn btn-primary" 
                    style={{
                      ...styles.bookBtn,
                      background: lawyer.onlineAvailable === false ? 'var(--border)' : 'var(--primary)',
                      cursor: lawyer.onlineAvailable === false ? 'not-allowed' : 'pointer'
                    }} 
                    disabled={lawyer.onlineAvailable === false}
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedLawyer(lawyer);
                      setBookingMode(true);
                    }}
                  >
                    {lawyer.onlineAvailable === false ? 'Unavailable' : 'Consult Advocate'}
                  </button>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* 2. DETAIL/BOOKING MODE */}
      {selectedLawyer && (
        <div style={styles.detailsView}>
          <button 
            style={styles.backLink} 
            onClick={() => { 
              setSelectedLawyer(null); 
              setBookingMode(false); 
              setBookingSuccess(false); 
            }}
          >
            <ArrowLeft size={16} /> Back to Directory
          </button>

          {!bookingMode ? (
            /* Lawyer Profile Details view */
            <div className="glass-panel" style={styles.detailsCard}>
              <h2 style={styles.detailsName}>{selectedLawyer.name}</h2>
              <div style={styles.detailsTag}>{selectedLawyer.specialization || 'General Practice'}</div>
              <div style={styles.divider} />
              
              <div style={styles.infoSummaryGrid}>
                <div style={styles.infoBox}>
                  <Clock size={18} color="var(--primary)" />
                  <div>
                    <div style={styles.infoLabel}>Experience</div>
                    <div style={styles.infoVal}>{selectedLawyer.experience || '0 Years'}</div>
                  </div>
                </div>
                <div style={styles.infoBox}>
                  <MapPin size={18} color="var(--primary)" />
                  <div>
                    <div style={styles.infoLabel}>City / Location</div>
                    <div style={styles.infoVal}>{selectedLawyer.location || 'Not Specified'}</div>
                  </div>
                </div>
                <div style={styles.infoBox}>
                  <DollarSign size={18} color="var(--primary)" />
                  <div>
                    <div style={styles.infoLabel}>Consultation Fee</div>
                    <div style={styles.infoVal}>₹{selectedLawyer.consultationFee || 500}</div>
                  </div>
                </div>
              </div>

              {selectedLawyer.bio && (
                <div style={styles.bioSection}>
                  <h4 style={styles.bioTitle}>About the Advocate</h4>
                  <p style={styles.bioText}>{selectedLawyer.bio}</p>
                </div>
              )}

              {selectedLawyer.onlineAvailable === false ? (
                <div style={{ color: 'var(--error)', fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '8px', padding: '12px', background: 'rgba(239, 68, 68, 0.05)', borderRadius: '10px', border: '1px dashed var(--error)' }}>
                  <ShieldAlert size={16} color="var(--error)" />
                  <span>Currently Unavailable for consultations. Please check back later.</span>
                </div>
              ) : (
                <div style={styles.barSection}>
                  <strong>Bar Council Enrollment ID:</strong> {selectedLawyer.barCouncilNumber || 'Under verification review'}
                </div>
              )}

              <button 
                className="btn btn-primary" 
                style={{
                  ...styles.actionConsultBtn,
                  background: selectedLawyer.onlineAvailable === false ? 'var(--border)' : 'var(--primary)',
                  cursor: selectedLawyer.onlineAvailable === false ? 'not-allowed' : 'pointer'
                }} 
                disabled={selectedLawyer.onlineAvailable === false}
                onClick={() => setBookingMode(true)}
              >
                {selectedLawyer.onlineAvailable === false ? 'Currently Unavailable' : 'Schedule a Consultation'}
              </button>
            </div>
          ) : (
            /* Appointment Booking Form view */
            <div className="glass-panel" style={styles.bookingCard}>
              {bookingSuccess ? (
                <div style={styles.successWrapper}>
                  <CheckCircle size={48} color="var(--secondary)" />
                  <h2>Booking Submitted Successfully!</h2>
                  <p>Your consultation request has been submitted. The advocate has been notified and will review your request.</p>
                  <button className="btn btn-primary" onClick={() => onNavigate('my-bookings')}>
                    Go to My Bookings
                  </button>
                </div>
              ) : (
                <>
                  <h2 style={{marginBottom: '8px'}}>Book Consultation</h2>
                  <p style={{fontSize: '13px', color: 'var(--text-muted)', marginBottom: '20px'}}>
                    Scheduling appointment with <strong>{selectedLawyer.name}</strong> for <strong>₹{selectedLawyer.consultationFee || 500}</strong>.
                  </p>
                  
                  <form onSubmit={handleBookingSubmit} style={styles.form}>
                    <div style={styles.formGroup}>
                      <label style={styles.label}>Case / Issue summary</label>
                      <div style={styles.formInputWrapper}>
                        <FileText size={18} style={styles.formIcon} />
                        <input 
                          type="text" 
                          placeholder="e.g. Property dispute, Cyber fraud, Divorce case" 
                          className="input-field"
                          value={caseTitle}
                          onChange={(e) => setCaseTitle(e.target.value)}
                          required
                        />
                      </div>
                    </div>

                    <div style={styles.formGroup}>
                      <label style={styles.label}>Detailed Description</label>
                      <textarea 
                        placeholder="Provide details about your situation so the advocate can prepare before the call."
                        className="input-field"
                        rows="4"
                        value={caseDescription}
                        onChange={(e) => setCaseDescription(e.target.value)}
                        style={{resize: 'none', paddingLeft: '16px'}}
                        required
                      />
                    </div>

                    <div style={styles.formRow}>
                      <div style={{...styles.formGroup, flex: 1}}>
                        <label style={styles.label}>Contact Number</label>
                        <div style={styles.formInputWrapper}>
                          <Phone size={18} style={styles.formIcon} />
                          <input 
                            type="tel" 
                            placeholder="Phone for callback" 
                            className="input-field"
                            value={contactNumber}
                            onChange={(e) => setContactNumber(e.target.value)}
                            required
                          />
                        </div>
                      </div>
                      <div style={{...styles.formGroup, flex: 1}}>
                        <label style={styles.label}>Consultation Type</label>
                        <select 
                          className="input-field" 
                          value={consultationType} 
                          onChange={(e) => setConsultationType(e.target.value)}
                          style={{paddingLeft: '16px', background: 'rgba(15,23,42,0.8)'}}
                        >
                          <option value="Online">Online / Video Call</option>
                          <option value="In-Person">In-Person at Office</option>
                        </select>
                      </div>
                    </div>

                    <div style={styles.formRow}>
                      <div style={{...styles.formGroup, flex: 1}}>
                        <label style={styles.label}>Appointment Date</label>
                        <input 
                          type="date" 
                          className="input-field" 
                          value={date} 
                          onChange={(e) => setDate(e.target.value)}
                          style={{paddingLeft: '16px', background: 'rgba(15,23,42,0.8)'}}
                          required
                        />
                      </div>
                      <div style={{...styles.formGroup, flex: 1}}>
                        <label style={styles.label}>Time Slot</label>
                        <input 
                          type="time" 
                          className="input-field" 
                          value={time} 
                          onChange={(e) => setTime(e.target.value)}
                          style={{paddingLeft: '16px', background: 'rgba(15,23,42,0.8)'}}
                          required
                        />
                      </div>
                    </div>

                    <button type="submit" className="btn btn-primary" style={styles.submitBtn} disabled={bookingLoading}>
                      {bookingLoading ? 'Submitting request...' : 'Confirm and Book'}
                    </button>
                  </form>
                </>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

const styles = {
  container: {
    padding: '24px',
    maxWidth: '1200px',
    margin: '0 auto'
  },
  header: {
    textAlign: 'center',
    marginBottom: '24px'
  },
  subtitle: {
    fontSize: '14px',
    color: 'var(--text-muted)',
    marginTop: '6px'
  },
  searchBar: {
    display: 'flex',
    alignItems: 'center',
    padding: '4px 16px',
    position: 'relative',
    marginBottom: '24px'
  },
  searchIcon: {
    position: 'absolute',
    left: '16px'
  },
  searchInput: {
    paddingLeft: '36px',
    border: 'none',
    background: 'none',
    height: '40px'
  },
  loader: {
    textAlign: 'center',
    padding: '40px',
    color: 'var(--text-muted)'
  },
  emptyText: {
    textAlign: 'center',
    padding: '40px',
    color: 'var(--text-muted)',
    fontSize: '14px'
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
    gap: '20px'
  },
  lawyerCard: {
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    cursor: 'pointer',
    transition: 'all 0.2s',
    ':hover': {
      borderColor: 'var(--primary)'
    }
  },
  lawyerName: {
    fontSize: '16px',
    fontWeight: '800'
  },
  metaRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px'
  },
  metaText: {
    fontSize: '13px',
    color: 'var(--text-muted)'
  },
  bookBtn: {
    marginTop: '8px',
    width: '100%',
    height: '40px',
    fontSize: '13px'
  },
  detailsView: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
    maxWidth: '650px',
    margin: '0 auto'
  },
  backLink: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    background: 'none',
    border: 'none',
    color: 'var(--primary)',
    fontWeight: '700',
    cursor: 'pointer',
    fontSize: '14px',
    alignSelf: 'flex-start'
  },
  detailsCard: {
    padding: '32px',
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  detailsName: {
    fontSize: '24px',
    fontWeight: '800'
  },
  detailsTag: {
    fontSize: '13px',
    fontWeight: '700',
    color: 'var(--secondary)',
    textTransform: 'uppercase'
  },
  divider: {
    height: '1px',
    background: 'var(--border)'
  },
  infoSummaryGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gap: '12px',
    margin: '8px 0'
  },
  infoBox: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '12px',
    background: 'rgba(255,255,255,0.01)',
    borderRadius: '12px',
    border: '1px solid var(--border)'
  },
  infoLabel: {
    fontSize: '10px',
    color: 'var(--text-muted)'
  },
  infoVal: {
    fontSize: '13px',
    fontWeight: '700'
  },
  bioSection: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px'
  },
  bioTitle: {
    fontSize: '14px',
    fontWeight: '700'
  },
  bioText: {
    fontSize: '14px',
    lineHeight: '1.6',
    color: 'var(--text-muted)'
  },
  barSection: {
    fontSize: '13px',
    color: 'var(--text-muted)',
    padding: '12px',
    background: 'rgba(255,255,255,0.02)',
    borderRadius: '10px',
    border: '1px dashed var(--border)'
  },
  actionConsultBtn: {
    height: '44px',
    fontSize: '14px',
    marginTop: '8px'
  },
  bookingCard: {
    padding: '32px'
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  formGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '6px'
  },
  label: {
    fontSize: '13px',
    fontWeight: '700',
    color: 'var(--text-main)'
  },
  formInputWrapper: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center'
  },
  formIcon: {
    position: 'absolute',
    left: '16px',
    color: 'var(--primary)'
  },
  formRow: {
    display: 'flex',
    gap: '16px'
  },
  submitBtn: {
    height: '48px',
    marginTop: '12px'
  },
  successWrapper: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    textAlign: 'center',
    gap: '16px',
    padding: '24px'
  }
};
