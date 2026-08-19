import { Link } from "react-router";

export function NotFoundPage() {
  return (
    <section className="empty-state">
      <p className="eyebrow">404</p>
      <h1>Page not found</h1>
      <p>The requested page does not exist.</p>
      <Link className="button-link" to="/catalog">Return to the catalog</Link>
    </section>
  );
}
