import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';

const firebaseConfig = {
  apiKey: "AIzaSyDwBzBjYUqTl0uBwoM7JXjlNRGmM60B5sQ",
  authDomain: "legal-ai-a42f0.firebaseapp.com",
  projectId: "legal-ai-a42f0",
  storageBucket: "legal-ai-a42f0.firebasestorage.app",
  messagingSenderId: "71341451975",
  appId: "1:71341451975:web:e78570e481a1c8feabace2"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
export default app;
