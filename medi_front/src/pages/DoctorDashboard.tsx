import React, { useState, useEffect } from "react";
import { motion } from "framer-motion";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { Calendar, dateFnsLocalizer } from "react-big-calendar";
import { format, parse, startOfWeek, getDay } from "date-fns";
import { enUS } from "date-fns/locale";
import { ChevronLeft, ChevronRight, LogOut } from "lucide-react";
import "react-big-calendar/lib/css/react-big-calendar.css";

const locales = {
  "en-US": enUS,
};

const localizer = dateFnsLocalizer({
  format,
  parse,
  startOfWeek: () => startOfWeek(new Date(), { weekStartsOn: 1 }),
  getDay,
  locales,
});

type Appointment = {
  id: number;
  date: string;
  time: string;
  patientId: number;
  patientName: string;
  description?: string
};

type Patient = {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
};

export default function DoctorDashboard() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [patients, setPatients] = useState<Patient[]>([]);
  const [patientId, setPatientId] = useState("");
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [createMsg, setCreateMsg] = useState("");
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [rescheduleModal, setRescheduleModal] = useState(false);
  const [rescheduleAppt, setRescheduleAppt] = useState<Appointment | null>(null);
  const [rescheduleDate, setRescheduleDate] = useState("");
  const [rescheduleTime, setRescheduleTime] = useState("");
  const [rescheduleMsg, setRescheduleMsg] = useState("");
  // Cancel modal state
  const [cancelModal, setCancelModal] = useState(false);
  const [cancelAppt, setCancelAppt] = useState<Appointment | null>(null);
  const [cancelMsg, setCancelMsg] = useState("");
  const [description, setDescription] = useState("");

  useEffect(() => {
    axios
      .get("http://localhost:8080/medilink/api/doctor/session", { withCredentials: true })
      .then((res) => {
        if (res.data.loggedIn) {
          axios
            .all([
              axios.get("http://localhost:8080/medilink/api/doctor/appointments", { withCredentials: true }),
              axios.get("http://localhost:8080/medilink/api/patients", { withCredentials: true }),
            ])
            .then(
              axios.spread((apptsRes, patientsRes) => {
                setAppointments(apptsRes.data);
                setPatients(patientsRes.data);
              }),
            );
        } else {
          navigate("/doctor/login");
        }
      })
      .catch(() => navigate("/doctor/login"))
      .finally(() => setLoading(false));
  }, [navigate]);

  const handleLogout = async () => {
    await axios.post("http://localhost:8080/medilink/api/doctor/logout", {}, { withCredentials: true });
    navigate("/doctor/login");
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateMsg("");
    try {
      const res = await axios.post(
        "http://localhost:8080/medilink/api/doctor/appointments",
        { patientId, date, time, description },
        { withCredentials: true },
      );
      if (res.data.success) {
        setCreateMsg("Appointment created successfully!");
        const res2 = await axios.get("http://localhost:8080/medilink/api/doctor/appointments", {
          withCredentials: true,
        });
        setAppointments(res2.data);
        setPatientId("");
        setDate("");
        setTime("");
        setDescription("");
        setTimeout(() => setCreateMsg(""), 2000);
      }
    } catch {
      setCreateMsg("Failed to create appointment");
    }
  };

  // Highlight days with appointments
  const dayHasAppointment = (date: Date) => {
    return appointments.some(a => {
      const apptDate = new Date(`${a.date}T${a.time}`);
      return (
        apptDate.getFullYear() === date.getFullYear() &&
        apptDate.getMonth() === date.getMonth() &&
        apptDate.getDate() === date.getDate()
      );
    });
  };

  // Helper to safely parse date strings (replace / with -)
  const safeDateString = (dateStr: string | Date) => {
    if (typeof dateStr === "string") {
      return dateStr.replace(/\//g, '-');
    }
    if (dateStr instanceof Date) {
      // Convert Date to yyyy-MM-dd
      return dateStr.toISOString().split('T')[0];
    }
    return "";
  };

  const events = appointments.map((a) => ({
    title: a.description ? `${a.patientName}: ${a.description}` : `Patient: ${a.patientName}`,
    start: new Date(`${safeDateString(a.date)}T${a.time}`),
    end: new Date(new Date(`${safeDateString(a.date)}T${a.time}`).getTime() + 3600000),
    allDay: false,
    description: a.description,
  }));

  // Filter appointments for the selected day (normalize to midnight)
  const normalizeDate = (date: Date): Date => {
    const normalized = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    return normalized;
  };

  const selectedDayAppointments = selectedDate
    ? appointments.filter(a => {
        if (!a.date || typeof a.date !== 'string' || !/\d{4}-\d{2}-\d{2}/.test(a.date)) {
          console.warn('Skipping invalid appointment date:', a);
          return false;
        }
        try {
          const apptDate = normalizeDate(new Date(`${safeDateString(a.date)}T${a.time}`));
          const selDate = normalizeDate(selectedDate);
          return apptDate.getTime() === selDate.getTime();
        } catch (error) {
          console.error('Error comparing dates:', error, a);
          return false;
        }
      })
    : [];

  // Add debug logging for appointments
  useEffect(() => {
    if (selectedDate) {
      console.log('Current appointments:', appointments.map(a => ({
        id: a.id,
        date: a.date,
        time: a.time,
        patientName: a.patientName
      })));
      console.log('Selected date:', selectedDate.toISOString());
      console.log('Filtered appointments:', selectedDayAppointments);
    }
  }, [appointments, selectedDate, selectedDayAppointments]);

  // Handle calendar navigation
  const handleNavigate = (newDate: Date) => {
    setSelectedDate(newDate);
  };

  // Open cancel modal
  const openCancel = (appt: Appointment) => {
    setCancelAppt(appt);
    setCancelMsg("");
    setCancelModal(true);
  };

  // Confirm cancel
  const handleCancel = async () => {
    if (!cancelAppt) return;
    try {
      await axios.delete(`http://localhost:8080/medilink/api/doctor/appointments?id=${cancelAppt.id}`, { withCredentials: true });
      setAppointments((prev) => prev.filter((a) => a.id !== cancelAppt.id));
      setCancelModal(false);
    } catch {
      setCancelMsg("Failed to cancel appointment");
    }
  };

  // Open reschedule modal
  const openReschedule = (appt: Appointment) => {
    setRescheduleAppt(appt);
    setRescheduleDate(appt.date);
    setRescheduleTime(appt.time);
    setRescheduleMsg("");
    setRescheduleModal(true);
  };

  // Submit reschedule
  const handleReschedule = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rescheduleAppt) return;
    try {
      const res = await axios.put(
        "http://localhost:8080/medilink/api/doctor/appointments",
        { id: rescheduleAppt.id, date: rescheduleDate, time: rescheduleTime },
        { withCredentials: true }
      );
      if (res.data.success) {
        setRescheduleMsg("Appointment rescheduled!");
        // Refresh appointments
        const res2 = await axios.get("http://localhost:8080/medilink/api/doctor/appointments", { withCredentials: true });
        setAppointments(res2.data);
        setRescheduleModal(false);
      } else {
        setRescheduleMsg(res.data.message || "Failed to reschedule");
      }
    } catch {
      setRescheduleMsg("Failed to reschedule");
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="text-foreground">
          Loading...
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-900 text-gray-100">
      <header className="p-6 flex justify-between items-center bg-gray-800/50 backdrop-blur-md border-b border-gray-700">
        <motion.h1
          className="text-2xl font-bold bg-gradient-to-r from-cyan-400 to-blue-500 bg-clip-text text-transparent"
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5 }}
        >
          Doctor Dashboard
        </motion.h1>
        <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.5 }}>
          <button
            onClick={handleLogout}
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-gray-700 hover:bg-gray-600 transition-all text-cyan-400"
          >
            <LogOut className="h-5 w-5" />
            <span>Logout</span>
          </button>
        </motion.div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <motion.div
          className="grid grid-cols-1 lg:grid-cols-2 gap-8"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6 }}
        >
          {/* Calendar Section */}
          <div className="rounded-xl p-6 bg-gray-800/50 backdrop-blur-md border border-gray-700">
            <Calendar
              localizer={localizer}
              events={events}
              startAccessor="start"
              endAccessor="end"
              style={{ height: 500 }}
              className="text-gray-100"
              eventPropGetter={(event: any) => ({
                style: {
                  backgroundColor: '#06b6d4',
                  color: '#fff',
                  borderRadius: '0.5rem',
                  border: 'none',
                  boxShadow: '0 2px 8px rgba(6,182,212,0.2)',
                  fontWeight: 'bold',
                },
              })}
              components={{
                toolbar: (props: any) => {
                  const currentDate = props.date;
                  const dayLabel = format(selectedDate || currentDate, "EEEE, MMM dd");
                  return (
                    <div className="flex items-center justify-between mb-4 text-gray-300">
                      <button 
                        onClick={() => props.onNavigate("PREV")} 
                        className="p-2 hover:bg-gray-700/50 rounded-lg"
                      >
                        <ChevronLeft className="h-5 w-5" />
                      </button>
                      <span className="text-lg font-medium">
                        {format(currentDate, "MMMM yyyy")} <span className="text-cyan-400 font-semibold">| {dayLabel}</span>
                      </span>
                      <button 
                        onClick={() => props.onNavigate("NEXT")} 
                        className="p-2 hover:bg-gray-700/50 rounded-lg"
                      >
                        <ChevronRight className="h-5 w-5" />
                      </button>
                    </div>
                  );
                },
                event: ({ event }: any) => (
                  <div title={event.description} className="truncate">
                    <span className="font-semibold">{event.title}</span>
                  </div>
                ),
              }}
              dayPropGetter={(date: Date) => {
                if (dayHasAppointment(date)) {
                  return { style: { backgroundColor: 'rgba(6,182,212,0.08)' } };
                }
                return {};
              }}
              onSelectSlot={(slotInfo: any) => setSelectedDate(slotInfo.start)}
              selectable
              onSelectEvent={(event: any) => setSelectedDate(event.start)}
              views={['month']}
              date={selectedDate || new Date()}
              onNavigate={handleNavigate}
            />
          </div>

          {/* Appointment Creation Section */}
          <motion.div
            className="rounded-xl p-6 bg-gray-800/50 backdrop-blur-md border border-gray-700"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4 }}
          >
            <h2 className="text-xl font-semibold mb-6 text-cyan-400">Create New Appointment</h2>
            <form onSubmit={handleCreate} className="space-y-4">
              <div className="space-y-2">
                <label className="block text-sm font-medium text-gray-300">Patient</label>
                <select
                  value={patientId}
                  onChange={(e) => setPatientId(e.target.value)}
                  className="w-full px-4 py-2 rounded-lg bg-gray-700 border border-gray-600 text-gray-100 focus:ring-2 focus:ring-cyan-500 focus:border-transparent"
                  required
                >
                  <option value="">Select Patient</option>
                  {patients.map((p) => (
                    <option key={p.id} value={p.id} className="bg-gray-800">
                      {p.firstName} {p.lastName}
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-2">
                <label className="block text-sm font-medium text-gray-300">Description</label>
                <textarea
                  value={description}
                  onChange={e => setDescription(e.target.value)}
                  className="w-full px-4 py-2 rounded-lg bg-gray-700 border border-gray-600 text-gray-100 focus:ring-2 focus:ring-cyan-500 focus:border-transparent"
                  rows={3}
                  placeholder="Appointment details, reason, etc. (optional)"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="block text-sm font-medium text-gray-300">Date</label>
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full px-4 py-2 rounded-lg bg-gray-700 border border-gray-600 text-gray-100 focus:ring-2 focus:ring-cyan-500 focus:border-transparent"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <label className="block text-sm font-medium text-gray-300">Time</label>
                  <input
                    type="time"
                    value={time}
                    onChange={(e) => setTime(e.target.value)}
                    className="w-full px-4 py-2 rounded-lg bg-gray-700 border border-gray-600 text-gray-100 focus:ring-2 focus:ring-cyan-500 focus:border-transparent"
                    required
                  />
                </div>
              </div>

              <button
                type="submit"
                className="w-full py-2 px-4 bg-cyan-600 hover:bg-cyan-500 text-gray-100 rounded-lg transition-all font-medium shadow-lg hover:shadow-cyan-500/20"
              >
                Create Appointment
              </button>

              {createMsg && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="text-center text-cyan-400 text-sm mt-2"
                >
                  {createMsg}
                </motion.div>
              )}
            </form>
          </motion.div>
        </motion.div>

        {/* Appointments List */}
        {selectedDate && selectedDayAppointments.length > 0 && (
          <motion.div className="mt-8" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.2 }}>
            <h2 className="text-xl font-semibold mb-4 text-cyan-400">Appointments for {format(selectedDate, "MMM dd, yyyy")}</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {selectedDayAppointments.map((appointment, index) => (
                <motion.div
                  key={appointment.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.1 }}
                  className="rounded-lg p-4 bg-gray-800/50 backdrop-blur-md border border-gray-700 hover:border-cyan-500/30 transition-colors"
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <h3 className="font-medium text-lg text-gray-100">{appointment.patientName}</h3>
                      <p className="text-sm text-cyan-400">
                        {format(new Date(appointment.date), "MMM dd, yyyy")} at {appointment.time}
                      </p>
                      {appointment.description && (
                        <p className="text-sm text-gray-300 mt-1">{appointment.description}</p>
                      )}
                    </div>
                    <span className="bg-cyan-500/20 text-cyan-400 px-3 py-1 rounded-full text-sm">#{index + 1}</span>
                  </div>
                  <div className="flex gap-2 mt-3">
                    <button
                      onClick={() => openCancel(appointment)}
                      className="px-3 py-1 rounded border border-red-400 text-red-400 hover:text-red-300 hover:border-red-300 bg-transparent text-xs transition-colors"
                    >
                      Cancel
                    </button>
                    <button
                      onClick={() => openReschedule(appointment)}
                      className="px-3 py-1 rounded border border-cyan-400 text-cyan-400 hover:text-cyan-300 hover:border-cyan-300 bg-transparent text-xs transition-colors"
                    >
                      Reschedule
                    </button>
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.div>
        )}
      </main>

      {rescheduleModal && (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50">
          <div className="bg-gray-800 p-6 rounded-lg shadow-lg w-full max-w-sm">
            <h3 className="text-lg font-semibold mb-4 text-cyan-400">Reschedule Appointment</h3>
            <form onSubmit={handleReschedule} className="space-y-4">
              <div>
                <label className="block text-sm mb-1">Date</label>
                <input
                  type="date"
                  value={rescheduleDate}
                  onChange={e => setRescheduleDate(e.target.value)}
                  className="w-full px-3 py-2 rounded bg-gray-700 border border-gray-600 text-gray-100"
                  required
                />
              </div>
              <div>
                <label className="block text-sm mb-1">Time</label>
                <input
                  type="time"
                  value={rescheduleTime}
                  onChange={e => setRescheduleTime(e.target.value)}
                  className="w-full px-3 py-2 rounded bg-gray-700 border border-gray-600 text-gray-100"
                  required
                />
              </div>
              <div className="flex gap-2 mt-4">
                <button
                  type="submit"
                  className="flex-1 py-2 rounded bg-cyan-600 hover:bg-cyan-500 text-white font-medium"
                >
                  Save
                </button>
                <button
                  type="button"
                  onClick={() => setRescheduleModal(false)}
                  className="flex-1 py-2 rounded bg-gray-600 hover:bg-gray-500 text-white"
                >
                  Cancel
                </button>
              </div>
              {rescheduleMsg && <div className="text-center text-red-400 text-sm mt-2">{rescheduleMsg}</div>}
            </form>
          </div>
        </div>
      )}

      {cancelModal && (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50">
          <div className="bg-gray-800 p-6 rounded-lg shadow-lg w-full max-w-sm">
            <h3 className="text-lg font-semibold mb-4 text-red-400">Cancel Appointment</h3>
            <p className="mb-4 text-gray-200">Are you sure you want to cancel the appointment with <span className="text-cyan-400 font-semibold">{cancelAppt?.patientName}</span> on <span className="text-cyan-400 font-semibold">{cancelAppt?.date}</span> at <span className="text-cyan-400 font-semibold">{cancelAppt?.time}</span>?</p>
            <div className="flex gap-2 mt-4">
              <button
                onClick={handleCancel}
                className="flex-1 py-2 rounded bg-red-600 hover:bg-red-500 text-white font-medium"
              >
                Yes, Cancel
              </button>
              <button
                onClick={() => setCancelModal(false)}
                className="flex-1 py-2 rounded bg-gray-600 hover:bg-gray-500 text-white"
              >
                No, Go Back
              </button>
            </div>
            {cancelMsg && <div className="text-center text-red-400 text-sm mt-2">{cancelMsg}</div>}
          </div>
        </div>
      )}
    </div>
  );
}