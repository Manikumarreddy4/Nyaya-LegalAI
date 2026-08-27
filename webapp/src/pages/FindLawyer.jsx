import React, { useState, useEffect } from 'react';
import { db, auth } from '../firebase';
import { collection, getDocs, doc, setDoc, query, where, onSnapshot, getDoc } from 'firebase/firestore';
import { Search, MapPin, Award, Clock, DollarSign, ArrowLeft, Calendar, FileText, Phone, CheckCircle, ShieldAlert, MessageSquare, User } from 'lucide-react';

const combineDateAndTime = (dateStr, timeStr) => {
  if (!dateStr || !timeStr) return null;
  const [year, month, day] = dateStr.split('-').map(Number);
  const [hours, minutes] = timeStr.split(':').map(Number);
  return new Date(year, month - 1, day, hours, minutes, 0, 0);
};

const generateTimeSlots = () => {
  const slots = [];
  for (let hour = 0; hour < 24; hour++) {
    for (let min of [0, 15, 30, 45]) {
      const hh = String(hour).padStart(2, '0');
      const mm = String(min).padStart(2, '0');
      const val = `${hh}:${mm}`;
      
      const ampm = hour >= 12 ? 'PM' : 'AM';
      const displayHour = hour % 12 === 0 ? 12 : hour % 12;
      const displayMin = String(min).padStart(2, '0');
      const label = `${displayHour}:${displayMin} ${ampm}`;
      
      slots.push({ value: val, label });
    }
  }
  return slots;
};
const ALL_TIME_SLOTS = generateTimeSlots();

export default function FindLawyer({ user, onNavigate, navExtra }) {
  const [lawyers, setLawyers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  
  // Selection States
  const [selectedLawyer, setSelectedLawyer] = useState(null);
  const [bookingMode, setBookingMode] = useState(false);
  const [bookingSuccess, setBookingSuccess] = useState(false);
  const [reviews, setReviews] = useState([]);
  const [reviewsLoading, setReviewsLoading] = useState(false);
  
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [selectedSort, setSelectedSort] = useState('Recommended');
  const [sortDropdownOpen, setSortDropdownOpen] = useState(false);

  // Booking Form Fields matching Android
  const [caseTitle, setCaseTitle] = useState('');
  const [caseDescription, setCaseDescription] = useState('');
  const [contactNumber, setContactNumber] = useState('');
  const [consultationType, setConsultationType] = useState('Online');
  const [dateChoice, setDateChoice] = useState('today'); // today, tomorrow, custom
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');
  const getSelectableTimeSlots = () => {
    const todayStr = new Date().toISOString().split('T')[0];
    if (date !== todayStr) {
      return ALL_TIME_SLOTS;
    }
    const minAllowedTime = Date.now() + 2 * 60 * 1000;
    return ALL_TIME_SLOTS.filter(slot => {
      const slotDateTime = combineDateAndTime(date, slot.value);
      return slotDateTime && slotDateTime.getTime() >= minAllowedTime;
    });
  };
  const [preferredLanguage, setPreferredLanguage] = useState('English');
  const [issueType, setIssueType] = useState('Civil Law');
  const [additionalNotes, setAdditionalNotes] = useState('');
  const [bookingLoading, setBookingLoading] = useState(false);
  const [showConfirmSummary, setShowConfirmSummary] = useState(false);
  const [retryTrigger, setRetryTrigger] = useState(0);

  const isContactNumberValid = /^[0-9]{10}$/.test(contactNumber);
  const contactNumberError = (contactNumber && !isContactNumberValid) ? "Phone number must contain exactly 10 digits." : "";

  const isOnlineAvailable = (lawyer) => {
    if (!lawyer) return false;
    const globalStatus = lawyer.availability_status !== undefined ? lawyer.availability_status === true : (lawyer.isAvailable !== false && lawyer.onlineAvailable !== false);
    if (!globalStatus) return false;
    return lawyer.video_consultation_available !== undefined ? lawyer.video_consultation_available === true : lawyer.onlineAvailable !== false;
  };

  const isInPersonAvailable = (lawyer) => {
    if (!lawyer) return false;
    const globalStatus = lawyer.availability_status !== undefined ? lawyer.availability_status === true : (lawyer.isAvailable !== false && lawyer.onlineAvailable !== false);
    if (!globalStatus) return false;
    return lawyer.in_person_consultation_available !== undefined ? lawyer.in_person_consultation_available === true : lawyer.isInPersonAvailable !== false;
  };

  const handleRetry = () => {
    setLoading(true);
    setError(null);
    setRetryTrigger(prev => prev + 1);
  };

  useEffect(() => {
    if (user && user.phone) {
      setContactNumber(user.phone);
    }
  }, [user]);

  useEffect(() => {
    if (selectedLawyer) {
      if (!isInPersonAvailable(selectedLawyer) && consultationType === 'In-Person') {
        setConsultationType('Online');
      } else if (!isOnlineAvailable(selectedLawyer) && consultationType === 'Online') {
        setConsultationType('In-Person');
      }
    }
  }, [selectedLawyer, consultationType]);

  useEffect(() => {
    const handleGlobalClick = () => {
      setSortDropdownOpen(false);
    };
    document.addEventListener('click', handleGlobalClick);
    return () => {
      document.removeEventListener('click', handleGlobalClick);
    };
  }, []);

  useEffect(() => {
    const todayStr = new Date().toISOString().split('T')[0];
    const tom = new Date();
    tom.setDate(tom.getDate() + 1);
    const tomStr = tom.toISOString().split('T')[0];
    
    if (dateChoice === 'today') {
      setDate(todayStr);
    } else if (dateChoice === 'tomorrow') {
      setDate(tomStr);
    } else if (dateChoice === 'custom') {
      // Allow custom date selection
    }
  }, [dateChoice]);

  useEffect(() => {
    const checkSelectedTimeValidity = () => {
      if (date && time) {
        const selectedDateTime = combineDateAndTime(date, time);
        const minAllowedTime = Date.now() + 2 * 60 * 1000;
        if (selectedDateTime && selectedDateTime.getTime() < minAllowedTime) {
          setTime('');
          alert("Please select a consultation time at least 2 minutes from now.");
        }
      }
    };
    checkSelectedTimeValidity();
    const interval = setInterval(checkSelectedTimeValidity, 5000);
    return () => clearInterval(interval);
  }, [date, time]);

  useEffect(() => {
    if (navExtra?.startBooking && navExtra?.lawyer) {
      const advocate = { ...navExtra.lawyer, id: navExtra.lawyer.id || navExtra.lawyer.lawyerId };
      setSelectedLawyer(advocate);
      setBookingMode(true);
      setBookingSuccess(false);
    } else if (navExtra?.selectedLawyer) {
      setSelectedLawyer(navExtra.selectedLawyer);
      setBookingMode(false);
      setBookingSuccess(false);
    }
  }, [navExtra]);


  useEffect(() => {
    if (!selectedLawyer) {
      setReviews([]);
      return;
    }
    setReviewsLoading(true);
    const lawyerId = selectedLawyer.lawyerId || selectedLawyer.id;
    const q = query(collection(db, 'reviews'), where('lawyerId', '==', lawyerId));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const list = [];
      snapshot.forEach((docSnap) => {
        list.push({ id: docSnap.id, ...docSnap.data() });
      });
      list.sort((a, b) => {
        const aTime = a.createdAt?.seconds || a.createdAt?.toMillis?.() / 1000 || 0;
        const bTime = b.createdAt?.seconds || b.createdAt?.toMillis?.() / 1000 || 0;
        return bTime - aTime;
      });
      setReviews(list);
      setReviewsLoading(false);
    }, (err) => {
      console.error("FIND_LAWYER ERROR: Error loading reviews:", err);
      setReviewsLoading(false);
    });
    return () => unsubscribe();
  }, [selectedLawyer]);

  // Load Lawyers from Firestore in real-time
  useEffect(() => {
    console.log('FIND_LAWYER: Page mounted');
    console.log(`FIND_LAWYER: Firebase auth checked - user is ${user ? 'logged in' : 'not logged in'}`);
    console.log('FIND_LAWYER: Firestore query started');

    const q = collection(db, 'lawyers');

    const timeoutId = setTimeout(() => {
      if (loading) {
        console.warn('FIND_LAWYER ERROR: Firestore request timed out after 15 seconds.');
        setError('Unable to load lawyers. Please check your internet connection and try again.');
        setLoading(false);
      }
    }, 15000);

    const unsubscribe = onSnapshot(q, (querySnapshot) => {
      clearTimeout(timeoutId);
      console.log('FIND_LAWYER: Firestore response received');
      console.log(`FIND_LAWYER: Total documents received = ${querySnapshot.size}`);

      const list = [];
      querySnapshot.forEach(docSnapshot => {
        const data = docSnapshot.data();
        const roleLower = (data.role || '').toLowerCase().trim();
        const isLawyer = roleLower === 'lawyer' || roleLower === 'advocate';
        const resolvedName = (data.name || data.fullName || data.displayName || '').trim();
        
        // General lawyer availability status check.
        // As per Step 7: availability filtering should safely handle true/false and old firestore fields
        const isAvailable = data.availability_status !== undefined ? data.availability_status === true : data.isAvailable !== false;
        
        if (isAvailable && isLawyer && resolvedName && resolvedName.toLowerCase() !== 'advocate') {
          list.push({ id: docSnapshot.id, ...data, name: resolvedName });
        }
      });

      console.log(`FIND_LAWYER: Total lawyers after filtering = ${list.length}`);
      setLawyers(list);
      setError(null);
      setLoading(false);
    }, (err) => {
      clearTimeout(timeoutId);
      console.error("FIND_LAWYER ERROR:", err);
      setError('Failed to load lawyers from Firestore: ' + err.message);
      setLoading(false);
    });

    return () => {
      clearTimeout(timeoutId);
      unsubscribe();
    };
  }, [retryTrigger, user]);

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

function matchesCategory(specialization, selectedCategory) {
  const cat = (selectedCategory || '').trim().toLowerCase();
  if (cat === 'all') return true;
  
  const spec = (specialization || '').trim().toLowerCase();
  if (!spec) return false;
  
  const cleanCat = cat.replace(" law", "").replace(" lawyer", "").replace(" advocate", "").replace("&", "").replace("  ", " ").trim();
  const cleanSpec = spec.replace(" law", "").replace(" lawyer", "").replace(" advocate", "").replace("&", "").replace("  ", " ").trim();
  
  if (!cleanCat || !cleanSpec) return false;
  
  const catWords = cleanCat.split(/\s+/);
  const specWords = cleanSpec.split(/\s+/);
  
  for (const catWord of catWords) {
    if (catWord && catWord !== 'and') {
      for (const specWord of specWords) {
        if (specWord && specWord !== 'and') {
          if (catWord === specWord || catWord.includes(specWord) || specWord.includes(catWord)) {
            return true;
          }
        }
      }
    }
  }
  
  return cleanSpec.includes(cleanCat) || cleanCat.includes(cleanSpec);
}

  const handleBookingSubmit = (e) => {
    e.preventDefault();
    const currentUser = auth.currentUser;
    if (!currentUser) {
      alert('Please login again to book a consultation.');
      return;
    }
    if (!caseTitle || !caseDescription || !contactNumber || !date || !time) {
      alert('Please fill in all booking fields.');
      return;
    }
    if (!isContactNumberValid) {
      alert('Phone number must contain exactly 10 digits.');
      return;
    }

    const selectedDateTime = new Date(`${date}T${time}`);
    const minAllowedTime = Date.now() + 2 * 60 * 1000;
    if (isNaN(selectedDateTime.getTime()) || selectedDateTime.getTime() < minAllowedTime) {
      alert("Please select a consultation time at least 2 minutes from now.");
      return;
    }

    setShowConfirmSummary(true);
  };

  const executeBooking = async () => {
    setBookingLoading(true);
    const currentUser = auth.currentUser;
    console.log("BOOKING: Booking started");
    console.log("BOOKING: Current user =", currentUser?.uid);
    try {
      if (!currentUser) {
        throw new Error("Please login again to book a consultation.");
      }

      // Call backend consultation validator endpoint
      const apiEndpoint = import.meta.env.DEV ? 'http://localhost:5000/api/consultations/validate' : '/api/consultations/validate';
      const validateRes = await fetch(apiEndpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          phone: contactNumber.trim(),
          userId: currentUser.uid,
          lawyerId: selectedLawyer.lawyerId || selectedLawyer.id,
          consultationType,
          date,
          time,
          video_consultation_available: isOnlineAvailable(selectedLawyer),
          in_person_consultation_available: isInPersonAvailable(selectedLawyer),
          availability_status: selectedLawyer.availability_status !== undefined ? selectedLawyer.availability_status === true : selectedLawyer.isAvailable !== false
        })
      });

      const responseText = await validateRes.text();
      let valData = null;
      if (responseText && responseText.trim()) {
        try {
          valData = JSON.parse(responseText);
        } catch (error) {
          console.error("Invalid JSON response:", responseText);
          throw new Error(
            `Server returned an invalid response. Status: ${validateRes.status}`
          );
        }
      }

      if (!validateRes.ok) {
        throw new Error(
          valData?.error ||
          valData?.message ||
          `Failed to book consultation. Status: ${validateRes.status}`
        );
      }

      const lawyerId = selectedLawyer.lawyerId || selectedLawyer.id;
      console.log("BOOKING: Lawyer ID =", lawyerId);
      console.log("BOOKING: Fetching lawyer document");
      
      const lawyerRef = doc(db, 'lawyers', lawyerId);
      const lawyerSnapshot = await getDoc(lawyerRef);
      console.log("BOOKING: Lawyer document exists =", lawyerSnapshot.exists());
      
      if (!lawyerSnapshot.exists()) {
        throw new Error("Lawyer profile not found.");
      }
      
      const lawyerData = lawyerSnapshot.data();
      const roleLower = (lawyerData.role || '').toLowerCase().trim();
      const isLawyer = roleLower === 'lawyer' || roleLower === 'advocate';
      
      if (!isLawyer) {
        throw new Error("Lawyer profile not found.");
      }

      const onlineAvail = isOnlineAvailable(lawyerData);
      const inPersonAvail = isInPersonAvailable(lawyerData);

      if (consultationType === 'Online') {
        if (!onlineAvail) {
          throw new Error("This lawyer is currently unavailable for online consultations.");
        }
      } else if (consultationType === 'In-Person') {
        if (!inPersonAvail) {
          throw new Error("This lawyer is not currently available for in-person consultations. Please select Online consultation.");
        }
      }

      const selectedDateObj = new Date(`${date}T${time}`);
      const consultationsRef = collection(db, 'consultations');
      const busyQuery = query(
        consultationsRef,
        where('lawyerId', '==', lawyerId),
        where('status', '==', 'ACCEPTED')
      );
      const busySnapshot = await getDocs(busyQuery);
      
      const isBusy = busySnapshot.docs.some(docSnapshot => {
        const data = docSnapshot.data();
        const apptTime = getAppointmentDateTime({ id: docSnapshot.id, ...data });
        return apptTime && apptTime.getTime() === selectedDateObj.getTime();
      });

      if (isBusy) {
        throw new Error("Lawyer is busy at this time. Please select another available date or time.");
      }

      console.log("BOOKING: Creating consultation");
      const consultationId = 'consult_' + Date.now();
      const newBooking = {
        consultationId,
        clientId: currentUser.uid,
        userId: currentUser.uid, // compatible field
        lawyerId: selectedLawyer.lawyerId || selectedLawyer.id,
        clientName: currentUser.displayName || user?.name || 'Client',
        userName: currentUser.displayName || user?.name || 'Client', // compatible field
        lawyerName: selectedLawyer.name,
        caseTitle: caseTitle.trim(),
        caseDescription: caseDescription.trim(),
        consultationType,
        date,
        time,
        contactNumber: contactNumber.trim(),
        userPhone: contactNumber.trim(), // compatible field
        userEmail: currentUser.email || user?.email || '', // compatible field
        status: 'PENDING',
        fee: parseFloat(selectedLawyer.consultationFee) || 500,
        appointmentDateTime: selectedDateObj, // Stored as Timestamp
        createdAt: new Date(),
        preferredLanguage,
        issueType,
        additionalNotes,
        notes: additionalNotes, // compatible field
        hasReviewed: false,
        reviewId: ""
      };

      // Save consultation under /consultations/{id}
      await setDoc(doc(db, 'consultations', consultationId), newBooking);
      
      // Also propagate to nested subcollections
      try {
        const cId = currentUser.uid;
        const lId = selectedLawyer.lawyerId || selectedLawyer.id;
        await setDoc(doc(db, 'users', cId, 'consultations', consultationId), newBooking);
        await setDoc(doc(db, 'users', lId, 'consultations', consultationId), newBooking);
      } catch (nestedErr) {
        console.warn("Failed to write to nested subcollections, continuing:", nestedErr);
      }
      
      console.log("BOOKING: Consultation created successfully");
      setShowConfirmSummary(false);
      setBookingSuccess(true);
      setCaseTitle('');
      setCaseDescription('');
      setContactNumber(user?.phone || '');
      setDate('');
      setTime('');
      setPreferredLanguage('English');
      setAdditionalNotes('');
      setIssueType('Civil Law');
    } catch (err) {
      console.error("BOOKING ERROR:", err);
      alert('Failed to book consultation: ' + err.message);
    } finally {
      setBookingLoading(false);
    }
  };

  const getSortedLawyers = (lawyerList) => {
    const sorted = [...lawyerList];
    if (selectedSort === 'Highest Rated') {
      return sorted.sort((a, b) => (b.rating || 0) - (a.rating || 0));
    } else if (selectedSort === 'Most Experienced') {
      const getExpYears = (expStr) => {
        if (!expStr) return 0;
        const matches = expStr.match(/\d+/);
        return matches ? parseInt(matches[0], 10) : 0;
      };
      return sorted.sort((a, b) => getExpYears(b.experience) - getExpYears(a.experience));
    } else if (selectedSort === 'Lowest Fee') {
      return sorted.sort((a, b) => (a.consultationFee || 0) - (b.consultationFee || 0));
    } else if (selectedSort === 'Highest Fee') {
      return sorted.sort((a, b) => (b.consultationFee || 0) - (a.consultationFee || 0));
    }
    return sorted; // Recommended: keep the original order
  };

  const filteredLawyers = getSortedLawyers(
    lawyers.filter(l => {
      const matchesSpec = selectedCategory === 'All' || matchesCategory(l.specialization, selectedCategory);
      const q = searchQuery.toLowerCase().trim();
      const matchesQuery = !q || 
                           l.name.toLowerCase().includes(q) || 
                           (l.specialization || '').toLowerCase().includes(q) ||
                           (l.location || '').toLowerCase().includes(q);
      return matchesSpec && matchesQuery;
    })
  );

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

          {/* Categories Horizontal Tabs */}
          <div style={styles.categoryRow}>
            {['All', 'Criminal Law', 'Civil Law', 'Corporate Law', 'Family & Corporate Law', 'Property Law', 'Cyber Law', 'Tax Law'].map(cat => (
              <button
                key={cat}
                style={{
                  ...styles.categoryBtn,
                  background: selectedCategory === cat ? 'var(--primary)' : 'rgba(255, 255, 255, 0.03)',
                  color: selectedCategory === cat ? '#ffffff' : 'var(--text-muted)',
                  borderColor: selectedCategory === cat ? 'var(--primary)' : 'var(--border)'
                }}
                onClick={() => setSelectedCategory(cat)}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* Sort Dropdown */}
          <div style={styles.sortContainer}>
            <div style={styles.sortWrapper}>
              <span style={styles.sortLabel}>Sort:</span>
              <button 
                style={styles.sortSelectButton} 
                onClick={(e) => {
                  e.stopPropagation();
                  setSortDropdownOpen(!sortDropdownOpen);
                }}
              >
                {selectedSort} <span style={styles.sortArrow}>▼</span>
              </button>
              
              {sortDropdownOpen && (
                <div style={styles.sortDropdownMenu}>
                  {['Recommended', 'Highest Rated', 'Most Experienced', 'Lowest Fee', 'Highest Fee'].map((option) => (
                    <div 
                      key={option}
                      style={{
                        ...styles.sortDropdownItem,
                        fontWeight: selectedSort === option ? 'bold' : 'normal',
                        color: selectedSort === option ? 'var(--secondary)' : 'var(--text-main)',
                      }}
                      onClick={() => {
                        setSelectedSort(option);
                        setSortDropdownOpen(false);
                      }}
                    >
                      {selectedSort === option ? '✓ ' : ''}{option}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {loading ? (
            <div style={styles.loader}>Loading advocates directory...</div>
          ) : error ? (
            <div style={{ textAlign: 'center', padding: '40px' }}>
              <div style={{ color: 'var(--error)', marginBottom: '16px', fontWeight: 'bold' }}>{error}</div>
              <button className="btn btn-primary" onClick={handleRetry}>Retry Loading</button>
            </div>
          ) : filteredLawyers.length === 0 ? (
            <div style={styles.emptyText}>
              {selectedCategory === 'All' 
                ? 'No lawyers available.' 
                : 'No lawyers available in this category.'}
            </div>
          ) : (
            <div style={styles.grid}>
              {filteredLawyers.map((lawyer) => (
                <div 
                  key={lawyer.id} 
                  className="glass-panel activity-card" 
                  style={styles.lawyerCard}
                  onClick={() => onNavigate('lawyer-profile', { lawyerId: lawyer.id })}
                >
                  <div style={styles.cardHeaderRow}>
                    <div style={styles.avatarContainer}>
                      {lawyer.profilePhotoUrl || lawyer.profileImage ? (
                        <img 
                          src={lawyer.profilePhotoUrl || lawyer.profileImage} 
                          alt={lawyer.name} 
                          style={styles.avatar} 
                        />
                      ) : (
                        <div style={styles.avatarPlaceholder}>
                          {lawyer.name.charAt(0).toUpperCase()}
                        </div>
                      )}
                    </div>
                    <div style={styles.headerInfo}>
                      <div style={styles.nameRow}>
                        <h3 style={styles.lawyerName}>{lawyer.name}</h3>
                        {lawyer.verificationStatus === 'VERIFIED' && (
                          <span title="Verified Advocate" style={styles.verifiedIcon}>
                            <CheckCircle size={15} color="var(--secondary)" fill="rgba(16, 185, 129, 0.15)" style={{ marginLeft: '6px' }} />
                          </span>
                        )}
                      </div>
                      <div style={styles.specializationText}>{lawyer.specialization || 'General Practice'}</div>
                      <div style={styles.ratingRow}>
                        <span style={{ color: '#FFB300', marginRight: '4px', fontSize: '14px' }}>★</span>
                        <span style={{ fontWeight: 'bold', fontSize: '13px' }}>
                          {Number(lawyer.rating || 5.0).toFixed(1)}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div style={styles.cardDetails}>
                    <div style={styles.cardDetailItem}>
                      <Clock size={13} color="var(--primary)" />
                      <span>{lawyer.experience || '0 Years'} Experience</span>
                    </div>
                    <div style={styles.cardDetailItem}>
                      <MapPin size={13} color="var(--primary)" />
                      <span>{lawyer.location || lawyer.city || 'Location not specified'}</span>
                    </div>
                    <div style={styles.cardDetailItem}>
                      <MessageSquare size={13} color="var(--primary)" />
                      <span>Languages: {lawyer.languages || 'English'}</span>
                    </div>
                    <div style={styles.cardDetailItem}>
                      <DollarSign size={13} color="var(--primary)" />
                      <span>₹{lawyer.consultationFee || 500} consultation fee</span>
                    </div>
                  </div>

                  <div style={styles.availabilityBadgesRow}>
                    <div style={{
                      ...styles.availabilityBadge,
                      backgroundColor: isOnlineAvailable(lawyer) ? 'rgba(16, 185, 129, 0.12)' : 'rgba(255, 255, 255, 0.05)',
                      color: isOnlineAvailable(lawyer) ? 'var(--secondary)' : 'var(--text-muted)',
                      border: `1px solid ${isOnlineAvailable(lawyer) ? 'rgba(16, 185, 129, 0.3)' : 'rgba(255, 255, 255, 0.08)'}`
                    }}>
                      🖥 Video Call: {isOnlineAvailable(lawyer) ? 'ON' : 'OFF'}
                    </div>
                    <div style={{
                      ...styles.availabilityBadge,
                      backgroundColor: isInPersonAvailable(lawyer) ? 'rgba(16, 185, 129, 0.12)' : 'rgba(255, 255, 255, 0.05)',
                      color: isInPersonAvailable(lawyer) ? 'var(--secondary)' : 'var(--text-muted)',
                      border: `1px solid ${isInPersonAvailable(lawyer) ? 'rgba(16, 185, 129, 0.3)' : 'rgba(255, 255, 255, 0.08)'}`
                    }}>
                      🏢 In-Person: {isInPersonAvailable(lawyer) ? 'ON' : 'OFF'}
                    </div>
                  </div>

                  <div style={styles.cardButtonsRow}>
                    <button 
                      className="btn btn-secondary" 
                      style={{ flex: 1, height: '36px', fontSize: '13px', padding: '0 8px' }}
                      onClick={(e) => {
                        e.stopPropagation();
                        onNavigate('lawyer-profile', { lawyerId: lawyer.id });
                      }}
                    >
                      View Profile
                    </button>
                    <button 
                      className="btn btn-primary" 
                      style={{ 
                        flex: 1, 
                        height: '36px', 
                        fontSize: '13px', 
                        padding: '0 8px',
                        background: (!isOnlineAvailable(lawyer) && !isInPersonAvailable(lawyer)) ? 'var(--border)' : 'linear-gradient(135deg, var(--primary), var(--tertiary))',
                        cursor: (!isOnlineAvailable(lawyer) && !isInPersonAvailable(lawyer)) ? 'not-allowed' : 'pointer'
                      }}
                      disabled={!isOnlineAvailable(lawyer) && !isInPersonAvailable(lawyer)}
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedLawyer(lawyer);
                        setBookingMode(true);
                      }}
                    >
                      Book Consult
                    </button>
                  </div>
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

              {/* Client Reviews Section */}
              <div style={{ marginTop: '24px', marginBottom: '24px' }}>
                <h4 style={{ fontSize: '16px', fontWeight: '700', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  ★ Client Reviews
                </h4>
                {reviewsLoading ? (
                  <div style={{ color: 'var(--text-muted)', fontSize: '13px' }}>Loading reviews...</div>
                ) : reviews.length === 0 ? (
                  <div style={{ color: 'var(--text-muted)', fontSize: '13px', fontStyle: 'italic' }}>
                    No client reviews yet.
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {reviews.map((r) => (
                      <div key={r.id} style={{
                        padding: '12px 16px',
                        background: 'rgba(255, 255, 255, 0.02)',
                        borderRadius: '10px',
                        border: '1px solid var(--border)'
                      }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                          <span style={{ fontWeight: '700', fontSize: '14px' }}>{r.userName || r.clientName || 'Client'}</span>
                          <span style={{ color: '#FFB300', fontWeight: 'bold', fontSize: '13px' }}>
                            ★ {Number(r.rating || 5).toFixed(1)}
                          </span>
                        </div>
                        <p style={{ color: 'var(--text-muted)', fontSize: '13px', margin: 0 }}>{r.comment || 'No comment provided.'}</p>
                        {r.createdAt && (
                          <div style={{ color: 'var(--text-muted)', fontSize: '10px', marginTop: '4px', textAlign: 'right' }}>
                            {new Date(r.createdAt.seconds ? r.createdAt.seconds * 1000 : r.createdAt).toLocaleDateString()}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {(selectedLawyer.onlineAvailable === false || selectedLawyer.isAvailable === false) ? (
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
                  background: (selectedLawyer.onlineAvailable === false || selectedLawyer.isAvailable === false) ? 'var(--border)' : 'var(--primary)',
                  cursor: (selectedLawyer.onlineAvailable === false || selectedLawyer.isAvailable === false) ? 'not-allowed' : 'pointer'
                }} 
                disabled={selectedLawyer.onlineAvailable === false || selectedLawyer.isAvailable === false}
                onClick={() => setBookingMode(true)}
              >
                {(selectedLawyer.onlineAvailable === false || selectedLawyer.isAvailable === false) ? 'Currently Unavailable' : 'Schedule a Consultation'}
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
                            onChange={(e) => {
                              const val = e.target.value;
                              if (/^[0-9]*$/.test(val) && val.length <= 10) {
                                setContactNumber(val);
                              }
                            }}
                            required
                          />
                        </div>
                        {contactNumberError && (
                          <span style={{ color: 'var(--error)', fontSize: '11px', marginTop: '2px', marginLeft: '4px', textAlign: 'left', display: 'block' }}>
                            {contactNumberError}
                          </span>
                        )}
                      </div>
                      <div style={{...styles.formGroup, flex: 1}}>
                        <label style={styles.label}>Consultation Type</label>
                        <select 
                          className="input-field" 
                          value={consultationType} 
                          onChange={(e) => setConsultationType(e.target.value)}
                          style={{paddingLeft: '16px', background: 'rgba(15,23,42,0.8)'}}
                        >
                          <option value="Online" disabled={!isOnlineAvailable(selectedLawyer)}>
                            Online / Video Call{!isOnlineAvailable(selectedLawyer) ? ' (Currently unavailable)' : ''}
                          </option>
                          <option value="In-Person" disabled={!isInPersonAvailable(selectedLawyer)}>
                            In-Person at Office{!isInPersonAvailable(selectedLawyer) ? ' (Currently unavailable)' : ''}
                          </option>
                        </select>
                      </div>
                    </div>

                    <div style={styles.formRow}>
                      <div style={{...styles.formGroup, flex: 1}}>
                        <label style={styles.label}>Preferred Language</label>
                        <select
                          className="input-field"
                          value={preferredLanguage}
                          onChange={(e) => setPreferredLanguage(e.target.value)}
                          style={{paddingLeft: '16px', background: 'rgba(15,23,42,0.8)'}}
                        >
                          {['English', 'Hindi', 'Tamil', 'Telugu', 'Marathi', 'Bengali', 'Kannada', 'Malayalam'].map(lang => (
                            <option key={lang} value={lang}>{lang}</option>
                          ))}
                        </select>
                      </div>
                      <div style={{...styles.formGroup, flex: 1}}>
                        <label style={styles.label}>Legal Category</label>
                        <select
                          className="input-field"
                          value={issueType}
                          onChange={(e) => setIssueType(e.target.value)}
                          style={{paddingLeft: '16px', background: 'rgba(15,23,42,0.8)'}}
                        >
                          {['Civil Law', 'Criminal Law', 'Corporate Law', 'Family Law', 'Property Law', 'Cyber Law', 'Other'].map(cat => (
                            <option key={cat} value={cat}>{cat}</option>
                          ))}
                        </select>
                      </div>
                    </div>

                    <div style={styles.formGroup}>
                      <label style={styles.label}>Appointment Date</label>
                      <div style={{display: 'flex', gap: '8px', marginBottom: '8px'}}>
                        {['today', 'tomorrow', 'custom'].map((choice) => (
                          <button
                            type="button"
                            key={choice}
                            onClick={() => setDateChoice(choice)}
                            style={{
                              ...styles.choiceBtn,
                              background: dateChoice === choice ? 'var(--primary)' : 'rgba(255, 255, 255, 0.03)',
                              color: dateChoice === choice ? 'white' : 'var(--text-muted)',
                              borderColor: dateChoice === choice ? 'var(--primary)' : 'var(--border)',
                              flex: 1
                            }}
                          >
                            {choice.charAt(0).toUpperCase() + choice.slice(1)}
                          </button>
                        ))}
                      </div>
                      
                      {dateChoice === 'custom' && (
                        <input 
                          type="date" 
                          className="input-field" 
                          value={date} 
                          onChange={(e) => setDate(e.target.value)}
                          min={new Date().toISOString().split('T')[0]}
                          style={{paddingLeft: '16px', background: 'rgba(15,23,42,0.8)'}}
                          required
                        />
                      )}
                    </div>

                    <div style={styles.formGroup}>
                      <label style={styles.label}>Time Slot</label>
                      <select
                        className="input-field"
                        value={time}
                        onChange={(e) => setTime(e.target.value)}
                        style={{paddingLeft: '16px', background: 'rgba(15,23,42,0.8)', color: 'white'}}
                        required
                      >
                        <option value="">Select Time Slot</option>
                        {getSelectableTimeSlots().map(slot => (
                          <option key={slot.value} value={slot.value}>
                            {slot.label}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div style={styles.formGroup}>
                      <label style={styles.label}>Additional Notes</label>
                      <textarea
                        placeholder="Write any additional notes or specific questions (optional)..."
                        className="input-field"
                        rows="3"
                        value={additionalNotes}
                        onChange={(e) => setAdditionalNotes(e.target.value)}
                        style={{resize: 'none', paddingLeft: '16px', height: '80px'}}
                      />
                    </div>

                    <button type="submit" className="btn btn-primary" style={styles.submitBtn} disabled={bookingLoading || !isContactNumberValid}>
                      {bookingLoading ? 'Submitting request...' : 'Confirm and Book'}
                    </button>
                  </form>
                </>
              )}
            </div>
          )}
        </div>
      )}

      {showConfirmSummary && (
        <div style={modalStyles.overlay}>
          <div className="glass-panel" style={modalStyles.modal}>
            <h3 style={{marginBottom: '16px'}}>Confirm Consultation Request?</h3>
            <div style={{display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '14px', marginBottom: '24px'}}>
              <div><strong>Lawyer:</strong> Advocate {selectedLawyer?.name}</div>
              <div><strong>Type:</strong> {consultationType} Consultation</div>
              <div><strong>Preferred Date:</strong> {date}</div>
              <div><strong>Preferred Time:</strong> {time}</div>
              <div><strong>Consultation Fee:</strong> ₹{selectedLawyer?.consultationFee || 500}</div>
              <div><strong>Legal Category:</strong> {issueType}</div>
              {preferredLanguage && <div><strong>Language:</strong> {preferredLanguage}</div>}
            </div>
            
            <div style={{display: 'flex', gap: '12px'}}>
              <button
                className="btn"
                style={{flex: 1, background: 'rgba(255,255,255,0.05)', color: 'var(--text-main)'}}
                onClick={() => setShowConfirmSummary(false)}
                disabled={bookingLoading}
              >
                Cancel
              </button>
              <button
                className="btn btn-primary"
                style={{flex: 1}}
                onClick={executeBooking}
                disabled={bookingLoading}
              >
                {bookingLoading ? 'Sending...' : 'Send Request'}
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
  },
  categoryRow: {
    display: 'flex',
    gap: '10px',
    overflowX: 'auto',
    paddingBottom: '12px',
    marginBottom: '24px',
    whiteSpace: 'nowrap',
    msOverflowStyle: 'none',
    scrollbarWidth: 'none',
    WebkitOverflowScrolling: 'touch'
  },
  categoryBtn: {
    padding: '8px 16px',
    borderRadius: '10px',
    border: '1px solid',
    fontSize: '13px',
    fontWeight: '700',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  choiceBtn: {
    padding: '8px 16px',
    borderRadius: '10px',
    border: '1px solid',
    fontSize: '13px',
    fontWeight: '700',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  cardHeaderRow: {
    display: 'flex',
    gap: '12px',
    alignItems: 'center'
  },
  avatarContainer: {
    width: '48px',
    height: '48px',
    borderRadius: '50%',
    overflow: 'hidden',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'linear-gradient(135deg, var(--primary), var(--tertiary))',
    flexShrink: 0
  },
  avatar: {
    width: '100%',
    height: '100%',
    objectFit: 'cover'
  },
  avatarPlaceholder: {
    color: '#ffffff',
    fontWeight: '800',
    fontSize: '18px'
  },
  headerInfo: {
    display: 'flex',
    flexDirection: 'column',
    gap: '2px',
    flex: 1,
    minWidth: 0
  },
  nameRow: {
    display: 'flex',
    alignItems: 'center',
    width: '100%',
    flexWrap: 'wrap'
  },
  verifiedIcon: {
    display: 'inline-flex',
    alignItems: 'center'
  },
  specializationText: {
    fontSize: '12px',
    color: 'var(--secondary)',
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  ratingRow: {
    display: 'flex',
    alignItems: 'center'
  },
  cardDetails: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    marginTop: '6px'
  },
  cardDetailItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontSize: '13px',
    color: 'var(--text-muted)'
  },
  availabilityBadgesRow: {
    display: 'flex',
    gap: '8px',
    marginTop: '6px',
    flexWrap: 'wrap'
  },
  availabilityBadge: {
    padding: '4px 8px',
    borderRadius: '6px',
    fontSize: '11px',
    fontWeight: '700',
    display: 'inline-flex',
    alignItems: 'center',
    gap: '4px'
  },
  cardButtonsRow: {
    display: 'flex',
    gap: '8px',
    marginTop: '10px'
  },
  sortContainer: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginBottom: '16px',
    position: 'relative',
    zIndex: 10
  },
  sortWrapper: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    position: 'relative'
  },
  sortLabel: {
    fontSize: '13px',
    color: 'var(--text-muted)',
    fontWeight: '500'
  },
  sortSelectButton: {
    background: 'rgba(255, 255, 255, 0.03)',
    border: '1px solid var(--border)',
    borderRadius: '8px',
    padding: '6px 12px',
    color: 'var(--primary)',
    fontSize: '13px',
    fontWeight: '600',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    transition: 'all 0.2s',
    outline: 'none'
  },
  sortArrow: {
    fontSize: '10px',
    color: 'var(--primary)'
  },
  sortDropdownMenu: {
    position: 'absolute',
    top: '36px',
    right: 0,
    background: 'rgba(15, 23, 42, 0.95)',
    backdropFilter: 'blur(10px)',
    border: '1px solid var(--border)',
    borderRadius: '10px',
    padding: '6px 0',
    minWidth: '160px',
    boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.5)',
    zIndex: 100
  },
  sortDropdownItem: {
    padding: '8px 16px',
    fontSize: '13px',
    cursor: 'pointer',
    transition: 'background 0.2s, color 0.2s',
    textAlign: 'left',
    display: 'flex',
    alignItems: 'center',
    ':hover': {
      background: 'rgba(255, 255, 255, 0.05)'
    }
  }
};
