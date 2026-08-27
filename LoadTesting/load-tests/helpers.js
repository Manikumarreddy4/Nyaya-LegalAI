export function generateRandomString(length = 8) {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

export function generateRandomUser() {
  const rand = generateRandomString(5);
  return {
    name: `User_${rand}`,
    email: `user_${rand}@example.com`,
    phone: `98765${Math.floor(10000 + Math.random() * 90000)}`, // 10 digit number
    password: `SecPass_${rand}!1`
  };
}
