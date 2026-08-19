import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";

import type { Product, ProductPage } from "../../shared/api/products";
import { ProductAdminPage } from "./ProductAdminPage";

const product: Product = {
  id: "0f6eb16e-65ed-4854-8a71-0ad8d1036aac",
  name: "Mechanical Keyboard",
  sku: "KEY-001",
  description: "Tactile mechanical keyboard",
  category: "Accessories",
  price: 49.9,
  stock: 25,
  weightKg: 0.85,
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("ProductAdminPage", () => {
  it("lists, filters and paginates products", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(() => Promise.resolve(
      jsonResponse(productPage([product], { totalElements: 11, totalPages: 2 })),
    ));
    vi.stubGlobal("fetch", fetchMock);

    renderAdmin();

    expect(await screen.findByText("Mechanical Keyboard")).toBeInTheDocument();
    expect(screen.getByText("KEY-001")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Search"), { target: { value: "mechanical" } });
    fireEvent.change(screen.getByLabelText("Category"), { target: { value: "Accessories" } });
    fireEvent.click(screen.getByRole("button", { name: "Apply filters" }));

    await waitFor(() => {
      const lastUrl = String(fetchMock.mock.calls.at(-1)?.[0]);
      expect(lastUrl).toContain("query=mechanical");
      expect(lastUrl).toContain("category=Accessories");
    });

    const nextButton = screen.getByRole("button", { name: "Next" });
    await waitFor(() => expect(nextButton).toBeEnabled());
    fireEvent.click(nextButton);

    await waitFor(() => {
      expect(String(fetchMock.mock.calls.at(-1)?.[0])).toContain("page=1");
    });
  });

  it("validates a new product before sending it", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(jsonResponse(productPage([])));
    vi.stubGlobal("fetch", fetchMock);

    renderAdmin();

    await screen.findByText("No products yet.");
    fireEvent.click(screen.getByRole("button", { name: "Add product" }));
    fireEvent.click(screen.getByRole("button", { name: "Create product" }));

    expect(await screen.findByText("Name is required")).toBeInTheDocument();
    expect(screen.getByText("SKU is required")).toBeInTheDocument();
    expect(screen.getByText("Weight is required")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("creates a product and refreshes the list", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation((_input, init) => {
      if (init?.method === "POST") {
        return Promise.resolve(jsonResponse(product, 201));
      }
      return Promise.resolve(jsonResponse(productPage([])));
    });
    vi.stubGlobal("fetch", fetchMock);

    renderAdmin();

    await screen.findByText("No products yet.");
    fireEvent.click(screen.getByRole("button", { name: "Add product" }));
    fillProductForm();
    fireEvent.click(screen.getByRole("button", { name: "Create product" }));

    expect(await screen.findByText("Product created.")).toBeInTheDocument();

    const createCall = fetchMock.mock.calls.find(([, init]) => init?.method === "POST");
    expect(createCall?.[0]).toBe("/api/products");
    expect(JSON.parse(String(createCall?.[1]?.body))).toEqual({
      name: "Mechanical Keyboard",
      sku: "KEY-001",
      description: "Tactile mechanical keyboard",
      category: "Accessories",
      price: 49.9,
      stock: 25,
      weightKg: 0.85,
    });
  });

  it("maps backend field errors into the form", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation((_input, init) => {
      if (init?.method === "POST") {
        return Promise.resolve(jsonResponse({
          title: "Invalid request",
          status: 400,
          detail: "One or more fields are invalid",
          errors: { sku: "must not exceed 64 characters" },
        }, 400));
      }
      return Promise.resolve(jsonResponse(productPage([])));
    });
    vi.stubGlobal("fetch", fetchMock);

    renderAdmin();

    await screen.findByText("No products yet.");
    fireEvent.click(screen.getByRole("button", { name: "Add product" }));
    fillProductForm();
    fireEvent.click(screen.getByRole("button", { name: "Create product" }));

    expect(await screen.findByText("must not exceed 64 characters")).toBeInTheDocument();
    expect(within(screen.getByRole("dialog")).getByLabelText(/SKU/)).toHaveAttribute("aria-invalid", "true");
  });

  it("updates and deletes a product with confirmation", async () => {
    const updatedProduct = { ...product, name: "Ergonomic Keyboard" };
    const fetchMock = vi.fn<typeof fetch>().mockImplementation((_input, init) => {
      if (init?.method === "PUT") {
        return Promise.resolve(jsonResponse(updatedProduct));
      }
      if (init?.method === "DELETE") {
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      return Promise.resolve(jsonResponse(productPage([product])));
    });
    vi.stubGlobal("fetch", fetchMock);

    renderAdmin();

    await screen.findByText("Mechanical Keyboard");
    fireEvent.click(screen.getByRole("button", { name: "Edit Mechanical Keyboard" }));
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Ergonomic Keyboard" } });
    fireEvent.click(screen.getByRole("button", { name: "Save changes" }));

    expect(await screen.findByText("Product updated.")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/products/${product.id}`,
      expect.objectContaining({ method: "PUT" }),
    );

    fireEvent.click(screen.getByRole("button", { name: "Delete Mechanical Keyboard" }));
    const confirmation = screen.getByRole("alertdialog");
    expect(within(confirmation).getByText(/will be removed from the catalog/)).toBeInTheDocument();
    fireEvent.click(within(confirmation).getByRole("button", { name: "Delete product" }));

    expect(await screen.findByText("Product deleted.")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      `/api/products/${product.id}`,
      expect.objectContaining({ method: "DELETE" }),
    );
  });
});

function renderAdmin() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ProductAdminPage />
    </QueryClientProvider>,
  );
}

function fillProductForm() {
  const productDialog = within(screen.getByRole("dialog"));
  fireEvent.change(productDialog.getByLabelText("Name"), { target: { value: product.name } });
  fireEvent.change(productDialog.getByLabelText("SKU"), { target: { value: product.sku } });
  fireEvent.change(productDialog.getByLabelText("Description"), { target: { value: product.description } });
  fireEvent.change(productDialog.getByLabelText("Category"), { target: { value: product.category } });
  fireEvent.change(productDialog.getByLabelText("Price"), { target: { value: String(product.price) } });
  fireEvent.change(productDialog.getByLabelText("Stock"), { target: { value: String(product.stock) } });
  fireEvent.change(productDialog.getByLabelText("Weight (kg)"), { target: { value: String(product.weightKg) } });
}

function productPage(
  content: Product[],
  overrides: Partial<ProductPage> = {},
): ProductPage {
  return {
    content,
    page: 0,
    size: 10,
    totalElements: content.length,
    totalPages: content.length > 0 ? 1 : 0,
    ...overrides,
  };
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
