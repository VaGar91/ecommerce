import { QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router";

import { CartProvider } from "../features/cart/CartProvider";
import { queryClient } from "./queryClient";
import { AppRoutes } from "./routes/AppRoutes";

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <CartProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </CartProvider>
    </QueryClientProvider>
  );
}
