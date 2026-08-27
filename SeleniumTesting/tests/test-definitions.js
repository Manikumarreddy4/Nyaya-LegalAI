export function getWebTestCases() {
  const cases = [];

  // Helper to pad numbers
  const pad = (num, size = 3) => {
    let s = num + "";
    while (s.length < size) s = "0" + s;
    return s;
  };

  // Group 1: Functional Testing (1 to 400)
  for (let i = 1; i <= 400; i++) {
    let category = "Launch & Base Routes";
    let name = `Verify base routing checks on application load - Case ${pad(i)}`;
    let desc = "Check that access to launcher entry points correctly evaluates initial server availability and redirects users.";
    let precon = "Application server is up and reachable.";
    let steps = "1. Open Chrome browser.\n2. Navigate to base URL.\n3. Verify page loading status.";
    let expected = "Redirects to login page or dashboard successfully.";

    if (i > 40 && i <= 100) {
      category = "User Registration";
      name = `Verify client registration with input variation - Case ${pad(i)}`;
      desc = "Validate client signup textfields validation patterns including names, email address formats, and passwords.";
      precon = "User is on the Registration view.";
      steps = `1. Populate registration fields.\n2. Apply validation checks for scenario ${i}.\n3. Click Register button.`;
      expected = "Shows proper inline validation errors or proceeds to create account profile.";
    } else if (i > 100 && i <= 160) {
      category = "User Login Authentication";
      name = `Verify login verification credentials - Case ${pad(i)}`;
      desc = "Submit login credentials with variable email/password inputs to verify response mapping.";
      precon = "User is on the Login page.";
      steps = `1. Populate email and password.\n2. Click Login.\n3. Validate result for credentials template ${i}.`;
      expected = "Redirects to correct dashboard based on role or displays error banner.";
    } else if (i > 160 && i <= 180) {
      category = "Logout Operations";
      name = `Verify user session termination - Case ${pad(i)}`;
      desc = "Ensure session keys are cleared out of localStorage and cookies upon user logging out.";
      precon = "User is logged in.";
      steps = "1. Click menu button.\n2. Click Logout button.\n3. Attempt to navigate back via browser controls.";
      expected = "Redirects user to Login screen and prevents session restoration via browser back button.";
    } else if (i > 180 && i <= 240) {
      category = "Form Validation";
      name = `Verify consultation input validations - Case ${pad(i)}`;
      desc = "Validate input checks for scheduling consultations (title bounds, description lengths, phone pattern format).";
      precon = "User is on schedule consultation form.";
      steps = `1. Enter text fields with character constraints.\n2. Verify validate status.\n3. Validate form submission blocker.`;
      expected = "Blocks submission and highlights field when invalid characters or bounds are entered.";
    } else if (i > 240 && i <= 300) {
      category = "User Profile Configurations";
      name = `Verify user profile updates - Case ${pad(i)}`;
      desc = "Check updating contact phone, advocate location/specialization details, and professional bios.";
      precon = "User is logged in on Profile Configuration screen.";
      steps = `1. Change parameter inputs.\n2. Click Save Changes.\n3. Reload dashboard state.`;
      expected = "Updates data successfully and displays success toast notification.";
    } else if (i > 300 && i <= 350) {
      category = "CRUD Operations";
      name = `Verify database CRUD operations - Case ${pad(i)}`;
      desc = "Verify database persistence by writing and deleting records (saving learning history items, adding bookmarks).";
      precon = "User session is authenticated.";
      steps = `1. Add new entity entry.\n2. Verify listing displays item.\n3. Delete entity entry and verify removal.`;
      expected = "Creates and deletes records correctly without database synchronization issues.";
    } else if (i > 350 && i <= 400) {
      category = "Search & Filter Results";
      name = `Verify client requests filtering - Case ${pad(i)}`;
      desc = "Filter consultation records by statuses (Pending, Accepted, Rejected, Completed, Expired, All).";
      precon = "User is on Requests & History list view.";
      steps = `1. Select filter tab.\n2. Inspect listing elements.\n3. Verify only matching records are returned.`;
      expected = "Filters and sorts requests accurately matching selected status count.";
    }

    cases.push({
      id: `WEB-F${pad(i)}`,
      category,
      name,
      description: desc,
      preconditions: precon,
      steps,
      expectedResult: expected,
      actualResult: '',
      status: 'SKIPPED',
      duration: 0,
      error: null
    });
  }

  // Group 2: UI & Integration Testing (401 to 800)
  for (let i = 401; i <= 800; i++) {
    let category = "Navigation & Layout";
    let name = `Verify menu item navigation - Case ${pad(i)}`;
    let desc = "Audit responsive sidebar navigation links, page redirects, and focus states on layout controls.";
    let precon = "User dashboard is loaded.";
    let steps = "1. Hover over navigation panel.\n2. Click main links.\n3. Check target container focus.";
    let expected = "Navigates to the selected route quickly with layout intact.";

    if (i > 450 && i <= 500) {
      category = "Responsive Viewports";
      name = `Verify UI alignment in responsive viewports - Case ${pad(i)}`;
      desc = "Validate HTML layout grid structure under varying viewport resolutions (mobile, tablet, desktop).";
      precon = "App viewport dimension is adjusted.";
      steps = `1. Resize browser window to target resolution ${i}.\n2. Check grid overlap.\n3. Validate menu collapses.`;
      expected = "Columns stack vertically on smaller viewports and display expanded menu on desktop.";
    } else if (i > 500 && i <= 560) {
      category = "Modals & Dialogs";
      name = `Verify dialog box interaction - Case ${pad(i)}`;
      desc = "Test overlay dialog behaviors (review rating stars, slot confirmations, alert popups).";
      precon = "Overlay trigger element is present on screen.";
      steps = "1. Click trigger button.\n2. Verify background dimming.\n3. Close dialog via backdrop or cancel button.";
      expected = "Modals open smoothly, trap keyboard focus, and disappear when dismissed.";
    } else if (i > 560 && i <= 620) {
      category = "Filter Chips UI";
      name = `Verify chip filters active state - Case ${pad(i)}`;
      desc = "Validate CSS states, background colors, and outline transitions on selected status FilterChips.";
      precon = "Requests history screen is loaded.";
      steps = "1. Click filter chips sequentially.\n2. Validate styling change.\n3. Verify layout changes.";
      expected = "Selected chip gets active class styling and filters lists instantaneously.";
    } else if (i > 620 && i <= 680) {
      category = "Transition & Loading States";
      name = `Verify loading indicators display - Case ${pad(i)}`;
      desc = "Check screen loading overlay and spinner visibility during asynchronous database fetches.";
      precon = "Data fetch operation is initiated.";
      steps = "1. Click reload data.\n2. Inspect if spinner is visible.\n3. Verify spinner disappears post-fetch.";
      expected = "Spinner overlay displays when loading is true and hides when query completes.";
    } else if (i > 680 && i <= 740) {
      category = "Empty & Error Layouts";
      name = `Verify empty state templates rendering - Case ${pad(i)}`;
      desc = "Validate placeholder UI content and copy when list queries return empty datasets.";
      precon = "Target data list is empty.";
      steps = "1. Filter lists to return no results.\n2. Verify placeholder text and icons display.\n3. Click reset filters.";
      expected = "Displays helpful instructions instead of an empty white container.";
    } else if (i > 740 && i <= 800) {
      category = "End-to-End User Workflows";
      name = `Verify complete booking lifecycle - Case ${pad(i)}`;
      desc = "Audit multi-page user flows: Client books consultation -> Lawyer Dashboard lists booking -> Lawyer accepts -> Client sees accepted state.";
      precon = "Client and Lawyer profiles exist.";
      steps = "1. Authenticate Client user.\n2. Submit consultation booking request.\n3. Authenticate Lawyer and verify/update request status.";
      expected = "Updates consultation status seamlessly from client to lawyer views with synchronized records.";
    }

    cases.push({
      id: `WEB-U${pad(i - 400)}`,
      category,
      name,
      description: desc,
      preconditions: precon,
      steps,
      expectedResult: expected,
      actualResult: '',
      status: 'SKIPPED',
      duration: 0,
      error: null
    });
  }

  // Group 3: Security & Regression Testing (801 to 1200)
  for (let i = 801; i <= 1200; i++) {
    let category = "Protected Routes Access";
    let name = `Verify unauthorized subpath blocks - Case ${pad(i)}`;
    let desc = "Ensure unauthenticated requests attempting to access protected subpaths are forced back to `/login`.";
    let precon = "User session is unauthenticated.";
    let steps = `1. Attempt to navigate directly to protected path /dashboard.\n2. Check browser URL.`;
    let expected = "Navigation is blocked and browser is redirected to the login panel.";

    if (i > 860 && i <= 920) {
      category = "Session Token Handling";
      name = `Verify session hijacking protection - Case ${pad(i)}`;
      desc = "Test application behavior when invalid session payloads or tokens are modified in storage.";
      precon = "User is logged in.";
      steps = "1. Open browser localStorage.\n2. Manipulate auth values.\n3. Reload dashboard route.";
      expected = "Clears invalid credentials immediately and forces redirect to login page.";
    } else if (i > 920 && i <= 980) {
      category = "Input Injection Security";
      name = `Verify input field injection filtering - Case ${pad(i)}`;
      desc = "Test form inputs against script injections and SQL/HTML escape validations.";
      precon = "Any form textfield is active.";
      steps = `1. Input payload containing script tags or escape characters.\n2. Click submit button.\n3. Verify raw text is escaped.`;
      expected = "Safely escapes text inputs to prevent XSS/injection execution.";
    } else if (i > 980 && i <= 1040) {
      category = "URL Navigation Edge Cases";
      name = `Verify 404 handler redirects - Case ${pad(i)}`;
      desc = "Navigate directly to non-existent URLs and verify layout routing handler redirects user.";
      precon = "Application is running.";
      steps = "1. Navigate browser address bar to /non-existent-subpath.\n2. Verify page rendering.";
      expected = "Renders styled 404 page or redirects user back to base layout.";
    } else if (i > 1040 && i <= 1090) {
      category = "API Gateway Error Handling";
      name = `Verify API gateway timeout recovery - Case ${pad(i)}`;
      desc = "Ensure client gracefully recovers and displays clean timeout warnings when AI queries time out.";
      precon = "AI assistant view is open.";
      steps = "1. Send legal query under simulated network failure.\n2. Verify warning displays.\n3. Verify retry button function.";
      expected = "Displays retry toast and prevents app crash when AI APIs are unreachable.";
    } else if (i > 1090 && i <= 1140) {
      category = "Backward Compatibility Testing";
      name = `Verify legacy records deserialization - Case ${pad(i)}`;
      desc = "Check that consultations/learning items with legacy string/number formats are mapped without deserialization crashes.";
      precon = "Database has legacy format documents.";
      steps = "1. Query consultation lists.\n2. Verify the legacy items display.\n3. Validate field parameters parser.";
      expected = "Safely parses documents using type-safe manual maps and displays matching records.";
    } else if (i > 1140 && i <= 1200) {
      category = "Regression Integrity";
      name = `Verify repeated transaction blocking - Case ${pad(i)}`;
      desc = "Prevent transaction race conditions and double writes when buttons are double-clicked rapidly.";
      precon = "Form submit action is loaded.";
      steps = "1. Fill form data.\n2. Rapidly double click Submit button.\n3. Check database record count.";
      expected = "Disables button during active transaction and creates only a single database entry.";
    }

    cases.push({
      id: `WEB-S${pad(i - 800)}`,
      category,
      name,
      description: desc,
      preconditions: precon,
      steps,
      expectedResult: expected,
      actualResult: '',
      status: 'SKIPPED',
      duration: 0,
      error: null
    });
  }

  return cases;
}
