import React, { useState, useEffect } from 'react';
import { auth, db } from './firebase';
import { onAuthStateChanged, signOut } from 'firebase/auth';
import { doc, getDoc } from 'firebase/firestore';
import Login from './pages/Login';
import ClientDashboard from './pages/ClientDashboard';
import LawyerDashboard from './pages/LawyerDashboard';
import LegalAssistant from './pages/LegalAssistant';
import LegalLearning from './pages/LegalLearning';
import LawEncyclopedia from './pages/LawEncyclopedia';
import FindLawyer from './pages/FindLawyer';
import MyBookings from './pages/MyBookings';
import Profile from './pages/Profile';

import { 
  Gavel, 
  LayoutDashboard, 
  MessageSquare, 
  School, 
  BookOpen, 
  Search, 
  Calendar, 
  User, 
  LogOut, 
  Menu, 
  X 
} from 'lucide-react';

export default function App() {
  const [user, setUser] = useState(null);
  const [role, setRole] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [currentRoute, setCurrentRoute] = useState('login');
  const [navExtra, setNavExtra] = useState(null);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  // Monitor Authentication State
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      setAuthLoading(true);
      if (firebaseUser) {
        try {
          const userDoc = await getDoc(doc(db, 'users', firebaseUser.uid));
          if (userDoc.exists()) {
            const userData = userDoc.data();
            const userRole = userData.role || 'USER';
            
            setUser({
              uid: firebaseUser.uid,
              email: firebaseUser.email,
              name: userData.name || firebaseUser.email.split('@')[0],
              role: userRole,
              phone: userData.phone || ''
            });
            setRole(userRole);
            setCurrentRoute(userRole === 'LAWYER' ? 'lawyer-dashboard' : 'dashboard');
          } else {
            // Default user fallback
            setUser({
              uid: firebaseUser.uid,
              email: firebaseUser.email,
              name: firebaseUser.email.split('@')[0],
              role: 'USER',
              phone: ''
            });
            setRole('USER');
            setCurrentRoute('dashboard');
          }
        } catch (e) {
          console.error('Error fetching user data from Firestore', e);
        }
      } else {
        setUser(null);
        setRole(null);
        setCurrentRoute('login');
      }
      setAuthLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const handleAuthSuccess = (userRole, userInfo) => {
    setUser(userInfo);
    setRole(userRole);
    setCurrentRoute(userRole === 'LAWYER' ? 'lawyer-dashboard' : 'dashboard');
  };

  const handleLogout = async () => {
    try {
      await signOut(auth);
      setUser(null);
      setRole(null);
      setCurrentRoute('login');
    } catch (e) {
      console.error(e);
    }
  };

  const handleNavigate = (route, extra = null) => {
    setNavExtra(extra);
    setCurrentRoute(route);
    setMobileMenuOpen(false);
  };

  // Render correct page
  const renderPage = () => {
    switch (currentRoute) {
      case 'login':
        return <Login onAuthSuccess={handleAuthSuccess} />;
      case 'dashboard':
        return <ClientDashboard user={user} onNavigate={handleNavigate} />;
      case 'lawyer-dashboard':
        return <LawyerDashboard user={user} onNavigate={handleNavigate} />;
      case 'legal-assistant':
        return <LegalAssistant user={user} preselectedSessionId={navExtra?.sessionId} />;
      case 'legal-learning':
        return <LegalLearning user={user} />;
      case 'law-search':
        return <LawEncyclopedia />;
      case 'find-lawyer':
        return <FindLawyer user={user} onNavigate={handleNavigate} />;
      case 'my-bookings':
        return <MyBookings user={user} initialFilter={navExtra?.filter} />;
      case 'profile':
        return <Profile user={user} onProfileUpdate={(updatedUser) => setUser(updatedUser)} />;
      default:
        return <ClientDashboard user={user} onNavigate={handleNavigate} />;
    }
  };

  if (authLoading) {
    return (
      <div style={styles.loaderContainer}>
        <div style={styles.logoCircle}>
          <Gavel size={32} color="white" />
        </div>
        <h3 style={{marginTop: '16px'}}>Loading Nyaya Legal AI...</h3>
      </div>
    );
  }

  const isLoginPage = currentRoute === 'login';

  return (
    <div style={styles.appContainer}>
      {!isLoginPage && (
        <>
          {/* Header for Mobile Layout */}
          <header style={styles.mobileHeader}>
            <div style={styles.mobileBrand}>
              <Gavel size={20} color="var(--primary)" />
              <span style={{fontWeight: '800'}}>Nyaya AI</span>
            </div>
            <button style={styles.menuBtn} onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
              {mobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          </header>

          {/* Desktop Sidebar navigation */}
          <aside className="glass-panel" style={styles.sidebar}>
            <div style={styles.brand}>
              <div style={styles.brandIconCircle}>
                <Gavel size={22} color="white" />
              </div>
              <h2 style={styles.brandName}>Nyaya Legal AI</h2>
            </div>

            <nav style={styles.nav}>
              {role === 'LAWYER' ? (
                <>
                  <SidebarLink 
                    active={currentRoute === 'lawyer-dashboard'} 
                    onClick={() => handleNavigate('lawyer-dashboard')}
                    icon={<LayoutDashboard size={18} />}
                    label="Lawyer Dashboard" 
                  />
                  <SidebarLink 
                    active={currentRoute === 'profile'} 
                    onClick={() => handleNavigate('profile')}
                    icon={<User size={18} />}
                    label="Account Details" 
                  />
                </>
              ) : (
                <>
                  <SidebarLink 
                    active={currentRoute === 'dashboard'} 
                    onClick={() => handleNavigate('dashboard')}
                    icon={<LayoutDashboard size={18} />}
                    label="Dashboard" 
                  />
                  <SidebarLink 
                    active={currentRoute === 'legal-assistant'} 
                    onClick={() => handleNavigate('legal-assistant')}
                    icon={<MessageSquare size={18} />}
                    label="AI Assistant" 
                  />
                  <SidebarLink 
                    active={currentRoute === 'legal-learning'} 
                    onClick={() => handleNavigate('legal-learning')}
                    icon={<School size={18} />}
                    label="Legal Learning" 
                  />
                  <SidebarLink 
                    active={currentRoute === 'law-search'} 
                    onClick={() => handleNavigate('law-search')}
                    icon={<BookOpen size={18} />}
                    label="Law Search" 
                  />
                  <SidebarLink 
                    active={currentRoute === 'find-lawyer'} 
                    onClick={() => handleNavigate('find-lawyer')}
                    icon={<Search size={18} />}
                    label="Find Lawyer" 
                  />
                  <SidebarLink 
                    active={currentRoute === 'my-bookings'} 
                    onClick={() => handleNavigate('my-bookings')}
                    icon={<Calendar size={18} />}
                    label="My Bookings" 
                  />
                  <SidebarLink 
                    active={currentRoute === 'profile'} 
                    onClick={() => handleNavigate('profile')}
                    icon={<User size={18} />}
                    label="My Profile" 
                  />
                </>
              )}
            </nav>

            <button style={styles.logoutBtn} onClick={handleLogout}>
              <LogOut size={16} /> Logout
            </button>
          </aside>

          {/* Mobile Overlay Menu */}
          {mobileMenuOpen && (
            <div style={styles.mobileOverlay}>
              <nav style={styles.mobileNav}>
                {role === 'LAWYER' ? (
                  <>
                    <MobileNavLink active={currentRoute === 'lawyer-dashboard'} onClick={() => handleNavigate('lawyer-dashboard')} label="Lawyer Dashboard" />
                    <MobileNavLink active={currentRoute === 'profile'} onClick={() => handleNavigate('profile')} label="Account Settings" />
                  </>
                ) : (
                  <>
                    <MobileNavLink active={currentRoute === 'dashboard'} onClick={() => handleNavigate('dashboard')} label="Home Dashboard" />
                    <MobileNavLink active={currentRoute === 'legal-assistant'} onClick={() => handleNavigate('legal-assistant')} label="AI Assistant" />
                    <MobileNavLink active={currentRoute === 'legal-learning'} onClick={() => handleNavigate('legal-learning')} label="Legal Learning" />
                    <MobileNavLink active={currentRoute === 'law-search'} onClick={() => handleNavigate('law-search')} label="Law Search & Database" />
                    <MobileNavLink active={currentRoute === 'find-lawyer'} onClick={() => handleNavigate('find-lawyer')} label="Find Advocate" />
                    <MobileNavLink active={currentRoute === 'my-bookings'} onClick={() => handleNavigate('my-bookings')} label="My Consultations" />
                    <MobileNavLink active={currentRoute === 'profile'} onClick={() => handleNavigate('profile')} label="My Profile" />
                  </>
                )}
                <button style={styles.mobileLogoutBtn} onClick={handleLogout}>
                  <LogOut size={16} /> Logout Account
                </button>
              </nav>
            </div>
          )}
        </>
      )}

      {/* Main Page Content Body */}
      <main style={{...styles.mainContent, width: isLoginPage ? '100%' : 'calc(100% - 260px)', paddingLeft: isLoginPage ? 0 : '260px'}}>
        {renderPage()}
      </main>
    </div>
  );
}

// Helper components
function SidebarLink({ active, onClick, icon, label }) {
  return (
    <button 
      onClick={onClick} 
      style={{
        ...styles.link,
        background: active ? 'rgba(99, 102, 241, 0.12)' : 'transparent',
        color: active ? 'var(--primary)' : 'var(--text-main)',
        borderLeft: active ? '4px solid var(--primary)' : '4px solid transparent'
      }}
    >
      {icon}
      <span>{label}</span>
    </button>
  );
}

function MobileNavLink({ active, onClick, label }) {
  return (
    <button 
      onClick={onClick} 
      style={{
        ...styles.mobileLink,
        color: active ? 'var(--primary)' : 'var(--text-main)'
      }}
    >
      {label}
    </button>
  );
}

const styles = {
  appContainer: {
    display: 'flex',
    minHeight: '100vh',
    width: '100vw',
    boxSizing: 'border-box'
  },
  loaderContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    height: '100vh',
    background: '#0f172a',
    color: '#ffffff'
  },
  logoCircle: {
    width: '64px',
    height: '64px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, var(--primary), var(--tertiary))',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    boxShadow: '0 4px 12px rgba(99, 102, 241, 0.4)'
  },
  sidebar: {
    position: 'fixed',
    top: 0,
    left: 0,
    bottom: 0,
    width: '260px',
    padding: '24px 16px',
    display: 'flex',
    flexDirection: 'column',
    gap: '24px',
    borderRight: '1px solid rgba(255,255,255,0.06)',
    borderRadius: 0,
    zIndex: 100,
    '@media (max-width: 768px)': {
      display: 'none'
    }
  },
  brand: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    paddingLeft: '8px'
  },
  brandIconCircle: {
    width: '36px',
    height: '36px',
    borderRadius: '8px',
    background: 'linear-gradient(135deg, var(--primary), var(--tertiary))',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  },
  brandName: {
    fontSize: '18px',
    fontWeight: '800'
  },
  nav: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    flex: 1
  },
  link: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px 16px',
    borderRadius: '0 12px 12px 0',
    border: 'none',
    textAlign: 'left',
    fontWeight: '600',
    fontSize: '14px',
    cursor: 'pointer',
    transition: 'all 0.2s'
  },
  logoutBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '12px 16px',
    background: 'none',
    border: 'none',
    color: 'var(--error)',
    fontWeight: '600',
    fontSize: '14px',
    cursor: 'pointer',
    alignSelf: 'flex-start'
  },
  mobileHeader: {
    display: 'none',
    '@media (max-width: 768px)': {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      position: 'fixed',
      top: 0, left: 0, right: 0,
      height: '60px',
      background: 'rgba(15, 23, 42, 0.95)',
      borderBottom: '1px solid var(--border)',
      padding: '0 20px',
      zIndex: 90
    }
  },
  mobileBrand: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px'
  },
  menuBtn: {
    background: 'none',
    border: 'none',
    color: 'var(--text-main)',
    cursor: 'pointer'
  },
  mobileOverlay: {
    position: 'fixed',
    top: '60px', left: 0, right: 0, bottom: 0,
    background: '#0f172a',
    zIndex: 80,
    padding: '24px'
  },
  mobileNav: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  mobileLink: {
    padding: '14px 0',
    background: 'none',
    border: 'none',
    borderBottom: '1px solid var(--border)',
    textAlign: 'left',
    fontWeight: '700',
    fontSize: '16px',
    cursor: 'pointer',
    width: '100%'
  },
  mobileLogoutBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '16px 0',
    background: 'none',
    border: 'none',
    color: 'var(--error)',
    fontWeight: '700',
    fontSize: '16px',
    cursor: 'pointer',
    width: '100%'
  },
  mainContent: {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    boxSizing: 'border-box',
    '@media (max-width: 768px)': {
      width: '100% !important',
      paddingLeft: '0 !important',
      paddingTop: '60px'
    }
  }
};
