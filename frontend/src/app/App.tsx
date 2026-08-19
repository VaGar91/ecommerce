import { QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router";

import { queryClient } from "./queryClient";
import { AppRoutes } from "./routes/AppRoutes";

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </QueryClientProvider>
  );
}
