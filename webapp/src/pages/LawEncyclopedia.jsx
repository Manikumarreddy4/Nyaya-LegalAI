import React, { useState, useEffect } from 'react';
import { Book, Search, Filter, HelpCircle, ChevronRight, Loader2, ArrowLeft } from 'lucide-react';

export default function LawEncyclopedia() {
  const [selectedLaw, setSelectedLaw] = useState(null);
  const [loading, setLoading] = useState(false);
  const [sections, setSections] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeSection, setActiveSection] = useState(null);

  const availableLaws = [
    { id: 'constitution', name: 'Constitution of India', desc: 'Supreme law of India (Articles 1-395+)', file: 'constitution_of_india.json' },
    { id: 'bns', name: 'Bharatiya Nyaya Sanhita (BNS 2023)', desc: 'Current Substantive Criminal Law', file: 'bns_en.json' },
    { id: 'bnss', name: 'Bharatiya Nagarik Suraksha Sanhita (BNSS 2023)', desc: 'Current Criminal Procedural Law', file: 'bnss_en.json' },
    { id: 'bsa', name: 'Bharatiya Sakshya Adhiniyam (BSA 2023)', desc: 'Current Law of Evidence', file: 'bsa_en.json' },
    { id: 'ipc', name: 'Indian Penal Code (IPC 1860 - Repealed)', desc: 'Historical Substantive Criminal Law', file: 'ipc.json' },
    { id: 'crpc', name: 'Code of Criminal Procedure (CrPC 1973 - Repealed)', desc: 'Historical Procedural Law', file: 'crpc.json' },
    { id: 'cpc', name: 'Code of Civil Procedure (CPC 1908)', desc: 'Civil Procedure and Suits', file: 'cpc.json' },
    { id: 'mva', name: 'Motor Vehicles Act (MVA)', desc: 'Traffic violations and vehicle regulations', file: 'MVA.json' }
  ];

  const loadLawData = async (law) => {
    setLoading(true);
    setSelectedLaw(law);
    setSearchQuery('');
    setActiveSection(null);
    try {
      const response = await fetch(`/assets/${law.file}`);
      const data = await response.json();
      
      // Normalize different formats
      let normalized = [];
      if (Array.isArray(data)) {
        normalized = data.map((item, idx) => {
          const sectionNum = item.Section || item.section || item.article || (idx + 1);
          const title = item.section_title || item.title || item.heading || '';
          const desc = item.section_desc || item.description || item.text || '';
          const chapter = item.chapter_title || (item.chapter ? `Chapter ${item.chapter}` : '');
          return {
            number: String(sectionNum),
            title: title.trim(),
            description: desc.trim(),
            chapter: chapter.trim()
          };
        });
      } else if (data && Array.isArray(data.sections)) {
        normalized = data.sections.map(item => ({
          number: String(item.section_number),
          title: (item.heading || '').trim(),
          description: (item.text || '').trim(),
          chapter: item.chapter ? ((item.chapter.title || item.chapter.code || '')).trim() : ''
        }));
      }
      setSections(normalized);
    } catch (e) {
      console.error(e);
      alert('Failed to load dataset details: ' + e.message);
      setSelectedLaw(null);
    } finally {
      setLoading(false);
    }
  };

  const filteredSections = sections.filter(s => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return s.number.toLowerCase().includes(q) || 
           s.title.toLowerCase().includes(q) || 
           s.description.toLowerCase().includes(q);
  });

  return (
    <div className="fade-in-up" style={styles.container}>
      {!selectedLaw ? (
        <>
          <div style={styles.header}>
            <Book size={36} color="var(--primary)" />
            <h2>Law Search & Encyclopedia</h2>
            <p style={styles.subtitle}>Browse and search full legal texts and code acts from our comprehensive offline legal database.</p>
          </div>

          <div style={styles.grid}>
            {availableLaws.map((law) => (
              <div 
                key={law.id} 
                className="glass-panel" 
                style={styles.lawCard}
                onClick={() => loadLawData(law)}
              >
                <div style={styles.lawIcon}>
                  <Book size={20} color="var(--primary)" />
                </div>
                <h3 style={styles.lawName}>{law.name}</h3>
                <p style={styles.lawDesc}>{law.desc}</p>
                <ChevronRight size={16} style={styles.cardArrow} />
              </div>
            ))}
          </div>
        </>
      ) : (
        <div style={styles.searchContainer}>
          {/* Back Navigation Bar */}
          <div style={styles.navBar}>
            <button onClick={() => setSelectedLaw(null)} style={styles.backBtn}>
              <ArrowLeft size={16} /> Back to Acts
            </button>
            <h3 style={styles.activeLawTitle}>{selectedLaw.name}</h3>
          </div>

          {/* Search Bar */}
          <div className="glass-panel" style={styles.searchBar}>
            <Search size={18} color="var(--primary)" style={styles.searchIcon} />
            <input 
              type="text" 
              placeholder="Search by Section No. or Keyword (e.g. '302', 'murder', 'bail')..." 
              className="input-field"
              style={styles.searchInput}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          {loading ? (
            <div style={styles.loader}>
              <Loader2 size={28} style={{animation: 'spin 1s linear infinite', color: 'var(--primary)'}} />
              <span>Loading sections...</span>
            </div>
          ) : (
            <div style={styles.resultsWrapper}>
              {/* Sections list sidebar */}
              <div className="glass-panel" style={styles.listSidebar}>
                <div style={styles.sidebarHeader}>Sections ({filteredSections.length})</div>
                <div style={styles.sectionsList}>
                  {filteredSections.map((s, idx) => (
                    <div 
                      key={idx}
                      style={{
                        ...styles.sectionListItem,
                        background: activeSection?.number === s.number ? 'var(--primary)' : 'transparent'
                      }}
                      onClick={() => setActiveSection(s)}
                    >
                      <div style={styles.sectionNumLabel}>Section / Article {s.number}</div>
                      <div style={styles.sectionTitleLabel}>{s.title}</div>
                    </div>
                  ))}
                  {filteredSections.length === 0 && (
                    <div style={styles.emptyList}>No sections match the search.</div>
                  )}
                </div>
              </div>

              {/* Section Details view panel */}
              <div className="glass-panel" style={styles.detailsView}>
                {activeSection ? (
                  <div className="fade-in-up" style={styles.detailsContent}>
                    {activeSection.chapter && (
                      <span style={styles.chapterLabel}>{activeSection.chapter}</span>
                    )}
                    <h2 style={styles.sectionTitle}>Section / Article {activeSection.number}</h2>
                    <h3 style={styles.sectionHeading}>{activeSection.title}</h3>
                    <div style={styles.divider} />
                    <p style={styles.sectionDescription}>{activeSection.description}</p>
                  </div>
                ) : (
                  <div style={styles.emptyDetails}>
                    <HelpCircle size={48} color="var(--border)" style={{marginBottom: '16px'}} />
                    <h3>No Section Selected</h3>
                    <p>Select a section from the list on the left to read its full description.</p>
                  </div>
                )}
              </div>
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
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '8px',
    marginBottom: '32px'
  },
  subtitle: {
    fontSize: '14px',
    color: 'var(--text-muted)',
    maxWidth: '650px',
    lineHeight: '1.6'
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
    gap: '20px'
  },
  lawCard: {
    padding: '24px',
    cursor: 'pointer',
    position: 'relative',
    transition: 'all 0.2s',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    ':hover': {
      borderColor: 'var(--primary)',
      transform: 'translateY(-2px)'
    }
  },
  lawIcon: {
    width: '40px',
    height: '40px',
    borderRadius: '10px',
    background: 'rgba(99, 102, 241, 0.08)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
  },
  lawName: {
    fontSize: '15px',
    fontWeight: '750',
    marginRight: '20px'
  },
  lawDesc: {
    fontSize: '13px',
    color: 'var(--text-muted)',
    lineHeight: '1.5'
  },
  cardArrow: {
    position: 'absolute',
    right: '16px',
    bottom: '24px',
    color: 'var(--text-muted)'
  },
  searchContainer: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px'
  },
  navBar: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingBottom: '8px',
    borderBottom: '1px solid var(--border)'
  },
  backBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    background: 'none',
    border: 'none',
    color: 'var(--primary)',
    fontWeight: '600',
    cursor: 'pointer'
  },
  activeLawTitle: {
    fontSize: '16px',
    fontWeight: '700'
  },
  searchBar: {
    display: 'flex',
    alignItems: 'center',
    padding: '4px 16px',
    position: 'relative'
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
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '12px',
    padding: '60px 0',
    color: 'var(--text-muted)'
  },
  resultsWrapper: {
    display: 'grid',
    gridTemplateColumns: '320px 1fr',
    gap: '20px',
    height: '600px',
    '@media (max-width: 768px)': {
      gridTemplateColumns: '1fr',
      height: 'auto'
    }
  },
  listSidebar: {
    display: 'flex',
    flexDirection: 'column',
    overflow: 'hidden'
  },
  sidebarHeader: {
    padding: '14px 16px',
    borderBottom: '1px solid var(--border)',
    fontSize: '12px',
    fontWeight: '700',
    color: 'var(--text-muted)',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  sectionsList: {
    flex: 1,
    overflowY: 'auto',
    display: 'flex',
    flexDirection: 'column'
  },
  sectionListItem: {
    padding: '12px 16px',
    borderBottom: '1px solid rgba(255, 255, 255, 0.03)',
    cursor: 'pointer',
    transition: 'all 0.2s',
    display: 'flex',
    flexDirection: 'column',
    gap: '4px'
  },
  sectionNumLabel: {
    fontSize: '12px',
    fontWeight: '700',
    color: 'var(--accent)'
  },
  sectionTitleLabel: {
    fontSize: '13px',
    fontWeight: '600',
    color: 'var(--text-main)',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis'
  },
  emptyList: {
    padding: '20px',
    textAlign: 'center',
    color: 'var(--text-muted)',
    fontSize: '13px'
  },
  detailsView: {
    flex: 1,
    padding: '32px',
    overflowY: 'auto'
  },
  detailsContent: {
    display: 'flex',
    flexDirection: 'column',
    gap: '12px'
  },
  chapterLabel: {
    fontSize: '12px',
    fontWeight: '800',
    color: 'var(--primary)',
    textTransform: 'uppercase',
    letterSpacing: '0.5px'
  },
  sectionTitle: {
    fontSize: '24px',
    fontWeight: '800'
  },
  sectionHeading: {
    fontSize: '16px',
    fontWeight: '700',
    color: 'var(--text-muted)'
  },
  divider: {
    height: '1px',
    background: 'var(--border)',
    margin: '8px 0'
  },
  sectionDescription: {
    fontSize: '14px',
    lineHeight: '1.7',
    color: 'var(--text-main)',
    whiteSpace: 'pre-wrap'
  },
  emptyDetails: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    height: '100%',
    color: 'var(--text-muted)',
    textAlign: 'center'
  }
};
