import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";

import { CartProvider } from "../cart/CartProvider";
import { CART_STORAGE_KEY, type CartItem } from "../cart/cartStore";
import { CheckoutPage } from "./CheckoutPage";

const keyboard: CartItem = {
  productId: "0f6eb16e-65ed-4854-8a71-0ad8d1036aac",
  name: "Mechanical Keyboard",
  sku: "KEY-001",
  price: 49.9,
  availableStock: 5,
  quantity: 2,
};

const cable: CartItem = {
  productId: "3ef8cf18-f727-4d97-8f39-2c3d8c62cb86",
  name: "USB Cable",
  sku: "CAB-002",
  price: 20,
  availableStock: 10,
  quantity: 1,
};

const paidOrder = {
  id: "62b2898e-fb75-42f2-b62e-8e5bb52d6026",
  status: "PAID",
  total: 119.8,
  currency: "USD",
  paymentReference: "fake-62b2898e",
  createdAt: "2026-08-19T21:30:00Z",
  items: [
    {
      productId: keyboard.productId,
      productName: keyboard.name,
      sku: keyboard.sku,
      unitPrice: keyboard.price,
      quantity: keyboard.quantity,
      lineTotal: 99.8,
    },
    {
      productId: cable.productId,
      productName: cable.name,
      sku: cable.sku,
      unitPrice: cable.price,
      quantity: cable.quantity,
      lineTotal: 20,
    },
  ],
};

afterEach(() => {
  vi.unstubAllGlobals();
  window.localStorage.clear();
});

describe("CheckoutPage", () => {
  it("prevents checkout when the cart is empty", () => {
    renderCheckout();

    expect(screen.getByRole("heading", { name: "There is nothing to purchase" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Browse products" })).toHaveAttribute("href", "/catalog");
  });

  it("purchases cart quantities and renders the authoritative paid order", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse(paidOrder, 201));
    vi.stubGlobal("fetch", fetchMock);
    seedCart([keyboard, cable]);
    renderCheckout();

    fireEvent.click(screen.getByRole("button", { name: "Place order" }));

    expect(await screen.findByRole("heading", { name: "Order confirmed" })).toBeInTheDocument();
    expect(screen.getByText("fake-62b2898e")).toBeInTheDocument();
    expect(screen.getByText("$119.80")).toBeInTheDocument();
    expect(screen.getByText("PAID")).toBeInTheDocument();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [path, request] = fetchMock.mock.calls[0];
    expect(path).toBe("/api/orders");
    expect(JSON.parse(String(request?.body))).toEqual({
      items: [
        { productId: keyboard.productId, quantity: 2 },
        { productId: cable.productId, quantity: 1 },
      ],
      paymentToken: "tok_approved",
    });

    await waitFor(() => expect(window.localStorage.getItem(CART_STORAGE_KEY)).toBe("[]"));
  });

  it("keeps the cart intact when the fake payment is declined", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({
      title: "Payment declined",
      status: 402,
      detail: "The fake payment provider declined the charge",
    }, 402));
    vi.stubGlobal("fetch", fetchMock);
    seedCart([keyboard]);
    renderCheckout();

    fireEvent.click(screen.getByLabelText(/Simulate a decline/));
    fireEvent.click(screen.getByRole("button", { name: "Place order" }));

    const error = await screen.findByRole("alert");
    expect(error).toHaveTextContent("Payment declined");
    expect(error).toHaveTextContent("inventory are unchanged");
    expect(JSON.parse(String(fetchMock.mock.calls[0]?.[1]?.body))).toMatchObject({
      paymentToken: "tok_declined",
    });
    expect(readStoredCart()).toHaveLength(1);

    fireEvent.click(screen.getByLabelText(/Approve payment/));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("explains a stock conflict without discarding the cart", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse({
      title: "Insufficient stock",
      status: 409,
      detail: "Insufficient stock for product KEY-001",
    }, 409));
    vi.stubGlobal("fetch", fetchMock);
    seedCart([keyboard]);
    renderCheckout();

    fireEvent.click(screen.getByRole("button", { name: "Place order" }));

    const error = await screen.findByRole("alert");
    expect(error).toHaveTextContent("Stock changed");
    expect(error).toHaveTextContent("No payment or inventory change was committed");
    expect(screen.getByRole("link", { name: "Return to cart" })).toHaveAttribute("href", "/cart");
    expect(readStoredCart()).toHaveLength(1);
  });
});

function renderCheckout() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <CartProvider>
        <MemoryRouter>
          <CheckoutPage />
        </MemoryRouter>
      </CartProvider>
    </QueryClientProvider>,
  );
}

function seedCart(items: CartItem[]) {
  window.localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items));
}

function readStoredCart(): CartItem[] {
  return JSON.parse(window.localStorage.getItem(CART_STORAGE_KEY) ?? "[]") as CartItem[];
}

function jsonResponse(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
