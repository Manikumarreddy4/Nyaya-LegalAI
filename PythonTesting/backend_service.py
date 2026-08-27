import uvicorn
from fastapi import FastAPI, Header, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, EmailStr
from typing import List, Optional
import time

app = FastAPI(title="Nyaya LegalAI Backend Audit Hub", version="2.0.0")

# Setup CORS middleware with custom policies to simulate security audits
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Permits audit checking for permissive CORS
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Request Models
class UserSignup(BaseModel):
    email: EmailStr
    password: str
    phone: str
    role: str  # CLIENT or ADVOCATE

class UserLogin(BaseModel):
    email: EmailStr
    password: str

class ChatQuery(BaseModel):
    message: str
    session_id: Optional[str] = None

class BookingRequest(BaseModel):
    advocate_id: str
    slot: str
    client_name: str

# In-Memory Database Simulation
USERS_DB = {}
CHAT_HISTORY = []
BOOKINGS = []

@app.get("/")
def read_root():
    return {"status": "HEALTHY", "service": "Nyaya LegalAI API Gateway", "timestamp": time.time()}

@app.post("/api/auth/signup")
def signup(user: UserSignup):
    if user.email in USERS_DB:
        raise HTTPException(status_code=400, detail="User already exists")
    if len(user.password) < 6:
        raise HTTPException(status_code=400, detail="Password must be at least 6 characters")
    USERS_DB[user.email] = user
    return {"status": "SUCCESS", "message": "User registered successfully", "email": user.email}

@app.post("/api/auth/login")
def login(user: UserLogin):
    if user.email not in USERS_DB or USERS_DB[user.email].password != user.password:
        raise HTTPException(status_code=401, detail="Invalid credentials")
    return {"status": "SUCCESS", "token": "audit_jwt_token_stub_" + user.email.replace("@", "_")}

@app.post("/api/auth/validate")
def validate_token(authorization: str = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Unauthorized")
    token = authorization.split(" ")[1]
    if "audit_jwt_token_stub_" not in token:
        raise HTTPException(status_code=401, detail="Invalid signature")
    return {"status": "VALID", "user": token.replace("audit_jwt_token_stub_", "").replace("_", "@")}

@app.post("/api/chat")
def post_chat(query: ChatQuery, authorization: str = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Unauthorized")
    
    # Check for SQL Injection probe
    if "SELECT" in query.message or "UNION" in query.message or "'" in query.message:
        return {"status": "AUDIT_BLOCKED", "type": "SQLI", "response": "Malicious payload detected and sanitized by AI Shield."}
    
    # Check for XSS probe
    if "<script>" in query.message or "javascript:" in query.message:
        return {"status": "AUDIT_BLOCKED", "type": "XSS", "response": "Malicious script tags removed."}
        
    response_text = f"Parsed legal scenario. Relevant provisions applied for scenario: '{query.message}'"
    CHAT_HISTORY.append({"query": query.message, "response": response_text})
    return {"status": "SUCCESS", "answer": response_text}

@app.get("/api/bookings/slots")
def get_slots():
    return {"slots": ["09:00 AM", "11:00 AM", "02:00 PM", "04:00 PM"]}

@app.post("/api/bookings")
def create_booking(booking: BookingRequest, authorization: str = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Unauthorized")
    if booking.slot not in ["09:00 AM", "11:00 AM", "02:00 PM", "04:00 PM"]:
        raise HTTPException(status_code=400, detail="Invalid slot timing selection")
    BOOKINGS.append(booking)
    return {"status": "SUCCESS", "booking_id": "booking_" + str(len(BOOKINGS))}

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)
