import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import DoctorLogin from "./pages/DoctorLogin";
import DoctorSignup from "./pages/DoctorSignup";

import './App.css'
import DoctorDashboard from "./pages/DoctorDashboard";

function App() {
 
  return (
    <Router>
      <Routes>
        <Route path="/doctor-login" element={<DoctorLogin />} />
        <Route path="/doctor-signup" element={<DoctorSignup />} />
        <Route path="/doctor-dashboard" element={<DoctorDashboard />} />
        <Route path="*" element={<Navigate to="/doctor-login" />} />
      </Routes>
    </Router>
  )
}

export default App
