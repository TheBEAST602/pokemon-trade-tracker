import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import ServerBanner from './components/ServerBanner.jsx'
import DealForm from './pages/DealForm.jsx'
import Deals from './pages/Deals.jsx'
import Today from './pages/Today.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <ServerBanner />
        <main className="page">
          <Routes>
            <Route path="/" element={<Today />} />
            <Route path="/deals" element={<Deals />} />
            <Route path="/deals/new" element={<DealForm />} />
            <Route path="/deals/:id/edit" element={<DealForm key="edit" />} />
            <Route path="*" element={<p className="loading">Page not found.</p>} />
          </Routes>
        </main>
        <nav className="nav" aria-label="Main">
          <NavLink to="/" end>
            Today
          </NavLink>
          <NavLink to="/deals" end>
            All deals
          </NavLink>
          <NavLink to="/deals/new" className="nav__add">
            Add deal
          </NavLink>
        </nav>
      </div>
    </BrowserRouter>
  )
}
