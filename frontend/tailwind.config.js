/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#1e293b',
        secondary: '#64748b',
        accent: '#0ea5e9',
      },
    },
  },
  plugins: [],
  important: true,
}
