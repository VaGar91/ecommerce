import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, useLocation } from "react-router";

import type { Product, ProductPage } from "../../shared/api/products";
import { CartProvider } from "../cart/CartProvider";
import { CART_STORAGE_KEY } from "../cart/cartStore";
import { CatalogPage } from "./CatalogPage";

const keyboard: Product = {
  id: "0f6eb16e-65ed-4854-8a71-0ad8d1036aac",
  name: "Mechanical Keyboard",
  sku: "KEY-001",
  description: "Tactile mechanical keyboard",
  category: "Accessories",
  price: 49.9,
  stock: 2,
  weightKg: 0.85,
};

const unavailableMonitor: Product = {
  ...keyboard,
  id: "6fbb6162-f5b1-4c85-8c7d-a9f83003cd1e",
  name: "Studio Monitor",
  sku: "MON-002",
  category: "Displays",
  stock: 0,
};

afterEach(() => {
  vi.unstubAllGlobals();
  window.localStorage.clear();
});

describe("CatalogPage", () => {
  it("renders stock-aware products and persists cart additions", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(() =>
      Promise.resolve(jsonResponse(productPage([keyboard, unavailableMonitor]))));
    vi.stubGlobal("fetch", fetchMock);
    const view = renderCatalog();

    expect(await screen.findByRole("heading", { name: "Mechanical Keyboard" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Out of stock" })).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: "Add to cart" }));
    fireEvent.click(screen.getByRole("button", { name: "Add another" }));

    expect(screen.getByRole("button", { name: "Stock limit reached" })).toBeDisabled();
    expect(screen.getByText("2 currently in cart")).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Quantity: 2");

    await waitFor(() => {
      const storedCart = JSON.parse(window.localStorage.getItem(CART_STORAGE_KEY) ?? "[]") as Array<{ quantity: number }>;
      expect(storedCart[0]?.quantity).toBe(2);
    });

    view.unmount();
    renderCatalog();
    expect(await screen.findByText("2 currently in cart")).toBeInTheDocument();
  });

  it("stores submitted search filters in the URL and API request", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(() =>
      Promise.resolve(jsonResponse(productPage([keyboard]))));
    vi.stubGlobal("fetch", fetchMock);
    renderCatalog();

    await screen.findByRole("heading", { name: "Mechanical Keyboard" });
    fireEvent.change(screen.getByLabelText("Search products"), { target: { value: "mechanical" } });
    fireEvent.change(screen.getByLabelText("Category"), { target: { value: "Accessories" } });
    fireEvent.click(screen.getByRole("button", { name: "Search" }));

    await waitFor(() => {
      const lastUrl = String(fetchMock.mock.calls.at(-1)?.[0]);
      expect(lastUrl).toContain("query=mechanical");
      expect(lastUrl).toContain("category=Accessories");
    });
    expect(screen.getByTestId("location")).toHaveTextContent("query=mechanical&category=Accessories");

    fireEvent.click(screen.getByRole("button", { name: "Clear" }));
    expect(screen.getByTestId("location")).toHaveTextContent("/catalog");
  });

  it("paginates through URL-backed catalog results", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(() =>
      Promise.resolve(jsonResponse(productPage([keyboard], { totalElements: 13, totalPages: 2 }))));
    vi.stubGlobal("fetch", fetchMock);
    renderCatalog();

    await screen.findByRole("heading", { name: "Mechanical Keyboard" });
    const nextButton = screen.getByRole("button", { name: "Next" });
    await waitFor(() => expect(nextButton).toBeEnabled());
    fireEvent.click(nextButton);

    await waitFor(() => {
      expect(String(fetchMock.mock.calls.at(-1)?.[0])).toContain("page=1");
    });
    expect(screen.getByTestId("location")).toHaveTextContent("page=1");
  });
});

function renderCatalog(initialEntry = "/catalog") {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <CartProvider>
        <MemoryRouter initialEntries={[initialEntry]}>
          <CatalogPage />
          <LocationProbe />
        </MemoryRouter>
      </CartProvider>
    </QueryClientProvider>,
  );
}

function LocationProbe() {
  const location = useLocation();
  return <span data-testid="location" hidden>{location.pathname}{location.search}</span>;
}

function productPage(content: Product[], overrides: Partial<ProductPage> = {}): ProductPage {
  return {
    content,
    page: 0,
    size: 12,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
    ...overrides,
  };
}

function jsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}
