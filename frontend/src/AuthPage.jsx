import { useState } from 'react'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Container,
  Link,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { Link as RouterLink, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { getApiError } from './api'
import { useAuth } from './auth-context'

const emptyRegister = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  password: '',
}

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (user) {
    return <Navigate to="/profile" replace />
  }

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(form)
      navigate(location.state?.from || '/profile', { replace: true })
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось войти. Проверьте данные.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout title="С возвращением" subtitle="Войдите в личный кабинет">
      <Box component="form" onSubmit={submit}>
        <Stack spacing={2.5}>
          {error && <Alert severity="error">{error}</Alert>}
          <TextField
            label="Email"
            type="email"
            required
            autoComplete="email"
            value={form.email}
            onChange={(event) => setForm({ ...form, email: event.target.value })}
          />
          <TextField
            label="Пароль"
            type="password"
            required
            autoComplete="current-password"
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
          />
          <Button type="submit" size="large" variant="contained" disabled={submitting}>
            {submitting ? <CircularProgress size={24} color="inherit" /> : 'Войти'}
          </Button>
          <Typography textAlign="center" color="text.secondary">
            Нет аккаунта?{' '}
            <Link component={RouterLink} to="/register">Зарегистрироваться</Link>
          </Typography>
        </Stack>
      </Box>
    </AuthLayout>
  )
}

export function RegisterPage() {
  const { user, register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(emptyRegister)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (user) {
    return <Navigate to="/profile" replace />
  }

  const change = (field) => (event) => {
    setForm({ ...form, [field]: event.target.value })
  }

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const result = await register(form)
      if (result.verificationToken) {
        navigate('/profile', { replace: true })
      } else {
        navigate('/login', {
          replace: true,
          state: { message: 'Проверьте почту и подтвердите регистрацию.' },
        })
      }
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось зарегистрироваться.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout title="Начните танцевать" subtitle="Создайте аккаунт клиента">
      <Box component="form" onSubmit={submit}>
        <Stack spacing={2.5}>
          {error && <Alert severity="error">{error}</Alert>}
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TextField
              fullWidth
              label="Имя"
              required
              value={form.firstName}
              onChange={change('firstName')}
            />
            <TextField
              fullWidth
              label="Фамилия"
              required
              value={form.lastName}
              onChange={change('lastName')}
            />
          </Stack>
          <TextField
            label="Email"
            type="email"
            required
            autoComplete="email"
            value={form.email}
            onChange={change('email')}
          />
          <TextField
            label="Телефон"
            type="tel"
            autoComplete="tel"
            value={form.phone}
            onChange={change('phone')}
          />
          <TextField
            label="Пароль"
            type="password"
            required
            autoComplete="new-password"
            helperText="Минимум 8 символов, одна заглавная буква и одна цифра"
            value={form.password}
            onChange={change('password')}
          />
          <Button type="submit" size="large" variant="contained" disabled={submitting}>
            {submitting ? <CircularProgress size={24} color="inherit" /> : 'Создать аккаунт'}
          </Button>
          <Typography textAlign="center" color="text.secondary">
            Уже есть аккаунт? <Link component={RouterLink} to="/login">Войти</Link>
          </Typography>
        </Stack>
      </Box>
    </AuthLayout>
  )
}

function AuthLayout({ title, subtitle, children }) {
  return (
    <Box className="auth-background">
      <Container maxWidth="sm">
        <Paper elevation={0} className="auth-card">
          <Typography variant="h3" textAlign="center">{title}</Typography>
          <Typography color="text.secondary" textAlign="center" sx={{ mt: 1, mb: 4 }}>
            {subtitle}
          </Typography>
          {children}
        </Paper>
      </Container>
    </Box>
  )
}
