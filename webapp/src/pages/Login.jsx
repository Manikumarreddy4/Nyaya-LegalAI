import React, { useState } from 'react';
import { auth, db } from '../firebase';
import { 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  sendPasswordResetEmail 
} from 'firebase/auth';
import { doc, setDoc, getDoc } from 'firebase/firestore';
import { Gavel, User, Mail, Lock, Phone, FileText, MapPin, Award, BookOpen, AlertCircle, CheckCircle, Clock, Wallet } from 'lucide-react';

export default function Login({ onAuthSuccess }) {
  const [isSignup, setIsSignup] = useState(false);
  const [isLawyer, setIsLawyer] = useState(false);
  
  // Common fields
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  
  // Lawyer specific fields
  const [barId, setBarId] = useState('');
  const [specialization, setSpecialization] = useState('');
  const [experience, setExperience] = useState('');
  const [location, setLocation] = useState('');
  const [bio, setBio] = useState('');
  const [fee, setFee] = useState('500');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const isPhoneValid = /^[0-9]{10}$/.test(phone);
  const isPasswordValid = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{6,}$/.test(password);

  const phoneError = (isSignup && phone && !isPhoneValid) ? "Phone number must contain exactly 10 digits." : "";
  const passwordError = (isSignup && password && !isPasswordValid) ? "Password must contain at least 6 characters, including one uppercase letter, one lowercase letter, one number, and one special character." : "";

  const isFormValid = name.trim() !== '' && 
                      email.trim() !== '' && 
                      isPhoneValid && 
                      isPasswordValid && 
                      (!isLawyer || (barId.trim() !== '' && specialization.trim() !== '' && experience.trim() !== '' && location.trim() !== ''));

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!email || !password) {
      setError('Please fill in all fields');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const userCredential = await signInWithEmailAndPassword(auth, email.trim(), password);
      const uid = userCredential.user.uid;
      
      // Fetch role from Firestore
      const userDoc = await getDoc(doc(db, 'users', uid));
      if (userDoc.exists()) {
        const userData = userDoc.data();
        const role = userData.role || 'USER';
        onAuthSuccess(role, { uid, email, name: userData.name, role, phone: userData.phone });
      } else {
        // Fallback or default profile creation
        const role = 'USER';
        await setDoc(doc(db, 'users', uid), {
          userId: uid,
          name: email.split('@')[0],
          email: email.trim(),
          role: 'USER',
          createdAt: new Date()
        });
        onAuthSuccess(role, { uid, email, name: email.split('@')[0], role, phone: '' });
      }
    } catch (err) {
      console.error(err);
      setError(err.message || 'Login failed. Please check credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleSignup = async (e) => {
    e.preventDefault();
    if (!email || !password || !name || !phone) {
      setError('Please fill in all required fields');
      return;
    }
    if (!isPhoneValid) {
      setError('Phone number must contain exactly 10 digits.');
      return;
    }
    if (!isPasswordValid) {
      setError('Password must contain at least 6 characters, including one uppercase letter, one lowercase letter, one number, and one special character.');
      return;
    }
    if (isLawyer && (!barId || !specialization || !experience || !location)) {
      setError('All professional lawyer details are required');
      return;
    }

    setLoading(true);
    setError('');
    try {
      const apiEndpoint = import.meta.env.DEV ? 'http://localhost:5000/api/auth/signup/validate' : '/api/auth/signup/validate';
      const valResponse = await fetch(apiEndpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: phone.trim(), password })
      });
      
      const valData = await valResponse.json();
      if (!valResponse.ok) {
        throw new Error(valData.error || 'Server-side validation failed.');
      }

      const userCredential = await createUserWithEmailAndPassword(auth, email.trim(), password);
      const uid = userCredential.user.uid;
      const finalRole = isLawyer ? 'LAWYER' : 'USER';

      if (isLawyer) {
        const lawyerProfile = {
          lawyerId: uid,
          userId: uid,
          name: name.trim(),
          email: email.trim(),
          phone: phone.trim(),
          barCouncilNumber: barId.trim(),
          enrollmentNumber: barId.trim(),
          specialization: specialization.trim(),
          experience: experience.trim() + " Years",
          location: location.trim(),
          city: location.trim(),
          bio: bio.trim(),
          consultationFee: parseFloat(fee) || 500,
          onlineAvailable: true,
          isAvailable: true,
          availability_status: true,
          video_consultation_available: true,
          inPersonAvailable: true,
          isInPersonAvailable: true,
          in_person_consultation_available: true,
          verificationStatus: 'PENDING',
          role: 'LAWYER',
          createdAt: new Date(),
          updatedAt: new Date()
        };
        // Save to users and lawyers
        await setDoc(doc(db, 'users', uid), lawyerProfile);
        await setDoc(doc(db, 'lawyers', uid), lawyerProfile);
      } else {
        const userProfile = {
          userId: uid,
          name: name.trim(),
          email: email.trim(),
          phone: phone.trim(),
          role: 'USER',
          createdAt: new Date()
        };
        await setDoc(doc(db, 'users', uid), userProfile);
      }

      onAuthSuccess(finalRole, { uid, email, name, role: finalRole, phone });
    } catch (err) {
      console.error(err);
      setError(err.message || 'Signup failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async () => {
    if (!email) {
      setError('Please enter your email to reset password');
      return;
    }
    setError('');
    setMessage('');
    try {
      await sendPasswordResetEmail(auth, email.trim());
      setMessage('Password reset email sent. Please check your inbox.');
    } catch (err) {
      setError(err.message || 'Failed to send reset email.');
    }
  };

  return (
    <div className="fade-in-up" style={styles.container}>
      <div className="glass-panel" style={styles.card}>
        <div style={styles.logoContainer}>
          <div style={styles.logoCircle}>
            <Gavel size={32} color="#ffffff" />
          </div>
          <h2 style={styles.logoText}>Nyaya Legal AI</h2>
          <p style={styles.subtext}>Secure and Intelligent Legal Assistant</p>
        </div>

        <div style={styles.toggleRow}>
          <button 
            style={{...styles.toggleBtn, borderBottom: !isSignup ? '3px solid var(--primary)' : 'none', color: !isSignup ? 'var(--text-main)' : 'var(--text-muted)'}}
            onClick={() => { setIsSignup(false); setError(''); }}
          >
            Login
          </button>
          <button 
            style={{...styles.toggleBtn, borderBottom: isSignup ? '3px solid var(--primary)' : 'none', color: isSignup ? 'var(--text-main)' : 'var(--text-muted)'}}
            onClick={() => { setIsSignup(true); setError(''); }}
          >
            Signup
          </button>
        </div>

        <form onSubmit={isSignup ? handleSignup : handleLogin} style={styles.form}>
          {isSignup && (
            <>
              <div style={styles.inputWrapper}>
                <User size={18} style={styles.icon} />
                <input 
                  type="text" 
                  placeholder="Full Name" 
                  className="input-field input-field-icon" 
                  value={name} 
                  onChange={(e) => setName(e.target.value)} 
                  required 
                />
              </div>

              <div style={styles.inputGroup}>
                <div style={styles.inputWrapper}>
                  <Phone size={18} style={styles.icon} />
                  <input 
                    type="tel" 
                    placeholder="Phone Number (10 digits)" 
                    className="input-field input-field-icon" 
                    value={phone} 
                    onChange={(e) => {
                      const val = e.target.value;
                      if (/^[0-9]*$/.test(val) && val.length <= 10) {
                        setPhone(val);
                      }
                    }} 
                    required 
                  />
                </div>
                {phoneError && <span style={styles.fieldError}>{phoneError}</span>}
              </div>

              <div style={styles.roleHeader}>I am registering as:</div>
              <div style={styles.roleSelector}>
                <button 
                  type="button" 
                  style={{...styles.roleBtn, background: !isLawyer ? 'var(--primary)' : 'rgba(255,255,255,0.05)'}}
                  onClick={() => setIsLawyer(false)}
                >
                  Client / User
                </button>
                <button 
                  type="button" 
                  style={{...styles.roleBtn, background: isLawyer ? 'var(--primary)' : 'rgba(255,255,255,0.05)'}}
                  onClick={() => setIsLawyer(true)}
                >
                  Lawyer
                </button>
              </div>

              {isLawyer && (
                <div style={styles.lawyerSection}>
                  <div style={styles.sectionTitle}>Professional Info</div>
                  <div style={styles.inputWrapper}>
                    <FileText size={18} style={styles.icon} />
                    <input 
                      type="text" 
                      placeholder="Bar Council ID / Enrollment No." 
                      className="input-field input-field-icon" 
                      value={barId} 
                      onChange={(e) => setBarId(e.target.value)} 
                      required 
                    />
                  </div>
                  <div style={styles.inputWrapper}>
                    <Award size={18} style={styles.icon} />
                    <input 
                      type="text" 
                      placeholder="Specialization (e.g. Criminal, Civil)" 
                      className="input-field input-field-icon" 
                      value={specialization} 
                      onChange={(e) => setSpecialization(e.target.value)} 
                      required 
                    />
                  </div>
                  <div style={styles.inputWrapper}>
                    <Clock size={18} style={styles.icon} />
                    <input 
                      type="number" 
                      placeholder="Experience (Years)" 
                      className="input-field input-field-icon" 
                      value={experience} 
                      onChange={(e) => setExperience(e.target.value)} 
                      required 
                    />
                  </div>
                  <div style={styles.inputWrapper}>
                    <MapPin size={18} style={styles.icon} />
                    <input 
                      type="text" 
                      placeholder="Location / City" 
                      className="input-field input-field-icon" 
                      value={location} 
                      onChange={(e) => setLocation(e.target.value)} 
                      required 
                    />
                  </div>
                  <div style={styles.inputWrapper}>
                    <Wallet size={18} style={styles.icon} />
                    <input 
                      type="number" 
                      placeholder="Consultation Fee (INR)" 
                      className="input-field input-field-icon" 
                      value={fee} 
                      onChange={(e) => setFee(e.target.value)} 
                      required 
                    />
                  </div>
                  <div style={styles.inputWrapper}>
                    <BookOpen size={18} style={styles.icon} />
                    <textarea 
                      placeholder="Professional Bio" 
                      className="input-field input-field-icon" 
                      rows="3" 
                      value={bio} 
                      onChange={(e) => setBio(e.target.value)} 
                      style={{resize: 'none'}}
                    />
                  </div>
                </div>
              )}
            </>
          )}

          <div style={styles.inputWrapper}>
            <Mail size={18} style={styles.icon} />
            <input 
              type="email" 
              placeholder="Email Address" 
              className="input-field input-field-icon" 
              value={email} 
              onChange={(e) => setEmail(e.target.value)} 
              required 
            />
          </div>

          <div style={styles.inputGroup}>
            <div style={styles.inputWrapper}>
              <Lock size={18} style={styles.icon} />
              <input 
                type="password" 
                placeholder="Password" 
                className="input-field input-field-icon" 
                value={password} 
                onChange={(e) => setPassword(e.target.value)} 
                required 
              />
            </div>
            {passwordError && <span style={styles.fieldError}>{passwordError}</span>}
            {isSignup && password && (
              <div style={styles.requirementsContainer}>
                <div style={styles.requirementsTitle}>Password Requirements:</div>
                <div style={{ ...styles.requirementItem, color: password.length >= 6 ? 'var(--success)' : 'var(--text-muted)' }}>
                  {password.length >= 6 ? '✓' : '○'} At least 6 characters
                </div>
                <div style={{ ...styles.requirementItem, color: /[A-Z]/.test(password) ? 'var(--success)' : 'var(--text-muted)' }}>
                  {/[A-Z]/.test(password) ? '✓' : '○'} One uppercase letter
                </div>
                <div style={{ ...styles.requirementItem, color: /[a-z]/.test(password) ? 'var(--success)' : 'var(--text-muted)' }}>
                  {/[a-z]/.test(password) ? '✓' : '○'} One lowercase letter
                </div>
                <div style={{ ...styles.requirementItem, color: /\d/.test(password) ? 'var(--success)' : 'var(--text-muted)' }}>
                  {/\d/.test(password) ? '✓' : '○'} One number
                </div>
                <div style={{ ...styles.requirementItem, color: /[^A-Za-z0-9]/.test(password) ? 'var(--success)' : 'var(--text-muted)' }}>
                  {/[^A-Za-z0-9]/.test(password) ? '✓' : '○'} One special character
                </div>
              </div>
            )}
          </div>

          {error && (
            <div style={styles.errorBox}>
              <AlertCircle size={18} color="var(--error)" />
              <span>{error}</span>
            </div>
          )}

          {message && (
            <div style={styles.successBox}>
              <CheckCircle size={18} color="var(--success)" />
              <span>{message}</span>
            </div>
          )}

          <button type="submit" className="btn btn-primary" style={styles.submitBtn} disabled={loading || (isSignup && !isFormValid)}>
            {loading ? 'Processing...' : isSignup ? 'Sign Up' : 'Log In'}
          </button>

          {!isSignup && (
            <button type="button" onClick={handleForgotPassword} style={styles.forgotBtn}>
              Forgot Password?
            </button>
          )}
        </form>
      </div>
    </div>
  );
}

const styles = {
  container: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    padding: '20px'
  },
  card: {
    width: '100%',
    maxWidth: '480px',
    padding: '32px',
    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.4)'
  },
  logoContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    marginBottom: '24px'
  },
  logoCircle: {
    width: '64px',
    height: '64px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, var(--primary), var(--tertiary))',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    boxShadow: '0 4px 12px rgba(99, 102, 241, 0.3)',
    marginBottom: '16px'
  },
  logoText: {
    fontSize: '24px',
    fontWeight: '800',
    letterSpacing: '-0.5px',
    marginBottom: '4px'
  },
  subtext: {
    fontSize: '13px',
    color: 'var(--text-muted)'
  },
  toggleRow: {
    display: 'flex',
    marginBottom: '24px',
    borderBottom: '1px solid var(--border)'
  },
  toggleBtn: {
    flex: 1,
    padding: '12px',
    background: 'none',
    border: 'none',
    fontWeight: '700',
    fontSize: '15px',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  inputWrapper: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center'
  },
  icon: {
    position: 'absolute',
    left: '16px',
    color: 'var(--primary)',
    pointerEvents: 'none'
  },
  submitBtn: {
    height: '48px',
    marginTop: '8px'
  },
  forgotBtn: {
    background: 'none',
    border: 'none',
    color: 'var(--primary)',
    fontWeight: '600',
    fontSize: '13px',
    cursor: 'pointer',
    alignSelf: 'center',
    marginTop: '8px'
  },
  roleHeader: {
    fontSize: '14px',
    fontWeight: '700',
    color: 'var(--text-main)',
    marginTop: '4px'
  },
  roleSelector: {
    display: 'flex',
    gap: '12px',
    marginBottom: '4px'
  },
  roleBtn: {
    flex: 1,
    padding: '12px',
    borderRadius: '12px',
    border: '1px solid var(--border)',
    color: 'white',
    fontWeight: '600',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  lawyerSection: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
    padding: '16px',
    background: 'rgba(255, 255, 255, 0.02)',
    borderRadius: '16px',
    border: '1px dashed var(--border)'
  },
  sectionTitle: {
    fontSize: '13px',
    fontWeight: '800',
    color: 'var(--primary)',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  errorBox: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    padding: '12px',
    background: 'rgba(239, 68, 68, 0.1)',
    border: '1px solid rgba(239, 68, 68, 0.2)',
    borderRadius: '12px',
    color: 'var(--text-main)',
    fontSize: '13px'
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
  inputGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '4px'
  },
  fieldError: {
    color: 'var(--error)',
    fontSize: '11px',
    marginTop: '2px',
    marginLeft: '4px',
    textAlign: 'left',
    display: 'block'
  },
  requirementsContainer: {
    display: 'flex',
    flexDirection: 'column',
    gap: '4px',
    padding: '10px 14px',
    background: 'rgba(255, 255, 255, 0.02)',
    border: '1px solid var(--border)',
    borderRadius: '8px',
    marginTop: '4px',
    textAlign: 'left'
  },
  requirementsTitle: {
    fontSize: '12px',
    fontWeight: '700',
    color: 'var(--text-main)',
    marginBottom: '4px'
  },
  requirementItem: {
    fontSize: '11px',
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    transition: 'color 0.2s'
  }
};
