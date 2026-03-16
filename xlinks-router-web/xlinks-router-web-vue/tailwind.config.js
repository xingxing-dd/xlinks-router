/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#7f22fe',
          foreground: '#ffffff',
        },
        secondary: {
          DEFAULT: '#c800de',
          foreground: '#ffffff',
        },
        muted: {
          DEFAULT: '#62748e',
          foreground: '#314158',
        },
        border: '#cad5e2',
        input: '#cad5e2',
        ring: '#7f22fe',
        background: '#ffffff',
        foreground: '#0f172b',
      },
      borderRadius: {
        lg: '24px',
        md: '16px',
        sm: '14px',
      },
      backgroundImage: {
        'gradient-main': 'linear-gradient(119.06deg, #0f172b 0%, #59168b 50%, #0f172b 100%)',
        'gradient-button': 'linear-gradient(90deg, #7f22fe 0%, #c800de 100%)',
        'gradient-icon': 'linear-gradient(135deg, #8e51ff 0%, #e12afb 100%)',
      },
    },
  },
  plugins: [],
}
