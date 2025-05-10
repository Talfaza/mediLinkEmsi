import React, { useState } from "react";
import axios from "axios";
import { Link, useNavigate } from "react-router-dom";

const DoctorSignup: React.FC = () => {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [specialty, setSpecialty] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      const response = await axios.post("http://192.168.1.28:8080/medilink/api/doctor/register", {
        firstName,
        lastName,
        email,
        password,
        specialty,
      });
      if (response.data.success) {
        setSuccess("Registration successful! Please check your email to verify your account.");
        setTimeout(() => navigate("/doctor-login"), 2000);
      } else {
        setError(response.data.message || "Registration failed");
      }
    } catch (err: any) {
      setError(err.response?.data?.message || "Registration failed");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-900">
      <div className="w-full max-w-md bg-slate-800 rounded-2xl shadow-xl p-8 relative overflow-hidden">
        {/* Decorative background elements */}
        <div className="absolute -top-32 -right-32 w-64 h-64 bg-sky-500/20 rounded-full"></div>
        <div className="absolute -bottom-32 -left-32 w-64 h-64 bg-sky-500/20 rounded-full"></div>

        <div className="flex flex-col items-center mb-8 relative z-10">
          <div className="bg-sky-500 rounded-xl p-4 mb-4 shadow-lg transition-transform hover:scale-105">
            <svg 
              className="w-12 h-12 text-white" 
              fill="none" 
              stroke="currentColor" 
              strokeWidth="2" 
              viewBox="0 0 24 24"
            >
              <path 
                d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" 
                strokeLinecap="round" 
                strokeLinejoin="round"
              />
              <circle 
                cx="9" 
                cy="7" 
                r="4" 
                strokeLinecap="round" 
                strokeLinejoin="round"
              />
              <path 
                d="M23 21v-2a4 4 0 0 0-3-3.87" 
                strokeLinecap="round" 
                strokeLinejoin="round"
              />
              <path 
                d="M16 3.13a4 4 0 0 1 0 7.75" 
                strokeLinecap="round" 
                strokeLinejoin="round"
              />
            </svg>
          </div>
          <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-sky-400 to-blue-600 mb-2">
            MediLink
          </h1>
          <p className="text-slate-300 text-lg">Doctor Registration</p>
        </div>

        <form onSubmit={handleSubmit} className="relative z-10">
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-slate-300 mb-2 font-medium">First Name</label>
              <input
                type="text"
                className="w-full px-4 py-3 rounded-lg bg-slate-700/50 border border-slate-600 text-white 
                  focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
                placeholder="John"
                value={firstName}
                onChange={e => setFirstName(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="block text-slate-300 mb-2 font-medium">Last Name</label>
              <input
                type="text"
                className="w-full px-4 py-3 rounded-lg bg-slate-700/50 border border-slate-600 text-white 
                  focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
                placeholder="Doe"
                value={lastName}
                onChange={e => setLastName(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="mb-4">
            <label className="block text-slate-300 mb-2 font-medium">Email</label>
            <input
              type="email"
              className="w-full px-4 py-3 rounded-lg bg-slate-700/50 border border-slate-600 text-white 
                focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
              placeholder="name@domain.com"
              value={email}
              onChange={e => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="mb-4">
            <label className="block text-slate-300 mb-2 font-medium">Password</label>
            <input
              type="password"
              className="w-full px-4 py-3 rounded-lg bg-slate-700/50 border border-slate-600 text-white 
                focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
              placeholder="••••••••"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
            />
          </div>

          <div className="mb-6">
            <label className="block text-slate-300 mb-2 font-medium">Specialty</label>
            <input
              type="text"
              className="w-full px-4 py-3 rounded-lg bg-slate-700/50 border border-slate-600 text-white 
                focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition-all"
              placeholder="Cardiology"
              value={specialty}
              onChange={e => setSpecialty(e.target.value)}
              required
            />
          </div>

          {error && <div className="text-rose-400 text-sm mb-4 text-center">{error}</div>}
          {success && <div className="text-emerald-400 text-sm mb-4 text-center">{success}</div>}

          <button 
            type="submit" 
            className="w-full bg-gradient-to-r from-sky-500 to-blue-600 hover:from-sky-600 hover:to-blue-700 
              text-white font-semibold py-3 rounded-lg transition-all duration-300 shadow-lg hover:shadow-xl"
          >
            Create Account
          </button>
        </form>

       
        <p className="text-slate-300 text-sm text-center mt-4 relative z-10">
          Already have an account?{" "}
          <Link 
            to="/doctor-login" 
            className="text-sky-400 font-semibold hover:text-sky-300 transition-colors underline"
          >
            Sign in here
          </Link>
        </p>
      </div>
    </div>
  );
};

export default DoctorSignup;