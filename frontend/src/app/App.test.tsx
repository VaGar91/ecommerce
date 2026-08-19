import { render, screen } from "@testing-library/react";

import { CART_STORAGE_KEY } from "../features/cart/cartStore";
import { App } from "./App";
import { queryClient } from "./queryClient";

afterEach(() => {
  vi.unstubAllGlobals();
  window.localStorage.clear();
  queryClient.clear();
});

describe("App", () => {
  it("renders the catalog route and primary navigation", async () => {
    stubEmptyCatalog();
    window.history.pushState({}, "", "/catalog");

    render(<App />);

    expect(await screen.findByRole("heading", { name: "Find the right product" })).toBeInTheDocument();
    expect(screen.getByRole("navigation", { name: "Primary navigation" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Products" })).toHaveAttribute("href", "/admin/products");
  });

  it("shows the persisted cart quantity in primary navigation", async () => {
    window.localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([{
      productId: "0f6eb16e-65ed-4854-8a71-0ad8d1036aac",
      name: "Mechanical Keyboard",
      sku: "KEY-001",
      price: 49.9,
      availableStock: 5,
      quantity: 2,
    }]));
    stubEmptyCatalog();
    window.history.pushState({}, "", "/catalog");

    render(<App />);

    expect(await screen.findByRole("link", { name: /Cart.*2 items in cart/ })).toHaveAttribute("href", "/cart");
  });
});

function stubEmptyCatalog() {
  vi.stubGlobal("fetch", vi.fn<typeof fetch>().mockImplementation(() => Promise.resolve(
    new Response(JSON.stringify({
      content: [],
      page: 0,
      size: 12,
      totalElements: 0,
      totalPages: 0,
    }), { status: 200, headers: { "Content-Type": "application/json" } }),
  )));
}
