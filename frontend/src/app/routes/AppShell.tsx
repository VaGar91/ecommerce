import { NavLink, Outlet } from "react-router";

const navigation = [
  { to: "/catalog", label: "Shop" },
  { to: "/admin/products", label: "Products" },
  { to: "/admin/import", label: "Import" },
  { to: "/cart", label: "Cart" },
];

export function AppShell() {
  return (
    <div className="app-shell">
      <header className="site-header">
        <NavLink className="brand" to="/catalog" aria-label="Gila Commerce home">
          <span className="brand-mark" aria-hidden="true">G</span>
          <span>
            <strong>Gila Commerce</strong>
            <small>Catalog and operations</small>
          </span>
        </NavLink>

        <nav className="site-nav" aria-label="Primary navigation">
          {navigation.map((item) => (
            <NavLink
              key={item.to}
              className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}
              to={item.to}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </header>

      <main className="page-container">
        <Outlet />
      </main>
    </div>
  );
}
