import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
app.use(cors());
app.use(express.json());

// Helper to read local.properties
function getKeysFromLocalProperties() {
  const keys = {};
  try {
    const propPath = path.join(__dirname, '../local.properties');
    if (fs.existsSync(propPath)) {
      const content = fs.readFileSync(propPath, 'utf-8');
      content.split('\n').forEach(line => {
        const parts = line.split('=');
        if (parts.length >= 2) {
          keys[parts[0].trim()] = parts.slice(1).join('=').trim();
        }
      });
    }
  } catch (e) {
    console.error('Error reading local.properties', e);
  }
  return keys;
}

const localKeys = getKeysFromLocalProperties();
// Prioritize GROQ_API_KEY from environment variables as requested
const GROQ_API_KEY = process.env.GROQ_API_KEY || localKeys.GROQ_API_KEY;
const GROQ_LEARNING_API_KEY = process.env.GROQ_LEARNING_API_KEY || localKeys.GROQ_LEARNING_API_KEY || GROQ_API_KEY;
const GROQ_ASSISTANT_API_KEY = process.env.GROQ_ASSISTANT_API_KEY || localKeys.GROQ_ASSISTANT_API_KEY || GROQ_API_KEY;

// Health check / baseline endpoint
app.get('/', (req, res) => {
  res.status(200).json({ status: "healthy", message: "Nyaya Legal AI API Server is running" });
});

// Unified Chat / AI Assistant & Learning Endpoint
app.post('/api/chat', async (req, res) => {
  const { message, conversation, isLearning } = req.body;

  // Print request debug logs
  console.log('[AI] Request received');
  console.log('[AI] Message:', message);
  console.log('[AI] Conversation history length:', Array.isArray(conversation) ? conversation.length : 0);

  if (!message || typeof message !== 'string' || !message.trim()) {
    console.error('[AI] Error: Empty message received');
    return res.status(400).json({
      success: false,
      error: 'Message parameter is required and cannot be empty.'
    });
  }

  // Select system prompt and API Key dynamically based on type of request
  const apiKey = isLearning ? GROQ_LEARNING_API_KEY : GROQ_ASSISTANT_API_KEY;
  if (!apiKey) {
    const keyErr = 'AI service unavailable: Groq API Key is not configured on the server. Please add GROQ_API_KEY to environment or local.properties.';
    console.error('[AI] Error:', keyErr);
    return res.status(500).json({
      success: false,
      error: keyErr
    });
  }

  // Custom system prompts matching user requirements
  const assistantSystemPrompt = `You are Nyaya Legal AI, an Indian legal information assistant.

STRICT RULE: You must ONLY answer questions related to law, legal issues, legal education, Indian laws, Indian constitution, legal procedures, rights, courts, cases, sections, acts, and legal services.
If the user asks something completely unrelated to law (such as programming, software engineering, languages like Java/Python, mathematics, science, sports, entertainment, coding help, writing poems, general knowledge, etc.), you MUST reject the query.
When rejecting a non-legal query, output EXACTLY the following message (or a close variation):
"I’m Nyaya Legal AI, a legal-focused assistant. I can only help with legal questions, Indian laws, legal rights, legal procedures, and related topics. Please ask a law-related question."

DO NOT explain programming, science, general knowledge, or write poems.

You MUST accept and answer valid short legal questions (like "IPC 302", "BNS 103", "Section 420", "BNS Section 103", "Article 21", "Article 14", "FIR", "Bail", "Divorce") and legal intent descriptions (such as unpaid salary, cybercrime, landlord disputes, arrest queries). Do not say "Please ask a longer question" or refuse useful short legal queries.

For legal questions, provide a professional, easy-to-understand response with:
1. Legal Provision (identify the Act/Law)
2. Simple Explanation
3. Key Elements
4. Punishment / Penalty (only if applicable)
5. Example (if useful)
6. Related Provisions
7. Practical Information / Next Steps
8. Important Note / Disclaimer (State that the information is for educational purposes and is not a substitute for professional legal advice).`;

  const learningSystemPrompt = `You are the Legal Learning assistant for Nyaya Legal AI.

STRICT RULE: Your sole purpose is to teach the user about law in a simple and educational way. Explain legal concepts, Indian laws, Acts, sections, constitutional Articles, legal terminology, legal procedures, court systems, rights, duties, and case-law concepts.
You must ONLY answer questions related to law, legal education, and legal concepts.
If the user asks something completely unrelated to law (such as explaining Java/Python, machine learning, photosynthesis, mathematics, science, writing a story/poem, general knowledge), you MUST reject the query.
When rejecting a non-legal query, output EXACTLY the following message (or a close variation):
"This Legal Learning assistant is designed only for legal education. Please ask me about Indian law, legal concepts, sections, Acts, constitutional provisions, or legal procedures."

DO NOT explain programming, science, general knowledge, or write stories.

You MUST accept and explain valid short legal queries (like "IPC 302", "BNS 103", "Section 420", "Article 21", "FIR", "Bail", "BNS").`;

  const systemPrompt = isLearning ? learningSystemPrompt : assistantSystemPrompt;

  // Build Groq messages list (including system prompt, history and current message)
  const messages = [
    { role: 'system', content: systemPrompt }
  ];

  // Append history (limit to last 10 messages to save context tokens)
  if (Array.isArray(conversation)) {
    const recentHistory = conversation.slice(-10);
    recentHistory.forEach(msg => {
      // Normalizing user/assistant sender fields
      const role = (msg.sender === 'User' || msg.role === 'user') ? 'user' : 'assistant';
      const text = msg.message || msg.content || '';
      if (text.trim()) {
        messages.push({ role, content: text });
      }
    });
  }

  // Append current user message
  messages.push({ role: 'user', content: message.trim() });

  console.log('[AI] Calling Groq');
  try {
    const response = await fetch('https://api.groq.com/openai/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${apiKey}`
      },
      body: JSON.stringify({
        model: 'groq/compound-mini',
        messages,
        temperature: 0.4,
        max_tokens: 1024,
        top_p: 0.9
      })
    });

    console.log('[AI] Groq response status:', response.status);
    const data = await response.json();

    if (response.ok && data.choices && data.choices[0] && data.choices[0].message) {
      const reply = data.choices[0].message.content;
      console.log('[AI] Response received, length:', reply.length);
      res.json({
        success: true,
        reply
      });
    } else {
      const errMsg = data?.error?.message || data?.error || JSON.stringify(data) || 'Unknown Groq Error';
      console.error('[AI] Groq API returned error:', errMsg);
      res.status(response.status || 500).json({
        success: false,
        error: `AI service unavailable: ${errMsg}`
      });
    }
  } catch (error) {
    console.error('[AI] Fetch Exception:', error);
    res.status(500).json({
      success: false,
      error: `AI service unavailable: ${error.message}`
    });
  }
});

// Serve frontend build static files (optional, for deployment preview)
const buildPath = path.join(__dirname, 'dist');
if (fs.existsSync(buildPath)) {
  app.use(express.static(buildPath));
  app.get(/.*/, (req, res) => {
    res.sendFile(path.join(buildPath, 'index.html'));
  });
}

// Server-side signup parameter validation endpoint
app.post('/api/auth/signup/validate', (req, res) => {
  const { phone, password } = req.body;

  if (!phone || typeof phone !== 'string') {
    return res.status(400).json({
      success: false,
      error: "Phone number must contain exactly 10 digits."
    });
  }

  const phonePattern = /^[0-9]{10}$/;
  if (!phonePattern.test(phone.trim())) {
    return res.status(400).json({
      success: false,
      error: "Phone number must contain exactly 10 digits."
    });
  }

  if (!password || typeof password !== 'string') {
    return res.status(400).json({
      success: false,
      error: "Password must contain at least 6 characters, including one uppercase letter, one lowercase letter, one number, and one special character."
    });
  }

  const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{6,}$/;
  if (!passwordPattern.test(password)) {
    return res.status(400).json({
      success: false,
      error: "Password must contain at least 6 characters, including one uppercase letter, one lowercase letter, one number, and one special character."
    });
  }

  return res.status(200).json({
    success: true,
    message: "Validation successful"
  });
});

// Server-side consultation booking parameter validation endpoint
app.post('/api/consultations/validate', (req, res) => {
  const { 
    phone, 
    contactNumber, 
    userId, 
    lawyerId, 
    consultationType, 
    date, 
    time, 
    video_consultation_available, 
    in_person_consultation_available, 
    availability_status 
  } = req.body;

  const targetPhone = phone || contactNumber;

  // 1. User authentication check
  if (!userId) {
    return res.status(401).json({
      success: false,
      error: "User authentication is required."
    });
  }

  // 2. Lawyer exists check
  if (!lawyerId) {
    return res.status(400).json({
      success: false,
      error: "Lawyer profile must be specified."
    });
  }

  // 3. Phone number validation
  if (!targetPhone || typeof targetPhone !== 'string') {
    return res.status(400).json({
      success: false,
      error: "Phone number must contain exactly 10 digits."
    });
  }

  const phonePattern = /^[0-9]{10}$/;
  if (!phonePattern.test(targetPhone.trim())) {
    return res.status(400).json({
      success: false,
      error: "Phone number must contain exactly 10 digits."
    });
  }

  // 4. Consultation type validation
  if (!consultationType || (consultationType !== 'Online' && consultationType !== 'In-Person')) {
    return res.status(400).json({
      success: false,
      error: "Consultation type is invalid."
    });
  }

  // 5. Date validation
  if (!date) {
    return res.status(400).json({
      success: false,
      error: "Selected date is invalid."
    });
  }

  // 6. Time validation
  if (!time) {
    return res.status(400).json({
      success: false,
      error: "Selected time is invalid."
    });
  }

  // 7. Lawyer availability checks
  if (availability_status === false) {
    return res.status(400).json({
      success: false,
      error: "Lawyer is currently unavailable."
    });
  }

  if (consultationType === 'Online' && video_consultation_available === false) {
    return res.status(400).json({
      success: false,
      error: "The selected consultation type is unavailable."
    });
  }

  if (consultationType === 'In-Person' && in_person_consultation_available === false) {
    return res.status(400).json({
      success: false,
      error: "The selected consultation type is unavailable."
    });
  }

  // 8. Appointment time has not already passed and is at least 2 minutes in the future
  if (date && time) {
    const selectedDateTime = new Date(`${date}T${time}`);
    if (isNaN(selectedDateTime.getTime())) {
      return res.status(400).json({
        success: false,
        error: "Selected date or time format is invalid.",
        message: "Selected date or time format is invalid."
      });
    }
    const minAllowedTime = Date.now() + 2 * 60 * 1000;
    if (selectedDateTime.getTime() < minAllowedTime) {
      return res.status(400).json({
        success: false,
        error: "Please select a consultation time at least 2 minutes from now.",
        message: "Please select a consultation time at least 2 minutes from now."
      });
    }
  }

  return res.status(200).json({
    success: true,
    message: "Validation successful"
  });
});

const PORT = process.env.PORT || 5000;
app.listen(PORT, () => {
  console.log(`Nyaya Legal AI Backend server running on port ${PORT}`);
});
