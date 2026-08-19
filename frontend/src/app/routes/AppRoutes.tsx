import { Navigate, Route, Routes } from "react-router";

import { CartPage } from "../../features/cart/CartPage";
import { CatalogPage } from "../../features/catalog/CatalogPage";
import { CheckoutPage } from "../../features/checkout/CheckoutPage";
import { ProductImportPage } from "../../features/product-import/ProductImportPage";
import { ProductAdminPage } from "../../features/products/ProductAdminPage";
import { AppShell } from "./AppShell";
import { NotFoundPage } from "./NotFoundPage";

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate to="/catalog" replace />} />
        <Route path="catalog" element={<CatalogPage />} />
        <Route path="admin/products" element={<ProductAdminPage />} />
        <Route path="admin/import" element={<ProductImportPage />} />
        <Route path="cart" element={<CartPage />} />
        <Route path="checkout" element={<CheckoutPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
