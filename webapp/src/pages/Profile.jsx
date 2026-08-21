import React, { useState, useEffect } from 'react';
import { db } from '../firebase';
import { doc, getDoc, updateDoc } from 'firebase/firestore';
import { User, Phone, Mail, Award, Clock, MapPin, DollarSign, BookOpen, FileText, CheckCircle } from 'lucide-react';

export default function Profile({ user, onProfileUpdate }) {
  const [profile, setProfile] = useState(null);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  
  // Lawyer specific fields
  const [specialization, setSpecialization] = useState('');
  const [experience, setExperience] = useState('');
  const [location, setLocation] = useState('');
  const [fee, setFee] = useState('');
  const [bio, setBio] = useState('');
  const [barId, setBarId] = useState('');

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    async function fetchProfile() {
      if (!user || !user.uid) return;
      try {
        const userDocRef = doc(db, 'users', user.uid);
        const docSnapshot = await getDoc(userDocRef);
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
        }
      } catch (e) {
        console.error('Error fetching profile', e);
      } finally {
        setLoading(false);
      }
    }
    fetchProfile();
  }, [user]);

  const handleSave = async (e) => {
    e.preventDefault();
    if (!name || !phone) {
      alert('Name and Phone are required.');
      return;
    }
    setSaving(true);
    setMessage('');
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
        
        // Save to users and lawyers
        await updateDoc(doc(db, 'users', user.uid), updates);
        await updateDoc(doc(db, 'lawyers', user.uid), updates);
      } else {
        await updateDoc(doc(db, 'users', user.uid), updates);
      }

      setMessage('✓ Profile updated successfully.');
      onProfileUpdate({ ...user, name: name.trim(), phone: phone.trim() });
    } catch (err) {
      console.error(err);
      alert('Error updating profile: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div style={styles.loader}>Loading profile details...</div>;
  }

  return (
    <div className="fade-in-up" style={styles.container}>
      <div className="glass-panel" style={styles.card}>
        <div style={styles.header}>
          <div style={styles.avatarCircle}>
            <User size={36} color="white" />
          </div>
          <h2>My Profile Settings</h2>
          <p style={styles.roleText}>{user.role === 'LAWYER' ? 'Registered Advocate' : 'Client Profile'}</p>
        </div>

        <form onSubmit={handleSave} style={styles.form}>
          <div style={styles.formSectionTitle}>Account Information</div>
          
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
            <label style={styles.label}>Email (Cannot be modified)</label>
            <div style={styles.inputWrapper}>
              <Mail size={16} style={styles.icon} />
              <input 
                type="email" 
                className="input-field" 
                value={profile?.email || ''} 
                disabled 
                style={{opacity: 0.6}}
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

          {user.role === 'LAWYER' && (
            <>
              <div style={styles.formSectionTitle}>Advocate Professional Credentials</div>
              
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

              <div style={styles.formRow}>
                <div style={{...styles.formGroup, flex: 1}}>
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
                
                <div style={{...styles.formGroup, flex: 1}}>
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
                  <BookOpen size={16} style={{...styles.icon, top: '16px'}} />
                  <textarea 
                    className="input-field" 
                    rows="3" 
                    value={bio} 
                    onChange={(e) => setBio(e.target.value)} 
                    style={{resize: 'none', paddingLeft: '48px'}}
                  />
                </div>
              </div>
            </>
          )}

          {message && (
            <div style={styles.successBox}>
              <CheckCircle size={18} color="var(--secondary)" />
              <span>{message}</span>
            </div>
          )}

          <button type="submit" className="btn btn-primary" style={styles.submitBtn} disabled={saving}>
            {saving ? 'Saving changes...' : 'Save Profile Details'}
          </button>
        </form>
      </div>
    </div>
  );
}

const styles = {
  container: {
    padding: '24px',
    maxWidth: '650px',
    margin: '0 auto'
  },
  card: {
    padding: '32px'
  },
  header: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '6px',
    marginBottom: '24px'
  },
  avatarCircle: {
    width: '72px',
    height: '72px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, var(--primary), var(--tertiary))',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: '8px'
  },
  roleText: {
    fontSize: '13px',
    color: 'var(--text-muted)',
    fontWeight: '700',
    textTransform: 'uppercase'
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  formSectionTitle: {
    fontSize: '12px',
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
    fontSize: '13px',
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
    height: '48px',
    marginTop: '12px'
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
    padding: '40px',
    color: 'var(--text-muted)'
  }
};
