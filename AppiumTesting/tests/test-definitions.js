export function getMobileTestCases() {
  const cases = [];

  const pad = (num, size = 3) => {
    let s = num + "";
    while (s.length < size) s = "0" + s;
    return s;
  };

  // Group 1: Functional Testing (1 to 400)
  for (let i = 1; i <= 400; i++) {
    let category = "Android Launcher & Splash";
    let name = `Verify app launches successfully - Case ${pad(i)}`;
    let desc = "Launch the apk, render splash layout, and verify direct routing to onboarding or login screen.";
    let precon = "Apk is installed on emulator.";
    let steps = "1. Launch Appium session.\n2. Start MainActivity.\n3. Check splash screen node existence.";
    let expected = "Onboarding/login activity loads within expected time constraints.";

    if (i > 40 && i <= 100) {
      category = "Onboarding & Registration";
      name = `Verify client signup flow validations - Case ${pad(i)}`;
      desc = "Validate input fields for names, email addresses, 10-digit phone patterns, and passwords.";
      precon = "User is on the Registration activity.";
      steps = `1. Populate registration fields.\n2. Apply validation checks for scenario ${i}.\n3. Tap Register button.`;
      expected = "Shows proper inline helper warnings or creates profile document.";
    } else if (i > 100 && i <= 180) {
      category = "Authentication Login";
      name = `Verify credentials login verification - Case ${pad(i)}`;
      desc = "Verify sign-in authentication, capturing success/error messages for credentials template.";
      precon = "User is on Login activity.";
      steps = `1. Input credentials.\n2. Tap Login button.\n3. Verify role dashboard redirection.`;
      expected = "Logs in user and redirects to correct lawyer/client dashboard layout.";
    } else if (i > 180 && i <= 240) {
      category = "Logout & Session Clearance";
      name = `Verify mobile session logout - Case ${pad(i)}`;
      desc = "Ensure login session state is cleared out of SharedPreferences upon logging out.";
      precon = "User is logged in.";
      steps = "1. Open navigation menu.\n2. Tap Logout.\n3. Attempt back button redirection.";
      expected = "Clears credentials, loads Login activity, and blocks back button navigation.";
    } else if (i > 240 && i <= 300) {
      category = "Profile Management";
      name = `Verify advocate profile updates - Case ${pad(i)}`;
      desc = "Update advocate locations, specialization categories, fee structures, and professional experience years.";
      precon = "Advocate profile screen is open.";
      steps = `1. Change parameter inputs.\n2. Tap Update.\n3. Verify new settings state.`;
      expected = "Saves profile updates to database and reloads matching dashboard fields.";
    } else if (i > 300 && i <= 360) {
      category = "CRUD Data Operations";
      name = `Verify local settings CRUD - Case ${pad(i)}`;
      desc = "Verify database persistence by writing and deleting local consultation bookmark records.";
      precon = "User session is active.";
      steps = `1. Save new legal bookmark.\n2. Verify listing displays entry.\n3. Delete bookmark.`;
      expected = "Saves and removes records successfully without synchronization lags.";
    } else if (i > 360 && i <= 400) {
      category = "Consultation Filters";
      name = `Verify request category filters - Case ${pad(i)}`;
      desc = "Filter consultation records by statuses (Pending, Accepted, Rejected, Completed, Expired, All).";
      precon = "Requests history dashboard is loaded.";
      steps = `1. Tap Filter tab.\n2. Inspect listing rows.\n3. Check matching counts.`;
      expected = "Applies filters and updates visible items matching selected criteria.";
    }

    cases.push({
      id: `AND-F${pad(i)}`,
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
    let category = "App Navigation Layout";
    let name = `Verify menu item navigation - Case ${pad(i)}`;
    let desc = "Navigate bottom navigation bar items and check content transitions.";
    let precon = "Client dashboard is open.";
    let steps = "1. Tap bottom navigation items.\n2. Verify active item styling.\n3. Check target container rendering.";
    let expected = "Transitions smoothly to the selected page viewport.";

    if (i > 450 && i <= 500) {
      category = "Scrolling & Recycler Views";
      name = `Verify recycler view scrolling - Case ${pad(i)}`;
      desc = "Validate vertical scroll performance, item view recycling, and cell click triggers.";
      precon = "List page contains items.";
      steps = `1. Perform vertical swipe scroll.\n2. Tap visible list cell ${i}.\n3. Verify details screen opens.`;
      expected = "Scrolls without stuttering and opens details view on cell tap.";
    } else if (i > 500 && i <= 560) {
      category = "Dialog overlays & Dialogs";
      name = `Verify overlay dialog interactions - Case ${pad(i)}`;
      desc = "Test dialog overlays for consultation slot bookings, reviews, and logout confirmations.";
      precon = "Dialog trigger button is on screen.";
      steps = "1. Click trigger button.\n2. Verify dimming backdrop.\n3. Dismiss modal using cancel button.";
      expected = "Opens modal overlays cleanly and traps user touches.";
    } else if (i > 560 && i <= 620) {
      category = "Filter Chip Views";
      name = `Verify FilterChips active states - Case ${pad(i)}`;
      desc = "Validate visual chip focus, outline colors, and text sizes upon selection.";
      precon = "Consultation dashboard screen is active.";
      steps = "1. Tap filter chips sequentially.\n2. Verify CSS highlight change.\n3. Check list item refresh.";
      expected = "Applies active state highlight to selected chip and updates items list.";
    } else if (i > 620 && i <= 680) {
      category = "App Loading Indicators";
      name = `Verify progress dialog indicator - Case ${pad(i)}`;
      desc = "Ensure modal progress indicators display during database fetch delays.";
      precon = "Asynchronous request is queued.";
      steps = "1. Initiate reload.\n2. Verify spinner dialog display.\n3. Verify spinner hides on response.";
      expected = "Displays loading indicator dialog and hides it upon query return.";
    } else if (i > 680 && i <= 740) {
      category = "Empty and Error Views";
      name = `Verify empty state widgets - Case ${pad(i)}`;
      desc = "Verify descriptive placeholders render when list queries return no result objects.";
      precon = "Selected history list is empty.";
      steps = "1. Trigger empty query.\n2. Check empty placeholder elements.\n3. Verify reset button.";
      expected = "Renders helpful onboarding descriptions and reset triggers.";
    } else if (i > 740 && i <= 800) {
      category = "E2E Mobile Integration Workflows";
      name = `Verify mobile booking flow - Case ${pad(i)}`;
      desc = "Audit complete client consult booking cycle through mobile app screens.";
      precon = "Client and advocate profiles exist.";
      steps = "1. Log in client.\n2. Submit consultation booking request.\n3. Verify dashboard pending counts update.";
      expected = "Saves consultation request and updates client dashboard counts instantly.";
    }

    cases.push({
      id: `AND-U${pad(i - 400)}`,
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
    let category = "Security Screen Access";
    let name = `Verify unauthorized screen locks - Case ${pad(i)}`;
    let desc = "Ensure unauthorized screen transitions redirect back to login.";
    let precon = "User is unauthenticated.";
    let steps = `1. Attempt to launch dashboard activity directly.\n2. Check visible screen ID.`;
    let expected = "Blocks launch activity and opens login activity layout.";

    if (i > 860 && i <= 920) {
      category = "Data Storage Security";
      name = `Verify encrypted preferences security - Case ${pad(i)}`;
      desc = "Verify credentials payloads are safely encrypted and isolated from public read access.";
      precon = "Client credentials saved locally.";
      steps = "1. Verify storage folder access rights.\n2. Check credentials format in xml.";
      expected = "Restricts shared preference folder access to app process ownership.";
    } else if (i > 920 && i <= 980) {
      category = "Input Form Sanitization";
      name = `Verify text fields sanitization - Case ${pad(i)}`;
      desc = "Verify text fields escape script inputs and SQL commands safely.";
      precon = "Text input edit widget is active.";
      steps = `1. Input injection string.\n2. Tap save button.\n3. Verify stored database string.`;
      expected = "Stores string safely as raw text without execution or database issues.";
    } else if (i > 980 && i <= 1040) {
      category = "System Back Buttons";
      name = `Verify navigation stack integrity - Case ${pad(i)}`;
      desc = "Check system back navigation handles layout stacks without exiting the app.";
      precon = "User navigated to sub-screen.";
      steps = "1. Press system back button.\n2. Verify current view.\n3. Check activity stack index.";
      expected = "Pops current view from stack and displays previous activity.";
    } else if (i > 1040 && i <= 1090) {
      category = "Network State Failures";
      name = `Verify offline recovery layouts - Case ${pad(i)}`;
      desc = "Ensure client renders offline retry dialogs when internet access drops.";
      precon = "App is running offline.";
      steps = "1. Trigger consult search.\n2. Check offline warning dialog.\n3. Re-enable network and tap retry.";
      expected = "Prompts helpful warning dialog and recovers gracefully on network restore.";
    } else if (i > 1090 && i <= 1140) {
      category = "Legacy Deserialization Safety";
      name = `Verify backward compatibility - Case ${pad(i)}`;
      desc = "Check that old consultation formats (string/number values) are parsed safely by repositories.";
      precon = "Database has old layout documents.";
      steps = "1. Retrieve list of consultations.\n2. Inspect legacy elements.\n3. Check parse validations.";
      expected = "Safely parses documents using type-safe manual maps and displays matching records.";
    } else if (i > 1140 && i <= 1200) {
      category = "Mobile Regression Auditing";
      name = `Verify double-tap transaction blocks - Case ${pad(i)}`;
      desc = "Verify transaction race conditions and double requests are blocked on rapid button double-taps.";
      precon = "Submit button is active.";
      steps = "1. Tap submit button rapidly twice.\n2. Check API call count logs.";
      expected = "Disables widget interaction during transaction and sends single API call.";
    }

    cases.push({
      id: `AND-S${pad(i - 800)}`,
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
