import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Paper,
  Stack,
  Switch,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from '@mui/material'
import { Navigate } from 'react-router-dom'
import {
  cancelClassSession,
  cancelAdminPassOrder,
  changeUserRole,
  changeUserStatus,
  completePassOrderPayment,
  createClassSession,
  createInstructor,
  createPassType,
  fetchAdminClasses,
  fetchAdminPassOrders,
  fetchAdminPassTypes,
  fetchAdminUsers,
  fetchInstructors,
  getApiError,
  grantUserPass,
  publishClassSession,
  updateInstructor,
  updatePassType,
} from './api'
import { useAuth } from './auth-context'

const emptyInstructor = {
  userId: '',
  specialization: '',
  description: '',
}

const emptyClass = {
  title: '',
  description: '',
  danceStyle: '',
  level: 'BEGINNER',
  instructorId: '',
  capacity: 12,
  startAt: '',
  durationMinutes: 60,
}

const emptyPassType = {
  name: '',
  description: '',
  type: 'LIMITED',
  visitCount: 4,
  validityDays: 30,
  price: 0,
  active: true,
}

const emptyGrant = {
  userId: '',
  passTypeId: '',
  validFrom: '',
}

export default function AdminPage() {
  const { user, initializing } = useAuth()
  const [tab, setTab] = useState(0)
  const [users, setUsers] = useState([])
  const [instructors, setInstructors] = useState([])
  const [classes, setClasses] = useState([])
  const [passTypes, setPassTypes] = useState([])
  const [passOrders, setPassOrders] = useState([])
  const [error, setError] = useState('')
  const [instructorDialog, setInstructorDialog] = useState(false)
  const [classDialog, setClassDialog] = useState(false)
  const [passDialog, setPassDialog] = useState(false)
  const [grantDialog, setGrantDialog] = useState(false)
  const [instructorForm, setInstructorForm] = useState(emptyInstructor)
  const [classForm, setClassForm] = useState(emptyClass)
  const [passForm, setPassForm] = useState(emptyPassType)
  const [grantForm, setGrantForm] = useState(emptyGrant)

  const load = async () => {
    setError('')
    try {
      const [usersData, instructorsData, classesData, passTypeData, passOrderData] = await Promise.all([
        fetchAdminUsers(),
        fetchInstructors(true),
        fetchAdminClasses(),
        fetchAdminPassTypes(),
        fetchAdminPassOrders(),
      ])
      setUsers(usersData)
      setInstructors(instructorsData)
      setClasses(classesData)
      setPassTypes(passTypeData)
      setPassOrders(passOrderData)
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось загрузить административные данные'))
    }
  }

  useEffect(() => {
    if (user?.role === 'ADMIN') {
      load()
    }
  }, [user])

  const instructorCandidates = useMemo(() => {
    const assigned = new Set(instructors.map((item) => item.userId))
    return users.filter((item) => !assigned.has(item.id) && item.status !== 'DEACTIVATED')
  }, [instructors, users])

  const clientUsers = useMemo(
    () => users.filter((item) => item.role === 'CLIENT' && item.status !== 'DEACTIVATED'),
    [users],
  )

  if (initializing) {
    return null
  }
  if (!user || user.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  const mutateUser = async (id, type, value) => {
    try {
      const updated = type === 'role'
        ? await changeUserRole(id, value)
        : await changeUserStatus(id, value)
      setUsers((current) => current.map((item) => item.id === id ? updated : item))
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось обновить пользователя'))
    }
  }

  const submitInstructor = async () => {
    try {
      await createInstructor({
        ...instructorForm,
        userId: Number(instructorForm.userId),
      })
      setInstructorDialog(false)
      setInstructorForm(emptyInstructor)
      await load()
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось создать инструктора'))
    }
  }

  const toggleInstructor = async (instructor) => {
    try {
      const updated = await updateInstructor(instructor.id, {
        specialization: instructor.specialization,
        description: instructor.description,
        publicProfile: !instructor.publicProfile,
      })
      setInstructors((current) =>
        current.map((item) => item.id === instructor.id ? updated : item))
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось обновить инструктора'))
    }
  }

  const submitClass = async () => {
    try {
      await createClassSession({
        ...classForm,
        instructorId: Number(classForm.instructorId),
        capacity: Number(classForm.capacity),
        durationMinutes: Number(classForm.durationMinutes),
        startAt: new Date(classForm.startAt).toISOString(),
      })
      setClassDialog(false)
      setClassForm(emptyClass)
      await load()
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось создать занятие'))
    }
  }

  const transitionClass = async (id, action) => {
    try {
      const updated = action === 'publish'
        ? await publishClassSession(id)
        : await cancelClassSession(id)
      setClasses((current) => current.map((item) => item.id === id ? updated : item))
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось изменить статус занятия'))
    }
  }

  const passPayload = (form) => ({
    ...form,
    visitCount: form.type === 'UNLIMITED' ? null : Number(form.visitCount),
    validityDays: Number(form.validityDays),
    price: Number(form.price),
  })

  const submitPassType = async () => {
    try {
      await createPassType(passPayload(passForm))
      setPassDialog(false)
      setPassForm(emptyPassType)
      await load()
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось создать абонемент'))
    }
  }

  const togglePassType = async (passType) => {
    try {
      const updated = await updatePassType(passType.id, passPayload({
        ...passType,
        active: !passType.active,
      }))
      setPassTypes((current) => current.map((item) => item.id === passType.id ? updated : item))
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось обновить абонемент'))
    }
  }

  const submitGrant = async () => {
    try {
      await grantUserPass({
        userId: Number(grantForm.userId),
        passTypeId: Number(grantForm.passTypeId),
        validFrom: grantForm.validFrom ? new Date(grantForm.validFrom).toISOString() : null,
      })
      setGrantDialog(false)
      setGrantForm(emptyGrant)
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось выдать абонемент'))
    }
  }

  const payOrder = async (id) => {
    try {
      const updated = await completePassOrderPayment(id, `manual-${id}`)
      setPassOrders((current) => current.map((item) => item.id === id ? updated : item))
      await load()
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось подтвердить оплату'))
    }
  }

  const cancelOrder = async (id) => {
    try {
      const updated = await cancelAdminPassOrder(id)
      setPassOrders((current) => current.map((item) => item.id === id ? updated : item))
    } catch (requestError) {
      setError(getApiError(requestError, 'Не удалось отменить заказ'))
    }
  }

  return (
    <Container maxWidth="xl" sx={{ py: 6 }}>
      <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" gap={2}>
        <Box>
          <Typography variant="h2" fontSize={{ xs: 38, md: 52 }}>Администрирование</Typography>
          <Typography color="text.secondary">Пользователи, команда и расписание</Typography>
        </Box>
        {tab === 1 && <Button variant="contained" onClick={() => setInstructorDialog(true)}>Добавить инструктора</Button>}
        {tab === 2 && <Button variant="contained" onClick={() => setClassDialog(true)}>Создать занятие</Button>}
        {tab === 3 && (
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" onClick={() => setGrantDialog(true)}>Выдать абонемент</Button>
            <Button variant="contained" onClick={() => setPassDialog(true)}>Создать абонемент</Button>
          </Stack>
        )}
      </Stack>

      {error && <Alert severity="error" onClose={() => setError('')} sx={{ mt: 3 }}>{error}</Alert>}

      <Paper variant="outlined" sx={{ mt: 4 }}>
        <Tabs value={tab} onChange={(_, value) => setTab(value)} variant="scrollable">
          <Tab label="Пользователи" />
          <Tab label="Инструкторы" />
          <Tab label="Расписание" />
          <Tab label="Абонементы" />
        </Tabs>
        <Box sx={{ p: { xs: 1, md: 3 } }}>
          {tab === 0 && <UsersTable users={users} mutateUser={mutateUser} />}
          {tab === 1 && <InstructorsTable instructors={instructors} toggle={toggleInstructor} />}
          {tab === 2 && <ClassesTable classes={classes} transition={transitionClass} />}
          {tab === 3 && (
            <Stack spacing={4}>
              <PassOrdersTable orders={passOrders} pay={payOrder} cancel={cancelOrder} />
              <PassTypesTable passTypes={passTypes} toggle={togglePassType} />
            </Stack>
          )}
        </Box>
      </Paper>

      <InstructorDialog
        open={instructorDialog}
        users={instructorCandidates}
        form={instructorForm}
        setForm={setInstructorForm}
        close={() => setInstructorDialog(false)}
        submit={submitInstructor}
      />
      <ClassDialog
        open={classDialog}
        instructors={instructors}
        form={classForm}
        setForm={setClassForm}
        close={() => setClassDialog(false)}
        submit={submitClass}
      />
      <PassTypeDialog
        open={passDialog}
        form={passForm}
        setForm={setPassForm}
        close={() => setPassDialog(false)}
        submit={submitPassType}
      />
      <GrantPassDialog
        open={grantDialog}
        users={clientUsers}
        passTypes={passTypes.filter((item) => item.active)}
        form={grantForm}
        setForm={setGrantForm}
        close={() => setGrantDialog(false)}
        submit={submitGrant}
      />
    </Container>
  )
}

function UsersTable({ users, mutateUser }) {
  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Пользователь</TableCell>
            <TableCell>Email</TableCell>
            <TableCell>Роль</TableCell>
            <TableCell>Статус</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {users.map((item) => (
            <TableRow key={item.id}>
              <TableCell>{item.firstName} {item.lastName}</TableCell>
              <TableCell>{item.email}</TableCell>
              <TableCell>
                <TextField
                  select
                  size="small"
                  value={item.role}
                  onChange={(event) => mutateUser(item.id, 'role', event.target.value)}
                >
                  {['CLIENT', 'INSTRUCTOR', 'ADMIN'].map((role) =>
                    <MenuItem key={role} value={role}>{role}</MenuItem>)}
                </TextField>
              </TableCell>
              <TableCell>
                <TextField
                  select
                  size="small"
                  value={item.status}
                  onChange={(event) => mutateUser(item.id, 'status', event.target.value)}
                >
                  {['PENDING', 'ACTIVE', 'BLOCKED', 'DEACTIVATED'].map((status) =>
                    <MenuItem key={status} value={status}>{status}</MenuItem>)}
                </TextField>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

function InstructorsTable({ instructors, toggle }) {
  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Инструктор</TableCell>
            <TableCell>Специализация</TableCell>
            <TableCell>Описание</TableCell>
            <TableCell>Публичный профиль</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {instructors.map((item) => (
            <TableRow key={item.id}>
              <TableCell>{item.firstName} {item.lastName}</TableCell>
              <TableCell>{item.specialization}</TableCell>
              <TableCell>{item.description || '—'}</TableCell>
              <TableCell>
                <Switch checked={item.publicProfile} onChange={() => toggle(item)} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

function ClassesTable({ classes, transition }) {
  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Занятие</TableCell>
            <TableCell>Инструктор</TableCell>
            <TableCell>Начало</TableCell>
            <TableCell>Места</TableCell>
            <TableCell>Статус</TableCell>
            <TableCell>Действия</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {classes.map((item) => (
            <TableRow key={item.id}>
              <TableCell>{item.title}<br /><small>{item.danceStyle}</small></TableCell>
              <TableCell>{item.instructorName}</TableCell>
              <TableCell>{new Date(item.startAt).toLocaleString('ru-RU')}</TableCell>
              <TableCell>{item.availablePlaces}/{item.capacity}</TableCell>
              <TableCell>{item.status}</TableCell>
              <TableCell>
                <Stack direction="row" spacing={1}>
                  {item.status === 'DRAFT' && (
                    <Button size="small" onClick={() => transition(item.id, 'publish')}>Опубликовать</Button>
                  )}
                  {!['CANCELLED', 'COMPLETED'].includes(item.status) && (
                    <Button size="small" color="error" onClick={() => transition(item.id, 'cancel')}>Отменить</Button>
                  )}
                </Stack>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

function PassTypesTable({ passTypes, toggle }) {
  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Абонемент</TableCell>
            <TableCell>Тип</TableCell>
            <TableCell>Срок</TableCell>
            <TableCell>Цена</TableCell>
            <TableCell>Активен</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {passTypes.map((item) => (
            <TableRow key={item.id}>
              <TableCell>{item.name}<br /><small>{item.description || 'без описания'}</small></TableCell>
              <TableCell>{item.type === 'UNLIMITED' ? 'Безлимит' : `${item.visitCount} занятий`}</TableCell>
              <TableCell>{item.validityDays} дней</TableCell>
              <TableCell>{item.price} {item.currency}</TableCell>
              <TableCell>
                <Switch checked={item.active} onChange={() => toggle(item)} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

function PassOrdersTable({ orders, pay, cancel }) {
  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>Заказы абонементов</Typography>
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Клиент</TableCell>
              <TableCell>Абонемент</TableCell>
              <TableCell>Сумма</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell>Создан</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {orders.map((item) => (
              <TableRow key={item.id}>
                <TableCell>{item.userName}</TableCell>
                <TableCell>{item.passName}</TableCell>
                <TableCell>{item.amount} {item.currency}</TableCell>
                <TableCell>{item.status}</TableCell>
                <TableCell>{new Date(item.createdAt).toLocaleString('ru-RU')}</TableCell>
                <TableCell>
                  {item.status === 'PENDING_PAYMENT' && (
                    <Stack direction="row" spacing={1}>
                      <Button size="small" onClick={() => pay(item.id)}>Подтвердить оплату</Button>
                      <Button size="small" color="error" onClick={() => cancel(item.id)}>Отменить</Button>
                    </Stack>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )
}

function InstructorDialog({ open, users, form, setForm, close, submit }) {
  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="sm">
      <DialogTitle>Новый инструктор</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            select
            label="Пользователь"
            value={form.userId}
            onChange={(event) => setForm({ ...form, userId: event.target.value })}
          >
            {users.map((item) => (
              <MenuItem key={item.id} value={item.id}>
                {item.firstName} {item.lastName} — {item.email}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            label="Специализация"
            value={form.specialization}
            onChange={(event) => setForm({ ...form, specialization: event.target.value })}
          />
          <TextField
            label="Описание"
            multiline
            minRows={3}
            value={form.description}
            onChange={(event) => setForm({ ...form, description: event.target.value })}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={close}>Отмена</Button>
        <Button variant="contained" onClick={submit} disabled={!form.userId || !form.specialization}>
          Создать
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function ClassDialog({ open, instructors, form, setForm, close, submit }) {
  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="sm">
      <DialogTitle>Новое занятие</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label="Название" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} />
          <TextField label="Описание" multiline minRows={2} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
          <TextField label="Направление" value={form.danceStyle} onChange={(event) => setForm({ ...form, danceStyle: event.target.value })} />
          <TextField select label="Уровень" value={form.level} onChange={(event) => setForm({ ...form, level: event.target.value })}>
            {['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'ALL'].map((level) => <MenuItem key={level} value={level}>{level}</MenuItem>)}
          </TextField>
          <TextField select label="Инструктор" value={form.instructorId} onChange={(event) => setForm({ ...form, instructorId: event.target.value })}>
            {instructors.map((item) => <MenuItem key={item.id} value={item.id}>{item.firstName} {item.lastName}</MenuItem>)}
          </TextField>
          <TextField type="datetime-local" label="Дата и время" InputLabelProps={{ shrink: true }} value={form.startAt} onChange={(event) => setForm({ ...form, startAt: event.target.value })} />
          <Stack direction="row" spacing={2}>
            <TextField fullWidth type="number" label="Вместимость" value={form.capacity} onChange={(event) => setForm({ ...form, capacity: event.target.value })} />
            <TextField fullWidth type="number" label="Длительность, мин." value={form.durationMinutes} onChange={(event) => setForm({ ...form, durationMinutes: event.target.value })} />
          </Stack>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={close}>Отмена</Button>
        <Button
          variant="contained"
          onClick={submit}
          disabled={!form.title || !form.danceStyle || !form.instructorId || !form.startAt}
        >
          Создать черновик
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function PassTypeDialog({ open, form, setForm, close, submit }) {
  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="sm">
      <DialogTitle>Новый абонемент</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField label="Название" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
          <TextField label="Описание" multiline minRows={2} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
          <TextField select label="Тип" value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>
            <MenuItem value="LIMITED">По количеству занятий</MenuItem>
            <MenuItem value="UNLIMITED">Безлимит</MenuItem>
          </TextField>
          {form.type === 'LIMITED' && (
            <TextField type="number" label="Количество занятий" value={form.visitCount} onChange={(event) => setForm({ ...form, visitCount: event.target.value })} />
          )}
          <Stack direction="row" spacing={2}>
            <TextField fullWidth type="number" label="Срок действия, дней" value={form.validityDays} onChange={(event) => setForm({ ...form, validityDays: event.target.value })} />
            <TextField fullWidth type="number" label="Цена" value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} />
          </Stack>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={close}>Отмена</Button>
        <Button
          variant="contained"
          onClick={submit}
          disabled={!form.name || !form.validityDays || (form.type === 'LIMITED' && !form.visitCount)}
        >
          Создать
        </Button>
      </DialogActions>
    </Dialog>
  )
}

function GrantPassDialog({ open, users, passTypes, form, setForm, close, submit }) {
  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="sm">
      <DialogTitle>Выдать абонемент</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField select label="Клиент" value={form.userId} onChange={(event) => setForm({ ...form, userId: event.target.value })}>
            {users.map((item) => (
              <MenuItem key={item.id} value={item.id}>
                {item.firstName} {item.lastName} — {item.email}
              </MenuItem>
            ))}
          </TextField>
          <TextField select label="Абонемент" value={form.passTypeId} onChange={(event) => setForm({ ...form, passTypeId: event.target.value })}>
            {passTypes.map((item) => (
              <MenuItem key={item.id} value={item.id}>
                {item.name} · {item.type === 'UNLIMITED' ? 'безлимит' : `${item.visitCount} занятий`}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            type="datetime-local"
            label="Действует с"
            InputLabelProps={{ shrink: true }}
            value={form.validFrom}
            onChange={(event) => setForm({ ...form, validFrom: event.target.value })}
            helperText="Если оставить пустым, начнётся с текущего момента"
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={close}>Отмена</Button>
        <Button variant="contained" onClick={submit} disabled={!form.userId || !form.passTypeId}>
          Выдать
        </Button>
      </DialogActions>
    </Dialog>
  )
}
