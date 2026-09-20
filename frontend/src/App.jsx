import { NavLink, Route, Routes } from 'react-router-dom'
import ProductsPage from './pages/ProductsPage.jsx'
import ProductDetailPage from './pages/ProductDetailPage.jsx'
import SellerPage from './pages/SellerPage.jsx'

export default function App() {
  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/products" className="brand">
            <span className="brand-mark">Bx</span>
            <span>BajriX Marketplace</span>
          </NavLink>
          <nav className="nav">
            <NavLink to="/products" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              Browse
            </NavLink>
            <NavLink to="/seller" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
              Seller Dashboard
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="main">
        <Routes>
          <Route path="/" element={<ProductsPage />} />
          <Route path="/products" element={<ProductsPage />} />
          <Route path="/products/:id" element={<ProductDetailPage />} />
          <Route path="/seller" element={<SellerPage />} />
          <Route path="*" element={<div className="empty-state">Page not found.</div>} />
        </Routes>
      </main>
    </div>
  )
}
