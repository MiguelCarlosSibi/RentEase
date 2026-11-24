// Same rules as the backend. The server checks everything again.
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const MOBILE_PATTERN = /^(09\d{9}|\+639\d{9})$/;
export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

export const PASSWORD_RULE = 'Password must be at least 8 characters and contain a letter and a number';
export const MOBILE_RULE = 'Enter a valid Philippine mobile number (09XXXXXXXXX or +639XXXXXXXXX)';
