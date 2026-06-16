import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material'
import App from './App'
import { AuthProvider } from './AuthContext'
import './styles.css'

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#9b4dca' },
    secondary: { main: '#e06c9f' },
    background: { default: '#fbf8fc', paper: '#ffffff' },
  },
  typography: {
    fontFamily: '"Inter", "Segoe UI", sans-serif',
    h1: { fontFamily: '"Georgia", serif', fontWeight: 500 },
    h2: { fontFamily: '"Georgia", serif', fontWeight: 500 },
  },
  shape: { borderRadius: 16 },
})

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  </StrictMode>,
)
