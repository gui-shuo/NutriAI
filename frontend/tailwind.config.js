/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}'
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Hand-drawn design palette
        paper: '#fdfbf7',
        pencil: '#2d2d2d',
        muted: '#e5e0d8',
        accent: '#ff4d4d',
        ink: '#2d5da1',
        sticky: '#fff9c4',
        // Keep primary/secondary for compatibility
        primary: {
          50: '#fef2f2',
          100: '#fee2e2',
          200: '#fecaca',
          300: '#fca5a5',
          400: '#ff6b6b',
          500: '#ff4d4d',
          600: '#dc2626',
          700: '#b91c1c',
          800: '#991b1b',
          900: '#7f1d1d'
        },
        secondary: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#2d5da1',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a5f'
        }
      },
      fontFamily: {
        heading: ['Kalam', 'ZCOOL KuaiLe', 'PingFang SC', 'Microsoft YaHei', 'cursive'],
        hand: ['Patrick Hand', 'PingFang SC', 'Microsoft YaHei', 'cursive'],
        sans: ['Patrick Hand', 'PingFang SC', 'Microsoft YaHei', 'Hiragino Sans GB', 'sans-serif'],
        mono: ['Fira Code', 'Consolas', 'Monaco', 'monospace']
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
        '128': '32rem'
      },
      borderRadius: {
        'wobbly': '255px 15px 225px 15px / 15px 225px 15px 255px',
        'wobbly-md': '15px 225px 15px 255px / 255px 15px 225px 15px',
        'wobbly-sm': '185px 10px 155px 10px / 10px 155px 10px 185px',
        'xl': '12px',
        '2xl': '16px',
        '3xl': '24px'
      },
      boxShadow: {
        'hard': '4px 4px 0px 0px #2d2d2d',
        'hard-lg': '8px 8px 0px 0px #2d2d2d',
        'hard-sm': '3px 3px 0px 0px rgba(45, 45, 45, 0.1)',
        'hard-hover': '2px 2px 0px 0px #2d2d2d',
        'hard-accent': '4px 4px 0px 0px #ff4d4d',
        'soft': '0 2px 12px rgba(0, 0, 0, 0.08)',
        'medium': '0 4px 16px rgba(0, 0, 0, 0.12)',
        'hover': '0 8px 24px rgba(76, 175, 80, 0.2)'
      },
      animation: {
        'wiggle': 'wiggle 0.3s ease-in-out',
        'bounce-slow': 'bounce 3s infinite',
      },
      keyframes: {
        wiggle: {
          '0%, 100%': { transform: 'rotate(-1deg)' },
          '50%': { transform: 'rotate(1deg)' },
        }
      }
    }
  },
  plugins: []
}
