/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#6366f1',
          50: '#eef2ff',
          100: '#e0e7ff',
          200: '#c7d2fe',
          300: '#a5b4fc',
          400: '#818cf8',
          500: '#6366f1',
          600: '#4f46e5',
          700: '#4338ca',
          800: '#3730a3',
          900: '#312e81',
        },
        secondary: {
          DEFAULT: '#0ea5e9',
        },
      },
      boxShadow: {
        'soft': '0 12px 30px rgba(15, 23, 42, 0.08)',
      },
      backgroundImage: {
        'gradient-main': 'linear-gradient(135deg, #f8fafc 0%, #eef2ff 45%, #f8fafc 100%)',
        'gradient-button': 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)',
        'gradient-icon': 'linear-gradient(135deg, #6366f1 0%, #0ea5e9 100%)',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
}
